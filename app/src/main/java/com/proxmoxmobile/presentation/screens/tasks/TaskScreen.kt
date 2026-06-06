package com.proxmoxmobile.presentation.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.navigation.NavController
import com.proxmoxmobile.R
import com.proxmoxmobile.data.model.Task
import com.proxmoxmobile.data.model.TaskLogEntry
import com.proxmoxmobile.data.task.ProxmoxTaskApi
import com.proxmoxmobile.data.task.TaskDetail
import com.proxmoxmobile.data.task.TaskFilters
import com.proxmoxmobile.data.task.TaskRepository
import com.proxmoxmobile.data.task.TaskStatusFilter
import com.proxmoxmobile.data.task.taskUpid
import com.proxmoxmobile.presentation.navigation.Screen
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    navController: NavController,
    viewModel: MainViewModel,
    initialNodeName: String? = null,
    initialVmid: Int? = null
) {
    val availableNodes = remember(viewModel, initialNodeName) {
        (viewModel.getCachedNodes()
            ?.map { it.node }
            ?.filter { it.isNotBlank() }
            .orEmpty() + listOfNotNull(initialNodeName?.takeIf { it.isNotBlank() }))
            .distinct()
    }
    val noNodesMessage = stringResource(R.string.task_error_no_nodes)
    val invalidTaskMessage = stringResource(R.string.task_error_invalid_task)
    val abortSuccessTemplate = stringResource(R.string.task_abort_success)
    val abortFailedTemplate = stringResource(R.string.task_abort_failed)
    val abortDialogTitle = stringResource(R.string.task_abort_dialog_title)
    val abortDialogMessageTemplate = stringResource(R.string.task_abort_dialog_message)
    val taskRepository = remember(viewModel) {
        TaskRepository(ProxmoxTaskApi { viewModel.getApiService() })
    }
    val taskListViewModel: TaskListViewModel = composeViewModel(
        key = "task-list-${availableNodes.joinToString("|")}-${initialNodeName.orEmpty()}-${initialVmid ?: 0}",
        factory = remember(availableNodes, initialNodeName, initialVmid, taskRepository, noNodesMessage, invalidTaskMessage) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(TaskListViewModel::class.java)) {
                        return TaskListViewModel(
                            availableNodes = availableNodes,
                            initialNodeName = initialNodeName,
                            initialFilters = TaskFilters(vmid = initialVmid?.takeIf { it > 0 }),
                            repository = taskRepository,
                            noNodesMessage = noNodesMessage,
                            invalidTaskMessage = invalidTaskMessage
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    )
    val uiState by taskListViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(taskListViewModel, uiState.selectedNode) {
        val selectedNode = uiState.selectedNode
        taskListViewModel.loadTasks()
        while (!selectedNode.isNullOrBlank()) {
            delay(10000)
            taskListViewModel.loadTasks(showLoading = false)
        }
    }

    LaunchedEffect(uiState.pendingActionNotice) {
        val notice = uiState.pendingActionNotice ?: return@LaunchedEffect
        val message = if (notice.errorMessage == null) {
            String.format(abortSuccessTemplate, notice.type.ifBlank { notice.upid })
        } else {
            String.format(abortFailedTemplate, notice.errorMessage)
        }
        snackbarHostState.showSnackbar(message)
        taskListViewModel.consumeActionNotice()
    }

    fun showAbortConfirmation(task: Task) {
        viewModel.showConfirmationDialog(
            MainViewModel.ConfirmationDialog(
                title = abortDialogTitle,
                message = String.format(
                    abortDialogMessageTemplate,
                    task.type.ifBlank { task.taskUpid().orEmpty() }
                ),
                onConfirm = {
                    viewModel.hideConfirmationDialog()
                    taskListViewModel.abortTask(task)
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
                        stringResource(R.string.task_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.task_back))
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.task_settings))
                    }
                    IconButton(onClick = {
                        viewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.task_logout))
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
            item {
                TaskRefreshHeader(
                    lastRefreshTime = uiState.lastRefreshTimeMillis,
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { taskListViewModel.loadTasks(showLoading = false) }
                )
            }

            item {
                TaskNodeSelectorCard(
                    availableNodes = uiState.availableNodes,
                    selectedNode = uiState.selectedNode,
                    onNodeSelected = taskListViewModel::selectNode
                )
            }

            item {
                TaskFilterCard(
                    filters = uiState.filters,
                    onApply = taskListViewModel::applyFilters,
                    onClear = taskListViewModel::clearFilters
                )
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                item {
                    TaskErrorCard(message = uiState.errorMessage.orEmpty())
                }
            }

            if (uiState.isLoading) {
                item {
                    TaskLoadingCard(text = stringResource(R.string.task_loading))
                }
            }

            if (uiState.tasks.isNotEmpty()) {
                item {
                    TaskStatisticsCard(uiState.tasks)
                }
            }

            if (!uiState.isLoading && uiState.tasks.isEmpty() && uiState.errorMessage.isNullOrBlank()) {
                item {
                    TaskEmptyCard()
                }
            }

            items(uiState.tasks, key = { it.taskUpid().orEmpty() }) { task ->
                val upid = task.taskUpid()
                TaskCard(
                    task = task,
                    actionInProgress = uiState.actionInProgressUpid == upid,
                    onOpenDetails = {
                        if (!upid.isNullOrBlank()) {
                            navController.navigate(Screen.TaskDetail.createRoute(task.node, upid))
                        }
                    },
                    onAbort = { showAbortConfirmation(task) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    navController: NavController,
    viewModel: MainViewModel,
    nodeName: String,
    upid: String,
    repositoryOverride: TaskRepository? = null
) {
    val invalidTaskMessage = stringResource(R.string.task_error_invalid_task)
    val abortSuccessTemplate = stringResource(R.string.task_abort_success)
    val abortFailedTemplate = stringResource(R.string.task_abort_failed)
    val abortDialogTitle = stringResource(R.string.task_abort_dialog_title)
    val abortDialogMessageTemplate = stringResource(R.string.task_abort_dialog_message)
    val taskRepository = remember(viewModel) {
        TaskRepository(ProxmoxTaskApi { viewModel.getApiService() })
    }
    val activeTaskRepository = repositoryOverride ?: taskRepository
    val taskDetailViewModel: TaskDetailViewModel = composeViewModel(
        key = "task-detail-$nodeName-$upid",
        factory = remember(nodeName, upid, activeTaskRepository, invalidTaskMessage) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(TaskDetailViewModel::class.java)) {
                        return TaskDetailViewModel(
                            nodeName = nodeName,
                            upid = upid,
                            repository = activeTaskRepository,
                            invalidTaskMessage = invalidTaskMessage
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    )
    val uiState by taskDetailViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(taskDetailViewModel) {
        taskDetailViewModel.loadTaskDetail()
    }

    LaunchedEffect(uiState.detail?.task?.status) {
        while (taskDetailViewModel.uiState.value.detail?.task?.status.equals("running", ignoreCase = true)) {
            delay(5000)
            taskDetailViewModel.loadTaskDetail(showLoading = false)
        }
    }

    LaunchedEffect(uiState.pendingActionNotice) {
        val notice = uiState.pendingActionNotice ?: return@LaunchedEffect
        val message = if (notice.errorMessage == null) {
            String.format(abortSuccessTemplate, notice.type.ifBlank { notice.upid })
        } else {
            String.format(abortFailedTemplate, notice.errorMessage)
        }
        snackbarHostState.showSnackbar(message)
        taskDetailViewModel.consumeActionNotice()
    }

    fun showAbortConfirmation() {
        val type = uiState.detail?.task?.type.orEmpty()
        viewModel.showConfirmationDialog(
            MainViewModel.ConfirmationDialog(
                title = abortDialogTitle,
                message = String.format(abortDialogMessageTemplate, type.ifBlank { upid }),
                onConfirm = {
                    viewModel.hideConfirmationDialog()
                    taskDetailViewModel.abortTask()
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
                        stringResource(R.string.task_detail_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.task_back))
                    }
                },
                actions = {
                    IconButton(onClick = { taskDetailViewModel.loadTaskDetail(showLoading = false) }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.task_refresh))
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
            item {
                TaskRefreshHeader(
                    lastRefreshTime = uiState.lastRefreshTimeMillis,
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { taskDetailViewModel.loadTaskDetail(showLoading = false) }
                )
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                item {
                    TaskErrorCard(message = uiState.errorMessage.orEmpty())
                }
            }

            if (uiState.isLoading) {
                item {
                    TaskLoadingCard(text = stringResource(R.string.task_loading_detail))
                }
            }

            uiState.detail?.let { detail ->
                item {
                    TaskDetailSummaryCard(
                        detail = detail,
                        isAborting = uiState.isAborting,
                        onAbort = ::showAbortConfirmation
                    )
                }

                item {
                    Text(
                        text = stringResource(R.string.task_log_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (detail.logEntries.isEmpty()) {
                    item {
                        TaskEmptyLogCard()
                    }
                } else {
                    items(detail.logEntries, key = { it.lineNumber }) { entry ->
                        TaskLogLine(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRefreshHeader(
    lastRefreshTime: Long,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.task_last_updated, formatTimeAgo(lastRefreshTime)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onRefresh, enabled = !isRefreshing) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.task_refresh))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskNodeSelectorCard(
    availableNodes: List<String>,
    selectedNode: String?,
    onNodeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.task_selected_node, selectedNode ?: stringResource(R.string.task_none)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedNode ?: stringResource(R.string.task_none),
                    onValueChange = {},
                    readOnly = true,
                    enabled = availableNodes.isNotEmpty(),
                    label = { Text(stringResource(R.string.task_node_selector_label)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableNodes.forEach { node ->
                        DropdownMenuItem(
                            text = { Text(node) },
                            onClick = {
                                expanded = false
                                onNodeSelected(node)
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.task_monitoring_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskFilterCard(
    filters: TaskFilters,
    onApply: (TaskFilters) -> Unit,
    onClear: () -> Unit
) {
    var selectedStatusName by rememberSaveable { mutableStateOf(filters.status.name) }
    var typeText by rememberSaveable { mutableStateOf(filters.typeFilter.orEmpty()) }
    var vmidText by rememberSaveable { mutableStateOf(filters.vmid?.toString().orEmpty()) }
    var syncedFiltersKey by rememberSaveable { mutableStateOf(filters.toDraftKey()) }
    var statusExpanded by remember { mutableStateOf(false) }
    val selectedStatus = runCatching { TaskStatusFilter.valueOf(selectedStatusName) }
        .getOrDefault(TaskStatusFilter.All)

    LaunchedEffect(filters) {
        val nextFiltersKey = filters.toDraftKey()
        if (nextFiltersKey != syncedFiltersKey) {
            selectedStatusName = filters.status.name
            typeText = filters.typeFilter.orEmpty()
            vmidText = filters.vmid?.toString().orEmpty()
            syncedFiltersKey = nextFiltersKey
        }
    }

    val parsedVmid = vmidText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
    val hasInvalidVmid = vmidText.isNotBlank() && parsedVmid == null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.task_filters_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (filters.hasActiveFilters()) {
                    Text(
                        text = stringResource(R.string.task_filters_active),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = !statusExpanded }
            ) {
                OutlinedTextField(
                    value = selectedStatus.toDisplayLabel(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.task_filter_status)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .testTag(TASK_FILTER_STATUS_TAG)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    TaskStatusFilter.values().forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.toDisplayLabel()) },
                            onClick = {
                                selectedStatusName = status.name
                                statusExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = typeText,
                onValueChange = { typeText = it },
                label = { Text(stringResource(R.string.task_filter_type)) },
                placeholder = { Text(stringResource(R.string.task_filter_type_placeholder)) },
                singleLine = true,
                modifier = Modifier
                    .testTag(TASK_FILTER_TYPE_TAG)
                    .fillMaxWidth()
            )

            OutlinedTextField(
                value = vmidText,
                onValueChange = { vmidText = it },
                label = { Text(stringResource(R.string.task_filter_vmid)) },
                placeholder = { Text(stringResource(R.string.task_filter_vmid_placeholder)) },
                isError = hasInvalidVmid,
                supportingText = {
                    if (hasInvalidVmid) {
                        Text(stringResource(R.string.task_filter_invalid_vmid))
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .testTag(TASK_FILTER_VMID_TAG)
                    .fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = {
                    selectedStatusName = TaskStatusFilter.All.name
                    typeText = ""
                    vmidText = ""
                    onClear()
                }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.task_filter_clear))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.task_filter_clear))
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        onApply(
                            TaskFilters(
                                status = selectedStatus,
                                typeFilter = typeText.trim().takeIf { it.isNotBlank() },
                                vmid = parsedVmid
                            )
                        )
                    },
                    enabled = !hasInvalidVmid
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.task_filter_apply))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.task_filter_apply))
                }
            }
        }
    }
}

private fun TaskFilters.toDraftKey(): String {
    return "${status.name}|${typeFilter.orEmpty()}|${vmid?.toString().orEmpty()}"
}

const val TASK_FILTER_STATUS_TAG = "task_filter_status"
const val TASK_FILTER_TYPE_TAG = "task_filter_type"
const val TASK_FILTER_VMID_TAG = "task_filter_vmid"

@Composable
private fun TaskCard(
    task: Task,
    actionInProgress: Boolean = false,
    onOpenDetails: () -> Unit,
    onAbort: () -> Unit
) {
    val statusLabel = task.displayStatusLabel()
    val statusColor = task.displayStatusColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetails),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = taskIcon(task.type),
                    contentDescription = null,
                    tint = statusColor
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.type.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.task_status_label, statusLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TaskStatusDot(statusColor)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TaskDetailRow(label = stringResource(R.string.task_node_label), value = task.node)
                TaskDetailRow(label = stringResource(R.string.task_user_label), value = task.user)
                TaskDetailRow(label = stringResource(R.string.task_resource_id_label), value = task.id)
                TaskDetailRow(label = stringResource(R.string.task_pid_label), value = task.pid.toString())
                TaskDetailRow(label = stringResource(R.string.task_started_label), value = formatTaskTimestamp(task.starttime))
                if (task.endtime != null && task.endtime > 0) {
                    TaskDetailRow(label = stringResource(R.string.task_ended_label), value = formatTaskTimestamp(task.endtime))
                }
                if (!task.exitstatus.isNullOrBlank()) {
                    TaskDetailRow(
                        label = stringResource(R.string.task_exit_status_label),
                        value = task.exitstatus,
                        valueColor = taskExitStatusColor(task.exitstatus)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onOpenDetails) {
                    Icon(Icons.Default.Info, contentDescription = stringResource(R.string.task_view_log))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.task_view_log))
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onAbort,
                    enabled = task.status.equals("running", ignoreCase = true) && !actionInProgress,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (actionInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.task_abort))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.task_abort))
                }
            }
        }
    }
}

