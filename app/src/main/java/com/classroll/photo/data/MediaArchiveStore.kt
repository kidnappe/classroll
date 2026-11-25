package com.classroll.photo.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ArchiveRequest(
    val uri: Uri,
    val displayName: String,
    val course: String,
    val week: Int,
    val takenAt: LocalDateTime?,
    val sourceId: Long? = null,
)

data class ArchiveEntry(
    val course: String,
    val fileName: String,
    val sourceName: String,
    val copied: Boolean,
    val skipped: Boolean,
    val relativePath: String,
    val error: String? = null,
    val sourceId: Long? = null,
)

data class ArchiveResult(
    val term: String,
    val copied: Int,
    val skipped: Int,
    val failed: Int,
    val albums: Map<String, Int>,
    val entries: List<ArchiveEntry>,
) {
    val total: Int get() = copied + skipped + failed
}

data class ArchiveAlbum(
    val term: String,
    val course: String,
    val count: Int,
    val coverUri: String?,
    val relativePath: String,
)

data class ArchivePhoto(
    val uri: String,
    val displayName: String,
    val takenAt: LocalDateTime?,
)

/** 归档库里的一张照片，带学期、课程、教学周和日期，用于按周/按课程分组。 */
data class ArchiveItem(
    val term: String,
    val course: String,
    val week: Int,
    val date: java.time.LocalDate?,
    val uri: String,
    val fileName: String,
    val available: Boolean = true,
    val sessionLabel: String = "",
)

/** 通过系统媒体库把选中的照片复制到 Pictures/课程归档/<学期>/<课程>/。只复制，不移动、不删除原图。 */
class MediaArchiveStore(private val context: Context) {
    private val resolver = context.contentResolver
    private val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    suspend fun archive(
        term: String,
        requests: List<ArchiveRequest>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): ArchiveResult = withContext(Dispatchers.IO) {
        val safeTerm = sanitizeSegment(term).ifBlank { "未命名学期" }
        val ordered = requests.sortedWith(compareBy({ it.course }, { it.takenAt ?: LocalDateTime.MIN }))
        val existingByPath = mutableMapOf<String, MutableSet<String>>()
        val sequenceByCourse = mutableMapOf<String, Int>()
        val entries = mutableListOf<ArchiveEntry>()
        var copied = 0
        val skipped = 0
        var failed = 0

        ordered.forEachIndexed { index, request ->
            val course = sanitizeSegment(request.course).ifBlank { "未命名课程" }
            val relativePath = "$ROOT$safeTerm/$course/"
            val existing = existingByPath.getOrPut(relativePath) { queryNames(relativePath).toMutableSet() }
            val original = sanitizeFileName(request.displayName).ifBlank { "photo.jpg" }
            var sequence = (sequenceByCourse[course] ?: 0) + 1
            var fileName = buildName(request.week, request.takenAt, sequence, original)
            while (fileName in existing) {
                sequence++
                fileName = buildName(request.week, request.takenAt, sequence, original)
            }
            sequenceByCourse[course] = sequence
            val error = writeOne(request, relativePath, fileName)
            if (error == null) {
                copied++
                existing += fileName
                entries += ArchiveEntry(course, fileName, request.displayName, true, false, relativePath, sourceId = request.sourceId)
            } else {
                failed++
                entries += ArchiveEntry(course, fileName, request.displayName, false, false, relativePath, error, request.sourceId)
            }
            onProgress(index + 1, ordered.size)
        }

        ArchiveResult(
            term = safeTerm,
            copied = copied,
            skipped = skipped,
            failed = failed,
            albums = entries.filter { it.copied }.groupingBy { it.course }.eachCount(),
            entries = entries,
        )
    }

