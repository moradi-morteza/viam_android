package com.app.viam.ui.personnel

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.viam.R
import com.app.viam.data.model.Permission
import com.app.viam.data.model.Role
import com.app.viam.ui.common.LtrFormField
import com.app.viam.ui.common.RtlFormField
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffFormScreen(
    viewModel: StaffFormViewModel,
    onSaveSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaveSuccess) {
        if (uiState.isSaveSuccess) {
            viewModel.onSaveNavigated()
            onSaveSuccess()
        }
    }

    val title = if (viewModel.isEditMode)
        stringResource(R.string.personnel_edit)
    else
        stringResource(R.string.personnel_create)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBackIos,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp)
        ) {
            // ── Basic info ──────────────────────────────────────────────
            RtlFormField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.profile_name),
                error = uiState.nameError,
                keyboardType = KeyboardType.Text,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            LtrFormField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                label = stringResource(R.string.profile_username),
                error = uiState.usernameError,
                keyboardType = KeyboardType.Text,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Password
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                if (viewModel.isEditMode)
                                    stringResource(R.string.personnel_password_optional)
                                else
                                    stringResource(R.string.password_label)
                            )
                        }
                    },
                    singleLine = true,
                    isError = uiState.passwordError != null,
                    supportingText = uiState.passwordError?.let { err ->
                        {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(err)
                            }
                        }
                    },
                    visualTransformation = if (uiState.isPasswordVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = viewModel::onTogglePasswordVisibility) {
                            Icon(
                                imageVector = if (uiState.isPasswordVisible)
                                    Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            LtrFormField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = stringResource(R.string.profile_email),
                error = uiState.emailError,
                keyboardType = KeyboardType.Email,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            LtrFormField(
                value = uiState.mobile,
                onValueChange = viewModel::onMobileChange,
                label = stringResource(R.string.profile_mobile),
                error = uiState.mobileError,
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            RtlFormField(
                value = uiState.address,
                onValueChange = viewModel::onAddressChange,
                label = stringResource(R.string.profile_address),
                error = null,
                keyboardType = KeyboardType.Text,
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ── Role dropdown ────────────────────────────────────────────
            RoleDropdown(
                roles = uiState.roles,
                selectedId = uiState.selectedRoleId,
                isLoading = uiState.isRolesLoading,
                onSelect = viewModel::onRoleSelected,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ── Permissions section ──────────────────────────────────────
            PermissionsSection(
                permissionsByCategory = uiState.permissionsByCategory,
                isLoading = uiState.isPermissionsLoading,
                isEnabled = { permId -> uiState.isPermissionEnabled(permId) },
                isFromRole = { permId -> uiState.isFromRole(permId) },
                onToggle = viewModel::onTogglePermission
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ── General error ────────────────────────────────────────────
            uiState.generalError?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Save button ──────────────────────────────────────────────
            Button(
                onClick = viewModel::onSaveClicked,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Role Dropdown
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleDropdown(
    roles: List<Role>,
    selectedId: Int?,
    isLoading: Boolean,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = if (selectedId == null) {
        stringResource(R.string.personnel_no_role)
    } else {
        roles.firstOrNull { it.id == selectedId }?.name
            ?: stringResource(R.string.personnel_no_role)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (!isLoading) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = if (isLoading) stringResource(R.string.loading) else selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.personnel_role)) },
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.personnel_no_role),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = { onSelect(null); expanded = false }
            )
            roles.forEach { role ->
                DropdownMenuItem(
                    text = { Text(role.name) },
                    onClick = { onSelect(role.id); expanded = false }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Permissions Section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PermissionsSection(
    permissionsByCategory: Map<String, List<Permission>>,
    isLoading: Boolean,
    isEnabled: (Int) -> Boolean,
    isFromRole: (Int) -> Boolean,
    onToggle: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.personnel_permissions),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }

        // Legend
        if (!isLoading && permissionsByCategory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(
                    color = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.personnel_from_role)
                )
                LegendDot(
                    color = Color(0xFF2E7D32),
                    label = stringResource(R.string.personnel_direct_permission)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            // loading handled by spinner in header
            return@Column
        }

        if (permissionsByCategory.isEmpty()) {
            Text(
                text = stringResource(R.string.profile_no_permissions),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        permissionsByCategory.entries.forEachIndexed { idx, (category, perms) ->
            if (idx > 0) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            // Category label
            Text(
                text = localizedCategory(category),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            perms.forEach { perm ->
                PermissionRow(
                    permission = perm,
                    checked = isEnabled(perm.id),
                    fromRole = isFromRole(perm.id),
                    onToggle = { onToggle(perm.id) }
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color)
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun PermissionRow(
    permission: Permission,
    checked: Boolean,
    fromRole: Boolean,
    onToggle: () -> Unit
) {
    val roleColor = MaterialTheme.colorScheme.primary
    val directColor = Color(0xFF2E7D32)

    val bgColor = when {
        fromRole -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        checked  -> Color(0xFF2E7D32).copy(alpha = 0.10f)
        else     -> Color.Transparent
    }
    val checkboxColor = when {
        fromRole -> CheckboxDefaults.colors(
            checkedColor = roleColor,
            disabledCheckedColor = roleColor.copy(alpha = 0.6f)
        )
        checked  -> CheckboxDefaults.colors(checkedColor = directColor)
        else     -> CheckboxDefaults.colors()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .then(
                if (bgColor != Color.Transparent)
                    Modifier
                        .border(
                            width = 1.dp,
                            color = if (fromRole) roleColor.copy(alpha = 0.4f)
                                    else directColor.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(6.dp)
                        )
                else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = if (fromRole) null else { _ -> onToggle() },
            enabled = !fromRole,
            colors = checkboxColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = permission.displayName ?: permission.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            if (fromRole) {
                Text(
                    text = stringResource(R.string.personnel_role_default),
                    style = MaterialTheme.typography.labelSmall,
                    color = roleColor
                )
            }
        }
    }
}

@Composable
private fun localizedCategory(category: String): String {
    return when (category) {
        "warehouse"  -> stringResource(R.string.perm_category_warehouse)
        "personnel"  -> stringResource(R.string.perm_category_personnel)
        "roles"      -> stringResource(R.string.perm_category_roles)
        "dashboard"  -> stringResource(R.string.perm_category_dashboard)
        else         -> category
    }
}
