package com.app.viam.persiancalendar.utils

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.app.viam.LOG_TAG
import com.app.viam.R
import com.app.viam.persiancalendar.common.DatePicker
import com.app.viam.persiancalendar.common.NumberPicker
import com.app.viam.persiancalendar.entities.Calendar
import com.app.viam.persiancalendar.entities.Jdn
import com.app.viam.persiancalendar.global.language
import com.app.viam.persiancalendar.global.yearMonthNameOfDate
import com.app.viam.utils.rememberCurrentLanguage
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

var enabledCalendars = listOf(Calendar.SHAMSI, Calendar.GREGORIAN, Calendar.ISLAMIC)
val mainCalendar get() = enabledCalendars.getOrNull(0) ?: Calendar.SHAMSI

object YekanColumnPaddings {
    val LazyColumnDefault = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = 8.dp,
        bottom = 72.dp
    )
}

// .split() turns an empty string into an array with an empty string which is undesirable
// for our use so this filter any non empty string after split, its name rhymes with .filterNotNull
fun String.splitFilterNotEmpty(delim: String) = this.split(delim).filter { it.isNotEmpty() }


inline val <T> T.debugAssertNotNull: T inline get() = checkNotNull(this)

val logException = fun(e: Throwable) { Log.e(LOG_TAG, "Handled Exception", e) }

fun debugLog(vararg message: Any?) {
    Log.d(LOG_TAG, message.joinToString(", "))
}

inline val Resources.isRtl get() = configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL || language.value.isLessKnownRtl
inline val Resources.isLandscape get() = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
inline val Resources.dp: Float get() = displayMetrics.density

fun Context.bringMarketPage(packageName: String = this.packageName) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
    }.onFailure(logException).onFailure {
        runCatching {
            val uri = "https://play.google.com/store/apps/details?id=$packageName".toUri()
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }.onFailure(logException)
    }
}

fun Bitmap.toPngByteArray(): ByteArray {
    val buffer = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.PNG, 100, buffer)
    return buffer.toByteArray()
}

//fun Bitmap.toPngBase64(): String =
//    "data:image/png;base64," + Base64.encodeToString(toByteArray(), Base64.DEFAULT)

private inline fun Context.saveAsFile(fileName: String, crossinline action: (File) -> Unit): Uri {
    return FileProvider.getUriForFile(
        applicationContext, "$packageName.provider", File(externalCacheDir, fileName).also(action)
    )
}


fun Context.shareText(text: String, chooserTitle: String) {
    runCatching {
        ShareCompat.IntentBuilder(this).setType("text/plain").setChooserTitle(chooserTitle)
            .setText(text).startChooser()
    }.onFailure(logException)
}

private fun Context.shareUriFile(uri: Uri, mime: String) {
    runCatching {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).also {
            it.type = mime
            it.putExtra(Intent.EXTRA_STREAM, uri)
        }, getString(R.string.share)))
    }.onFailure(logException)
}

fun Context.shareTextFile(text: String, fileName: String, mime: String) =
    shareUriFile(saveAsFile(fileName) { it.writeText(text) }, mime)

fun Context.shareBinaryFile(binary: ByteArray, fileName: String, mime: String) =
    shareUriFile(saveAsFile(fileName) { it.writeBytes(binary) }, mime)

fun createFlingDetector(
    context: Context, callback: (velocityX: Float, velocityY: Float) -> Boolean
): GestureDetector {
    class FlingListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
        ): Boolean = callback(velocityX, velocityY)
    }

    return GestureDetector(context, FlingListener())
}

/**
 * Similar to [androidx.compose.foundation.isSystemInDarkTheme] implementation but
 * for non composable contexts, in composable context, use the compose one.
 */
fun isSystemInDarkTheme(configuration: Configuration): Boolean =
    configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

