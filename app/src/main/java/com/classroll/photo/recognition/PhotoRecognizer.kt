package com.classroll.photo.recognition

import android.content.Context
import com.classroll.photo.model.AnalysisStatus
import com.classroll.photo.model.CoarseEvidence
import com.classroll.photo.model.PhotoItem
import kotlinx.coroutines.CancellationException

class PhotoRecognizer(
    context: Context,
    private val coarseAnalyzer: CoarseAnalyzer = CoarseAnalyzer(context),
    private val textRecognizer: ChineseTextRecognizer = ChineseTextRecognizer(),
) : AutoCloseable {
    private val appContext = context.applicationContext

    suspend fun analyzeCoarse(photo: PhotoItem, boundary: Boolean): PhotoItem {
        if (RecognitionRules.isScreenshotName(photo.displayName)) {
            val decision = RecognitionRules.afterCoarse(CoarseEvidence(), boundary, photo.displayName)
            return photo.copy(
                status = AnalysisStatus.OcrSkipped,
                verdict = decision.verdict,
                reason = decision.reason,
                error = null,
            )
        }
        return try {
            val coarse = coarseAnalyzer.analyze(photo.imageUri)
            val decision = RecognitionRules.afterCoarse(coarse, boundary, photo.displayName)
            photo.copy(
                status = if (decision.needsOcr) AnalysisStatus.CoarseDone else AnalysisStatus.OcrSkipped,
                verdict = decision.verdict,
                coarse = coarse,
                reason = decision.reason,
                error = null,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            photo.copy(
                status = AnalysisStatus.Failed,
                reason = "粗筛失败",
                error = "这张照片无法读取或粗筛失败：${error.message ?: "未知错误"}",
            )
        }
    }

    suspend fun analyzeOcr(photo: PhotoItem, boundary: Boolean, force: Boolean = false): PhotoItem {
        if (photo.status == AnalysisStatus.OcrSkipped && !force) return photo
        return try {
            val bitmap = BitmapDecoder.decode(appContext, photo.imageUri, 1600)
            val ocr = try {
                textRecognizer.recognize(bitmap)
            } finally {
                bitmap.recycle()
            }
            val decision = RecognitionRules.afterOcr(photo.coarse, ocr, boundary, photo.displayName)
            photo.copy(
                status = AnalysisStatus.OcrDone,
                verdict = decision.verdict,
                ocr = ocr,
                reason = decision.reason,
                error = null,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            photo.copy(
                status = AnalysisStatus.Failed,
                reason = "文字识别失败",
                error = "中文文字识别失败：${error.message ?: "未知错误"}",
            )
        }
    }

    override fun close() {
        textRecognizer.close()
        coarseAnalyzer.close()
    }
}
