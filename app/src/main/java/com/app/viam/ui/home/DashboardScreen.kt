package com.app.viam.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.viam.R
import com.app.viam.data.model.DashboardStats
import com.app.viam.data.model.LowStockBox
import com.app.viam.data.model.RecentTransaction
import java.time.Duration
import java.time.Instant

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun DashboardScreen(
    uiState: HomeUiState,
    onRefresh: () -> Unit,
    onQuickTransact: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Show pull-to-refresh spinner only when refreshing already-loaded data,
    // not on the initial load (to avoid double spinner with the center indicator).
    val isRefreshing = uiState.isLoadingStats && uiState.stats != null

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        if (uiState.isLoadingStats && uiState.stats == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@PullToRefreshBox
        }

        val stats = uiState.stats
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Welcome card
            item {
                WelcomeCard(name = uiState.user?.name ?: "")
            }

            // Quick transaction shortcut
            if (onQuickTransact != null) {
                item {
                    QuickTransactCard(onTap = onQuickTransact)
                }
            }

            // Quick stats row (personnel + warehouse top-level)
            if (stats?.personnel != null || stats?.warehouse != null || stats?.parts != null) {
                item {
                    QuickStatsRow(stats = stats)
                }
            }

            // Warehouse structure breakdown
            if (stats?.warehouse != null) {
                item {
                    WarehouseStructureCard(stats.warehouse.zones, stats.warehouse.shelves, stats.warehouse.rows, stats.warehouse.boxes)
                }
            }

            // Transaction stats
            if (stats?.transactions != null) {
                item {
                    TransactionStatsCard(stats)
                }
            }

            // Recent transactions
            if (!stats?.recentTransactions.isNullOrEmpty()) {
                item {
                    RecentTransactionsCard(stats!!.recentTransactions!!)
                }
            }

            // Low stock alerts
            if (!stats?.lowStock.isNullOrEmpty()) {
                item {
                    LowStockCard(
                        items = stats!!.lowStock!!,
                        emptyBoxes = stats.emptyBoxes ?: 0
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

// ─── Welcome card ────────────────────────────────────────────────────────────

@Composable
private fun WelcomeCard(name: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.dashboard_welcome, name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.dashboard_welcome_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }
    }
}

// ─── Quick stats row ─────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickStatsRow(stats: DashboardStats) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        stats.personnel?.let { p ->
            StatChip(
                value = p.total.toString(),
                label = stringResource(R.string.dashboard_staff),
                icon = Icons.Filled.People,
                color = Color(0xFF1976D2)
            )
            StatChip(
                value = p.admins.toString(),
                label = stringResource(R.string.dashboard_admins),
                icon = Icons.Filled.Shield,
                color = Color(0xFF7B1FA2)
            )
        }
        stats.warehouse?.let { w ->
            StatChip(
                value = w.boxes.toString(),
                label = stringResource(R.string.dashboard_boxes),
                icon = Icons.Filled.Warehouse,
                color = Color(0xFFE65100)
            )
        }
        stats.parts?.let { p ->
            StatChip(
                value = p.total.toString(),
                label = stringResource(R.string.dashboard_parts_total),
                icon = Icons.Filled.Inventory2,
                color = Color(0xFF00838F)
            )
            StatChip(
                value = p.totalStock.toString(),
                label = stringResource(R.string.dashboard_total_stock),
                icon = Icons.Filled.StackedBarChart,
                color = Color(0xFF303F9F)
            )
        }
    }
}

@Composable
private fun StatChip(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = color.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Warehouse structure ──────────────────────────────────────────────────────

@Composable
private fun WarehouseStructureCard(zones: Int, shelves: Int, rows: Int, boxes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.dashboard_warehouse_structure),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StructureStat(zones.toString(), stringResource(R.string.dashboard_zones), Color(0xFF2E7D32))
                StructureStat(shelves.toString(), stringResource(R.string.dashboard_shelves), Color(0xFF1565C0))
                StructureStat(rows.toString(), stringResource(R.string.dashboard_rows), Color(0xFF6A1B9A))
                StructureStat(boxes.toString(), stringResource(R.string.dashboard_boxes), Color(0xFFE65100))
            }
        }
    }
}

