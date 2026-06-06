package com.proxmoxmobile.presentation

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.proxmoxmobile.R
import com.proxmoxmobile.presentation.screens.auth.LOGIN_API_TOKEN_SWITCH_TAG
import com.proxmoxmobile.presentation.screens.auth.LOGIN_CERTIFICATE_FINGERPRINT_TAG
import com.proxmoxmobile.presentation.screens.auth.LOGIN_CONNECT_BUTTON_TAG
import com.proxmoxmobile.presentation.screens.auth.LOGIN_HTTPS_SWITCH_TAG
import com.proxmoxmobile.presentation.screens.auth.LOGIN_VERIFY_SSL_SWITCH_TAG
import com.proxmoxmobile.presentation.screens.auth.LOGIN_VERIFY_SSL_WARNING_TAG
import com.proxmoxmobile.presentation.screens.auth.LoginScreen
import com.proxmoxmobile.presentation.theme.ProxmoxTheme
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun clearSavedCredentials() {
        targetContext
            .getSharedPreferences("proxmox_secure_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun loginScreenSupportsLocalApiTokenTlsStateAcrossRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeRule.onNodeWithText(text(R.string.login_app_title)).assertIsDisplayed()
            composeRule.onNodeWithText(text(R.string.login_server_configuration)).assertIsDisplayed()
            composeRule.onNodeWithTag(LOGIN_CONNECT_BUTTON_TAG)
                .performScrollTo()
                .assertIsNotEnabled()

            composeRule.onNodeWithText(text(R.string.login_host))
                .performScrollTo()
                .performTextReplacement("demo.example.test")
            composeRule.onNodeWithText(text(R.string.login_username))
                .performScrollTo()
                .performTextReplacement("tester")
            composeRule.onNodeWithTag(LOGIN_CERTIFICATE_FINGERPRINT_TAG)
                .performScrollTo()
                .performTextReplacement("invalid")
            composeRule.onNodeWithText(text(R.string.login_certificate_fingerprint_invalid))
                .assertIsDisplayed()

            composeRule.onNodeWithTag(LOGIN_VERIFY_SSL_SWITCH_TAG)
                .performScrollTo()
                .assertIsEnabled()
            composeRule.onNodeWithTag(LOGIN_API_TOKEN_SWITCH_TAG)
                .performScrollTo()
                .performClick()
            composeRule.onNodeWithText(text(R.string.login_api_token_id))
                .performScrollTo()
                .performTextReplacement("mobile")
            composeRule.onNodeWithText(text(R.string.login_api_token_secret))
                .performScrollTo()
                .performTextReplacement("token-secret")
            composeRule.onNodeWithTag(LOGIN_CONNECT_BUTTON_TAG)
                .performScrollTo()
                .assertIsNotEnabled()

            composeRule.onNodeWithTag(LOGIN_CERTIFICATE_FINGERPRINT_TAG)
                .performScrollTo()
                .performTextReplacement(VALID_SHA256_FINGERPRINT)
            composeRule.onNodeWithTag(LOGIN_CONNECT_BUTTON_TAG)
                .performScrollTo()
                .assertIsEnabled()

            scenario.recreate()

            composeRule.onNodeWithText(text(R.string.login_api_token_id))
                .performScrollTo()
                .assertIsDisplayed()
            composeRule.onNodeWithText(VALID_SHA256_FINGERPRINT)
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun loginScreenForcesTlsVerificationWhenInsecureTlsIsUnavailable() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    ProxmoxTheme {
                        LoginScreen(
                            viewModel = MainViewModel(),
                            onNavigateToDashboard = {},
                            allowInsecureTls = false
                        )
                    }
                }
            }

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
            composeRule.onNodeWithTag(LOGIN_CERTIFICATE_FINGERPRINT_TAG)
                .performScrollTo()
                .performTextReplacement("invalid")
            composeRule.onNodeWithTag(LOGIN_CONNECT_BUTTON_TAG)
                .performScrollTo()
                .assertIsNotEnabled()
            composeRule.onNodeWithTag(LOGIN_CERTIFICATE_FINGERPRINT_TAG)
                .performScrollTo()
                .performTextReplacement(VALID_SHA256_FINGERPRINT)
            composeRule.onNodeWithText(text(R.string.login_password))
                .performScrollTo()
                .performTextReplacement("password-secret")

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
