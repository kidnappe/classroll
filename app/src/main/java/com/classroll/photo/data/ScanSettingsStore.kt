package com.classroll.photo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

private val Context.scanSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "scan_settings")

class ScanSettingsStore(context: Context) {
    private val store = context.applicationContext.scanSettingsDataStore

    suspend fun loadIncludeSharedImages(): Boolean = store.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .first()[KEY_INCLUDE_SHARED] ?: false

    suspend fun saveIncludeSharedImages(value: Boolean) {
        store.edit { it[KEY_INCLUDE_SHARED] = value }
    }

    private companion object {
        val KEY_INCLUDE_SHARED = booleanPreferencesKey("include_shared_images")
    }
}
