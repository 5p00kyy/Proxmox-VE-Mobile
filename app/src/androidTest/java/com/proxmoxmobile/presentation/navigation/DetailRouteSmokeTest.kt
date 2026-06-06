package com.proxmoxmobile.presentation.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.proxmoxmobile.data.backup.BackupRepository
import com.proxmoxmobile.data.cluster.ClusterRepository
import com.proxmoxmobile.data.dashboard.DashboardRepository
import com.proxmoxmobile.data.lxc.LxcRepository
import com.proxmoxmobile.data.network.NetworkRepository
import com.proxmoxmobile.data.node.NodeRepository
import com.proxmoxmobile.data.storage.StorageRepository
import com.proxmoxmobile.data.task.TaskRepository
import com.proxmoxmobile.data.task.TaskResult
import com.proxmoxmobile.data.user.UserRepository
import com.proxmoxmobile.data.vm.VmRepository
import com.proxmoxmobile.presentation.navigation.NavigationSmokeFixtures.LAB_NODE
import com.proxmoxmobile.presentation.navigation.NavigationSmokeFixtures.LXC_ID
import com.proxmoxmobile.presentation.navigation.NavigationSmokeFixtures.LXC_NAME
import com.proxmoxmobile.presentation.navigation.NavigationSmokeFixtures.TASK_UPID
import com.proxmoxmobile.presentation.navigation.NavigationSmokeFixtures.VM_ID
import com.proxmoxmobile.presentation.navigation.NavigationSmokeFixtures.VM_NAME
import com.proxmoxmobile.presentation.screens.dashboard.DASHBOARD_RECENT_TASKS_METRIC_TAG
import com.proxmoxmobile.presentation.theme.ProxmoxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class DetailRouteSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun fakeNodeDetailRouteRendersPopulatedContent() {
        startFakeAuthenticatedRoute(
            route = Screen.NodeDetail.createRoute(LAB_NODE),
            nodeRepositoryOverride = NavigationSmokeFixtures.fakeNodeRepository()
        )

        composeRule.waitUntilAtLeastOneExists(hasText(LAB_NODE, substring = true))
        composeRule.onNodeWithText("8.2.0", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("VMs").assertIsDisplayed()
        composeRule.onNodeWithText("LXC").assertIsDisplayed()
    }

    @Test
    fun fakeVmDetailRouteRendersPopulatedContent() {
        startFakeAuthenticatedRoute(
            route = Screen.VMDetailWithNode.createRoute(LAB_NODE, VM_ID),
            vmRepositoryOverride = NavigationSmokeFixtures.fakeVmRepository()
        )

        composeRule.waitUntilAtLeastOneExists(hasText(VM_NAME, substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("snap-before-upgrade", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("ostype", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("l26", substring = true))
    }

    @Test
    fun fakeContainerDetailRouteRendersPopulatedContent() {
        startFakeAuthenticatedRoute(
            route = Screen.ContainerDetailWithNode.createRoute(LAB_NODE, LXC_ID),
            lxcRepositoryOverride = NavigationSmokeFixtures.fakeLxcRepository()
        )

        composeRule.waitUntilAtLeastOneExists(hasText(LXC_NAME, substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("snap-clean-install", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("Resource Management", substring = true))
    }

    @Test
    fun fakeTaskDetailRouteRendersPopulatedContent() {
        startFakeAuthenticatedRoute(
            route = Screen.TaskDetail.createRoute(LAB_NODE, TASK_UPID),
            taskRepositoryOverride = NavigationSmokeFixtures.fakeTaskRepository()
        )

        composeRule.waitUntilAtLeastOneExists(hasText("Task Details"))
        composeRule.waitUntilAtLeastOneExists(hasText("QMSTART", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText(TASK_UPID, substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("TASK_OK", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("fixture task completed", substring = true))
    }

    @Test
    fun fakeStorageRouteRendersPopulatedContent() {
        startFakeAuthenticatedRoute(
            route = Screen.Storage.createRoute(LAB_NODE),
            storageRepositoryOverride = NavigationSmokeFixtures.fakeStorageRepository()
        )

        composeRule.waitUntilAtLeastOneExists(hasText("local-fixture", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("dir", substring = true))
        composeRule.onNodeWithText("Browse content").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("local-fixture:iso/proxmox-mobile-fixture.iso", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("Public fixture ISO", substring = true))
    }

    @Test
    fun fakeStorageRouteRendersEmptyContentState() {
        startFakeAuthenticatedRoute(
            route = Screen.Storage.createRoute(LAB_NODE),
            storageRepositoryOverride = NavigationSmokeFixtures.fakeStorageRepository(
                storageContentForName = { emptyList() }
            )
        )

        composeRule.waitUntilAtLeastOneExists(hasText("local-fixture", substring = true))
        composeRule.onNodeWithText("Browse content").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Content on local-fixture", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("No content found on this storage.", substring = true))
    }

    @Test
    fun fakeNetworkRouteRendersPopulatedContent() {
        startFakeAuthenticatedRoute(
            route = Screen.NodeNetwork.createRoute(LAB_NODE),
            networkRepositoryOverride = NavigationSmokeFixtures.fakeNetworkRepository()
        )

        composeRule.waitUntilAtLeastOneExists(hasText("Network Interfaces (2)", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("vmbr-fixture", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("bridge", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("eth-fixture", substring = true))
    }

    @Test
    fun fakeNetworkRouteRendersEmptyState() {
        startFakeAuthenticatedRoute(
            route = Screen.NodeNetwork.createRoute(LAB_NODE),
            networkRepositoryOverride = NavigationSmokeFixtures.fakeNetworkRepository(interfaces = emptyList())
        )

        composeRule.waitUntilAtLeastOneExists(hasText("No Network Interfaces", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("No network interfaces found on this node", substring = true))
    }

    @Test
    fun fakeUsersRouteRendersPopulatedContent() {
        startFakeAuthenticatedRoute(
            route = Screen.Users.route,
            userRepositoryOverride = NavigationSmokeFixtures.fakeUserRepository()
        )

        composeRule.waitUntilAtLeastOneExists(hasText("Users (2)", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("alpha-fixture@pam", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("Ada Fixture", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("ada.fixture@example.test", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("Public QA fixture user", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("disabled-fixture@pve", substring = true))
    }

    @Test
    fun fakeUsersRouteRendersEmptyState() {
        startFakeAuthenticatedRoute(
            route = Screen.Users.route,
            userRepositoryOverride = NavigationSmokeFixtures.fakeUserRepository(users = emptyList())
        )

        composeRule.waitUntilAtLeastOneExists(hasText("No Users Found", substring = true))
        composeRule.waitUntilAtLeastOneExists(
            hasText("No users are currently configured on this system", substring = true)
        )
        composeRule.waitUntilAtLeastOneExists(
            hasText("User management actions are read-only in this beta.", substring = true)
        )
    }

    @Test
    fun fakeBackupsRouteRendersPopulatedContent() {
        startFakeAuthenticatedRoute(
            route = Screen.Backups.route,
            backupRepositoryOverride = NavigationSmokeFixtures.fakeBackupRepository()
        )

        composeRule.waitUntilAtLeastOneExists(hasText("Backups (1)", substring = true))
        composeRule.waitUntilAtLeastOneExists(
            hasText("backup-fixture:backup/vzdump-qemu-102-fixture.vma.zst", substring = true)
        )
        composeRule.waitUntilAtLeastOneExists(hasText("Public fixture VM backup", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("VMA.ZST", substring = true))
    }

    @Test
    fun fakeBackupsRouteRendersEmptyStateWithStorageFilter() {
        startFakeAuthenticatedRoute(
            route = Screen.Backups.route,
            backupRepositoryOverride = NavigationSmokeFixtures.fakeBackupRepository(
                storageContentForName = { emptyList() }
            )
        )

        composeRule.waitUntilAtLeastOneExists(hasText("No Backups Found", substring = true))
        composeRule.waitUntilAtLeastOneExists(
            hasText("No backups are currently available on this system", substring = true)
        )
        composeRule.waitUntilAtLeastOneExists(hasText("Storage: All storage", substring = true))
        composeRule.waitUntilAtLeastOneExists(
            hasText("Backup actions are read-only in this beta.", substring = true)
        )
    }

    @Test
    fun fakeClusterRouteRendersPopulatedContent() {
        startFakeAuthenticatedRoute(
            route = Screen.Cluster.route,
            clusterRepositoryOverride = NavigationSmokeFixtures.fakeClusterRepository()
        )

        composeRule.waitUntilAtLeastOneExists(hasText("Cluster Overview", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("fixture-cluster", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("Online nodes: 2 / 2", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("Votes: 2 / 2", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("Nodes (2)", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText(LAB_NODE, substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("qa-node", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("192.0.2.10", substring = true))
    }

    @Test
    fun fakeClusterRouteRendersErrorState() {
        startFakeAuthenticatedRoute(
            route = Screen.Cluster.route,
            clusterRepositoryOverride = NavigationSmokeFixtures.failingClusterRepository()
        )

        composeRule.waitUntilAtLeastOneExists(hasText("Cluster status unavailable", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("Cluster fixture unavailable", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("Retry", substring = true))
    }

    @Test
    fun fakeDashboardRouteRendersPopulatedContent() {
        startFakeAuthenticatedRoute(
            route = Screen.Dashboard.route,
            dashboardRepositoryOverride = NavigationSmokeFixtures.fakeDashboardRepository()
        )

        composeRule.waitUntilAtLeastOneExists(hasText("System Status", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("Nodes (1)", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText(LAB_NODE, substring = true))
        composeRule.waitUntilAtLeastOneExists(
            hasTestTag(DASHBOARD_RECENT_TASKS_METRIC_TAG) and hasText("2")
        )
        composeRule.waitUntilAtLeastOneExists(hasText("APTUPDATE on lab-node (OK)", substring = true))
    }

    @Test
    fun fakeDashboardRouteRendersTaskSummaryErrorState() {
        startFakeAuthenticatedRoute(
            route = Screen.Dashboard.route,
            dashboardRepositoryOverride = NavigationSmokeFixtures.fakeDashboardRepository(
                taskSummaryResult = TaskResult.Error("Task monitor unavailable in QA fixture")
            )
        )

        composeRule.waitUntilAtLeastOneExists(hasText("System Status", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("Task Activity", substring = true))
        composeRule.waitUntilAtLeastOneExists(
            hasText("Task activity unavailable: Task monitor unavailable in QA fixture", substring = true)
        )
        composeRule.waitUntilAtLeastOneExists(hasText("Nodes (1)", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText(LAB_NODE, substring = true))
    }

    private fun startFakeAuthenticatedRoute(
        route: String,
        vmRepositoryOverride: VmRepository? = null,
        lxcRepositoryOverride: LxcRepository? = null,
        nodeRepositoryOverride: NodeRepository? = null,
        taskRepositoryOverride: TaskRepository? = null,
        storageRepositoryOverride: StorageRepository? = null,
        networkRepositoryOverride: NetworkRepository? = null,
        userRepositoryOverride: UserRepository? = null,
        backupRepositoryOverride: BackupRepository? = null,
        clusterRepositoryOverride: ClusterRepository? = null,
        dashboardRepositoryOverride: DashboardRepository? = null
    ) {
        composeRule.setContent {
            val navController = rememberNavController()
            val viewModel = NavigationSmokeFixtures.fakeAuthenticatedViewModel()

            LaunchedEffect(route) {
                navController.navigate(route) {
                    popUpTo(Screen.Settings.route) { inclusive = true }
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
                        startDestination = Screen.Settings.route,
                        vmRepositoryOverride = vmRepositoryOverride,
                        lxcRepositoryOverride = lxcRepositoryOverride,
                        nodeRepositoryOverride = nodeRepositoryOverride,
                        taskRepositoryOverride = taskRepositoryOverride,
                        storageRepositoryOverride = storageRepositoryOverride,
                        networkRepositoryOverride = networkRepositoryOverride,
                        userRepositoryOverride = userRepositoryOverride,
                        backupRepositoryOverride = backupRepositoryOverride,
                        clusterRepositoryOverride = clusterRepositoryOverride,
                        dashboardRepositoryOverride = dashboardRepositoryOverride
                    )
                }
            }
        }
    }

}
