package com.classroll.photo.scan

import com.classroll.photo.model.Verdict
import com.classroll.photo.recognition.TextLayout
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class RecordTrigger(val wireName: String) {
    ScanComplete("scan_complete"),
    SelectionChanged("selection_changed"),
    ConfirmDone("confirm_done"),
}

/** 设备/版本信息由 Android 层收集，序列化层保持纯 JVM，方便单测。 */
data class RecordEnv(
    val device: String,
    val manufacturer: String,
    val androidRelease: String,
    val androidSdk: Int,
    val appVersion: String,
    val generatedAt: String = nowText(),
) {
    companion object {
        private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

        fun nowText(): String = OffsetDateTime.now().format(timeFormat)
    }
}

/**
 * 扫描记录序列化。字段口径与 `tools/make_scan_record.py` 产出的 scan-record.json 对齐：
 * 顶层 schemaVersion / recordType / generatedAt / scan / items，item 内嵌 coarse / ocr / layout。
 *
 * 这个对象不依赖 Android API，App 里由 [ScanRecordStore] 调用来落盘，单测里可以直接跑。
 */
object ScanRecordSerializer {
    const val SCHEMA_VERSION = 1
    const val RECORD_TYPE = "classroll_scan_record"

    private val labelPolicy = listOf(
        "Likely 档全部视为已审阅：selected=true -> positive；selected=false -> negative。",
        "Boundary 档只把 selected=true 的记为 positive_boundary，不把其余 Boundary 当成 negative。",
        "Doubtful / Unlike 未逐张审阅，label=unlabeled，不参与 precision/recall。",
        "positive_boundary 单独统计，不混入 Likely 的正负样本。",
        "touched=true 表示这张照片被人工点过或批量改过；touched=false 且 verdict=Likely 的是扫描后默认勾选/默认未勾。",
        "verdictAtScan 是写这份快照时的识别档位；trigger=scan_complete 时就是扫描刚结束的档位。",
        "layout 由 ocr.text 与 ocr.boxes 实时计算，口径与 App 识别规则 TextLayout 完全一致。",
    )

