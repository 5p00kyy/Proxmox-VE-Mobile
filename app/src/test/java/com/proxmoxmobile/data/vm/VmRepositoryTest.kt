package com.proxmoxmobile.data.vm

import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.VirtualMachine
import com.proxmoxmobile.data.model.VmSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VmRepositoryTest {
    @Test
    fun getVirtualMachines_filtersInvalidPayloadRows() = runBlocking {
        val repository = VmRepository(
            FakeVmApi(
                vms = listOf(
                    vm(vmid = 100, name = "db", status = "running"),
                    vm(vmid = 0, name = "invalid-id", status = "running"),
                    vm(vmid = 101, name = "", status = "running"),
                    vm(vmid = 102, name = "missing-status", status = "")
                )
            )
        )

        val result = repository.getVirtualMachines("pve")

        assertTrue(result is VmResult.Success)
        val vms = (result as VmResult.Success).data
        assertEquals(1, vms.size)
        assertEquals(100, vms.single().vmid)
    }

    @Test
    fun performAction_returnsTaskId() = runBlocking {
        val api = FakeVmApi(actionTaskId = "UPID:pve:123:start:100")
        val repository = VmRepository(api)

        val result = repository.performAction("pve", 100, VmPowerAction.Start)

        assertTrue(result is VmResult.Success)
        val action = (result as VmResult.Success).data
        assertEquals(VmPowerAction.Start, action.action)
        assertEquals(100, action.vmid)
        assertEquals("UPID:pve:123:start:100", action.taskId)
        assertEquals(listOf("start"), api.actionRequests)
    }

    @Test
    fun performAction_sendsRebootApiValue() = runBlocking {
        val api = FakeVmApi(actionTaskId = "UPID:pve:123:reboot:100")
        val repository = VmRepository(api)

        val result = repository.performAction("pve", 100, VmPowerAction.Reboot)

        assertTrue(result is VmResult.Success)
        val action = (result as VmResult.Success).data
        assertEquals(VmPowerAction.Reboot, action.action)
        assertEquals("UPID:pve:123:reboot:100", action.taskId)
        assertEquals(listOf("reboot"), api.actionRequests)
    }

    @Test
    fun performAction_sendsShutdownApiValue() = runBlocking {
        val api = FakeVmApi(actionTaskId = "UPID:pve:123:shutdown:100")
        val repository = VmRepository(api)

        val result = repository.performAction("pve", 100, VmPowerAction.Shutdown)

        assertTrue(result is VmResult.Success)
        val action = (result as VmResult.Success).data
        assertEquals(VmPowerAction.Shutdown, action.action)
        assertEquals("UPID:pve:123:shutdown:100", action.taskId)
        assertEquals(listOf("shutdown"), api.actionRequests)
    }

    @Test
    fun deleteVirtualMachine_returnsTaskIdAndUsesDeleteEndpoint() = runBlocking {
        val api = FakeVmApi(deleteTaskId = "UPID:pve:123:qmdestroy:100")
        val repository = VmRepository(api)

        val result = repository.deleteVirtualMachine("pve", 100)

        assertTrue(result is VmResult.Success)
        val action = (result as VmResult.Success).data
        assertEquals(VmPowerAction.Delete, action.action)
        assertEquals(100, action.vmid)
        assertEquals("UPID:pve:123:qmdestroy:100", action.taskId)
        assertEquals(listOf(100), api.deleteRequests)
        assertEquals(emptyList<String>(), api.actionRequests)
    }

    @Test
    fun getVirtualMachineDetail_usesPreferredNodeFirst() = runBlocking {
        val api = FakeVmApi(
            vmsByNode = mapOf(
                "pve-a" to listOf(vm(vmid = 100, name = "on-a")),
                "pve-b" to listOf(vm(vmid = 100, name = "on-b"))
            )
        )
        val repository = VmRepository(api)

        val result = repository.getVirtualMachineDetail(
            nodeNames = listOf("pve-a", "pve-b"),
            vmid = 100,
            preferredNodeName = "pve-b"
        )

        assertTrue(result is VmResult.Success)
        val detail = (result as VmResult.Success).data
        assertEquals("pve-b", detail.nodeName)
        assertEquals("on-b", detail.virtualMachine.name)
        assertEquals(listOf("pve-b"), api.statusRequests)
    }

    @Test
    fun getVirtualMachineDetail_fallsBackAcrossCachedNodes() = runBlocking {
        val api = FakeVmApi(
            vmsByNode = mapOf(
                "pve-a" to emptyList(),
                "pve-b" to listOf(vm(vmid = 101, name = "web"))
            )
        )
        val repository = VmRepository(api)

        val result = repository.getVirtualMachineDetail(
            nodeNames = listOf("pve-a", "pve-b"),
            vmid = 101
        )

        assertTrue(result is VmResult.Success)
        val detail = (result as VmResult.Success).data
        assertEquals("pve-b", detail.nodeName)
        assertEquals("web", detail.virtualMachine.name)
        assertEquals(listOf("pve-a", "pve-b"), api.statusRequests)
    }

    @Test
    fun getVirtualMachineDetail_reportsVmNotFound() = runBlocking {
        val repository = VmRepository(
            FakeVmApi(vmsByNode = mapOf("pve" to emptyList()))
        )

        val result = repository.getVirtualMachineDetail(
            nodeNames = listOf("pve"),
            vmid = 404
        )

        assertTrue(result is VmResult.Error)
        assertEquals("VM not found", (result as VmResult.Error).message)
    }

    @Test
    fun getVirtualMachineDetail_reportsMissingNodeNames() = runBlocking {
        val repository = VmRepository(FakeVmApi())

        val result = repository.getVirtualMachineDetail(
            nodeNames = emptyList(),
            vmid = 100
        )

        assertTrue(result is VmResult.Error)
        assertEquals("At least one node is required", (result as VmResult.Error).message)
    }

    @Test
    fun getVirtualMachines_reportsMissingApiServiceAsNotAuthenticated() = runBlocking {
        val repository = VmRepository(
            ProxmoxVmApi { null }
        )

        val result = repository.getVirtualMachines("pve")

        assertTrue(result is VmResult.Error)
        assertEquals("Not authenticated", (result as VmResult.Error).message)
    }

    @Test
    fun getVirtualMachineDetail_reportsMissingApiServiceAsNotAuthenticated() = runBlocking {
        val repository = VmRepository(
            ProxmoxVmApi { null }
        )

        val result = repository.getVirtualMachineDetail(
            nodeNames = listOf("pve"),
            vmid = 100
        )

        assertTrue(result is VmResult.Error)
        assertEquals("Not authenticated", (result as VmResult.Error).message)
    }

    @Test
    fun getSnapshots_filtersInvalidRowsAndSortsCurrentThenNewest() = runBlocking {
        val repository = VmRepository(
            FakeVmApi(
                snapshotsByNodeAndVmid = mapOf(
                    "pve" to mapOf(
                        100 to listOf(
                            snapshot(name = "older", snaptime = 10),
                            snapshot(name = "", snaptime = 30),
                            snapshot(name = "newer", snaptime = 30),
                            snapshot(name = "invalid-time", snaptime = -1),
                            snapshot(name = "current", snaptime = null)
                        )
                    )
                )
            )
        )

        val result = repository.getSnapshots("pve", 100)

        assertTrue(result is VmResult.Success)
        assertEquals(
            listOf("current", "newer", "older"),
            (result as VmResult.Success).data.map { it.name }
        )
    }

    @Test
    fun getSnapshots_reportsInvalidRequest() = runBlocking {
        val repository = VmRepository(FakeVmApi())

        val blankNodeResult = repository.getSnapshots(" ", 100)
        val invalidVmidResult = repository.getSnapshots("pve", 0)

        assertTrue(blankNodeResult is VmResult.Error)
        assertEquals("Node name is required", (blankNodeResult as VmResult.Error).message)
        assertTrue(invalidVmidResult is VmResult.Error)
        assertEquals("VMID is required", (invalidVmidResult as VmResult.Error).message)
    }

    @Test
    fun getSnapshots_reportsMissingApiServiceAsNotAuthenticated() = runBlocking {
        val repository = VmRepository(ProxmoxVmApi { null })

        val result = repository.getSnapshots("pve", 100)

        assertTrue(result is VmResult.Error)
        assertEquals("Not authenticated", (result as VmResult.Error).message)
    }

    @Test
    fun getConfig_mapsSortedEntriesAndRedactsSensitiveValues() = runBlocking {
        val repository = VmRepository(
            FakeVmApi(
                configsByNodeAndVmid = mapOf(
                    "pve" to mapOf(
                        100 to mapOf(
                            "net0" to "virtio=AA:BB:CC,bridge=vmbr0",
                            "memory" to 2048.0,
                            "cipassword" to "super-secret",
                            "" to "ignored",
                            "empty" to "",
                            "cores" to 2
                        )
                    )
                )
            )
        )

        val result = repository.getConfig("pve", 100)

        assertTrue(result is VmResult.Success)
        val config = (result as VmResult.Success).data
        assertEquals(
            listOf("memory", "cores", "cipassword", "net0"),
            config.map { it.key }
        )
        assertEquals("2048", config.first { it.key == "memory" }.value)
        assertEquals("2", config.first { it.key == "cores" }.value)
        assertEquals("[redacted]", config.first { it.key == "cipassword" }.value)
    }

    @Test
    fun getConfig_reportsInvalidRequest() = runBlocking {
        val repository = VmRepository(FakeVmApi())

        val blankNodeResult = repository.getConfig(" ", 100)
        val invalidVmidResult = repository.getConfig("pve", 0)

        assertTrue(blankNodeResult is VmResult.Error)
        assertEquals("Node name is required", (blankNodeResult as VmResult.Error).message)
        assertTrue(invalidVmidResult is VmResult.Error)
        assertEquals("VMID is required", (invalidVmidResult as VmResult.Error).message)
    }

    @Test
    fun getConfig_reportsMissingApiServiceAsNotAuthenticated() = runBlocking {
        val repository = VmRepository(ProxmoxVmApi { null })

        val result = repository.getConfig("pve", 100)

        assertTrue(result is VmResult.Error)
        assertEquals("Not authenticated", (result as VmResult.Error).message)
    }

    private class FakeVmApi(
        private val vms: List<VirtualMachine> = listOf(vm()),
        private val vmsByNode: Map<String, List<VirtualMachine>> = emptyMap(),
        private val snapshotsByNodeAndVmid: Map<String, Map<Int, List<VmSnapshot>>> = emptyMap(),
        private val configsByNodeAndVmid: Map<String, Map<Int, Map<String, Any?>>> = emptyMap(),
        private val actionTaskId: String = "UPID:pve:123:action:100",
        private val deleteTaskId: String = "UPID:pve:123:delete:100"
    ) : VmApi {
        val statusRequests = mutableListOf<String>()
        val actionRequests = mutableListOf<String>()
        val deleteRequests = mutableListOf<Int>()

        override suspend fun getVirtualMachines(nodeName: String): ApiResponse<List<VirtualMachine>> {
            return ApiResponse(vmsByNode[nodeName] ?: vms)
        }

        override suspend fun getVMStatus(
            nodeName: String,
            vmid: Int
        ): ApiResponse<VirtualMachine> {
            statusRequests += nodeName
            val vm = (vmsByNode[nodeName] ?: vms).firstOrNull { it.vmid == vmid }
                ?: throw IllegalArgumentException("not found")
            return ApiResponse(vm)
        }

        override suspend fun getVMConfig(
            nodeName: String,
            vmid: Int
        ): ApiResponse<Map<String, Any?>> {
            return ApiResponse(configsByNodeAndVmid[nodeName]?.get(vmid).orEmpty())
        }

        override suspend fun performVMAction(
            nodeName: String,
            vmid: Int,
            action: String
        ): ApiResponse<String> {
            actionRequests += action
            return ApiResponse(actionTaskId)
        }

        override suspend fun deleteVM(nodeName: String, vmid: Int): ApiResponse<String> {
            deleteRequests += vmid
            return ApiResponse(deleteTaskId)
        }

        override suspend fun getVMSnapshots(
            nodeName: String,
            vmid: Int
        ): ApiResponse<List<VmSnapshot>> {
            return ApiResponse(snapshotsByNodeAndVmid[nodeName]?.get(vmid).orEmpty())
        }
    }

    companion object {
        private fun vm(
            vmid: Int = 100,
            name: String = "web",
            status: String = "running"
        ): VirtualMachine {
            return VirtualMachine(
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
                qmpstatus = "running",
                running_machine = null,
                running_qemu = null,
                tags = null
            )
        }

        private fun snapshot(
            name: String = "before-upgrade",
            snaptime: Long? = 100
        ): VmSnapshot {
            return VmSnapshot(
                name = name,
                description = null,
                snaptime = snaptime,
                vmstate = 0,
                parent = null
            )
        }
    }
}
