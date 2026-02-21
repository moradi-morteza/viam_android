package com.app.viam.ui.warehouse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.app.viam.R
import com.app.viam.data.model.TransactionRequest
import com.app.viam.ui.common.PersianDateTimePickerSheet

// Transaction types
private enum class TxType(val value: String) {
    IN("IN"), OUT("OUT"), ADJUST("ADJUST")
}

/**
 * Reusable bottom sheet for creating a warehouse transaction.
 *
 * @param boxLabel         Box code/label shown as subtitle under the title (e.g. "BOX-001").
 * @param currentQuantity  Current box quantity (shown as reference).
 * @param isLoading        Whether the submit call is in flight.
 * @param error            Server error message to display, null = no error.
 * @param onDismiss        Called when sheet is closed without submitting.
 * @param onSubmit         Called with the built [TransactionRequest] when user taps Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormSheet(
    boxLabel: String = "",
    boxLocation: String = "",
    currentQuantity: Double,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (TransactionRequest) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember { mutableStateOf(TxType.IN) }
    var amountText by remember { mutableStateOf("") }
    var newQtyText by remember { mutableStateOf(currentQuantity.toLong().toString()) }
    var reference by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    // Date/time — null means "now" (backend uses server time)
    var selectedDateIso by remember { mutableStateOf<String?>(null) }
    var selectedDateDisplay by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Live diff for ADJUST
    val adjustDiff: Long? = if (selectedType == TxType.ADJUST) {
        newQtyText.toLongOrNull()?.let { it - currentQuantity.toLong() }
    } else null

    val diffColor = when {
        adjustDiff == null -> MaterialTheme.colorScheme.onSurfaceVariant
        adjustDiff > 0 -> Color(0xFF2E7D32)
        adjustDiff < 0 -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .imePadding()
        ) {
            // Header
            Text(
                text = stringResource(R.string.transaction_new),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (boxLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = boxLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (boxLocation.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = boxLocation,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error banner
            AnimatedVisibility(
                visible = error != null,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    ErrorBanner(message = error ?: "")
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // --- Type selector ---
            Text(
                text = stringResource(R.string.transaction_type_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            TransactionTypeSelector(
                selected = selectedType,
                onSelect = { selectedType = it }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // --- Amount / New quantity ---
            if (selectedType != TxType.ADJUST) {
                // IN / OUT: simple integer amount
                Text(
                    text = stringResource(R.string.transaction_amount_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { v ->
                        // Only allow positive integers
                        if (v.all { it.isDigit() }) amountText = v
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        Text(
                            text = "${stringResource(R.string.transaction_current_stock)}: ${currentQuantity.toLong()}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            } else {
                // ADJUST: target quantity + live diff
                Text(
                    text = stringResource(R.string.transaction_new_quantity_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = newQtyText,
                    onValueChange = { v ->
                        if (v.all { it.isDigit() }) newQtyText = v
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        Text(
                            text = stringResource(R.string.transaction_adjust_hint),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Diff preview row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${stringResource(R.string.transaction_current_stock)}: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentQuantity.toLong().toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "→ ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (adjustDiff != null) {
                        val prefix = if (adjustDiff > 0) "+" else ""
                        Text(
                            text = "$prefix$adjustDiff",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = diffColor
                        )
                    } else {
                        Text(
                            text = "—",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Reference ---
            Text(
                text = stringResource(R.string.transaction_reference),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = reference,
                onValueChange = { reference = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        text = stringResource(R.string.transaction_reference_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Description ---
            Text(
                text = stringResource(R.string.transaction_description),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                placeholder = {
                    Text(
                        text = stringResource(R.string.transaction_description_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Date / Time (optional) ---
            Text(
                text = stringResource(R.string.transaction_date_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = if (selectedDateDisplay != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = selectedDateDisplay ?: stringResource(R.string.transaction_date_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedDateDisplay != null)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedDateDisplay != null) {
                        IconButton(
                            onClick = { selectedDateIso = null; selectedDateDisplay = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Action buttons ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    onClick = {
                        val req = buildRequest(
                            type = selectedType,
                            amountText = amountText,
                            newQtyText = newQtyText,
                            reference = reference.ifBlank { null },
                            description = description.ifBlank { null },
                            transactionDate = selectedDateIso
                        )
                        if (req != null) onSubmit(req)
                    },
                    modifier = Modifier.weight(2f),
                    enabled = !isLoading && isFormValid(selectedType, amountText, newQtyText)
                ) {
                    Text(
                        text = if (isLoading) stringResource(R.string.transaction_saving)
                               else stringResource(R.string.save)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        PersianDateTimePickerSheet(
            onDismiss = { showDatePicker = false },
            onConfirm = { iso, display ->
                selectedDateIso = iso
                selectedDateDisplay = display
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun TransactionTypeSelector(
    selected: TxType,
    onSelect: (TxType) -> Unit
) {
    val types = listOf(
        TxType.IN to stringResource(R.string.transaction_type_in),
        TxType.OUT to stringResource(R.string.transaction_type_out),
        TxType.ADJUST to stringResource(R.string.transaction_type_adjust)
    )
    val typeColors = mapOf(
        TxType.IN to Color(0xFF2E7D32),
        TxType.OUT to Color(0xFFC62828),
        TxType.ADJUST to Color(0xFFF57F17)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        types.forEach { (type, label) ->
            val color = typeColors[type] ?: MaterialTheme.colorScheme.primary
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (selected == type) FontWeight.Bold else FontWeight.Normal
                    )
                },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.15f),
                    selectedLabelColor = color,
                    selectedLeadingIconColor = color
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == type,
                    selectedBorderColor = color,
                    selectedBorderWidth = 1.5.dp
                )
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

private fun isFormValid(type: TxType, amountText: String, newQtyText: String): Boolean {
    return when (type) {
        TxType.IN, TxType.OUT -> amountText.toLongOrNull()?.let { it > 0 } == true
        TxType.ADJUST -> newQtyText.toLongOrNull()?.let { it >= 0 } == true
    }
}

private fun buildRequest(
    type: TxType,
    amountText: String,
    newQtyText: String,
    reference: String?,
    description: String?,
    transactionDate: String?
): TransactionRequest? {
    return when (type) {
        TxType.IN, TxType.OUT -> {
            val amount = amountText.toIntOrNull() ?: return null
            if (amount <= 0) return null
            TransactionRequest(
                type = type.value,
                amount = amount,
                reference = reference,
                description = description,
                transactionDate = transactionDate
            )
        }
        TxType.ADJUST -> {
            val qty = newQtyText.toIntOrNull() ?: return null
            TransactionRequest(
                type = type.value,
                newQuantity = qty,
                reference = reference,
                description = description,
                transactionDate = transactionDate
            )
        }
    }
}
