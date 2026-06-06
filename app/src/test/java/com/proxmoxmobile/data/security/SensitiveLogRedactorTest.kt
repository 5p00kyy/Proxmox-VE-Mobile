package com.proxmoxmobile.data.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveLogRedactorTest {
    @Test
    fun redact_removesAuthHeadersAndInlineSecrets() {
        val message = listOf(
            "Cookie: PVEAuthCookie=ticket-value;",
            "Authorization: PVEAPIToken=user@realm!token-id=token-secret",
            "CSRFPreventionToken: csrf-secret",
            "password=plain-secret",
            "token: another-secret"
        ).joinToString(" ")

        val redacted = SensitiveLogRedactor.redact(message)

        assertTrue(redacted.contains("PVEAuthCookie=<redacted>"))
        assertTrue(redacted.contains("PVEAPIToken=<redacted>"))
        assertTrue(redacted.contains("CSRFPreventionToken=<redacted>"))
        assertTrue(redacted.contains("password=<redacted>"))
        assertTrue(redacted.contains("token=<redacted>"))
        assertFalse(redacted.contains("ticket-value"))
        assertFalse(redacted.contains("token-secret"))
        assertFalse(redacted.contains("csrf-secret"))
        assertFalse(redacted.contains("plain-secret"))
        assertFalse(redacted.contains("another-secret"))
    }
}
