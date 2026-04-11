package com.proxmoxmobile.presentation.screens.vms

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.proxmoxmobile.data.model.VirtualMachine
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import android.util.Log
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
    val stopSuccessTemplate = stringResource(R.string.vm_stop_success)
    val stopFailedTemplate = stringResource(R.string.vm_stop_failed)
    val deleteSuccessTemplate = stringResource(R.string.vm_delete_success)
    val deleteFailedTemplate = stringResource(R.string.vm_delete_failed)
    val deleteDialogTitle = stringResource(R.string.vm_delete_dialog_title)
    val deleteDialogMessageTemplate = stringResource(R.string.vm_delete_dialog_message)
    var vms by remember { mutableStateOf<List<VirtualMachine>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }
    var lastRefreshTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var actionInProgress by remember { mutableStateOf<Pair<String, Int>?>(null) } // action, vmid
    val snackbarHostState = remember { SnackbarHostState() }
    
    val scope = rememberCoroutineScope()
    val apiService = viewModel.getApiService()

    // Real-time data refresh
    LaunchedEffect(nodeName) {
        while (!nodeName.isNullOrBlank()) {
            delay(15000) // Refresh every 15 seconds for VMs
            if (viewModel.isAuthenticated.value && apiService != null) {
                scope.launch {
                    refreshVMs(apiService, nodeName, { newVMs ->
                        vms = newVMs
                        lastRefreshTime = System.currentTimeMillis()
                    }, { error ->
                        errorMessage = error
                    })
                }
            }
        }
    }

    // Load VMs function
    fun loadVMs() {
        if (apiService != null && !nodeName.isNullOrBlank()) {
            scope.launch {
                refreshVMs(apiService, nodeName, { newVMs ->
                    vms = newVMs
                    lastRefreshTime = System.currentTimeMillis()
                    errorMessage = null
                }, { error ->
                    errorMessage = error
                })
            }
        }
    }

    // Load VMs when the screen is first displayed
    LaunchedEffect(apiService, nodeName) {
        if (apiService != null && !nodeName.isNullOrBlank()) {
            loadVMs()
        } else {
            errorMessage = "Invalid node name or API service not available"
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
                    IconButton(onClick = { /* TODO: Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.vm_settings))
                    }
                    IconButton(onClick = {
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = stringResource(R.string.vm_logout))
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
                        text = stringResource(R.string.vm_last_updated, formatTimeAgo(lastRefreshTime)),
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
                                text = stringResource(R.string.vm_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // VM list
            items(vms) { vm ->
                VMCard(
                    vm = vm,
                    actionInProgress = actionInProgress?.second == vm.vmid,
                    onStart = {
                        actionInProgress = "start" to vm.vmid
                        viewModel.startVM(nodeName!!, vm.vmid,
                            onSuccess = {
                                actionInProgress = null
                                snackbarMessage = String.format(startSuccessTemplate, vm.name)
                                scope.launch { snackbarHostState.showSnackbar(snackbarMessage!!) }
                                loadVMs() // Refresh the list
                            },
                            onError = { error ->
                                actionInProgress = null
                                snackbarMessage = String.format(startFailedTemplate, error)
                                scope.launch { snackbarHostState.showSnackbar(snackbarMessage!!) }
                            }
                        )
                    },
                    onStop = {
                        actionInProgress = "stop" to vm.vmid
                        viewModel.stopVM(nodeName!!, vm.vmid,
                            onSuccess = {
                                actionInProgress = null
                                snackbarMessage = String.format(stopSuccessTemplate, vm.name)
                                scope.launch { snackbarHostState.showSnackbar(snackbarMessage!!) }
                                loadVMs() // Refresh the list
                            },
                            onError = { error ->
                                actionInProgress = null
                                snackbarMessage = String.format(stopFailedTemplate, error)
                                scope.launch { snackbarHostState.showSnackbar(snackbarMessage!!) }
                            }
                        )
                    },
                    onDelete = {
                        viewModel.showConfirmationDialog(
                            MainViewModel.ConfirmationDialog(
                                title = deleteDialogTitle,
                                message = String.format(deleteDialogMessageTemplate, vm.name, vm.vmid),
                                onConfirm = {
                                    viewModel.hideConfirmationDialog()
                                    actionInProgress = "delete" to vm.vmid
                                    viewModel.deleteVM(nodeName!!, vm.vmid,
                                        onSuccess = {
                                            actionInProgress = null
                                            snackbarMessage = String.format(deleteSuccessTemplate, vm.name)
                                            scope.launch { snackbarHostState.showSnackbar(snackbarMessage!!) }
                                            loadVMs() // Refresh the list
                                        },
                                        onError = { error ->
                                            actionInProgress = null
                                            snackbarMessage = String.format(deleteFailedTemplate, error)
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
fun VMCard(
    vm: VirtualMachine,
    actionInProgress: Boolean = false,
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onDelete: () -> Unit = {}
) {

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
                    enabled = vm.status != "running" && !actionInProgress,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    if (actionInProgress) {
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
                    onClick = onStop,
                    enabled = vm.status == "running" && !actionInProgress,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    if (actionInProgress) {
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
                
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.vm_delete))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.vm_delete))
                }
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

// Helper function to refresh VMs
suspend fun refreshVMs(
    apiService: com.proxmoxmobile.data.api.ProxmoxApiService,
    nodeName: String,
    onSuccess: (List<VirtualMachine>) -> Unit,
    onError: (String) -> Unit
) {
    try {
        Log.d("VMListScreen", "Refreshing VMs for node: $nodeName")
        val response = apiService.getVirtualMachines(nodeName)
        Log.d("VMListScreen", "API response received: ${response.data?.size ?: 0} VMs")
        
        // Safely handle the response data
        val vmList = response.data ?: emptyList()
        Log.d("VMListScreen", "Processed ${vmList.size} VMs")
        
        // Validate each VM before adding to the list
        val validVMs = vmList.filter { vm ->
            try {
                vm.vmid > 0 && 
                vm.name.isNotBlank() && 
                vm.status.isNotBlank() &&
                vm.cpu >= 0 &&
                vm.mem >= 0 &&
                vm.maxmem >= 0 &&
                vm.uptime >= 0
            } catch (e: Exception) {
                Log.w("VMListScreen", "Invalid VM data: ${e.message}")
                false
            }
        }
        
        onSuccess(validVMs)
        Log.d("VMListScreen", "Successfully refreshed ${validVMs.size} valid VMs")
        
    } catch (e: retrofit2.HttpException) {
        Log.e("VMListScreen", "HTTP error refreshing VMs: ${e.code()}", e)
        when (e.code()) {
            401 -> onError("Authentication required - please login again")
            403 -> onError("Access forbidden - check permissions")
            404 -> onError("Node not found: $nodeName")
            500 -> onError("Server error - please try again")
            else -> onError("Failed to refresh VMs: HTTP ${e.code()}")
        }
    } catch (e: Exception) {
        Log.e("VMListScreen", "Failed to refresh VMs", e)
        onError("Failed to refresh VMs: ${e.message}")
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