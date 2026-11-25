package com.classroll.photo.recognition

import com.classroll.photo.model.OcrEvidence
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 从 OCR 结果还原文字版式。真机上 ML Kit 每个 line 给一个框，因此框数就是行数；
 * 用每行平均字数、短行比例和贴顶行比例区分“课件排版”和“应用界面/聊天记录”。
 *
 * 这份版式同时是识别规则和扫描记录的共用口径：
 * [com.classroll.photo.scan.ScanRecordSerializer] 会把这里算出的字段原样写进 scan-record.json，
 * 保证 App 落盘的记录和 PC 侧 `tools/make_scan_record.py` 的回放口径一致。
 */
data class TextLayout(
    val lineCount: Int,
    val averageCharsPerLine: Float,
    val shortLineRatio: Float,
    val topEdgeRatio: Float,
    val averageBoxHeight: Float,
    val maxBoxHeight: Float,
    val clockHits: Int,
    val screenshotName: Boolean,
    val headingCount: Int,
) {
    fun describe(): String =
        "$lineCount 行，平均每行 ${averageCharsPerLine.roundToInt()} 字，短行 ${percent(shortLineRatio)}"

    companion object {
        private val clockPattern = Regex("""\b\d{1,2}:\d{2}\b""")
        private val screenshotNamePattern = Regex(
            """(screenshot|screencap|screen[-_ ]?shot|截屏|截图)""",
            RegexOption.IGNORE_CASE,
        )
        private val headingPattern = Regex(
            """^\s*(?:[一二三四五六七八九十]+[、.．]|第[一二三四五六七八九十0-9]+[章节讲部分]|\d+[、.．)）]|[（(]\d+[）)]|[·•●▪◆◇→\-—])""",
        )

        fun from(ocr: OcrEvidence, displayName: String = ""): TextLayout {
            val textLines = ocr.text
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toList()
            val lineCount = maxOf(ocr.boxes.size, textLines.size)
            val averageCharsPerLine = if (lineCount > 0) {
                ocr.characterCount.toFloat() / lineCount
            } else {
                0f
            }
            val shortLineRatio = if (textLines.isNotEmpty()) {
                textLines.count { line -> line.count { !it.isWhitespace() } <= 6 }.toFloat() / textLines.size
            } else {
                1f
            }
            val topEdgeRatio = if (ocr.boxes.isNotEmpty()) {
                ocr.boxes.count { it.top < 0.06f }.toFloat() / ocr.boxes.size
            } else {
                0f
            }
            val heights = ocr.boxes.map { box -> (box.bottom - box.top).coerceAtLeast(0f) }
            val averageBoxHeight = if (heights.isEmpty()) 0f else heights.sum() / heights.size
            val maxBoxHeight = heights.maxOrNull() ?: 0f
            val clockHits = clockPattern.findAll(ocr.text).count()
            val screenshotName = displayName.isNotEmpty() && screenshotNamePattern.containsMatchIn(displayName)
            val headingCount = textLines.count { headingPattern.containsMatchIn(it) }
            return TextLayout(
                lineCount = lineCount,
                averageCharsPerLine = averageCharsPerLine,
                shortLineRatio = shortLineRatio,
                topEdgeRatio = topEdgeRatio,
                averageBoxHeight = averageBoxHeight,
                maxBoxHeight = maxBoxHeight,
                clockHits = clockHits,
                screenshotName = screenshotName,
                headingCount = headingCount,
            )
        }
    }
}

internal fun percent(value: Float): String = "${(value * 100).roundToInt()}%"
