package com.classroll.photo.recognition

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class ScreenEvidence(
    val score: Float,
    val brightness: Float,
)

object ScreenAnalyzer {
    fun analyze(bitmap: Bitmap): ScreenEvidence {
        val scale = min(1f, 256f / max(bitmap.width, bitmap.height))
        val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)
        if (scaled !== bitmap) scaled.recycle()
        return analyzePixels(width, height, pixels)
    }

    fun analyzePixels(width: Int, height: Int, pixels: IntArray): ScreenEvidence {
        require(pixels.size == width * height)
        val gray = IntArray(pixels.size)
        val histogram = IntArray(256)
        var brightnessSum = 0L
        pixels.forEachIndexed { index, color ->
            val value = (((color ushr 16) and 0xff) * 299 + ((color ushr 8) and 0xff) * 587 + (color and 0xff) * 114) / 1000
            gray[index] = value
            histogram[value]++
            brightnessSum += value
        }
        val threshold = max(175, otsuThreshold(histogram, pixels.size))
        val visited = BooleanArray(pixels.size)
        val stack = IntArray(pixels.size)
        var largestArea = 0
        var largestMinX = 0
        var largestMinY = 0
        var largestMaxX = 0
        var largestMaxY = 0
        for (index in pixels.indices) {
            if (visited[index] || gray[index] < threshold) continue
            var stackSize = 0
            stack[stackSize++] = index
            visited[index] = true
            var area = 0
            var minX = width
            var minY = height
            var maxX = 0
            var maxY = 0
            while (stackSize > 0) {
                val current = stack[--stackSize]
                val x = current % width
                val y = current / width
                area++
                minX = min(minX, x)
                minY = min(minY, y)
                maxX = max(maxX, x)
                maxY = max(maxY, y)
                val neighbors = intArrayOf(
                    if (x > 0) current - 1 else -1,
                    if (x < width - 1) current + 1 else -1,
                    if (y > 0) current - width else -1,
                    if (y < height - 1) current + width else -1,
                )
                neighbors.forEach { next ->
                    if (next >= 0 && !visited[next] && gray[next] >= threshold) {
                        visited[next] = true
                        stack[stackSize++] = next
                    }
                }
            }
            if (area > largestArea) {
                largestArea = area
                largestMinX = minX
                largestMinY = minY
                largestMaxX = maxX
                largestMaxY = maxY
            }
        }
        if (largestArea == 0) return ScreenEvidence(0f, brightnessSum.toFloat() / pixels.size / 255f)
        val boxWidth = largestMaxX - largestMinX + 1
        val boxHeight = largestMaxY - largestMinY + 1
        val areaRatio = largestArea.toFloat() / pixels.size
        val fillRatio = largestArea.toFloat() / (boxWidth * boxHeight)
        val spanRatio = min(boxWidth.toFloat() / width, boxHeight.toFloat() / height)
        val borderDarkness = borderDarkness(gray, width, height)
        var score = (areaRatio * fillRatio * spanRatio * (0.35f + 0.65f * borderDarkness)).coerceIn(0f, 1f)
        if (areaRatio > 0.9f && borderDarkness < 0.15f) score *= 0.35f
        return ScreenEvidence(score, brightnessSum.toFloat() / pixels.size / 255f)
    }

    private fun otsuThreshold(histogram: IntArray, total: Int): Int {
        var sum = 0L
        histogram.forEachIndexed { value, count -> sum += value.toLong() * count }
        var backgroundWeight = 0L
        var backgroundSum = 0L
        var bestVariance = -1.0
        var bestThreshold = 0
        for (threshold in 0 until 256) {
            backgroundWeight += histogram[threshold]
            if (backgroundWeight == 0L) continue
            val foregroundWeight = total - backgroundWeight
            if (foregroundWeight == 0L) break
            backgroundSum += threshold.toLong() * histogram[threshold]
            val backgroundMean = backgroundSum.toDouble() / backgroundWeight
            val foregroundMean = (sum - backgroundSum).toDouble() / foregroundWeight
            val difference = backgroundMean - foregroundMean
            val variance = difference * difference * backgroundWeight * foregroundWeight
            if (variance > bestVariance) {
                bestVariance = variance
                bestThreshold = threshold
            }
        }
        return bestThreshold
    }

    private fun borderDarkness(gray: IntArray, width: Int, height: Int): Float {
        var sum = 0L
        var count = 0
        val band = max(1, min(width, height) / 40)
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (x < band || y < band || x >= width - band || y >= height - band) {
                    sum += gray[y * width + x]
                    count++
                }
            }
        }
        return (1f - sum.toFloat() / count / 255f).coerceIn(0f, 1f)
    }
}
