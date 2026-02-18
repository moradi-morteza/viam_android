package com.app.viam.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.viam.BuildConfig
import com.app.viam.R
import com.app.viam.data.local.UserPreferences
import com.app.viam.ui.common.DrawerScreen
import com.app.viam.ui.common.MainScaffold
import com.app.viam.ui.developer.DeveloperScreen
import com.app.viam.ui.developer.DeveloperViewModel
import com.app.viam.ui.profile.ProfileScreen

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    userPreferences: UserPreferences,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentScreen by rememberSaveable { mutableStateOf(DrawerScreen.HOME) }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            viewModel.onLogoutNavigated()
            onLogout()
        }
    }

    val screenTitle = when (currentScreen) {
        DrawerScreen.HOME -> stringResource(R.string.home_welcome)
        DrawerScreen.PROFILE -> stringResource(R.string.profile_title)
        DrawerScreen.DEVELOPER -> stringResource(R.string.developer_title)
    }

    MainScaffold(
        title = screenTitle,
        user = uiState.user,
        currentScreen = currentScreen,
        onNavigate = { currentScreen = it },
        onLogout = { viewModel.onLogoutConfirmed() }
    ) { contentModifier ->
        when (currentScreen) {
            DrawerScreen.HOME -> HomeDashboard(
                uiState = uiState,
                modifier = contentModifier
            )
            DrawerScreen.PROFILE -> {
                uiState.user?.let { user ->
                    ProfileScreen(user = user, modifier = contentModifier)
                }
            }
            DrawerScreen.DEVELOPER -> {
                if (BuildConfig.DEBUG) {
                    val devViewModel: DeveloperViewModel = viewModel(
                        factory = DeveloperViewModel.Factory(userPreferences)
                    )
                    DeveloperScreen(viewModel = devViewModel, modifier = contentModifier)
                }
            }
        }
    }
}

@Composable
private fun HomeDashboard(
    uiState: HomeUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        uiState.user?.let { user ->
            Text(
                text = "${stringResource(R.string.home_welcome)}، ${user.name}",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (user.isAdmin()) stringResource(R.string.profile_role_admin)
                       else stringResource(R.string.profile_role_staff),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
