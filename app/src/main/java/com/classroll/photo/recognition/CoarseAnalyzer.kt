package com.classroll.photo.recognition

import android.content.Context
import android.graphics.Bitmap
import com.classroll.photo.model.CoarseEvidence

class CoarseAnalyzer(context: Context, private val faceAnalyzer: FaceAnalyzer = FaceAnalyzer()) : AutoCloseable {
    private val appContext = context.applicationContext

    suspend fun analyze(uri: String): CoarseEvidence {
        val bitmap = BitmapDecoder.decode(appContext, uri, 1600)
        return try {
            val screen = ScreenAnalyzer.analyze(bitmap)
            val face = faceAnalyzer.analyze(bitmap)
            CoarseEvidence(
                screenScore = screen.score,
                faceCount = face.count,
                maxFaceAreaRatio = face.maxAreaRatio,
                brightness = screen.brightness,
                perceptualHash = PerceptualHash.fromBitmap(bitmap),
            )
        } finally {
            bitmap.recycle()
        }
    }

    fun analyzeBitmap(bitmap: Bitmap): CoarseEvidence {
        val screen = ScreenAnalyzer.analyze(bitmap)
        return CoarseEvidence(
            screenScore = screen.score,
            brightness = screen.brightness,
            perceptualHash = PerceptualHash.fromBitmap(bitmap),
        )
    }

    override fun close() {
        faceAnalyzer.close()
    }
}
