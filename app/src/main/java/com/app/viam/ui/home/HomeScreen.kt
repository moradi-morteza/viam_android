package com.app.viam.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
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
import com.app.viam.data.model.Part
import com.app.viam.data.model.User
import com.app.viam.data.repository.PartRepository
import com.app.viam.data.repository.PersonnelRepository
import com.app.viam.ui.common.DrawerScreen
import com.app.viam.ui.common.MainScaffold
import com.app.viam.ui.developer.DeveloperScreen
import com.app.viam.ui.developer.DeveloperViewModel
import com.app.viam.ui.parts.PartFormScreen
import com.app.viam.ui.parts.PartFormViewModel
import com.app.viam.ui.parts.PartListScreen
import com.app.viam.ui.parts.PartListViewModel
import com.app.viam.ui.personnel.PersonnelListScreen
import com.app.viam.ui.personnel.PersonnelListViewModel
import com.app.viam.ui.personnel.StaffFormScreen
import com.app.viam.ui.personnel.StaffFormViewModel
import com.app.viam.ui.profile.ProfileScreen

private enum class PersonnelSubScreenType { LIST, CREATE, EDIT }
private enum class PartSubScreenType { LIST, CREATE, EDIT }

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    userPreferences: UserPreferences,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentScreen by rememberSaveable { mutableStateOf(DrawerScreen.HOME) }
    var personnelSubScreen by rememberSaveable { mutableStateOf(PersonnelSubScreenType.LIST) }
    var staffToEdit by remember { mutableStateOf<User?>(null) }
    var partSubScreen by rememberSaveable { mutableStateOf(PartSubScreenType.LIST) }
    var partToEdit by remember { mutableStateOf<Part?>(null) }

    BackHandler(
        enabled = personnelSubScreen != PersonnelSubScreenType.LIST ||
                partSubScreen != PartSubScreenType.LIST ||
                currentScreen != DrawerScreen.HOME
    ) {
        when {
            personnelSubScreen != PersonnelSubScreenType.LIST -> {
                personnelSubScreen = PersonnelSubScreenType.LIST
                staffToEdit = null
            }
            partSubScreen != PartSubScreenType.LIST -> {
                partSubScreen = PartSubScreenType.LIST
                partToEdit = null
            }
            currentScreen != DrawerScreen.HOME -> currentScreen = DrawerScreen.HOME
        }
    }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            viewModel.onLogoutNavigated()
            onLogout()
        }
    }

    LaunchedEffect(currentScreen) {
        if (currentScreen != DrawerScreen.PERSONNEL) {
            personnelSubScreen = PersonnelSubScreenType.LIST
            staffToEdit = null
        }
        if (currentScreen != DrawerScreen.PARTS) {
            partSubScreen = PartSubScreenType.LIST
            partToEdit = null
        }
    }

    val currentUser = uiState.user
    val canViewPersonnel = currentUser?.isAdmin() == true ||
            currentUser?.hasPermission("view-personnel") == true
    val canViewParts = currentUser?.isAdmin() == true ||
            currentUser?.hasPermission("view-parts") == true ||
            currentUser?.hasPermission("manage-parts") == true

    // Sub-form screens (have their own TopAppBar with back)
    val isInPersonnelForm = currentScreen == DrawerScreen.PERSONNEL &&
            personnelSubScreen != PersonnelSubScreenType.LIST
    val isInPartForm = currentScreen == DrawerScreen.PARTS &&
            partSubScreen != PartSubScreenType.LIST

    if (isInPersonnelForm) {
        val repo = PersonnelRepository()
        val editStaff = if (personnelSubScreen == PersonnelSubScreenType.EDIT) staffToEdit else null
        val formVm: StaffFormViewModel = viewModel(
            key = if (editStaff != null) "edit_staff_${editStaff.id}" else "create_staff",
            factory = StaffFormViewModel.Factory(repo, editStaff)
        )
        StaffFormScreen(
            viewModel = formVm,
            onSaveSuccess = { personnelSubScreen = PersonnelSubScreenType.LIST; staffToEdit = null },
            onBack = { personnelSubScreen = PersonnelSubScreenType.LIST; staffToEdit = null }
        )
        return
    }

    if (isInPartForm) {
        val repo = PartRepository()
        val editPart = if (partSubScreen == PartSubScreenType.EDIT) partToEdit else null
        val formVm: PartFormViewModel = viewModel(
            key = if (editPart != null) "edit_part_${editPart.id}" else "create_part",
            factory = PartFormViewModel.Factory(repo, editPart)
        )
        PartFormScreen(
            viewModel = formVm,
            onSaveSuccess = { partSubScreen = PartSubScreenType.LIST; partToEdit = null },
            onBack = { partSubScreen = PartSubScreenType.LIST; partToEdit = null }
        )
        return
    }

    val screenTitle = when {
        currentScreen == DrawerScreen.HOME -> stringResource(R.string.home_welcome)
        currentScreen == DrawerScreen.PROFILE -> stringResource(R.string.profile_title)
        currentScreen == DrawerScreen.DEVELOPER -> stringResource(R.string.developer_title)
        currentScreen == DrawerScreen.PERSONNEL -> stringResource(R.string.personnel_title)
        currentScreen == DrawerScreen.PARTS -> stringResource(R.string.parts_title)
        else -> stringResource(R.string.app_name)
    }

    MainScaffold(
        title = screenTitle,
        user = uiState.user,
        currentScreen = currentScreen,
        onNavigate = { currentScreen = it },
        onLogout = { viewModel.onLogoutConfirmed() },
        showPersonnel = canViewPersonnel,
        showParts = canViewParts
    ) { contentModifier ->
        when (currentScreen) {
            DrawerScreen.HOME -> HomeDashboard(uiState = uiState, modifier = contentModifier)
            DrawerScreen.PROFILE -> {
                uiState.user?.let { user ->
                    ProfileScreen(user = user, modifier = contentModifier)
                }
            }
            DrawerScreen.PERSONNEL -> {
                if (canViewPersonnel && currentUser != null) {
                    val repo = PersonnelRepository()
                    val listVm: PersonnelListViewModel = viewModel(
                        factory = PersonnelListViewModel.Factory(repo, currentUser)
                    )
                    PersonnelListScreen(
                        viewModel = listVm,
                        onNavigateToCreate = { personnelSubScreen = PersonnelSubScreenType.CREATE },
                        onNavigateToEdit = { staff ->
                            staffToEdit = staff
                            personnelSubScreen = PersonnelSubScreenType.EDIT
                        },
                        modifier = contentModifier
                    )
                }
            }
            DrawerScreen.PARTS -> {
                if (canViewParts && currentUser != null) {
                    val repo = PartRepository()
                    val listVm: PartListViewModel = viewModel(
                        factory = PartListViewModel.Factory(repo, currentUser)
                    )
                    PartListScreen(
                        viewModel = listVm,
                        onNavigateToCreate = { partSubScreen = PartSubScreenType.CREATE },
                        onNavigateToEdit = { part ->
                            partToEdit = part
                            partSubScreen = PartSubScreenType.EDIT
                        },
                        modifier = contentModifier
                    )
                }
            }
            DrawerScreen.DEVELOPER -> {
                if (BuildConfig.DEBUG) {
                    val devVm: DeveloperViewModel = viewModel(
                        factory = DeveloperViewModel.Factory(userPreferences)
                    )
                    DeveloperScreen(viewModel = devVm, modifier = contentModifier)
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
