package com.proxmoxmobile.presentation.screens.vms

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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
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
import com.proxmoxmobile.data.model.VmSnapshot
import com.proxmoxmobile.data.vm.VmConfigEntry
import com.proxmoxmobile.data.vm.ProxmoxVmApi
import com.proxmoxmobile.data.vm.VmRepository
import com.proxmoxmobile.presentation.navigation.Screen
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VMDetailScreen(
    vmid: Int,
    nodeName: String? = null,
    viewModel: MainViewModel,
    navController: NavController,
    repositoryOverride: VmRepository? = null
) {
    val candidateNodeNames = remember(viewModel, nodeName) {
        (
            listOfNotNull(nodeName?.takeIf { it.isNotBlank() }) +
                viewModel.getCachedNodes().orEmpty().map { it.node }.filter { it.isNotBlank() }
            )
            .distinct()
    }
    val defaultVmRepository = remember(viewModel) {
        VmRepository(ProxmoxVmApi { viewModel.getApiService() })
    }
    val vmRepository = repositoryOverride ?: defaultVmRepository
    val vmDetailViewModel: VmDetailViewModel = composeViewModel(
        key = "vm-detail-${nodeName.orEmpty()}-$vmid",
        factory = remember(vmid, candidateNodeNames, nodeName, vmRepository) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(VmDetailViewModel::class.java)) {
                        return VmDetailViewModel(
                            vmid = vmid,
                            nodeNames = candidateNodeNames,
                            preferredNodeName = nodeName,
                            repository = vmRepository
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    )
    val uiState by vmDetailViewModel.uiState.collectAsState()
    val vm = uiState.virtualMachine
    val resolvedNodeName = uiState.nodeName?.takeIf { it.isNotBlank() }
    val errorMessage = uiState.errorMessage

    LaunchedEffect(vmDetailViewModel) {
        vmDetailViewModel.loadVirtualMachine()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vm_details_title, vmid)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.vm_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { vmDetailViewModel.loadVirtualMachine(showLoading = false) },
                        enabled = !uiState.isLoading && !uiState.isRefreshing
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.vm_refresh))
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
        } else if (vm != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                    tint = vm.status.toStatusColor()
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.vm_information),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(stringResource(R.string.vm_name_label, vm.name))
                            Text(stringResource(R.string.vm_id_label, vm.vmid))
                            resolvedNodeName?.let {
                                Text(stringResource(R.string.vm_node_label, it))
                            }
                            Text(
                                text = stringResource(R.string.vm_status_label, vm.status),
                                color = vm.status.toStatusColor()
                            )
                            Text(stringResource(R.string.vm_uptime_hours, (vm.uptime / 3600).toInt()))
                            Text(stringResource(R.string.vm_template_label, vm.template.toYesNoString()))
                            vm.tags?.takeIf { it.isNotBlank() }?.let {
                                Text(stringResource(R.string.vm_tags_label, it))
                            }
                        }
                    }
                }

                item {
                    VmSnapshotsCard(
                        snapshots = uiState.snapshots,
                        isLoading = uiState.isSnapshotsLoading,
                        isRefreshing = uiState.isSnapshotsRefreshing,
                        errorMessage = uiState.snapshotErrorMessage,
                        onRefresh = {
                            vmDetailViewModel.loadSnapshots(showLoading = false)
                        }
                    )
                }

                item {
                    VmConfigCard(
                        entries = uiState.configEntries,
                        isLoading = uiState.isConfigLoading,
                        isRefreshing = uiState.isConfigRefreshing,
                        errorMessage = uiState.configErrorMessage,
                        onRefresh = {
                            vmDetailViewModel.loadConfig(showLoading = false)
                        }
                    )
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
                            Text(
                                text = stringResource(R.string.vm_current_resource_usage),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(stringResource(R.string.vm_cpu_usage, vm.cpu * 100))
                            Text(stringResource(R.string.vm_cpu_cores_count, vm.cpus))
                            Text(stringResource(R.string.vm_ram_usage, formatVmBytes(vm.mem)))
                            Text(stringResource(R.string.vm_ram_allocated, formatVmBytes(vm.maxmem)))
                            Text(stringResource(R.string.vm_disk_allocated, formatVmBytes(vm.disk)))
                            Text(stringResource(R.string.vm_disk_io, formatVmBytes(vm.diskread), formatVmBytes(vm.diskwrite)))
                            Text(stringResource(R.string.vm_network_io, formatVmBytes(vm.netin), formatVmBytes(vm.netout)))
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
                            Text(
                                text = stringResource(R.string.vm_runtime_state),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(stringResource(R.string.vm_qmp_status_label, vm.qmpstatus.ifBlank { "-" }))
                            Text(stringResource(R.string.vm_machine_label, vm.running_machine ?: "-"))
                            Text(stringResource(R.string.vm_qemu_label, vm.running_qemu ?: "-"))
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
                                text = stringResource(R.string.vm_actions),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedButton(
                                onClick = {
                                    resolvedNodeName?.let {
                                        navController.navigate(Screen.ResourceTasks.createRoute(it, vm.vmid))
                                    }
                                },
                                enabled = resolvedNodeName != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.vm_view_tasks))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.vm_view_tasks))
                            }
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Terminal, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.vm_open_console))
                            }
                            Text(
                                text = stringResource(R.string.vm_console_not_implemented),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                        Text(stringResource(R.string.vm_go_back))
                    }
                }
            }
        }
    }
}

