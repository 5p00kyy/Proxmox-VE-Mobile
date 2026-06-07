package com.proxmoxmobile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proxmoxmobile.data.api.ProxmoxApiService
import com.proxmoxmobile.data.model.ServerConfig
import com.proxmoxmobile.data.security.CredentialAuthMethod
import com.proxmoxmobile.data.security.CredentialSaveRequest
import com.proxmoxmobile.data.security.CredentialStore
import com.proxmoxmobile.data.security.SavedCredentials
import com.proxmoxmobile.data.security.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import android.content.Context
import com.proxmoxmobile.data.model.Node
import com.proxmoxmobile.data.security.ServerCertificateInfo
import com.proxmoxmobile.presentation.auth.AuthSessionController

class MainViewModel(
    private val authSessionController: AuthSessionController = AuthSessionController()
) : ViewModel() {
    companion object {
        private const val TAG = "MainViewModel"
    }

    // In-memory cache for nodes
    private var cachedNodes: List<Node>? = null
    fun getCachedNodes(): List<Node>? = cachedNodes
    fun setCachedNodes(nodes: List<Node>) { cachedNodes = nodes }

    val isAuthenticated: StateFlow<Boolean> = authSessionController.isAuthenticated
    val currentServer: StateFlow<ServerConfig?> = authSessionController.currentServer
    val authToken: StateFlow<String?> = authSessionController.authToken
    val csrfToken: StateFlow<String?> = authSessionController.csrfToken
    val isLoading: StateFlow<Boolean> = authSessionController.isLoading
    val errorMessage: StateFlow<String?> = authSessionController.errorMessage
    val untrustedServerCertificate: StateFlow<ServerCertificateInfo?> = authSessionController.untrustedServerCertificate

    private val _showConfirmationDialog = MutableStateFlow<ConfirmationDialog?>(null)
    val showConfirmationDialog: StateFlow<ConfirmationDialog?> = _showConfirmationDialog.asStateFlow()

    private var credentialStore: CredentialStore? = null

    data class ConfirmationDialog(
        val title: String,
        val message: String,
        val onConfirm: () -> Unit,
        val onDismiss: () -> Unit = {}
    )

    fun showConfirmationDialog(dialog: ConfirmationDialog) {
        _showConfirmationDialog.value = dialog
    }

    fun hideConfirmationDialog() {
        _showConfirmationDialog.value = null
    }

    fun initialize(context: Context) {
        credentialStore = SecureStorage(context)
    }

    fun saveCredentials(
        serverConfig: ServerConfig,
        password: String?,
        saveCredentials: Boolean,
        authMethod: String = CredentialAuthMethod.PASSWORD,
        apiTokenId: String? = null,
        apiTokenSecret: String? = null
    ) {
        credentialStore?.saveCredentials(
            CredentialSaveRequest(
                serverConfig = serverConfig,
                password = password,
                saveCredentials = saveCredentials,
                authMethod = authMethod,
                apiTokenId = apiTokenId,
                apiTokenSecret = apiTokenSecret
            )
        )
    }

    fun loadSavedCredentials(): SavedCredentials? {
        return credentialStore?.loadSavedCredentials()
    }

    fun clearSavedCredentials() {
        credentialStore?.clearSavedCredentials()
    }

    fun hasSavedCredentials(): Boolean {
        return credentialStore?.hasSavedCredentials() ?: false
    }

    fun authenticate(serverConfig: ServerConfig, onSuccess: (() -> Unit)? = null) {
        Log.d(TAG, "Starting authentication process")
        viewModelScope.launch {
            if (authSessionController.authenticate(serverConfig)) {
                onSuccess?.invoke()
            }
        }
    }

    fun getApiService(): ProxmoxApiService? {
        return authSessionController.createApiService()
    }

    fun setAuthenticated(authenticated: Boolean) {
        authSessionController.setAuthenticated(authenticated)
    }

    fun setCurrentServer(server: ServerConfig?) {
        authSessionController.setCurrentServer(server)
    }

    fun setLoading(loading: Boolean) {
        authSessionController.setLoading(loading)
    }

    fun setErrorMessage(message: String?) {
        authSessionController.setErrorMessage(message)
    }

    fun clearError() {
        authSessionController.clearError()
    }

    fun clearUntrustedServerCertificate() {
        authSessionController.clearUntrustedServerCertificate()
    }

    fun logout() {
        Log.d(TAG, "Logging out user")
        viewModelScope.launch {
            authSessionController.logout()
        }
    }
}
