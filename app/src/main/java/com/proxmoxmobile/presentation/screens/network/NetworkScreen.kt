package com.proxmoxmobile.presentation.screens.network

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.proxmoxmobile.data.model.NetworkInterface
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import android.util.Log
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.res.stringResource
import com.proxmoxmobile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    var networkInterfaces by remember { mutableStateOf<List<NetworkInterface>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedNode by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Get cached nodes to determine which node to query
    val cachedNodes = viewModel.getCachedNodes()

    val errAuthFailed = stringResource(R.string.network_error_auth_failed)
    val errNotAuthenticated = stringResource(R.string.network_error_not_authenticated)
    val errNoNodes = stringResource(R.string.network_error_no_nodes)
    val errConnectPrefix = stringResource(R.string.network_error_connect_prefix)
    val errAuthRequired = stringResource(R.string.network_error_auth_required)
    val errAccessForbidden = stringResource(R.string.network_error_access_forbidden)
    val errServerError = stringResource(R.string.network_error_server)
    val errHttpPrefix = stringResource(R.string.network_error_http_prefix)
    val errLoadFailedPrefix = stringResource(R.string.network_error_load_failed_prefix)
    val errUnexpected = stringResource(R.string.network_error_unexpected)

    // Fetch network interfaces when screen loads
    LaunchedEffect(Unit) {
        try {
            scope.launch {
                try {
                    isLoading = true
                    errorMessage = null
                    
                    Log.d("NetworkScreen", "Starting to load network interfaces")
                    
                    // Check if we have a valid API service
                    val apiService = try {
                        viewModel.getApiService()
                    } catch (e: Exception) {
                        Log.e("NetworkScreen", "Failed to get API service", e)
                        errorMessage = errAuthFailed
                        return@launch
                    }

                    if (apiService == null) {
                        Log.e("NetworkScreen", "API service is null")
                        errorMessage = errNotAuthenticated
                        return@launch
                    }

                    // Use the first available node, or show error if no nodes
                    val nodes = cachedNodes ?: emptyList()
                    if (nodes.isEmpty()) {
                        errorMessage = errNoNodes
                        return@launch
                    }
                    
                    selectedNode = nodes.first().node
                    Log.d("NetworkScreen", "Using node: $selectedNode")
                    
                    // Fetch network interfaces
                    val response = try {
                        apiService.getNetworkInterfaces(selectedNode!!)
                    } catch (e: Exception) {
                        Log.e("NetworkScreen", "API call failed", e)
                        errorMessage = "$errConnectPrefix ${e.message}"
                        return@launch
                    }
                    
                    Log.d("NetworkScreen", "Network interfaces response received: ${response.data?.size ?: 0} interfaces")
                    
                    // Safely handle the response data
                    val interfaceList = response.data ?: emptyList()
                    Log.d("NetworkScreen", "Processed ${interfaceList.size} network interfaces")
                    
                    // Validate each interface before adding to the list
                    val validInterfaces = interfaceList.filter { iface ->
                        try {
                            iface.iface.isNotBlank() && 
                            iface.type.isNotBlank()
                        } catch (e: Exception) {
                            Log.w("NetworkScreen", "Invalid interface data: ${e.message}")
                            false
                        }
                    }
                    networkInterfaces = validInterfaces
                    Log.d("NetworkScreen", "Successfully loaded ${networkInterfaces.size} valid network interfaces")
                } catch (e: retrofit2.HttpException) {
                    Log.e("NetworkScreen", "HTTP error loading network interfaces: ${e.code()}", e)
                    when (e.code()) {
                        401 -> errorMessage = errAuthRequired
                        403 -> errorMessage = errAccessForbidden
                        500 -> errorMessage = errServerError
                        else -> errorMessage = "$errHttpPrefix ${e.code()}"
                    }
                } catch (e: Exception) {
                    Log.e("NetworkScreen", "Failed to load network interfaces", e)
                    errorMessage = "$errLoadFailedPrefix ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkScreen", "Critical error in LaunchedEffect", e)
            errorMessage = errUnexpected
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.network_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.network_back))
                    }
                },
                actions = {
                    if (selectedNode != null) {
                        Text(
                            text = stringResource(R.string.network_node_label, selectedNode ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.network_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.network_error_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                // Retry loading
                                scope.launch {
                                    // Reset state and retry
                                    networkInterfaces = emptyList()
                                    errorMessage = null
                                    isLoading = true
                                    // This will trigger LaunchedEffect again
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.network_retry))
                        }
                    }
                }
            }
            networkInterfaces.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.network_empty_title),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.network_empty_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.network_interfaces_count, networkInterfaces.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    items(networkInterfaces) { iface ->
                        NetworkInterfaceCard(iface = iface)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkInterfaceCard(iface: NetworkInterface) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row with interface name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = iface.iface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = iface.type,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (iface.active) Color.Green else Color.Red,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    Text(
                        text = if (iface.active) stringResource(R.string.network_status_active) else stringResource(R.string.network_status_inactive),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (iface.active) Color.Green else Color.Red
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Configuration details
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (iface.method.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.network_field_method),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = iface.method,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                if (!iface.address.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.network_field_address),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = iface.address,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                if (!iface.netmask.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.network_field_netmask),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = iface.netmask,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                if (!iface.gateway.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.network_field_gateway),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = iface.gateway,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                if (iface.families.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.network_field_families),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = iface.families.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.network_field_autostart),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (iface.autostart) stringResource(R.string.network_yes) else stringResource(R.string.network_no),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (iface.autostart) Color.Green else Color.Red
                    )
                }
            }
        }
    }
} 