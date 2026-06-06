package com.proxmoxmobile.data.security

import com.proxmoxmobile.data.model.ServerConfig

object CredentialAuthMethod {
    const val PASSWORD = "password"
    const val API_TOKEN = "api_token"
}

data class CredentialSaveRequest(
    val serverConfig: ServerConfig,
    val password: String?,
    val saveCredentials: Boolean,
    val authMethod: String = CredentialAuthMethod.PASSWORD,
    val apiTokenId: String? = null,
    val apiTokenSecret: String? = null
)

data class SavedCredentials(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val realm: String,
    val useHttps: Boolean,
    val verifySsl: Boolean,
    val certificateFingerprint: String,
    val authMethod: String,
    val apiTokenId: String,
    val apiTokenSecret: String
)

interface CredentialStore {
    fun saveCredentials(request: CredentialSaveRequest)
    fun loadSavedCredentials(): SavedCredentials?
    fun clearSavedCredentials()
    fun hasSavedCredentials(): Boolean
}