    suspend fun loadAlbums(): List<ArchiveAlbum> = withContext(Dispatchers.IO) {
        val grouped = LinkedHashMap<Pair<String, String>, MutableList<String>>()
        resolver.query(
            collection,
            arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.RELATIVE_PATH),
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("$ROOT%"),
            "${MediaStore.Images.Media.RELATIVE_PATH} ASC, ${MediaStore.Images.Media.DISPLAY_NAME} ASC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                val parsed = parseArchivePath(cursor.getString(pathColumn)) ?: continue
                val uri = ContentUris.withAppendedId(collection, cursor.getLong(idColumn)).toString()
                grouped.getOrPut(parsed) { mutableListOf() } += uri
            }
        }
        grouped.map { (key, uris) ->
            ArchiveAlbum(
                term = key.first,
                course = key.second,
                count = uris.size,
                coverUri = uris.firstOrNull(),
                relativePath = "$ROOT${key.first}/${key.second}/",
            )
        }.sortedWith(compareByDescending<ArchiveAlbum> { it.term }.thenBy { it.course })
    }

    suspend fun loadArchiveItems(): List<ArchiveItem> = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val items = mutableListOf<ArchiveItem>()
        resolver.query(
            collection,
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.RELATIVE_PATH,
                MediaStore.Images.Media.DATE_TAKEN,
            ),
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("$ROOT%"),
            "${MediaStore.Images.Media.RELATIVE_PATH} ASC, ${MediaStore.Images.Media.DISPLAY_NAME} ASC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val takenColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
            while (cursor.moveToNext()) {
                val parsed = parseArchivePath(cursor.getString(pathColumn)) ?: continue
                val name = cursor.getString(nameColumn) ?: continue
                val taken = if (takenColumn >= 0) cursor.getLong(takenColumn) else 0L
                val takenDate = taken.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                items += ArchiveItem(
                    term = parsed.first,
                    course = parsed.second,
                    week = parseWeek(name),
                    date = takenDate ?: parseFallbackDate(parsed.first, name),
                    uri = ContentUris.withAppendedId(collection, cursor.getLong(idColumn)).toString(),
                    fileName = name,
                )
            }
        }
        items
    }

    /** 当前媒体库里还存在的照片 ID，用来判断索引条目是否失效。 */
    suspend fun aliveImageIds(): Set<Long> = withContext(Dispatchers.IO) {
        val ids = mutableSetOf<Long>()
        resolver.query(collection, arrayOf(MediaStore.Images.Media._ID), null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) ids += cursor.getLong(idColumn)
        }
        ids
    }

    /** 删除某门课的归档副本；原图不动。返回删除成功的文件数。 */
    suspend fun deleteCourse(term: String, course: String): Int = withContext(Dispatchers.IO) {
        val relativePath = "$ROOT${sanitizeSegment(term)}/${sanitizeSegment(course)}/"
        val uris = mutableListOf<Uri>()
        resolver.query(
            collection,
            arrayOf(MediaStore.Images.Media._ID),
            "${MediaStore.Images.Media.RELATIVE_PATH} = ?",
            arrayOf(relativePath),
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                uris += ContentUris.withAppendedId(collection, cursor.getLong(idColumn))
            }
        }
        uris.count { uri -> runCatching { resolver.delete(uri, null, null) > 0 }.getOrDefault(false) }
    }

    suspend fun loadAlbumPhotos(term: String, course: String): List<ArchivePhoto> = withContext(Dispatchers.IO) {
        val relativePath = "$ROOT${sanitizeSegment(term)}/${sanitizeSegment(course)}/"
        val zone = ZoneId.systemDefault()
        val photos = mutableListOf<ArchivePhoto>()
        resolver.query(
            collection,
            arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_TAKEN),
            "${MediaStore.Images.Media.RELATIVE_PATH} = ?",
            arrayOf(relativePath),
            "${MediaStore.Images.Media.DISPLAY_NAME} ASC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val takenColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
            while (cursor.moveToNext()) {
                val taken = if (takenColumn >= 0) cursor.getLong(takenColumn) else 0L
                photos += ArchivePhoto(
                    uri = ContentUris.withAppendedId(collection, cursor.getLong(idColumn)).toString(),
                    displayName = cursor.getString(nameColumn) ?: "照片",
                    takenAt = taken.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() },
                )
            }
        }
        photos
    }

    private fun writeOne(request: ArchiveRequest, relativePath: String, fileName: String): String? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, resolver.getType(request.uri) ?: mimeFor(fileName))
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Images.Media.IS_PENDING, 1)
            request.takenAt?.let {
                put(MediaStore.Images.Media.DATE_TAKEN, it.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            }
        }
        val target = runCatching { resolver.insert(collection, values) }.getOrNull()
            ?: return "系统相册拒绝创建文件"
        return runCatching {
            val input = resolver.openInputStream(request.uri) ?: throw IOException("无法读取原图")
            input.use { source ->
                val output = resolver.openOutputStream(target) ?: throw IOException("无法写入归档文件")
                output.use { source.copyTo(it) }
            }
            resolver.update(target, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            null
        }.getOrElse { error ->
            runCatching { resolver.delete(target, null, null) }
            error.message ?: error.javaClass.simpleName
        }
    }

    private fun queryNames(relativePath: String): Set<String> {
        val names = mutableSetOf<String>()
        resolver.query(
            collection,
            arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
            "${MediaStore.Images.Media.RELATIVE_PATH} = ?",
            arrayOf(relativePath),
            null,
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) cursor.getString(nameColumn)?.let(names::add)
        }
        return names
    }

    private fun parseArchivePath(path: String?): Pair<String, String>? {
        if (path == null || !path.startsWith(ROOT)) return null
        val parts = path.removePrefix(ROOT).split('/').filter { it.isNotBlank() }
        if (parts.size < 2) return null
        return parts[0] to parts[1]
    }

    companion object {
        const val ROOT = "Pictures/课程归档/"

        fun sanitizeSegment(value: String): String = value
            .replace(Regex("[\\\\/:*?\"<>|\\r\\n\\t]"), "_")
            .trim()
            .trimEnd('.')

        fun sanitizeFileName(value: String): String = value
            .replace(Regex("[\\\\/:*?\"<>|\\r\\n\\t]"), "_")
            .trim()

        private fun parseWeek(name: String): Int =
            Regex("^W(\\d{1,2})_").find(name)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        private fun parseFallbackDate(term: String, name: String): java.time.LocalDate? {
            val match = Regex("^W\\d{1,2}_(\\d{2})(\\d{2})_").find(name) ?: return null
            val year = term.take(4).toIntOrNull() ?: return null
            val month = match.groupValues[1].toIntOrNull() ?: return null
            val day = match.groupValues[2].toIntOrNull() ?: return null
            val actualYear = if (month >= 7) year else year + 1
            return runCatching { java.time.LocalDate.of(actualYear, month, day) }.getOrNull()
        }

        private fun buildName(week: Int, takenAt: LocalDateTime?, sequence: Int, original: String): String {
            val monthDay = takenAt?.format(DateTimeFormatter.ofPattern("MMdd")) ?: "0000"
            val weekLabel = if (week > 0) "W%02d".format(week) else "W00"
            return "${weekLabel}_${monthDay}_%03d_%s".format(sequence, original)
        }

        private fun mimeFor(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "heic", "heif" -> "image/heic"
            "gif" -> "image/gif"
            else -> "image/jpeg"
        }
    }
}
