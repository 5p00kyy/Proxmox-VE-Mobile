package com.proxmoxmobile.presentation.navigation

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
import com.proxmoxmobile.data.model.User
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
import com.proxmoxmobile.data.vm.VmApi
import com.proxmoxmobile.data.vm.VmRepository
import com.proxmoxmobile.presentation.viewmodel.MainViewModel

internal object NavigationSmokeFixtures {
    const val LAB_NODE = "lab-node"
    const val QA_NODE = "qa-node"
    const val VM_ID = 102
    const val VM_NAME = "beta-vm"
    const val LXC_ID = 202
    const val LXC_NAME = "beta-lxc"
    const val TASK_UPID = "UPID:fixture:0001:qmstart:102:tester@pam:"

    fun fakeAuthenticatedViewModel(): MainViewModel {
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

    fun fakeNodeRepository(): NodeRepository = NodeRepository(FakeNodeApi())

    fun fakeVmRepository(): VmRepository = VmRepository(FakeVmApi())

    fun fakeLxcRepository(): LxcRepository = LxcRepository(FakeLxcApi())

    fun fakeTaskRepository(): TaskRepository = TaskRepository(FakeTaskApi())

    fun fakeStorageRepository(
        storageContentForName: (String) -> List<StorageContent> = { storageName ->
            listOf(fakeStorageContent(storageName))
        }
    ): StorageRepository {
        return StorageRepository(FakeStorageApi(storageContentForName = storageContentForName))
    }

    fun fakeNetworkRepository(
        interfaces: List<NetworkInterface> = fakeNetworkInterfaces()
    ): NetworkRepository {
        return NetworkRepository(FakeNetworkApi(interfaces = interfaces))
    }

    fun fakeUserRepository(
        users: List<User> = fakeUsers()
    ): UserRepository {
        return UserRepository(FakeUserApi(users = users))
    }

    fun fakeBackupRepository(
        storageContentForName: (String) -> List<StorageContent> = { storageName ->
            listOf(fakeBackupContent(storageName))
        }
    ): BackupRepository {
        return BackupRepository(FakeBackupApi(storageContentForName = storageContentForName))
    }

    fun fakeClusterRepository(): ClusterRepository = ClusterRepository(FakeClusterApi())

    fun failingClusterRepository(): ClusterRepository = ClusterRepository(FailingClusterApi())

    fun fakeDashboardRepository(
        taskSummaryResult: TaskResult<TaskSummary> = fakeDashboardTaskSummary()
    ): DashboardRepository {
        return DashboardRepository(
            api = FakeDashboardApi(),
            taskSummarySource = FakeDashboardTaskSummarySource(taskSummaryResult)
        )
    }

    fun fakeNode(nodeName: String = LAB_NODE): Node {
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

    fun fakeTask(
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

    private fun fakeDashboardTaskSummary(): TaskResult<TaskSummary> {
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

    private fun fakeStorage(): Storage {
        return Storage(
            storage = "local-fixture",
            type = "dir",
            content = listOf("iso", "backup"),
            nodes = listOf(LAB_NODE),
            shared = false,
            active = true,
            available = 80L * 1024L * 1024L * 1024L,
            used = 20L * 1024L * 1024L * 1024L,
            total = 100L * 1024L * 1024L * 1024L
        )
    }

    private fun fakeStorageContent(storageName: String): StorageContent {
        return StorageContent(
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
    }

    private fun fakeBackupStorage(): Storage {
        return Storage(
            storage = "backup-fixture",
            type = "dir",
            content = listOf("backup"),
            nodes = listOf(LAB_NODE),
            shared = false,
            active = true,
            available = 64L * 1024L * 1024L * 1024L,
            used = 16L * 1024L * 1024L * 1024L,
            total = 80L * 1024L * 1024L * 1024L
        )
    }

    private fun fakeBackupContent(storageName: String): StorageContent {
        return StorageContent(
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
    }

    private fun fakeNetworkInterfaces(): List<NetworkInterface> {
        return listOf(
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
    }

    private fun fakeUsers(): List<User> {
        return listOf(
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

    private class FakeStorageApi(
        private val storages: List<Storage> = listOf(fakeStorage()),
        private val storageContentForName: (String) -> List<StorageContent>
    ) : StorageApi {
        override suspend fun getStorages(nodeName: String): ApiResponse<List<Storage>> {
            return ApiResponse(storages)
        }

        override suspend fun getStorageContent(
            nodeName: String,
            storageName: String
        ): ApiResponse<List<StorageContent>> {
            return ApiResponse(storageContentForName(storageName))
        }
    }

    private class FakeNetworkApi(
        private val interfaces: List<NetworkInterface>
    ) : NetworkApi {
        override suspend fun getNetworkInterfaces(nodeName: String): ApiResponse<List<NetworkInterface>> {
            return ApiResponse(interfaces)
        }
    }

    private class FakeUserApi(
        private val users: List<User>
    ) : UserApi {
        override suspend fun getUsers(): ApiResponse<List<User>> {
            return ApiResponse(users)
        }
    }

    private class FakeBackupApi(
        private val storages: List<Storage> = listOf(fakeBackupStorage()),
        private val storageContentForName: (String) -> List<StorageContent>
    ) : BackupApi {
        override suspend fun getStorages(nodeName: String): ApiResponse<List<Storage>> {
            return ApiResponse(storages)
        }

        override suspend fun getStorageContent(
            nodeName: String,
            storageName: String
        ): ApiResponse<List<StorageContent>> {
            return ApiResponse(storageContentForName(storageName))
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
                        name = QA_NODE,
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

    private class FailingClusterApi : ClusterApi {
        override suspend fun getClusterStatus(): ApiResponse<List<ClusterStatusEntry>> {
            throw IllegalStateException("Cluster fixture unavailable")
        }
    }

    private class FakeDashboardApi(
        private val nodes: List<Node> = listOf(fakeNode())
    ) : DashboardApi {
        override suspend fun getNodes(): ApiResponse<List<Node>> {
            return ApiResponse(nodes)
        }
    }

    private class FakeDashboardTaskSummarySource(
        private val result: TaskResult<TaskSummary>
    ) : DashboardTaskSummarySource {
        override suspend fun getTaskSummary(nodeNames: List<String>): TaskResult<TaskSummary> {
            return result
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
}
