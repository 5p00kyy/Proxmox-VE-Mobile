package com.proxmoxmobile.presentation.navigation

import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.proxmoxmobile.R
import com.proxmoxmobile.data.lxc.LxcRepository
import com.proxmoxmobile.data.network.NetworkRepository
import com.proxmoxmobile.data.node.NodeRepository
import com.proxmoxmobile.data.storage.StorageRepository
import com.proxmoxmobile.data.task.TaskRepository
import com.proxmoxmobile.data.vm.VmRepository
import com.proxmoxmobile.presentation.screens.auth.LoginScreen
import com.proxmoxmobile.presentation.screens.dashboard.DashboardScreen
import com.proxmoxmobile.presentation.screens.nodes.NodeDetailScreen
import com.proxmoxmobile.presentation.screens.servers.ServerListScreen
import com.proxmoxmobile.presentation.screens.settings.SettingsScreen
import com.proxmoxmobile.presentation.screens.vms.VMDetailScreen
import com.proxmoxmobile.presentation.screens.vms.VMListScreen
import com.proxmoxmobile.presentation.screens.containers.ContainerListScreen
import com.proxmoxmobile.presentation.screens.storage.StorageScreen
import com.proxmoxmobile.presentation.screens.network.NetworkScreen
import com.proxmoxmobile.presentation.screens.users.UserManagementScreen
import com.proxmoxmobile.presentation.screens.backups.BackupScreen
import com.proxmoxmobile.presentation.screens.tasks.TaskDetailScreen
import com.proxmoxmobile.presentation.screens.tasks.TaskScreen
import com.proxmoxmobile.presentation.screens.cluster.ClusterScreen
import com.proxmoxmobile.presentation.viewmodel.MainViewModel

