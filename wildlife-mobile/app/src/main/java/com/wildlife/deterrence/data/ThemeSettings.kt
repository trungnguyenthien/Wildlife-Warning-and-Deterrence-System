package com.wildlife.deterrence.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

object ThemeSettings {
    private var prefs: SharedPreferences? = null
    val themeMode = MutableStateFlow("system")

    fun init(context: Context) {
        prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        themeMode.value = prefs?.getString("theme_mode", "system") ?: "system"
    }

    fun setThemeMode(mode: String) {
        prefs?.edit()?.putString("theme_mode", mode)?.apply()
        themeMode.value = mode
    }
}
