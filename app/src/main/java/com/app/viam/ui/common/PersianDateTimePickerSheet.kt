package com.app.viam.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.viam.R
import com.app.viam.persiancalendar.calendar.CivilDate
import com.app.viam.persiancalendar.common.DatePicker
import com.app.viam.persiancalendar.common.NumberPicker
import com.app.viam.persiancalendar.entities.Calendar
import com.app.viam.persiancalendar.entities.Jdn
import com.app.viam.persiancalendar.global.yearMonthNameOfDate
import com.app.viam.persiancalendar.utils.formatNumber
import com.app.viam.persiancalendar.utils.performHapticFeedbackVirtualKey
import java.util.GregorianCalendar

/**
 * A bottom sheet that lets the user pick a Persian (Shamsi) date and time.
 * Calls [onConfirm] with an ISO-8601 string suitable for the API ("yyyy-MM-ddTHH:mm:ss").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersianDateTimePickerSheet(
    onDismiss: () -> Unit,
    onConfirm: (isoDateTime: String, displayText: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val view = LocalView.current

    // Initialise to current date/time
    val nowGregorian = GregorianCalendar()
    val todayJdn = Jdn(
        CivilDate(
            nowGregorian.get(GregorianCalendar.YEAR),
            nowGregorian.get(GregorianCalendar.MONTH) + 1,
            nowGregorian.get(GregorianCalendar.DAY_OF_MONTH)
        )
    )

    var selectedJdn by remember { mutableStateOf(todayJdn) }
    var selectedHour by remember { mutableIntStateOf(nowGregorian.get(GregorianCalendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(nowGregorian.get(GregorianCalendar.MINUTE)) }

    // Persian date display text
    val persianDate = selectedJdn.toPersianDate()
    val monthNames = yearMonthNameOfDate(persianDate)
    val persianDateText = "${formatNumber(persianDate.dayOfMonth)} ${monthNames[persianDate.month - 1]} ${formatNumber(persianDate.year)}"
    val timeText = "${formatNumber(selectedHour).padStart(2, '0')}:${formatNumber(selectedMinute).padStart(2, '0')}"

    // Build ISO string from Jdn + time
    fun buildIso(): String {
        val civil = selectedJdn.toCivilDate()
        val h = selectedHour.toString().padStart(2, '0')
        val m = selectedMinute.toString().padStart(2, '0')
        val mo = civil.month.toString().padStart(2, '0')
        val d = civil.dayOfMonth.toString().padStart(2, '0')
        return "${civil.year}-$mo-${d}T$h:$m:00"
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
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.transaction_date_label),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Date section ──────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.day),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            DatePicker(
                calendar = Calendar.SHAMSI,
                jdn = selectedJdn,
                setJdn = { selectedJdn = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Time section ──────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.time_picker_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour
                NumberPicker(
                    modifier = Modifier.weight(1f),
                    label = { formatNumber(it).padStart(2, '0') },
                    range = 0..23,
                    value = selectedHour,
                    onClickLabel = stringResource(R.string.hour),
                    onValueChange = {
                        selectedHour = it
                        view.performHapticFeedbackVirtualKey()
                    }
                )

                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Minute
                NumberPicker(
                    modifier = Modifier.weight(1f),
                    label = { formatNumber(it).padStart(2, '0') },
                    range = 0..59,
                    value = selectedMinute,
                    onClickLabel = stringResource(R.string.minute),
                    onValueChange = {
                        selectedMinute = it
                        view.performHapticFeedbackVirtualKey()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Preview ───────────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.transaction_date_label) + " " + stringResource(R.string.transaction_date_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$persianDateText   $timeText",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Buttons ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = { onConfirm(buildIso(), "$persianDateText - $timeText") },
                    modifier = Modifier.weight(2f)
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}
