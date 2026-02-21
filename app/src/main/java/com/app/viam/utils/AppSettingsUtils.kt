package com.app.viam.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.app.viam.data.local.UserPreferences
import kotlin.collections.firstOrNull
import kotlin.collections.lastOrNull
import kotlin.text.ifBlank
import kotlin.text.isNullOrBlank
import kotlin.text.orEmpty
import kotlin.text.split

/**
 * Utility object for accessing app settings throughout the app
 */
object AppSettingsUtils {
    
    /**
     * Get current language from SharedPreferences (synchronous, for non-composables)
     * This uses SharedPreferences for immediate access
     */
    fun getCurrentLanguage(context: Context): String {
        return try {
            val sharedPrefs = context.getSharedPreferences("yekan_language", Context.MODE_PRIVATE)
            sharedPrefs.getString("language", "fa") ?: "fa"
        } catch (e: Exception) {
            "fa" // Default fallback
        }
    }
    
    /**
     * Get localized text based on current language
     */
    fun getLocalizedText(context: Context, persianText: String, englishText: String): String {
        return when (getCurrentLanguage(context)) {
            "fa" -> persianText.ifBlank { englishText }
            "en" -> englishText.ifBlank { persianText }
            else -> persianText.ifBlank { englishText }
        }
    }
    
    /**
     * Check if current language is RTL
     */
    fun isRTL(context: Context): Boolean {
        return getCurrentLanguage(context) == "fa" || getCurrentLanguage(context) == "ar"
    }
    
    /**
     * Get display name for objects with both Persian and English names
     */
    fun getDisplayName(
        context: Context,
        persianName: String?,
        englishName: String?
    ): String {
        val persian = persianName ?: ""
        val english = englishName ?: ""
        
        return when (getCurrentLanguage(context)) {
            "fa" -> persian.ifBlank { english }
            "en" -> english.ifBlank { persian }
            else -> persian.ifBlank { english }
        }
    }
}

@Composable
fun rememberCurrentLanguage(): String {
    return "fa"
}

/**
 * Composable function to get app lang-based currency display name using string resources
 */
@Composable
fun rememberCurrencyDisplayName(isoCode: String, splite: Boolean = true): String {
    val context = LocalContext.current
    val currentLanguage = rememberCurrentLanguage()
    
    return remember(isoCode, splite, currentLanguage) {
        val resourceId = context.resources.getIdentifier(isoCode, "string", context.packageName)
        if (resourceId != 0) {
            val full_name = context.getString(resourceId)
            if (splite) {
                when (currentLanguage) {
                    "fa" -> full_name.split(" ").firstOrNull().orEmpty()
                    "en" -> full_name.split(" ").lastOrNull().orEmpty()
                    else -> full_name.split(" ").firstOrNull().orEmpty()
                }
            } else {
                full_name
            }
        } else {
            isoCode // Fallback to ISO code if no string resource found
        }
    }
}

/**
 * Composable function to get app lang-based sub-currency display name using string resources
 */
@Composable
fun rememberSubCurrencyDisplayName(subCurrency: String?): String {
    val context = LocalContext.current
    
    return remember(subCurrency) {
        if (subCurrency.isNullOrBlank()) {
            ""
        } else {
            val resourceId = context.resources.getIdentifier(subCurrency, "string", context.packageName)
            if (resourceId != 0) {
                context.getString(resourceId)
            } else {
                subCurrency // Fallback to original name if no string resource found
            }
        }
    }
}

fun Modifier.appShadow(): Modifier = this.then(
    Modifier.shadow(
        elevation = 12.dp,
        shape = RectangleShape,
        clip = false
    ).zIndex(1f)
)