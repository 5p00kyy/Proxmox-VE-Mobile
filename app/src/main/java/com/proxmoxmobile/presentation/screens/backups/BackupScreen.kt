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
import androidx.navigation.NavController
import com.proxmoxmobile.data.model.Backup
import com.proxmoxmobile.data.model.Storage
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import android.util.Log
import kotlinx.coroutines.launch
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
    var backups by remember { mutableStateOf<List<Backup>>(emptyList()) }
    var storages by remember { mutableStateOf<List<Storage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedNode by remember { mutableStateOf<String?>(null) }
    var selectedStorage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Get cached nodes to determine which node to query
    val cachedNodes = viewModel.getCachedNodes()

    // Fetch backups when screen loads
    LaunchedEffect(Unit) {
        try {
            scope.launch {
                try {
                    isLoading = true
                    errorMessage = null
                    
                    Log.d("BackupScreen", "Starting to load backups")
                    
                    // Check if we have a valid API service
                    val apiService = try {
                        viewModel.getApiService()
                    } catch (e: Exception) {
                        Log.e("BackupScreen", "Failed to get API service", e)
                        errorMessage = "Authentication failed - please login again"
                        return@launch
                    }
                    
                    if (apiService == null) {
                        Log.e("BackupScreen", "API service is null")
                        errorMessage = "Not authenticated - please login again"
                        return@launch
                    }
                    
                    // Use the first available node, or show error if no nodes
                    val nodes = cachedNodes ?: emptyList()
                    if (nodes.isEmpty()) {
                        errorMessage = "No nodes available - please check dashboard first"
                        return@launch
                    }
                    
                    selectedNode = nodes.first().node
                    Log.d("BackupScreen", "Using node: $selectedNode")
                    
                    // First, fetch storages to get backup locations
                    val storageResponse = try {
                        apiService.getStorages(selectedNode!!)
                    } catch (e: Exception) {
                        Log.e("BackupScreen", "Failed to fetch storages", e)
                        errorMessage = "Failed to fetch storage information: ${e.message}"
                        return@launch
                    }
                    
                    val storageList = storageResponse.data ?: emptyList()
                    storages = storageList.filter { it.content.contains("backup") || it.content.contains("vzdump") }
                    Log.d("BackupScreen", "Found ${storages.size} backup-capable storages")
                    
                    // Fetch backups from each storage
                    val allBackups = mutableListOf<Backup>()
                    for (storage in storages) {
                        try {
                            val backupResponse = apiService.getStorageContent(selectedNode!!, storage.storage)
                            val storageBackups = backupResponse.data ?: emptyList()
                            allBackups.addAll(storageBackups)
                            Log.d("BackupScreen", "Found ${storageBackups.size} backups in storage ${storage.storage}")
                        } catch (e: Exception) {
                            Log.w("BackupScreen", "Failed to fetch backups from storage ${storage.storage}", e)
                        }
                    }
                    
                    Log.d("BackupScreen", "Total backups found: ${allBackups.size}")
                    
                    // Validate each backup before adding to the list
                    val validBackups = allBackups.filter { backup ->
                        try {
                            backup.volid.isNotBlank() && 
                            backup.content.isNotBlank()
                        } catch (e: Exception) {
                            Log.w("BackupScreen", "Invalid backup data: ${e.message}")
                            false
                        }
                    }
                    backups = validBackups
                    Log.d("BackupScreen", "Successfully loaded ${backups.size} valid backups")
                } catch (e: retrofit2.HttpException) {
                    Log.e("BackupScreen", "HTTP error loading backups: ${e.code()}", e)
                    when (e.code()) {
                        401 -> errorMessage = "Authentication required - please login again"
                        403 -> errorMessage = "Access forbidden - check permissions"
                        500 -> errorMessage = "Server error - please try again"
                        else -> errorMessage = "Failed to load backups: HTTP ${e.code()}"
                    }
                } catch (e: Exception) {
                    Log.e("BackupScreen", "Failed to load backups", e)
                    errorMessage = "Failed to load backups: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        } catch (e: Exception) {
            Log.e("BackupScreen", "Critical error in LaunchedEffect", e)
            errorMessage = "An unexpected error occurred"
            isLoading = false
        }
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
                    if (selectedNode != null) {
                        Text(
                            text = stringResource(R.string.backup_node_label, selectedNode ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
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
                            text = stringResource(R.string.backup_loading),
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
                            text = stringResource(R.string.backup_error),
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
                                    backups = emptyList()
                                    storages = emptyList()
                                    errorMessage = null
                                    isLoading = true
                                    // This will trigger LaunchedEffect again
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.backup_retry))
                        }
                    }
                }
            }
            backups.isEmpty() -> {
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
                        Button(
                            onClick = { showCreateDialog = true }
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.backup_list_header, backups.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    items(backups) { backup ->
                        BackupCard(backup = backup)
                    }
                }
            }
        }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { /* TODO: Download backup */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.backup_download))
                }
                
                OutlinedButton(
                    onClick = { /* TODO: Restore backup */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.backup_restore))
                }
                
                OutlinedButton(
                    onClick = { /* TODO: Delete backup */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.backup_delete))
                }
            }
        }
    }
} 