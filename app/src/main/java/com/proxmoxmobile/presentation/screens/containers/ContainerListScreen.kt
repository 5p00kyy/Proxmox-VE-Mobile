@file:OptIn(ExperimentalMaterial3Api::class)
package com.proxmoxmobile.presentation.screens.containers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.proxmoxmobile.data.model.Container
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import android.util.Log
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.text.style.TextAlign
import com.proxmoxmobile.presentation.navigation.Screen
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import com.proxmoxmobile.data.api.ProxmoxApiService
import androidx.compose.ui.res.stringResource
import com.proxmoxmobile.R


fun Double.format(digits: Int) = String.format(Locale.US, "%.${digits}f", this)

// Data class for resource metrics
data class ResourceMetric(
    val timestamp: Long,
    val cpu: Double,
    val ram: Long,
    val disk: Long,
    val networkIn: Long,
    val networkOut: Long
)

// Global resource metrics storage
object ResourceMetricsStorage {
    private val metrics = ConcurrentHashMap<Int, MutableList<ResourceMetric>>()
    
    fun addMetric(containerId: Int, metric: ResourceMetric) {
        val containerMetrics = metrics.getOrPut(containerId) { mutableListOf() }
        containerMetrics.add(metric)
        
        // Keep only last 5 minutes of data (300 seconds)
        val cutoffTime = System.currentTimeMillis() - (5 * 60 * 1000)
        containerMetrics.removeAll { it.timestamp < cutoffTime }
    }
    
