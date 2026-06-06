package com.proxmoxmobile.presentation.screens.backups

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
import androidx.compose.ui.res.stringResource
import com.proxmoxmobile.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.navigation.NavController
import com.proxmoxmobile.data.backup.BackupRepository
import com.proxmoxmobile.data.backup.ProxmoxBackupApi
import com.proxmoxmobile.data.model.Backup
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val availableNodes = viewModel.getCachedNodes()
        ?.map { it.node }
        ?.filter { it.isNotBlank() }
        .orEmpty()
    val noNodesMessage = stringResource(R.string.backup_error_no_nodes)
    val backupRepository = remember(viewModel) {
        BackupRepository(ProxmoxBackupApi { viewModel.getApiService() })
    }
    val backupListViewModel: BackupListViewModel = composeViewModel(
        key = "backup-list-${availableNodes.joinToString("|")}",
        factory = remember(availableNodes, backupRepository, noNodesMessage) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(BackupListViewModel::class.java)) {
                        return BackupListViewModel(
                            availableNodes = availableNodes,
                            repository = backupRepository,
                            noNodesMessage = noNodesMessage
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    )
    val uiState by backupListViewModel.uiState.collectAsState()
    var nodeMenuExpanded by remember { mutableStateOf(false) }
    var storageMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(backupListViewModel) {
        backupListViewModel.loadBackups()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.backup_management_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.backup_back))
                    }
                },
                actions = {
                    uiState.selectedNodeName?.let { selectedNode ->
                        Box {
                            TextButton(
                                onClick = { nodeMenuExpanded = true },
                                enabled = uiState.availableNodes.size > 1
                            ) {
                                Text(
                                    text = stringResource(R.string.backup_node_label, selectedNode),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                                if (uiState.availableNodes.size > 1) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(
                                expanded = nodeMenuExpanded,
                                onDismissRequest = { nodeMenuExpanded = false }
                            ) {
                                uiState.availableNodes.forEach { nodeName ->
                                    DropdownMenuItem(
                                        text = { Text(nodeName) },
                                        onClick = {
                                            nodeMenuExpanded = false
                                            backupListViewModel.selectNode(nodeName)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    IconButton(
                        onClick = { backupListViewModel.loadBackups(showLoading = false) },
                        enabled = !uiState.isLoading && !uiState.isRefreshing
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.backup_retry))
                    }
                    IconButton(
                        onClick = {},
                        enabled = false
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.backup_create_backup))
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
            uiState.isLoading -> {
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
                            text = stringResource(R.string.backup_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            uiState.errorMessage != null -> {
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
                            text = stringResource(R.string.backup_error),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { backupListViewModel.loadBackups() }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.backup_retry))
                        }
                    }
                }
            }
            uiState.visibleBackupEntries.isEmpty() -> {
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
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.backup_none_found_title),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.backup_none_available_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        BackupStorageFilter(
                            uiState = uiState,
                            expanded = storageMenuExpanded,
                            onExpandedChange = { storageMenuExpanded = it },
                            onStorageSelected = backupListViewModel::selectStorage
                        )
                        BackupStorageWarning(uiState = uiState)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.backup_actions_planned),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {},
                            enabled = false
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.backup_create_first))
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        BackupStorageFilter(
                            uiState = uiState,
                            expanded = storageMenuExpanded,
                            onExpandedChange = { storageMenuExpanded = it },
                            onStorageSelected = backupListViewModel::selectStorage
                        )
                        BackupStorageWarning(uiState = uiState)
                    }

                    item {
                        Text(
                            text = stringResource(R.string.backup_list_header, uiState.visibleBackupEntries.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(uiState.visibleBackupEntries) { entry ->
                        BackupCard(backup = entry.backup)
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupStorageFilter(
    uiState: BackupListUiState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onStorageSelected: (String?) -> Unit
) {
    if (uiState.backupStorages.isEmpty()) return

    val selectedStorage = uiState.selectedStorageName
        ?: stringResource(R.string.backup_storage_all)

    Box {
        OutlinedButton(
            onClick = { onExpandedChange(true) },
            enabled = uiState.backupStorages.isNotEmpty()
        ) {
            Text(
                text = stringResource(R.string.backup_storage_label, selectedStorage),
                maxLines = 1
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.backup_storage_all)) },
                onClick = {
                    onExpandedChange(false)
                    onStorageSelected(null)
                }
            )
            uiState.backupStorages.forEach { storage ->
                DropdownMenuItem(
                    text = { Text(storage.storage) },
                    onClick = {
                        onExpandedChange(false)
                        onStorageSelected(storage.storage)
                    }
                )
            }
        }
    }
}

@Composable
private fun BackupStorageWarning(uiState: BackupListUiState) {
    if (uiState.storageErrors.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = stringResource(
                R.string.backup_storage_partial_error,
                uiState.storageErrors.joinToString { it.storageName }
            ),
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupCard(backup: Backup) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val sizeInMB = backup.size / (1024 * 1024)

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
            // Header row with backup name and format
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = backup.volid,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = backup.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Format indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = when (backup.format.lowercase()) {
                                    "vma" -> Color.Blue
                                    "raw" -> Color.Green
                                    "qcow2" -> Color.Yellow
                                    else -> Color.Gray
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    Text(
                        text = backup.format.uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = when (backup.format.lowercase()) {
                            "vma" -> Color.Blue
                            "raw" -> Color.Green
                            "qcow2" -> Color.Yellow
                            else -> Color.Gray
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Backup details
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.backup_size_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.backup_size_mb, sizeInMB),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.backup_created_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(Date(backup.ctime * 1000)),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (!backup.notes.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.backup_notes_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = backup.notes,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Action buttons
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.backup_actions_planned),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = stringResource(R.string.backup_download),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = stringResource(R.string.backup_restore),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = stringResource(R.string.backup_delete),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
