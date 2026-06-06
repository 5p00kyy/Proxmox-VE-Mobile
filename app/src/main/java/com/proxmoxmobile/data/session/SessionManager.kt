package com.proxmoxmobile.data.session

import com.proxmoxmobile.data.api.AuthenticationService
import com.proxmoxmobile.data.api.ProxmoxApiFactory
import com.proxmoxmobile.data.api.ProxmoxApiService
import com.proxmoxmobile.data.api.ProxmoxApiServiceFactory
import com.proxmoxmobile.data.api.ProxmoxAuthenticationService
import com.proxmoxmobile.data.api.ProxmoxAuth
import com.proxmoxmobile.data.model.LoginResponse
import com.proxmoxmobile.data.model.ServerConfig

interface AuthSessionService {
    suspend fun authenticate(serverConfig: ServerConfig): Result<AuthenticatedSession>
    fun createApiService(): ProxmoxApiService?
    fun currentSession(): AuthenticatedSession?
    fun logout()
}

class SessionManager(
    private val apiFactory: ProxmoxApiServiceFactory = ProxmoxApiFactory(),
    private val authenticationService: ProxmoxAuthenticationService = AuthenticationService(apiFactory)
) : AuthSessionService {
    private var activeSession: AuthenticatedSession? = null

    override suspend fun authenticate(serverConfig: ServerConfig): Result<AuthenticatedSession> {
        val apiToken = serverConfig.apiToken?.takeIf { it.isNotBlank() }

        return if (apiToken != null) {
            authenticateWithApiToken(serverConfig, apiToken)
        } else {
            try {
                authenticationService.authenticate(serverConfig).map { loginResponse ->
                    loginResponse.toSession(serverConfig)
                }
            } catch (e: Exception) {
                Result.failure(authenticationService.toUserFacingException(e))
            }.onFailure {
                activeSession = null
            }.onSuccess { session ->
                activeSession = session
            }
        }
    }

    override fun createApiService(): ProxmoxApiService? {
        val session = activeSession ?: return null
        return apiFactory.createApiService(session.serverConfig, session.auth)
    }

    override fun currentSession(): AuthenticatedSession? = activeSession

    override fun logout() {
        activeSession = null
    }

    private suspend fun authenticateWithApiToken(
        serverConfig: ServerConfig,
        apiToken: String
    ): Result<AuthenticatedSession> {
        return try {
            validateApiTokenConfig(serverConfig, apiToken)
            val auth = ProxmoxAuth.ApiToken(apiToken)
            apiFactory.createApiService(serverConfig, auth).getVersion()

            val session = AuthenticatedSession(
                serverConfig = serverConfig.withoutSecrets(),
                auth = auth,
                authToken = null,
                csrfToken = null,
                username = serverConfig.username
            )
            activeSession = session
            Result.success(session)
        } catch (e: Exception) {
            activeSession = null
            Result.failure(authenticationService.toUserFacingException(e))
        }
    }

    private fun validateApiTokenConfig(serverConfig: ServerConfig, apiToken: String) {
        require(serverConfig.host.isNotBlank()) { "Host cannot be empty" }
        require(serverConfig.username.isNotBlank()) { "Username cannot be empty" }
        require(apiToken.isNotBlank()) { "API token cannot be empty" }
    }

    private fun LoginResponse.toSession(serverConfig: ServerConfig): AuthenticatedSession {
        return AuthenticatedSession(
            serverConfig = serverConfig.withoutSecrets(),
            auth = ProxmoxAuth.Ticket(
                ticket = data.ticket,
                csrfToken = data.csrfToken
            ),
            authToken = data.ticket,
            csrfToken = data.csrfToken,
            username = data.username
        )
    }

    private fun ServerConfig.withoutSecrets(): ServerConfig {
        return copy(password = null, apiToken = null)
    }
}

data class AuthenticatedSession(
    val serverConfig: ServerConfig,
    val auth: ProxmoxAuth,
    val authToken: String?,
    val csrfToken: String?,
    val username: String
)
