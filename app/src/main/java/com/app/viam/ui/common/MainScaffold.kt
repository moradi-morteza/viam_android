package com.app.viam.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Category
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

enum class DrawerScreen { HOME, PROFILE, PERSONNEL, PARTS, PART_CATEGORIES, WAREHOUSE, WAREHOUSE_STRUCTURE, DEVELOPER }

// Shape for drawer items — slightly rounded rectangle
private val DrawerItemShape = RoundedCornerShape(6.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    title: String,
    user: User?,
    currentScreen: DrawerScreen,
    onNavigate: (DrawerScreen) -> Unit,
    onLogout: () -> Unit,
    showPersonnel: Boolean = false,
    showParts: Boolean = false,
    showPartCategories: Boolean = false,
    showWarehouse: Boolean = false,
    showWarehouseStructure: Boolean = false,
    actions: @Composable () -> Unit = {},
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

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // Header — top padding
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
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
                    shape = DrawerItemShape,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_profile)) },
                    selected = currentScreen == DrawerScreen.PROFILE,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate(DrawerScreen.PROFILE)
                    },
                    shape = DrawerItemShape,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                if (showPersonnel) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.People, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_personnel)) },
                        selected = currentScreen == DrawerScreen.PERSONNEL,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(DrawerScreen.PERSONNEL)
                        },
                        shape = DrawerItemShape,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }

                if (showParts) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Inventory2, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_parts)) },
                        selected = currentScreen == DrawerScreen.PARTS,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(DrawerScreen.PARTS)
                        },
                        shape = DrawerItemShape,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }

                if (showPartCategories) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Category, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_part_categories)) },
                        selected = currentScreen == DrawerScreen.PART_CATEGORIES,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(DrawerScreen.PART_CATEGORIES)
                        },
                        shape = DrawerItemShape,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }

                if (showWarehouse) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Warehouse, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_warehouse)) },
                        selected = currentScreen == DrawerScreen.WAREHOUSE,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(DrawerScreen.WAREHOUSE)
                        },
                        shape = DrawerItemShape,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }

                if (showWarehouseStructure) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.AccountTree, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_warehouse_structure)) },
                        selected = currentScreen == DrawerScreen.WAREHOUSE_STRUCTURE,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(DrawerScreen.WAREHOUSE_STRUCTURE)
                        },
                        shape = DrawerItemShape,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }

                if (BuildConfig.DEBUG) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Code, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_developer)) },
                        selected = currentScreen == DrawerScreen.DEVELOPER,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(DrawerScreen.DEVELOPER)
                        },
                        shape = DrawerItemShape,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
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
                    shape = DrawerItemShape,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
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
                    },
                    actions = { actions() }
                )
            }
        ) { innerPadding ->
            content(Modifier.padding(innerPadding))
        }
    }
}
