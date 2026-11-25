package com.classroll.photo.recognition

import android.graphics.Bitmap
import kotlin.math.roundToInt

object PerceptualHash {
    fun fromBitmap(bitmap: Bitmap): Long {
        val resized = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        val pixels = IntArray(72)
        resized.getPixels(pixels, 0, 9, 0, 0, 9, 8)
        if (resized !== bitmap) resized.recycle()
        return fromPixels(pixels)
    }

    fun fromPixels(pixels: IntArray): Long {
        require(pixels.size == 72)
        val gray = IntArray(72) { index ->
            val color = pixels[index]
            (((color ushr 16) and 0xff) * 299 + ((color ushr 8) and 0xff) * 587 + (color and 0xff) * 114) / 1000
        }
        var hash = 0L
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val bit = if (gray[y * 9 + x] > gray[y * 9 + x + 1]) 1 else 0
                hash = (hash shl 1) or bit.toLong()
            }
        }
        return hash
    }

    fun distance(first: Long, second: Long): Int = java.lang.Long.bitCount(first xor second)
}
