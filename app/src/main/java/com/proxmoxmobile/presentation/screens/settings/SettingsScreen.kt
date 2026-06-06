package com.proxmoxmobile.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.proxmoxmobile.R
import com.proxmoxmobile.presentation.navigation.Screen
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import com.proxmoxmobile.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    var showClearCredentialsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var autoRefreshInterval by remember { mutableStateOf(30) } // seconds
    var enableNotifications by remember { mutableStateOf(true) }
    var enableBiometric by remember { mutableStateOf(false) }
    var enableAutoLogin by remember { mutableStateOf(viewModel.hasSavedCredentials()) }
    var enableDarkMode by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
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
            // App Info Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.settings_app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.settings_app_version, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = stringResource(R.string.settings_app_tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Display Settings Section
            item {
                SettingsSection(title = stringResource(R.string.settings_section_display)) {
                    SettingsSwitchItem(
                        icon = Icons.Default.DarkMode,
                        title = stringResource(R.string.settings_dark_mode_title),
                        subtitle = stringResource(R.string.settings_dark_mode_subtitle),
                        checked = enableDarkMode,
                        onCheckedChange = { enableDarkMode = it },
                        enabled = false
                    )
                }
            }

            // Auto-refresh Settings Section
            item {
                SettingsSection(title = stringResource(R.string.settings_section_auto_refresh)) {
                    SettingsSliderItem(
                        icon = Icons.Default.Refresh,
                        title = stringResource(R.string.settings_refresh_interval_title),
                        subtitle = stringResource(R.string.settings_refresh_interval_subtitle, autoRefreshInterval),
                        value = autoRefreshInterval.toFloat(),
                        onValueChange = { autoRefreshInterval = it.toInt() },
                        valueRange = 10f..60f,
                        steps = 5,
                        enabled = false
                    )
                }
            }

            // Security Settings Section
            item {
                SettingsSection(title = stringResource(R.string.settings_section_security)) {
                    SettingsSwitchItem(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.settings_biometric_title),
                        subtitle = stringResource(R.string.settings_biometric_subtitle),
                        checked = enableBiometric,
                        onCheckedChange = { enableBiometric = it },
                        enabled = false
                    )

                    SettingsSwitchItem(
                        icon = Icons.AutoMirrored.Filled.Login,
                        title = stringResource(R.string.settings_auto_login_title),
                        subtitle = stringResource(R.string.settings_auto_login_subtitle),
                        checked = enableAutoLogin,
                        onCheckedChange = { enableAutoLogin = it },
                        enabled = false
                    )

                    SettingsButtonItem(
                        icon = Icons.Default.Delete,
                        title = stringResource(R.string.settings_clear_credentials_title),
                        subtitle = stringResource(R.string.settings_clear_credentials_subtitle),
                        onClick = { showClearCredentialsDialog = true }
                    )
                }
            }

            // Notification Settings Section
            item {
                SettingsSection(title = stringResource(R.string.settings_section_notifications)) {
                    SettingsSwitchItem(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.settings_enable_notifications_title),
                        subtitle = stringResource(R.string.settings_enable_notifications_subtitle),
                        checked = enableNotifications,
                        onCheckedChange = { enableNotifications = it },
                        enabled = false
                    )
                }
            }

            // About Section
            item {
                SettingsSection(title = stringResource(R.string.settings_section_about)) {
                    SettingsButtonItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.settings_about_title),
                        subtitle = stringResource(R.string.settings_about_subtitle),
                        onClick = { showAboutDialog = true }
                    )

                    SettingsButtonItem(
                        icon = Icons.Default.BugReport,
                        title = stringResource(R.string.settings_report_bug_title),
                        subtitle = stringResource(R.string.settings_report_bug_subtitle),
                        onClick = {},
                        enabled = false
                    )
                }
            }

            // Logout Section
            item {
                SettingsSection(title = stringResource(R.string.settings_section_account)) {
                    SettingsButtonItem(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = stringResource(R.string.settings_logout_title),
                        subtitle = stringResource(R.string.settings_logout_subtitle),
                        onClick = {
                            viewModel.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        textColor = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Clear credentials confirmation dialog
    if (showClearCredentialsDialog) {
        AlertDialog(
            onDismissRequest = { showClearCredentialsDialog = false },
            title = { Text(stringResource(R.string.settings_clear_credentials_dialog_title)) },
            text = { Text(stringResource(R.string.settings_clear_credentials_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearSavedCredentials()
                        enableAutoLogin = false
                        showClearCredentialsDialog = false
                    }
                ) {
                    Text(stringResource(R.string.settings_clear_button), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCredentialsDialog = false }) {
                    Text(stringResource(R.string.settings_cancel_button))
                }
            }
        )
    }

    // About dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.settings_about_dialog_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME))
                    Text(stringResource(R.string.settings_about_build, BuildConfig.VERSION_CODE))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_about_description))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_about_features_header))
                    Text(stringResource(R.string.settings_about_feature_auth))
                    Text(stringResource(R.string.settings_about_feature_monitoring))
                    Text(stringResource(R.string.settings_about_feature_lxc_vm))
                    Text(stringResource(R.string.settings_about_feature_storage_network))
                    Text(stringResource(R.string.settings_about_feature_user_task))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_about_built_with_header))
                    Text(stringResource(R.string.settings_about_built_compose))
                    Text(stringResource(R.string.settings_about_built_material))
                    Text(stringResource(R.string.settings_about_built_coroutines))
                    Text(stringResource(R.string.settings_about_built_retrofit))
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.settings_ok_button))
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            content()
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { 
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}

@Composable
fun SettingsSliderItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { 
            Column {
                Text(subtitle)
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    steps = steps,
                    enabled = enabled,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        leadingContent = { 
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    )
}

@Composable
fun SettingsButtonItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true
) {
    val effectiveColor = if (enabled) textColor else MaterialTheme.colorScheme.onSurfaceVariant
    ListItem(
        headlineContent = { 
            Text(
                text = title,
                color = effectiveColor
            )
        },
        supportingContent = { 
            Text(
                text = subtitle,
                color = effectiveColor.copy(alpha = 0.7f)
            )
        },
        leadingContent = { 
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = effectiveColor
            )
        },
        trailingContent = { 
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = effectiveColor.copy(alpha = 0.6f)
            )
        },
        modifier = Modifier.clickable(enabled = enabled) { onClick() }
    )
}
