package com.proxmoxmobile.presentation.navigation

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ActivityScenario
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
class ProxmoxNavHostRecreationSmokeTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun postLoginNodeScopedTaskRouteSurvivesActivityRecreation() {
        lateinit var navController: NavHostController

        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.setProxmoxNavContent { navController = it }

            composeRule.runOnIdle {
                navController.navigate(Screen.NodeTasks.createRoute(POST_LOGIN_NODE))
            }
            assertNodeScopedTaskRouteVisible()

            scenario.recreate()
            scenario.setProxmoxNavContent { navController = it }

            assertNodeScopedTaskRouteVisible()
        }
    }

    private fun ActivityScenario<ComponentActivity>.setProxmoxNavContent(
        onNavControllerReady: (NavHostController) -> Unit
    ) {
        onActivity { activity ->
            activity.setContent {
                val navController = rememberNavController()
                onNavControllerReady(navController)

                ProxmoxTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        ProxmoxNavHost(
                            navController = navController,
                            viewModel = fakeAuthenticatedViewModel(),
                            startDestination = Screen.Dashboard.route
                        )
                    }
                }
            }
        }
    }

    private fun assertNodeScopedTaskRouteVisible() {
        composeRule.onNodeWithText(text(R.string.task_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(text(R.string.task_selected_node, POST_LOGIN_NODE))
            .assertIsDisplayed()
    }

    private fun fakeAuthenticatedViewModel(): MainViewModel {
        return MainViewModel().apply {
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
    }

    private fun text(@StringRes id: Int): String {
        return targetContext.getString(id)
    }

    private fun text(@StringRes id: Int, vararg args: Any): String {
        return targetContext.getString(id, *args)
    }

    private companion object {
        private const val POST_LOGIN_NODE = "lab-node"
    }
}
