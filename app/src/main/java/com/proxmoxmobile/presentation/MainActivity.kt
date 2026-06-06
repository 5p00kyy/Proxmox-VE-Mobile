package com.proxmoxmobile.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.proxmoxmobile.BuildConfig
import com.proxmoxmobile.presentation.navigation.ProxmoxNavHost
import com.proxmoxmobile.presentation.theme.ProxmoxTheme
import com.proxmoxmobile.presentation.viewmodel.MainViewModel
import com.proxmoxmobile.data.model.ServerConfig
import com.proxmoxmobile.data.security.CredentialAuthMethod

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        setContent {
            val navController = rememberNavController()
            val viewModel = remember { MainViewModel() }
            
            // Initialize ViewModel with context for SharedPreferences
            LaunchedEffect(Unit) {
                viewModel.initialize(this@MainActivity)
            }
            
            // Check for saved credentials and auto-login
            LaunchedEffect(Unit) {
                val savedCredentials = viewModel.loadSavedCredentials()
                if (savedCredentials != null) {
                    val verifySsl = savedCredentials.verifySsl || !BuildConfig.DEBUG
                    // Convert SavedCredentials to ServerConfig for auto-login
                    val serverConfig = ServerConfig(
                        host = savedCredentials.host,
                        port = savedCredentials.port,
                        username = savedCredentials.username,
                        password = if (savedCredentials.authMethod == CredentialAuthMethod.API_TOKEN) {
                            null
                        } else {
                            savedCredentials.password
                        },
                        apiToken = if (savedCredentials.authMethod == CredentialAuthMethod.API_TOKEN) {
                            "${savedCredentials.username}@${savedCredentials.realm}!${savedCredentials.apiTokenId}=${savedCredentials.apiTokenSecret}"
                        } else {
                            null
                        },
                        realm = savedCredentials.realm,
                        useHttps = savedCredentials.useHttps,
                        verifySsl = verifySsl,
                        certificateFingerprint = savedCredentials.certificateFingerprint.takeIf { it.isNotBlank() }
                    )
                    viewModel.setCurrentServer(serverConfig)
                }
            }
            
            ProxmoxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ProxmoxNavHost(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
