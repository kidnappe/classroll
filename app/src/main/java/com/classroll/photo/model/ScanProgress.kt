package com.classroll.photo.model

enum class ScanStage {
    Idle,
    Querying,
    Matching,
    Coarse,
    Ocr,
    Done,
    Error,
    Cancelled,
}

data class ScanProgress(
    val stage: ScanStage = ScanStage.Idle,
    val total: Int = 0,
    val completed: Int = 0,
    val message: String = "准备扫描",
    val elapsedMillis: Long = 0L,
) {
    val fraction: Float
        get() = if (total <= 0) 0f else (completed.toFloat() / total).coerceIn(0f, 1f)

    val running: Boolean
        get() = stage == ScanStage.Querying || stage == ScanStage.Matching || stage == ScanStage.Coarse || stage == ScanStage.Ocr
}
