package com.proxmoxmobile.presentation.navigation

import androidx.activity.ComponentActivity
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
import com.proxmoxmobile.data.lxc.LxcApi
import com.proxmoxmobile.data.lxc.LxcRepository
import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.Container
import com.proxmoxmobile.data.model.LxcSnapshot
import com.proxmoxmobile.data.model.NodeCpuInfo
import com.proxmoxmobile.data.model.NodeMemory
import com.proxmoxmobile.data.model.NodeStatus
import com.proxmoxmobile.data.model.RootFS
import com.proxmoxmobile.data.model.ServerConfig
import com.proxmoxmobile.data.model.Swap
import com.proxmoxmobile.data.model.VirtualMachine
import com.proxmoxmobile.data.model.VmSnapshot
import com.proxmoxmobile.data.node.NodeApi
import com.proxmoxmobile.data.node.NodeRepository
import com.proxmoxmobile.data.vm.VmApi
import com.proxmoxmobile.data.vm.VmRepository
import com.proxmoxmobile.presentation.theme.ProxmoxTheme
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
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

    private fun startFakeAuthenticatedRoute(
        route: String,
        vmRepositoryOverride: VmRepository? = null,
        lxcRepositoryOverride: LxcRepository? = null,
        nodeRepositoryOverride: NodeRepository? = null
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
                        nodeRepositoryOverride = nodeRepositoryOverride
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

    private companion object {
        private const val LAB_NODE = "lab-node"
        private const val VM_ID = 102
        private const val VM_NAME = "beta-vm"
        private const val LXC_ID = 202
        private const val LXC_NAME = "beta-lxc"

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
    }
}
