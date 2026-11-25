package com.classroll.photo.recognition

import com.classroll.photo.model.CoarseEvidence
import com.classroll.photo.model.OcrEvidence
import com.classroll.photo.model.Verdict
import kotlin.math.roundToInt

data class RuleDecision(
    val verdict: Verdict,
    val needsOcr: Boolean,
    val reason: String,
)

object RecognitionRules {
    private const val MIN_CHARACTERS = 30
    private const val MIN_TEXT_AREA = 0.03f
    private const val MIN_LINES = 3
    private const val MAX_LINES = 60
    private const val MIN_AVERAGE_CHARS_PER_LINE = 8f
    private const val MAX_SHORT_LINE_RATIO = 0.6f
    private const val MAX_TOP_EDGE_RATIO = 0.25f

    // 板书、纸质笔记、讲义内页：行短、字数少，但字号明显更大。
    // 只对“非截图、非分享文件”放开这一支，避免聊天记录和游戏截图混进来。
    private const val RELAXED_MIN_CHARACTERS = 25
    private const val RELAXED_MIN_TEXT_AREA = 0.02f
    private const val RELAXED_MIN_AVERAGE_CHARS_PER_LINE = 5.5f
    private const val RELAXED_MAX_SHORT_LINE_RATIO = 0.8f
    private const val RELAXED_MAX_TOP_EDGE_RATIO = 0.30f
    private const val MIN_AVERAGE_BOX_HEIGHT = 0.02f

    // 来自微信/QQ/小黑盒等分享、保存、编辑过的文件名：必须版式非常像课件才放行。
    private const val SHARED_MIN_AVERAGE_CHARS_PER_LINE = 12f
    private const val SHARED_MIN_TEXT_AREA = 0.05f
    private const val SHARED_MAX_SHORT_LINE_RATIO = 0.5f

    private val screenshotPattern = Regex(
        """(screenshot|screencap|screen[-_ ]?shot|截屏|截图)""",
        RegexOption.IGNORE_CASE,
    )
    private val sharedNamePattern = Regex(
        """(mmexport|wx_camera|wechat|weixin|qqimage|qq_image|heybox|share_|comment_|image_editor|_edit|screenshot|screencap|screen[-_ ]?shot|截屏|截图)""",
        RegexOption.IGNORE_CASE,
    )

    fun isScreenshotName(displayName: String): Boolean =
        displayName.isNotEmpty() && screenshotPattern.containsMatchIn(displayName)

    fun afterCoarse(coarse: CoarseEvidence, boundary: Boolean, displayName: String = ""): RuleDecision {
        if (isScreenshotName(displayName)) {
            return RuleDecision(Verdict.Unlike, false, "文件名来自截图，按规则跳过识别")
        }
        if (boundary) {
            return RuleDecision(Verdict.Boundary, true, "照片落在课间缓冲或课程重叠时间，内容识别后仍由你确认归属")
        }
        if (isStrongFaceNegative(coarse)) {
            return RuleDecision(Verdict.Unlike, false, "人脸面积较大或多脸明显，且没有屏幕证据；已跳过文字识别")
        }
        return RuleDecision(Verdict.Doubtful, true, "粗筛没有足够证据，继续做中文文字识别")
    }

