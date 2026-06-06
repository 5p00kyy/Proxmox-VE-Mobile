package com.proxmoxmobile.data.node

import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.NodeStatus
import com.proxmoxmobile.data.model.RootFS
import com.proxmoxmobile.data.model.Swap
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NodeRepositoryTest {
    @Test
    fun getNodeDetail_trimsNodeNameAndReturnsStatus() = runBlocking {
        val api = FakeNodeApi()
        val repository = NodeRepository(api)

        val result = repository.getNodeDetail(" pve ")

        assertTrue(result is NodeResult.Success)
        val detail = (result as NodeResult.Success).data
        assertEquals("pve", detail.nodeName)
        assertEquals("online", detail.status.status)
        assertEquals(listOf("pve"), api.requestedNodeNames)
    }

    @Test
    fun getNodeDetail_reportsBlankNodeName() = runBlocking {
        val repository = NodeRepository(FakeNodeApi())

        val result = repository.getNodeDetail(" ")

        assertTrue(result is NodeResult.Error)
        assertEquals("Node name is required", (result as NodeResult.Error).message)
    }

    @Test
    fun getNodeDetail_rejectsInvalidStatusPayload() = runBlocking {
        val repository = NodeRepository(
            FakeNodeApi(status = nodeStatus(status = "", mem = -1))
        )

        val result = repository.getNodeDetail("pve")

        assertTrue(result is NodeResult.Error)
        assertEquals("Node status payload is invalid", (result as NodeResult.Error).message)
    }

    @Test
    fun getNodeDetail_reportsMissingApiServiceAsNotAuthenticated() = runBlocking {
        val repository = NodeRepository(
            ProxmoxNodeApi { null }
        )

        val result = repository.getNodeDetail("pve")

        assertTrue(result is NodeResult.Error)
        assertEquals("Not authenticated", (result as NodeResult.Error).message)
    }

    private class FakeNodeApi(
        private val status: NodeStatus = nodeStatus()
    ) : NodeApi {
        val requestedNodeNames = mutableListOf<String>()

        override suspend fun getNodeStatus(nodeName: String): ApiResponse<NodeStatus> {
            requestedNodeNames += nodeName
            return ApiResponse(status)
        }
    }

    companion object {
        private fun nodeStatus(
            status: String = "online",
            mem: Long = 2L * 1024L * 1024L * 1024L
        ): NodeStatus {
            return NodeStatus(
                node = "pve",
                status = status,
                cpu = 0.2,
                maxcpu = 8,
                mem = mem,
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
                idle = 80
            )
        }
    }
}
