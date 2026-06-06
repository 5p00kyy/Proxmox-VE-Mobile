package com.proxmoxmobile.data.network

import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.NetworkInterface
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkRepositoryTest {
    @Test
    fun getNetworkInterfaces_filtersInvalidRowsAndSortsByInterfaceName() = runBlocking {
        val repository = NetworkRepository(
            FakeNetworkApi(
                interfaces = listOf(
                    networkInterface(iface = "vmbr0", type = "bridge"),
                    networkInterface(iface = "", type = "bridge"),
                    networkInterface(iface = "eno1", type = "eth"),
                    networkInterface(iface = "broken", type = "")
                )
            )
        )

        val result = repository.getNetworkInterfaces("pve")

        assertTrue(result is NetworkResult.Success)
        val interfaces = (result as NetworkResult.Success).data
        assertEquals(listOf("eno1", "vmbr0"), interfaces.map { it.iface })
    }

    @Test
    fun getNetworkInterfaces_reportsMissingApiServiceAsNotAuthenticated() = runBlocking {
        val repository = NetworkRepository(ProxmoxNetworkApi { null })

        val result = repository.getNetworkInterfaces("pve")

        assertTrue(result is NetworkResult.Error)
        assertEquals("Not authenticated", (result as NetworkResult.Error).message)
    }

    @Test
    fun getNetworkInterfaces_reportsBlankNodeAsInvalidRequest() = runBlocking {
        val repository = NetworkRepository(FakeNetworkApi())

        val result = repository.getNetworkInterfaces(" ")

        assertTrue(result is NetworkResult.Error)
        assertEquals("Node name is required", (result as NetworkResult.Error).message)
    }

    private class FakeNetworkApi(
        private val interfaces: List<NetworkInterface> = listOf(networkInterface())
    ) : NetworkApi {
        override suspend fun getNetworkInterfaces(nodeName: String): ApiResponse<List<NetworkInterface>> {
            return ApiResponse(interfaces)
        }
    }

    companion object {
        private fun networkInterface(
            iface: String = "vmbr0",
            type: String = "bridge"
        ): NetworkInterface {
            return NetworkInterface(
                iface = iface,
                type = type,
                method = "static",
                address = "192.168.1.10",
                netmask = "24",
                gateway = "192.168.1.1",
                active = true,
                autostart = true,
                exists = true,
                families = listOf("inet")
            )
        }
    }
}
