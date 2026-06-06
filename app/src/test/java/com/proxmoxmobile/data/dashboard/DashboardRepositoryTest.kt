package com.proxmoxmobile.data.dashboard

import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.Node
import com.proxmoxmobile.data.model.Task
import com.proxmoxmobile.data.task.TaskResult
import com.proxmoxmobile.data.task.TaskSummary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardRepositoryTest {
    @Test
    fun getDashboardSnapshot_filtersInvalidNodesAndRequestsTaskSummaryForValidNodes() = runBlocking {
        val summarySource = FakeTaskSummarySource(
            result = TaskResult.Success(
                TaskSummary(
                    nodesChecked = 2,
                    runningCount = 1,
                    recentCount = 3,
                    latestTask = task(node = "pve-b")
                )
            )
        )
        val repository = DashboardRepository(
            api = FakeDashboardApi(
                nodes = listOf(
                    node(node = "pve-b", status = "online"),
                    node(node = "", status = "online"),
                    node(node = "pve-a", status = "online"),
                    node(node = "broken", status = ""),
                    node(node = "negative", mem = -1)
                )
            ),
            taskSummarySource = summarySource
        )

        val result = repository.getDashboardSnapshot()

        assertTrue(result is DashboardResult.Success)
        val snapshot = (result as DashboardResult.Success).data
        assertEquals(listOf("pve-a", "pve-b"), snapshot.nodes.map { it.node })
        assertEquals(listOf("pve-a", "pve-b"), summarySource.requestedNodeNames)
        assertEquals(1, snapshot.taskSummary.summary?.runningCount)
        assertNull(snapshot.taskSummary.errorMessage)
    }

    @Test
    fun getDashboardSnapshot_keepsNodeSuccessWhenTaskSummaryFails() = runBlocking {
        val repository = DashboardRepository(
            api = FakeDashboardApi(nodes = listOf(node(node = "pve"))),
            taskSummarySource = FakeTaskSummarySource(
                result = TaskResult.Error("Task endpoint unavailable")
            )
        )

        val result = repository.getDashboardSnapshot()

        assertTrue(result is DashboardResult.Success)
        val snapshot = (result as DashboardResult.Success).data
        assertEquals(listOf("pve"), snapshot.nodes.map { it.node })
        assertNull(snapshot.taskSummary.summary)
        assertEquals("Task endpoint unavailable", snapshot.taskSummary.errorMessage)
    }

    @Test
    fun getDashboardSnapshot_doesNotRequestTaskSummaryWhenNoNodesRemain() = runBlocking {
        val summarySource = FakeTaskSummarySource()
        val repository = DashboardRepository(
            api = FakeDashboardApi(nodes = listOf(node(node = ""), node(node = "bad", status = ""))),
            taskSummarySource = summarySource
        )

        val result = repository.getDashboardSnapshot()

        assertTrue(result is DashboardResult.Success)
        val snapshot = (result as DashboardResult.Success).data
        assertTrue(snapshot.nodes.isEmpty())
        assertTrue(summarySource.requestedNodeNames.isEmpty())
        assertNull(snapshot.taskSummary.summary)
        assertNull(snapshot.taskSummary.errorMessage)
    }

    @Test
    fun getDashboardSnapshot_reportsMissingApiServiceAsNotAuthenticated() = runBlocking {
        val repository = DashboardRepository(
            api = ProxmoxDashboardApi { null },
            taskSummarySource = FakeTaskSummarySource()
        )

        val result = repository.getDashboardSnapshot()

        assertTrue(result is DashboardResult.Error)
        assertEquals("Not authenticated", (result as DashboardResult.Error).message)
    }

    private class FakeDashboardApi(
        private val nodes: List<Node> = listOf(node())
    ) : DashboardApi {
        override suspend fun getNodes(): ApiResponse<List<Node>> {
            return ApiResponse(nodes)
        }
    }

    private class FakeTaskSummarySource(
        private val result: TaskResult<TaskSummary> = TaskResult.Success(
            TaskSummary(
                nodesChecked = 1,
                runningCount = 0,
                recentCount = 1,
                latestTask = task()
            )
        )
    ) : DashboardTaskSummarySource {
        var requestedNodeNames: List<String> = emptyList()
            private set

        override suspend fun getTaskSummary(nodeNames: List<String>): TaskResult<TaskSummary> {
            requestedNodeNames = nodeNames
            return result
        }
    }

    companion object {
        private fun node(
            node: String = "pve",
            status: String = "online",
            mem: Long = 1024L * 1024L * 1024L
        ): Node {
            return Node(
                node = node,
                status = status,
                cpu = 0.1,
                level = "",
                maxcpu = 4,
                maxmem = 8L * 1024L * 1024L * 1024L,
                mem = mem,
                ssl_fingerprint = "",
                uptime = 3600
            )
        }

        private fun task(node: String = "pve"): Task {
            return Task(
                upid = "UPID:$node:task",
                id = "UPID:$node:task",
                node = node,
                pid = 1234,
                pstart = 10,
                type = "qmstart",
                status = "running",
                exitstatus = null,
                starttime = 100,
                endtime = null,
                user = "root@pam",
                saved = true
            )
        }
    }
}
