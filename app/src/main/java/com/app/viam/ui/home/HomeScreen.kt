package com.app.viam.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.viam.BuildConfig
import com.app.viam.R
import com.app.viam.data.local.UserPreferences
import com.app.viam.data.model.Part
import com.app.viam.data.model.User
import com.app.viam.data.repository.PartRepository
import com.app.viam.data.repository.PersonnelRepository
import com.app.viam.data.repository.WarehouseRepository
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
import com.app.viam.ui.warehouse.BoxDetailScreen
import com.app.viam.ui.warehouse.BoxDetailViewModel
import com.app.viam.ui.warehouse.WarehouseListScreen
import com.app.viam.ui.warehouse.WarehouseListViewModel
import com.app.viam.ui.warehouse.WarehouseStructureScreen
import com.app.viam.ui.warehouse.WarehouseStructureViewModel

private enum class PersonnelSubScreenType { LIST, CREATE, EDIT }
private enum class PartSubScreenType { LIST, CREATE, EDIT }
private enum class WarehouseSubScreenType { LIST, DETAIL }

@OptIn(ExperimentalMaterial3Api::class)
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
    var warehouseSubScreen by rememberSaveable { mutableStateOf(WarehouseSubScreenType.LIST) }
    var selectedBoxId by rememberSaveable { mutableStateOf<Int?>(null) }

    BackHandler(
        enabled = personnelSubScreen != PersonnelSubScreenType.LIST ||
                partSubScreen != PartSubScreenType.LIST ||
                warehouseSubScreen != WarehouseSubScreenType.LIST ||
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
            warehouseSubScreen != WarehouseSubScreenType.LIST -> {
                warehouseSubScreen = WarehouseSubScreenType.LIST
                selectedBoxId = null
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
        if (currentScreen == DrawerScreen.HOME) {
            viewModel.refreshMe()
        }
        if (currentScreen != DrawerScreen.PERSONNEL) {
            personnelSubScreen = PersonnelSubScreenType.LIST
            staffToEdit = null
        }
        if (currentScreen != DrawerScreen.PARTS) {
            partSubScreen = PartSubScreenType.LIST
            partToEdit = null
        }
        if (currentScreen != DrawerScreen.WAREHOUSE) {
            warehouseSubScreen = WarehouseSubScreenType.LIST
            selectedBoxId = null
        }
    }

    val currentUser = uiState.user
    val canViewPersonnel = currentUser?.isAdmin() == true ||
            currentUser?.hasPermission("view-personnel") == true
    val canViewParts = currentUser?.isAdmin() == true ||
            currentUser?.hasPermission("view-parts") == true ||
            currentUser?.hasPermission("manage-parts") == true
    val canViewWarehouse = currentUser?.isAdmin() == true ||
            currentUser?.hasPermission("view-warehouse") == true
    val isAdmin = currentUser?.isAdmin() == true
    val canCreateZones = isAdmin || currentUser?.hasPermission("create-zones") == true
    val canEditZones = isAdmin || currentUser?.hasPermission("edit-zones") == true
    val canDeleteZones = isAdmin || currentUser?.hasPermission("delete-zones") == true
    val canCreateShelves = isAdmin || currentUser?.hasPermission("create-shelves") == true
    val canEditShelves = isAdmin || currentUser?.hasPermission("edit-shelves") == true
    val canDeleteShelves = isAdmin || currentUser?.hasPermission("delete-shelves") == true
    val canCreateRows = isAdmin || currentUser?.hasPermission("create-rows") == true
    val canEditRows = isAdmin || currentUser?.hasPermission("edit-rows") == true
    val canDeleteRows = isAdmin || currentUser?.hasPermission("delete-rows") == true
    val canManageStructure = canCreateZones || canEditZones || canDeleteZones ||
            canCreateShelves || canEditShelves || canDeleteShelves ||
            canCreateRows || canEditRows || canDeleteRows
    val canDeleteBoxes = isAdmin || currentUser?.hasPermission("delete-boxes") == true

    // Sub-form screens (have their own TopAppBar with back)
    val isInPersonnelForm = currentScreen == DrawerScreen.PERSONNEL &&
            personnelSubScreen != PersonnelSubScreenType.LIST
    val isInPartForm = currentScreen == DrawerScreen.PARTS &&
            partSubScreen != PartSubScreenType.LIST
    val isInBoxDetail = currentScreen == DrawerScreen.WAREHOUSE &&
            warehouseSubScreen == WarehouseSubScreenType.DETAIL

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

    if (isInBoxDetail && selectedBoxId != null) {
        val repo = WarehouseRepository()
        val detailVm: BoxDetailViewModel = viewModel(
            key = "box_detail_$selectedBoxId",
            factory = BoxDetailViewModel.Factory(repo, selectedBoxId!!)
        )
        BoxDetailScreen(
            viewModel = detailVm,
            canDelete = canDeleteBoxes,
            onBack = { warehouseSubScreen = WarehouseSubScreenType.LIST; selectedBoxId = null }
        )
        return
    }

    val screenTitle = when {
        currentScreen == DrawerScreen.HOME -> stringResource(R.string.home_welcome)
        currentScreen == DrawerScreen.PROFILE -> stringResource(R.string.profile_title)
        currentScreen == DrawerScreen.DEVELOPER -> stringResource(R.string.developer_title)
        currentScreen == DrawerScreen.PERSONNEL -> stringResource(R.string.personnel_title)
        currentScreen == DrawerScreen.PARTS -> stringResource(R.string.parts_title)
        currentScreen == DrawerScreen.WAREHOUSE -> stringResource(R.string.warehouse_title)
        currentScreen == DrawerScreen.WAREHOUSE_STRUCTURE -> stringResource(R.string.nav_warehouse_structure)
        else -> stringResource(R.string.app_name)
    }

    // Create warehouse ViewModel here so the TopAppBar action can reference it
    val warehouseVm: WarehouseListViewModel? = if (canViewWarehouse && currentUser != null) {
        viewModel(factory = WarehouseListViewModel.Factory(WarehouseRepository(), currentUser))
    } else null

    MainScaffold(
        title = screenTitle,
        user = uiState.user,
        currentScreen = currentScreen,
        onNavigate = { currentScreen = it },
        onLogout = { viewModel.onLogoutConfirmed() },
        showPersonnel = canViewPersonnel,
        showParts = canViewParts,
        showWarehouse = canViewWarehouse,
        showWarehouseStructure = canManageStructure,
        actions = {
            if (currentScreen == DrawerScreen.WAREHOUSE && warehouseVm != null) {
                IconButton(onClick = warehouseVm::onCreateClicked) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.warehouse_create_title))
                }
            }
        }
    ) { contentModifier ->
        when (currentScreen) {
            DrawerScreen.HOME -> DashboardScreen(
                uiState = uiState,
                onRefresh = viewModel::loadStats,
                modifier = contentModifier
            )
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
            DrawerScreen.WAREHOUSE -> {
                if (warehouseVm != null) {
                    WarehouseListScreen(
                        viewModel = warehouseVm,
                        onNavigateToDetail = { boxId ->
                            selectedBoxId = boxId
                            warehouseSubScreen = WarehouseSubScreenType.DETAIL
                        },
                        modifier = contentModifier
                    )
                }
            }
            DrawerScreen.WAREHOUSE_STRUCTURE -> {
                val repo = WarehouseRepository()
                val structureVm: WarehouseStructureViewModel = viewModel(
                    factory = WarehouseStructureViewModel.Factory(repo)
                )
                WarehouseStructureScreen(
                    viewModel = structureVm,
                    canCreateZones = canCreateZones,
                    canEditZones = canEditZones,
                    canDeleteZones = canDeleteZones,
                    canCreateShelves = canCreateShelves,
                    canEditShelves = canEditShelves,
                    canDeleteShelves = canDeleteShelves,
                    canCreateRows = canCreateRows,
                    canEditRows = canEditRows,
                    canDeleteRows = canDeleteRows,
                    modifier = contentModifier
                )
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
