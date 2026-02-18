package com.app.viam.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.app.viam.R
import com.app.viam.data.model.Permission
import com.app.viam.data.model.User

@Composable
fun ProfileScreen(
    user: User,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ProfileRow(label = stringResource(R.string.profile_name), value = user.name)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ProfileRow(label = stringResource(R.string.profile_username), value = user.username)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ProfileRow(
                    label = stringResource(R.string.profile_role),
                    value = if (user.isAdmin()) stringResource(R.string.profile_role_admin)
                            else stringResource(R.string.profile_role_staff)
                )
                if (!user.email.isNullOrBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileRow(label = stringResource(R.string.profile_email), value = user.email)
                }
                if (!user.mobile.isNullOrBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileRow(label = stringResource(R.string.profile_mobile), value = user.mobile)
                }
                if (!user.address.isNullOrBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileRow(label = stringResource(R.string.profile_address), value = user.address)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Permissions section
        Text(
            text = stringResource(R.string.profile_permissions),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (user.permissions.isEmpty()) {
            Text(
                text = stringResource(R.string.profile_no_permissions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // Group permissions by category
            val grouped = user.permissions.groupBy { it.category ?: "other" }

            grouped.forEach { (category, perms) ->
                PermissionCategorySection(category = category, permissions = perms)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PermissionCategorySection(category: String, permissions: List<Permission>) {
    val categoryLabel = when (category) {
        "warehouse" -> stringResource(R.string.perm_category_warehouse)
        "personnel" -> stringResource(R.string.perm_category_personnel)
        "roles" -> stringResource(R.string.perm_category_roles)
        "dashboard" -> stringResource(R.string.perm_category_dashboard)
        else -> stringResource(R.string.perm_category_other)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = categoryLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // Wrap chips in rows manually (FlowRow alternative using simple wrapping)
            val chunked = permissions.chunked(2)
            chunked.forEach { rowPerms ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowPerms.forEach { perm ->
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = perm.displayName ?: perm.name,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining space if odd number
                    if (rowPerms.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