@Composable
private fun TaskDetailSummaryCard(
    detail: TaskDetail,
    isAborting: Boolean,
    onAbort: () -> Unit
) {
    val task = detail.task
    val statusLabel = task.displayStatusLabel()
    val statusColor = task.displayStatusColor()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = taskIcon(task.type),
                    contentDescription = null,
                    tint = statusColor
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.type.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.task_status_label, statusLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TaskStatusDot(statusColor)
            }

            Spacer(modifier = Modifier.height(12.dp))
            TaskDetailRow(label = stringResource(R.string.task_node_label), value = detail.nodeName)
            TaskDetailRow(label = stringResource(R.string.task_upid_label), value = detail.upid)
            TaskDetailRow(label = stringResource(R.string.task_user_label), value = task.user)
            TaskDetailRow(label = stringResource(R.string.task_resource_id_label), value = task.id)
            TaskDetailRow(label = stringResource(R.string.task_pid_label), value = task.pid.toString())
            TaskDetailRow(label = stringResource(R.string.task_started_label), value = formatTaskTimestamp(task.starttime))
            if (task.endtime != null && task.endtime > 0) {
                TaskDetailRow(label = stringResource(R.string.task_ended_label), value = formatTaskTimestamp(task.endtime))
            }
            if (!task.exitstatus.isNullOrBlank()) {
                TaskDetailRow(
                    label = stringResource(R.string.task_exit_status_label),
                    value = task.exitstatus,
                    valueColor = taskExitStatusColor(task.exitstatus)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onAbort,
                    enabled = task.status.equals("running", ignoreCase = true) && !isAborting,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (isAborting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.task_abort))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.task_abort))
                }
            }
        }
    }
}

