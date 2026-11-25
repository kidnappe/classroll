package com.classroll.photo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.themeSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

class ThemeSettingsStore(context: Context) {
    private val store = context.applicationContext.themeSettingsDataStore

    val skinId: Flow<String> = store.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[KEY_SKIN] ?: DEFAULT_SKIN }

    suspend fun saveSkin(id: String) {
        store.edit { it[KEY_SKIN] = id }
    }

    private companion object {
        const val DEFAULT_SKIN = "paper"
        val KEY_SKIN = stringPreferencesKey("skin_id")
    }
}
