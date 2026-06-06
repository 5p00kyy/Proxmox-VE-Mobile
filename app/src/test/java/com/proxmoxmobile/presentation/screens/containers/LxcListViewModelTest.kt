package com.proxmoxmobile.presentation.screens.containers

import com.proxmoxmobile.data.lxc.LxcApi
import com.proxmoxmobile.data.lxc.LxcPowerAction
import com.proxmoxmobile.data.lxc.LxcRepository
import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.Container
import com.proxmoxmobile.data.model.LxcSnapshot
import com.proxmoxmobile.presentation.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LxcListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun startContainer_keepsReturnedTaskNoticeForDetailHandoff() = runTest {
        val api = FakeLxcApi(actionTaskId = "UPID:lab-node:vzstart:201")
        val viewModel = viewModel(api)

        viewModel.startContainer(container(status = "stopped"))

        val state = viewModel.uiState.value
        assertEquals(listOf("start"), api.actionRequests)
        assertEquals(1, api.listRequests)
        assertNull(state.actionInProgress)
        assertEquals(LxcPowerAction.Start, state.pendingActionNotice?.action)
        assertEquals("UPID:lab-node:vzstart:201", state.pendingActionNotice?.taskId)
        assertEquals("UPID:lab-node:vzstart:201", state.lastTaskNotice?.taskId)
    }

    @Test
    fun deleteRunningContainer_stopsBeforeRepositoryCall() = runTest {
        val api = FakeLxcApi()
        val viewModel = viewModel(api, deleteRequiresStoppedMessage = "Stop the container before deletion")

        viewModel.deleteContainer(container(status = "running"))

        val state = viewModel.uiState.value
        assertEquals(emptyList<Int>(), api.deleteRequests)
        assertEquals("Stop the container before deletion", state.errorMessage)
        assertEquals(LxcPowerAction.Delete, state.pendingActionNotice?.action)
        assertEquals("Stop the container before deletion", state.pendingActionNotice?.errorMessage)
        assertNull(state.lastTaskNotice)
    }

    @Test
    fun deleteStoppedContainer_usesDeleteEndpointAndKeepsTaskNotice() = runTest {
        val api = FakeLxcApi(deleteTaskId = "UPID:lab-node:vzdestroy:201")
        val viewModel = viewModel(api)

        viewModel.deleteContainer(container(status = "stopped"))

        val state = viewModel.uiState.value
        assertEquals(listOf(201), api.deleteRequests)
        assertEquals(emptyList<String>(), api.actionRequests)
        assertEquals(LxcPowerAction.Delete, state.pendingActionNotice?.action)
        assertEquals("UPID:lab-node:vzdestroy:201", state.lastTaskNotice?.taskId)
    }

    private fun viewModel(
        api: FakeLxcApi,
        invalidNodeMessage: String = "Invalid node",
        deleteRequiresStoppedMessage: String = "Stop the container before deletion"
    ): LxcListViewModel {
        return LxcListViewModel(
            nodeName = "lab-node",
            repository = LxcRepository(api),
            invalidNodeMessage = invalidNodeMessage,
            deleteRequiresStoppedMessage = deleteRequiresStoppedMessage
        )
    }

    private class FakeLxcApi(
        private val actionTaskId: String = "UPID:lab-node:action:201",
        private val deleteTaskId: String = "UPID:lab-node:delete:201"
    ) : LxcApi {
        val actionRequests = mutableListOf<String>()
        val deleteRequests = mutableListOf<Int>()
        var listRequests = 0
            private set

        override suspend fun getContainers(nodeName: String): ApiResponse<List<Container>> {
            listRequests += 1
            return ApiResponse(listOf(container(status = "running")))
        }

        override suspend fun getContainerStatus(nodeName: String, vmid: Int): ApiResponse<Container> {
            return ApiResponse(container(vmid = vmid))
        }

        override suspend fun performContainerAction(
            nodeName: String,
            vmid: Int,
            action: String
        ): ApiResponse<String> {
            actionRequests += action
            return ApiResponse(actionTaskId)
        }

        override suspend fun deleteContainer(nodeName: String, vmid: Int): ApiResponse<String> {
            deleteRequests += vmid
            return ApiResponse(deleteTaskId)
        }

        override suspend fun getLXCSnapshots(
            nodeName: String,
            vmid: Int
        ): ApiResponse<List<LxcSnapshot>> {
            return ApiResponse(emptyList())
        }
    }

    companion object {
        private fun container(
            vmid: Int = 201,
            name: String = "demo-lxc",
            status: String = "running"
        ): Container {
            return Container(
                vmid = vmid,
                name = name,
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
