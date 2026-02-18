package com.app.viam.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.viam.data.local.UserPreferences
import com.app.viam.data.remote.SessionManager
import com.app.viam.data.repository.AuthRepository
import com.app.viam.ui.auth.LoginScreen
import com.app.viam.ui.auth.LoginViewModel
import com.app.viam.ui.home.HomeScreen
import com.app.viam.ui.home.HomeViewModel
import kotlinx.coroutines.flow.map

sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
}

private object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
}

@Composable
fun AppNavigation(
    userPreferences: UserPreferences,
    authRepository: AuthRepository
) {
    val navController = rememberNavController()

    val authState by userPreferences.tokenFlow
        .map { token ->
            if (token.isNullOrBlank()) AuthState.Unauthenticated else AuthState.Authenticated
        }
        .collectAsStateWithLifecycle(initialValue = AuthState.Loading)

    // Observe 401 session expiry and force logout
    val sessionExpired by SessionManager.sessionExpired
        .collectAsStateWithLifecycle(initialValue = Unit.let { null })

    LaunchedEffect(sessionExpired) {
        if (sessionExpired != null) {
            authRepository.logout()
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            LaunchedEffect(authState) {
                when (authState) {
                    is AuthState.Authenticated -> navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                    is AuthState.Unauthenticated -> navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                    is AuthState.Loading -> { /* wait */ }
                }
            }
        }

        composable(Routes.LOGIN) {
            val loginViewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.Factory(authRepository)
            )
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(authRepository)
            )
            HomeScreen(
                viewModel = homeViewModel,
                userPreferences = userPreferences,
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}
