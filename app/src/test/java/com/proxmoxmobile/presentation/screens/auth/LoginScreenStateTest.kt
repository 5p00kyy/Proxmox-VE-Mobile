package com.proxmoxmobile.presentation.screens.auth

import com.proxmoxmobile.data.security.CredentialAuthMethod
import com.proxmoxmobile.data.security.SavedCredentials
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginScreenStateTest {
    @Test
    fun savedCredentialsMatchNonSecretLoginDraft_matchesPasswordDraftWithReleaseSslUpgrade() {
        val savedCredentials = savedCredentials(
            verifySsl = false
        )

        val matches = savedCredentialsMatchNonSecretLoginDraft(
            savedCredentials = savedCredentials,
            host = "demo.example.test",
            port = "8006",
            username = "tester",
            realm = "pam",
            useHttps = true,
            verifySsl = true,
            certificateFingerprint = "",
            useApiToken = false,
            apiTokenId = "",
            allowInsecureTls = false
        )

        assertTrue(matches)
    }

    @Test
    fun savedCredentialsMatchNonSecretLoginDraft_rejectsEditedHostBeforeReloadingSecret() {
        val matches = savedCredentialsMatchNonSecretLoginDraft(
            savedCredentials = savedCredentials(),
            host = "changed.example.test",
            port = "8006",
            username = "tester",
            realm = "pam",
            useHttps = true,
            verifySsl = true,
            certificateFingerprint = "",
            useApiToken = false,
            apiTokenId = "",
            allowInsecureTls = true
        )

        assertFalse(matches)
    }

    @Test
    fun savedCredentialsMatchNonSecretLoginDraft_requiresApiTokenIdForTokenDraft() {
        val savedCredentials = savedCredentials(
            authMethod = CredentialAuthMethod.API_TOKEN,
            apiTokenId = "mobile"
        )

        val matches = savedCredentialsMatchNonSecretLoginDraft(
            savedCredentials = savedCredentials,
            host = "demo.example.test",
            port = "8006",
            username = "tester",
            realm = "pam",
            useHttps = true,
            verifySsl = true,
            certificateFingerprint = "",
            useApiToken = true,
            apiTokenId = "other",
            allowInsecureTls = true
        )

        assertFalse(matches)
    }

    @Test
    fun savedCredentialsMatchNonSecretLoginDraft_matchesEquivalentFingerprintFormatting() {
        val fingerprint = "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99"
        val savedCredentials = savedCredentials(
            certificateFingerprint = fingerprint
        )

        val matches = savedCredentialsMatchNonSecretLoginDraft(
            savedCredentials = savedCredentials,
            host = "demo.example.test",
            port = "8006",
            username = "tester",
            realm = "pam",
            useHttps = true,
            verifySsl = true,
            certificateFingerprint = fingerprint.replace(":", ""),
            useApiToken = false,
            apiTokenId = "",
            allowInsecureTls = true
        )

        assertTrue(matches)
    }

    private fun savedCredentials(
        verifySsl: Boolean = true,
        authMethod: String = CredentialAuthMethod.PASSWORD,
        apiTokenId: String = "",
        certificateFingerprint: String = ""
    ): SavedCredentials {
        return SavedCredentials(
            host = "demo.example.test",
            port = 8006,
            username = "tester",
            password = "password-secret",
            realm = "pam",
            useHttps = true,
            verifySsl = verifySsl,
            certificateFingerprint = certificateFingerprint,
            authMethod = authMethod,
            apiTokenId = apiTokenId,
            apiTokenSecret = "token-secret"
        )
    }
}
