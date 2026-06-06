package com.proxmoxmobile.data.lxc

import com.proxmoxmobile.data.api.ProxmoxApiService
import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.Container
import com.proxmoxmobile.data.model.LxcSnapshot
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class LxcRepository(
    private val api: LxcApi
) {
    suspend fun getContainers(nodeName: String): LxcResult<List<Container>> {
        return runLxcRequest {
            require(nodeName.isNotBlank()) { "Node name is required" }
            api.getContainers(nodeName)
                .data
                .filter { it.isValid() }
                .sortedBy { it.vmid }
        }
    }

    suspend fun getContainerDetail(
        nodeNames: List<String>,
        vmid: Int,
        preferredNodeName: String? = null
    ): LxcResult<LxcDetail> {
        return runLxcRequest {
            require(vmid > 0) { "Container ID is required" }
            val candidateNodes = (
                listOfNotNull(preferredNodeName?.trim()?.takeIf { it.isNotBlank() }) +
                    nodeNames.map { it.trim() }.filter { it.isNotBlank() }
                )
                .distinct()
            require(candidateNodes.isNotEmpty()) { "At least one node is required" }

            for (nodeName in candidateNodes) {
                try {
                    val container = api.getContainerStatus(nodeName, vmid).data
                    if (container.isValid()) {
                        return@runLxcRequest LxcDetail(
                            nodeName = nodeName,
                            container = container
                        )
                    }
                } catch (e: LxcNotAuthenticatedException) {
                    throw e
                } catch (_: Exception) {
                    // Continue searching other cached nodes; the container may live elsewhere.
                }
            }

            throw IllegalArgumentException("Container not found")
        }
    }

    suspend fun performAction(
        nodeName: String,
        vmid: Int,
        action: LxcPowerAction
    ): LxcResult<LxcActionResult> {
        return runLxcRequest {
            require(nodeName.isNotBlank()) { "Node name is required" }
            require(vmid > 0) { "Container ID is required" }

            val response = api.performContainerAction(nodeName, vmid, action.apiValue)
            LxcActionResult(
                vmid = vmid,
                action = action,
                taskId = response.data.takeIf { it.isNotBlank() }
            )
        }
    }

    suspend fun deleteContainer(
        nodeName: String,
        vmid: Int
    ): LxcResult<LxcActionResult> {
        return runLxcRequest {
            require(nodeName.isNotBlank()) { "Node name is required" }
            require(vmid > 0) { "Container ID is required" }

            val response = api.deleteContainer(nodeName, vmid)
            LxcActionResult(
                vmid = vmid,
                action = LxcPowerAction.Delete,
                taskId = response.data.takeIf { it.isNotBlank() }
            )
        }
    }

    suspend fun getSnapshots(
        nodeName: String,
        vmid: Int
    ): LxcResult<List<LxcSnapshot>> {
        return runLxcRequest {
            require(nodeName.isNotBlank()) { "Node name is required" }
            require(vmid > 0) { "Container ID is required" }

            api.getLXCSnapshots(nodeName, vmid)
                .data
                .filter { it.isValid() }
                .sortedWith(
                    compareByDescending<LxcSnapshot> { it.name == CURRENT_SNAPSHOT_NAME }
                        .thenByDescending { it.snaptime ?: 0L }
                        .thenBy { it.name }
                )
        }
    }

    private suspend fun <T> runLxcRequest(block: suspend () -> T): LxcResult<T> {
        return try {
            LxcResult.Success(block())
        } catch (e: Exception) {
            LxcResult.Error(e.toLxcErrorMessage())
        }
    }

    private fun Container.isValid(): Boolean {
        return runCatching {
            vmid > 0 &&
                name.isNotBlank() &&
                status.isNotBlank() &&
                cpu >= 0 &&
                mem >= 0 &&
                maxmem >= 0 &&
                uptime >= 0
        }.getOrDefault(false)
    }

    private fun LxcSnapshot.isValid(): Boolean {
        return runCatching {
            name.isNotBlank() &&
                (snaptime == null || snaptime >= 0)
        }.getOrDefault(false)
    }

    private fun Exception.toLxcErrorMessage(): String {
        return when (this) {
            is LxcNotAuthenticatedException -> "Not authenticated"
            is retrofit2.HttpException -> when (code()) {
                401 -> "Authentication required - please login again"
                403 -> "Access forbidden - check permissions"
                404 -> "Node or container not found"
                500 -> "Server error - please try again"
                else -> "Proxmox API error: HTTP ${code()}"
            }
            is SSLHandshakeException -> "TLS certificate validation failed"
            is UnknownHostException -> "Host not found"
            is SocketTimeoutException -> "Connection timed out"
            is IllegalArgumentException -> message ?: "Invalid container request"
            else -> message ?: "Container request failed"
        }
    }
}

private const val CURRENT_SNAPSHOT_NAME = "current"

interface LxcApi {
    suspend fun getContainers(nodeName: String): ApiResponse<List<Container>>

    suspend fun getContainerStatus(
        nodeName: String,
        vmid: Int
    ): ApiResponse<Container>

    suspend fun performContainerAction(
        nodeName: String,
        vmid: Int,
        action: String
    ): ApiResponse<String>

    suspend fun deleteContainer(
        nodeName: String,
        vmid: Int
    ): ApiResponse<String>

    suspend fun getLXCSnapshots(
        nodeName: String,
        vmid: Int
    ): ApiResponse<List<LxcSnapshot>>
}

class ProxmoxLxcApi(
    private val apiServiceProvider: () -> ProxmoxApiService?
) : LxcApi {
    override suspend fun getContainers(nodeName: String): ApiResponse<List<Container>> {
        return apiService().getContainers(nodeName)
    }

    override suspend fun getContainerStatus(
        nodeName: String,
        vmid: Int
    ): ApiResponse<Container> {
        return apiService().getContainerStatus(nodeName, vmid)
    }

    override suspend fun performContainerAction(
        nodeName: String,
        vmid: Int,
        action: String
    ): ApiResponse<String> {
        return apiService().performContainerAction(nodeName, vmid, action)
    }

    override suspend fun deleteContainer(
        nodeName: String,
        vmid: Int
    ): ApiResponse<String> {
        return apiService().deleteContainer(nodeName, vmid)
    }

    override suspend fun getLXCSnapshots(
        nodeName: String,
        vmid: Int
    ): ApiResponse<List<LxcSnapshot>> {
        return apiService().getLXCSnapshots(nodeName, vmid)
    }

    private fun apiService(): ProxmoxApiService {
        return apiServiceProvider() ?: throw LxcNotAuthenticatedException()
    }
}

sealed class LxcResult<out T> {
    data class Success<T>(val data: T) : LxcResult<T>()
    data class Error(val message: String) : LxcResult<Nothing>()
}

data class LxcActionResult(
    val vmid: Int,
    val action: LxcPowerAction,
    val taskId: String?
)

data class LxcDetail(
    val nodeName: String,
    val container: Container
)

enum class LxcPowerAction(val apiValue: String) {
    Start("start"),
    Shutdown("shutdown"),
    Stop("stop"),
    Reboot("reboot"),
    Delete("delete")
}

class LxcNotAuthenticatedException : IllegalStateException("Not authenticated")
