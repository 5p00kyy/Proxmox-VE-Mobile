package com.proxmoxmobile.presentation.screens.vms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.navigation.NavController
import com.proxmoxmobile.data.vm.ProxmoxVmApi
import com.proxmoxmobile.data.vm.VmPowerAction
import com.proxmoxmobile.data.vm.VmRepository
import com.proxmoxmobile.data.model.VirtualMachine
import com.proxmoxmobile.presentation.navigation.Screen
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import com.proxmoxmobile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VMListScreen(
    navController: NavController,
    viewModel: MainViewModel,
    nodeName: String? = null
) {
    val startSuccessTemplate = stringResource(R.string.vm_start_success)
    val startFailedTemplate = stringResource(R.string.vm_start_failed)
    val shutdownSuccessTemplate = stringResource(R.string.vm_shutdown_success)
    val shutdownFailedTemplate = stringResource(R.string.vm_shutdown_failed)
    val stopSuccessTemplate = stringResource(R.string.vm_stop_success)
    val stopFailedTemplate = stringResource(R.string.vm_stop_failed)
    val rebootSuccessTemplate = stringResource(R.string.vm_reboot_success)
    val rebootFailedTemplate = stringResource(R.string.vm_reboot_failed)
    val deleteSuccessTemplate = stringResource(R.string.vm_delete_success)
    val deleteFailedTemplate = stringResource(R.string.vm_delete_failed)
    val shutdownDialogTitle = stringResource(R.string.vm_shutdown_dialog_title)
    val shutdownDialogMessageTemplate = stringResource(R.string.vm_shutdown_dialog_message)
    val stopDialogTitle = stringResource(R.string.vm_stop_dialog_title)
    val stopDialogMessageTemplate = stringResource(R.string.vm_stop_dialog_message)
    val rebootDialogTitle = stringResource(R.string.vm_reboot_dialog_title)
    val rebootDialogMessageTemplate = stringResource(R.string.vm_reboot_dialog_message)
    val deleteDialogTitle = stringResource(R.string.vm_delete_dialog_title)
    val deleteDialogMessageTemplate = stringResource(R.string.vm_delete_dialog_message)
    val taskIdLabel = stringResource(R.string.vm_task_id_label)
    val viewTaskLabel = stringResource(R.string.vm_view_task)
    val vmRepository = remember(viewModel) {
        VmRepository(ProxmoxVmApi { viewModel.getApiService() })
    }
    val vmListViewModel: VmListViewModel = composeViewModel(
        key = "vm-list-${nodeName.orEmpty()}",
        factory = remember(nodeName, vmRepository) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(VmListViewModel::class.java)) {
                        return VmListViewModel(nodeName, vmRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    )
    val uiState by vmListViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(nodeName, vmListViewModel) {
        vmListViewModel.loadVirtualMachines()
        while (!nodeName.isNullOrBlank()) {
            delay(15000) // Refresh every 15 seconds for VMs
            vmListViewModel.loadVirtualMachines(showLoading = false)
        }
    }

    LaunchedEffect(uiState.pendingActionNotice) {
        val notice = uiState.pendingActionNotice ?: return@LaunchedEffect
        val message = buildVmActionMessage(
            notice = notice,
            startSuccessTemplate = startSuccessTemplate,
            startFailedTemplate = startFailedTemplate,
            shutdownSuccessTemplate = shutdownSuccessTemplate,
            shutdownFailedTemplate = shutdownFailedTemplate,
            stopSuccessTemplate = stopSuccessTemplate,
            stopFailedTemplate = stopFailedTemplate,
            rebootSuccessTemplate = rebootSuccessTemplate,
            rebootFailedTemplate = rebootFailedTemplate,
            deleteSuccessTemplate = deleteSuccessTemplate,
            deleteFailedTemplate = deleteFailedTemplate,
            taskIdLabel = taskIdLabel
        )
        val taskId = notice.taskId
        val taskNode = nodeName?.takeIf { it.isNotBlank() }
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = viewTaskLabel.takeIf { taskId != null && taskNode != null }
        )
        if (result == SnackbarResult.ActionPerformed && taskId != null && taskNode != null) {
            navController.navigate(Screen.TaskDetail.createRoute(taskNode, taskId))
        }
        vmListViewModel.consumeActionNotice()
    }

    fun actionInProgressFor(vmid: Int): VmPowerAction? {
        return uiState.actionInProgress?.takeIf { it.vmid == vmid }?.action
    }

    fun showShutdownConfirmation(vm: VirtualMachine) {
        viewModel.showConfirmationDialog(
            MainViewModel.ConfirmationDialog(
                title = shutdownDialogTitle,
                message = String.format(shutdownDialogMessageTemplate, vm.name, vm.vmid),
                onConfirm = {
                    viewModel.hideConfirmationDialog()
                    vmListViewModel.shutdownVirtualMachine(vm)
                },
                onDismiss = {
                    viewModel.hideConfirmationDialog()
                }
            )
        )
    }

    fun showStopConfirmation(vm: VirtualMachine) {
        viewModel.showConfirmationDialog(
            MainViewModel.ConfirmationDialog(
                title = stopDialogTitle,
                message = String.format(stopDialogMessageTemplate, vm.name, vm.vmid),
                onConfirm = {
                    viewModel.hideConfirmationDialog()
                    vmListViewModel.stopVirtualMachine(vm)
                },
                onDismiss = {
                    viewModel.hideConfirmationDialog()
                }
            )
        )
    }

    fun showRebootConfirmation(vm: VirtualMachine) {
        viewModel.showConfirmationDialog(
            MainViewModel.ConfirmationDialog(
                title = rebootDialogTitle,
                message = String.format(rebootDialogMessageTemplate, vm.name, vm.vmid),
                onConfirm = {
                    viewModel.hideConfirmationDialog()
                    vmListViewModel.rebootVirtualMachine(vm)
                },
                onDismiss = {
                    viewModel.hideConfirmationDialog()
                }
            )
        )
    }

    fun showDeleteConfirmation(vm: VirtualMachine) {
        viewModel.showConfirmationDialog(
            MainViewModel.ConfirmationDialog(
                title = deleteDialogTitle,
                message = String.format(deleteDialogMessageTemplate, vm.name, vm.vmid),
                onConfirm = {
                    viewModel.hideConfirmationDialog()
                    vmListViewModel.deleteVirtualMachine(vm)
                },
                onDismiss = {
                    viewModel.hideConfirmationDialog()
                }
            )
        )
    }

    fun refreshNow() {
        scope.launch {
            vmListViewModel.loadVirtualMachines(showLoading = false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.vm_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.vm_back))
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.vm_settings))
                    }
                    IconButton(onClick = {
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.vm_logout))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Last refresh indicator
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.vm_last_updated, formatTimeAgo(uiState.lastRefreshTimeMillis)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = { refreshNow() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.vm_refresh)
                        )
                    }
                }
            }

            uiState.lastTaskNotice?.let { taskNotice ->
                val taskId = taskNotice.taskId ?: return@let
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(
                                    R.string.vm_last_task_action,
                                    taskNotice.action.toDisplayLabel(),
                                    taskNotice.vmName,
                                    taskNotice.vmid
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = stringResource(R.string.vm_last_task_id, taskId),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            nodeName?.takeIf { it.isNotBlank() }?.let { taskNode ->
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        navController.navigate(Screen.TaskDetail.createRoute(taskNode, taskId))
                                    }
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = stringResource(R.string.vm_view_task))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.vm_view_task))
                                }
                            }
                        }
                    }
                }
            }

            // Error message
            if (!uiState.errorMessage.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = uiState.errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Loading indicator
            if (uiState.isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.vm_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // VM list
            items(uiState.vms) { vm ->
                VMCard(
                    vm = vm,
                    actionInProgress = actionInProgressFor(vm.vmid),
                    onStart = { vmListViewModel.startVirtualMachine(vm) },
                    onShutdown = { showShutdownConfirmation(vm) },
                    onStop = { showStopConfirmation(vm) },
                    onReboot = { showRebootConfirmation(vm) },
                    onDelete = { showDeleteConfirmation(vm) },
                    detailEnabled = !nodeName.isNullOrBlank(),
                    onDetails = {
                        nodeName?.takeIf { it.isNotBlank() }?.let { node ->
                            navController.navigate(Screen.VMDetailWithNode.createRoute(node, vm.vmid))
                        }
                    },
                    taskHistoryEnabled = !nodeName.isNullOrBlank(),
                    onTasks = {
                        nodeName?.takeIf { it.isNotBlank() }?.let { node ->
                            navController.navigate(Screen.ResourceTasks.createRoute(node, vm.vmid))
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VMCard(
    vm: VirtualMachine,
    actionInProgress: VmPowerAction? = null,
    onStart: () -> Unit = {},
    onShutdown: () -> Unit = {},
    onStop: () -> Unit = {},
    onReboot: () -> Unit = {},
    onDelete: () -> Unit = {},
    detailEnabled: Boolean = true,
    onDetails: () -> Unit = {},
    taskHistoryEnabled: Boolean = true,
    onTasks: () -> Unit = {}
) {
    val hasActionInProgress = actionInProgress != null

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with VM name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = null,
                    tint = when (vm.status) {
                        "running" -> Color.Green
                        "stopped" -> Color.Red
                        "paused" -> Color.Yellow
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vm.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.vm_id_status, vm.vmid, vm.status.uppercase()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = when (vm.status) {
                                "running" -> Color.Green
                                "stopped" -> Color.Red
                                "paused" -> Color.Yellow
                                else -> Color.Gray
                            },
                            shape = CircleShape
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // VM details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                                    VMDetailItem(
                        label = stringResource(R.string.vm_cpu),
                        value = stringResource(R.string.vm_cpu_value, vm.cpu),
                        color = MaterialTheme.colorScheme.primary
                    )
                    VMDetailItem(
                        label = stringResource(R.string.vm_memory),
                        value = stringResource(R.string.vm_memory_value, vm.mem / 1024.0 / 1024.0 / 1024.0),
                        color = MaterialTheme.colorScheme.secondary
                    )
                VMDetailItem(
                    label = stringResource(R.string.vm_uptime),
                    value = formatUptime(vm.uptime),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            
            // Action buttons
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStart,
                    enabled = vm.status != "running" && !hasActionInProgress,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    if (actionInProgress == VmPowerAction.Start) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.vm_start))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.vm_start))
                }
                
                Button(
                    onClick = onShutdown,
                    enabled = vm.status == "running" && !hasActionInProgress,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    if (actionInProgress == VmPowerAction.Shutdown) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.PowerSettingsNew, contentDescription = stringResource(R.string.vm_shutdown))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.vm_shutdown))
                }

                OutlinedButton(
                    onClick = onReboot,
                    enabled = vm.status == "running" && !hasActionInProgress,
                    modifier = Modifier.weight(1f)
                ) {
                    if (actionInProgress == VmPowerAction.Reboot) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.vm_reboot))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.vm_reboot))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onStop,
                enabled = vm.status == "running" && !hasActionInProgress,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (actionInProgress == VmPowerAction.Stop) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.vm_stop))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.vm_stop))
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDelete,
                enabled = !hasActionInProgress,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (actionInProgress == VmPowerAction.Delete) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.vm_delete))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.vm_delete))
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDetails,
                enabled = !hasActionInProgress && detailEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Info, contentDescription = stringResource(R.string.vm_view_details))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.vm_view_details))
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onTasks,
                enabled = !hasActionInProgress && taskHistoryEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.vm_view_tasks))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.vm_view_tasks))
            }
        }
    }
}

