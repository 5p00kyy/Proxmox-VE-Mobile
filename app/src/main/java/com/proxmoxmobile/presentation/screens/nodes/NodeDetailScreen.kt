package com.proxmoxmobile.presentation.screens.nodes

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.navigation.NavController
import com.proxmoxmobile.R
import com.proxmoxmobile.data.model.NodeStatus
import com.proxmoxmobile.data.node.NodeRepository
import com.proxmoxmobile.data.node.ProxmoxNodeApi
import com.proxmoxmobile.presentation.navigation.Screen
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeDetailScreen(
    nodeName: String,
    viewModel: MainViewModel,
    navController: NavController
) {
    val nodeRepository = remember(viewModel) {
        NodeRepository(ProxmoxNodeApi { viewModel.getApiService() })
    }
    val nodeDetailViewModel: NodeDetailViewModel = composeViewModel(
        key = "node-detail-$nodeName",
        factory = remember(nodeName, nodeRepository) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(NodeDetailViewModel::class.java)) {
                        return NodeDetailViewModel(
                            nodeName = nodeName,
                            repository = nodeRepository
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    )
    val uiState by nodeDetailViewModel.uiState.collectAsState()
    val status = uiState.status
    val errorMessage = uiState.errorMessage

    LaunchedEffect(nodeDetailViewModel) {
        nodeDetailViewModel.loadNode()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.node_detail_title, uiState.nodeName)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.node_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { nodeDetailViewModel.loadNode(showLoading = false) },
                        enabled = !uiState.isLoading && !uiState.isRefreshing
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.node_refresh))
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
        } else if (status != null) {
            NodeDetailContent(
                nodeName = uiState.nodeName,
                status = status,
                errorMessage = errorMessage,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                onOpenVms = {
                    navController.navigate(Screen.VMList.createRoute(uiState.nodeName))
                },
                onOpenContainers = {
                    navController.navigate(Screen.ContainerList.createRoute(uiState.nodeName))
                },
                onOpenStorage = {
                    navController.navigate(Screen.Storage.createRoute(uiState.nodeName))
                },
                onOpenNetwork = {
                    navController.navigate(Screen.NodeNetwork.createRoute(uiState.nodeName))
                },
                onOpenTasks = {
                    navController.navigate(Screen.NodeTasks.createRoute(uiState.nodeName))
                }
            )
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Filled.Info,
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
                        Text(stringResource(R.string.node_go_back))
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeDetailContent(
    nodeName: String,
    status: NodeStatus,
    errorMessage: String?,
    modifier: Modifier,
    onOpenVms: () -> Unit,
    onOpenContainers: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenTasks: () -> Unit
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Computer,
                            contentDescription = null,
                            tint = status.status.toStatusColor()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.node_overview),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(stringResource(R.string.node_name_label, nodeName))
                    Text(
                        text = stringResource(R.string.node_status_label, status.status),
                        color = status.status.toStatusColor()
                    )
                    Text(stringResource(R.string.node_pve_version_label, status.pveversion.ifBlank { "-" }))
                    Text(stringResource(R.string.node_kernel_label, status.kversion.ifBlank { "-" }))
                    Text(stringResource(R.string.node_uptime_label, formatNodeUptime(status.uptime)))
                    Text(stringResource(R.string.node_load_average_label, status.loadavg.joinToString(", ") { it.format(2) }))
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.node_resources),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(stringResource(R.string.node_cpu_usage, status.cpu * 100))
                    Text(stringResource(R.string.node_cpu_cores, status.maxcpu))
                    Text(stringResource(R.string.node_memory_usage, formatNodeBytes(status.mem), formatNodeBytes(status.maxmem)))
                    Text(stringResource(R.string.node_rootfs_usage, formatNodeBytes(status.rootfs.used), formatNodeBytes(status.rootfs.total)))
                    Text(stringResource(R.string.node_swap_usage, formatNodeBytes(status.swap.used), formatNodeBytes(status.swap.total)))
                }
            }
        }

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
                        text = stringResource(R.string.node_actions),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NodeActionButton(
                            text = stringResource(R.string.node_open_vms),
                            icon = Icons.Filled.Computer,
                            onClick = onOpenVms,
                            modifier = Modifier.weight(1f)
                        )
                        NodeActionButton(
                            text = stringResource(R.string.node_open_containers),
                            icon = Icons.Filled.Storage,
                            onClick = onOpenContainers,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NodeActionButton(
                            text = stringResource(R.string.node_open_storage),
                            icon = Icons.Filled.Folder,
                            onClick = onOpenStorage,
                            modifier = Modifier.weight(1f)
                        )
                        NodeActionButton(
                            text = stringResource(R.string.node_open_network),
                            icon = Icons.Filled.Wifi,
                            onClick = onOpenNetwork,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenTasks,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.node_open_tasks))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.node_open_tasks))
                    }
                }
            }
        }

        if (errorMessage != null) {
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
                            Icons.Filled.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(icon, contentDescription = text)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text)
    }
}

private fun String.toStatusColor(): Color {
    return when (this.lowercase(Locale.US)) {
        "online", "running" -> Color.Green
        "offline", "stopped" -> Color.Red
        else -> Color.Gray
    }
}

private fun formatNodeUptime(uptime: Long): String {
    return when {
        uptime < 60 -> "${uptime}s"
        uptime < 3600 -> "${uptime / 60}m"
        uptime < 86400 -> "${uptime / 3600}h"
        else -> "${uptime / 86400}d"
    }
}

private fun formatNodeBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> "${(bytes.toDouble() / 1024 / 1024 / 1024).format(1)}GB"
        bytes >= 1024 * 1024 -> "${(bytes.toDouble() / 1024 / 1024).format(1)}MB"
        bytes >= 1024 -> "${(bytes.toDouble() / 1024).format(1)}KB"
        else -> "${bytes}B"
    }
}

private fun Double.format(digits: Int): String {
    return String.format(Locale.US, "%.${digits}f", this)
}