@Composable
private fun StructureStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Transaction stats ────────────────────────────────────────────────────────

@Composable
private fun TransactionStatsCard(stats: DashboardStats) {
    val tx = stats.transactions ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.dashboard_transactions_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Today / This week / This month
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PeriodStat(tx.today.toString(), stringResource(R.string.dashboard_today), Color(0xFF1565C0))
                PeriodStat(tx.thisWeek.toString(), stringResource(R.string.dashboard_this_week), Color(0xFF2E7D32))
                PeriodStat(tx.thisMonth.toString(), stringResource(R.string.dashboard_this_month), Color(0xFF6A1B9A))
            }

            // By type
            tx.byType?.let { byType ->
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.dashboard_by_type),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypeBadge(
                        label = stringResource(R.string.transaction_type_in),
                        count = byType.`in`,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                    TypeBadge(
                        label = stringResource(R.string.transaction_type_out),
                        count = byType.out,
                        color = Color(0xFFC62828),
                        modifier = Modifier.weight(1f)
                    )
                    TypeBadge(
                        label = stringResource(R.string.transaction_type_adjust),
                        count = byType.adjust,
                        color = Color(0xFFF57F17),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TypeBadge(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// ─── Recent transactions ──────────────────────────────────────────────────────

@Composable
private fun RecentTransactionsCard(transactions: List<RecentTransaction>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.dashboard_recent_transactions),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            transactions.forEachIndexed { index, tx ->
                RecentTransactionRow(tx)
                if (index < transactions.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionRow(tx: RecentTransaction) {
    val type = tx.type.lowercase()
    val color = when (type) {
        "in" -> Color(0xFF2E7D32)
        "out" -> Color(0xFFC62828)
        else -> Color(0xFFF57F17)
    }
    val sign = when (type) { "in" -> "+" ; "out" -> "−" ; else -> "~" }
    val amountText = "$sign${tx.amount.toLong()}"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Type circle
        Surface(
            shape = MaterialTheme.shapes.small,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = sign,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.partName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = "${tx.boxCode} · ${tx.userName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = amountText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = relativeTime(tx.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Low stock alerts ─────────────────────────────────────────────────────────

@Composable
private fun LowStockCard(items: List<LowStockBox>, emptyBoxes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFC62828).copy(alpha = 0.06f)
        ),
        border = CardDefaults.outlinedCardBorder().let {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC62828).copy(alpha = 0.3f))
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.dashboard_low_stock),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )
                if (emptyBoxes > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "($emptyBoxes ${stringResource(R.string.dashboard_empty_boxes)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            items.forEach { box ->
                LowStockRow(box)
                if (box != items.last()) HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun LowStockRow(box: LowStockBox) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = Color(0xFFC62828).copy(alpha = 0.12f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = box.quantity.toLong().toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = box.partName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = box.code,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = box.location,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

// ─── Quick transact card ──────────────────────────────────────────────────────

@Composable
private fun QuickTransactCard(onTap: () -> Unit) {
    Card(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(28.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.quick_transaction),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    modifier = Modifier.padding(top = 10.dp),
                    text = stringResource(R.string.quick_transaction_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun relativeTime(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val diff = Duration.between(instant, Instant.now())
        val mins = diff.toMinutes()
        val hours = diff.toHours()
        val days = diff.toDays()
        when {
            mins < 1 -> "الان"
            mins < 60 -> "$mins دقیقه پیش"
            hours < 24 -> "$hours ساعت پیش"
            days < 7 -> "$days روز پیش"
            else -> isoString.take(10)
        }
    } catch (_: Exception) {
        isoString.take(10)
    }
}
