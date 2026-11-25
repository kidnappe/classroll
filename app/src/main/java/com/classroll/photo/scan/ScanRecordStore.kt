package com.classroll.photo.scan

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 扫描记录落盘。主目录：`Android/data/com.classroll.photo/files/scan-records/`。
 *
 * 每次扫描固定产出四类文件：
 *  - `scan-<id>-scan.json`：扫描刚结束的完整快照（trigger=scan_complete）
 *  - `scan-<id>-latest.json`：人工勾选变化后的最新完整快照（trigger=selection_changed），原子覆盖
 *  - `scan-<id>-confirm.json`：点“完成”时的最终快照（trigger=confirm_done）
 *  - `scan-<id>-selection.jsonl`：每次勾选变化追一行，含完整 selectedIds，进程被杀也能还原
 *
 * 外部目录不可写时自动退回 App 内部目录，保证记录不丢。
 */
class ScanRecordStore(context: Context) {
    private val appContext = context.applicationContext
    private val mutex = Mutex()

    fun directory(): File {
        val external = appContext.getExternalFilesDir("scan-records")
        val dir = external ?: File(appContext.filesDir, "scan-records")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun fallbackDirectory(): File {
        val dir = File(appContext.filesDir, "scan-records")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun newScanId(): String {
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
        val suffix = Random.nextInt(0x1000, 0xFFFF).toString(16)
        return "$stamp-$suffix"
    }

    suspend fun writeSnapshot(
        state: ScanUiState,
        trigger: RecordTrigger,
        elapsedMillis: Long,
    ): File? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val scanId = state.scanId ?: return@withContext null
            val env = buildEnv()
            val json = ScanRecordSerializer.build(state, trigger, env, elapsedMillis)
            val suffix = when (trigger) {
                RecordTrigger.ScanComplete -> "scan"
                RecordTrigger.SelectionChanged -> "latest"
                RecordTrigger.ConfirmDone -> "confirm"
            }
            val files = linkedMapOf("scan-$scanId-$suffix.json" to json)
            if (trigger != RecordTrigger.SelectionChanged) {
                files["scan-$scanId-$suffix-summary.txt"] = ScanRecordSerializer.buildSummary(state, trigger, env)
            }
            files["latest.json"] =
                "{\"scanId\":\"$scanId\",\"trigger\":\"${trigger.wireName}\",\"file\":\"scan-$scanId-$suffix.json\"}\n"
            val written = writeGroup(files)
            Log.i(TAG, "snapshot ${trigger.wireName} rev=${state.recordRevision} -> ${written.first.absolutePath}")
            written.first
        }
    }

    suspend fun appendSelectionEvent(
        state: ScanUiState,
        trigger: String,
        changedIds: Set<String>,
    ): File? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val scanId = state.scanId ?: return@withContext null
            val name = "scan-$scanId-selection.jsonl"
            val env = buildEnv()
            val line = ScanRecordSerializer.buildSelectionEvent(state, trigger, changedIds, env)
            try {
                val target = File(directory(), name)
                target.appendText(line, Charsets.UTF_8)
                target
            } catch (error: Throwable) {
                Log.e(TAG, "append selection event failed, retry internal storage", error)
                val target = File(fallbackDirectory(), name)
                target.appendText(line, Charsets.UTF_8)
                target
            }
        }
    }

    /** 只保留最近 [keep] 次扫描的文件，避免多次扫描把手机塞满。 */
    fun pruneOldScans(keep: Int = 5) {
        runCatching {
            val files = directory().listFiles() ?: return
            val groups = files
                .mapNotNull { file ->
                    val match = scanIdPattern.find(file.name) ?: return@mapNotNull null
                    match.groupValues[1] to file
                }
                .groupBy({ it.first }, { it.second })
            if (groups.size <= keep) return
            val staleIds = groups.entries
                .sortedByDescending { entry -> entry.value.maxOf { it.lastModified() } }
                .drop(keep)
                .map { it.key }
            staleIds.forEach { id ->
                groups[id]?.forEach { file -> file.delete() }
            }
        }
    }

    private fun writeGroup(files: Map<String, String>): Pair<File, Boolean> {
        return try {
            val dir = directory()
            files.forEach { (name, content) -> writeAtomically(File(dir, name), content) }
            File(dir, files.keys.first()) to false
        } catch (error: Throwable) {
            Log.e(TAG, "external record write failed, retry internal storage", error)
            val dir = fallbackDirectory()
            files.forEach { (name, content) -> writeAtomically(File(dir, name), content) }
            File(dir, files.keys.first()) to true
        }
    }

    private fun buildEnv(): RecordEnv = RecordEnv(
        device = Build.MODEL ?: "unknown",
        manufacturer = Build.MANUFACTURER ?: "unknown",
        androidRelease = Build.VERSION.RELEASE ?: "unknown",
        androidSdk = Build.VERSION.SDK_INT,
        appVersion = runCatching {
            @Suppress("DEPRECATION")
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
        }.getOrNull() ?: "unknown",
    )

    private fun writeAtomically(target: File, content: String) {
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, target.name + ".tmp")
        temp.writeText(content, Charsets.UTF_8)
        if (temp.renameTo(target)) return
        temp.inputStream().use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        temp.delete()
    }

    private companion object {
        const val TAG = "ScanRecordStore"
        val scanIdPattern = Regex("""^scan-(\d{8}-\d{6}-[0-9a-f]{4})-""")
    }
}
