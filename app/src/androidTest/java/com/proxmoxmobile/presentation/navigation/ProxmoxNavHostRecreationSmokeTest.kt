package com.proxmoxmobile.presentation.navigation

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.proxmoxmobile.R
import com.proxmoxmobile.data.backup.BackupRepository
import com.proxmoxmobile.data.cluster.ClusterRepository
import com.proxmoxmobile.data.dashboard.DashboardRepository
import com.proxmoxmobile.data.lxc.LxcRepository
import com.proxmoxmobile.data.network.NetworkRepository
import com.proxmoxmobile.data.node.NodeRepository
import com.proxmoxmobile.data.storage.StorageRepository
import com.proxmoxmobile.data.task.TaskRepository
import com.proxmoxmobile.data.user.UserRepository
import com.proxmoxmobile.data.vm.VmRepository
import com.proxmoxmobile.presentation.theme.ProxmoxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ProxmoxNavHostRecreationSmokeTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun postLoginNodeScopedTaskRouteSurvivesRecreationAndBackgroundResume() {
        assertRouteSurvivesRecreationAndBackgroundResume(
            route = Screen.NodeTasks.createRoute(NavigationSmokeFixtures.LAB_NODE),
            overrides = NavigationRepositoryOverrides(
                taskRepositoryOverride = NavigationSmokeFixtures.fakeTaskRepository()
            ),
            assertRouteVisible = ::assertNodeScopedTaskRouteVisible
        )
    }

    @Test
    fun postLoginTaskRouteSurvivesRecreationAndBackgroundResume() {
        assertRouteSurvivesRecreationAndBackgroundResume(
            route = Screen.Tasks.route,
            overrides = NavigationRepositoryOverrides(
                taskRepositoryOverride = NavigationSmokeFixtures.fakeTaskRepository()
            ),
            assertRouteVisible = ::assertTaskRouteVisible
        )
    }

    @Test
    fun postLoginResourceTaskRouteSurvivesRecreationAndBackgroundResume() {
        assertRouteSurvivesRecreationAndBackgroundResume(
            route = Screen.ResourceTasks.createRoute(
                NavigationSmokeFixtures.LAB_NODE,
                NavigationSmokeFixtures.VM_ID
            ),
            overrides = NavigationRepositoryOverrides(
                taskRepositoryOverride = NavigationSmokeFixtures.fakeTaskRepository()
            ),
            assertRouteVisible = ::assertResourceTaskRouteVisible
        )
    }

    @Test
    fun postLoginTaskDetailRouteSurvivesRecreationAndBackgroundResume() {
        assertRouteSurvivesRecreationAndBackgroundResume(
            route = Screen.TaskDetail.createRoute(
                NavigationSmokeFixtures.LAB_NODE,
                NavigationSmokeFixtures.TASK_UPID
            ),
            overrides = NavigationRepositoryOverrides(
                taskRepositoryOverride = NavigationSmokeFixtures.fakeTaskRepository()
            ),
            assertRouteVisible = ::assertTaskDetailRouteVisible
        )
    }

    @Test
    fun postLoginNodeNetworkRouteSurvivesRecreationAndBackgroundResume() {
        assertRouteSurvivesRecreationAndBackgroundResume(
            route = Screen.NodeNetwork.createRoute(NavigationSmokeFixtures.LAB_NODE),
            overrides = NavigationRepositoryOverrides(
                networkRepositoryOverride = NavigationSmokeFixtures.fakeNetworkRepository()
            ),
            assertRouteVisible = ::assertNodeNetworkRouteVisible
        )
    }

    @Test
    fun postLoginStorageRouteSurvivesRecreationAndBackgroundResume() {
        assertRouteSurvivesRecreationAndBackgroundResume(
            route = Screen.Storage.createRoute(NavigationSmokeFixtures.LAB_NODE),
            overrides = NavigationRepositoryOverrides(
                storageRepositoryOverride = NavigationSmokeFixtures.fakeStorageRepository()
            ),
            assertRouteVisible = ::assertStorageRouteVisible
        )
    }

    @Test
    fun dashboardRouteSurvivesRecreationAndBackgroundResume() {
        assertRouteSurvivesRecreationAndBackgroundResume(
            route = Screen.Dashboard.route,
            overrides = NavigationRepositoryOverrides(
                dashboardRepositoryOverride = NavigationSmokeFixtures.fakeDashboardRepository()
            ),
            assertRouteVisible = ::assertDashboardRouteVisible
        )
    }

    @Test
    fun nodeDetailRouteSurvivesRecreationAndBackgroundResume() {
        assertRouteSurvivesRecreationAndBackgroundResume(
            route = Screen.NodeDetail.createRoute(NavigationSmokeFixtures.LAB_NODE),
            overrides = NavigationRepositoryOverrides(
                nodeRepositoryOverride = NavigationSmokeFixtures.fakeNodeRepository()
            ),
            assertRouteVisible = ::assertNodeDetailRouteVisible
        )
    }

    @Test
    fun vmListRouteSurvivesRecreationAndBackgroundResume() {
        assertRouteSurvivesRecreationAndBackgroundResume(
            route = Screen.VMList.createRoute(NavigationSmokeFixtures.LAB_NODE),
            overrides = NavigationRepositoryOverrides(
                vmRepositoryOverride = NavigationSmokeFixtures.fakeVmRepository()
            ),
            assertRouteVisible = ::assertVmListRouteVisible
        )
    }

    @Test
    fun vmDetailRouteSurvivesRecreationAndBackgroundResume() {
        assertRouteSurvivesRecreationAndBackgroundResume(
            route = Screen.VMDetailWithNode.createRoute(
                NavigationSmokeFixtures.LAB_NODE,
                NavigationSmokeFixtures.VM_ID
            ),
            overrides = NavigationRepositoryOverrides(
                vmRepositoryOverride = NavigationSmokeFixtures.fakeVmRepository()
            ),
            assertRouteVisible = ::assertVmDetailRouteVisible
        )
    }

    @Test
    fun lxcListRouteSurvivesRecreationAndBackgroundResume() {
        assertRouteSurvivesRecreationAndBackgroundResume(
            route = Screen.ContainerList.createRoute(NavigationSmokeFixtures.LAB_NODE),
            overrides = NavigationRepositoryOverrides(
                lxcRepositoryOverride = NavigationSmokeFixtures.fakeLxcRepository()
            ),
            assertRouteVisible = ::assertLxcListRouteVisible
        )
    }

    @Test
    fun lxcDetailRouteSurvivesRecreationAndBackgroundResume() {
        assertRouteSurvivesRecreationAndBackgroundResume(
            route = Screen.ContainerDetailWithNode.createRoute(
                NavigationSmokeFixtures.LAB_NODE,
                NavigationSmokeFixtures.LXC_ID
            ),
            overrides = NavigationRepositoryOverrides(
                lxcRepositoryOverride = NavigationSmokeFixtures.fakeLxcRepository()
            ),
            assertRouteVisible = ::assertLxcDetailRouteVisible
        )
    }

    @Test
    fun usersRouteSurvivesRecreationAndBackgroundResume() {
        assertRouteSurvivesRecreationAndBackgroundResume(
            route = Screen.Users.route,
            overrides = NavigationRepositoryOverrides(
                userRepositoryOverride = NavigationSmokeFixtures.fakeUserRepository()
            ),
            assertRouteVisible = ::assertUsersRouteVisible
        )
    }

    @Test
    fun backupsRouteSurvivesRecreationAndBackgroundResume() {
        assertRouteSurvivesRecreationAndBackgroundResume(
            route = Screen.Backups.route,
            overrides = NavigationRepositoryOverrides(
                backupRepositoryOverride = NavigationSmokeFixtures.fakeBackupRepository()
            ),
            assertRouteVisible = ::assertBackupsRouteVisible
        )
    }

    @Test
    fun clusterRouteSurvivesRecreationAndBackgroundResume() {
        assertRouteSurvivesRecreationAndBackgroundResume(
            route = Screen.Cluster.route,
            overrides = NavigationRepositoryOverrides(
                clusterRepositoryOverride = NavigationSmokeFixtures.fakeClusterRepository()
            ),
            assertRouteVisible = ::assertClusterRouteVisible
        )
    }

    @Test
    fun settingsRouteSurvivesRecreationAndBackgroundResume() {
        assertRouteSurvivesRecreationAndBackgroundResume(
            route = Screen.Settings.route,
            assertRouteVisible = ::assertSettingsRouteVisible
        )
    }

    private fun assertRouteSurvivesRecreationAndBackgroundResume(
        route: String,
        overrides: NavigationRepositoryOverrides = NavigationRepositoryOverrides(),
        assertRouteVisible: () -> Unit
    ) {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.setProxmoxNavContent(route, overrides)
            assertRouteVisible()

            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            composeRule.waitForIdle()
            assertRouteVisible()

            scenario.recreate()
            scenario.setProxmoxNavContent(route, overrides)

            assertRouteVisible()
        }
    }

    private fun ActivityScenario<ComponentActivity>.setProxmoxNavContent(
        route: String,
        overrides: NavigationRepositoryOverrides = NavigationRepositoryOverrides()
    ) {
        onActivity { activity ->
            activity.setContent {
                val navController = rememberNavController()

                LaunchedEffect(route) {
                    if (route != Screen.Dashboard.route) {
                        navController.navigate(route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
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
                            viewModel = NavigationSmokeFixtures.fakeAuthenticatedViewModel(),
                            startDestination = Screen.Dashboard.route,
                            vmRepositoryOverride = overrides.vmRepositoryOverride,
                            lxcRepositoryOverride = overrides.lxcRepositoryOverride,
                            nodeRepositoryOverride = overrides.nodeRepositoryOverride,
                            taskRepositoryOverride = overrides.taskRepositoryOverride,
                            storageRepositoryOverride = overrides.storageRepositoryOverride,
                            networkRepositoryOverride = overrides.networkRepositoryOverride,
                            userRepositoryOverride = overrides.userRepositoryOverride,
                            backupRepositoryOverride = overrides.backupRepositoryOverride,
                            clusterRepositoryOverride = overrides.clusterRepositoryOverride,
                            dashboardRepositoryOverride = overrides.dashboardRepositoryOverride
                                ?: NavigationSmokeFixtures.fakeDashboardRepository()
                        )
                    }
                }
            }
        }
    }

    private fun assertNodeScopedTaskRouteVisible() {
        composeRule.onNodeWithText(text(R.string.task_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(text(R.string.task_selected_node, NavigationSmokeFixtures.LAB_NODE))
            .assertIsDisplayed()
        composeRule.waitUntilAtLeastOneExists(hasText("QMSTART", substring = true))
    }

    private fun assertTaskRouteVisible() {
        composeRule.onNodeWithText(text(R.string.task_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(text(R.string.task_selected_node, NavigationSmokeFixtures.LAB_NODE))
            .assertIsDisplayed()
        composeRule.waitUntilAtLeastOneExists(hasText("QMSTART", substring = true))
    }

    private fun assertResourceTaskRouteVisible() {
        composeRule.onNodeWithText(text(R.string.task_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(text(R.string.task_selected_node, NavigationSmokeFixtures.LAB_NODE))
            .assertIsDisplayed()
        composeRule
            .waitUntilAtLeastOneExists(hasText(NavigationSmokeFixtures.VM_ID.toString()))
        composeRule.waitUntilAtLeastOneExists(hasText("QMSTART", substring = true))
    }

    private fun assertTaskDetailRouteVisible() {
        composeRule.onNodeWithText(text(R.string.task_detail_title)).assertIsDisplayed()
        composeRule.onNodeWithText(NavigationSmokeFixtures.TASK_UPID).assertIsDisplayed()
        composeRule.onNodeWithText("fixture task completed").assertIsDisplayed()
    }

    private fun assertNodeNetworkRouteVisible() {
        composeRule.onNodeWithText(text(R.string.network_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(text(R.string.network_node_label, NavigationSmokeFixtures.LAB_NODE))
            .assertIsDisplayed()
    }

    private fun assertStorageRouteVisible() {
        composeRule
            .onNodeWithText(text(R.string.storage_title, NavigationSmokeFixtures.LAB_NODE))
            .assertIsDisplayed()
    }

    private fun assertDashboardRouteVisible() {
        composeRule.waitUntilAtLeastOneExists(hasText(text(R.string.dashboard_system_status), substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText(NavigationSmokeFixtures.LAB_NODE, substring = true))
    }

    private fun assertNodeDetailRouteVisible() {
        composeRule.waitUntilAtLeastOneExists(hasText(NavigationSmokeFixtures.LAB_NODE, substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("8.2.0", substring = true))
    }

    private fun assertVmListRouteVisible() {
        composeRule.waitUntilAtLeastOneExists(hasText(NavigationSmokeFixtures.VM_NAME, substring = true))
    }

    private fun assertVmDetailRouteVisible() {
        composeRule.waitUntilAtLeastOneExists(hasText(NavigationSmokeFixtures.VM_NAME, substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("snap-before-upgrade", substring = true))
    }

    private fun assertLxcListRouteVisible() {
        composeRule.waitUntilAtLeastOneExists(hasText(NavigationSmokeFixtures.LXC_NAME, substring = true))
    }

    private fun assertLxcDetailRouteVisible() {
        composeRule.waitUntilAtLeastOneExists(hasText(NavigationSmokeFixtures.LXC_NAME, substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("snap-clean-install", substring = true))
    }

    private fun assertUsersRouteVisible() {
        composeRule.waitUntilAtLeastOneExists(hasText("Users (2)", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("alpha-fixture@pam", substring = true))
    }

    private fun assertBackupsRouteVisible() {
        composeRule.waitUntilAtLeastOneExists(hasText("Backups (1)", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("Public fixture VM backup", substring = true))
    }

    private fun assertClusterRouteVisible() {
        composeRule.waitUntilAtLeastOneExists(hasText("fixture-cluster", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText(NavigationSmokeFixtures.QA_NODE, substring = true))
    }

    private fun assertSettingsRouteVisible() {
        composeRule.onNodeWithText(text(R.string.settings_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_app_name)).assertIsDisplayed()
    }

    private fun text(@StringRes id: Int): String {
        return targetContext.getString(id)
    }

    private fun text(@StringRes id: Int, vararg args: Any): String {
        return targetContext.getString(id, *args)
    }

    private data class NavigationRepositoryOverrides(
        val vmRepositoryOverride: VmRepository? = null,
        val lxcRepositoryOverride: LxcRepository? = null,
        val nodeRepositoryOverride: NodeRepository? = null,
        val taskRepositoryOverride: TaskRepository? = null,
        val storageRepositoryOverride: StorageRepository? = null,
        val networkRepositoryOverride: NetworkRepository? = null,
        val userRepositoryOverride: UserRepository? = null,
        val backupRepositoryOverride: BackupRepository? = null,
        val clusterRepositoryOverride: ClusterRepository? = null,
        val dashboardRepositoryOverride: DashboardRepository? = null
    )
}
