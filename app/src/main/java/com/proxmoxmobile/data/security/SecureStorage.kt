package com.proxmoxmobile.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(private val context: Context) : CredentialStore {
    
    companion object {
        private const val MASTER_KEY_ALIAS = "proxmox_master_key"
        private const val PREFS_NAME = "proxmox_secure_prefs"
        private const val KEY_HOST = "host"
        private const val KEY_PORT = "port"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_REALM = "realm"
        private const val KEY_USE_HTTPS = "use_https"
        private const val KEY_VERIFY_SSL = "verify_ssl"
        private const val KEY_CERTIFICATE_FINGERPRINT = "certificate_fingerprint"
        private const val KEY_SAVE_CREDENTIALS = "save_credentials"
        private const val KEY_AUTH_METHOD = "auth_method"
        private const val KEY_API_TOKEN_ID = "api_token_id"
        private const val KEY_API_TOKEN_SECRET = "api_token_secret"
    }
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false)
            .build()
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    override fun saveCredentials(request: CredentialSaveRequest) {
        val serverConfig = request.serverConfig
        encryptedPrefs.edit().apply {
            putString(KEY_HOST, serverConfig.host)
            putInt(KEY_PORT, serverConfig.port)
            putString(KEY_USERNAME, serverConfig.username)
            putString(KEY_PASSWORD, request.password.orEmpty())
            putString(KEY_REALM, serverConfig.realm)
            putBoolean(KEY_USE_HTTPS, serverConfig.useHttps)
            putBoolean(KEY_VERIFY_SSL, serverConfig.verifySsl)
            putString(KEY_CERTIFICATE_FINGERPRINT, serverConfig.certificateFingerprint.orEmpty())
            putBoolean(KEY_SAVE_CREDENTIALS, request.saveCredentials)
            putString(KEY_AUTH_METHOD, request.authMethod)
            putString(KEY_API_TOKEN_ID, request.apiTokenId.orEmpty())
            putString(KEY_API_TOKEN_SECRET, request.apiTokenSecret.orEmpty())
            apply()
        }
    }
    
    override fun loadSavedCredentials(): SavedCredentials? {
        val saveCredentials = encryptedPrefs.getBoolean(KEY_SAVE_CREDENTIALS, false)
        if (!saveCredentials) return null
        
        val host = encryptedPrefs.getString(KEY_HOST, "")
        val port = encryptedPrefs.getInt(KEY_PORT, 8006)
        val username = encryptedPrefs.getString(KEY_USERNAME, "")
        val password = encryptedPrefs.getString(KEY_PASSWORD, "")
        val realm = encryptedPrefs.getString(KEY_REALM, "pam")
        val useHttps = encryptedPrefs.getBoolean(KEY_USE_HTTPS, true)
        val verifySsl = encryptedPrefs.getBoolean(KEY_VERIFY_SSL, true)
        val certificateFingerprint = encryptedPrefs.getString(KEY_CERTIFICATE_FINGERPRINT, "")
        val authMethod = encryptedPrefs.getString(KEY_AUTH_METHOD, CredentialAuthMethod.PASSWORD)
            ?: CredentialAuthMethod.PASSWORD
        val apiTokenId = encryptedPrefs.getString(KEY_API_TOKEN_ID, "")
        val apiTokenSecret = encryptedPrefs.getString(KEY_API_TOKEN_SECRET, "")
        
        val hasPasswordCredentials = authMethod == CredentialAuthMethod.PASSWORD && password?.isNotBlank() == true
        val hasApiTokenCredentials = authMethod == CredentialAuthMethod.API_TOKEN &&
            apiTokenId?.isNotBlank() == true &&
            apiTokenSecret?.isNotBlank() == true

        if (host?.isNotBlank() == true && username?.isNotBlank() == true && (hasPasswordCredentials || hasApiTokenCredentials)) {
            return SavedCredentials(
                host = host,
                port = port,
                username = username,
                password = password.orEmpty(),
                realm = realm ?: "pam",
                useHttps = useHttps,
                verifySsl = verifySsl,
                certificateFingerprint = certificateFingerprint.orEmpty(),
                authMethod = authMethod,
                apiTokenId = apiTokenId.orEmpty(),
                apiTokenSecret = apiTokenSecret.orEmpty()
            )
        }
        return null
    }
    
    override fun clearSavedCredentials() {
        encryptedPrefs.edit().clear().apply()
    }
    
    override fun hasSavedCredentials(): Boolean {
        return encryptedPrefs.getBoolean(KEY_SAVE_CREDENTIALS, false)
    }
}
