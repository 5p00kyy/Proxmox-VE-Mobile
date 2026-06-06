package com.proxmoxmobile.presentation.screens.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proxmoxmobile.data.model.Storage
import com.proxmoxmobile.data.model.StorageContent
import com.proxmoxmobile.data.storage.StorageRepository
import com.proxmoxmobile.data.storage.StorageResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StorageViewModel(
    private val nodeName: String?,
    private val repository: StorageRepository,
    private val invalidNodeMessage: String,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val _uiState = MutableStateFlow(StorageUiState(nodeName = nodeName))
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    fun loadStorages(showLoading: Boolean = true) {
        val node = nodeName
        if (node.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = invalidNodeMessage
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

            when (val result = repository.getStorages(node)) {
                is StorageResult.Success -> {
                    _uiState.update {
                        it.copy(
                            storages = result.data,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null,
                            lastRefreshTimeMillis = clock()
                        )
                    }
                }
                is StorageResult.Error -> {
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

    fun loadStorageContent(
        storageName: String,
        showLoading: Boolean = true
    ) {
        val node = nodeName
        val normalizedStorageName = storageName.trim()
        if (node.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    selectedStorageName = normalizedStorageName.takeIf { storage -> storage.isNotBlank() },
                    isContentLoading = false,
                    contentErrorMessage = invalidNodeMessage
                )
            }
            return
        }
        if (normalizedStorageName.isBlank()) return

        viewModelScope.launch {
            if (showLoading) {
                _uiState.update {
                    it.copy(
                        selectedStorageName = normalizedStorageName,
                        selectedStorageContent = emptyList(),
                        isContentLoading = true,
                        contentErrorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        selectedStorageName = normalizedStorageName,
                        isContentRefreshing = true,
                        contentErrorMessage = null
                    )
                }
            }

            when (val result = repository.getStorageContent(node, normalizedStorageName)) {
                is StorageResult.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedStorageName = normalizedStorageName,
                            selectedStorageContent = result.data,
                            isContentLoading = false,
                            isContentRefreshing = false,
                            contentErrorMessage = null
                        )
                    }
                }
                is StorageResult.Error -> {
                    _uiState.update {
                        it.copy(
                            selectedStorageName = normalizedStorageName,
                            selectedStorageContent = emptyList(),
                            isContentLoading = false,
                            isContentRefreshing = false,
                            contentErrorMessage = result.message
                        )
                    }
                }
            }
        }
    }
}

data class StorageUiState(
    val nodeName: String?,
    val storages: List<Storage> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val lastRefreshTimeMillis: Long = System.currentTimeMillis(),
    val selectedStorageName: String? = null,
    val selectedStorageContent: List<StorageContent> = emptyList(),
    val isContentLoading: Boolean = false,
    val isContentRefreshing: Boolean = false,
    val contentErrorMessage: String? = null
)
