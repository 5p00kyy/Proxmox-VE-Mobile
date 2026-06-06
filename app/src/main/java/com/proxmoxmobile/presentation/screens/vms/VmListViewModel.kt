package com.proxmoxmobile.presentation.screens.vms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proxmoxmobile.data.model.VirtualMachine
import com.proxmoxmobile.data.vm.VmPowerAction
import com.proxmoxmobile.data.vm.VmRepository
import com.proxmoxmobile.data.vm.VmResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VmListViewModel(
    private val nodeName: String?,
    private val repository: VmRepository,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val _uiState = MutableStateFlow(VmListUiState(nodeName = nodeName))
    val uiState: StateFlow<VmListUiState> = _uiState.asStateFlow()

    fun loadVirtualMachines(showLoading: Boolean = true) {
        val node = nodeName
        if (node.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = "Invalid node name or API service not available"
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

            when (val result = repository.getVirtualMachines(node)) {
                is VmResult.Success -> {
                    _uiState.update {
                        it.copy(
                            vms = result.data,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null,
                            lastRefreshTimeMillis = clock()
                        )
                    }
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

    fun startVirtualMachine(vm: VirtualMachine) {
        performAction(vm, VmPowerAction.Start)
    }

    fun shutdownVirtualMachine(vm: VirtualMachine) {
        performAction(vm, VmPowerAction.Shutdown)
    }

    fun stopVirtualMachine(vm: VirtualMachine) {
        performAction(vm, VmPowerAction.Stop)
    }

    fun rebootVirtualMachine(vm: VirtualMachine) {
        performAction(vm, VmPowerAction.Reboot)
    }

    fun deleteVirtualMachine(vm: VirtualMachine) {
        performAction(vm, VmPowerAction.Delete)
    }

    fun consumeActionNotice() {
        _uiState.update { it.copy(pendingActionNotice = null) }
    }

    private fun performAction(vm: VirtualMachine, action: VmPowerAction) {
        val node = nodeName
        if (node.isNullOrBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Invalid node name or API service not available")
            }
            return
        }

        if (_uiState.value.actionInProgress != null) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    actionInProgress = VmActionInProgress(action = action, vmid = vm.vmid),
                    errorMessage = null
                )
            }

            val result = if (action == VmPowerAction.Delete) {
                repository.deleteVirtualMachine(node, vm.vmid)
            } else {
                repository.performAction(node, vm.vmid, action)
            }

            when (result) {
                is VmResult.Success -> {
                    _uiState.update {
                        val notice = VmActionNotice(
                            vmid = vm.vmid,
                            vmName = vm.name,
                            action = action,
                            taskId = result.data.taskId
                        )
                        it.copy(
                            actionInProgress = null,
                            pendingActionNotice = notice,
                            lastTaskNotice = notice.takeIf { taskNotice -> taskNotice.taskId != null }
                        )
                    }
                    loadVirtualMachines(showLoading = false)
                }
                is VmResult.Error -> {
                    _uiState.update {
                        it.copy(
                            actionInProgress = null,
                            errorMessage = result.message,
                            pendingActionNotice = VmActionNotice(
                                vmid = vm.vmid,
                                vmName = vm.name,
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

data class VmListUiState(
    val nodeName: String?,
    val vms: List<VirtualMachine> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val lastRefreshTimeMillis: Long = System.currentTimeMillis(),
    val actionInProgress: VmActionInProgress? = null,
    val pendingActionNotice: VmActionNotice? = null,
    val lastTaskNotice: VmActionNotice? = null
)

data class VmActionInProgress(
    val action: VmPowerAction,
    val vmid: Int
)

data class VmActionNotice(
    val vmid: Int,
    val vmName: String,
    val action: VmPowerAction,
    val taskId: String?,
    val errorMessage: String? = null
)
