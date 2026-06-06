package com.proxmoxmobile.data.task

import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.Task
import com.proxmoxmobile.data.model.TaskLogEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskRepositoryTest {
    @Test
    fun getTasks_filtersInvalidRowsAndSortsNewestFirst() = runBlocking {
        val repository = TaskRepository(
            FakeTaskApi(
                tasks = listOf(
                    task(upid = "UPID:pve:old", id = "100", starttime = 10),
                    task(upid = "UPID:pve:new", id = "101", starttime = 20),
                    task(upid = null, id = "102", starttime = 30),
                    task(upid = "UPID:pve:missing-node", node = "", starttime = 40),
                    task(upid = "UPID:pve:missing-status", status = "", starttime = 50)
                )
            )
        )

        val result = repository.getTasks("pve")

        assertTrue(result is TaskResult.Success)
        val tasks = (result as TaskResult.Success).data
        assertEquals(listOf("UPID:pve:new", "UPID:pve:old"), tasks.map { it.taskUpid() })
    }

    @Test
    fun getTaskDetail_returnsStatusAndOrderedNonBlankLogEntries() = runBlocking {
        val repository = TaskRepository(
            FakeTaskApi(
                taskStatus = task(upid = "UPID:pve:task", status = "running"),
                logEntries = listOf(
                    TaskLogEntry(lineNumber = 2, text = "second"),
                    TaskLogEntry(lineNumber = 1, text = "first"),
                    TaskLogEntry(lineNumber = 3, text = "")
                )
            )
        )

        val result = repository.getTaskDetail("pve", "UPID:pve:task")

        assertTrue(result is TaskResult.Success)
        val detail = (result as TaskResult.Success).data
        assertEquals("UPID:pve:task", detail.upid)
        assertEquals("running", detail.task.status)
        assertEquals(listOf("first", "second"), detail.logEntries.map { it.text })
    }

    @Test
    fun getTaskDetail_usesRouteUpidWhenStatusPayloadDoesNotIncludeUpid() = runBlocking {
        val repository = TaskRepository(
            FakeTaskApi(
                taskStatus = task(upid = null, id = "100", status = "running")
            )
        )

        val result = repository.getTaskDetail("pve", "UPID:pve:qmstart:100")

        assertTrue(result is TaskResult.Success)
        val detail = (result as TaskResult.Success).data
        assertEquals("UPID:pve:qmstart:100", detail.upid)
        assertEquals("100", detail.task.id)
    }

    @Test
    fun getTasks_forwardsTaskFiltersToApi() = runBlocking {
        val api = FakeTaskApi()
        val repository = TaskRepository(api)

        val result = repository.getTasks(
            nodeName = "pve",
            filters = TaskFilters(
                status = TaskStatusFilter.Running,
                typeFilter = " vzdump ",
                vmid = 100
            )
        )

        assertTrue(result is TaskResult.Success)
        assertEquals(
            TaskRequest(
                nodeName = "pve",
                limit = 100,
                start = 0,
                statusFilter = "running",
                typeFilter = "vzdump",
                vmid = 100
            ),
            api.requests.single()
        )
    }

    @Test
    fun getTasks_mapsFinishedFilterToStoppedApiStatus() = runBlocking {
        val api = FakeTaskApi()
        val repository = TaskRepository(api)

        val result = repository.getTasks(
            nodeName = "pve",
            filters = TaskFilters(status = TaskStatusFilter.Finished)
        )

        assertTrue(result is TaskResult.Success)
        assertEquals("stopped", api.requests.single().statusFilter)
    }

    @Test
    fun getTaskSummary_aggregatesAcrossDistinctNodes() = runBlocking {
        val api = FakeTaskApi(
            tasksByNode = mapOf(
                "pve-a" to listOf(
                    task(upid = "UPID:pve-a:running", node = "pve-a", status = "running", starttime = 30),
                    task(upid = "UPID:pve-a:old", node = "pve-a", status = "finished", starttime = 10)
                ),
                "pve-b" to listOf(
                    task(upid = "UPID:pve-b:new", node = "pve-b", status = "finished", starttime = 50),
                    task(upid = null, node = "pve-b", status = "running", starttime = 60)
                )
            )
        )
        val repository = TaskRepository(api)

        val result = repository.getTaskSummary(listOf("pve-a", "pve-b", "pve-a"))

        assertTrue(result is TaskResult.Success)
        val summary = (result as TaskResult.Success).data
        assertEquals(2, summary.nodesChecked)
        assertEquals(1, summary.runningCount)
        assertEquals(3, summary.recentCount)
        assertEquals("UPID:pve-b:new", summary.latestTask?.taskUpid())
        assertEquals(listOf("pve-a", "pve-b"), api.requestedNodes)
    }

    @Test
    fun abortTask_usesUpid() = runBlocking {
        val api = FakeTaskApi()
        val repository = TaskRepository(api)

        val result = repository.abortTask("pve", "UPID:pve:task")

        assertTrue(result is TaskResult.Success)
        assertEquals("UPID:pve:task", api.abortedUpid)
    }

    @Test
    fun getTasks_reportsMissingApiServiceAsNotAuthenticated() = runBlocking {
        val repository = TaskRepository(
            ProxmoxTaskApi { null }
        )

        val result = repository.getTasks("pve")

        assertTrue(result is TaskResult.Error)
        assertEquals("Not authenticated", (result as TaskResult.Error).message)
    }

    private class FakeTaskApi(
        private val tasks: List<Task> = listOf(task()),
        private val tasksByNode: Map<String, List<Task>> = emptyMap(),
        private val taskStatus: Task = task(),
        private val logEntries: List<TaskLogEntry> = emptyList()
    ) : TaskApi {
        var abortedUpid: String? = null
            private set
        val requests = mutableListOf<TaskRequest>()
        val requestedNodes: List<String>
            get() = requests.map { it.nodeName }

        override suspend fun getTasks(
            nodeName: String,
            limit: Int,
            start: Int,
            statusFilter: String?,
            typeFilter: String?,
            vmid: Int?
        ): ApiResponse<List<Task>> {
            requests += TaskRequest(
                nodeName = nodeName,
                limit = limit,
                start = start,
                statusFilter = statusFilter,
                typeFilter = typeFilter,
                vmid = vmid
            )
            return ApiResponse(tasksByNode[nodeName] ?: tasks)
        }

        override suspend fun getTaskStatus(nodeName: String, upid: String): ApiResponse<Task> {
            return ApiResponse(taskStatus)
        }

        override suspend fun getTaskLog(
            nodeName: String,
            upid: String,
            start: Int,
            limit: Int
        ): ApiResponse<List<TaskLogEntry>> {
            return ApiResponse(logEntries)
        }

        override suspend fun abortTask(
            nodeName: String,
            upid: String
        ): ApiResponse<Map<String, String>> {
            abortedUpid = upid
            return ApiResponse(emptyMap())
        }
    }

    private data class TaskRequest(
        val nodeName: String,
        val limit: Int,
        val start: Int,
        val statusFilter: String?,
        val typeFilter: String?,
        val vmid: Int?
    )

    companion object {
        private fun task(
            upid: String? = "UPID:pve:task",
            id: String = "100",
            node: String = "pve",
            type: String = "qmstart",
            status: String = "finished",
            starttime: Long = 100
        ): Task {
            return Task(
                upid = upid,
                id = id,
                node = node,
                pid = 1234,
                pstart = 10,
                type = type,
                status = status,
                exitstatus = "OK",
                starttime = starttime,
                endtime = starttime + 5,
                user = "root@pam",
                saved = true
            )
        }
    }
}
