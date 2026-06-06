package com.proxmoxmobile.presentation.navigation

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
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
@OptIn(ExperimentalTestApi::class)
class ProxmoxNavHostRouteSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun fakeAuthenticatedSessionCanStartOnSettingsRoute() {
        startFakeAuthenticatedRoute(Screen.Settings.route)

        composeRule.onNodeWithText(text(R.string.settings_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_app_name)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_section_display)).assertIsDisplayed()
    }

    @Test
    fun fakeAuthenticatedSessionCanStartOnServerListRoute() {
        startFakeAuthenticatedRoute(Screen.ServerList.route)

        composeRule.onNodeWithText(text(R.string.servers_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.servers_management)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.servers_coming_soon)).assertIsDisplayed()
    }

    @Test
    fun fakeAuthenticatedSessionCanStartOnDashboardWithoutApiSession() {
        startFakeAuthenticatedRoute(Screen.Dashboard.route)

        composeRule.onNodeWithText(text(R.string.dashboard_app_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.dashboard_ready)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.dashboard_welcome_title)).assertIsDisplayed()
    }

    @Test
    fun fakeAuthenticatedSessionCanStartOnTasksRouteWithoutCachedNodes() {
        startFakeAuthenticatedRoute(Screen.Tasks.route)

        composeRule.onNodeWithText(text(R.string.task_title)).assertIsDisplayed()
        composeRule.onNodeWithText(
            text(R.string.task_selected_node, text(R.string.task_none))
        ).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.task_error_no_nodes)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.task_filters_title)).assertIsDisplayed()
    }

    @Test
    fun fakeAuthenticatedSessionCanDecodeNodeScopedTaskRoute() {
        startFakeAuthenticatedRoute(Screen.NodeTasks.createRoute(ROUTE_NODE))

        composeRule.onNodeWithText(text(R.string.task_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.task_selected_node, ROUTE_NODE)).fetchSemanticsNode()
        composeRule.waitUntilAtLeastOneExists(hasText(NOT_AUTHENTICATED), timeoutMillis = 5_000)
        composeRule.onNodeWithText(NOT_AUTHENTICATED).assertIsDisplayed()
    }

    @Test
    fun fakeAuthenticatedSessionCanDecodeResourceTaskRoute() {
        startFakeAuthenticatedRoute(Screen.ResourceTasks.createRoute(ROUTE_NODE, ROUTE_VMID))

        composeRule.onNodeWithText(text(R.string.task_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.task_selected_node, ROUTE_NODE)).fetchSemanticsNode()
        composeRule.onNodeWithText(ROUTE_VMID.toString()).fetchSemanticsNode()
        composeRule.waitUntilAtLeastOneExists(hasText(NOT_AUTHENTICATED), timeoutMillis = 5_000)
        composeRule.onNodeWithText(NOT_AUTHENTICATED).assertIsDisplayed()
    }

    @Test
    fun fakeAuthenticatedSessionCanDecodeTaskDetailRoute() {
        startFakeAuthenticatedRoute(Screen.TaskDetail.createRoute(ROUTE_NODE, ROUTE_UPID))

        composeRule.onNodeWithText(text(R.string.task_detail_title)).assertIsDisplayed()
        composeRule.waitUntilAtLeastOneExists(hasText(NOT_AUTHENTICATED), timeoutMillis = 5_000)
        composeRule.onNodeWithText(NOT_AUTHENTICATED).assertIsDisplayed()
    }

    @Test
    fun fakeAuthenticatedSessionCanStartOnNetworkWithoutCachedNodes() {
        startFakeAuthenticatedRoute(Screen.Network.route)

        composeRule.onNodeWithText(text(R.string.network_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.network_error_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.network_error_no_nodes)).assertIsDisplayed()
    }

    @Test
    fun fakeAuthenticatedSessionCanDecodeNodeNetworkRoute() {
        startFakeAuthenticatedRoute(Screen.NodeNetwork.createRoute(ROUTE_NODE))

        composeRule.onNodeWithText(text(R.string.network_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.network_node_label, ROUTE_NODE)).fetchSemanticsNode()
        composeRule.waitUntilAtLeastOneExists(hasText(NOT_AUTHENTICATED), timeoutMillis = 5_000)
        composeRule.onNodeWithText(NOT_AUTHENTICATED).assertIsDisplayed()
    }

    @Test
    fun fakeAuthenticatedSessionCanStartOnClusterWithoutApiSession() {
        startFakeAuthenticatedRoute(Screen.Cluster.route)

        composeRule.onNodeWithText(text(R.string.cluster_title)).assertIsDisplayed()
        composeRule.waitUntilAtLeastOneExists(hasText(text(R.string.cluster_error_title)), timeoutMillis = 5_000)
        composeRule.onNodeWithText(text(R.string.cluster_error_title)).assertIsDisplayed()
        composeRule.onNodeWithText(NOT_AUTHENTICATED).assertIsDisplayed()
    }

    @Test
    fun fakeAuthenticatedSessionCanDecodeStorageRoute() {
        startFakeAuthenticatedRoute(Screen.Storage.createRoute(ROUTE_NODE))

        composeRule.onNodeWithText(text(R.string.storage_title, ROUTE_NODE)).fetchSemanticsNode()
        composeRule.waitUntilAtLeastOneExists(hasText(NOT_AUTHENTICATED), timeoutMillis = 5_000)
        composeRule.onNodeWithText(NOT_AUTHENTICATED).assertIsDisplayed()
    }

    private fun startFakeAuthenticatedRoute(startDestination: String) {
        composeRule.setContent {
            val navController = rememberNavController()
            val viewModel = fakeAuthenticatedViewModel()
            LaunchedEffect(startDestination) {
                if (startDestination != Screen.Settings.route) {
                    navController.navigate(startDestination) {
                        popUpTo(Screen.Settings.route) { inclusive = true }
                    }
                }
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
        private const val ROUTE_NODE = "lab-node"
        private const val ROUTE_VMID = 102
        private const val ROUTE_UPID = "UPID:fixture:0001:task:102:tester@pam:"
        private const val NOT_AUTHENTICATED = "Not authenticated"
    }
}
