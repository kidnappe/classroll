package com.classroll.photo.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * 扫描历史：记录每张照片被扫描几次、是否被选中过。
 * 连续两次出现在结果里都没被选中，第三次起不再进入候选。
 */
class ScanHistoryStore(context: Context) {
    private val file = File(context.applicationContext.filesDir, "scan-history.json")

    private data class Record(val misses: Int, val selected: Boolean)

    suspend fun ignoredIds(): Set<Long> = withContext(Dispatchers.IO) {
        read().filterValues { !it.selected && it.misses >= 2 }.keys
    }

    suspend fun recordRound(photoIds: Set<Long>, selectedIds: Set<Long>) = withContext(Dispatchers.IO) {
        if (photoIds.isEmpty()) return@withContext
        val current = read()
        photoIds.forEach { id ->
            val old = current[id] ?: Record(0, false)
            current[id] = when {
                id in selectedIds -> Record(0, true)
                old.selected -> old
                else -> Record(old.misses + 1, false)
            }
        }
        write(current)
    }

    private fun read(): MutableMap<Long, Record> {
        if (!file.exists()) return mutableMapOf()
        return runCatching {
            val root = JSONObject(file.readText())
            val result = mutableMapOf<Long, Record>()
            root.keys().forEach { key ->
                val id = key.toLongOrNull() ?: return@forEach
                val item = root.optJSONObject(key) ?: return@forEach
                result[id] = Record(item.optInt("misses", 0), item.optBoolean("selected", false))
            }
            result
        }.getOrDefault(mutableMapOf())
    }

    private fun write(records: Map<Long, Record>) {
        val root = JSONObject()
        records.forEach { (id, record) ->
            root.put(
                id.toString(),
                JSONObject().put("misses", record.misses).put("selected", record.selected),
            )
        }
        file.writeText(root.toString())
    }
}
