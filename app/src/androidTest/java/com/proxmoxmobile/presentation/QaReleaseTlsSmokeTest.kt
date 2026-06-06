package com.proxmoxmobile.presentation

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.proxmoxmobile.BuildConfig
import com.proxmoxmobile.R
import com.proxmoxmobile.presentation.screens.auth.LOGIN_CERTIFICATE_FINGERPRINT_TAG
import com.proxmoxmobile.presentation.screens.auth.LOGIN_CONNECT_BUTTON_TAG
import com.proxmoxmobile.presentation.screens.auth.LOGIN_HTTPS_SWITCH_TAG
import com.proxmoxmobile.presentation.screens.auth.LOGIN_VERIFY_SSL_SWITCH_TAG
import com.proxmoxmobile.presentation.screens.auth.LOGIN_VERIFY_SSL_WARNING_TAG
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QaReleaseTlsSmokeTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun clearSavedCredentials() {
        assumeFalse("qaRelease TLS smoke requires ALLOW_INSECURE_TLS=false", BuildConfig.ALLOW_INSECURE_TLS)
        targetContext
            .getSharedPreferences("proxmox_secure_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun qaReleaseLoginDoesNotExposeInsecureTlsMode() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithText(text(R.string.login_app_title)).assertIsDisplayed()
            composeRule.onNodeWithTag(LOGIN_HTTPS_SWITCH_TAG)
                .performScrollTo()
                .assertIsOn()
            composeRule.onNodeWithTag(LOGIN_CERTIFICATE_FINGERPRINT_TAG)
                .performScrollTo()
                .assertIsDisplayed()
            composeRule.onNodeWithTag(LOGIN_VERIFY_SSL_SWITCH_TAG)
                .performScrollTo()
                .assertIsDisplayed()
                .assertIsNotEnabled()
                .assertIsOn()
            composeRule.onAllNodesWithTag(LOGIN_VERIFY_SSL_WARNING_TAG).assertCountEquals(0)

            composeRule.onNodeWithText(text(R.string.login_host))
                .performScrollTo()
                .performTextReplacement("demo.example.test")
            composeRule.onNodeWithText(text(R.string.login_username))
                .performScrollTo()
                .performTextReplacement("tester")
            composeRule.onNodeWithText(text(R.string.login_password))
                .performScrollTo()
                .performTextReplacement("password-secret")
            composeRule.onNodeWithTag(LOGIN_CERTIFICATE_FINGERPRINT_TAG)
                .performScrollTo()
                .performTextReplacement(VALID_SHA256_FINGERPRINT)

            composeRule.onNodeWithTag(LOGIN_CONNECT_BUTTON_TAG)
                .performScrollTo()
                .assertIsEnabled()
        }
    }

    private fun text(@StringRes id: Int): String {
        return targetContext.getString(id)
    }

    private companion object {
        private const val VALID_SHA256_FINGERPRINT =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }
}