    fun build(
        state: ScanUiState,
        trigger: RecordTrigger,
        env: RecordEnv,
        elapsedMillis: Long,
    ): String {
        val items = state.photos
        val selectedCount = items.count { it.id in state.selectedIds }
        val verdictCounts = items.groupingBy { it.verdict }.eachCount()
        val labelCounts = items.groupingBy { labelOf(it.id, it.verdict, state) }.eachCount()
        val positive = labelCounts["positive"] ?: 0
        val negative = labelCounts["negative"] ?: 0
        val boundaryPositive = labelCounts["positive_boundary"] ?: 0
        val unlabeled = labelCounts["unlabeled"] ?: 0
        val reviewed = items.count { reviewedOf(it.id, it.verdict, state) }
        val likelyTotal = verdictCounts[Verdict.Likely] ?: 0

        val json = StringBuilder(2_000_000)
        json.append('{')
        json.field("schemaVersion", SCHEMA_VERSION, first = true)
        json.field("recordType", RECORD_TYPE)
        json.field("generatedAt", env.generatedAt)
        json.field("scanId", state.scanId ?: "")
        json.field("trigger", trigger.wireName)
        json.field("revision", state.recordRevision)

        json.append(",\"env\":{")
        json.field("device", env.device, first = true)
        json.field("manufacturer", env.manufacturer)
        json.field("androidRelease", env.androidRelease)
        json.field("androidSdk", env.androidSdk)
        json.field("appVersion", env.appVersion)
        json.append('}')

        json.append(",\"scan\":{")
        json.field("source", state.source?.name ?: "Unknown", first = true)
        json.field("candidateCount", items.size)
        json.field("selectedCount", selectedCount)
        json.field("reviewedCount", reviewed)
        json.field("elapsedMillis", elapsedMillis)
        json.field("stage", state.progress.stage.name)
        json.field("confirmed", state.confirmed)
        json.field("scheduleCourseCount", state.schedule?.courses?.size ?: 0)
        json.field("semesterStart", state.schedule?.semesterStart?.toString() ?: "")
        json.field("includeSharedImages", state.includeSharedImages)

        json.append(",\"verdictCounts\":{")
        var first = true
        Verdict.entries.forEach { verdict ->
            json.field(verdict.name, verdictCounts[verdict] ?: 0, first = first)
            first = false
        }
        json.append('}')

        json.append(",\"labelCounts\":{")
        json.field("positive", positive, first = true)
        json.field("negative", negative)
        json.field("positive_boundary", boundaryPositive)
        json.field("unlabeled", unlabeled)
        json.append('}')

        json.append(",\"tuningSet\":{")
        json.field("verdict", Verdict.Likely.name, first = true)
        json.field("total", likelyTotal)
        json.field("positive", positive)
        json.field("negative", negative)
        json.append('}')

        json.field("boundaryPositive", boundaryPositive)
        json.field("unlabeled", unlabeled)

        json.append(",\"labelPolicy\":[")
        labelPolicy.forEachIndexed { index, line ->
            if (index > 0) json.append(',')
            json.string(line)
        }
        json.append(']')
        json.append('}')

        json.append(",\"groups\":[")
        state.groups.values.forEachIndexed { index, group ->
            if (index > 0) json.append(',')
            json.append('{')
            json.field("id", group.id, first = true)
            json.field("course", group.course)
            json.field("session", group.session)
            json.field("boundary", group.boundary)
            json.field("date", group.date?.toString() ?: "")
            json.append('}')
        }
        json.append(']')

        json.append(",\"items\":[")
        items.forEachIndexed { index, photo ->
            if (index > 0) json.append(',')
            val layout = TextLayout.from(photo.ocr, photo.displayName)
            json.append('{')
            json.field("id", photo.id, first = true)
            json.field("imageUri", photo.imageUri)
            json.field("displayName", photo.displayName)
            json.field("title", photo.title)
            json.field("groupId", photo.groupId)
            json.field("relativePath", photo.relativePath ?: "")
            json.field("timeLabel", photo.timeLabel)
            json.field("takenAt", photo.takenAt?.toString() ?: "")
            json.field("timeSource", photo.timeSource.name)
            json.field("statusAtScan", photo.status.name)
            json.field("verdictAtScan", photo.verdict.name)
            json.field("reason", photo.reason)
            if (photo.error == null) json.field("error", null as String?) else json.field("error", photo.error)
            json.field("selected", photo.id in state.selectedIds)
            json.field("touched", photo.id in state.touchedIds)
            json.field("reviewed", reviewedOf(photo.id, photo.verdict, state))
            json.field("label", labelOf(photo.id, photo.verdict, state))
            json.field("tuningEligible", photo.verdict == Verdict.Likely)
            json.append(",\"coarse\":{")
            json.number("screenScore", photo.coarse.screenScore, first = true)
            json.field("faceCount", photo.coarse.faceCount)
            json.number("maxFaceAreaRatio", photo.coarse.maxFaceAreaRatio)
            json.number("brightness", photo.coarse.brightness)
            json.field("perceptualHash", photo.coarse.perceptualHash)
            json.field("nearDuplicateCount", photo.coarse.nearDuplicateCount)
            val outlier = photo.coarse.outlierDistance
            if (outlier == null) json.field("outlierDistance", null as Int?) else json.field("outlierDistance", outlier)
            json.append('}')
            json.append(",\"ocr\":{")
            json.field("characterCount", photo.ocr.characterCount, first = true)
            json.number("textAreaRatio", photo.ocr.textAreaRatio)
            json.field("text", photo.ocr.text)
            json.append(",\"boxes\":[")
            photo.ocr.boxes.forEachIndexed { boxIndex, box ->
                if (boxIndex > 0) json.append(',')
                json.append('{')
                json.number("left", box.left, first = true)
                json.number("top", box.top)
                json.number("right", box.right)
                json.number("bottom", box.bottom)
                json.append('}')
            }
            json.append(']')
            json.append('}')
            json.append(",\"layout\":{")
            json.field("lineCount", layout.lineCount, first = true)
            json.number("averageCharsPerLine", layout.averageCharsPerLine)
            json.number("shortLineRatio", layout.shortLineRatio)
            json.number("topEdgeRatio", layout.topEdgeRatio)
            json.number("averageBoxHeight", layout.averageBoxHeight)
            json.number("maxBoxHeight", layout.maxBoxHeight)
            json.field("clockHits", layout.clockHits)
            json.field("screenshotName", layout.screenshotName)
            json.field("headingCount", layout.headingCount)
            json.append('}')
            json.append('}')
        }
        json.append(']')
        json.append('}')
        json.append('\n')
        return json.toString()
    }

