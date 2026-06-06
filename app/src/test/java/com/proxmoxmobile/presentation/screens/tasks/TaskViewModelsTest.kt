package com.proxmoxmobile.presentation.screens.tasks

import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.Task
import com.proxmoxmobile.data.model.TaskLogEntry
import com.proxmoxmobile.data.task.TaskApi
import com.proxmoxmobile.data.task.TaskFilters
import com.proxmoxmobile.data.task.TaskRepository
import com.proxmoxmobile.data.task.TaskStatusFilter
import com.proxmoxmobile.presentation.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialResourceFilter_loadsTasksWithNodeAndVmid() = runTest {
        val api = FakeTaskApi(tasks = listOf(task(upid = "UPID:lab-node:task:101")))
        val viewModel = taskListViewModel(
            api = api,
            initialNodeName = "lab-node",
            initialFilters = TaskFilters(vmid = 101)
        )

        viewModel.loadTasks()

        assertEquals(listOf("lab-node"), api.taskRequests.map { it.nodeName })
        assertEquals(101, api.taskRequests.single().vmid)
        assertEquals(listOf("UPID:lab-node:task:101"), viewModel.uiState.value.tasks.map { it.id })
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun applyFilters_forwardsStatusTypeAndVmidToRepository() = runTest {
        val api = FakeTaskApi()
        val viewModel = taskListViewModel(api = api)

        viewModel.applyFilters(
            TaskFilters(
                status = TaskStatusFilter.Running,
                typeFilter = " qmstart ",
                vmid = 102
            )
        )

        val request = api.taskRequests.single()
        assertEquals("running", request.statusFilter)
        assertEquals("qmstart", request.typeFilter)
        assertEquals(102, request.vmid)
        assertEquals(TaskStatusFilter.Running, viewModel.uiState.value.filters.status)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun initialFilters_arePreservedWhenSelectingAnotherNode() = runTest {
        val api = FakeTaskApi()
        val filters = TaskFilters(
            status = TaskStatusFilter.Running,
            typeFilter = " vzdump ",
            vmid = 101
        )
        val viewModel = taskListViewModel(
            api = api,
            availableNodes = listOf("node-a", "node-b"),
            initialNodeName = "node-a",
            initialFilters = filters
        )

        viewModel.loadTasks()
        viewModel.selectNode("node-b")

        assertEquals("node-b", viewModel.uiState.value.selectedNode)
        assertEquals(filters, viewModel.uiState.value.filters)
        assertEquals(
            listOf(
                TaskRequest(
                    nodeName = "node-a",
                    limit = 100,
                    statusFilter = "running",
                    typeFilter = "vzdump",
                    vmid = 101
                ),
                TaskRequest(
                    nodeName = "node-b",
                    limit = 100,
                    statusFilter = "running",
                    typeFilter = "vzdump",
                    vmid = 101
                )
            ),
            api.taskRequests
        )
    }

    @Test
    fun clearFilters_resetsDraftAndRefreshesWithoutFilterParameters() = runTest {
        val api = FakeTaskApi()
        val viewModel = taskListViewModel(api = api)

        viewModel.applyFilters(
            TaskFilters(
                status = TaskStatusFilter.Finished,
                typeFilter = "qmstop",
                vmid = 102
            )
        )
        viewModel.clearFilters()

        assertEquals(TaskFilters(), viewModel.uiState.value.filters)
        assertEquals(
            listOf(
                TaskRequest(
                    nodeName = "lab-node",
                    limit = 100,
                    statusFilter = "stopped",
                    typeFilter = "qmstop",
                    vmid = 102
                ),
                TaskRequest(
                    nodeName = "lab-node",
                    limit = 100,
                    statusFilter = null,
                    typeFilter = null,
                    vmid = null
                )
            ),
            api.taskRequests
        )
    }

    @Test
    fun abortTask_keepsUpidNoticeForDetailHandoffAndRefreshesWithActiveFilters() = runTest {
        val api = FakeTaskApi()
        val filters = TaskFilters(
            status = TaskStatusFilter.Running,
            typeFilter = "vzdump",
            vmid = 101
        )
        val viewModel = taskListViewModel(api = api, initialFilters = filters)
        val task = task(upid = "UPID:lab-node:vzdump:101", type = "vzdump", status = "running")

        viewModel.abortTask(task)

        assertEquals(listOf("UPID:lab-node:vzdump:101"), api.abortRequests.map { it.upid })
        assertEquals("UPID:lab-node:vzdump:101", viewModel.uiState.value.pendingActionNotice?.upid)
        assertEquals("vzdump", viewModel.uiState.value.pendingActionNotice?.type)
        assertNull(viewModel.uiState.value.pendingActionNotice?.errorMessage)
        assertEquals(
            listOf(
                TaskRequest(
                    nodeName = "lab-node",
                    limit = 100,
                    statusFilter = "running",
                    typeFilter = "vzdump",
                    vmid = 101
                )
            ),
            api.taskRequests
        )

        viewModel.consumeActionNotice()

        assertNull(viewModel.uiState.value.pendingActionNotice)
    }

    @Test
    fun duplicateTaskAbortWhileFirstIsInProgress_isSuppressed() = runTest {
        val gate = CompletableDeferred<Unit>()
        val api = FakeTaskApi().apply { abortGate = gate }
        val viewModel = taskListViewModel(api = api)
        val task = task(upid = "UPID:lab-node:abort:100")

        viewModel.abortTask(task)
        viewModel.abortTask(task)

        assertEquals("UPID:lab-node:abort:100", viewModel.uiState.value.actionInProgressUpid)
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("UPID:lab-node:abort:100"), api.abortRequests.map { it.upid })
        assertNull(viewModel.uiState.value.actionInProgressUpid)
        assertEquals("UPID:lab-node:abort:100", viewModel.uiState.value.pendingActionNotice?.upid)
    }

    @Test
    fun taskAbortWithoutSelectedNode_surfacesInvalidTaskMessage() = runTest {
        val api = FakeTaskApi()
        val viewModel = taskListViewModel(
            api = api,
            availableNodes = emptyList(),
            initialNodeName = null
        )

        viewModel.abortTask(task(upid = "UPID:lab-node:abort:100"))

        assertEquals(emptyList<AbortRequest>(), api.abortRequests)
        assertEquals("Invalid task", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun duplicateTaskDetailAbortWhileFirstIsInProgress_isSuppressed() = runTest {
        val gate = CompletableDeferred<Unit>()
        val api = FakeTaskApi().apply { abortGate = gate }
        val viewModel = taskDetailViewModel(api = api, upid = "UPID:lab-node:abort:100")

        viewModel.abortTask()
        viewModel.abortTask()

        assertTrue(viewModel.uiState.value.isAborting)
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("UPID:lab-node:abort:100"), api.abortRequests.map { it.upid })
        assertEquals(false, viewModel.uiState.value.isAborting)
        assertEquals("UPID:lab-node:abort:100", viewModel.uiState.value.pendingActionNotice?.upid)
    }

    @Test
    fun taskDetailViewModel_usesRouteUpidForLoadAndAbortNotice() = runTest {
        val api = FakeTaskApi(
            taskStatus = task(upid = null, type = "qmstart", status = "running"),
            logEntries = listOf(TaskLogEntry(lineNumber = 1, text = "started"))
        )
        val viewModel = taskDetailViewModel(api = api, upid = "UPID:lab-node:qmstart:101")

        viewModel.loadTaskDetail()
        viewModel.abortTask()

        assertEquals(
            listOf(
                TaskDetailRequest("lab-node", "UPID:lab-node:qmstart:101"),
                TaskDetailRequest("lab-node", "UPID:lab-node:qmstart:101")
            ),
            api.detailRequests
        )
        assertEquals(listOf("UPID:lab-node:qmstart:101"), api.abortRequests.map { it.upid })
        assertEquals("UPID:lab-node:qmstart:101", viewModel.uiState.value.detail?.upid)
        assertEquals("UPID:lab-node:qmstart:101", viewModel.uiState.value.pendingActionNotice?.upid)
        assertEquals("qmstart", viewModel.uiState.value.pendingActionNotice?.type)
        assertNull(viewModel.uiState.value.pendingActionNotice?.errorMessage)
    }

    private fun taskListViewModel(
        api: FakeTaskApi,
        availableNodes: List<String> = listOf("lab-node"),
        initialNodeName: String? = "lab-node",
        initialFilters: TaskFilters = TaskFilters()
    ): TaskListViewModel {
        return TaskListViewModel(
            availableNodes = availableNodes,
            initialNodeName = initialNodeName,
            initialFilters = initialFilters,
            repository = TaskRepository(api),
            noNodesMessage = "No nodes",
            invalidTaskMessage = "Invalid task"
        )
    }

    private fun taskDetailViewModel(
        api: FakeTaskApi,
        nodeName: String = "lab-node",
        upid: String = "UPID:lab-node:task:100"
    ): TaskDetailViewModel {
        return TaskDetailViewModel(
            nodeName = nodeName,
            upid = upid,
            repository = TaskRepository(api),
            invalidTaskMessage = "Invalid task"
        )
    }

    private class FakeTaskApi(
        private val tasks: List<Task> = listOf(task()),
        private val taskStatus: Task? = null,
        private val logEntries: List<TaskLogEntry> = emptyList()
    ) : TaskApi {
        val taskRequests = mutableListOf<TaskRequest>()
        val detailRequests = mutableListOf<TaskDetailRequest>()
        val abortRequests = mutableListOf<AbortRequest>()
        var abortGate: CompletableDeferred<Unit>? = null

        override suspend fun getTasks(
            nodeName: String,
            limit: Int,
            start: Int,
            statusFilter: String?,
            typeFilter: String?,
            vmid: Int?
        ): ApiResponse<List<Task>> {
            taskRequests += TaskRequest(nodeName, limit, statusFilter, typeFilter, vmid)
            return ApiResponse(tasks)
        }

        override suspend fun getTaskStatus(nodeName: String, upid: String): ApiResponse<Task> {
            detailRequests += TaskDetailRequest(nodeName, upid)
            return ApiResponse(taskStatus ?: task(upid = upid))
        }

        override suspend fun getTaskLog(
            nodeName: String,
            upid: String,
            start: Int,
            limit: Int
        ): ApiResponse<List<TaskLogEntry>> {
            return ApiResponse(logEntries)
        }

        override suspend fun abortTask(nodeName: String, upid: String): ApiResponse<Map<String, String>> {
            abortGate?.await()
            abortRequests += AbortRequest(nodeName, upid)
            return ApiResponse(emptyMap())
        }
    }

    private data class TaskRequest(
        val nodeName: String,
        val limit: Int,
        val statusFilter: String?,
        val typeFilter: String?,
        val vmid: Int?
    )

    private data class AbortRequest(
        val nodeName: String,
        val upid: String
    )

    private data class TaskDetailRequest(
        val nodeName: String,
        val upid: String
    )

    private companion object {
        fun task(
            upid: String? = "UPID:lab-node:task:100",
            status: String = "running",
            type: String = "qmstart"
        ): Task {
            return Task(
                upid = upid,
                id = upid ?: "100",
                node = "lab-node",
                pid = 1234,
                pstart = 1,
                type = type,
                status = status,
                exitstatus = null,
                starttime = 2,
                endtime = null,
                user = "tester@pam",
                saved = false
            )
        }
    }
}
