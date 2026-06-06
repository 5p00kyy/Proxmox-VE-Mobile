package com.proxmoxmobile.data.network

import com.proxmoxmobile.data.api.ProxmoxApiService
import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.NetworkInterface
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class NetworkRepository(
    private val api: NetworkApi
) {
    suspend fun getNetworkInterfaces(nodeName: String): NetworkResult<List<NetworkInterface>> {
        return runNetworkRequest {
            require(nodeName.isNotBlank()) { "Node name is required" }
            api.getNetworkInterfaces(nodeName)
                .data
                .filter { it.isValid() }
                .sortedBy { it.iface }
        }
    }

    private suspend fun <T> runNetworkRequest(block: suspend () -> T): NetworkResult<T> {
        return try {
            NetworkResult.Success(block())
        } catch (e: Exception) {
            NetworkResult.Error(e.toNetworkErrorMessage())
        }
    }

    private fun NetworkInterface.isValid(): Boolean {
        return iface.isNotBlank() && type.isNotBlank()
    }

    private fun Exception.toNetworkErrorMessage(): String {
        return when (this) {
            is NetworkNotAuthenticatedException -> "Not authenticated"
            is retrofit2.HttpException -> when (code()) {
                401 -> "Authentication required - please login again"
                403 -> "Access forbidden - check permissions"
                404 -> "Node or network interface not found"
                500 -> "Server error - please try again"
                else -> "Proxmox API error: HTTP ${code()}"
            }
            is SSLHandshakeException -> "TLS certificate validation failed"
            is UnknownHostException -> "Host not found"
            is SocketTimeoutException -> "Connection timed out"
            is IllegalArgumentException -> message ?: "Invalid network request"
            else -> message ?: "Network request failed"
        }
    }
}

interface NetworkApi {
    suspend fun getNetworkInterfaces(nodeName: String): ApiResponse<List<NetworkInterface>>
}

class ProxmoxNetworkApi(
    private val apiServiceProvider: () -> ProxmoxApiService?
) : NetworkApi {
    override suspend fun getNetworkInterfaces(nodeName: String): ApiResponse<List<NetworkInterface>> {
        return apiService().getNetworkInterfaces(nodeName)
    }

    private fun apiService(): ProxmoxApiService {
        return apiServiceProvider() ?: throw NetworkNotAuthenticatedException()
    }
}

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String) : NetworkResult<Nothing>()
}

class NetworkNotAuthenticatedException : IllegalStateException("Not authenticated")
