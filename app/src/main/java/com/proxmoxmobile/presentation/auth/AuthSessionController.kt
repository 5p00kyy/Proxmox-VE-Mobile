package com.proxmoxmobile.presentation.auth

import com.proxmoxmobile.data.api.ProxmoxApiService
import com.proxmoxmobile.data.model.ServerConfig
import com.proxmoxmobile.data.security.ServerCertificateInfo
import com.proxmoxmobile.data.security.UntrustedServerCertificateException
import com.proxmoxmobile.data.session.AuthSessionService
import com.proxmoxmobile.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthSessionController(
    private val sessionManager: AuthSessionService = SessionManager()
) {
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentServer = MutableStateFlow<ServerConfig?>(null)
    val currentServer: StateFlow<ServerConfig?> = _currentServer.asStateFlow()

    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    private val _csrfToken = MutableStateFlow<String?>(null)
    val csrfToken: StateFlow<String?> = _csrfToken.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _untrustedServerCertificate = MutableStateFlow<ServerCertificateInfo?>(null)
    val untrustedServerCertificate: StateFlow<ServerCertificateInfo?> = _untrustedServerCertificate.asStateFlow()

    suspend fun authenticate(serverConfig: ServerConfig): Boolean {
        _isLoading.value = true
        _errorMessage.value = null
        _untrustedServerCertificate.value = null

        try {
            val result = sessionManager.authenticate(serverConfig)
            return result.fold(
                onSuccess = { session ->
                    _authToken.value = session.authToken
                    _csrfToken.value = session.csrfToken
                    _currentServer.value = session.serverConfig
                    _isAuthenticated.value = true
                    _errorMessage.value = "✅ Authentication successful!"
                    true
                },
                onFailure = { exception ->
                    clearSessionState()
                    if (exception is UntrustedServerCertificateException) {
                        _untrustedServerCertificate.value = exception.certificateInfo
                    }
                    _errorMessage.value = "❌ ${exception.message}"
                    sessionManager.logout()
                    false
                }
            )
        } catch (e: Exception) {
            clearSessionState()
            _errorMessage.value = "❌ Network error: ${e.message}"
            _untrustedServerCertificate.value = null
            sessionManager.logout()
            return false
        } finally {
            _isLoading.value = false
        }
    }

    fun createApiService(): ProxmoxApiService? {
        val session = sessionManager.currentSession() ?: return null
        if (session.serverConfig.host.isBlank()) return null

        return try {
            sessionManager.createApiService()
        } catch (_: Exception) {
            null
        }
    }

    fun setAuthenticated(authenticated: Boolean) {
        _isAuthenticated.value = authenticated
    }

    fun setCurrentServer(server: ServerConfig?) {
        _currentServer.value = server
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearUntrustedServerCertificate() {
        _untrustedServerCertificate.value = null
    }

    fun logout() {
        clearSessionState()
        _currentServer.value = null
        _errorMessage.value = null
        _untrustedServerCertificate.value = null
        sessionManager.logout()
    }

    private fun clearSessionState() {
        _isAuthenticated.value = false
        _authToken.value = null
        _csrfToken.value = null
    }
}