    /** 勾选历史：每次人工勾选变化追加一行，含完整 selectedIds，进程被杀也能还原。 */
    fun buildSelectionEvent(
        state: ScanUiState,
        trigger: String,
        changedIds: Set<String>,
        env: RecordEnv,
    ): String {
        val items = state.photos
        val itemIds = items.mapTo(HashSet()) { it.id }
        val verdictCounts = items.groupingBy { it.verdict }.eachCount()
        val labelCounts = items.groupingBy { labelOf(it.id, it.verdict, state) }.eachCount()
        val json = StringBuilder(16_000)
        json.append('{')
        json.field("schemaVersion", SCHEMA_VERSION, first = true)
        json.field("recordType", "classroll_selection_event")
        json.field("scanId", state.scanId ?: "")
        json.field("revision", state.recordRevision)
        json.field("at", env.generatedAt)
        json.field("trigger", trigger)
        json.append(",\"changedIds\":[")
        changedIds.forEachIndexed { index, id ->
            if (index > 0) json.append(',')
            json.string(id)
        }
        json.append(']')
        json.append(",\"selectedIds\":[")
        state.selectedIds.forEachIndexed { index, id ->
            if (index > 0) json.append(',')
            json.string(id)
        }
        json.append(']')
        json.field("selectedCount", state.selectedIds.count { it in itemIds })
        json.append(",\"verdictCounts\":{")
        var first = true
        Verdict.entries.forEach { verdict ->
            json.field(verdict.name, verdictCounts[verdict] ?: 0, first = first)
            first = false
        }
        json.append('}')
        json.append(",\"labelCounts\":{")
        json.field("positive", labelCounts["positive"] ?: 0, first = true)
        json.field("negative", labelCounts["negative"] ?: 0)
        json.field("positive_boundary", labelCounts["positive_boundary"] ?: 0)
        json.field("unlabeled", labelCounts["unlabeled"] ?: 0)
        json.append('}')
        json.append('}')
        json.append('\n')
        return json.toString()
    }

    fun buildSummary(state: ScanUiState, trigger: RecordTrigger, env: RecordEnv): String {
        val items = state.photos
        val verdictCounts = items.groupingBy { it.verdict }.eachCount()
        val labelCounts = items.groupingBy { labelOf(it.id, it.verdict, state) }.eachCount()
        val selected = items.count { it.id in state.selectedIds }
        val reviewed = items.count { reviewedOf(it.id, it.verdict, state) }
        return buildString {
            appendLine("scanId: ${state.scanId}")
            appendLine("trigger: ${trigger.wireName}")
            appendLine("revision: ${state.recordRevision}")
            appendLine("generatedAt: ${env.generatedAt}")
            appendLine("device: ${env.manufacturer} ${env.device} · Android ${env.androidRelease} (SDK ${env.androidSdk})")
            appendLine("app: ${env.appVersion}")
            appendLine("source: ${state.source?.name ?: "Unknown"}")
            appendLine("includeSharedImages: ${state.includeSharedImages}")
            appendLine("candidates: ${items.size}")
            appendLine("selected: $selected")
            appendLine("reviewed: $reviewed")
            appendLine("verdicts: ${Verdict.entries.associateWith { verdictCounts[it] ?: 0 }}")
            appendLine("labels: ${listOf("positive", "negative", "positive_boundary", "unlabeled").associateWith { labelCounts[it] ?: 0 }}")
            appendLine()
            appendLine("label policy:")
            labelPolicy.forEach { appendLine("- $it") }
        }
    }

    fun labelOf(id: String, verdict: Verdict, state: ScanUiState): String = when {
        verdict == Verdict.Likely -> if (id in state.selectedIds) "positive" else "negative"
        verdict == Verdict.Boundary && id in state.selectedIds -> "positive_boundary"
        else -> "unlabeled"
    }

    fun reviewedOf(id: String, verdict: Verdict, state: ScanUiState): Boolean =
        verdict == Verdict.Likely || id in state.touchedIds

    private fun StringBuilder.field(name: String, value: String?, first: Boolean = false) {
        if (!first) append(',')
        string(name)
        append(':')
        if (value == null) append("null") else string(value)
    }

    private fun StringBuilder.field(name: String, value: Int?, first: Boolean = false) {
        if (!first) append(',')
        string(name)
        append(':')
        if (value == null) append("null") else append(value)
    }

    private fun StringBuilder.field(name: String, value: Long, first: Boolean = false) {
        if (!first) append(',')
        string(name)
        append(':')
        append(value)
    }

    private fun StringBuilder.field(name: String, value: Boolean, first: Boolean = false) {
        if (!first) append(',')
        string(name)
        append(':')
        append(value)
    }

    private fun StringBuilder.number(name: String, value: Float, first: Boolean = false) {
        if (!first) append(',')
        string(name)
        append(':')
        append(String.format(Locale.US, "%.4f", value))
    }

    private fun StringBuilder.string(value: String) {
        append('"')
        var index = 0
        while (index < value.length) {
            val char = value[index]
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                else -> {
                    if (char < ' ') {
                        append(String.format(Locale.US, "\\u%04x", char.code))
                    } else {
                        append(char)
                    }
                }
            }
            index++
        }
        append('"')
    }
}
