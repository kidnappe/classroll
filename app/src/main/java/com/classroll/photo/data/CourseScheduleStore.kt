package com.classroll.photo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.classroll.photo.model.CourseSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

private val Context.courseScheduleDataStore: DataStore<Preferences> by androidx.datastore.preferences.preferencesDataStore(
    name = "course_schedule",
)

class CourseScheduleStore(context: Context) {
    private val store = context.applicationContext.courseScheduleDataStore

    suspend fun load(): CourseSchedule? {
        val raw = store.data
            .catch { error ->
                if (error is IOException) emit(emptyPreferences()) else throw error
            }
            .first()[KEY]
            ?: return null
        return runCatching { CourseScheduleJson.parseSchedule(raw) }
            .onFailure { clear() }
            .getOrNull()
    }

    suspend fun save(schedule: CourseSchedule) {
        store.edit { it[KEY] = CourseScheduleJson.serializeSchedule(schedule) }
    }

    suspend fun clear() {
        store.edit { it.remove(KEY) }
    }

    private companion object {
        val KEY = stringPreferencesKey("schedule_json")
    }
}
