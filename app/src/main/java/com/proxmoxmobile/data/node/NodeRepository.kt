package com.proxmoxmobile.data.node

import com.proxmoxmobile.data.api.ProxmoxApiService
import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.NodeStatus
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class NodeRepository(
    private val api: NodeApi
) {
    suspend fun getNodeDetail(nodeName: String): NodeResult<NodeDetail> {
        return runNodeRequest {
            val normalizedNodeName = nodeName.trim()
            require(normalizedNodeName.isNotBlank()) { "Node name is required" }

            val status = api.getNodeStatus(normalizedNodeName).data
            require(status.isValid()) { "Node status payload is invalid" }

            NodeDetail(
                nodeName = normalizedNodeName,
                status = status
            )
        }
    }

    private suspend fun <T> runNodeRequest(block: suspend () -> T): NodeResult<T> {
        return try {
            NodeResult.Success(block())
        } catch (e: Exception) {
            NodeResult.Error(e.toNodeErrorMessage())
        }
    }

    private fun NodeStatus.isValid(): Boolean {
        return status.isNotBlank() &&
            cpu >= 0 &&
            maxcpu >= 0 &&
            mem >= 0 &&
            maxmem >= 0 &&
            uptime >= 0 &&
            loadavg.all { it >= 0 } &&
            rootfs.total >= 0 &&
            rootfs.used >= 0 &&
            swap.total >= 0 &&
            swap.used >= 0
    }

    private fun Exception.toNodeErrorMessage(): String {
        return when (this) {
            is NodeNotAuthenticatedException -> "Not authenticated"
            is retrofit2.HttpException -> when (code()) {
                401 -> "Authentication required - please login again"
                403 -> "Access forbidden - check permissions"
                404 -> "Node not found"
                500 -> "Server error - please try again"
                else -> "Proxmox API error: HTTP ${code()}"
            }
            is SSLHandshakeException -> "TLS certificate validation failed"
            is UnknownHostException -> "Host not found"
            is SocketTimeoutException -> "Connection timed out"
            is IllegalArgumentException -> message ?: "Invalid node request"
            else -> message ?: "Node request failed"
        }
    }
}

interface NodeApi {
    suspend fun getNodeStatus(nodeName: String): ApiResponse<NodeStatus>
}

class ProxmoxNodeApi(
    private val apiServiceProvider: () -> ProxmoxApiService?
) : NodeApi {
    override suspend fun getNodeStatus(nodeName: String): ApiResponse<NodeStatus> {
        return apiService().getNodeStatus(nodeName)
    }

    private fun apiService(): ProxmoxApiService {
        return apiServiceProvider() ?: throw NodeNotAuthenticatedException()
    }
}

sealed class NodeResult<out T> {
    data class Success<T>(val data: T) : NodeResult<T>()
    data class Error(val message: String) : NodeResult<Nothing>()
}

data class NodeDetail(
    val nodeName: String,
    val status: NodeStatus
)

class NodeNotAuthenticatedException : IllegalStateException("Not authenticated")
