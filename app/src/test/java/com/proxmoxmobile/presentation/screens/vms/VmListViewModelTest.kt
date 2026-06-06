package com.proxmoxmobile.presentation.screens.vms

import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.VirtualMachine
import com.proxmoxmobile.data.model.VmSnapshot
import com.proxmoxmobile.data.vm.VmApi
import com.proxmoxmobile.data.vm.VmPowerAction
import com.proxmoxmobile.data.vm.VmRepository
import com.proxmoxmobile.presentation.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VmListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun startVirtualMachine_keepsReturnedTaskNoticeForDetailHandoff() = runTest {
        val api = FakeVmApi(actionTaskId = "UPID:lab-node:qmstart:101")
        val viewModel = viewModel(api)

        viewModel.startVirtualMachine(vm(status = "stopped"))

        val state = viewModel.uiState.value
        assertEquals(listOf("start"), api.actionRequests)
        assertEquals(1, api.listRequests)
        assertNull(state.actionInProgress)
        assertEquals(VmPowerAction.Start, state.pendingActionNotice?.action)
        assertEquals("UPID:lab-node:qmstart:101", state.pendingActionNotice?.taskId)
        assertEquals("UPID:lab-node:qmstart:101", state.lastTaskNotice?.taskId)
    }

    @Test
    fun lifecycleVirtualMachineActions_keepReturnedTaskNoticeForDetailHandoff() = runTest {
        val cases = listOf(
            VmPowerAction.Shutdown to VmListViewModel::shutdownVirtualMachine,
            VmPowerAction.Stop to VmListViewModel::stopVirtualMachine,
            VmPowerAction.Reboot to VmListViewModel::rebootVirtualMachine
        )

        cases.forEach { (action, runAction) ->
            val taskId = "UPID:lab-node:${action.apiValue}:101"
            val api = FakeVmApi(actionTaskId = taskId)
            val viewModel = viewModel(api)

            runAction(viewModel, vm(status = "running"))

            val state = viewModel.uiState.value
            assertEquals("action ${action.apiValue}", listOf(action.apiValue), api.actionRequests)
            assertEquals("refresh ${action.apiValue}", 1, api.listRequests)
            assertNull("progress ${action.apiValue}", state.actionInProgress)
            assertEquals("pending action ${action.apiValue}", action, state.pendingActionNotice?.action)
            assertEquals("pending task ${action.apiValue}", taskId, state.pendingActionNotice?.taskId)
            assertEquals("last task ${action.apiValue}", taskId, state.lastTaskNotice?.taskId)
        }
    }

    @Test
    fun actionWithBlankTaskId_keepsPendingNoticeWithoutLastTaskHandoff() = runTest {
        val api = FakeVmApi(actionTaskId = "")
        val viewModel = viewModel(api)

        viewModel.startVirtualMachine(vm(status = "stopped"))

        val state = viewModel.uiState.value
        assertEquals(listOf("start"), api.actionRequests)
        assertEquals(VmPowerAction.Start, state.pendingActionNotice?.action)
        assertNull(state.pendingActionNotice?.taskId)
        assertNull(state.lastTaskNotice)
    }

    @Test
    fun failedActionClearsStaleLastTaskNotice() = runTest {
        val api = FakeVmApi(actionTaskId = "UPID:lab-node:qmstart:101")
        val viewModel = viewModel(api)
        viewModel.startVirtualMachine(vm(status = "stopped"))
        assertEquals("UPID:lab-node:qmstart:101", viewModel.uiState.value.lastTaskNotice?.taskId)

        api.actionException = IllegalStateException("Proxmox action failed")
        viewModel.shutdownVirtualMachine(vm(status = "running"))

        val state = viewModel.uiState.value
        assertEquals("Proxmox action failed", state.errorMessage)
        assertEquals(VmPowerAction.Shutdown, state.pendingActionNotice?.action)
        assertEquals("Proxmox action failed", state.pendingActionNotice?.errorMessage)
        assertNull(state.pendingActionNotice?.taskId)
        assertNull(state.lastTaskNotice)
    }

    @Test
    fun deleteRunningVirtualMachine_stopsBeforeRepositoryCall() = runTest {
        val api = FakeVmApi()
        val viewModel = viewModel(api, deleteRequiresStoppedMessage = "Stop the VM before deletion")

        viewModel.deleteVirtualMachine(vm(status = "running"))

        val state = viewModel.uiState.value
        assertEquals(emptyList<Int>(), api.deleteRequests)
        assertEquals("Stop the VM before deletion", state.errorMessage)
        assertEquals(VmPowerAction.Delete, state.pendingActionNotice?.action)
        assertEquals("Stop the VM before deletion", state.pendingActionNotice?.errorMessage)
        assertNull(state.lastTaskNotice)
    }

    @Test
    fun deleteStoppedVirtualMachine_usesDeleteEndpointAndKeepsTaskNotice() = runTest {
        val api = FakeVmApi(deleteTaskId = "UPID:lab-node:qmdestroy:101")
        val viewModel = viewModel(api)

        viewModel.deleteVirtualMachine(vm(status = "stopped"))

        val state = viewModel.uiState.value
        assertEquals(listOf(101), api.deleteRequests)
        assertEquals(emptyList<String>(), api.actionRequests)
        assertEquals(VmPowerAction.Delete, state.pendingActionNotice?.action)
        assertEquals("UPID:lab-node:qmdestroy:101", state.lastTaskNotice?.taskId)
    }

    private fun viewModel(
        api: FakeVmApi,
        deleteRequiresStoppedMessage: String = "Stop the VM before deletion"
    ): VmListViewModel {
        return VmListViewModel(
            nodeName = "lab-node",
            repository = VmRepository(api),
            deleteRequiresStoppedMessage = deleteRequiresStoppedMessage
        )
    }

    private class FakeVmApi(
        private val actionTaskId: String = "UPID:lab-node:action:101",
        private val deleteTaskId: String = "UPID:lab-node:delete:101",
        var actionException: Exception? = null,
        var deleteException: Exception? = null
    ) : VmApi {
        val actionRequests = mutableListOf<String>()
        val deleteRequests = mutableListOf<Int>()
        var listRequests = 0
            private set

        override suspend fun getVirtualMachines(nodeName: String): ApiResponse<List<VirtualMachine>> {
            listRequests += 1
            return ApiResponse(listOf(vm(status = "running")))
        }

        override suspend fun getVMStatus(nodeName: String, vmid: Int): ApiResponse<VirtualMachine> {
            return ApiResponse(vm(vmid = vmid))
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
            actionRequests += action
            return ApiResponse(actionTaskId)
        }

        override suspend fun deleteVM(nodeName: String, vmid: Int): ApiResponse<String> {
            deleteException?.let { throw it }
            deleteRequests += vmid
            return ApiResponse(deleteTaskId)
        }

        override suspend fun getVMSnapshots(
            nodeName: String,
            vmid: Int
        ): ApiResponse<List<VmSnapshot>> {
            return ApiResponse(emptyList())
        }
    }

    companion object {
        private fun vm(
            vmid: Int = 101,
            name: String = "demo-vm",
            status: String = "running"
        ): VirtualMachine {
            return VirtualMachine(
                vmid = vmid,
                name = name,
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
    }
}
