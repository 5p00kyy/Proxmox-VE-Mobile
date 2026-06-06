package com.proxmoxmobile.data.security

object TlsPolicy {
    const val RELEASE_INSECURE_TLS_MESSAGE =
        "Disabling SSL verification is only available in debug builds. Use platform TLS or configure the server certificate SHA-256 fingerprint."

    fun requireInsecureTlsAllowed(isDebugBuild: Boolean) {
        require(isDebugBuild) { RELEASE_INSECURE_TLS_MESSAGE }
    }
}
