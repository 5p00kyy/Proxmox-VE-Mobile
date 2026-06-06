package com.proxmoxmobile.data.security

import org.junit.Assert.assertEquals
import org.junit.Test

class TlsPolicyTest {
    @Test
    fun requireInsecureTlsAllowed_allowsDebugBuilds() {
        TlsPolicy.requireInsecureTlsAllowed(isDebugBuild = true)
    }

    @Test
    fun requireInsecureTlsAllowed_rejectsReleaseBuilds() {
        val exception = kotlin.runCatching {
            TlsPolicy.requireInsecureTlsAllowed(isDebugBuild = false)
        }.exceptionOrNull()

        assertEquals(TlsPolicy.RELEASE_INSECURE_TLS_MESSAGE, exception?.message)
    }
}
