package com.proxmoxmobile.presentation.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.proxmoxmobile.data.dashboard.DashboardRepository
import com.proxmoxmobile.data.task.TaskRepository
import com.proxmoxmobile.data.vm.VmRepository
import com.proxmoxmobile.presentation.theme.ProxmoxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ProxmoxNavHostViewportSmokeTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun compactPortraitDashboardRendersCoreContent() {
        assertRouteRendersInViewport(
            route = Screen.Dashboard.route,
            viewport = CompactPortrait,
            overrides = ViewportRepositoryOverrides(
                dashboardRepositoryOverride = NavigationSmokeFixtures.fakeDashboardRepository()
            )
        ) {
            waitForText("System Status")
            waitForText(NavigationSmokeFixtures.LAB_NODE)
        }
    }

    @Test
    fun compactLandscapeVmDetailRendersCoreContent() {
        assertRouteRendersInViewport(
            route = Screen.VMDetailWithNode.createRoute(
                NavigationSmokeFixtures.LAB_NODE,
                NavigationSmokeFixtures.VM_ID
            ),
            viewport = CompactLandscape,
            overrides = ViewportRepositoryOverrides(
                vmRepositoryOverride = NavigationSmokeFixtures.fakeVmRepository()
            )
        ) {
            waitForText(NavigationSmokeFixtures.VM_NAME)
            waitForText(NavigationSmokeFixtures.VM_ID.toString())
        }
    }

    @Test
    fun compactLandscapeTaskDetailRendersCoreContent() {
        assertRouteRendersInViewport(
            route = Screen.TaskDetail.createRoute(
                NavigationSmokeFixtures.LAB_NODE,
                NavigationSmokeFixtures.TASK_UPID
            ),
            viewport = CompactLandscape,
            overrides = ViewportRepositoryOverrides(
                taskRepositoryOverride = NavigationSmokeFixtures.fakeTaskRepository()
            )
        ) {
            waitForText("Task Details")
            waitForText("QMSTART")
        }
    }

    private fun assertRouteRendersInViewport(
        route: String,
        viewport: Viewport,
        overrides: ViewportRepositoryOverrides = ViewportRepositoryOverrides(),
        assertVisible: () -> Unit
    ) {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    val navController = rememberNavController()
                    val viewModel = NavigationSmokeFixtures.fakeAuthenticatedViewModel()

                    LaunchedEffect(route) {
                        if (route != Screen.Dashboard.route) {
                            navController.navigate(route)
                        }
                    }

                    ProxmoxTheme {
                        Box(modifier = Modifier.requiredSize(viewport.width, viewport.height)) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                ProxmoxNavHost(
                                    navController = navController,
                                    viewModel = viewModel,
                                    startDestination = Screen.Dashboard.route,
                                    vmRepositoryOverride = overrides.vmRepositoryOverride,
                                    taskRepositoryOverride = overrides.taskRepositoryOverride,
                                    dashboardRepositoryOverride = overrides.dashboardRepositoryOverride
                                        ?: NavigationSmokeFixtures.fakeDashboardRepository()
                                )
                            }
                        }
                    }
                }
            }

            assertVisible()
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntilAtLeastOneExists(
            matcher = hasText(text, substring = true),
            timeoutMillis = 5_000
        )
    }

    private data class Viewport(
        val width: Dp,
        val height: Dp
    )

    private data class ViewportRepositoryOverrides(
        val vmRepositoryOverride: VmRepository? = null,
        val taskRepositoryOverride: TaskRepository? = null,
        val dashboardRepositoryOverride: DashboardRepository? = null
    )

    private companion object {
        val CompactPortrait = Viewport(width = 320.dp, height = 568.dp)
        val CompactLandscape = Viewport(width = 640.dp, height = 360.dp)
    }
}
