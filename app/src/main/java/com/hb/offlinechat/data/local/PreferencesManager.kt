package com.hb.offlinechat.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hb_chat_prefs")

class PreferencesManager(private val context: Context) {
    private val darkThemeKey = booleanPreferencesKey("dark_theme")
    private val displayNameKey = stringPreferencesKey("display_name")

    val darkTheme: Flow<Boolean> = context.dataStore.data.map { it[darkThemeKey] ?: true }
    val displayName: Flow<String> = context.dataStore.data.map { it[displayNameKey] ?: "" }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[darkThemeKey] = enabled }
    }

    suspend fun setDisplayName(name: String) {
        context.dataStore.edit { it[displayNameKey] = name }
    }
}
