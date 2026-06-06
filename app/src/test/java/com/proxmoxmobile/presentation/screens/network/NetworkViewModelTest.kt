package com.proxmoxmobile.presentation.screens.network

import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.NetworkInterface
import com.proxmoxmobile.data.network.NetworkApi
import com.proxmoxmobile.data.network.NetworkRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkViewModelTest {
    @Test
    fun init_prefersInitialNodeWhenItIsAvailable() {
        val viewModel = networkViewModel(
            availableNodes = listOf("pve-a", "pve-b"),
            initialNodeName = " pve-b "
        )

        assertEquals("pve-b", viewModel.uiState.value.selectedNodeName)
    }

    @Test
    fun init_fallsBackToFirstAvailableNodeWhenInitialNodeIsUnknown() {
        val viewModel = networkViewModel(
            availableNodes = listOf("pve-a", "pve-b"),
            initialNodeName = "pve-c"
        )

        assertEquals("pve-a", viewModel.uiState.value.selectedNodeName)
    }

    @Test
    fun init_trimsAndDeduplicatesAvailableNodes() {
        val viewModel = networkViewModel(
            availableNodes = listOf(" pve-a ", "", "pve-a", "pve-b")
        )

        assertEquals(listOf("pve-a", "pve-b"), viewModel.uiState.value.availableNodes)
        assertEquals("pve-a", viewModel.uiState.value.selectedNodeName)
    }

    private fun networkViewModel(
        availableNodes: List<String>,
        initialNodeName: String? = null
    ): NetworkViewModel {
        return NetworkViewModel(
            availableNodes = availableNodes,
            repository = NetworkRepository(FakeNetworkApi()),
            noNodesMessage = "No nodes available",
            initialNodeName = initialNodeName
        )
    }

    private class FakeNetworkApi : NetworkApi {
        override suspend fun getNetworkInterfaces(nodeName: String): ApiResponse<List<NetworkInterface>> {
            return ApiResponse(emptyList())
        }
    }
}