@Composable
fun ProxmoxNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    startDestination: String = Screen.Login.route,
    vmRepositoryOverride: VmRepository? = null,
    lxcRepositoryOverride: LxcRepository? = null,
    nodeRepositoryOverride: NodeRepository? = null,
    taskRepositoryOverride: TaskRepository? = null,
    storageRepositoryOverride: StorageRepository? = null,
    networkRepositoryOverride: NetworkRepository? = null
) {
    val confirmationDialog by viewModel.showConfirmationDialog.collectAsState()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        
        composable(Screen.ContainerList.pattern) { backStackEntry ->
            val nodeName = backStackEntry.arguments?.getString("nodeName")?.let(Uri::decode)
            ContainerListScreen(
                navController = navController,
                viewModel = viewModel,
                nodeName = nodeName,
                repositoryOverride = lxcRepositoryOverride
            )
        }
        
        composable(Screen.VMList.pattern) { backStackEntry ->
            val nodeName = backStackEntry.arguments?.getString("nodeName")?.let(Uri::decode)
            VMListScreen(
                navController = navController,
                viewModel = viewModel,
                nodeName = nodeName,
                repositoryOverride = vmRepositoryOverride
            )
        }
        
        composable(Screen.Storage.pattern) { backStackEntry ->
            val nodeName = backStackEntry.arguments?.getString("nodeName")?.let(Uri::decode)
            StorageScreen(
                navController = navController,
                viewModel = viewModel,
                nodeName = nodeName,
                repositoryOverride = storageRepositoryOverride
            )
        }
        
        composable(Screen.Network.route) {
            NetworkScreen(
                navController = navController,
                viewModel = viewModel,
                repositoryOverride = networkRepositoryOverride
            )
        }

        composable(Screen.NodeNetwork.route) { backStackEntry ->
            val node = backStackEntry.arguments?.getString("node")?.let(Uri::decode)
            NetworkScreen(
                navController = navController,
                viewModel = viewModel,
                initialNodeName = node,
                repositoryOverride = networkRepositoryOverride
            )
        }
        
        composable(Screen.Users.route) {
            UserManagementScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        
        composable(Screen.Tasks.route) {
            TaskScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable(Screen.NodeTasks.route) { backStackEntry ->
            val node = backStackEntry.arguments?.getString("node")?.let(Uri::decode)
            TaskScreen(
                navController = navController,
                viewModel = viewModel,
                initialNodeName = node
            )
        }

        composable(Screen.ResourceTasks.route) { backStackEntry ->
            val node = backStackEntry.arguments?.getString("node")?.let(Uri::decode)
            val vmid = backStackEntry.arguments?.getString("vmid")?.toIntOrNull()
            TaskScreen(
                navController = navController,
                viewModel = viewModel,
                initialNodeName = node,
                initialVmid = vmid
            )
        }

        composable(Screen.TaskDetail.route) { backStackEntry ->
            val node = backStackEntry.arguments?.getString("node")?.let(Uri::decode).orEmpty()
            val upid = backStackEntry.arguments?.getString("upid")?.let(Uri::decode).orEmpty()
            TaskDetailScreen(
                navController = navController,
                viewModel = viewModel,
                nodeName = node,
                upid = upid,
                repositoryOverride = taskRepositoryOverride
            )
        }
        
        composable(Screen.Backups.route) {
            BackupScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        
        composable(Screen.Cluster.route) {
            ClusterScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        
        composable(Screen.ServerList.route) {
            ServerListScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable(Screen.NodeDetail.route) { backStackEntry ->
            val node = backStackEntry.arguments?.getString("node")?.let(Uri::decode)
            if (!node.isNullOrBlank()) {
                NodeDetailScreen(
                    nodeName = node,
                    viewModel = viewModel,
                    navController = navController,
                    repositoryOverride = nodeRepositoryOverride
                )
            }
        }

        composable(Screen.ContainerDetail.route) { backStackEntry ->
            val vmid = backStackEntry.arguments?.getString("vmid")?.toIntOrNull()
            if (vmid != null) {
                com.proxmoxmobile.presentation.screens.containers.ContainerDetailScreen(
                    vmid = vmid,
                    nodeName = null,
                    viewModel = viewModel,
                    navController = navController,
                    repositoryOverride = lxcRepositoryOverride
                )
            }
        }

        composable(Screen.ContainerDetailWithNode.route) { backStackEntry ->
            val node = backStackEntry.arguments?.getString("node")?.let(Uri::decode)
            val vmid = backStackEntry.arguments?.getString("vmid")?.toIntOrNull()
            if (vmid != null) {
                com.proxmoxmobile.presentation.screens.containers.ContainerDetailScreen(
                    vmid = vmid,
                    nodeName = node,
                    viewModel = viewModel,
                    navController = navController,
                    repositoryOverride = lxcRepositoryOverride
                )
            }
        }

        composable(Screen.VMDetail.route) { backStackEntry ->
            val vmid = backStackEntry.arguments?.getString("vmid")?.toIntOrNull()
            if (vmid != null) {
                VMDetailScreen(
                    vmid = vmid,
                    nodeName = null,
                    viewModel = viewModel,
                    navController = navController,
                    repositoryOverride = vmRepositoryOverride
                )
            }
        }

        composable(Screen.VMDetailWithNode.route) { backStackEntry ->
            val node = backStackEntry.arguments?.getString("node")?.let(Uri::decode)
            val vmid = backStackEntry.arguments?.getString("vmid")?.toIntOrNull()
            if (vmid != null) {
                VMDetailScreen(
                    vmid = vmid,
                    nodeName = node,
                    viewModel = viewModel,
                    navController = navController,
                    repositoryOverride = vmRepositoryOverride
                )
            }
        }
    }

    confirmationDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = {
                dialog.onDismiss()
                viewModel.hideConfirmationDialog()
            },
            title = { Text(dialog.title) },
            text = { Text(dialog.message) },
            confirmButton = {
                TextButton(onClick = dialog.onConfirm) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dialog.onDismiss()
                        viewModel.hideConfirmationDialog()
                    }
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}
