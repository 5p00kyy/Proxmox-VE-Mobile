package com.proxmoxmobile.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proxmoxmobile.data.dashboard.DashboardRepository
import com.proxmoxmobile.data.dashboard.DashboardResult
import com.proxmoxmobile.data.model.Node
import com.proxmoxmobile.data.task.TaskSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    initialCachedNodes: List<Node>,
    private val repository: DashboardRepository,
    private val cacheNodes: (List<Node>) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        DashboardUiState(
            nodes = initialCachedNodes,
            lastRefreshTimeMillis = clock()
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadDashboard(showLoading: Boolean = true) {
        val hasCurrentNodes = _uiState.value.nodes.isNotEmpty()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = showLoading && !hasCurrentNodes,
                    isRefreshing = !showLoading || hasCurrentNodes,
                    isTaskSummaryLoading = hasCurrentNodes && it.taskSummary == null,
                    errorMessage = null
                )
            }

            when (val result = repository.getDashboardSnapshot()) {
                is DashboardResult.Success -> {
                    cacheNodes(result.data.nodes)
                    _uiState.update {
                        it.copy(
                            nodes = result.data.nodes,
                            isLoading = false,
                            isRefreshing = false,
                            taskSummary = result.data.taskSummary.summary,
                            taskSummaryError = result.data.taskSummary.errorMessage,
                            isTaskSummaryLoading = false,
                            errorMessage = null,
                            lastRefreshTimeMillis = clock()
                        )
                    }
                }
                is DashboardResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isTaskSummaryLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }
}

data class DashboardUiState(
    val nodes: List<Node> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val lastRefreshTimeMillis: Long = System.currentTimeMillis(),
    val taskSummary: TaskSummary? = null,
    val taskSummaryError: String? = null,
    val isTaskSummaryLoading: Boolean = false
)
