package com.proxmoxmobile.data.lxc

import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.Container
import com.proxmoxmobile.data.model.LxcSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LxcRepositoryTest {
    @Test
    fun getContainers_filtersInvalidPayloadRowsAndSortsById() = runBlocking {
        val repository = LxcRepository(
            FakeLxcApi(
                containers = listOf(
                    container(vmid = 300, name = "app", status = "running"),
                    container(vmid = 100, name = "db", status = "stopped"),
                    container(vmid = 0, name = "invalid-id", status = "running"),
                    container(vmid = 200, name = "", status = "running"),
                    container(vmid = 201, name = "missing-status", status = "")
                )
            )
        )

        val result = repository.getContainers("pve")

        assertTrue(result is LxcResult.Success)
        val containers = (result as LxcResult.Success).data
        assertEquals(listOf(100, 300), containers.map { it.vmid })
    }

    @Test
    fun performAction_returnsTaskId() = runBlocking {
        val api = FakeLxcApi(actionTaskId = "UPID:pve:123:start:100")
        val repository = LxcRepository(api)

        val result = repository.performAction("pve", 100, LxcPowerAction.Start)

        assertTrue(result is LxcResult.Success)
        val action = (result as LxcResult.Success).data
        assertEquals(LxcPowerAction.Start, action.action)
        assertEquals(100, action.vmid)
        assertEquals("UPID:pve:123:start:100", action.taskId)
        assertEquals(listOf("start"), api.actionRequests)
    }

    @Test
    fun performAction_sendsRebootApiValue() = runBlocking {
        val api = FakeLxcApi(actionTaskId = "UPID:pve:123:reboot:100")
        val repository = LxcRepository(api)

        val result = repository.performAction("pve", 100, LxcPowerAction.Reboot)

        assertTrue(result is LxcResult.Success)
        val action = (result as LxcResult.Success).data
        assertEquals(LxcPowerAction.Reboot, action.action)
        assertEquals("UPID:pve:123:reboot:100", action.taskId)
        assertEquals(listOf("reboot"), api.actionRequests)
    }

    @Test
    fun performAction_sendsShutdownApiValue() = runBlocking {
        val api = FakeLxcApi(actionTaskId = "UPID:pve:123:shutdown:100")
        val repository = LxcRepository(api)

        val result = repository.performAction("pve", 100, LxcPowerAction.Shutdown)

        assertTrue(result is LxcResult.Success)
        val action = (result as LxcResult.Success).data
        assertEquals(LxcPowerAction.Shutdown, action.action)
        assertEquals("UPID:pve:123:shutdown:100", action.taskId)
        assertEquals(listOf("shutdown"), api.actionRequests)
    }

    @Test
    fun deleteContainer_returnsTaskIdAndUsesDeleteEndpoint() = runBlocking {
        val api = FakeLxcApi(deleteTaskId = "UPID:pve:123:vzdestroy:100")
        val repository = LxcRepository(api)

        val result = repository.deleteContainer("pve", 100)

        assertTrue(result is LxcResult.Success)
        val action = (result as LxcResult.Success).data
        assertEquals(LxcPowerAction.Delete, action.action)
        assertEquals(100, action.vmid)
        assertEquals("UPID:pve:123:vzdestroy:100", action.taskId)
        assertEquals(listOf(100), api.deleteRequests)
        assertEquals(emptyList<String>(), api.actionRequests)
    }

    @Test
    fun getContainerDetail_usesPreferredNodeFirst() = runBlocking {
        val api = FakeLxcApi(
            containersByNode = mapOf(
                "pve-a" to listOf(container(vmid = 100, name = "on-a")),
                "pve-b" to listOf(container(vmid = 100, name = "on-b"))
            )
        )
        val repository = LxcRepository(api)

        val result = repository.getContainerDetail(
            nodeNames = listOf("pve-a", "pve-b"),
            vmid = 100,
            preferredNodeName = "pve-b"
        )

        assertTrue(result is LxcResult.Success)
        val detail = (result as LxcResult.Success).data
        assertEquals("pve-b", detail.nodeName)
        assertEquals("on-b", detail.container.name)
        assertEquals(listOf("pve-b"), api.statusRequests)
    }

    @Test
    fun getContainerDetail_fallsBackAcrossCachedNodes() = runBlocking {
        val api = FakeLxcApi(
            containersByNode = mapOf(
                "pve-a" to emptyList(),
                "pve-b" to listOf(container(vmid = 101, name = "web"))
            )
        )
        val repository = LxcRepository(api)

        val result = repository.getContainerDetail(
            nodeNames = listOf("pve-a", "pve-b"),
            vmid = 101
        )

        assertTrue(result is LxcResult.Success)
        val detail = (result as LxcResult.Success).data
        assertEquals("pve-b", detail.nodeName)
        assertEquals("web", detail.container.name)
        assertEquals(listOf("pve-a", "pve-b"), api.statusRequests)
    }

    @Test
    fun getContainerDetail_reportsContainerNotFound() = runBlocking {
        val repository = LxcRepository(
            FakeLxcApi(containersByNode = mapOf("pve" to emptyList()))
        )

        val result = repository.getContainerDetail(
            nodeNames = listOf("pve"),
            vmid = 404
        )

        assertTrue(result is LxcResult.Error)
        assertEquals("Container not found", (result as LxcResult.Error).message)
    }

    @Test
    fun getContainerDetail_reportsMissingNodeNames() = runBlocking {
        val repository = LxcRepository(FakeLxcApi())

        val result = repository.getContainerDetail(
            nodeNames = emptyList(),
            vmid = 100
        )

        assertTrue(result is LxcResult.Error)
        assertEquals("At least one node is required", (result as LxcResult.Error).message)
    }

    @Test
    fun getContainers_reportsMissingApiServiceAsNotAuthenticated() = runBlocking {
        val repository = LxcRepository(
            ProxmoxLxcApi { null }
        )

        val result = repository.getContainers("pve")

        assertTrue(result is LxcResult.Error)
        assertEquals("Not authenticated", (result as LxcResult.Error).message)
    }

    @Test
    fun getContainerDetail_reportsMissingApiServiceAsNotAuthenticated() = runBlocking {
        val repository = LxcRepository(
            ProxmoxLxcApi { null }
        )

        val result = repository.getContainerDetail(
            nodeNames = listOf("pve"),
            vmid = 100
        )

        assertTrue(result is LxcResult.Error)
        assertEquals("Not authenticated", (result as LxcResult.Error).message)
    }

    @Test
    fun getSnapshots_filtersInvalidRowsAndSortsCurrentThenNewest() = runBlocking {
        val repository = LxcRepository(
            FakeLxcApi(
                snapshotsByNodeAndVmid = mapOf(
                    "pve" to mapOf(
                        100 to listOf(
                            snapshot(name = "before-upgrade", snaptime = 10),
                            snapshot(name = "", snaptime = 30),
                            snapshot(name = "post-config", snaptime = 30),
                            snapshot(name = "invalid-time", snaptime = -1),
                            snapshot(name = "current", snaptime = null)
                        )
                    )
                )
            )
        )

        val result = repository.getSnapshots("pve", 100)

        assertTrue(result is LxcResult.Success)
        assertEquals(
            listOf("current", "post-config", "before-upgrade"),
            (result as LxcResult.Success).data.map { it.name }
        )
    }

    @Test
    fun getSnapshots_reportsInvalidRequest() = runBlocking {
        val repository = LxcRepository(FakeLxcApi())

        val blankNodeResult = repository.getSnapshots(" ", 100)
        val invalidVmidResult = repository.getSnapshots("pve", 0)

        assertTrue(blankNodeResult is LxcResult.Error)
        assertEquals("Node name is required", (blankNodeResult as LxcResult.Error).message)
        assertTrue(invalidVmidResult is LxcResult.Error)
        assertEquals("Container ID is required", (invalidVmidResult as LxcResult.Error).message)
    }

    @Test
    fun getSnapshots_reportsMissingApiServiceAsNotAuthenticated() = runBlocking {
        val repository = LxcRepository(ProxmoxLxcApi { null })

        val result = repository.getSnapshots("pve", 100)

        assertTrue(result is LxcResult.Error)
        assertEquals("Not authenticated", (result as LxcResult.Error).message)
    }

    private class FakeLxcApi(
        private val containers: List<Container> = listOf(container()),
        private val containersByNode: Map<String, List<Container>> = emptyMap(),
        private val snapshotsByNodeAndVmid: Map<String, Map<Int, List<LxcSnapshot>>> = emptyMap(),
        private val actionTaskId: String = "UPID:pve:123:action:100",
        private val deleteTaskId: String = "UPID:pve:123:delete:100"
    ) : LxcApi {
        val statusRequests = mutableListOf<String>()
        val actionRequests = mutableListOf<String>()
        val deleteRequests = mutableListOf<Int>()

        override suspend fun getContainers(nodeName: String): ApiResponse<List<Container>> {
            return ApiResponse(containersByNode[nodeName] ?: containers)
        }

        override suspend fun getContainerStatus(
            nodeName: String,
            vmid: Int
        ): ApiResponse<Container> {
            statusRequests += nodeName
            val container = (containersByNode[nodeName] ?: containers).firstOrNull { it.vmid == vmid }
                ?: throw IllegalArgumentException("not found")
            return ApiResponse(container)
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
            return ApiResponse(snapshotsByNodeAndVmid[nodeName]?.get(vmid).orEmpty())
        }
    }

    companion object {
        private fun container(
            vmid: Int = 100,
            name: String = "web",
            status: String = "running"
        ): Container {
            return Container(
                vmid = vmid,
                name = name,
                status = status,
                cpu = 0.1,
                maxcpu = 2,
                mem = 512L * 1024L * 1024L,
                maxmem = 1024L * 1024L * 1024L,
                uptime = 3600,
                template = false,
                cpus = 2,
                disk = 10L * 1024L * 1024L * 1024L,
                diskread = 0,
                diskwrite = 0,
                netin = 0,
                netout = 0,
                tags = null
            )
        }

        private fun snapshot(
            name: String = "before-upgrade",
            snaptime: Long? = 100
        ): LxcSnapshot {
            return LxcSnapshot(
                name = name,
                description = null,
                snaptime = snaptime,
                vmstate = 0,
                parent = null
            )
        }
    }
}
