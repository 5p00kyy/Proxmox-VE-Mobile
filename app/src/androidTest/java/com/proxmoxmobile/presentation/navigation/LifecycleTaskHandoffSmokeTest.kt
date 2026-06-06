package com.proxmoxmobile.presentation.navigation

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.proxmoxmobile.R
import com.proxmoxmobile.data.lxc.LxcApi
import com.proxmoxmobile.data.lxc.LxcRepository
import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.Container
import com.proxmoxmobile.data.model.LxcSnapshot
import com.proxmoxmobile.data.model.ServerConfig
import com.proxmoxmobile.data.model.VirtualMachine
import com.proxmoxmobile.data.model.VmSnapshot
import com.proxmoxmobile.data.vm.VmApi
import com.proxmoxmobile.data.vm.VmRepository
import com.proxmoxmobile.presentation.screens.containers.ContainerListScreen
import com.proxmoxmobile.presentation.screens.containers.LXC_LAST_TASK_VIEW_TASK_TAG
import com.proxmoxmobile.presentation.screens.vms.VMListScreen
import com.proxmoxmobile.presentation.screens.vms.VM_LAST_TASK_VIEW_TASK_TAG
import com.proxmoxmobile.presentation.theme.ProxmoxTheme
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class LifecycleTaskHandoffSmokeTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun fakeVmLifecycleTaskCardNavigatesToTaskDetailRoute() {
        val taskId = "UPID:qa-node:qmstart:101"

        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.setVmHandoffContent(taskId)

            composeRule.waitUntilAtLeastOneExists(hasText(QA_VM_NAME))
            composeRule.onNodeWithText(text(R.string.vm_start)).performClick()
            composeRule.waitUntilAtLeastOneExists(hasTestTag(VM_LAST_TASK_VIEW_TASK_TAG))
            composeRule.onNodeWithTag(VM_LAST_TASK_VIEW_TASK_TAG).performClick()

            composeRule
                .onNodeWithText(taskProbeText(QA_NODE, taskId))
                .assertIsDisplayed()
        }
    }

    @Test
    fun fakeLxcLifecycleTaskCardNavigatesToTaskDetailRoute() {
        val taskId = "UPID:qa-node:vzstart:201"

        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.setLxcHandoffContent(taskId)

            composeRule.waitUntilAtLeastOneExists(hasText(QA_LXC_NAME))
            composeRule.onNodeWithText(text(R.string.container_start)).performClick()
            composeRule.waitUntilAtLeastOneExists(hasTestTag(LXC_LAST_TASK_VIEW_TASK_TAG))
            composeRule.onNodeWithTag(LXC_LAST_TASK_VIEW_TASK_TAG).performClick()

            composeRule
                .onNodeWithText(taskProbeText(QA_NODE, taskId))
                .assertIsDisplayed()
        }
    }

    @Test
    fun fakeVmLifecycleTaskCardSurvivesActivityRecreation() {
        val taskId = "UPID:qa-node:qmstart:101"

        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.setVmHandoffContent(taskId)

            composeRule.waitUntilAtLeastOneExists(hasText(QA_VM_NAME))
            composeRule.onNodeWithText(text(R.string.vm_start)).performClick()
            composeRule.waitUntilAtLeastOneExists(hasTestTag(VM_LAST_TASK_VIEW_TASK_TAG))

            scenario.recreate()
            scenario.setVmHandoffContent(taskId)

            composeRule.waitUntilAtLeastOneExists(hasTestTag(VM_LAST_TASK_VIEW_TASK_TAG))
            composeRule.onNodeWithTag(VM_LAST_TASK_VIEW_TASK_TAG).performClick()

            composeRule
                .onNodeWithText(taskProbeText(QA_NODE, taskId))
                .assertIsDisplayed()
        }
    }

    @Test
    fun fakeRunningVmDeleteGuardIsVisibleAndDisabled() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.setVmHandoffContent(vmStatus = "running")

            composeRule.waitUntilAtLeastOneExists(hasText(QA_VM_NAME))
            composeRule.waitUntilAtLeastOneExists(hasText(text(R.string.vm_delete_requires_stopped)))
            composeRule.onNodeWithText(text(R.string.vm_delete_requires_stopped)).assertIsNotEnabled()
        }
    }

    @Test
    fun fakeVmFailedStartShowsErrorAndNoTaskHandoff() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.setVmHandoffContent(
                vmStatus = "stopped",
                actionException = IllegalStateException(PROXMOX_ACTION_FAILED)
            )

            composeRule.waitUntilAtLeastOneExists(hasText(QA_VM_NAME))
            composeRule.onNodeWithText(text(R.string.vm_start)).performClick()
            composeRule.waitUntilAtLeastOneExists(hasText(PROXMOX_ACTION_FAILED))
            composeRule.onAllNodesWithTag(VM_LAST_TASK_VIEW_TASK_TAG).assertCountEquals(0)
        }
    }

    @Test
    fun fakeLxcLifecycleTaskCardSurvivesActivityRecreation() {
        val taskId = "UPID:qa-node:vzstart:201"

        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.setLxcHandoffContent(taskId)

            composeRule.waitUntilAtLeastOneExists(hasText(QA_LXC_NAME))
            composeRule.onNodeWithText(text(R.string.container_start)).performClick()
            composeRule.waitUntilAtLeastOneExists(hasTestTag(LXC_LAST_TASK_VIEW_TASK_TAG))

            scenario.recreate()
            scenario.setLxcHandoffContent(taskId)

            composeRule.waitUntilAtLeastOneExists(hasTestTag(LXC_LAST_TASK_VIEW_TASK_TAG))
            composeRule.onNodeWithTag(LXC_LAST_TASK_VIEW_TASK_TAG).performClick()

            composeRule
                .onNodeWithText(taskProbeText(QA_NODE, taskId))
                .assertIsDisplayed()
        }
    }

    @Test
    fun fakeRunningLxcDeleteGuardIsVisibleAndDisabled() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.setLxcHandoffContent(containerStatus = "running")

            composeRule.waitUntilAtLeastOneExists(hasText(QA_LXC_NAME))
            composeRule.waitUntilAtLeastOneExists(hasText(text(R.string.container_delete_requires_stopped)))
            composeRule.onNodeWithText(text(R.string.container_delete_requires_stopped)).assertIsNotEnabled()
        }
    }

    @Test
    fun fakeLxcFailedStartShowsErrorAndNoTaskHandoff() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.setLxcHandoffContent(
                containerStatus = "stopped",
                actionException = IllegalStateException(PROXMOX_ACTION_FAILED)
            )

            composeRule.waitUntilAtLeastOneExists(hasText(QA_LXC_NAME))
            composeRule.onNodeWithText(text(R.string.container_start)).performClick()
            composeRule.waitUntilAtLeastOneExists(hasText(PROXMOX_ACTION_FAILED))
            composeRule.onAllNodesWithTag(LXC_LAST_TASK_VIEW_TASK_TAG).assertCountEquals(0)
        }
    }

    private fun androidx.navigation.NavGraphBuilder.taskDetailProbeDestination() {
        composable(Screen.TaskDetail.route) { backStackEntry ->
            val node = backStackEntry.arguments?.getString("node").orEmpty()
            val upid = backStackEntry.arguments?.getString("upid").orEmpty()
            Text(taskProbeText(node, upid))
        }
    }

    private fun ActivityScenario<ComponentActivity>.setVmHandoffContent(
        taskId: String = "UPID:qa-node:qmstart:101",
        vmStatus: String = "stopped",
        actionException: RuntimeException? = null
    ) {
        onActivity { activity ->
            activity.setContent {
                val navController = rememberNavController()
                val viewModel = fakeAuthenticatedViewModel()

                ProxmoxTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavHost(navController = navController, startDestination = "vm_handoff") {
                            composable("vm_handoff") {
                                VMListScreen(
                                    navController = navController,
                                    viewModel = viewModel,
                                    nodeName = QA_NODE,
                                    repositoryOverride = VmRepository(
                                        FakeVmApi(
                                            taskId = taskId,
                                            vmStatus = vmStatus,
                                            actionException = actionException
                                        )
                                    )
                                )
                            }
                            taskDetailProbeDestination()
                        }
                    }
                }
            }
        }
    }

    private fun ActivityScenario<ComponentActivity>.setLxcHandoffContent(
        taskId: String = "UPID:qa-node:vzstart:201",
        containerStatus: String = "stopped",
        actionException: RuntimeException? = null
    ) {
        onActivity { activity ->
            activity.setContent {
                val navController = rememberNavController()
                val viewModel = fakeAuthenticatedViewModel()

                ProxmoxTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavHost(navController = navController, startDestination = "lxc_handoff") {
                            composable("lxc_handoff") {
                                ContainerListScreen(
                                    navController = navController,
                                    viewModel = viewModel,
                                    nodeName = QA_NODE,
                                    repositoryOverride = LxcRepository(
                                        FakeLxcApi(
                                            taskId = taskId,
                                            containerStatus = containerStatus,
                                            actionException = actionException
                                        )
                                    )
                                )
                            }
                            taskDetailProbeDestination()
                        }
                    }
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

    private class FakeVmApi(
        private val taskId: String,
        private val vmStatus: String,
        private val actionException: RuntimeException?
    ) : VmApi {
        override suspend fun getVirtualMachines(nodeName: String): ApiResponse<List<VirtualMachine>> {
            return ApiResponse(listOf(fakeVm(status = vmStatus)))
        }

        override suspend fun getVMStatus(nodeName: String, vmid: Int): ApiResponse<VirtualMachine> {
            return ApiResponse(fakeVm(vmid = vmid, status = vmStatus))
        }

        override suspend fun getVMConfig(nodeName: String, vmid: Int): ApiResponse<Map<String, Any?>> {
            return ApiResponse(emptyMap())
        }

        override suspend fun performVMAction(
            nodeName: String,
            vmid: Int,
            action: String
        ): ApiResponse<String> {
            actionException?.let { throw it }
            return ApiResponse(taskId)
        }

        override suspend fun deleteVM(nodeName: String, vmid: Int): ApiResponse<String> {
            return ApiResponse("UPID:qa-node:qmdestroy:$vmid")
        }

        override suspend fun getVMSnapshots(
            nodeName: String,
            vmid: Int
        ): ApiResponse<List<VmSnapshot>> {
            return ApiResponse(emptyList())
        }
    }

    private class FakeLxcApi(
        private val taskId: String,
        private val containerStatus: String,
        private val actionException: RuntimeException?
    ) : LxcApi {
        override suspend fun getContainers(nodeName: String): ApiResponse<List<Container>> {
            return ApiResponse(listOf(fakeContainer(status = containerStatus)))
        }

        override suspend fun getContainerStatus(nodeName: String, vmid: Int): ApiResponse<Container> {
            return ApiResponse(fakeContainer(vmid = vmid, status = containerStatus))
        }

        override suspend fun performContainerAction(
            nodeName: String,
            vmid: Int,
            action: String
        ): ApiResponse<String> {
            actionException?.let { throw it }
            return ApiResponse(taskId)
        }

        override suspend fun deleteContainer(nodeName: String, vmid: Int): ApiResponse<String> {
            return ApiResponse("UPID:qa-node:vzdestroy:$vmid")
        }

        override suspend fun getLXCSnapshots(
            nodeName: String,
            vmid: Int
        ): ApiResponse<List<LxcSnapshot>> {
            return ApiResponse(emptyList())
        }
    }

    companion object {
        private const val QA_NODE = "qa-node"
        private const val QA_VM_NAME = "qa-vm"
        private const val QA_LXC_NAME = "qa-lxc"
        private const val PROXMOX_ACTION_FAILED = "Proxmox action failed"

        private fun taskProbeText(node: String, upid: String): String {
            return "Task route reached: $node | $upid"
        }

        private fun fakeVm(
            vmid: Int = 101,
            status: String = "stopped"
        ): VirtualMachine {
            return VirtualMachine(
                vmid = vmid,
                name = QA_VM_NAME,
                status = status,
                cpu = 0.0,
                maxcpu = 2,
                mem = 0,
                maxmem = 1_073_741_824,
                uptime = 0,
                template = false,
                cpus = 2,
                disk = 0,
                diskread = 0,
                diskwrite = 0,
                netin = 0,
                netout = 0,
                qmpstatus = status,
                running_machine = null,
                running_qemu = null,
                tags = null
            )
        }

        private fun fakeContainer(
            vmid: Int = 201,
            status: String = "stopped"
        ): Container {
            return Container(
                vmid = vmid,
                name = QA_LXC_NAME,
                status = status,
                cpu = 0.0,
                maxcpu = 2,
                mem = 0,
                maxmem = 536_870_912,
                uptime = 0,
                template = false,
                cpus = 2,
                disk = 0,
                diskread = 0,
                diskwrite = 0,
                netin = 0,
                netout = 0,
                tags = null
            )
        }
    }
}
