package com.example.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val titleArabic: String) {
    SYSTEM("تلقائي النظام"),
    LIGHT("فاتح"),
    DARK("داكن")
}

class ThemeManager(context: Context) {
    private val prefs = context.getSharedPreferences("sori_theme_preferences", Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(loadSavedTheme())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private fun loadSavedTheme(): AppThemeMode {
        val savedName = prefs.getString("selected_theme_mode", AppThemeMode.SYSTEM.name)
        return try {
            AppThemeMode.valueOf(savedName ?: AppThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("selected_theme_mode", mode.name).apply()
    }

    @Composable
    fun isDarkThemeActive(): Boolean {
        return when (_themeMode.value) {
            AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK -> true
        }
    }
}
