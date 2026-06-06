package com.proxmoxmobile.presentation.screens.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proxmoxmobile.data.model.NetworkInterface
import com.proxmoxmobile.data.network.NetworkRepository
import com.proxmoxmobile.data.network.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NetworkViewModel(
    availableNodes: List<String>,
    private val repository: NetworkRepository,
    private val noNodesMessage: String,
    initialNodeName: String? = null,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val nodeNames = availableNodes
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    private val _uiState = MutableStateFlow(
        NetworkUiState(
            availableNodes = nodeNames,
            selectedNodeName = initialNodeName
                ?.trim()
                ?.takeIf { nodeNames.contains(it) }
                ?: nodeNames.firstOrNull()
        )
    )
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    fun selectNode(nodeName: String) {
        val normalizedNodeName = nodeName.trim().takeIf { it.isNotBlank() } ?: return
        if (!nodeNames.contains(normalizedNodeName)) return
        if (_uiState.value.selectedNodeName == normalizedNodeName) return

        _uiState.update {
            it.copy(
                selectedNodeName = normalizedNodeName,
                networkInterfaces = emptyList(),
                errorMessage = null
            )
        }
        loadNetworkInterfaces()
    }

    fun loadNetworkInterfaces(showLoading: Boolean = true) {
        val node = _uiState.value.selectedNodeName
        if (node.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = noNodesMessage
                )
            }
            return
        }

        viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isRefreshing = true) }
            }

            when (val result = repository.getNetworkInterfaces(node)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            networkInterfaces = result.data,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null,
                            lastRefreshTimeMillis = clock()
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }
}

data class NetworkUiState(
    val availableNodes: List<String> = emptyList(),
    val selectedNodeName: String? = null,
    val networkInterfaces: List<NetworkInterface> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val lastRefreshTimeMillis: Long = System.currentTimeMillis()
)