@Composable
private fun TaskDetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value.ifBlank { "-" },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            modifier = Modifier.weight(0.65f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TaskStatisticsCard(tasks: List<Task>) {
    val runningTasks = tasks.count { it.isRunningTask() }
    val finishedTasks = tasks.count { it.isFinishedTask() }
    val stoppedTasks = tasks.count { it.isStoppedTask() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.task_statistics),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TaskStatItem(
                    label = stringResource(R.string.task_stat_running),
                    value = runningTasks.toString(),
                    color = taskRunningColor()
                )
                TaskStatItem(
                    label = stringResource(R.string.task_stat_finished),
                    value = finishedTasks.toString(),
                    color = taskFinishedColor()
                )
                TaskStatItem(
                    label = stringResource(R.string.task_stat_stopped),
                    value = stoppedTasks.toString(),
                    color = taskStoppedColor()
                )
            }
        }
    }
}

@Composable
private fun TaskStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun TaskLogLine(entry: TaskLogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = String.format(Locale.US, "%04d", entry.lineNumber),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(52.dp)
            )
            SelectionContainer(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.text,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Visible
                )
            }
        }
    }
}

@Composable
private fun TaskErrorCard(message: String) {
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
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun TaskLoadingCard(text: String) {
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
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TaskEmptyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.task_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.task_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TaskEmptyLogCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = stringResource(R.string.task_log_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun TaskStatusDot(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(
                color = color,
                shape = RoundedCornerShape(6.dp)
            )
    )
}

private fun taskIcon(type: String): ImageVector {
    return when (type) {
        "qmstart", "vzstart" -> Icons.Default.PlayArrow
        "qmstop", "vzstop", "qmshutdown", "vzshutdown" -> Icons.Default.Stop
        "qmreboot", "vzreboot" -> Icons.Default.Refresh
        "qmclone", "vzclone" -> Icons.Default.ContentCopy
        "qmbackup", "vzdump", "lxcbackup" -> Icons.Default.Backup
        "qmrestore", "vzrestore" -> Icons.Default.Restore
        "qmdestroy", "vzdestroy", "qmdelete", "lxcdelete" -> Icons.Default.Delete
        else -> Icons.Default.Pending
    }
}

@Composable
private fun TaskStatusFilter.toDisplayLabel(): String {
    return when (this) {
        TaskStatusFilter.All -> stringResource(R.string.task_filter_status_all)
        TaskStatusFilter.Running -> stringResource(R.string.task_filter_status_running)
        TaskStatusFilter.Finished -> stringResource(R.string.task_filter_status_finished)
    }
}

@Composable
private fun taskStatusColor(status: String): Color {
    return when {
        status.equals("running", ignoreCase = true) -> taskRunningColor()
        status.equals("OK", ignoreCase = true) || status == "0" -> taskFinishedColor()
        status.equals("stopped", ignoreCase = true) -> taskStoppedColor()
        status.equals("finished", ignoreCase = true) -> taskFinishedColor()
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun taskExitStatusColor(exitStatus: String): Color {
    return if (exitStatus.equals("OK", ignoreCase = true) || exitStatus == "0") {
        taskFinishedColor()
    } else {
        taskStoppedColor()
    }
}

@Composable
private fun taskRunningColor(): Color = MaterialTheme.colorScheme.tertiary

@Composable
private fun taskFinishedColor(): Color = MaterialTheme.colorScheme.primary

@Composable
private fun taskStoppedColor(): Color = MaterialTheme.colorScheme.error

private fun Task.isRunningTask(): Boolean {
    return status.equals("running", ignoreCase = true)
}

private fun Task.isFinishedTask(): Boolean {
    return status.equals("finished", ignoreCase = true) ||
        status.equals("OK", ignoreCase = true) ||
        status == "0" ||
        exitstatus.equals("OK", ignoreCase = true) ||
        exitstatus == "0"
}

private fun Task.isStoppedTask(): Boolean {
    return status.equals("stopped", ignoreCase = true) && !isFinishedTask()
}

private fun Task.displayStatusLabel(): String {
    return when {
        isRunningTask() -> "RUNNING"
        isFinishedTask() -> "OK"
        isStoppedTask() -> "STOPPED"
        else -> status.uppercase(Locale.getDefault())
    }
}

@Composable
private fun Task.displayStatusColor(): Color {
    return when {
        isRunningTask() -> taskRunningColor()
        isFinishedTask() -> taskFinishedColor()
        isStoppedTask() -> taskStoppedColor()
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun formatTaskTimestamp(seconds: Long): String {
    if (seconds <= 0) return "-"
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    return dateFormat.format(Date(seconds * 1000))
}

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
