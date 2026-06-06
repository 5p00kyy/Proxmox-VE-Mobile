package com.proxmoxmobile.presentation.screens.cluster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proxmoxmobile.data.cluster.ClusterOverview
import com.proxmoxmobile.data.cluster.ClusterRepository
import com.proxmoxmobile.data.cluster.ClusterResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClusterViewModel(
    private val repository: ClusterRepository,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val _uiState = MutableStateFlow(ClusterUiState())
    val uiState: StateFlow<ClusterUiState> = _uiState.asStateFlow()

    fun loadCluster(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            }

            when (val result = repository.getClusterOverview()) {
                is ClusterResult.Success -> {
                    _uiState.update {
                        it.copy(
                            overview = result.data,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null,
                            lastRefreshTimeMillis = clock()
                        )
                    }
                }
                is ClusterResult.Error -> {
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

data class ClusterUiState(
    val overview: ClusterOverview? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val lastRefreshTimeMillis: Long = System.currentTimeMillis()
)
