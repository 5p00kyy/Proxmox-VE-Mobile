package com.proxmoxmobile.presentation.navigation

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.proxmoxmobile.R
import com.proxmoxmobile.data.model.ServerConfig
import com.proxmoxmobile.presentation.theme.ProxmoxTheme
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProxmoxNavHostRouteSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun fakeAuthenticatedSessionCanStartOnSettingsRoute() {
        composeRule.setContent {
            val navController = rememberNavController()
            val viewModel = MainViewModel().apply {
                setCurrentServer(
                    ServerConfig(
                        host = "example.test",
                        port = 8006,
                        username = "tester",
                        password = null,
                        realm = "pam",
                        useHttps = true,
                        verifySsl = true
                    )
                )
                setAuthenticated(true)
            }

            ProxmoxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ProxmoxNavHost(
                        navController = navController,
                        viewModel = viewModel,
                        startDestination = Screen.Settings.route
                    )
                }
            }
        }

        composeRule.onNodeWithText(text(R.string.settings_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_app_name)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_section_display)).assertIsDisplayed()
    }

    private fun text(@StringRes id: Int): String {
        return targetContext.getString(id)
    }
}