@Composable
fun VMDetailItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.7f)
        )
    }
}

private fun buildVmActionMessage(
    notice: VmActionNotice,
    startSuccessTemplate: String,
    startFailedTemplate: String,
    shutdownSuccessTemplate: String,
    shutdownFailedTemplate: String,
    stopSuccessTemplate: String,
    stopFailedTemplate: String,
    rebootSuccessTemplate: String,
    rebootFailedTemplate: String,
    deleteSuccessTemplate: String,
    deleteFailedTemplate: String,
    taskIdLabel: String
): String {
    val baseMessage = if (notice.errorMessage == null) {
        when (notice.action) {
            VmPowerAction.Start -> String.format(startSuccessTemplate, notice.vmName)
            VmPowerAction.Shutdown -> String.format(shutdownSuccessTemplate, notice.vmName)
            VmPowerAction.Stop -> String.format(stopSuccessTemplate, notice.vmName)
            VmPowerAction.Reboot -> String.format(rebootSuccessTemplate, notice.vmName)
            VmPowerAction.Delete -> String.format(deleteSuccessTemplate, notice.vmName)
        }
    } else {
        when (notice.action) {
            VmPowerAction.Start -> String.format(startFailedTemplate, notice.errorMessage)
            VmPowerAction.Shutdown -> String.format(shutdownFailedTemplate, notice.errorMessage)
            VmPowerAction.Stop -> String.format(stopFailedTemplate, notice.errorMessage)
            VmPowerAction.Reboot -> String.format(rebootFailedTemplate, notice.errorMessage)
            VmPowerAction.Delete -> String.format(deleteFailedTemplate, notice.errorMessage)
        }
    }

    return notice.taskId?.let { "$baseMessage\n$taskIdLabel: $it" } ?: baseMessage
}

@Composable
private fun VmPowerAction.toDisplayLabel(): String {
    return when (this) {
        VmPowerAction.Start -> stringResource(R.string.vm_start)
        VmPowerAction.Shutdown -> stringResource(R.string.vm_shutdown)
        VmPowerAction.Stop -> stringResource(R.string.vm_stop)
        VmPowerAction.Reboot -> stringResource(R.string.vm_reboot)
        VmPowerAction.Delete -> stringResource(R.string.vm_delete)
    }
}

// Helper function to format time ago
fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}

// Helper function to format uptime
fun formatUptime(uptime: Long): String {
    return when {
        uptime < 60 -> "${uptime}s"
        uptime < 3600 -> "${uptime / 60}m"
        uptime < 86400 -> "${uptime / 3600}h"
        else -> "${uptime / 86400}d"
    }
}
