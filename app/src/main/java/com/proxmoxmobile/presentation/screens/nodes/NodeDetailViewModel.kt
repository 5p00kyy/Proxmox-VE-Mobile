package com.proxmoxmobile.presentation.screens.nodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proxmoxmobile.data.model.NodeStatus
import com.proxmoxmobile.data.node.NodeRepository
import com.proxmoxmobile.data.node.NodeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NodeDetailViewModel(
    private val nodeName: String,
    private val repository: NodeRepository,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val _uiState = MutableStateFlow(NodeDetailUiState(nodeName = nodeName))
    val uiState: StateFlow<NodeDetailUiState> = _uiState.asStateFlow()

    fun loadNode(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            }

            when (val result = repository.getNodeDetail(nodeName)) {
                is NodeResult.Success -> {
                    _uiState.update {
                        it.copy(
                            nodeName = result.data.nodeName,
                            status = result.data.status,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null,
                            lastRefreshTimeMillis = clock()
                        )
                    }
                }
                is NodeResult.Error -> {
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

data class NodeDetailUiState(
    val nodeName: String,
    val status: NodeStatus? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val lastRefreshTimeMillis: Long = System.currentTimeMillis()
)
