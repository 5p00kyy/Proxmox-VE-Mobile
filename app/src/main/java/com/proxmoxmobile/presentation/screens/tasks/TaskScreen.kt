package com.proxmoxmobile.presentation.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.proxmoxmobile.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.proxmoxmobile.data.model.Task
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedNode by remember { mutableStateOf<String?>(null) }
    var lastRefreshTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var actionInProgress by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val authFailedMsg = stringResource(R.string.task_error_auth_failed)
    val notAuthenticatedMsg = stringResource(R.string.task_error_not_authenticated)
    val noNodesMsg = stringResource(R.string.task_error_no_nodes)
    val unexpectedErrorMsg = stringResource(R.string.task_error_unexpected)
    val deleteSuccessMsg = stringResource(R.string.task_delete_success)
    val context = androidx.compose.ui.platform.LocalContext.current

    // Get cached nodes to determine which node to query
    val cachedNodes = viewModel.getCachedNodes()

    // Real-time data refresh
    LaunchedEffect(selectedNode) {
        while (selectedNode != null) {
            delay(10000) // Refresh every 10 seconds for tasks
            if (viewModel.isAuthenticated.value) {
                scope.launch {
                    refreshTasks(viewModel, selectedNode!!, { newTasks ->
                        tasks = newTasks
                        lastRefreshTime = System.currentTimeMillis()
                    }, { error ->
                        errorMessage = error
                    })
                }
            }
        }
    }

    // Load tasks function
    fun loadTasks() {
        if (selectedNode != null) {
            scope.launch {
                refreshTasks(viewModel, selectedNode!!, { newTasks ->
                    tasks = newTasks
                    lastRefreshTime = System.currentTimeMillis()
                    errorMessage = null
                }, { error ->
                    errorMessage = error
                })
            }
        }
    }

    // Fetch tasks when screen loads
    LaunchedEffect(Unit) {
        try {
            scope.launch {
                try {
                    isLoading = true
                    errorMessage = null
                    
                    Log.d("TaskScreen", "Starting to load tasks")
                    
                    // Check if we have a valid API service
                    val apiService = try {
                        viewModel.getApiService()
                    } catch (e: Exception) {
                        Log.e("TaskScreen", "Failed to get API service", e)
                        errorMessage = authFailedMsg
                        return@launch
                    }

                    if (apiService == null) {
                        Log.e("TaskScreen", "API service is null")
                        errorMessage = notAuthenticatedMsg
                        return@launch
                    }

                    // Use the first available node, or show error if no nodes
                    val nodes = cachedNodes ?: emptyList()
                    if (nodes.isEmpty()) {
                        errorMessage = noNodesMsg
                        return@launch
                    }
                    
                    selectedNode = nodes.first().node
                    Log.d("TaskScreen", "Using node: $selectedNode")
                    
                    // Load tasks for the selected node
                    loadTasks()
                    
                } catch (e: Exception) {
                    Log.e("TaskScreen", "Critical error in LaunchedEffect", e)
                    errorMessage = unexpectedErrorMsg
                } finally {
                    isLoading = false
                }
            }
        } catch (e: Exception) {
            Log.e("TaskScreen", "Critical error in LaunchedEffect", e)
            errorMessage = "An unexpected error occurred"
            isLoading = false
        }
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
                    IconButton(onClick = { /* TODO: Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.task_settings))
                    }
                    IconButton(onClick = {
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = stringResource(R.string.task_logout))
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
                        text = stringResource(R.string.task_last_updated, formatTimeAgo(lastRefreshTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                }
            }

            // Node selector
            if (cachedNodes != null && cachedNodes.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.task_selected_node, selectedNode ?: stringResource(R.string.task_none)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.task_monitoring_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Error message
            if (!errorMessage.isNullOrBlank()) {
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
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Loading indicator
            if (isLoading) {
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
                                text = stringResource(R.string.task_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Task statistics
            if (tasks.isNotEmpty()) {
                item {
                    TaskStatisticsCard(tasks)
                }
            }

            // Task list
            items(tasks) { task ->
                TaskCard(
                    task = task,
                    actionInProgress = actionInProgress == task.id,
                    onDelete = {
                        actionInProgress = task.id
                        viewModel.deleteTask(selectedNode!!, task.id,
                            onSuccess = {
                                actionInProgress = null
                                snackbarMessage = deleteSuccessMsg
                                scope.launch { snackbarHostState.showSnackbar(snackbarMessage!!) }
                                loadTasks() // Refresh the list
                            },
                            onError = { error ->
                                actionInProgress = null
                                snackbarMessage = context.getString(R.string.task_delete_failed, error)
                                scope.launch { snackbarHostState.showSnackbar(snackbarMessage!!) }
                            }
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    actionInProgress: Boolean = false,
    onDelete: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with task type and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (task.type) {
                        "qmstart" -> Icons.Default.PlayArrow
                        "qmstop" -> Icons.Default.Stop
                        "qmrestart" -> Icons.Default.Refresh
                        "qmclone" -> Icons.Default.ContentCopy
                        "qmbackup" -> Icons.Default.Backup
                        "qmrestore" -> Icons.Default.Restore
                        "qmdelete" -> Icons.Default.Delete
                        "lxcstart" -> Icons.Default.PlayArrow
                        "lxcstop" -> Icons.Default.Stop
                        "lxcclone" -> Icons.Default.ContentCopy
                        "lxcbackup" -> Icons.Default.Backup
                        "lxcdelete" -> Icons.Default.Delete
                        else -> Icons.Default.Pending
                    },
                    contentDescription = null,
                    tint = when (task.status) {
                        "running" -> Color.Green
                        "stopped" -> Color.Red
                        "finished" -> Color.Blue
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.type.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.task_status_label, task.status.uppercase()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = when (task.status) {
                                "running" -> Color.Green
                                "stopped" -> Color.Red
                                "finished" -> Color.Blue
                                else -> Color.Gray
                            },
                            shape = RoundedCornerShape(6.dp)
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Task details
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.task_node_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = task.node,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.task_user_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = task.user,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.task_pid_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = task.pid.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.task_started_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(Date(task.starttime * 1000)),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (task.endtime != null && task.endtime > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.task_ended_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormat.format(Date(task.endtime * 1000)),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                if (!task.exitstatus.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.task_exit_status_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = task.exitstatus,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (task.exitstatus == "0") Color.Green else Color.Red
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.task_saved_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (task.saved) stringResource(R.string.task_yes) else stringResource(R.string.task_no),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (task.saved) Color.Green else Color.Gray
                    )
                }
            }
            
            // Action buttons
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    enabled = !actionInProgress,
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
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.task_delete))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.task_delete))
                }
            }
        }
    }
}

@Composable
fun TaskStatisticsCard(tasks: List<Task>) {
    val runningTasks = tasks.count { it.status == "running" }
    val finishedTasks = tasks.count { it.status == "finished" }
    val stoppedTasks = tasks.count { it.status == "stopped" }
    
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
                    color = Color.Green
                )
                TaskStatItem(
                    label = stringResource(R.string.task_stat_finished),
                    value = finishedTasks.toString(),
                    color = Color.Blue
                )
                TaskStatItem(
                    label = stringResource(R.string.task_stat_stopped),
                    value = stoppedTasks.toString(),
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
fun TaskStatItem(
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

// Helper function to refresh tasks
suspend fun refreshTasks(
    viewModel: MainViewModel,
    nodeName: String,
    onSuccess: (List<Task>) -> Unit,
    onError: (String) -> Unit
) {
    try {
        Log.d("TaskScreen", "Refreshing tasks for node: $nodeName")
        val apiService = viewModel.getApiService()
        
        if (apiService == null) {
            onError("Not authenticated - please login again")
            return
        }
        
        val response = apiService.getTasks(nodeName, limit = 100)
        Log.d("TaskScreen", "Tasks response received: ${response.data?.size ?: 0} tasks")
        
        // Safely handle the response data
        val taskList = response.data ?: emptyList()
        Log.d("TaskScreen", "Processed ${taskList.size} tasks")
        
        // Validate each task before adding to the list
        val validTasks = taskList.filter { task ->
            try {
                task.id.isNotBlank() && 
                task.node.isNotBlank() &&
                task.type.isNotBlank()
            } catch (e: Exception) {
                Log.w("TaskScreen", "Invalid task data: ${e.message}")
                false
            }
        }
        
        onSuccess(validTasks)
        Log.d("TaskScreen", "Successfully refreshed ${validTasks.size} valid tasks")
        
    } catch (e: retrofit2.HttpException) {
        Log.e("TaskScreen", "HTTP error refreshing tasks: ${e.code()}", e)
        when (e.code()) {
            401 -> onError("Authentication required - please login again")
            403 -> onError("Access forbidden - check permissions")
            500 -> onError("Server error - please try again")
            else -> onError("Failed to refresh tasks: HTTP ${e.code()}")
        }
    } catch (e: Exception) {
        Log.e("TaskScreen", "Failed to refresh tasks", e)
        onError("Failed to refresh tasks: ${e.message}")
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