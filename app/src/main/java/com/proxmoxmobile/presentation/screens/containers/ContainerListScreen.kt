@file:OptIn(ExperimentalMaterial3Api::class)
package com.proxmoxmobile.presentation.screens.containers

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
import com.proxmoxmobile.data.lxc.LxcPowerAction
import com.proxmoxmobile.data.lxc.LxcRepository
import com.proxmoxmobile.data.lxc.ProxmoxLxcApi
import com.proxmoxmobile.data.model.Container
import com.proxmoxmobile.data.model.LxcSnapshot
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.proxmoxmobile.presentation.navigation.Screen
import com.proxmoxmobile.presentation.navigation.taskDetailRouteForNotice
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.proxmoxmobile.R


fun Double.format(digits: Int) = String.format(Locale.US, "%.${digits}f", this)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerListScreen(
    navController: NavController,
    viewModel: MainViewModel,
    nodeName: String? = null
) {
    val invalidNodeMsg = stringResource(R.string.container_invalid_node)
    val startSuccessTemplate = stringResource(R.string.container_start_success)
    val startErrorTemplate = stringResource(R.string.container_start_error)
    val shutdownSuccessTemplate = stringResource(R.string.container_shutdown_success)
    val shutdownErrorTemplate = stringResource(R.string.container_shutdown_error)
    val stopSuccessTemplate = stringResource(R.string.container_stop_success)
    val stopErrorTemplate = stringResource(R.string.container_stop_error)
    val rebootSuccessTemplate = stringResource(R.string.container_reboot_success)
    val rebootErrorTemplate = stringResource(R.string.container_reboot_error)
    val deleteSuccessTemplate = stringResource(R.string.container_delete_success)
    val deleteErrorTemplate = stringResource(R.string.container_delete_error)
    val shutdownTitle = stringResource(R.string.container_shutdown_title)
    val shutdownMessageTemplate = stringResource(R.string.container_shutdown_message)
    val stopTitle = stringResource(R.string.container_stop_title)
    val stopMessageTemplate = stringResource(R.string.container_stop_message)
    val rebootTitle = stringResource(R.string.container_reboot_title)
    val rebootMessageTemplate = stringResource(R.string.container_reboot_message)
    val deleteTitle = stringResource(R.string.container_delete_title)
    val deleteMessageTemplate = stringResource(R.string.container_delete_message)
    val deleteRequiresStoppedMessage = stringResource(R.string.container_delete_requires_stopped)
    val taskIdLabel = stringResource(R.string.container_task_id_label)
    val viewTaskLabel = stringResource(R.string.container_view_task)
    val snackbarHostState = remember { SnackbarHostState() }
    val lxcRepository = remember(viewModel) {
        LxcRepository(ProxmoxLxcApi { viewModel.getApiService() })
    }
    val lxcListViewModel: LxcListViewModel = composeViewModel(
        key = "lxc-list-${nodeName.orEmpty()}",
        factory = remember(nodeName, lxcRepository, invalidNodeMsg, deleteRequiresStoppedMessage) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(LxcListViewModel::class.java)) {
                        return LxcListViewModel(
                            nodeName = nodeName,
                            repository = lxcRepository,
                            invalidNodeMessage = invalidNodeMsg,
                            deleteRequiresStoppedMessage = deleteRequiresStoppedMessage
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    )
    val uiState by lxcListViewModel.uiState.collectAsState()

    LaunchedEffect(nodeName, lxcListViewModel) {
        lxcListViewModel.loadContainers()
        while (!nodeName.isNullOrBlank()) {
            delay(15000) // Refresh every 15 seconds for containers
            lxcListViewModel.loadContainers(showLoading = false)
        }
    }

    LaunchedEffect(uiState.pendingActionNotice) {
        val notice = uiState.pendingActionNotice ?: return@LaunchedEffect
        val message = buildLxcActionMessage(
            notice = notice,
            startSuccessTemplate = startSuccessTemplate,
            startErrorTemplate = startErrorTemplate,
            shutdownSuccessTemplate = shutdownSuccessTemplate,
            shutdownErrorTemplate = shutdownErrorTemplate,
            stopSuccessTemplate = stopSuccessTemplate,
            stopErrorTemplate = stopErrorTemplate,
            rebootSuccessTemplate = rebootSuccessTemplate,
            rebootErrorTemplate = rebootErrorTemplate,
            deleteSuccessTemplate = deleteSuccessTemplate,
            deleteErrorTemplate = deleteErrorTemplate,
            taskIdLabel = taskIdLabel
        )
        val taskRoute = taskDetailRouteForNotice(nodeName, notice.taskId)
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = viewTaskLabel.takeIf { taskRoute != null }
        )
        if (result == SnackbarResult.ActionPerformed && taskRoute != null) {
            navController.navigate(taskRoute)
        }
        lxcListViewModel.consumeActionNotice()
    }

    fun actionInProgressFor(vmid: Int): LxcPowerAction? {
        return uiState.actionInProgress?.takeIf { it.vmid == vmid }?.action
    }

    fun showShutdownConfirmation(container: Container) {
        viewModel.showConfirmationDialog(
            MainViewModel.ConfirmationDialog(
                title = shutdownTitle,
                message = String.format(shutdownMessageTemplate, container.name, container.vmid),
                onConfirm = {
                    viewModel.hideConfirmationDialog()
                    lxcListViewModel.shutdownContainer(container)
                },
                onDismiss = {
                    viewModel.hideConfirmationDialog()
                }
            )
        )
    }

    fun showStopConfirmation(container: Container) {
        viewModel.showConfirmationDialog(
            MainViewModel.ConfirmationDialog(
                title = stopTitle,
                message = String.format(stopMessageTemplate, container.name, container.vmid),
                onConfirm = {
                    viewModel.hideConfirmationDialog()
                    lxcListViewModel.stopContainer(container)
                },
                onDismiss = {
                    viewModel.hideConfirmationDialog()
                }
            )
        )
    }

    fun showRebootConfirmation(container: Container) {
        viewModel.showConfirmationDialog(
            MainViewModel.ConfirmationDialog(
                title = rebootTitle,
                message = String.format(rebootMessageTemplate, container.name, container.vmid),
                onConfirm = {
                    viewModel.hideConfirmationDialog()
                    lxcListViewModel.rebootContainer(container)
                },
                onDismiss = {
                    viewModel.hideConfirmationDialog()
                }
            )
        )
    }

    fun showDeleteConfirmation(container: Container) {
        viewModel.showConfirmationDialog(
            MainViewModel.ConfirmationDialog(
                title = deleteTitle,
                message = String.format(deleteMessageTemplate, container.name, container.vmid),
                onConfirm = {
                    viewModel.hideConfirmationDialog()
                    lxcListViewModel.deleteContainer(container)
                },
                onDismiss = {
                    viewModel.hideConfirmationDialog()
                }
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.container_title_lxc),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.container_back))
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.container_settings))
                    }
                    IconButton(onClick = {
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.container_logout))
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
                        text = stringResource(R.string.container_last_updated, formatTimeAgo(uiState.lastRefreshTimeMillis)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = { lxcListViewModel.loadContainers(showLoading = false) }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.container_refresh)
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
                                    R.string.container_last_task_action,
                                    taskNotice.action.toDisplayLabel(),
                                    taskNotice.containerName,
                                    taskNotice.vmid
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = stringResource(R.string.container_last_task_id, taskId),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            taskDetailRouteForNotice(nodeName, taskId)?.let { taskRoute ->
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        navController.navigate(taskRoute)
                                    }
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = stringResource(R.string.container_view_task))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.container_view_task))
                                }
                            }
                        }
                    }
                }
            }

            // Error message
            if (!uiState.errorMessage.isNullOrBlank()) {
                val listErrorMessage = uiState.errorMessage.orEmpty()
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
                                text = listErrorMessage,
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
                                text = stringResource(R.string.container_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Container list
            items(uiState.containers) { container ->
                ContainerCard(
                    container = container,
                    actionInProgress = actionInProgressFor(container.vmid),
                    onStart = { lxcListViewModel.startContainer(container) },
                    onShutdown = { showShutdownConfirmation(container) },
                    onStop = { showStopConfirmation(container) },
                    onReboot = { showRebootConfirmation(container) },
                    onDelete = { showDeleteConfirmation(container) },
                    detailEnabled = !nodeName.isNullOrBlank(),
                    onDetails = {
                        nodeName?.takeIf { it.isNotBlank() }?.let { node ->
                            navController.navigate(Screen.ContainerDetailWithNode.createRoute(node, container.vmid))
                        }
                    },
                    taskHistoryEnabled = !nodeName.isNullOrBlank(),
                    onTasks = {
                        nodeName?.takeIf { it.isNotBlank() }?.let { node ->
                            navController.navigate(Screen.ResourceTasks.createRoute(node, container.vmid))
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerCard(
    container: Container,
    actionInProgress: LxcPowerAction? = null,
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header with container info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = when (container.status) {
                        "running" -> Color.Green
                        "stopped" -> Color.Red
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = container.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.container_id_status, container.vmid, container.status.uppercase()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = when (container.status) {
                                "running" -> Color.Green
                                "stopped" -> Color.Red
                                else -> Color.Gray
                            },
                            shape = CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Resource usage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ContainerDetailItem(
                    label = stringResource(R.string.container_cpu),
                    value = stringResource(R.string.container_cpu_value, container.cpu),
                    color = MaterialTheme.colorScheme.primary
                )
                ContainerDetailItem(
                    label = stringResource(R.string.container_memory),
                    value = stringResource(R.string.container_memory_value, container.mem / 1024.0 / 1024.0 / 1024.0),
                    color = MaterialTheme.colorScheme.secondary
                )
                ContainerDetailItem(
                    label = stringResource(R.string.container_uptime),
                    value = formatUptime(container.uptime),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // Action buttons
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStart,
                    enabled = container.status != "running" && !hasActionInProgress,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    if (actionInProgress == LxcPowerAction.Start) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.container_start),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = stringResource(R.string.container_start),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onShutdown,
                    enabled = container.status == "running" && !hasActionInProgress,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    if (actionInProgress == LxcPowerAction.Shutdown) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.PowerSettingsNew,
                            contentDescription = stringResource(R.string.container_shutdown),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = stringResource(R.string.container_shutdown),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onReboot,
                    enabled = container.status == "running" && !hasActionInProgress,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    if (actionInProgress == LxcPowerAction.Reboot) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.container_reboot),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = stringResource(R.string.container_reboot),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onStop,
                enabled = container.status == "running" && !hasActionInProgress,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                if (actionInProgress == LxcPowerAction.Stop) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.container_stop))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.container_stop),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            val deleteEnabled = container.status.equals("stopped", ignoreCase = true) && !hasActionInProgress
            val deleteLabel = if (container.status.equals("stopped", ignoreCase = true)) {
                stringResource(R.string.container_delete)
            } else {
                stringResource(R.string.container_delete_requires_stopped)
            }
            OutlinedButton(
                onClick = onDelete,
                enabled = deleteEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                if (actionInProgress == LxcPowerAction.Delete) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Delete, contentDescription = deleteLabel)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(deleteLabel)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDetails,
                    enabled = !hasActionInProgress && detailEnabled,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = stringResource(R.string.container_view_details))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.container_view_details),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onTasks,
                    enabled = !hasActionInProgress && taskHistoryEnabled,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.container_view_tasks))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.container_view_tasks),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ContainerDetailItem(
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

// Add ContainerDetailScreen
@Composable
fun ContainerDetailScreen(
    vmid: Int,
    nodeName: String? = null,
    viewModel: MainViewModel,
    navController: NavController
) {
    val candidateNodeNames = remember(viewModel, nodeName) {
        (
            listOfNotNull(nodeName?.takeIf { it.isNotBlank() }) +
                viewModel.getCachedNodes().orEmpty().map { it.node }.filter { it.isNotBlank() }
            )
            .distinct()
    }
    val lxcRepository = remember(viewModel) {
        LxcRepository(ProxmoxLxcApi { viewModel.getApiService() })
    }
    val lxcDetailViewModel: LxcDetailViewModel = composeViewModel(
        key = "lxc-detail-${nodeName.orEmpty()}-$vmid",
        factory = remember(vmid, candidateNodeNames, nodeName, lxcRepository) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(LxcDetailViewModel::class.java)) {
                        return LxcDetailViewModel(
                            vmid = vmid,
                            nodeNames = candidateNodeNames,
                            preferredNodeName = nodeName,
                            repository = lxcRepository
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    )
    val uiState by lxcDetailViewModel.uiState.collectAsState()
    val container = uiState.container
    val errorMessage = uiState.errorMessage
    val cpu = container?.cpu ?: 0.0
    val ram = container?.mem ?: 0L
    val maxRam = container?.maxmem ?: 0L
    val isUpdatingResources = false
    var showCpuConfigDialog by remember { mutableStateOf(false) }
    var showRamConfigDialog by remember { mutableStateOf(false) }
    var tempCpuCores by remember(container?.vmid, container?.cpus) {
        mutableStateOf(container?.cpus ?: 1)
    }
    var tempRamAllocation by remember(container?.vmid, container?.maxmem) {
        mutableStateOf(container?.maxmem ?: 512L * 1024L * 1024L)
    }
    val resourceApiNotImplMsg = stringResource(R.string.container_resource_api_not_implemented)

    LaunchedEffect(lxcDetailViewModel) {
        lxcDetailViewModel.loadContainer()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.container_details_title, vmid)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.container_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { lxcDetailViewModel.loadContainer(showLoading = false) },
                        enabled = !uiState.isLoading && !uiState.isRefreshing
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.container_refresh))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (container != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Container Info Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.container_information),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(stringResource(R.string.container_name_label, container.name))
                            Text(stringResource(R.string.container_id_label, container.vmid))
                            Text(
                                text = stringResource(R.string.container_status_label, container.status),
                                color = when (container.status) {
                                    "running" -> Color.Green
                                    "stopped" -> Color.Red
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(stringResource(R.string.container_uptime_hours, (container.uptime / 3600).toInt()))
                        }
                    }
                }

                // Current Resource Usage Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.container_current_resource_usage),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(stringResource(R.string.container_cpu_usage, (cpu * 100).format(1)))
                            Text(stringResource(R.string.container_cpu_cores_count, container.cpus))
                            Text(stringResource(R.string.container_ram_usage, formatBytes(ram)))
                            Text(stringResource(R.string.container_ram_allocated, formatBytes(maxRam)))
                        }
                    }
                }

                // Resource Management Section
                item {
                    LxcSnapshotsCard(
                        snapshots = uiState.snapshots,
                        isLoading = uiState.isSnapshotsLoading,
                        isRefreshing = uiState.isSnapshotsRefreshing,
                        errorMessage = uiState.snapshotErrorMessage,
                        onRefresh = {
                            lxcDetailViewModel.loadSnapshots(showLoading = false)
                        }
                    )
                }

                // Resource Management Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.container_resource_management),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // CPU Configuration Card
                            ResourceCard(
                                title = stringResource(R.string.container_cpu_cores),
                                currentValue = stringResource(R.string.container_cores_allocated, container.cpus),
                                icon = Icons.Filled.Memory,
                                onClick = { showCpuConfigDialog = true },
                                enabled = false
                            )

                            // RAM Configuration Card
                            ResourceCard(
                                title = stringResource(R.string.container_ram_allocation),
                                currentValue = stringResource(R.string.container_bytes_allocated, formatBytes(maxRam)),
                                icon = Icons.Filled.Storage,
                                onClick = { showRamConfigDialog = true },
                                enabled = false
                            )

                            Text(
                                text = resourceApiNotImplMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Apply Changes Button
                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isUpdatingResources) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Filled.Save, contentDescription = null)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.container_apply_resource_changes))
                            }
                        }
                    }
                }

                // Action Buttons Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.container_actions),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.container_start))
                                }

                                Button(
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Stop, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.container_stop))
                                }
                            }

                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Terminal, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.container_open_console))
                            }
                        }
                    }
                }

                // Error Message
                if (errorMessage != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = errorMessage,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { navController.navigateUp() }) {
                        Text(stringResource(R.string.container_go_back))
                    }
                }
            }
        }
    }

    // CPU Configuration Dialog
    if (showCpuConfigDialog) {
        AlertDialog(
            onDismissRequest = { showCpuConfigDialog = false },
            title = { Text(stringResource(R.string.container_configure_cpu_cores)) },
            text = {
                Column {
                    Text(stringResource(R.string.container_current_allocation_cores, container?.cpus ?: 0))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.container_enter_cpu_cores))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempCpuCores.toString(),
                        onValueChange = {
                            val value = it.toIntOrNull() ?: 1
                            tempCpuCores = value.coerceIn(1, 32)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.container_cpu_cores_placeholder)) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCpuConfigDialog = false
                    }
                ) {
                    Text(stringResource(R.string.container_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCpuConfigDialog = false }) {
                    Text(stringResource(R.string.container_cancel))
                }
            }
        )
    }

    // RAM Configuration Dialog
    if (showRamConfigDialog) {
        AlertDialog(
            onDismissRequest = { showRamConfigDialog = false },
            title = { Text(stringResource(R.string.container_configure_ram_allocation)) },
            text = {
                Column {
                    Text(stringResource(R.string.container_current_allocation_bytes, formatBytes(container?.maxmem ?: 0)))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.container_enter_ram_mb))
                    Spacer(modifier = Modifier.height(8.dp))
                    var ramInputText by remember { mutableStateOf((tempRamAllocation / 1024 / 1024).toString()) }

                    OutlinedTextField(
                        value = ramInputText,
                        onValueChange = { input ->
                            // Only allow digits
                            val cleanInput = input.filter { it.isDigit() }
                            ramInputText = cleanInput

                            // Update the actual value only if input is valid
                            if (cleanInput.isNotEmpty()) {
                                val value = cleanInput.toIntOrNull() ?: 512
                                val mbValue = value.coerceIn(128, 65536)
                                tempRamAllocation = (mbValue * 1024 * 1024).toLong()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.container_ram_placeholder)) }
                    )
                    Text("${formatBytes(tempRamAllocation)}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRamConfigDialog = false
                    }
                ) {
                    Text(stringResource(R.string.container_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRamConfigDialog = false }) {
                    Text(stringResource(R.string.container_cancel))
                }
            }
        )
    }
}

