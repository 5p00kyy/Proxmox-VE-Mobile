package com.proxmoxmobile.presentation.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proxmoxmobile.data.model.Task
import com.proxmoxmobile.data.task.TaskDetail
import com.proxmoxmobile.data.task.TaskFilters
import com.proxmoxmobile.data.task.TaskRepository
import com.proxmoxmobile.data.task.TaskResult
import com.proxmoxmobile.data.task.taskUpid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TaskListViewModel(
    availableNodes: List<String>,
    initialNodeName: String? = null,
    initialFilters: TaskFilters = TaskFilters(),
    private val repository: TaskRepository,
    private val noNodesMessage: String,
    private val invalidTaskMessage: String,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        TaskListUiState(
            availableNodes = availableNodes,
            selectedNode = initialNodeName
                ?.takeIf { it.isNotBlank() }
                ?: availableNodes.firstOrNull(),
            filters = initialFilters
        )
    )
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    fun loadTasks(showLoading: Boolean = true) {
        val nodeName = _uiState.value.selectedNode
        if (nodeName.isNullOrBlank()) {
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

            when (val result = repository.getTasks(nodeName, filters = _uiState.value.filters)) {
                is TaskResult.Success -> {
                    _uiState.update {
                        it.copy(
                            tasks = result.data,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null,
                            lastRefreshTimeMillis = clock()
                        )
                    }
                }
                is TaskResult.Error -> {
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

    fun selectNode(nodeName: String) {
        if (nodeName == _uiState.value.selectedNode) return

        _uiState.update {
            it.copy(
                selectedNode = nodeName,
                tasks = emptyList(),
                errorMessage = null,
                actionInProgressUpid = null
            )
        }
        loadTasks()
    }

    fun applyFilters(filters: TaskFilters) {
        _uiState.update {
            it.copy(
                filters = filters,
                tasks = emptyList(),
                errorMessage = null
            )
        }
        loadTasks()
    }

    fun clearFilters() {
        applyFilters(TaskFilters())
    }

    fun abortTask(task: Task) {
        val nodeName = _uiState.value.selectedNode
        val upid = task.taskUpid()

        if (nodeName.isNullOrBlank() || upid.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = invalidTaskMessage) }
            return
        }

        if (_uiState.value.actionInProgressUpid != null) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(actionInProgressUpid = upid, errorMessage = null)
            }

            when (val result = repository.abortTask(nodeName, upid)) {
                is TaskResult.Success -> {
                    _uiState.update {
                        it.copy(
                            actionInProgressUpid = null,
                            pendingActionNotice = TaskActionNotice(
                                upid = upid,
                                type = task.type
                            )
                        )
                    }
                    loadTasks(showLoading = false)
                }
                is TaskResult.Error -> {
                    _uiState.update {
                        it.copy(
                            actionInProgressUpid = null,
                            errorMessage = result.message,
                            pendingActionNotice = TaskActionNotice(
                                upid = upid,
                                type = task.type,
                                errorMessage = result.message
                            )
                        )
                    }
                }
            }
        }
    }

    fun consumeActionNotice() {
        _uiState.update { it.copy(pendingActionNotice = null) }
    }
}

class TaskDetailViewModel(
    private val nodeName: String,
    private val upid: String,
    private val repository: TaskRepository,
    private val invalidTaskMessage: String,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val _uiState = MutableStateFlow(TaskDetailUiState(nodeName = nodeName, upid = upid))
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    fun loadTaskDetail(showLoading: Boolean = true) {
        if (nodeName.isBlank() || upid.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = invalidTaskMessage
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

            when (val result = repository.getTaskDetail(nodeName, upid)) {
                is TaskResult.Success -> {
                    _uiState.update {
                        it.copy(
                            detail = result.data,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null,
                            lastRefreshTimeMillis = clock()
                        )
                    }
                }
                is TaskResult.Error -> {
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

    fun abortTask() {
        if (nodeName.isBlank() || upid.isBlank()) {
            _uiState.update { it.copy(errorMessage = invalidTaskMessage) }
            return
        }

        if (_uiState.value.isAborting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAborting = true, errorMessage = null) }

            when (val result = repository.abortTask(nodeName, upid)) {
                is TaskResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isAborting = false,
                            pendingActionNotice = TaskActionNotice(
                                upid = upid,
                                type = it.detail?.task?.type.orEmpty()
                            )
                        )
                    }
                    loadTaskDetail(showLoading = false)
                }
                is TaskResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isAborting = false,
                            errorMessage = result.message,
                            pendingActionNotice = TaskActionNotice(
                                upid = upid,
                                type = it.detail?.task?.type.orEmpty(),
                                errorMessage = result.message
                            )
                        )
                    }
                }
            }
        }
    }

    fun consumeActionNotice() {
        _uiState.update { it.copy(pendingActionNotice = null) }
    }
}

data class TaskListUiState(
    val availableNodes: List<String>,
    val selectedNode: String?,
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val lastRefreshTimeMillis: Long = System.currentTimeMillis(),
    val filters: TaskFilters = TaskFilters(),
    val actionInProgressUpid: String? = null,
    val pendingActionNotice: TaskActionNotice? = null
)

data class TaskDetailUiState(
    val nodeName: String,
    val upid: String,
    val detail: TaskDetail? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isAborting: Boolean = false,
    val errorMessage: String? = null,
    val lastRefreshTimeMillis: Long = System.currentTimeMillis(),
    val pendingActionNotice: TaskActionNotice? = null
)

data class TaskActionNotice(
    val upid: String,
    val type: String,
    val errorMessage: String? = null
)
