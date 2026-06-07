package com.proxmoxmobile.data.security

import com.proxmoxmobile.data.model.ServerConfig
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertSame
import org.junit.Test

class ServerCertificateProbeTest {
    @Test
    fun toTrustPromptException_doesNotOfferTrustWhenFingerprintIsAlreadyPinned() = runBlocking {
        val fallback = Exception("TLS certificate validation failed")

        val mapped = ServerCertificateProbe.toTrustPromptException(
            exception = SSLHandshakeException("Server certificate fingerprint did not match the configured pin"),
            serverConfig = ServerConfig(
                host = "example.test",
                port = 8006,
                username = "tester",
                password = "secret",
                useHttps = true,
                verifySsl = true,
                certificateFingerprint = "00112233445566778899AABBCCDDEEFF00112233445566778899AABBCCDDEEFF"
            ),
            fallback = fallback
        )

        assertSame(fallback, mapped)
    }
}
