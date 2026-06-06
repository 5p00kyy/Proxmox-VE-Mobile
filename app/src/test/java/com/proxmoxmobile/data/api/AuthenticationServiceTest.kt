package com.proxmoxmobile.data.api

import javax.net.ssl.SSLHandshakeException
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticationServiceTest {
    @Test
    fun toUserFacingException_forTlsFailureMentionsSecureSelfSignedOptions() {
        val exception = AuthenticationService()
            .toUserFacingException(SSLHandshakeException("certificate rejected"))

        val message = exception.message.orEmpty()
        assertTrue(message.contains("trusted CA store"))
        assertTrue(message.contains("SHA-256 fingerprint"))
        assertTrue(message.contains("debug builds"))
    }
}