// Android 14 has a grayscale dynamic colors mode and this is somehow a hack to check for that
// I guess there will be better ways to check for that in the future I guess but this does the trick
// Android 13, at least in Extension 5 emulator image, also provides such theme.
// https://stackoverflow.com/a/76272434
val Resources.isDynamicGrayscale: Boolean
    get() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val hsv = FloatArray(3)
        return listOf(
            android.R.color.system_accent1_500,
            android.R.color.system_accent2_500,
            android.R.color.system_accent3_500,
        ).all { Color.colorToHSV(getColor(it, null), hsv); hsv[1] < .25 }
    }

fun View.performHapticFeedbackVirtualKey() {
    debugLog("Preformed a haptic feedback virtual key")
    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
}

fun HapticFeedback.performLongPress() {
    debugLog("Preformed a haptic feedback long press")
    performHapticFeedback(HapticFeedbackType.LongPress)
}

enum class LineSide {
    LEFT, RIGHT, BOTH, NONE
}


@Composable
fun LinedText(
    text: String,
    lineSide: LineSide = LineSide.BOTH,
    lineColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
    lineThickness: Dp = 1.dp,
    textPadding: Dp = 8.dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (lineSide == LineSide.LEFT || lineSide == LineSide.BOTH) {
            Divider(
                color = lineColor,
                thickness = lineThickness,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = text,
            modifier = Modifier.padding(end = textPadding),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )

        if (lineSide == LineSide.RIGHT || lineSide == LineSide.BOTH) {
            Divider(
                color = lineColor,
                thickness = lineThickness,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Composable
fun MainDatePickerScreen() {

    // State for selected Jdn
    var selectedJdn by remember { mutableStateOf(Jdn.today()) }
    // Show the Persian date picker
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DatePicker(calendar = mainCalendar, jdn = selectedJdn, setJdn = { selectedJdn = it })
        Text(text = "Selected JDN: ${selectedJdn.value}")
    }
}

// YearMonthPickerScreen: lets user pick year and month, shows result as year/month
@Composable
fun YearMonthPickerScreen() {
    val calendar = mainCalendar
    val today = Jdn.today().on(calendar)
    var year by remember { mutableIntStateOf(today.year) }
    var month by remember { mutableIntStateOf(today.month) }
    val monthsInYear = calendar.getYearMonths(year)
    val monthNames = yearMonthNameOfDate(today)
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row {
            // Month picker
            NumberPicker(
                modifier = Modifier.weight(1f),
                label = { i -> monthNames[i - 1] + " / " + formatNumber(i) },
                range = 1..monthsInYear,
                value = month,
                onClickLabel = null,
                onValueChange = { month = it }
            )
            // Year picker
            NumberPicker(
                modifier = Modifier.weight(1f),
                label = { i -> formatNumber(i) },
                range = (year - 100)..(year + 100),
                value = year,
                onClickLabel = null,
                onValueChange = { year = it }
            )
        }
        // Show result as year/month
        Text(text = "Selected: ${formatNumber(year)}/${formatNumber(month)}")
    }
}


/**
 * Helper function to format date for chip display according to current language
 */
@Composable
fun formatDateForLanguageChip(timestamp: Long?): String {
    if (timestamp == null) return ""

    val currentLanguage = rememberCurrentLanguage()
    return if (currentLanguage == "fa") {
        // Use Persian calendar with shorter format for chips
        val calendar = GregorianCalendar().apply { timeInMillis = timestamp }
        val jdn = Jdn(
            com.app.viam.persiancalendar.calendar.CivilDate(
                calendar.get(GregorianCalendar.YEAR),
                calendar.get(GregorianCalendar.MONTH) + 1,
                calendar.get(GregorianCalendar.DAY_OF_MONTH)
            )
        )
        val persianDate = jdn.toPersianDate()
        val monthNames = yearMonthNameOfDate(persianDate)
        // Shorter format for chips
        "${formatNumber(persianDate.dayOfMonth)} ${monthNames[persianDate.month - 1].take(3)}"
    } else {
        // Use Gregorian calendar with shorter format
        SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}

