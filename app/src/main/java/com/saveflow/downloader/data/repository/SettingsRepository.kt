package com.saveflow.downloader.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.saveflow.downloader.data.api.ApiConfig
import com.saveflow.downloader.domain.models.AppSettings
import com.saveflow.downloader.domain.models.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val QUALITY = stringPreferencesKey("quality")
        val CLIPBOARD = booleanPreferencesKey("clipboard_detection")
        val THEME = stringPreferencesKey("theme")
        val FOLDER = stringPreferencesKey("folder")
        val LAST_CLIP = stringPreferencesKey("last_suggested_clip")
    }

    private val data: Flow<Preferences> = context.dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val settings: Flow<AppSettings> = data.map { p ->
        val defaults = AppSettings()
        AppSettings(
            quality = p[Keys.QUALITY]?.takeIf { it in ApiConfig.knownQualities } ?: defaults.quality,
            clipboardDetection = p[Keys.CLIPBOARD] ?: defaults.clipboardDetection,
            theme = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: defaults.theme,
            folder = p[Keys.FOLDER] ?: defaults.folder
        )
    }

    val lastSuggestedClip: Flow<String?> = data.map { it[Keys.LAST_CLIP] }

    suspend fun setQuality(value: String) = context.dataStore.edit { it[Keys.QUALITY] = value }
    suspend fun setClipboardDetection(value: Boolean) = context.dataStore.edit { it[Keys.CLIPBOARD] = value }
    suspend fun setTheme(value: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = value.name }
    suspend fun setFolder(value: String) = context.dataStore.edit { it[Keys.FOLDER] = value.trim() }
    suspend fun setLastSuggestedClip(value: String) = context.dataStore.edit { it[Keys.LAST_CLIP] = value }
}
