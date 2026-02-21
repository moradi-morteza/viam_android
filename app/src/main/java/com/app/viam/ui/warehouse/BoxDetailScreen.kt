package com.app.viam.ui.warehouse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.icu.text.DateFormat
import android.icu.util.ULocale
import android.icu.util.Calendar
import com.app.viam.R
import com.app.viam.data.model.BoxTransaction
import java.time.Instant
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxDetailScreen(
    viewModel: BoxDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorDismissed()
        }
    }

    val successMsg = stringResource(R.string.transaction_success)
    LaunchedEffect(uiState.transactionSuccess) {
        if (uiState.transactionSuccess) {
            snackbarHostState.showSnackbar(successMsg)
            viewModel.onTransactionSuccessConsumed()
        }
    }

    // Transaction form sheet
    val currentBox = uiState.box
    if (uiState.showTransactionSheet && currentBox != null) {
        TransactionFormSheet(
            currentQuantity = currentBox.quantity,
            isLoading = uiState.isSubmittingTransaction,
            error = uiState.transactionError,
            onDismiss = { viewModel.onTransactionSheetDismissed() },
            onSubmit = { req -> viewModel.submitTransaction(req) }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.box != null) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.onAddTransactionClicked() },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.transaction_new)) }
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    val code = uiState.box?.code
                    Text(
                        if (code != null) "${stringResource(R.string.warehouse_box_prefix)} $code"
                        else stringResource(R.string.warehouse_box_code)
                    )
                },
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
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.box == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.warehouse_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                val box = uiState.box!!

                // Build location breadcrumb
                val locationParts = mutableListOf<String>()
                box.row?.shelf?.zone?.name?.let { locationParts.add(it) }
                box.row?.shelf?.name?.let { locationParts.add(it) }
                box.row?.name?.let { locationParts.add(it) }
                val locationText = locationParts.joinToString(" › ")

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Box info card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${stringResource(R.string.warehouse_box_prefix)} ${box.code}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            DetailRow(
                                label = stringResource(R.string.warehouse_quantity),
                                value = if (box.quantity == box.quantity.toLong().toDouble())
                                            box.quantity.toLong().toString()
                                        else box.quantity.toString(),
                                valueColor = if (box.quantity > 0) MaterialTheme.colorScheme.primary
                                             else MaterialTheme.colorScheme.error
                            )

                            if (locationText.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                DetailRow(
                                    label = stringResource(R.string.warehouse_location),
                                    value = locationText
                                )
                            }

                            if (box.part != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                DetailRow(
                                    label = stringResource(R.string.parts_name),
                                    value = box.part.name
                                )
                                if (!box.part.sku.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    DetailRow(
                                        label = stringResource(R.string.parts_sku),
                                        value = box.part.sku
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                                DetailRow(
                                    label = stringResource(R.string.parts_name),
                                    value = stringResource(R.string.warehouse_no_part)
                                )
                            }

                            if (!box.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                DetailRow(
                                    label = stringResource(R.string.parts_description),
                                    value = box.description
                                )
                            }
                        }
                    }

                    // Transactions section
                    Text(
                        text = stringResource(R.string.warehouse_transactions),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    val transactions = box.transactions
                    if (transactions.isNullOrEmpty()) {
                        Text(
                            text = stringResource(R.string.warehouse_no_transactions),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            transactions.forEach { tx ->
                                TransactionCard(tx)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatPersianDateTime(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val date = Date.from(instant)
        val persianLocale = ULocale("fa_IR@calendar=persian")
        val cal = Calendar.getInstance(persianLocale)
        cal.time = date
        val fmt = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT,
            persianLocale
        )
        fmt.format(date)
    } catch (_: Exception) {
        isoString.take(16).replace("T", " ")
    }
}

@Composable
private fun TransactionCard(tx: BoxTransaction) {
    val type = tx.type.lowercase()
    val typeLabel = when (type) {
        "in" -> stringResource(R.string.transaction_type_in)
        "out" -> stringResource(R.string.transaction_type_out)
        "adjust" -> stringResource(R.string.transaction_type_adjust)
        else -> tx.type
    }
    val typeColor = when (type) {
        "in" -> Color(0xFF2E7D32)
        "out" -> Color(0xFFC62828)
        else -> Color(0xFFF57F17)
    }
    val amountPrefix = when (type) {
        "in" -> "+"
        "out" -> ""
        else -> ""
    }
    val amountText = tx.amount.toLong().toString()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = typeColor.copy(alpha = 0.07f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top row: type badge + amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$amountText$amountPrefix",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = typeColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = typeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = typeColor.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            // Doer (user who made the transaction)
            if (tx.user != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${stringResource(R.string.transaction_doer)}: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = tx.user.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Description
            if (!tx.description.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "${stringResource(R.string.transaction_description)}: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = tx.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Reference
            if (!tx.reference.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${stringResource(R.string.transaction_reference)}: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = tx.reference,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Balance after + date  (same line, opposite ends)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(R.string.transaction_balance_after)}: ${tx.balanceAfter.toLong()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatPersianDateTime(tx.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
