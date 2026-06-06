package com.proxmoxmobile.presentation.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.proxmoxmobile.BuildConfig
import com.proxmoxmobile.R
import com.proxmoxmobile.data.model.ServerConfig
import com.proxmoxmobile.data.security.CertificateFingerprint
import com.proxmoxmobile.data.security.CredentialAuthMethod
import com.proxmoxmobile.data.security.SavedCredentials
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background

const val LOGIN_API_TOKEN_SWITCH_TAG = "login_api_token_switch"
const val LOGIN_CERTIFICATE_FINGERPRINT_TAG = "login_certificate_fingerprint_field"
const val LOGIN_CERTIFICATE_FINGERPRINT_SUPPORTING_TEXT_TAG = "login_certificate_fingerprint_supporting_text"
const val LOGIN_CONNECT_BUTTON_TAG = "login_connect_button"
const val LOGIN_HTTPS_SWITCH_TAG = "login_use_https_switch"
const val LOGIN_VERIFY_SSL_SWITCH_TAG = "login_verify_ssl_switch"
const val LOGIN_VERIFY_SSL_WARNING_TAG = "login_verify_ssl_warning"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onNavigateToDashboard: () -> Unit,
    allowInsecureTls: Boolean = BuildConfig.ALLOW_INSECURE_TLS
) {
    var host by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("8006") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var realm by rememberSaveable { mutableStateOf("pam") }
    var useHttps by rememberSaveable { mutableStateOf(true) }
    var verifySsl by rememberSaveable { mutableStateOf(true) }
    var certificateFingerprint by rememberSaveable { mutableStateOf("") }
    var saveCredentials by rememberSaveable { mutableStateOf(false) }
    var useApiToken by rememberSaveable { mutableStateOf(false) }
    var apiTokenId by rememberSaveable { mutableStateOf("") }
    var apiTokenSecret by remember { mutableStateOf("") }
    var hasLoadedSavedCredentials by remember { mutableStateOf(false) }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    // Load saved credentials on first load
    LaunchedEffect(Unit) {
        if (hasLoadedSavedCredentials) {
            return@LaunchedEffect
        }
        hasLoadedSavedCredentials = true
        val savedCredentials = viewModel.loadSavedCredentials()
        if (savedCredentials != null) {
            val hasDraft = host.isNotBlank() ||
                username.isNotBlank() ||
                password.isNotBlank() ||
                apiTokenId.isNotBlank() ||
                apiTokenSecret.isNotBlank()

            if (!hasDraft) {
                host = savedCredentials.host
                port = savedCredentials.port.toString()
                username = savedCredentials.username
                password = savedCredentials.password
                realm = savedCredentials.realm
                useHttps = savedCredentials.useHttps
                verifySsl = savedCredentials.verifySsl || !allowInsecureTls
                certificateFingerprint = savedCredentials.certificateFingerprint
                saveCredentials = true
                useApiToken = savedCredentials.authMethod == CredentialAuthMethod.API_TOKEN
                apiTokenId = savedCredentials.apiTokenId
                apiTokenSecret = savedCredentials.apiTokenSecret
            } else if (
                saveCredentials &&
                savedCredentialsMatchNonSecretLoginDraft(
                    savedCredentials = savedCredentials,
                    host = host,
                    port = port,
                    username = username,
                    realm = realm,
                    useHttps = useHttps,
                    verifySsl = verifySsl,
                    certificateFingerprint = certificateFingerprint,
                    useApiToken = useApiToken,
                    apiTokenId = apiTokenId,
                    allowInsecureTls = allowInsecureTls
                )
            ) {
                if (useApiToken && apiTokenSecret.isBlank()) {
                    apiTokenSecret = savedCredentials.apiTokenSecret
                } else if (!useApiToken && password.isBlank()) {
                    password = savedCredentials.password
                }
            }
        }
    }

    val normalizedCertificateFingerprint = CertificateFingerprint.normalize(certificateFingerprint)
    val hasInvalidCertificateFingerprint = certificateFingerprint.isNotBlank() && normalizedCertificateFingerprint == null

    val canSubmit = host.isNotBlank() &&
        username.isNotBlank() &&
        realm.isNotBlank() &&
        !hasInvalidCertificateFingerprint &&
        if (useApiToken) {
            apiTokenId.isNotBlank() && apiTokenSecret.isNotBlank()
        } else {
            password.isNotBlank()
        }
    
    // Navigate to dashboard when authenticated
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            onNavigateToDashboard()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Logo/Title Section
            Spacer(modifier = Modifier.height(20.dp))
            
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.login_app_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = stringResource(R.string.login_app_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Login Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.login_server_configuration),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Host and Port Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text(stringResource(R.string.login_host)) },
                            modifier = Modifier.weight(2f),
                            enabled = !isLoading,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text(stringResource(R.string.login_port)) },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                    
                    // Username and Realm Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text(stringResource(R.string.login_username)) },
                            modifier = Modifier.weight(2f),
                            enabled = !isLoading,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        
                        OutlinedTextField(
                            value = realm,
                            onValueChange = { realm = it },
                            label = { Text(stringResource(R.string.login_realm)) },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                    
                    // HTTPS Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.login_use_https),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = useHttps,
                            onCheckedChange = { useHttps = it },
                            enabled = !isLoading,
                            modifier = Modifier.testTag(LOGIN_HTTPS_SWITCH_TAG)
                        )
                    }

                    if (useHttps) {
                        val displayedVerifySsl = verifySsl || !allowInsecureTls
                        OutlinedTextField(
                            value = certificateFingerprint,
                            onValueChange = { certificateFingerprint = it },
                            label = { Text(stringResource(R.string.login_certificate_fingerprint)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(LOGIN_CERTIFICATE_FINGERPRINT_TAG),
                            enabled = !isLoading,
                            singleLine = true,
                            isError = hasInvalidCertificateFingerprint,
                            supportingText = {
                                Text(
                                    text = if (hasInvalidCertificateFingerprint) {
                                        stringResource(R.string.login_certificate_fingerprint_invalid)
                                    } else {
                                        stringResource(R.string.login_certificate_fingerprint_help)
                                    },
                                    modifier = Modifier.testTag(LOGIN_CERTIFICATE_FINGERPRINT_SUPPORTING_TEXT_TAG)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.login_verify_ssl),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (allowInsecureTls && !verifySsl) {
                                    Text(
                                        text = stringResource(R.string.login_verify_ssl_warning),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.testTag(LOGIN_VERIFY_SSL_WARNING_TAG)
                                    )
                                }
                            }
                            Switch(
                                checked = displayedVerifySsl,
                                onCheckedChange = { verifySsl = it },
                                enabled = !isLoading && allowInsecureTls,
                                modifier = Modifier.testTag(LOGIN_VERIFY_SSL_SWITCH_TAG)
                            )
                        }
                    }

                    // Authentication Method Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.login_use_api_token),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = useApiToken,
                            onCheckedChange = { useApiToken = it },
                            enabled = !isLoading,
                            modifier = Modifier.testTag(LOGIN_API_TOKEN_SWITCH_TAG)
                        )
                    }

                    if (useApiToken) {
                        OutlinedTextField(
                            value = apiTokenId,
                            onValueChange = { apiTokenId = it },
                            label = { Text(stringResource(R.string.login_api_token_id)) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        OutlinedTextField(
                            value = apiTokenSecret,
                            onValueChange = { apiTokenSecret = it },
                            label = { Text(stringResource(R.string.login_api_token_secret)) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    } else {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.login_password)) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                    
                    // Save Credentials Checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = saveCredentials,
                            onCheckedChange = { saveCredentials = it },
                            enabled = !isLoading
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.login_save_credentials),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Error Message
                    if (errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    
                    // Login Button
                    Button(
                        onClick = {
                            if (canSubmit) {
                                val apiToken = if (useApiToken) {
                                    "$username@$realm!$apiTokenId=$apiTokenSecret"
                                } else {
                                    null
                                }
                                val serverConfig = ServerConfig(
                                    host = host,
                                    port = port.toIntOrNull() ?: 8006,
                                    username = username,
                                    password = if (useApiToken) null else password,
                                    apiToken = apiToken,
                                    realm = realm,
                                    useHttps = useHttps,
                                    verifySsl = if (useHttps) verifySsl || !allowInsecureTls else false,
                                    certificateFingerprint = if (useHttps) normalizedCertificateFingerprint else null
                                )
                                
                                viewModel.authenticate(serverConfig) {
                                    if (saveCredentials) {
                                        viewModel.saveCredentials(
                                            serverConfig = serverConfig,
                                            password = if (useApiToken) null else password,
                                            saveCredentials = true,
                                            authMethod = if (useApiToken) {
                                                CredentialAuthMethod.API_TOKEN
                                            } else {
                                                CredentialAuthMethod.PASSWORD
                                            },
                                            apiTokenId = if (useApiToken) apiTokenId else null,
                                            apiTokenSecret = if (useApiToken) apiTokenSecret else null
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag(LOGIN_CONNECT_BUTTON_TAG),
                        enabled = canSubmit && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(R.string.login_connecting))
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.login_connect_button))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Footer
            Text(
                text = stringResource(R.string.login_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

internal fun savedCredentialsMatchNonSecretLoginDraft(
    savedCredentials: SavedCredentials,
    host: String,
    port: String,
    username: String,
    realm: String,
    useHttps: Boolean,
    verifySsl: Boolean,
    certificateFingerprint: String,
    useApiToken: Boolean,
    apiTokenId: String,
    allowInsecureTls: Boolean
): Boolean {
    val expectedUseApiToken = savedCredentials.authMethod == CredentialAuthMethod.API_TOKEN
    val expectedVerifySsl = savedCredentials.verifySsl || !allowInsecureTls

    return savedCredentials.host == host &&
        savedCredentials.port == port.toIntOrNull() &&
        savedCredentials.username == username &&
        savedCredentials.realm == realm &&
        savedCredentials.useHttps == useHttps &&
        (!useHttps || verifySsl == expectedVerifySsl) &&
        certificateFingerprintMatches(savedCredentials.certificateFingerprint, certificateFingerprint, useHttps) &&
        useApiToken == expectedUseApiToken &&
        (!expectedUseApiToken || savedCredentials.apiTokenId == apiTokenId)
}

private fun certificateFingerprintMatches(
    savedFingerprint: String,
    draftFingerprint: String,
    useHttps: Boolean
): Boolean {
    if (!useHttps) {
        return true
    }
    if (savedFingerprint.isBlank() && draftFingerprint.isBlank()) {
        return true
    }
    val normalizedSavedFingerprint = CertificateFingerprint.normalize(savedFingerprint)
    val normalizedDraftFingerprint = CertificateFingerprint.normalize(draftFingerprint)
    return normalizedSavedFingerprint != null &&
        normalizedSavedFingerprint == normalizedDraftFingerprint
}
