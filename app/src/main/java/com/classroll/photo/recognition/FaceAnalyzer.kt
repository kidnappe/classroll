package com.classroll.photo.recognition

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

data class FaceEvidence(
    val count: Int,
    val maxAreaRatio: Float,
)

class FaceAnalyzer : AutoCloseable {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.12f)
            .build(),
    )

    suspend fun analyze(bitmap: Bitmap): FaceEvidence {
        val faces = detector.process(InputImage.fromBitmap(bitmap, 0)).awaitResult()
        val pixelCount = bitmap.width.toLong() * bitmap.height
        val maxArea = faces.maxOfOrNull { face ->
            face.boundingBox.width().toLong() * face.boundingBox.height()
        } ?: 0L
        return FaceEvidence(faces.size, (maxArea.toFloat() / pixelCount).coerceIn(0f, 1f))
    }

    override fun close() {
        detector.close()
    }
}
