package com.app.viam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.app.viam.data.local.UserPreferences
import com.app.viam.data.repository.AuthRepository
import com.app.viam.navigation.AppNavigation
import com.app.viam.ui.theme.ViamTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val userPreferences by lazy { UserPreferences(applicationContext) }
    private val authRepository by lazy { AuthRepository(userPreferences) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Load token from DataStore into the in-memory cache before any API call runs
        lifecycleScope.launch {
            authRepository.initializeTokenCache()
        }

        setContent {
            ViamTheme {
                AppNavigation(
                    userPreferences = userPreferences,
                    authRepository = authRepository
                )
            }
        }
    }
}
