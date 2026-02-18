package com.app.viam.ui.developer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.viam.data.local.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeveloperUiState(
    val entries: Map<String, String> = emptyMap()
)

class DeveloperViewModel(private val userPreferences: UserPreferences) : ViewModel() {

    private val _uiState = MutableStateFlow(DeveloperUiState())
    val uiState: StateFlow<DeveloperUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                userPreferences.tokenFlow,
                userPreferences.userFlow
            ) { token, user ->
                buildMap {
                    put("auth_token", token ?: "(نشست فعال نیست)")
                    put("user_json", if (user != null) {
                        buildString {
                            appendLine("{")
                            appendLine("  \"id\": ${user.id},")
                            appendLine("  \"name\": \"${user.name}\",")
                            appendLine("  \"username\": \"${user.username}\",")
                            appendLine("  \"role\": \"${user.role}\",")
                            appendLine("  \"email\": ${user.email?.let { "\"$it\"" } ?: "null"},")
                            appendLine("  \"mobile\": ${user.mobile?.let { "\"$it\"" } ?: "null"},")
                            appendLine("  \"address\": ${user.address?.let { "\"$it\"" } ?: "null"},")
                            appendLine("  \"avatar\": ${user.avatar?.let { "\"$it\"" } ?: "null"},")
                            appendLine("  \"permissions\": [${user.permissions.size} مورد]")
                            append("}")
                        }
                    } else {
                        "(کاربری ذخیره نشده)"
                    })
                    put("permissions_count", (user?.permissions?.size ?: 0).toString())
                    put("permissions_list", user?.permissions?.joinToString("\n") {
                        "• ${it.name} [${it.category ?: "?"}]"
                    } ?: "(خالی)")
                }
            }.collect { entries ->
                _uiState.update { it.copy(entries = entries) }
            }
        }
    }

    class Factory(private val userPreferences: UserPreferences) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DeveloperViewModel(userPreferences) as T
    }
}
