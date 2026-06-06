package com.proxmoxmobile.presentation.screens.storage

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.navigation.NavController
import com.proxmoxmobile.R
import com.proxmoxmobile.data.model.Storage
import com.proxmoxmobile.data.model.StorageContent
import com.proxmoxmobile.data.storage.ProxmoxStorageApi
import com.proxmoxmobile.data.storage.StorageRepository
import com.proxmoxmobile.presentation.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    navController: NavController,
    viewModel: MainViewModel,
    nodeName: String? = null
) {
    val invalidNodeName = stringResource(R.string.storage_invalid_node)
    val storageRepository = remember(viewModel) {
        StorageRepository(ProxmoxStorageApi { viewModel.getApiService() })
    }
    val storageViewModel: StorageViewModel = composeViewModel(
        key = "storage-${nodeName.orEmpty()}",
        factory = remember(nodeName, storageRepository, invalidNodeName) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(StorageViewModel::class.java)) {
                        return StorageViewModel(
                            nodeName = nodeName,
                            repository = storageRepository,
                            invalidNodeMessage = invalidNodeName
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    )
    val uiState by storageViewModel.uiState.collectAsState()

    LaunchedEffect(nodeName, storageViewModel) {
        storageViewModel.loadStorages()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.storage_title, nodeName ?: stringResource(R.string.storage_unknown))) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.storage_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { storageViewModel.loadStorages(showLoading = false) },
                        enabled = !uiState.isLoading && !uiState.isRefreshing
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.storage_retry))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Error message
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                OutlinedButton(
                    onClick = { storageViewModel.loadStorages() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.storage_retry))
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.storages.isEmpty() && uiState.errorMessage == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.storage_no_storage_found),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.storage_no_storage_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.storage_header, uiState.storages.size),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    items(uiState.storages) { storage ->
                        StorageCard(
                            storage = storage,
                            selected = uiState.selectedStorageName == storage.storage,
                            contentLoading = uiState.selectedStorageName == storage.storage &&
                                (uiState.isContentLoading || uiState.isContentRefreshing),
                            onBrowseContent = {
                                storageViewModel.loadStorageContent(
                                    storageName = storage.storage,
                                    showLoading = uiState.selectedStorageName != storage.storage
                                )
                            }
                        )

                        if (uiState.selectedStorageName == storage.storage) {
                            StorageContentSection(
                                storageName = storage.storage,
                                content = uiState.selectedStorageContent,
                                isLoading = uiState.isContentLoading,
                                errorMessage = uiState.contentErrorMessage,
                                onRetry = {
                                    storageViewModel.loadStorageContent(storage.storage)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageCard(
    storage: Storage,
    selected: Boolean = false,
    contentLoading: Boolean = false,
    onBrowseContent: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = storage.storage,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = storage.type,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = when (storage.type) {
                        "dir" -> Icons.Default.Folder
                        "lvm" -> Icons.Default.Storage
                        "nfs" -> Icons.Default.Cloud
                        "iscsi" -> Icons.Default.Storage
                        "rbd" -> Icons.Default.Storage
                        "zfs" -> Icons.Default.Storage
                        else -> Icons.Default.Storage
                    },
                    contentDescription = storage.type,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.storage_content),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = storage.content.takeIf { it.isNotEmpty() }?.joinToString(", ")
                        ?: stringResource(R.string.storage_none),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.storage_available),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.storage_size_gb, (storage.available / 1024 / 1024 / 1024).toInt()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.storage_used),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.storage_size_gb, (storage.used / 1024 / 1024 / 1024).toInt()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.storage_total, (storage.total / 1024 / 1024 / 1024).toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onBrowseContent,
                enabled = storage.active && !contentLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (contentLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.storage_browse_content))
            }
        }
    }
}

@Composable
private fun StorageContentSection(
    storageName: String,
    content: List<StorageContent>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.storage_content_title, storageName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            when {
                isLoading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.storage_content_loading))
                    }
                }
                errorMessage != null -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                        OutlinedButton(onClick = onRetry) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.storage_retry))
                        }
                    }
                }
                content.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.storage_content_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    content.forEach { item ->
                        StorageContentRow(item)
                    }
                    Text(
                        text = stringResource(R.string.storage_content_actions_planned),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageContentRow(item: StorageContent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.content.toStorageContentIcon(),
            contentDescription = item.content,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.volid,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(
                    R.string.storage_content_detail,
                    item.content,
                    item.format?.takeIf { it.isNotBlank() } ?: stringResource(R.string.storage_unknown),
                    formatStorageContentBytes(item.size)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            item.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun String.toStorageContentIcon() = when (lowercase()) {
    "backup", "vzdump" -> Icons.Default.Archive
    "iso" -> Icons.Default.DiscFull
    "vztmpl" -> Icons.Default.Inventory
    "images", "rootdir" -> Icons.Default.Storage
    "snippets" -> Icons.Default.Description
    else -> Icons.Default.Description
}

private fun formatStorageContentBytes(bytes: Long?): String {
    val value = bytes ?: return "-"
    return when {
        value >= 1024L * 1024L * 1024L -> "${value / 1024L / 1024L / 1024L}GB"
        value >= 1024L * 1024L -> "${value / 1024L / 1024L}MB"
        value >= 1024L -> "${value / 1024L}KB"
        else -> "${value}B"
    }
}