    fun getAverageMetrics(containerId: Int): ResourceMetric? {
        val containerMetrics = metrics[containerId] ?: return null
        if (containerMetrics.isEmpty()) return null
        
        // Get metrics from last 5 minutes only
        val cutoffTime = System.currentTimeMillis() - (5 * 60 * 1000)
        val recentMetrics = containerMetrics.filter { it.timestamp >= cutoffTime }
        
        if (recentMetrics.isEmpty()) return null
        
        val avgCpu = recentMetrics.map { it.cpu }.average()
        val avgRam = recentMetrics.map { it.ram }.average().toLong()
        val avgDisk = recentMetrics.map { it.disk }.average().toLong()
        val avgNetworkIn = recentMetrics.map { it.networkIn }.average().toLong()
        val avgNetworkOut = recentMetrics.map { it.networkOut }.average().toLong()
        
        return ResourceMetric(
            timestamp = System.currentTimeMillis(),
            cpu = avgCpu,
            ram = avgRam,
            disk = avgDisk,
            networkIn = avgNetworkIn,
            networkOut = avgNetworkOut
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerListScreen(
    navController: NavController,
    viewModel: MainViewModel,
    nodeName: String? = null
) {
    var containers by remember { mutableStateOf<List<Container>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var lastRefreshTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var actionInProgress by remember { mutableStateOf<Pair<String, Int>?>(null) } // action, vmid
    val snackbarHostState = remember { SnackbarHostState() }
    
    val scope = rememberCoroutineScope()
    val apiService = viewModel.getApiService()
    val invalidNodeMsg = stringResource(R.string.container_invalid_node)
    val startSuccessTemplate = stringResource(R.string.container_start_success)
    val startErrorTemplate = stringResource(R.string.container_start_error)
    val stopSuccessTemplate = stringResource(R.string.container_stop_success)
    val stopErrorTemplate = stringResource(R.string.container_stop_error)
    val deleteSuccessTemplate = stringResource(R.string.container_delete_success)
    val deleteErrorTemplate = stringResource(R.string.container_delete_error)
    val deleteTitle = stringResource(R.string.container_delete_title)
    val deleteMessageTemplate = stringResource(R.string.container_delete_message)

    // Real-time data refresh
    LaunchedEffect(nodeName) {
        while (!nodeName.isNullOrBlank()) {
            delay(15000) // Refresh every 15 seconds for containers
            if (viewModel.isAuthenticated.value && apiService != null) {
                scope.launch {
                    refreshContainers(apiService, nodeName, { newContainers ->
                        containers = newContainers
                        lastRefreshTime = System.currentTimeMillis()
                    }, { error ->
                        errorMessage = error
                    })
                }
            }
        }
    }

    // Load containers function
    fun loadContainers() {
        if (apiService != null && !nodeName.isNullOrBlank()) {
            scope.launch {
                refreshContainers(apiService, nodeName, { newContainers ->
                    containers = newContainers
                    lastRefreshTime = System.currentTimeMillis()
                    errorMessage = null
                }, { error ->
                    errorMessage = error
                })
            }
        }
    }

    // Load containers when the screen is first displayed
    LaunchedEffect(apiService, nodeName) {
        if (apiService != null && !nodeName.isNullOrBlank()) {
            loadContainers()
        } else {
            errorMessage = invalidNodeMsg
        }
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
                    IconButton(onClick = { /* TODO: Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.container_settings))
                    }
                    IconButton(onClick = {
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = stringResource(R.string.container_logout))
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
                        text = stringResource(R.string.container_last_updated, formatTimeAgo(lastRefreshTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

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
                                text = stringResource(R.string.container_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Container list
            items(containers) { container ->
                ContainerCard(
                    container = container,
                    onStart = {
                        actionInProgress = "start" to container.vmid
                        viewModel.startContainer(nodeName!!, container.vmid,
                            onSuccess = {
                                actionInProgress = null
                                snackbarMessage = String.format(startSuccessTemplate, container.name)
                                scope.launch { snackbarHostState.showSnackbar(snackbarMessage!!) }
                                loadContainers() // Refresh the list
                            },
                            onError = { error ->
                                actionInProgress = null
                                snackbarMessage = String.format(startErrorTemplate, error)
                                scope.launch { snackbarHostState.showSnackbar(snackbarMessage!!) }
                            }
                        )
                    },
                    onStop = {
                        actionInProgress = "stop" to container.vmid
                        viewModel.stopContainer(nodeName!!, container.vmid,
                            onSuccess = {
                                actionInProgress = null
                                snackbarMessage = String.format(stopSuccessTemplate, container.name)
                                scope.launch { snackbarHostState.showSnackbar(snackbarMessage!!) }
                                loadContainers() // Refresh the list
                            },
                            onError = { error ->
                                actionInProgress = null
                                snackbarMessage = String.format(stopErrorTemplate, error)
                                scope.launch { snackbarHostState.showSnackbar(snackbarMessage!!) }
                            }
                        )
                    },
                    onDelete = {
                        viewModel.showConfirmationDialog(
                            MainViewModel.ConfirmationDialog(
                                title = deleteTitle,
                                message = String.format(deleteMessageTemplate, container.name, container.vmid),
                                onConfirm = {
                                    viewModel.hideConfirmationDialog()
                                    actionInProgress = "delete" to container.vmid
                                    viewModel.deleteContainer(nodeName!!, container.vmid,
                                        onSuccess = {
                                            actionInProgress = null
                                            snackbarMessage = String.format(deleteSuccessTemplate, container.name)
                                            scope.launch { snackbarHostState.showSnackbar(snackbarMessage!!) }
                                            loadContainers() // Refresh the list
                                        },
                                        onError = { error ->
                                            actionInProgress = null
                                            snackbarMessage = String.format(deleteErrorTemplate, error)
                                            scope.launch { snackbarHostState.showSnackbar(snackbarMessage!!) }
                                        }
                                    )
                                },
                                onDismiss = {
                                    viewModel.hideConfirmationDialog()
                                }
                            )
                        )
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
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var isActionInProgress by remember { mutableStateOf(false) }
    
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
            modifier = Modifier.padding(16.dp)
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
            
            Spacer(modifier = Modifier.height(12.dp))
            
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
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStart,
                    enabled = container.status != "running" && !isActionInProgress,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    if (isActionInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.container_start))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.container_start))
                }
                
                Button(
                    onClick = onStop,
                    enabled = container.status == "running" && !isActionInProgress,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    if (isActionInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.container_stop))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.container_stop))
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.container_delete))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.container_delete))
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

// Real-time metrics collection
private suspend fun startMetricsCollection(apiService: ProxmoxApiService, nodeName: String) {
    // Initial delay to avoid interfering with initial load
    delay(5000)
    
    while (true) {
        try {
            val response = apiService.getContainers(nodeName)
            val containerList = response.data ?: emptyList()
            
            for (container in containerList) {
                // Only collect metrics for running containers or if we have valid data
                if (container.status == "running" || container.cpu > 0 || container.mem > 0) {
                    val metric = ResourceMetric(
                        timestamp = System.currentTimeMillis(),
                        cpu = container.cpu,
                        ram = container.mem,
                        disk = container.disk,
                        networkIn = container.netin,
                        networkOut = container.netout
                    )
                    ResourceMetricsStorage.addMetric(container.vmid, metric)
                    Log.d("MetricsCollection", "Added metric for container ${container.vmid}: CPU=${container.cpu}, RAM=${container.mem}")
                }
            }
            
            // Collect metrics every 30 seconds (less aggressive)
            delay(30000)
        } catch (e: Exception) {
            Log.e("ContainerListScreen", "Failed to collect metrics", e)
            delay(60000) // Wait longer on error
        }
    }
}

// Add ContainerDetailScreen
@Composable
fun ContainerDetailScreen(
    vmid: Int,
    viewModel: MainViewModel,
    navController: NavController
) {
    var container by remember { mutableStateOf<Container?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var cpu by remember { mutableStateOf(0.0) }
    var ram by remember { mutableStateOf(0L) }
    var maxCpu by remember { mutableStateOf(0) }
    var maxRam by remember { mutableStateOf(0L) }
    var isUpdatingResources by remember { mutableStateOf(false) }
    var showCpuConfigDialog by remember { mutableStateOf(false) }
    var showRamConfigDialog by remember { mutableStateOf(false) }
    var tempCpuCores by remember { mutableStateOf(1) }
    var tempRamAllocation by remember { mutableStateOf(512L) }
    val scope = rememberCoroutineScope()
    val notAuthenticatedMsg = stringResource(R.string.container_not_authenticated)
    val containerNotFoundMsg = stringResource(R.string.container_not_found)
    val failedToLoadTemplate = stringResource(R.string.container_failed_to_load)
    val loadingTimeoutMsg = stringResource(R.string.container_loading_timeout)
    val failedToUpdateTemplate = stringResource(R.string.container_failed_to_update)
    val resourceApiNotImplMsg = stringResource(R.string.container_resource_api_not_implemented)
    val startNotImplMsg = stringResource(R.string.container_start_not_implemented)
    val stopNotImplMsg = stringResource(R.string.container_stop_not_implemented)
    val consoleNotImplMsg = stringResource(R.string.container_console_not_implemented)
    val cpuApiNotImplMsg = stringResource(R.string.container_cpu_api_not_implemented)
    val ramApiNotImplMsg = stringResource(R.string.container_ram_api_not_implemented)

    LaunchedEffect(vmid) {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null
                
                // Add timeout for loading
                withTimeout(15000) { // 15 second timeout
                    // Get the API service
                    val apiService = viewModel.getApiService()
                    if (apiService == null) {
                        errorMessage = notAuthenticatedMsg
                        isLoading = false
                        return@withTimeout
                    }

                    // Try to find container more efficiently
                    val nodes = viewModel.getCachedNodes() ?: emptyList()
                    var foundContainer: Container? = null
                    var foundNode: String? = null
                    
                    // Try the first node first (most common case)
                    if (nodes.isNotEmpty()) {
                        try {
                            val containers = apiService!!.getContainers(nodes[0].node).data ?: emptyList()
                            val container = containers.find { it.vmid == vmid }
                            if (container != null) {
                                foundContainer = container
                                foundNode = nodes[0].node
                            }
                        } catch (e: Exception) {
                            Log.w("ContainerDetailScreen", "Failed to load from first node: ${e.message}")
                        }
                    }
                    
                    // If not found in first node, search others
                    if (foundContainer == null) {
                        for (i in 1 until nodes.size) {
                                                    try {
                            val containers = apiService!!.getContainers(nodes[i].node).data ?: emptyList()
                            val container = containers.find { it.vmid == vmid }
                            if (container != null) {
                                foundContainer = container
                                foundNode = nodes[i].node
                                break
                            }
                        } catch (e: Exception) {
                            Log.w("ContainerDetailScreen", "Failed to load from node ${nodes[i].node}: ${e.message}")
                        }
                        }
                    }

                    if (foundContainer != null && foundNode != null) {
                        container = foundContainer
                        cpu = foundContainer.cpu
                        ram = foundContainer.mem
                        maxCpu = foundContainer.maxcpu
                        maxRam = foundContainer.maxmem
                        tempCpuCores = foundContainer.cpus
                        tempRamAllocation = foundContainer.maxmem
                        Log.d("ContainerDetailScreen", "Successfully loaded container ${foundContainer.name}")
                    } else {
                        errorMessage = containerNotFoundMsg
                        Log.e("ContainerDetailScreen", "Container $vmid not found in any node")
                    }
                }
            } catch (e: Exception) {
                errorMessage = String.format(failedToLoadTemplate, e.message ?: "")
                Log.e("ContainerDetailScreen", "Error loading container", e)
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                errorMessage = loadingTimeoutMsg
                Log.e("ContainerDetailScreen", "Loading timeout for container $vmid")
            } finally {
                isLoading = false
            }
        }
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
                    // Refresh button
                    IconButton(
                        onClick = {
                            scope.launch {
                                // Reload container data
                                try {
                                    isLoading = true
                                    errorMessage = null
                                    
                                    val apiService = viewModel.getApiService()
                                    if (apiService == null) {
                                        errorMessage = notAuthenticatedMsg
                                        isLoading = false
                                        return@launch
                                    }

                                    // Find the container by searching all nodes
                                    val nodes = viewModel.getCachedNodes() ?: emptyList()
                                    var foundContainer: Container? = null
                                    var foundNode: String? = null
                                    
                                    for (node in nodes) {
                                        try {
                                            val containers = apiService.getContainers(node.node).data ?: emptyList()
                                            val container = containers.find { it.vmid == vmid }
                                            if (container != null) {
                                                foundContainer = container
                                                foundNode = node.node
                                                break
                                            }
                                        } catch (e: Exception) {
                                            // Continue searching other nodes
                                        }
                                    }

                                    if (foundContainer != null && foundNode != null) {
                                        val containerData = foundContainer!!
                                        container = containerData
                                        cpu = containerData.cpu
                                        ram = containerData.mem
                                        maxCpu = containerData.maxcpu
                                        maxRam = containerData.maxmem
                                    } else {
                                        errorMessage = containerNotFoundMsg
                                    }
                                } catch (e: Exception) {
                                    errorMessage = String.format(failedToLoadTemplate, e.message ?: "")
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.container_refresh))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
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
                            Text(stringResource(R.string.container_name_label, container!!.name))
                            Text(stringResource(R.string.container_id_label, container!!.vmid))
                            Text(
                                text = stringResource(R.string.container_status_label, container!!.status),
                                color = when (container!!.status) {
                                    "running" -> Color.Green
                                    "stopped" -> Color.Red
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(stringResource(R.string.container_uptime_hours, (container!!.uptime / 3600).toInt()))
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
                            Text(stringResource(R.string.container_cpu_cores_count, container!!.cpus))
                            Text(stringResource(R.string.container_ram_usage, formatBytes(ram)))
                            Text(stringResource(R.string.container_ram_allocated, formatBytes(maxRam)))
                        }
                    }
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
                                currentValue = stringResource(R.string.container_cores_allocated, container!!.cpus),
                                icon = Icons.Filled.Memory,
                                onClick = {
                                    showCpuConfigDialog = true
                                }
                            )

                            // RAM Configuration Card
                            ResourceCard(
                                title = stringResource(R.string.container_ram_allocation),
                                currentValue = stringResource(R.string.container_bytes_allocated, formatBytes(maxRam)),
                                icon = Icons.Filled.Storage,
                                onClick = {
                                    showRamConfigDialog = true
                                }
                            )
                            
                            // Apply Changes Button
                            Button(
                                onClick = {
                                    scope.launch {
                                        isUpdatingResources = true
                                        try {
                                            // TODO: Implement API call to update container resources
                                            // This would require additional API endpoints for resource management
                                            errorMessage = resourceApiNotImplMsg
                                        } catch (e: Exception) {
                                            errorMessage = String.format(failedToUpdateTemplate, e.message ?: "")
                                        } finally {
                                            isUpdatingResources = false
                                        }
                                    }
                                },
                                enabled = !isUpdatingResources,
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
                                    onClick = {
                                        scope.launch {
                                            // TODO: Implement start container action
                                            errorMessage = startNotImplMsg
                                        }
                                    },
                                    enabled = container!!.status != "running",
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.container_start))
                                }
                                
                                Button(
                                    onClick = {
                                        scope.launch {
                                            // TODO: Implement stop container action
                                            errorMessage = stopNotImplMsg
                                        }
                                    },
                                    enabled = container!!.status == "running",
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Stop, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.container_stop))
                                }
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    // TODO: Implement console access
                                    errorMessage = consoleNotImplMsg
                                },
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
                                text = errorMessage!!,
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
                        text = errorMessage!!,
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
                        // TODO: Implement API call to update CPU cores
                        errorMessage = cpuApiNotImplMsg
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
                        // TODO: Implement API call to update RAM allocation
                        errorMessage = ramApiNotImplMsg
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

// Helper function to format bytes
fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> "${(bytes.toDouble() / 1024 / 1024 / 1024).format(1)}GB"
        bytes >= 1024 * 1024 -> "${(bytes.toDouble() / 1024 / 1024).format(1)}MB"
        bytes >= 1024 -> "${(bytes.toDouble() / 1024).format(1)}KB"
        else -> "${bytes}B"
    }
}

// Helper function to refresh containers
suspend fun refreshContainers(
    apiService: ProxmoxApiService,
    nodeName: String,
    onSuccess: (List<Container>) -> Unit,
    onError: (String) -> Unit
) {
    try {
        Log.d("ContainerListScreen", "Refreshing containers for node: $nodeName")
        val response = apiService.getContainers(nodeName)
        Log.d("ContainerListScreen", "API response received: ${response.data?.size ?: 0} containers")
        
        // Safely handle the response data
        val containerList = response.data ?: emptyList()
        Log.d("ContainerListScreen", "Processed ${containerList.size} containers")
        
        // Validate each container before adding to the list
        val validContainers = containerList.filter { container ->
            try {
                container.vmid > 0 && 
                container.name.isNotBlank() && 
                container.status.isNotBlank() &&
                container.cpu >= 0 &&
                container.mem >= 0 &&
                container.maxmem >= 0 &&
                container.uptime >= 0
            } catch (e: Exception) {
                Log.w("ContainerListScreen", "Invalid container data: ${e.message}")
                false
            }
        }
        
        // Sort: by VMID ascending
        val sortedContainers = validContainers.sortedBy { it.vmid }
        onSuccess(sortedContainers)
        Log.d("ContainerListScreen", "Successfully refreshed ${sortedContainers.size} valid containers")
        
    } catch (e: retrofit2.HttpException) {
        Log.e("ContainerListScreen", "HTTP error refreshing containers: ${e.code()}", e)
        when (e.code()) {
            401 -> onError("Authentication required - please login again")
            403 -> onError("Access forbidden - check permissions")
            404 -> onError("Node not found: $nodeName")
            500 -> onError("Server error - please try again")
            else -> onError("Failed to refresh containers: HTTP ${e.code()}")
        }
    } catch (e: Exception) {
        Log.e("ContainerListScreen", "Failed to refresh containers", e)
        onError("Failed to refresh containers: ${e.message}")
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Edit indicator
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.container_edit),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
} 