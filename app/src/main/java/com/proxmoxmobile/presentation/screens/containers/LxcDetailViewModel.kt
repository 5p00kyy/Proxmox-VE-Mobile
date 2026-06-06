package com.proxmoxmobile.presentation.screens.containers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proxmoxmobile.data.lxc.LxcRepository
import com.proxmoxmobile.data.lxc.LxcResult
import com.proxmoxmobile.data.model.Container
import com.proxmoxmobile.data.model.LxcSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LxcDetailViewModel(
    private val vmid: Int,
    nodeNames: List<String>,
    private val preferredNodeName: String?,
    private val repository: LxcRepository,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val candidateNodeNames = (
        listOfNotNull(preferredNodeName?.trim()?.takeIf { it.isNotBlank() }) +
            nodeNames.map { it.trim() }.filter { it.isNotBlank() }
        )
        .distinct()

    private val _uiState = MutableStateFlow(
        LxcDetailUiState(
            vmid = vmid,
            nodeName = preferredNodeName?.takeIf { it.isNotBlank() }
        )
    )
    val uiState: StateFlow<LxcDetailUiState> = _uiState.asStateFlow()

    fun loadContainer(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            }

            when (
                val result = repository.getContainerDetail(
                    nodeNames = candidateNodeNames,
                    vmid = vmid,
                    preferredNodeName = preferredNodeName
                )
            ) {
                is LxcResult.Success -> {
                    val resolvedNodeName = result.data.nodeName
                    _uiState.update {
                        it.copy(
                            nodeName = resolvedNodeName,
                            container = result.data.container,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null,
                            lastRefreshTimeMillis = clock()
                        )
                    }
                    loadSnapshotsForNode(
                        nodeName = resolvedNodeName,
                        showLoading = showLoading
                    )
                }
                is LxcResult.Error -> {
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

    fun loadSnapshots(showLoading: Boolean = true) {
        val node = _uiState.value.nodeName?.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch {
            loadSnapshotsForNode(
                nodeName = node,
                showLoading = showLoading
            )
        }
    }

    private suspend fun loadSnapshotsForNode(
        nodeName: String,
        showLoading: Boolean
    ) {
        if (showLoading) {
            _uiState.update {
                it.copy(
                    isSnapshotsLoading = true,
                    snapshotErrorMessage = null
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isSnapshotsRefreshing = true,
                    snapshotErrorMessage = null
                )
            }
        }

        when (val result = repository.getSnapshots(nodeName, vmid)) {
            is LxcResult.Success -> {
                _uiState.update {
                    it.copy(
                        snapshots = result.data,
                        isSnapshotsLoading = false,
                        isSnapshotsRefreshing = false,
                        snapshotErrorMessage = null
                    )
                }
            }
            is LxcResult.Error -> {
                _uiState.update {
                    it.copy(
                        snapshots = emptyList(),
                        isSnapshotsLoading = false,
                        isSnapshotsRefreshing = false,
                        snapshotErrorMessage = result.message
                    )
                }
            }
        }
    }
}

data class LxcDetailUiState(
    val vmid: Int,
    val nodeName: String? = null,
    val container: Container? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val lastRefreshTimeMillis: Long = System.currentTimeMillis(),
    val snapshots: List<LxcSnapshot> = emptyList(),
    val isSnapshotsLoading: Boolean = false,
    val isSnapshotsRefreshing: Boolean = false,
    val snapshotErrorMessage: String? = null
)
