package com.proxmoxmobile.presentation.screens.vms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proxmoxmobile.data.model.VirtualMachine
import com.proxmoxmobile.data.model.VmSnapshot
import com.proxmoxmobile.data.vm.VmConfigEntry
import com.proxmoxmobile.data.vm.VmRepository
import com.proxmoxmobile.data.vm.VmResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VmDetailViewModel(
    private val vmid: Int,
    nodeNames: List<String>,
    private val preferredNodeName: String?,
    private val repository: VmRepository,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val candidateNodeNames = (
        listOfNotNull(preferredNodeName?.trim()?.takeIf { it.isNotBlank() }) +
            nodeNames.map { it.trim() }.filter { it.isNotBlank() }
        )
        .distinct()

    private val _uiState = MutableStateFlow(
        VmDetailUiState(
            vmid = vmid,
            nodeName = preferredNodeName?.takeIf { it.isNotBlank() }
        )
    )
    val uiState: StateFlow<VmDetailUiState> = _uiState.asStateFlow()

    fun loadVirtualMachine(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            }

            when (
                val result = repository.getVirtualMachineDetail(
                    nodeNames = candidateNodeNames,
                    vmid = vmid,
                    preferredNodeName = preferredNodeName
                )
            ) {
                is VmResult.Success -> {
                    val resolvedNodeName = result.data.nodeName
                    _uiState.update {
                        it.copy(
                            nodeName = resolvedNodeName,
                            virtualMachine = result.data.virtualMachine,
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
                    loadConfigForNode(
                        nodeName = resolvedNodeName,
                        showLoading = showLoading
                    )
                }
                is VmResult.Error -> {
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

    fun loadConfig(showLoading: Boolean = true) {
        val node = _uiState.value.nodeName?.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch {
            loadConfigForNode(
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
            is VmResult.Success -> {
                _uiState.update {
                    it.copy(
                        snapshots = result.data,
                        isSnapshotsLoading = false,
                        isSnapshotsRefreshing = false,
                        snapshotErrorMessage = null
                    )
                }
            }
            is VmResult.Error -> {
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

    private suspend fun loadConfigForNode(
        nodeName: String,
        showLoading: Boolean
    ) {
        if (showLoading) {
            _uiState.update {
                it.copy(
                    isConfigLoading = true,
                    configErrorMessage = null
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isConfigRefreshing = true,
                    configErrorMessage = null
                )
            }
        }

        when (val result = repository.getConfig(nodeName, vmid)) {
            is VmResult.Success -> {
                _uiState.update {
                    it.copy(
                        configEntries = result.data,
                        isConfigLoading = false,
                        isConfigRefreshing = false,
                        configErrorMessage = null
                    )
                }
            }
            is VmResult.Error -> {
                _uiState.update {
                    it.copy(
                        configEntries = emptyList(),
                        isConfigLoading = false,
                        isConfigRefreshing = false,
                        configErrorMessage = result.message
                    )
                }
            }
        }
    }
}

data class VmDetailUiState(
    val vmid: Int,
    val nodeName: String? = null,
    val virtualMachine: VirtualMachine? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val lastRefreshTimeMillis: Long = System.currentTimeMillis(),
    val snapshots: List<VmSnapshot> = emptyList(),
    val isSnapshotsLoading: Boolean = false,
    val isSnapshotsRefreshing: Boolean = false,
    val snapshotErrorMessage: String? = null,
    val configEntries: List<VmConfigEntry> = emptyList(),
    val isConfigLoading: Boolean = false,
    val isConfigRefreshing: Boolean = false,
    val configErrorMessage: String? = null
)