@Composable
private fun VmConfigCard(
    entries: List<VmConfigEntry>,
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
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.vm_config_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading && !isRefreshing
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.vm_config_refresh))
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
                        Text(stringResource(R.string.vm_config_loading))
                    }
                }
                errorMessage != null -> {
                    Text(
                        text = stringResource(R.string.vm_config_error, errorMessage),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                entries.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.vm_config_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    entries.take(MAX_CONFIG_ROWS).forEach { entry ->
                        VmConfigRow(entry)
                    }
                    if (entries.size > MAX_CONFIG_ROWS) {
                        Text(
                            text = stringResource(R.string.vm_config_more, entries.size - MAX_CONFIG_ROWS),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(R.string.vm_config_actions_planned),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun VmConfigRow(entry: VmConfigEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = entry.key,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.38f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = entry.value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.62f)
        )
    }
}

private const val MAX_CONFIG_ROWS = 12

@Composable
private fun VmSnapshotsCard(
    snapshots: List<VmSnapshot>,
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
                    text = stringResource(R.string.vm_snapshots_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading && !isRefreshing
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.vm_snapshots_refresh))
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
                        Text(stringResource(R.string.vm_snapshots_loading))
                    }
                }
                errorMessage != null -> {
                    Text(
                        text = stringResource(R.string.vm_snapshots_error, errorMessage),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                snapshots.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.vm_snapshots_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    snapshots.forEach { snapshot ->
                        VmSnapshotRow(snapshot)
                    }
                    Text(
                        text = stringResource(R.string.vm_snapshots_actions_planned),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun VmSnapshotRow(snapshot: VmSnapshot) {
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
                    R.string.vm_snapshot_detail,
                    formatVmSnapshotTime(snapshot.snaptime),
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

private fun String.toStatusColor(): Color {
    return when (this) {
        "running" -> Color.Green
        "stopped" -> Color.Red
        "paused" -> Color.Yellow
        else -> Color.Gray
    }
}

@Composable
private fun Boolean.toYesNoString(): String {
    return if (this) stringResource(R.string.common_yes) else stringResource(R.string.common_no)
}

private fun formatVmBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> "${(bytes.toDouble() / 1024 / 1024 / 1024).format(1)}GB"
        bytes >= 1024 * 1024 -> "${(bytes.toDouble() / 1024 / 1024).format(1)}MB"
        bytes >= 1024 -> "${(bytes.toDouble() / 1024).format(1)}KB"
        else -> "${bytes}B"
    }
}

private fun formatVmSnapshotTime(snaptime: Long?): String {
    val seconds = snaptime?.takeIf { it > 0 } ?: return "-"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(seconds * 1000L))
}

private fun Double.format(digits: Int): String {
    return String.format(Locale.US, "%.${digits}f", this)
}
