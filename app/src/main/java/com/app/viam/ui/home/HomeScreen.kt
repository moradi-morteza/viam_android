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
import com.app.viam.data.model.User
import com.app.viam.data.repository.PersonnelRepository
import com.app.viam.ui.common.DrawerScreen
import com.app.viam.ui.common.MainScaffold
import com.app.viam.ui.developer.DeveloperScreen
import com.app.viam.ui.developer.DeveloperViewModel
import com.app.viam.ui.personnel.PersonnelListScreen
import com.app.viam.ui.personnel.PersonnelListViewModel
import com.app.viam.ui.personnel.StaffFormScreen
import com.app.viam.ui.personnel.StaffFormViewModel
import com.app.viam.ui.profile.ProfileScreen

// Sub-screen within the Personnel section
private enum class PersonnelSubScreenType { LIST, CREATE, EDIT }

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    userPreferences: UserPreferences,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentScreen by rememberSaveable { mutableStateOf(DrawerScreen.HOME) }
    var personnelSubScreenType by rememberSaveable { mutableStateOf(PersonnelSubScreenType.LIST) }
    var staffToEdit by remember { mutableStateOf<User?>(null) }

    // Back press handling:
    // - In sub-form (create/edit): go back to personnel list
    // - On any non-HOME screen: go back to HOME
    // - On HOME: do nothing (system default = close app)
    BackHandler(
        enabled = personnelSubScreenType != PersonnelSubScreenType.LIST ||
                currentScreen != DrawerScreen.HOME
    ) {
        when {
            personnelSubScreenType != PersonnelSubScreenType.LIST -> {
                personnelSubScreenType = PersonnelSubScreenType.LIST
                staffToEdit = null
            }
            currentScreen != DrawerScreen.HOME -> {
                currentScreen = DrawerScreen.HOME
            }
        }
    }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            viewModel.onLogoutNavigated()
            onLogout()
        }
    }

    // When switching away from personnel, reset sub-screen
    LaunchedEffect(currentScreen) {
        if (currentScreen != DrawerScreen.PERSONNEL) {
            personnelSubScreenType = PersonnelSubScreenType.LIST
            staffToEdit = null
        }
    }

    val currentUser = uiState.user
    val canViewPersonnel = currentUser?.isAdmin() == true ||
            currentUser?.hasPermission("view-personnel") == true

    val screenTitle = when {
        currentScreen == DrawerScreen.HOME -> stringResource(R.string.home_welcome)
        currentScreen == DrawerScreen.PROFILE -> stringResource(R.string.profile_title)
        currentScreen == DrawerScreen.DEVELOPER -> stringResource(R.string.developer_title)
        currentScreen == DrawerScreen.PERSONNEL && personnelSubScreenType == PersonnelSubScreenType.CREATE ->
            stringResource(R.string.personnel_create)
        currentScreen == DrawerScreen.PERSONNEL && personnelSubScreenType == PersonnelSubScreenType.EDIT ->
            stringResource(R.string.personnel_edit)
        else -> stringResource(R.string.personnel_title)
    }

    // Personnel sub-screens (create/edit) have their own TopAppBar with back button,
    // so we don't wrap them in MainScaffold
    val isInSubForm = currentScreen == DrawerScreen.PERSONNEL &&
            personnelSubScreenType != PersonnelSubScreenType.LIST

    if (isInSubForm) {
        val personnelRepository = PersonnelRepository()
        val editStaff = if (personnelSubScreenType == PersonnelSubScreenType.EDIT) staffToEdit else null
        val formVm: StaffFormViewModel = viewModel(
            key = if (editStaff != null) "edit_${editStaff.id}" else "create",
            factory = StaffFormViewModel.Factory(personnelRepository, editStaff)
        )
        StaffFormScreen(
            viewModel = formVm,
            onSaveSuccess = {
                personnelSubScreenType = PersonnelSubScreenType.LIST
                staffToEdit = null
            },
            onBack = {
                personnelSubScreenType = PersonnelSubScreenType.LIST
                staffToEdit = null
            }
        )
        return
    }

    MainScaffold(
        title = screenTitle,
        user = uiState.user,
        currentScreen = currentScreen,
        onNavigate = { currentScreen = it },
        onLogout = { viewModel.onLogoutConfirmed() },
        showPersonnel = canViewPersonnel
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
            DrawerScreen.PERSONNEL -> {
                if (canViewPersonnel && currentUser != null) {
                    val personnelRepository = PersonnelRepository()
                    val listVm: PersonnelListViewModel = viewModel(
                        factory = PersonnelListViewModel.Factory(personnelRepository, currentUser)
                    )
                    PersonnelListScreen(
                        viewModel = listVm,
                        onNavigateToCreate = { personnelSubScreenType = PersonnelSubScreenType.CREATE },
                        onNavigateToEdit = { staff ->
                            staffToEdit = staff
                            personnelSubScreenType = PersonnelSubScreenType.EDIT
                        },
                        modifier = contentModifier
                    )
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
