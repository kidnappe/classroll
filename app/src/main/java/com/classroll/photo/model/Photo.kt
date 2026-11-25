package com.classroll.photo.model

import java.time.LocalDateTime

enum class Verdict(val label: String) {
    Likely("像课件"),
    Doubtful("存疑"),
    Boundary("边界"),
    Unlike("不像"),
}

enum class PhotoTimeSource {
    Taken,
    Added,
    Picker,
}

enum class AnalysisStatus {
    Pending,
    CoarseDone,
    OcrDone,
    OcrSkipped,
    Failed,
}

data class PhotoGroup(
    val id: String,
    val course: String,
    val session: String,
    val meta: String,
    val week: Int?,
    val date: LocalDateTime?,
    val boundary: Boolean = false,
)

data class TextBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class OcrEvidence(
    val text: String = "",
    val characterCount: Int = 0,
    val textAreaRatio: Float = 0f,
    val boxes: List<TextBox> = emptyList(),
)

data class CoarseEvidence(
    val screenScore: Float = 0f,
    val faceCount: Int = 0,
    val maxFaceAreaRatio: Float = 0f,
    val brightness: Float = 0f,
    val perceptualHash: Long = 0L,
    val nearDuplicateCount: Int = 0,
    val outlierDistance: Int? = null,
)

data class PhotoItem(
    val id: String,
    val imageUri: String,
    val displayName: String,
    val mediaStoreId: Long?,
    val groupId: String,
    val title: String,
    val timeLabel: String,
    val takenAt: LocalDateTime?,
    val timeSource: PhotoTimeSource,
    val status: AnalysisStatus = AnalysisStatus.Pending,
    val verdict: Verdict = Verdict.Doubtful,
    val coarse: CoarseEvidence = CoarseEvidence(),
    val ocr: OcrEvidence = OcrEvidence(),
    val reason: String = "等待识别",
    val error: String? = null,
    /** MediaStore 的相对路径，例如 DCIM/Camera/；用于区分相机照片、截图和分享图片。 */
    val relativePath: String? = null,
) {
    val evidenceSummary: String
        get() = when {
            error != null -> error
            status == AnalysisStatus.Pending -> "等待识别"
            status == AnalysisStatus.OcrSkipped -> "粗筛已跳过文字识别 · $reason"
            ocr.characterCount > 0 -> "识别 ${ocr.characterCount} 字 · 屏幕 ${(coarse.screenScore * 100).roundToInt()}% · 人脸 ${coarse.faceCount} 张"
            else -> "未识别出文字 · 屏幕 ${(coarse.screenScore * 100).roundToInt()}% · 人脸 ${coarse.faceCount} 张"
        }
}

private fun Float.roundToInt(): Int = kotlin.math.round(this).toInt()
