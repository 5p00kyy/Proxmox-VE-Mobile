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
import androidx.compose.ui.res.stringResource
import com.proxmoxmobile.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.proxmoxmobile.data.model.Node
import com.proxmoxmobile.presentation.navigation.Screen
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import android.util.Log
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign
import java.util.Locale
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

fun Double.format(digits: Int) = String.format(Locale.US, "%.${digits}f", this)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    var nodes by remember { mutableStateOf<List<Node>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastRefreshTime by remember { mutableStateOf(System.currentTimeMillis()) }
    val scope = rememberCoroutineScope()

            // Real-time data refresh
        LaunchedEffect(Unit) {
            while (true) {
                delay(30000) // Refresh every 30 seconds
                if (viewModel.isAuthenticated.value) {
                    scope.launch {
                        refreshNodes(viewModel, { newNodes ->
                            nodes = newNodes
                            lastRefreshTime = System.currentTimeMillis()
                        }, { error ->
                            errorMessage = error
                        })
                    }
                }
            }
        }

    // Initial load and manual refresh function
    fun loadNodes() {
        scope.launch {
            refreshNodes(viewModel, { newNodes ->
                nodes = newNodes
                lastRefreshTime = System.currentTimeMillis()
                errorMessage = null
            }, { error ->
                errorMessage = error
            })
        }
    }

    // Fetch nodes when screen loads
    LaunchedEffect(Unit) {
        try {
            val cached = viewModel.getCachedNodes()
            if (cached != null && cached.isNotEmpty()) {
                nodes = cached
                lastRefreshTime = System.currentTimeMillis()
                isLoading = false
                errorMessage = null
                Log.d("DashboardScreen", "Loaded nodes from cache: ${cached.size}")
            } else {
                loadNodes()
            }
        } catch (e: Exception) {
            Log.e("DashboardScreen", "Critical error in LaunchedEffect", e)
            errorMessage = "An unexpected error occurred"
            isLoading = false
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
                    IconButton(onClick = { /* TODO: Settings */ }) {
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
                        text = stringResource(R.string.dashboard_last_updated, formatTimeAgo(lastRefreshTime)),
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
            errorMessage?.let { error ->
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
            if (isLoading) {
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
                                navController.navigate("${Screen.VMList.route}/${node.node}")
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
                                navController.navigate("${Screen.ContainerList.route}/${nodes.first().node}")
                            } else {
                                // Fallback navigation
                                navController.navigate(Screen.ContainerList.route)
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
                                navController.navigate("${Screen.VMList.route}/${nodes.first().node}")
                            } else {
                                // Fallback navigation
                                navController.navigate(Screen.VMList.route)
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
                                navController.navigate("${Screen.Storage.route}/${nodes.first().node}")
                            } else {
                                // Fallback navigation
                                navController.navigate(Screen.Storage.route)
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
        }
    }
}

private fun isValidNode(node: Node): Boolean {
    return node.node.isNotBlank() && 
           node.cpu >= 0 && 
           node.mem >= 0 && 
           node.uptime >= 0
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

// Helper function to refresh nodes
suspend fun refreshNodes(
    viewModel: MainViewModel,
    onSuccess: (List<Node>) -> Unit,
    onError: (String) -> Unit
) {
    try {
        Log.d("DashboardScreen", "Starting to refresh nodes")
        
        // Check if we have a valid API service
        val apiService = try {
            viewModel.getApiService()
        } catch (e: Exception) {
            Log.e("DashboardScreen", "Failed to get API service", e)
            onError("Authentication failed - please login again")
            return
        }
        
        if (apiService == null) {
            Log.e("DashboardScreen", "API service is null")
            onError("Not authenticated - please login again")
            return
        }
        
        Log.d("DashboardScreen", "API service available, fetching nodes")
        
        // Wrap the API call in additional error handling
        val response = try {
            apiService.getNodes()
        } catch (e: Exception) {
            Log.e("DashboardScreen", "API call failed", e)
            onError("Failed to connect to server: ${e.message}")
            return
        }
        
        Log.d("DashboardScreen", "Nodes response received: ${response.data?.size ?: 0} nodes")
        
        // Safely handle the response data
        val nodeList = response.data ?: emptyList()
        Log.d("DashboardScreen", "Processed ${nodeList.size} nodes")
        
        // Validate each node before adding to the list
        val validNodes = nodeList.filter { node ->
            try {
                node.node.isNotBlank() && 
                node.status.isNotBlank() &&
                node.cpu >= 0 &&
                node.mem >= 0 &&
                node.uptime >= 0
            } catch (e: Exception) {
                Log.w("DashboardScreen", "Invalid node data: ${e.message}")
                false
            }
        }
        
        viewModel.setCachedNodes(validNodes)
        onSuccess(validNodes)
        Log.d("DashboardScreen", "Successfully refreshed ${validNodes.size} valid nodes")
        
    } catch (e: retrofit2.HttpException) {
        Log.e("DashboardScreen", "HTTP error refreshing nodes: ${e.code()}", e)
        when (e.code()) {
            401 -> onError("Authentication required - please login again")
            403 -> onError("Access forbidden - check permissions")
            500 -> onError("Server error - please try again")
            else -> onError("Failed to refresh nodes: HTTP ${e.code()}")
        }
    } catch (e: Exception) {
        Log.e("DashboardScreen", "Failed to refresh nodes", e)
        onError("Failed to refresh nodes: ${e.message}")
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