package com.proxmoxmobile.presentation.screens.backups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proxmoxmobile.data.backup.BackupEntry
import com.proxmoxmobile.data.backup.BackupRepository
import com.proxmoxmobile.data.backup.BackupResult
import com.proxmoxmobile.data.backup.BackupStorageError
import com.proxmoxmobile.data.model.Storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BackupListViewModel(
    availableNodes: List<String>,
    private val repository: BackupRepository,
    private val noNodesMessage: String,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val nodeNames = availableNodes
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    private val _uiState = MutableStateFlow(
        BackupListUiState(
            availableNodes = nodeNames,
            selectedNodeName = nodeNames.firstOrNull()
        )
    )
    val uiState: StateFlow<BackupListUiState> = _uiState.asStateFlow()

    fun selectNode(nodeName: String) {
        val normalizedNodeName = nodeName.trim().takeIf { it.isNotBlank() } ?: return
        if (!nodeNames.contains(normalizedNodeName)) return
        if (_uiState.value.selectedNodeName == normalizedNodeName) return

        _uiState.update {
            it.copy(
                selectedNodeName = normalizedNodeName,
                selectedStorageName = null,
                backupStorages = emptyList(),
                backupEntries = emptyList(),
                storageErrors = emptyList(),
                errorMessage = null
            )
        }
        loadBackups()
    }

    fun selectStorage(storageName: String?) {
        if (storageName == null) {
            _uiState.update { it.copy(selectedStorageName = null) }
            return
        }

        val normalizedStorageName = storageName.trim().takeIf { it.isNotBlank() } ?: return
        if (_uiState.value.backupStorages.none { it.storage == normalizedStorageName }) return

        _uiState.update { it.copy(selectedStorageName = normalizedStorageName) }
    }

    fun loadBackups(showLoading: Boolean = true) {
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

            when (val result = repository.getBackupInventory(node)) {
                is BackupResult.Success -> {
                    _uiState.update { previousState ->
                        val availableStorageNames = result.data.storages.map { it.storage }
                        previousState.copy(
                            backupStorages = result.data.storages,
                            backupEntries = result.data.backups,
                            storageErrors = result.data.storageErrors,
                            selectedStorageName = previousState.selectedStorageName
                                ?.takeIf { it in availableStorageNames },
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null,
                            lastRefreshTimeMillis = clock()
                        )
                    }
                }
                is BackupResult.Error -> {
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

data class BackupListUiState(
    val availableNodes: List<String> = emptyList(),
    val selectedNodeName: String? = null,
    val selectedStorageName: String? = null,
    val backupStorages: List<Storage> = emptyList(),
    val backupEntries: List<BackupEntry> = emptyList(),
    val storageErrors: List<BackupStorageError> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val lastRefreshTimeMillis: Long = System.currentTimeMillis()
) {
    val visibleBackupEntries: List<BackupEntry>
        get() {
            val storageName = selectedStorageName ?: return backupEntries
            return backupEntries.filter { it.storageName == storageName }
        }
}
