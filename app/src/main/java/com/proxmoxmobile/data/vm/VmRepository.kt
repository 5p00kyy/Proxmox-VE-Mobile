package com.proxmoxmobile.data.vm

import com.proxmoxmobile.data.api.ProxmoxApiService
import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.VirtualMachine
import com.proxmoxmobile.data.model.VmSnapshot
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class VmRepository(
    private val api: VmApi
) {
    suspend fun getVirtualMachines(nodeName: String): VmResult<List<VirtualMachine>> {
        return runVmRequest {
            require(nodeName.isNotBlank()) { "Node name is required" }
            api.getVirtualMachines(nodeName).data.filter { it.isValid() }
        }
    }

    suspend fun getVirtualMachineDetail(
        nodeNames: List<String>,
        vmid: Int,
        preferredNodeName: String? = null
    ): VmResult<VmDetail> {
        return runVmRequest {
            require(vmid > 0) { "VMID is required" }
            val candidateNodes = (
                listOfNotNull(preferredNodeName?.trim()?.takeIf { it.isNotBlank() }) +
                    nodeNames.map { it.trim() }.filter { it.isNotBlank() }
                )
                .distinct()
            require(candidateNodes.isNotEmpty()) { "At least one node is required" }

            for (nodeName in candidateNodes) {
                try {
                    val vm = api.getVMStatus(nodeName, vmid).data
                    if (vm.isValid()) {
                        return@runVmRequest VmDetail(
                            nodeName = nodeName,
                            virtualMachine = vm
                        )
                    }
                } catch (e: NotAuthenticatedException) {
                    throw e
                } catch (_: Exception) {
                    // Continue searching other cached nodes; the VM may live elsewhere.
                }
            }

            throw IllegalArgumentException("VM not found")
        }
    }

    suspend fun performAction(
        nodeName: String,
        vmid: Int,
        action: VmPowerAction
    ): VmResult<VmActionResult> {
        return runVmRequest {
            require(nodeName.isNotBlank()) { "Node name is required" }
            require(vmid > 0) { "VMID is required" }

            val response = api.performVMAction(nodeName, vmid, action.apiValue)
            VmActionResult(
                vmid = vmid,
                action = action,
                taskId = response.data.takeIf { it.isNotBlank() }
            )
        }
    }

    suspend fun deleteVirtualMachine(
        nodeName: String,
        vmid: Int
    ): VmResult<VmActionResult> {
        return runVmRequest {
            require(nodeName.isNotBlank()) { "Node name is required" }
            require(vmid > 0) { "VMID is required" }

            val response = api.deleteVM(nodeName, vmid)
            VmActionResult(
                vmid = vmid,
                action = VmPowerAction.Delete,
                taskId = response.data.takeIf { it.isNotBlank() }
            )
        }
    }

    suspend fun getSnapshots(
        nodeName: String,
        vmid: Int
    ): VmResult<List<VmSnapshot>> {
        return runVmRequest {
            require(nodeName.isNotBlank()) { "Node name is required" }
            require(vmid > 0) { "VMID is required" }

            api.getVMSnapshots(nodeName, vmid)
                .data
                .filter { it.isValid() }
                .sortedWith(
                    compareByDescending<VmSnapshot> { it.name == CURRENT_SNAPSHOT_NAME }
                        .thenByDescending { it.snaptime ?: 0L }
                        .thenBy { it.name }
                )
        }
    }

    suspend fun getConfig(
        nodeName: String,
        vmid: Int
    ): VmResult<List<VmConfigEntry>> {
        return runVmRequest {
            require(nodeName.isNotBlank()) { "Node name is required" }
            require(vmid > 0) { "VMID is required" }

            api.getVMConfig(nodeName, vmid)
                .data
                .mapNotNull { (key, value) ->
                    key.trim().takeIf { it.isNotBlank() }?.let { normalizedKey ->
                        value.toConfigValue()?.let { normalizedValue ->
                            VmConfigEntry(
                                key = normalizedKey,
                                value = normalizedKey.redactIfSensitive(normalizedValue)
                            )
                        }
                    }
                }
                .sortedWith(compareBy<VmConfigEntry> { it.key.configSortWeight() }.thenBy { it.key })
        }
    }

    private suspend fun <T> runVmRequest(block: suspend () -> T): VmResult<T> {
        return try {
            VmResult.Success(block())
        } catch (e: Exception) {
            VmResult.Error(e.toVmErrorMessage())
        }
    }

    private fun VirtualMachine.isValid(): Boolean {
        return vmid > 0 &&
            name.isNotBlank() &&
            status.isNotBlank() &&
            cpu >= 0 &&
            mem >= 0 &&
            maxmem >= 0 &&
            uptime >= 0
    }

    private fun VmSnapshot.isValid(): Boolean {
        return name.isNotBlank() &&
            (snaptime == null || snaptime >= 0)
    }

    private fun Any?.toConfigValue(): String? {
        val rawValue = when (this) {
            null -> null
            is Number -> {
                val doubleValue = toDouble()
                if (doubleValue % 1.0 == 0.0) toLong().toString() else toString()
            }
            is Boolean -> toString()
            else -> toString()
        }
        return rawValue?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun String.redactIfSensitive(value: String): String {
        val normalized = lowercase()
        val sensitive = listOf("password", "secret", "token", "key").any { normalized.contains(it) }
        return if (sensitive) "[redacted]" else value
    }

    private fun String.configSortWeight(): Int {
        return when (this) {
            "name" -> 0
            "ostype" -> 1
            "memory" -> 2
            "cores" -> 3
            "sockets" -> 4
            "cpu" -> 5
            "bios" -> 6
            "boot" -> 7
            else -> 100
        }
    }

    private fun Exception.toVmErrorMessage(): String {
        return when (this) {
            is NotAuthenticatedException -> "Not authenticated"
            is retrofit2.HttpException -> when (code()) {
                401 -> "Authentication required - please login again"
                403 -> "Access forbidden - check permissions"
                404 -> "Node or VM not found"
                500 -> "Server error - please try again"
                else -> "Proxmox API error: HTTP ${code()}"
            }
            is SSLHandshakeException -> "TLS certificate validation failed"
            is UnknownHostException -> "Host not found"
            is SocketTimeoutException -> "Connection timed out"
            is IllegalArgumentException -> message ?: "Invalid VM request"
            else -> message ?: "VM request failed"
        }
    }
}

private const val CURRENT_SNAPSHOT_NAME = "current"

interface VmApi {
    suspend fun getVirtualMachines(nodeName: String): ApiResponse<List<VirtualMachine>>

    suspend fun getVMStatus(
        nodeName: String,
        vmid: Int
    ): ApiResponse<VirtualMachine>

    suspend fun getVMConfig(
        nodeName: String,
        vmid: Int
    ): ApiResponse<Map<String, Any?>>

    suspend fun performVMAction(
        nodeName: String,
        vmid: Int,
        action: String
    ): ApiResponse<String>

    suspend fun deleteVM(
        nodeName: String,
        vmid: Int
    ): ApiResponse<String>

    suspend fun getVMSnapshots(
        nodeName: String,
        vmid: Int
    ): ApiResponse<List<VmSnapshot>>
}

class ProxmoxVmApi(
    private val apiServiceProvider: () -> ProxmoxApiService?
) : VmApi {
    override suspend fun getVirtualMachines(nodeName: String): ApiResponse<List<VirtualMachine>> {
        return apiService().getVirtualMachines(nodeName)
    }

    override suspend fun getVMStatus(
        nodeName: String,
        vmid: Int
    ): ApiResponse<VirtualMachine> {
        return apiService().getVMStatus(nodeName, vmid)
    }

    override suspend fun getVMConfig(
        nodeName: String,
        vmid: Int
    ): ApiResponse<Map<String, Any?>> {
        return apiService().getVMConfig(nodeName, vmid)
    }

    override suspend fun performVMAction(
        nodeName: String,
        vmid: Int,
        action: String
    ): ApiResponse<String> {
        return apiService().performVMAction(nodeName, vmid, action)
    }

    override suspend fun deleteVM(
        nodeName: String,
        vmid: Int
    ): ApiResponse<String> {
        return apiService().deleteVM(nodeName, vmid)
    }

    override suspend fun getVMSnapshots(
        nodeName: String,
        vmid: Int
    ): ApiResponse<List<VmSnapshot>> {
        return apiService().getVMSnapshots(nodeName, vmid)
    }

    private fun apiService(): ProxmoxApiService {
        return apiServiceProvider() ?: throw NotAuthenticatedException()
    }
}

sealed class VmResult<out T> {
    data class Success<T>(val data: T) : VmResult<T>()
    data class Error(val message: String) : VmResult<Nothing>()
}

data class VmActionResult(
    val vmid: Int,
    val action: VmPowerAction,
    val taskId: String?
)

data class VmDetail(
    val nodeName: String,
    val virtualMachine: VirtualMachine
)

data class VmConfigEntry(
    val key: String,
    val value: String
)

enum class VmPowerAction(val apiValue: String) {
    Start("start"),
    Shutdown("shutdown"),
    Stop("stop"),
    Reboot("reboot"),
    Delete("delete")
}

class NotAuthenticatedException : IllegalStateException("Not authenticated")
