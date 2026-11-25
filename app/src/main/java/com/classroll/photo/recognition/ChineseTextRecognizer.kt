package com.classroll.photo.recognition

import android.graphics.Bitmap
import com.classroll.photo.model.OcrEvidence
import com.classroll.photo.model.TextBox
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions

class ChineseTextRecognizer : AutoCloseable {
    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    suspend fun recognize(bitmap: Bitmap): OcrEvidence {
        val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitResult()
        val lines = result.textBlocks.flatMap { it.lines }
        val text = lines.joinToString("\n") { it.text.trim() }.lines().filter { it.isNotEmpty() }.joinToString("\n")
        val boxes = lines.mapNotNull { line ->
            val box = line.boundingBox ?: return@mapNotNull null
            TextBox(
                left = box.left.toFloat() / bitmap.width,
                top = box.top.toFloat() / bitmap.height,
                right = box.right.toFloat() / bitmap.width,
                bottom = box.bottom.toFloat() / bitmap.height,
            )
        }
        val areaRatio = boxes.sumOf { box ->
            ((box.right - box.left).coerceAtLeast(0f) * (box.bottom - box.top).coerceAtLeast(0f)).toDouble()
        }.coerceAtMost(1.0).toFloat()
        return OcrEvidence(
            text = text,
            characterCount = text.count { !it.isWhitespace() },
            textAreaRatio = areaRatio,
            boxes = boxes,
        )
    }

    override fun close() {
        recognizer.close()
    }
}
