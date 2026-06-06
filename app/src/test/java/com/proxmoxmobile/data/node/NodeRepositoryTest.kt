package com.proxmoxmobile.data.node

import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.NodeCpuInfo
import com.proxmoxmobile.data.model.NodeMemory
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
            FakeNodeApi(status = nodeStatus(mem = -1))
        )

        val result = repository.getNodeDetail("pve")

        assertTrue(result is NodeResult.Error)
        assertEquals("Node status payload is invalid", (result as NodeResult.Error).message)
    }

    @Test
    fun getNodeDetail_defaultsBlankStatusFromNodeStatusEndpoint() = runBlocking {
        val repository = NodeRepository(
            FakeNodeApi(status = nodeStatus(status = ""))
        )

        val result = repository.getNodeDetail("pve")

        assertTrue(result is NodeResult.Success)
        assertEquals("online", (result as NodeResult.Success).data.status.status)
    }

    @Test
    fun getNodeDetail_usesNestedCpuAndMemoryFromStatusEndpoint() = runBlocking {
        val repository = NodeRepository(
            FakeNodeApi(
                status = nodeStatus(
                    maxcpu = 0,
                    mem = 0,
                    maxmem = 0,
                    cpuinfo = NodeCpuInfo(cpus = 20),
                    memory = NodeMemory(
                        free = 12L * 1024L * 1024L * 1024L,
                        total = 32L * 1024L * 1024L * 1024L,
                        used = 20L * 1024L * 1024L * 1024L
                    )
                )
            )
        )

        val result = repository.getNodeDetail("pve")

        assertTrue(result is NodeResult.Success)
        val status = (result as NodeResult.Success).data.status
        assertEquals(20, status.maxcpu)
        assertEquals(20L * 1024L * 1024L * 1024L, status.mem)
        assertEquals(32L * 1024L * 1024L * 1024L, status.maxmem)
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
            maxcpu: Int = 8,
            mem: Long = 2L * 1024L * 1024L * 1024L,
            maxmem: Long = 16L * 1024L * 1024L * 1024L,
            cpuinfo: NodeCpuInfo? = null,
            memory: NodeMemory? = null
        ): NodeStatus {
            return NodeStatus(
                node = "pve",
                status = status,
                cpu = 0.2,
                maxcpu = maxcpu,
                mem = mem,
                maxmem = maxmem,
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
                idle = 80,
                cpuinfo = cpuinfo,
                memory = memory
            )
        }
    }
}