    fun afterOcr(
        coarse: CoarseEvidence,
        ocr: OcrEvidence,
        boundary: Boolean,
        displayName: String = "",
    ): RuleDecision {
        if (isScreenshotName(displayName)) {
            return RuleDecision(Verdict.Unlike, false, "文件名来自截图，按规则跳过识别")
        }
        if (boundary) {
            return RuleDecision(Verdict.Boundary, false, boundaryReason(ocr))
        }
        if (isStrongFaceNegative(coarse) && ocr.characterCount < 12) {
            return RuleDecision(Verdict.Unlike, false, "大脸或多脸明显，文字识别也没有足够课程内容")
        }

        val layout = TextLayout.from(ocr, displayName)
        val strongLayout = ocr.characterCount >= MIN_CHARACTERS &&
            ocr.textAreaRatio >= MIN_TEXT_AREA &&
            layout.lineCount in MIN_LINES..MAX_LINES &&
            layout.averageCharsPerLine >= MIN_AVERAGE_CHARS_PER_LINE &&
            layout.shortLineRatio <= MAX_SHORT_LINE_RATIO &&
            layout.topEdgeRatio <= MAX_TOP_EDGE_RATIO
        val relaxedLayout = ocr.characterCount >= RELAXED_MIN_CHARACTERS &&
            ocr.textAreaRatio >= RELAXED_MIN_TEXT_AREA &&
            layout.lineCount in MIN_LINES..MAX_LINES &&
            layout.averageCharsPerLine >= RELAXED_MIN_AVERAGE_CHARS_PER_LINE &&
            layout.shortLineRatio <= RELAXED_MAX_SHORT_LINE_RATIO &&
            layout.topEdgeRatio <= RELAXED_MAX_TOP_EDGE_RATIO &&
            layout.averageBoxHeight >= MIN_AVERAGE_BOX_HEIGHT
        val sharedName = sharedNamePattern.containsMatchIn(displayName)
        val sharedHighConfidence = sharedName && layout.averageCharsPerLine >= SHARED_MIN_AVERAGE_CHARS_PER_LINE &&
            ocr.textAreaRatio >= SHARED_MIN_TEXT_AREA &&
            layout.shortLineRatio <= SHARED_MAX_SHORT_LINE_RATIO
        val slideLayout = strongLayout || (relaxedLayout && (!sharedName || sharedHighConfidence))

        if (slideLayout && layout.clockHits < 2) {
            return RuleDecision(
                Verdict.Likely,
                false,
                "版式像课件：${layout.lineCount} 行，平均每行 ${layout.averageCharsPerLine.roundToInt()} 字，" +
                    "文字框覆盖 ${percent(ocr.textAreaRatio)}",
            )
        }
        if (slideLayout) {
            return RuleDecision(
                Verdict.Doubtful,
                false,
                "文字量足够，但含 ${layout.clockHits} 处状态栏时间，版式更像应用界面或聊天记录；已降为存疑",
            )
        }
        if (relaxedLayout && sharedName) {
            return RuleDecision(
                Verdict.Doubtful,
                false,
                "文件名来自分享/保存/截图类应用，版式证据不够强（${layout.describe()}），保留给你确认",
            )
        }
        if (ocr.characterCount > 0) {
            val why = if (ocr.characterCount < RELAXED_MIN_CHARACTERS) {
                "字数偏少"
            } else if (ocr.textAreaRatio < RELAXED_MIN_TEXT_AREA) {
                "文字覆盖面积偏小"
            } else if (layout.averageBoxHeight < MIN_AVERAGE_BOX_HEIGHT) {
                "字号偏小"
            } else {
                "行数或每行字数不像课件"
            }
            return RuleDecision(
                Verdict.Doubtful,
                false,
                "识别到 ${ocr.characterCount} 字，但${why}（${layout.describe()}），保留给你确认",
            )
        }
        if (coarse.screenScore >= 0.45f) {
            return RuleDecision(Verdict.Doubtful, false, "检测到较大亮区，但没有识别出文字；可能反光、模糊或空屏幕")
        }
        return RuleDecision(Verdict.Doubtful, false, "没有识别出文字，不能仅凭这一点判为不像课件")
    }

    private fun isStrongFaceNegative(coarse: CoarseEvidence): Boolean =
        coarse.screenScore < 0.35f && (coarse.maxFaceAreaRatio >= 0.18f || coarse.faceCount >= 3)

    private fun boundaryReason(ocr: OcrEvidence): String = if (ocr.characterCount > 0) {
        "内容识别到 ${ocr.characterCount} 字，但拍摄时间落在课程边界，需确认归到哪节课"
    } else {
        "拍摄时间落在课程边界，且没有识别出文字，需人工确认"
    }
}
