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
import com.proxmoxmobile.data.backup.BackupApi
import com.proxmoxmobile.data.backup.BackupRepository
import com.proxmoxmobile.data.cluster.ClusterApi
import com.proxmoxmobile.data.cluster.ClusterRepository
import com.proxmoxmobile.data.dashboard.DashboardApi
import com.proxmoxmobile.data.dashboard.DashboardRepository
import com.proxmoxmobile.data.dashboard.DashboardTaskSummarySource
import com.proxmoxmobile.data.lxc.LxcApi
import com.proxmoxmobile.data.lxc.LxcRepository
import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.ClusterStatusEntry
import com.proxmoxmobile.data.model.Container
import com.proxmoxmobile.data.model.LxcSnapshot
import com.proxmoxmobile.data.model.NetworkInterface
import com.proxmoxmobile.data.model.Node
import com.proxmoxmobile.data.model.NodeCpuInfo
import com.proxmoxmobile.data.model.NodeMemory
import com.proxmoxmobile.data.model.NodeStatus
import com.proxmoxmobile.data.model.RootFS
import com.proxmoxmobile.data.model.ServerConfig
import com.proxmoxmobile.data.model.Storage
import com.proxmoxmobile.data.model.StorageContent
import com.proxmoxmobile.data.model.Swap
import com.proxmoxmobile.data.model.Task
import com.proxmoxmobile.data.model.TaskLogEntry
import com.proxmoxmobile.data.model.VirtualMachine
import com.proxmoxmobile.data.model.VmSnapshot
import com.proxmoxmobile.data.network.NetworkApi
import com.proxmoxmobile.data.network.NetworkRepository
import com.proxmoxmobile.data.node.NodeApi
import com.proxmoxmobile.data.node.NodeRepository
import com.proxmoxmobile.data.storage.StorageApi
import com.proxmoxmobile.data.storage.StorageRepository
import com.proxmoxmobile.data.task.TaskApi
import com.proxmoxmobile.data.task.TaskRepository
import com.proxmoxmobile.data.task.TaskResult
import com.proxmoxmobile.data.task.TaskSummary
import com.proxmoxmobile.data.user.UserApi
import com.proxmoxmobile.data.user.UserRepository
import com.proxmoxmobile.data.model.User
import com.proxmoxmobile.data.vm.VmApi
import com.proxmoxmobile.data.vm.VmRepository
import com.proxmoxmobile.presentation.theme.ProxmoxTheme
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import com.proxmoxmobile.presentation.screens.dashboard.DASHBOARD_RECENT_TASKS_METRIC_TAG
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
            nodeRepositoryOverride = NodeRepository(FakeNodeApi())
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
            vmRepositoryOverride = VmRepository(FakeVmApi())
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
            lxcRepositoryOverride = LxcRepository(FakeLxcApi())
        )

        composeRule.waitUntilAtLeastOneExists(hasText(LXC_NAME, substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("snap-clean-install", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("Resource Management", substring = true))
    }

    @Test
    fun fakeTaskDetailRouteRendersPopulatedContent() {
        startFakeAuthenticatedRoute(
            route = Screen.TaskDetail.createRoute(LAB_NODE, TASK_UPID),
            taskRepositoryOverride = TaskRepository(FakeTaskApi())
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
            storageRepositoryOverride = StorageRepository(FakeStorageApi())
        )

        composeRule.waitUntilAtLeastOneExists(hasText("local-fixture", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("dir", substring = true))
        composeRule.onNodeWithText("Browse content").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("local-fixture:iso/proxmox-mobile-fixture.iso", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("Public fixture ISO", substring = true))
    }

    @Test
    fun fakeNetworkRouteRendersPopulatedContent() {
        startFakeAuthenticatedRoute(
            route = Screen.NodeNetwork.createRoute(LAB_NODE),
            networkRepositoryOverride = NetworkRepository(FakeNetworkApi())
        )

        composeRule.waitUntilAtLeastOneExists(hasText("Network Interfaces (2)", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("vmbr-fixture", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("bridge", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("eth-fixture", substring = true))
    }

    @Test
    fun fakeUsersRouteRendersPopulatedContent() {
        startFakeAuthenticatedRoute(
            route = Screen.Users.route,
            userRepositoryOverride = UserRepository(FakeUserApi())
        )

        composeRule.waitUntilAtLeastOneExists(hasText("Users (2)", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("alpha-fixture@pam", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("Ada Fixture", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("ada.fixture@example.test", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("Public QA fixture user", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("disabled-fixture@pve", substring = true))
    }

    @Test
    fun fakeBackupsRouteRendersPopulatedContent() {
        startFakeAuthenticatedRoute(
            route = Screen.Backups.route,
            backupRepositoryOverride = BackupRepository(FakeBackupApi())
        )

        composeRule.waitUntilAtLeastOneExists(hasText("Backups (1)", substring = true))
        composeRule.waitUntilAtLeastOneExists(
            hasText("backup-fixture:backup/vzdump-qemu-102-fixture.vma.zst", substring = true)
        )
        composeRule.waitUntilAtLeastOneExists(hasText("Public fixture VM backup", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("VMA.ZST", substring = true))
    }

    @Test
    fun fakeClusterRouteRendersPopulatedContent() {
        startFakeAuthenticatedRoute(
            route = Screen.Cluster.route,
            clusterRepositoryOverride = ClusterRepository(FakeClusterApi())
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
    fun fakeDashboardRouteRendersPopulatedContent() {
        startFakeAuthenticatedRoute(
            route = Screen.Dashboard.route,
            dashboardRepositoryOverride = DashboardRepository(
                api = FakeDashboardApi(),
                taskSummarySource = FakeDashboardTaskSummarySource()
            )
        )

        composeRule.waitUntilAtLeastOneExists(hasText("System Status", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText("Nodes (1)", substring = true))
        composeRule.waitUntilAtLeastOneExists(hasText(LAB_NODE, substring = true))
        composeRule.waitUntilAtLeastOneExists(
            hasTestTag(DASHBOARD_RECENT_TASKS_METRIC_TAG) and hasText("2")
        )
        composeRule.waitUntilAtLeastOneExists(hasText("APTUPDATE on lab-node (OK)", substring = true))
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
            val viewModel = fakeAuthenticatedViewModel()

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
            setCachedNodes(listOf(fakeNode()))
            setAuthenticated(true)
        }
    }

    private class FakeNodeApi : NodeApi {
        override suspend fun getNodeStatus(nodeName: String): ApiResponse<NodeStatus> {
            return ApiResponse(fakeNodeStatus(nodeName))
        }
    }

    private class FakeVmApi : VmApi {
        override suspend fun getVirtualMachines(nodeName: String): ApiResponse<List<VirtualMachine>> {
            return ApiResponse(listOf(fakeVm()))
        }

        override suspend fun getVMStatus(nodeName: String, vmid: Int): ApiResponse<VirtualMachine> {
            return ApiResponse(fakeVm(vmid = vmid))
        }

        override suspend fun getVMConfig(nodeName: String, vmid: Int): ApiResponse<Map<String, Any?>> {
            return ApiResponse(
                mapOf(
                    "name" to VM_NAME,
                    "ostype" to "l26",
                    "memory" to 2048,
                    "boot" to "order=scsi0"
                )
            )
        }

        override suspend fun performVMAction(
            nodeName: String,
            vmid: Int,
            action: String
        ): ApiResponse<String> {
            return ApiResponse("UPID:fixture:qm$action:$vmid")
        }

        override suspend fun deleteVM(nodeName: String, vmid: Int): ApiResponse<String> {
            return ApiResponse("UPID:fixture:qmdestroy:$vmid")
        }

        override suspend fun getVMSnapshots(
            nodeName: String,
            vmid: Int
        ): ApiResponse<List<VmSnapshot>> {
            return ApiResponse(
                listOf(
                    VmSnapshot(
                        name = "current",
                        description = null,
                        snaptime = null,
                        vmstate = null,
                        parent = null
                    ),
                    VmSnapshot(
                        name = "snap-before-upgrade",
                        description = "Public fixture snapshot",
                        snaptime = 1_700_000_000,
                        vmstate = 0,
                        parent = null
                    )
                )
            )
        }
    }

    private class FakeLxcApi : LxcApi {
        override suspend fun getContainers(nodeName: String): ApiResponse<List<Container>> {
            return ApiResponse(listOf(fakeContainer()))
        }

        override suspend fun getContainerStatus(nodeName: String, vmid: Int): ApiResponse<Container> {
            return ApiResponse(fakeContainer(vmid = vmid))
        }

        override suspend fun performContainerAction(
            nodeName: String,
            vmid: Int,
            action: String
        ): ApiResponse<String> {
            return ApiResponse("UPID:fixture:vz$action:$vmid")
        }

        override suspend fun deleteContainer(nodeName: String, vmid: Int): ApiResponse<String> {
            return ApiResponse("UPID:fixture:vzdestroy:$vmid")
        }

        override suspend fun getLXCSnapshots(
            nodeName: String,
            vmid: Int
        ): ApiResponse<List<LxcSnapshot>> {
            return ApiResponse(
                listOf(
                    LxcSnapshot(
                        name = "current",
                        description = null,
                        snaptime = null,
                        vmstate = null,
                        parent = null
                    ),
                    LxcSnapshot(
                        name = "snap-clean-install",
                        description = "Public fixture snapshot",
                        snaptime = 1_700_100_000,
                        vmstate = 0,
                        parent = null
                    )
                )
            )
        }
    }

    private class FakeStorageApi : StorageApi {
        override suspend fun getStorages(nodeName: String): ApiResponse<List<Storage>> {
            return ApiResponse(
                listOf(
                    Storage(
                        storage = "local-fixture",
                        type = "dir",
                        content = listOf("iso", "backup"),
                        nodes = listOf(nodeName),
                        shared = false,
                        active = true,
                        available = 80L * 1024L * 1024L * 1024L,
                        used = 20L * 1024L * 1024L * 1024L,
                        total = 100L * 1024L * 1024L * 1024L
                    )
                )
            )
        }

        override suspend fun getStorageContent(
            nodeName: String,
            storageName: String
        ): ApiResponse<List<StorageContent>> {
            return ApiResponse(
                listOf(
                    StorageContent(
                        volid = "$storageName:iso/proxmox-mobile-fixture.iso",
                        content = "iso",
                        size = 512L * 1024L * 1024L,
                        format = "iso",
                        ctime = 1_700_000_000,
                        notes = "Public fixture ISO",
                        vmid = null,
                        used = null,
                        parent = null,
                        protectedContent = null
                    )
                )
            )
        }
    }

    private class FakeNetworkApi : NetworkApi {
        override suspend fun getNetworkInterfaces(nodeName: String): ApiResponse<List<NetworkInterface>> {
            return ApiResponse(
                listOf(
                    NetworkInterface(
                        iface = "vmbr-fixture",
                        type = "bridge",
                        method = "manual",
                        address = null,
                        netmask = null,
                        gateway = null,
                        active = true,
                        autostart = true,
                        exists = true,
                        families = listOf("inet")
                    ),
                    NetworkInterface(
                        iface = "eth-fixture",
                        type = "eth",
                        method = "manual",
                        address = null,
                        netmask = null,
                        gateway = null,
                        active = false,
                        autostart = false,
                        exists = true,
                        families = emptyList()
                    )
                )
            )
        }
    }

    private class FakeUserApi : UserApi {
        override suspend fun getUsers(): ApiResponse<List<User>> {
            return ApiResponse(
                listOf(
                    User(
                        userid = "alpha-fixture@pam",
                        enable = true,
                        expire = null,
                        firstname = "Ada",
                        lastname = "Fixture",
                        email = "ada.fixture@example.test",
                        comment = "Public QA fixture user"
                    ),
                    User(
                        userid = "disabled-fixture@pve",
                        enable = false,
                        expire = null,
                        firstname = null,
                        lastname = null,
                        email = null,
                        comment = "Disabled fixture user"
                    )
                )
            )
        }
    }

    private class FakeBackupApi : BackupApi {
        override suspend fun getStorages(nodeName: String): ApiResponse<List<Storage>> {
            return ApiResponse(
                listOf(
                    Storage(
                        storage = "backup-fixture",
                        type = "dir",
                        content = listOf("backup"),
                        nodes = listOf(nodeName),
                        shared = false,
                        active = true,
                        available = 64L * 1024L * 1024L * 1024L,
                        used = 16L * 1024L * 1024L * 1024L,
                        total = 80L * 1024L * 1024L * 1024L
                    )
                )
            )
        }

        override suspend fun getStorageContent(
            nodeName: String,
            storageName: String
        ): ApiResponse<List<StorageContent>> {
            return ApiResponse(
                listOf(
                    StorageContent(
                        volid = "$storageName:backup/vzdump-qemu-102-fixture.vma.zst",
                        content = "backup",
                        size = 2L * 1024L * 1024L * 1024L,
                        format = "vma.zst",
                        ctime = 1_700_000_000,
                        notes = "Public fixture VM backup",
                        vmid = VM_ID,
                        used = null,
                        parent = null,
                        protectedContent = false
                    )
                )
            )
        }
    }

    private class FakeClusterApi : ClusterApi {
        override suspend fun getClusterStatus(): ApiResponse<List<ClusterStatusEntry>> {
            return ApiResponse(
                listOf(
                    ClusterStatusEntry(
                        type = "cluster",
                        name = "fixture-cluster",
                        nodeid = null,
                        ip = null,
                        local = null,
                        online = null,
                        level = null,
                        quorate = 1,
                        nodes = 2,
                        votes = 2,
                        expected_votes = 2
                    ),
                    ClusterStatusEntry(
                        type = "node",
                        name = LAB_NODE,
                        nodeid = 1,
                        ip = "192.0.2.10",
                        local = 1,
                        online = 1,
                        level = "node",
                        quorate = null,
                        nodes = null,
                        votes = null,
                        expected_votes = null
                    ),
                    ClusterStatusEntry(
                        type = "node",
                        name = "qa-node",
                        nodeid = 2,
                        ip = "192.0.2.11",
                        local = 0,
                        online = 1,
                        level = "node",
                        quorate = null,
                        nodes = null,
                        votes = null,
                        expected_votes = null
                    )
                )
            )
        }
    }

    private class FakeDashboardApi : DashboardApi {
        override suspend fun getNodes(): ApiResponse<List<Node>> {
            return ApiResponse(listOf(fakeNode()))
        }
    }

    private class FakeDashboardTaskSummarySource : DashboardTaskSummarySource {
        override suspend fun getTaskSummary(nodeNames: List<String>): TaskResult<TaskSummary> {
            return TaskResult.Success(
                TaskSummary(
                    nodesChecked = 1,
                    runningCount = 0,
                    recentCount = 2,
                    latestTask = fakeTask(nodeName = LAB_NODE, upid = "UPID:fixture:0002:aptupdate")
                        .copy(type = "aptupdate", status = "OK")
                )
            )
        }
    }

    private class FakeTaskApi : TaskApi {
        override suspend fun getTasks(
            nodeName: String,
            limit: Int,
            start: Int,
            statusFilter: String?,
            typeFilter: String?,
            vmid: Int?
        ): ApiResponse<List<Task>> {
            return ApiResponse(listOf(fakeTask(nodeName)))
        }

        override suspend fun getTaskStatus(nodeName: String, upid: String): ApiResponse<Task> {
            return ApiResponse(fakeTask(nodeName = nodeName, upid = upid))
        }

        override suspend fun getTaskLog(
            nodeName: String,
            upid: String,
            start: Int,
            limit: Int
        ): ApiResponse<List<TaskLogEntry>> {
            return ApiResponse(
                listOf(
                    TaskLogEntry(lineNumber = 1, text = "fixture task entered queue"),
                    TaskLogEntry(lineNumber = 2, text = "fixture task completed")
                )
            )
        }

        override suspend fun abortTask(nodeName: String, upid: String): ApiResponse<Map<String, String>> {
            return ApiResponse(mapOf("status" to "fixture-aborted"))
        }
    }

    private companion object {
        private const val LAB_NODE = "lab-node"
        private const val VM_ID = 102
        private const val VM_NAME = "beta-vm"
        private const val LXC_ID = 202
        private const val LXC_NAME = "beta-lxc"
        private const val TASK_UPID = "UPID:fixture:0001:qmstart:102:tester@pam:"

        private fun fakeNode(nodeName: String = LAB_NODE): Node {
            return Node(
                node = nodeName,
                status = "online",
                cpu = 0.16,
                level = "",
                maxcpu = 8,
                maxmem = 16L * 1024L * 1024L * 1024L,
                mem = 2L * 1024L * 1024L * 1024L,
                ssl_fingerprint = "",
                uptime = 7200
            )
        }

        private fun fakeNodeStatus(nodeName: String): NodeStatus {
            return NodeStatus(
                node = nodeName,
                status = "online",
                cpu = 0.16,
                maxcpu = 8,
                mem = 2L * 1024L * 1024L * 1024L,
                maxmem = 16L * 1024L * 1024L * 1024L,
                uptime = 7200,
                loadavg = listOf(0.1, 0.2, 0.3),
                kversion = "6.8.12",
                pveversion = "8.2.0",
                rootfs = RootFS(
                    avail = 40L * 1024L * 1024L * 1024L,
                    total = 100L * 1024L * 1024L * 1024L,
                    used = 60L * 1024L * 1024L * 1024L,
                    free = 40L * 1024L * 1024L * 1024L
                ),
                swap = Swap(
                    free = 4L * 1024L * 1024L * 1024L,
                    total = 8L * 1024L * 1024L * 1024L,
                    used = 4L * 1024L * 1024L * 1024L
                ),
                idle = 84,
                cpuinfo = NodeCpuInfo(cpus = 8),
                memory = NodeMemory(
                    free = 14L * 1024L * 1024L * 1024L,
                    total = 16L * 1024L * 1024L * 1024L,
                    used = 2L * 1024L * 1024L * 1024L
                )
            )
        }

        private fun fakeVm(vmid: Int = VM_ID): VirtualMachine {
            return VirtualMachine(
                vmid = vmid,
                name = VM_NAME,
                status = "running",
                cpu = 0.12,
                maxcpu = 4,
                mem = 512L * 1024L * 1024L,
                maxmem = 2L * 1024L * 1024L * 1024L,
                uptime = 3600,
                template = false,
                cpus = 2,
                disk = 32L * 1024L * 1024L * 1024L,
                diskread = 1L * 1024L * 1024L,
                diskwrite = 2L * 1024L * 1024L,
                netin = 3L * 1024L * 1024L,
                netout = 4L * 1024L * 1024L,
                qmpstatus = "running",
                running_machine = "pc-q35-fixture",
                running_qemu = "8.1.5",
                tags = "beta"
            )
        }

        private fun fakeContainer(vmid: Int = LXC_ID): Container {
            return Container(
                vmid = vmid,
                name = LXC_NAME,
                status = "running",
                cpu = 0.08,
                maxcpu = 2,
                mem = 256L * 1024L * 1024L,
                maxmem = 1L * 1024L * 1024L * 1024L,
                uptime = 1800,
                template = false,
                cpus = 2,
                disk = 8L * 1024L * 1024L * 1024L,
                diskread = 512L * 1024L,
                diskwrite = 768L * 1024L,
                netin = 1L * 1024L * 1024L,
                netout = 2L * 1024L * 1024L,
                tags = "beta"
            )
        }

        private fun fakeTask(
            nodeName: String = LAB_NODE,
            upid: String = TASK_UPID
        ): Task {
            return Task(
                upid = upid,
                id = VM_ID.toString(),
                node = nodeName,
                pid = 2001,
                pstart = 1_700_000_100,
                type = "qmstart",
                status = "stopped",
                exitstatus = "TASK_OK",
                starttime = 1_700_000_000,
                endtime = 1_700_000_020,
                user = "tester@pam",
                saved = true
            )
        }
    }
}
