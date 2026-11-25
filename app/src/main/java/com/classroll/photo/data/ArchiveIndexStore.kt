package com.classroll.photo.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

data class ArchiveIndexEntry(
    val sourceId: Long,
    val uri: String,
    val displayName: String,
    val term: String,
    val course: String,
    val week: Int,
    /** yyyy-MM-dd；解析不到时为空。 */
    val date: String,
    val sessionLabel: String,
    val addedAt: String,
)

/**
 * 归档索引：只记录“哪张原图归到哪门课/哪一周”，不再复制照片。
 * 存在 App 私有目录 files/archive-index.json，可整体导出。
 */
class ArchiveIndexStore(context: Context) {
    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "archive-index.json")

    suspend fun load(): List<ArchiveIndexEntry> = withContext(Dispatchers.IO) { read() }

    suspend fun archivedSourceIds(): Set<Long> = withContext(Dispatchers.IO) {
        read().mapTo(mutableSetOf()) { it.sourceId }
    }

    suspend fun addAll(entries: List<ArchiveIndexEntry>) = withContext(Dispatchers.IO) {
        if (entries.isEmpty()) return@withContext
        val merged = LinkedHashMap<Long, ArchiveIndexEntry>()
        read().forEach { merged[it.sourceId] = it }
        entries.forEach { merged[it.sourceId] = it }
        write(merged.values.toList())
    }

    suspend fun removeByUris(uris: Set<String>) = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext
        write(read().filterNot { it.uri in uris })
    }

    suspend fun removeCourse(term: String, course: String) = withContext(Dispatchers.IO) {
        write(read().filterNot { it.term == term && it.course == course })
    }

    suspend fun clear() = withContext(Dispatchers.IO) { write(emptyList()) }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) { toJson(read()) }

    private fun read(): List<ArchiveIndexEntry> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val text = file.readText().trim()
            if (text.isEmpty()) return emptyList()
            val array = if (text.startsWith("{")) {
                JSONObject(text).optJSONArray("items") ?: JSONArray()
            } else {
                JSONArray(text)
            }
            (0 until array.length()).mapNotNull { index -> parseItem(array.optJSONObject(index)) }
        }.getOrDefault(emptyList())
    }

    private fun parseItem(item: JSONObject?): ArchiveIndexEntry? {
        if (item == null) return null
        val sourceId = item.optLong("sourceId", 0)
        if (sourceId <= 0) return null
        val legacyName = item.optString("fileName")
        val displayName = item.optString("displayName").ifBlank { legacyName.ifBlank { "IMG_$sourceId" } }
        val uri = item.optString("uri").ifBlank {
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, sourceId).toString()
        }
        val term = item.optString("term")
        val week = item.optInt("week", 0).takeIf { it > 0 } ?: parseWeek(legacyName)
        val date = item.optString("date").ifBlank { parseDate(legacyName, term) }
        return ArchiveIndexEntry(
            sourceId = sourceId,
            uri = uri,
            displayName = displayName,
            term = term,
            course = item.optString("course"),
            week = week,
            date = date,
            sessionLabel = item.optString("sessionLabel"),
            addedAt = item.optString("addedAt"),
        )
    }

    private fun write(entries: List<ArchiveIndexEntry>) {
        runCatching {
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(toJson(entries))
            if (!temp.renameTo(file)) {
                file.writeText(toJson(entries))
                temp.delete()
            }
        }
    }

    private fun toJson(entries: List<ArchiveIndexEntry>): String {
        val items = JSONArray()
        entries.forEach { entry ->
            items.put(
                JSONObject()
                    .put("sourceId", entry.sourceId)
                    .put("uri", entry.uri)
                    .put("displayName", entry.displayName)
                    .put("term", entry.term)
                    .put("course", entry.course)
                    .put("week", entry.week)
                    .put("date", entry.date)
                    .put("sessionLabel", entry.sessionLabel)
                    .put("addedAt", entry.addedAt),
            )
        }
        return JSONObject().put("version", 2).put("items", items).toString(2)
    }

    private fun parseWeek(name: String): Int =
        Regex("^W(\\d{1,2})_").find(name)?.groupValues?.get(1)?.toIntOrNull() ?: 0

    private fun parseDate(name: String, term: String): String {
        val match = Regex("^W\\d{1,2}_(\\d{2})(\\d{2})_").find(name) ?: return ""
        val year = term.take(4).toIntOrNull() ?: return ""
        val month = match.groupValues[1].toIntOrNull() ?: return ""
        val day = match.groupValues[2].toIntOrNull() ?: return ""
        val actualYear = if (month >= 7) year else year + 1
        return runCatching { LocalDate.of(actualYear, month, day).toString() }.getOrDefault("")
    }
}
