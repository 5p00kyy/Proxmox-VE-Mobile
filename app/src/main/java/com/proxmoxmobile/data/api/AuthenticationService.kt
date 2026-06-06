package com.proxmoxmobile.data.api

import android.util.Log
import com.proxmoxmobile.data.model.LoginRequest
import com.proxmoxmobile.data.model.LoginResponse
import com.proxmoxmobile.data.model.ServerConfig
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

interface ProxmoxAuthenticationService {
    suspend fun authenticate(serverConfig: ServerConfig): Result<LoginResponse>
    fun toUserFacingException(e: Exception): Exception
}

class AuthenticationService(
    private val apiFactory: ProxmoxApiServiceFactory = ProxmoxApiFactory()
) : ProxmoxAuthenticationService {
    
    companion object {
        private const val TAG = "AuthenticationService"
        private const val TLS_CERTIFICATE_FAILURE_MESSAGE =
            "TLS certificate validation failed. Use Android's trusted CA store, set the server certificate SHA-256 fingerprint, or disable SSL verification only in debug builds for a trusted lab server."
    }

    fun createApiService(serverConfig: ServerConfig): ProxmoxApiService {
        return apiFactory.createApiService(serverConfig, ProxmoxAuth.None)
    }
    
    override suspend fun authenticate(serverConfig: ServerConfig): Result<LoginResponse> {
        return try {
            Log.d(TAG, "Starting authentication request")
            
            validatePasswordConfig(serverConfig)
            
            Log.d(TAG, "Creating API service and attempting login")
            
            val apiService = createApiService(serverConfig)
            val loginRequest = LoginRequest(
                username = serverConfig.username,
                password = serverConfig.password.orEmpty(),
                realm = serverConfig.realm
            )
            
            Log.d(TAG, "Sending login request")
            val response = apiService.login(loginRequest)
            
            if (response.data.ticket.isBlank()) {
                throw IllegalStateException("Received empty authentication ticket")
            }
            
            Log.d(TAG, "Login successful")
            
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Authentication failed with exception", e)
            Result.failure(toUserFacingException(e))
        }
    }

    override fun toUserFacingException(e: Exception): Exception {
        return when (e) {
            is retrofit2.HttpException -> when (e.code()) {
                401 -> Exception("Invalid username, password, API token, or TFA challenge")
                403 -> Exception("Access forbidden - check user permissions")
                500 -> Exception("Server error - please try again")
                else -> Exception("Authentication failed: HTTP ${e.code()}")
            }
            is IllegalArgumentException -> e
            is IllegalStateException -> e
            is SSLHandshakeException -> Exception(TLS_CERTIFICATE_FAILURE_MESSAGE)
            is UnknownHostException -> Exception("Host not found")
            is SocketTimeoutException -> Exception("Connection timed out")
            else -> Exception(e.message ?: "Authentication failed")
        }
    }

    private fun validatePasswordConfig(serverConfig: ServerConfig) {
        require(serverConfig.host.isNotBlank()) { "Host cannot be empty" }
        require(serverConfig.username.isNotBlank()) { "Username cannot be empty" }
        require(!serverConfig.password.isNullOrBlank()) { "Password cannot be empty" }
    }
}
