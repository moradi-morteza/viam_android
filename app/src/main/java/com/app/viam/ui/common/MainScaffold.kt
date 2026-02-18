package com.app.viam.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.app.viam.BuildConfig
import com.app.viam.R
import com.app.viam.data.model.User
import kotlinx.coroutines.launch

enum class DrawerScreen { HOME, PROFILE, PERSONNEL, DEVELOPER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    title: String,
    user: User?,
    currentScreen: DrawerScreen,
    onNavigate: (DrawerScreen) -> Unit,
    onLogout: () -> Unit,
    showPersonnel: Boolean = false,
    content: @Composable (Modifier) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showLogoutConfirm by remember { mutableStateOf(false) }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text = { Text(stringResource(R.string.logout_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    onLogout()
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Close drawer on back press when it is open
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // Header
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = user?.name ?: "",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = user?.username ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_home)) },
                    selected = currentScreen == DrawerScreen.HOME,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate(DrawerScreen.HOME)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_profile)) },
                    selected = currentScreen == DrawerScreen.PROFILE,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate(DrawerScreen.PROFILE)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Personnel item — only if user has view-personnel permission
                if (showPersonnel) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.People, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_personnel)) },
                        selected = currentScreen == DrawerScreen.PERSONNEL,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(DrawerScreen.PERSONNEL)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                // Developer item — only in debug builds
                if (BuildConfig.DEBUG) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Code, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_developer)) },
                        selected = currentScreen == DrawerScreen.DEVELOPER,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(DrawerScreen.DEVELOPER)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Filled.PowerSettingsNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    label = {
                        Text(
                            stringResource(R.string.nav_logout),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showLogoutConfirm = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "منو")
                        }
                    }
                )
            }
        ) { innerPadding ->
            content(Modifier.padding(innerPadding))
        }
    }
}
