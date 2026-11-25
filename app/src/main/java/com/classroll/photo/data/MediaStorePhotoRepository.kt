package com.classroll.photo.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.classroll.photo.model.PhotoTimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class MediaStorePhoto(
    val id: Long,
    val uri: String,
    val displayName: String,
    val takenAt: LocalDateTime?,
    val addedAt: LocalDateTime,
    val timeSource: PhotoTimeSource,
    val relativePath: String? = null,
)

class MediaStorePhotoRepository(private val context: Context) {
    suspend fun query(schedule: com.classroll.photo.model.CourseSchedule, now: LocalDateTime): List<MediaStorePhoto> = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val from = schedule.semesterStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = now.atZone(zone).toInstant().toEpochMilli()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.RELATIVE_PATH,
        )
        val selection = buildString {
            append("(${MediaStore.Images.Media.DATE_TAKEN} BETWEEN ? AND ?")
            append(" OR ((${MediaStore.Images.Media.DATE_TAKEN} IS NULL OR ${MediaStore.Images.Media.DATE_TAKEN} = 0)")
            append(" AND ${MediaStore.Images.Media.DATE_ADDED} BETWEEN ? AND ?))")
            append(" AND ${MediaStore.Images.Media.RELATIVE_PATH} NOT LIKE ?")
        }
        val args = arrayOf(from.toString(), to.toString(), (from / 1000).toString(), (to / 1000).toString(), "Pictures/课程归档/%")
        val result = mutableListOf<MediaStorePhoto>()
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val takenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val pathColumn = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val takenMillis = cursor.getLong(takenColumn)
                val addedSeconds = cursor.getLong(addedColumn)
                val taken = if (takenMillis > 0) Instant.ofEpochMilli(takenMillis).atZone(zone).toLocalDateTime() else null
                val added = Instant.ofEpochSecond(addedSeconds).atZone(zone).toLocalDateTime()
                result += MediaStorePhoto(
                    id = id,
                    uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString(),
                    displayName = cursor.getString(nameColumn) ?: "照片 $id",
                    takenAt = taken,
                    addedAt = added,
                    timeSource = if (taken != null) PhotoTimeSource.Taken else PhotoTimeSource.Added,
                    relativePath = if (pathColumn >= 0) cursor.getString(pathColumn) else null,
                )
            }
        }
        result
    }

    suspend fun selectedPhotoUris(uris: List<Uri>): List<MediaStorePhoto> = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        uris.mapIndexed { index, uri ->
            var displayName = uri.lastPathSegment ?: "照片 ${index + 1}"
            runCatching {
                context.contentResolver.query(
                    uri,
                    arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null,
                )?.use { cursor: Cursor ->
                    if (cursor.moveToFirst()) {
                        val nameColumn = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameColumn >= 0) cursor.getString(nameColumn)?.let { displayName = it }
                    }
                }
            }
            val mediaId = runCatching { ContentUris.parseId(uri) }.getOrNull()
            var takenAt: LocalDateTime? = null
            var addedAt = now
            if (mediaId != null) {
                runCatching {
                    val mediaUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId)
                    context.contentResolver.query(
                        mediaUri,
                        arrayOf(MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_ADDED),
                        null,
                        null,
                        null,
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val takenColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                            if (takenColumn >= 0 && !cursor.isNull(takenColumn)) {
                                val millis = cursor.getLong(takenColumn)
                                if (millis > 0) takenAt = Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()
                            }
                            val addedColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                            if (addedColumn >= 0 && !cursor.isNull(addedColumn)) {
                                addedAt = Instant.ofEpochSecond(cursor.getLong(addedColumn)).atZone(zone).toLocalDateTime()
                            }
                        }
                    }
                }
            }
            MediaStorePhoto(
                id = mediaId ?: -index.toLong() - 1,
                uri = uri.toString(),
                displayName = displayName,
                takenAt = takenAt,
                addedAt = addedAt,
                timeSource = PhotoTimeSource.Picker,
            )
        }
    }

    companion object {
        fun hasFullAccess(context: Context): Boolean {
            val permission = when {
                Build.VERSION.SDK_INT >= 33 -> Manifest.permission.READ_MEDIA_IMAGES
                else -> Manifest.permission.READ_EXTERNAL_STORAGE
            }
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) return true
            if (Build.VERSION.SDK_INT >= 34) {
                val visualSelected = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                if (visualSelected == PackageManager.PERMISSION_GRANTED) return false
            }
            return false
        }
    }
}
