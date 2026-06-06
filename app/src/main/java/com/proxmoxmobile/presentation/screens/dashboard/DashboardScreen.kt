package com.proxmoxmobile.presentation.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import com.proxmoxmobile.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.navigation.NavController
import com.proxmoxmobile.data.dashboard.DashboardRepository
import com.proxmoxmobile.data.dashboard.ProxmoxDashboardApi
import com.proxmoxmobile.data.dashboard.ProxmoxDashboardTaskSummarySource
import com.proxmoxmobile.data.model.Node
import com.proxmoxmobile.data.task.ProxmoxTaskApi
import com.proxmoxmobile.data.task.TaskRepository
import com.proxmoxmobile.data.task.TaskSummary
import com.proxmoxmobile.presentation.navigation.Screen
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import android.util.Log
import androidx.compose.ui.text.style.TextAlign
import java.util.Locale
import kotlinx.coroutines.delay

fun Double.format(digits: Int) = String.format(Locale.US, "%.${digits}f", this)

const val DASHBOARD_RECENT_TASKS_METRIC_TAG = "dashboard_recent_tasks_metric"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: MainViewModel,
    repositoryOverride: DashboardRepository? = null
) {
    val initialCachedNodes = remember(viewModel) {
        viewModel.getCachedNodes().orEmpty()
    }
    val dashboardRepository = remember(viewModel) {
        DashboardRepository(
            api = ProxmoxDashboardApi { viewModel.getApiService() },
            taskSummarySource = ProxmoxDashboardTaskSummarySource(
                TaskRepository(ProxmoxTaskApi { viewModel.getApiService() })
            )
        )
    }
    val activeDashboardRepository = repositoryOverride ?: dashboardRepository
    val dashboardViewModel: DashboardViewModel = composeViewModel(
        key = "dashboard",
        factory = remember(initialCachedNodes, activeDashboardRepository, viewModel) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                        return DashboardViewModel(
                            initialCachedNodes = initialCachedNodes,
                            repository = activeDashboardRepository,
                            cacheNodes = viewModel::setCachedNodes
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    )
    val uiState by dashboardViewModel.uiState.collectAsState()
    val nodes = uiState.nodes

    // Real-time data refresh
    LaunchedEffect(dashboardViewModel) {
        dashboardViewModel.loadDashboard()
        while (true) {
            delay(30000) // Refresh every 30 seconds
            if (viewModel.isAuthenticated.value) {
                dashboardViewModel.loadDashboard(showLoading = false)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.dashboard_app_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.dashboard_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { dashboardViewModel.loadDashboard(showLoading = false) },
                        enabled = !uiState.isLoading && !uiState.isRefreshing
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.dashboard_refresh))
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.dashboard_settings))
                    }
                    IconButton(onClick = {
                        viewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.dashboard_logout))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Last refresh indicator
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_last_updated, formatTimeAgo(uiState.lastRefreshTimeMillis)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // System Status Card (only show if we have nodes)
            if (nodes.isNotEmpty()) {
                item {
                    SystemStatusCard(nodes.first())
                }
                item {
                    TaskActivityCard(
                        summary = uiState.taskSummary,
                        isLoading = uiState.isTaskSummaryLoading,
                        errorMessage = uiState.taskSummaryError,
                        onOpenTasks = {
                            navController.navigate(Screen.Tasks.route)
                        }
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 3.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.dashboard_ready),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.dashboard_data_loading_disabled),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Welcome Section
            item {
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
                            text = stringResource(R.string.dashboard_welcome_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.dashboard_welcome_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Error message
            uiState.errorMessage?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Loading state
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Nodes Section
            if (nodes.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.dashboard_nodes_count, nodes.size),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(nodes) { node ->
                    NodeCard(
                        node = node,
                        onClick = {
                            try {
                                navController.navigate(Screen.NodeDetail.createRoute(node.node))
                            } catch (e: Exception) {
                                Log.e("DashboardScreen", "Navigation error", e)
                            }
                        }
                    )
                }
            }

            // Quick Actions
            item {
                Text(
                    text = stringResource(R.string.dashboard_quick_actions),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // LXC Containers
                    QuickActionCard(
                        title = stringResource(R.string.dashboard_lxc),
                        subtitle = stringResource(R.string.dashboard_containers),
                        icon = Icons.Default.Storage,
                        modifier = Modifier.weight(1f)
                    ) {
                        try {
                            if (nodes.isNotEmpty()) {
                                navController.navigate(Screen.ContainerList.createRoute(nodes.first().node))
                            }
                        } catch (e: Exception) {
                            Log.e("DashboardScreen", "Navigation error to LXC", e)
                        }
                    }

                    // Virtual Machines
                    QuickActionCard(
                        title = stringResource(R.string.dashboard_vm),
                        subtitle = stringResource(R.string.dashboard_machines),
                        icon = Icons.Default.Computer,
                        modifier = Modifier.weight(1f)
                    ) {
                        try {
                            if (nodes.isNotEmpty()) {
                                navController.navigate(Screen.VMList.createRoute(nodes.first().node))
                            }
                        } catch (e: Exception) {
                            Log.e("DashboardScreen", "Navigation error to VM", e)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Storage
                    QuickActionCard(
                        title = stringResource(R.string.dashboard_storage),
                        subtitle = stringResource(R.string.dashboard_pools),
                        icon = Icons.Default.Folder,
                        modifier = Modifier.weight(1f)
                    ) {
                        try {
                            if (nodes.isNotEmpty()) {
                                navController.navigate(Screen.Storage.createRoute(nodes.first().node))
                            }
                        } catch (e: Exception) {
                            Log.e("DashboardScreen", "Navigation error to Storage", e)
                        }
                    }

                    // Network
                    QuickActionCard(
                        title = stringResource(R.string.dashboard_network),
                        subtitle = stringResource(R.string.dashboard_interfaces),
                        icon = Icons.Default.Wifi,
                        modifier = Modifier.weight(1f)
                    ) {
                        try {
                            navController.navigate(Screen.Network.route)
                        } catch (e: Exception) {
                            Log.e("DashboardScreen", "Navigation error to Network", e)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Users
                    QuickActionCard(
                        title = stringResource(R.string.dashboard_users),
                        subtitle = stringResource(R.string.dashboard_management),
                        icon = Icons.Default.People,
                        modifier = Modifier.weight(1f)
                    ) {
                        try {
                            navController.navigate(Screen.Users.route)
                        } catch (e: Exception) {
                            Log.e("DashboardScreen", "Navigation error to Users", e)
                        }
                    }

                    // Tasks
                    QuickActionCard(
                        title = stringResource(R.string.dashboard_tasks),
                        subtitle = stringResource(R.string.dashboard_monitoring),
                        icon = Icons.AutoMirrored.Filled.List,
                        modifier = Modifier.weight(1f)
                    ) {
                        try {
                            navController.navigate(Screen.Tasks.route)
                        } catch (e: Exception) {
                            Log.e("DashboardScreen", "Navigation error to Tasks", e)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Backups
                    QuickActionCard(
                        title = stringResource(R.string.dashboard_backups),
                        subtitle = stringResource(R.string.dashboard_backup_history),
                        icon = Icons.Default.Cloud,
                        modifier = Modifier.weight(1f)
                    ) {
                        try {
                            navController.navigate(Screen.Backups.route)
                        } catch (e: Exception) {
                            Log.e("DashboardScreen", "Navigation error to Backups", e)
                        }
                    }

                    // Cluster
                    QuickActionCard(
                        title = stringResource(R.string.dashboard_cluster),
                        subtitle = stringResource(R.string.dashboard_cluster_status),
                        icon = Icons.Default.Share,
                        modifier = Modifier.weight(1f)
                    ) {
                        try {
                            navController.navigate(Screen.Cluster.route)
                        } catch (e: Exception) {
                            Log.e("DashboardScreen", "Navigation error to Cluster", e)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemStatusCard(node: Node) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MonitorHeart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.dashboard_system_status),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                // Use weight for better scaling
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusItem(
                    label = stringResource(R.string.dashboard_cpu),
                    value = "${String.format("%.1f", node.cpu * 100)}%",
                    icon = Icons.Default.Memory,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatusItem(
                    label = stringResource(R.string.dashboard_memory),
                    value = if (node.mem >= 1024 * 1024 * 1024) "${String.format("%.1f", node.mem.toDouble() / 1024 / 1024 / 1024)}GB" else "${String.format("%.1f", node.mem.toDouble() / 1024 / 1024)}MB",
                    icon = Icons.Default.Storage,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                StatusItem(
                    label = stringResource(R.string.dashboard_uptime),
                    value = "${(node.uptime / 3600).toInt()}h",
                    icon = Icons.Default.Schedule,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskActivityCard(
    summary: TaskSummary?,
    isLoading: Boolean,
    errorMessage: String?,
    onOpenTasks: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenTasks,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.dashboard_task_activity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.dashboard_open_tasks),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.dashboard_task_activity_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                errorMessage != null -> {
                    Text(
                        text = stringResource(R.string.dashboard_task_activity_error, errorMessage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                summary != null -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TaskActivityMetric(
                            label = stringResource(R.string.dashboard_running_tasks),
                            value = summary.runningCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        TaskActivityMetric(
                            label = stringResource(R.string.dashboard_recent_tasks),
                            value = summary.recentCount.toString(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag(DASHBOARD_RECENT_TASKS_METRIC_TAG)
                                .semantics(mergeDescendants = true) {}
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val latestTask = summary.latestTask
                    Text(
                        text = stringResource(R.string.dashboard_latest_task),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                    )
                    Text(
                        text = if (latestTask == null) {
                            stringResource(R.string.dashboard_no_recent_tasks)
                        } else {
                            stringResource(
                                R.string.dashboard_latest_task_value,
                                latestTask.type.uppercase(Locale.getDefault()),
                                latestTask.node,
                                latestTask.status
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun TaskActivityMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
        )
    }
}

@Composable
fun StatusItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeCard(
    node: Node,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.node,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.dashboard_status, node.status),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