@Composable
private fun LxcSnapshotsCard(
    snapshots: List<LxcSnapshot>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.container_snapshots_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading && !isRefreshing
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.container_snapshots_refresh))
                }
            }

            when {
                isLoading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.container_snapshots_loading))
                    }
                }
                errorMessage != null -> {
                    Text(
                        text = stringResource(R.string.container_snapshots_error, errorMessage),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                snapshots.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.container_snapshots_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    snapshots.forEach { snapshot ->
                        LxcSnapshotRow(snapshot)
                    }
                    Text(
                        text = stringResource(R.string.container_snapshots_actions_planned),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LxcSnapshotRow(snapshot: LxcSnapshot) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = null,
            tint = if (snapshot.name == "current") {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = snapshot.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(
                    R.string.container_snapshot_detail,
                    formatSnapshotTime(snapshot.snaptime),
                    if (snapshot.vmstate == 1) {
                        stringResource(R.string.common_yes)
                    } else {
                        stringResource(R.string.common_no)
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            snapshot.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helper function to format bytes
fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> "${(bytes.toDouble() / 1024 / 1024 / 1024).format(1)}GB"
        bytes >= 1024 * 1024 -> "${(bytes.toDouble() / 1024 / 1024).format(1)}MB"
        bytes >= 1024 -> "${(bytes.toDouble() / 1024).format(1)}KB"
        else -> "${bytes}B"
    }
}

private fun formatSnapshotTime(snaptime: Long?): String {
    val seconds = snaptime?.takeIf { it > 0 } ?: return "-"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(seconds * 1000L))
}

private fun buildLxcActionMessage(
    notice: LxcActionNotice,
    startSuccessTemplate: String,
    startErrorTemplate: String,
    shutdownSuccessTemplate: String,
    shutdownErrorTemplate: String,
    stopSuccessTemplate: String,
    stopErrorTemplate: String,
    rebootSuccessTemplate: String,
    rebootErrorTemplate: String,
    deleteSuccessTemplate: String,
    deleteErrorTemplate: String,
    taskIdLabel: String
): String {
    val baseMessage = if (notice.errorMessage == null) {
        when (notice.action) {
            LxcPowerAction.Start -> String.format(startSuccessTemplate, notice.containerName)
            LxcPowerAction.Shutdown -> String.format(shutdownSuccessTemplate, notice.containerName)
            LxcPowerAction.Stop -> String.format(stopSuccessTemplate, notice.containerName)
            LxcPowerAction.Reboot -> String.format(rebootSuccessTemplate, notice.containerName)
            LxcPowerAction.Delete -> String.format(deleteSuccessTemplate, notice.containerName)
        }
    } else {
        when (notice.action) {
            LxcPowerAction.Start -> String.format(startErrorTemplate, notice.errorMessage)
            LxcPowerAction.Shutdown -> String.format(shutdownErrorTemplate, notice.errorMessage)
            LxcPowerAction.Stop -> String.format(stopErrorTemplate, notice.errorMessage)
            LxcPowerAction.Reboot -> String.format(rebootErrorTemplate, notice.errorMessage)
            LxcPowerAction.Delete -> String.format(deleteErrorTemplate, notice.errorMessage)
        }
    }

    return notice.taskId?.let { "$baseMessage\n$taskIdLabel: $it" } ?: baseMessage
}

@Composable
private fun LxcPowerAction.toDisplayLabel(): String {
    return when (this) {
        LxcPowerAction.Start -> stringResource(R.string.container_start)
        LxcPowerAction.Shutdown -> stringResource(R.string.container_shutdown)
        LxcPowerAction.Stop -> stringResource(R.string.container_stop)
        LxcPowerAction.Reboot -> stringResource(R.string.container_reboot)
        LxcPowerAction.Delete -> stringResource(R.string.container_delete)
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

@Composable
fun ResourceCard(
    title: String,
    currentValue: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = currentValue,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }

            // Edit indicator
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.container_edit),
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
