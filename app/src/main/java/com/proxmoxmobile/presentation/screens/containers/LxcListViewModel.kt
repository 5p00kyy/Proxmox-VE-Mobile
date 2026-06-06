package com.proxmoxmobile.presentation.screens.containers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proxmoxmobile.data.lxc.LxcPowerAction
import com.proxmoxmobile.data.lxc.LxcRepository
import com.proxmoxmobile.data.lxc.LxcResult
import com.proxmoxmobile.data.model.Container
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LxcListViewModel(
    private val nodeName: String?,
    private val repository: LxcRepository,
    private val invalidNodeMessage: String,
    private val deleteRequiresStoppedMessage: String,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val _uiState = MutableStateFlow(LxcListUiState(nodeName = nodeName))
    val uiState: StateFlow<LxcListUiState> = _uiState.asStateFlow()

    fun loadContainers(showLoading: Boolean = true) {
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

            when (val result = repository.getContainers(node)) {
                is LxcResult.Success -> {
                    _uiState.update {
                        it.copy(
                            containers = result.data,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null,
                            lastRefreshTimeMillis = clock()
                        )
                    }
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

    fun startContainer(container: Container) {
        performAction(container, LxcPowerAction.Start)
    }

    fun shutdownContainer(container: Container) {
        performAction(container, LxcPowerAction.Shutdown)
    }

    fun stopContainer(container: Container) {
        performAction(container, LxcPowerAction.Stop)
    }

    fun rebootContainer(container: Container) {
        performAction(container, LxcPowerAction.Reboot)
    }

    fun deleteContainer(container: Container) {
        performAction(container, LxcPowerAction.Delete)
    }

    fun consumeActionNotice() {
        _uiState.update { it.copy(pendingActionNotice = null) }
    }

    private fun performAction(container: Container, action: LxcPowerAction) {
        val node = nodeName
        if (node.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = invalidNodeMessage) }
            return
        }

        if (action == LxcPowerAction.Delete && !container.status.equals("stopped", ignoreCase = true)) {
            _uiState.update {
                it.copy(
                    errorMessage = deleteRequiresStoppedMessage,
                    pendingActionNotice = LxcActionNotice(
                        vmid = container.vmid,
                        containerName = container.name,
                        action = action,
                        taskId = null,
                        errorMessage = deleteRequiresStoppedMessage
                    )
                )
            }
            return
        }

        if (_uiState.value.actionInProgress != null) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    actionInProgress = LxcActionInProgress(action = action, vmid = container.vmid),
                    errorMessage = null
                )
            }

            val result = if (action == LxcPowerAction.Delete) {
                repository.deleteContainer(node, container.vmid)
            } else {
                repository.performAction(node, container.vmid, action)
            }

            when (result) {
                is LxcResult.Success -> {
                    _uiState.update {
                        val notice = LxcActionNotice(
                            vmid = container.vmid,
                            containerName = container.name,
                            action = action,
                            taskId = result.data.taskId
                        )
                        it.copy(
                            actionInProgress = null,
                            pendingActionNotice = notice,
                            lastTaskNotice = notice.takeIf { taskNotice -> taskNotice.taskId != null }
                        )
                    }
                    loadContainers(showLoading = false)
                }
                is LxcResult.Error -> {
                    _uiState.update {
                        it.copy(
                            actionInProgress = null,
                            errorMessage = result.message,
                            pendingActionNotice = LxcActionNotice(
                                vmid = container.vmid,
                                containerName = container.name,
                                action = action,
                                taskId = null,
                                errorMessage = result.message
                            )
                        )
                    }
                }
            }
        }
    }
}

data class LxcListUiState(
    val nodeName: String?,
    val containers: List<Container> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val lastRefreshTimeMillis: Long = System.currentTimeMillis(),
    val actionInProgress: LxcActionInProgress? = null,
    val pendingActionNotice: LxcActionNotice? = null,
    val lastTaskNotice: LxcActionNotice? = null
)

data class LxcActionInProgress(
    val action: LxcPowerAction,
    val vmid: Int
)

data class LxcActionNotice(
    val vmid: Int,
    val containerName: String,
    val action: LxcPowerAction,
    val taskId: String?,
    val errorMessage: String? = null
)
