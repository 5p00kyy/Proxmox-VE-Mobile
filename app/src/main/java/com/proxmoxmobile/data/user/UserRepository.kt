package com.proxmoxmobile.data.user

import com.proxmoxmobile.data.api.ProxmoxApiService
import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.User
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class UserRepository(
    private val api: UserApi
) {
    suspend fun getUsers(): UserResult<List<User>> {
        return runUserRequest {
            api.getUsers()
                .data
                .filter { it.userid.isNotBlank() }
                .sortedBy { it.userid.lowercase() }
        }
    }

    private suspend fun <T> runUserRequest(block: suspend () -> T): UserResult<T> {
        return try {
            UserResult.Success(block())
        } catch (e: Exception) {
            UserResult.Error(e.toUserErrorMessage())
        }
    }

    private fun Exception.toUserErrorMessage(): String {
        return when (this) {
            is UserNotAuthenticatedException -> "Not authenticated"
            is retrofit2.HttpException -> when (code()) {
                401 -> "Authentication required - please login again"
                403 -> "Access forbidden - check permissions"
                404 -> "User endpoint not found"
                500 -> "Server error - please try again"
                else -> "Proxmox API error: HTTP ${code()}"
            }
            is SSLHandshakeException -> "TLS certificate validation failed"
            is UnknownHostException -> "Host not found"
            is SocketTimeoutException -> "Connection timed out"
            is IllegalArgumentException -> message ?: "Invalid user request"
            else -> message ?: "User request failed"
        }
    }
}

interface UserApi {
    suspend fun getUsers(): ApiResponse<List<User>>
}

class ProxmoxUserApi(
    private val apiServiceProvider: () -> ProxmoxApiService?
) : UserApi {
    override suspend fun getUsers(): ApiResponse<List<User>> {
        return apiService().getUsers()
    }

    private fun apiService(): ProxmoxApiService {
        return apiServiceProvider() ?: throw UserNotAuthenticatedException()
    }
}

sealed class UserResult<out T> {
    data class Success<T>(val data: T) : UserResult<T>()
    data class Error(val message: String) : UserResult<Nothing>()
}

class UserNotAuthenticatedException : IllegalStateException("Not authenticated")
