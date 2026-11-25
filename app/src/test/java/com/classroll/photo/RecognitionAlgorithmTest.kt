package com.classroll.photo

import com.classroll.photo.model.CoarseEvidence
import com.classroll.photo.model.OcrEvidence
import com.classroll.photo.model.TextBox
import com.classroll.photo.model.Verdict
import com.classroll.photo.recognition.PerceptualHash
import com.classroll.photo.recognition.RecognitionRules
import com.classroll.photo.recognition.ScreenAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionAlgorithmTest {
    @Test
    fun perceptualHashIsStableAndDetectsDifference() {
        val first = IntArray(72) { 0xFF202020.toInt() }
        val second = first.copyOf()
        assertEquals(PerceptualHash.fromPixels(first), PerceptualHash.fromPixels(second))
        second[0] = 0xFFE0E0E0.toInt()
        assertTrue(PerceptualHash.distance(PerceptualHash.fromPixels(first), PerceptualHash.fromPixels(second)) > 0)
    }

    @Test
    fun brightRectangleScoresHigherThanFlatWhitePage() {
        val rectangle = IntArray(100 * 100) { 0xFF181818.toInt() }
        for (y in 20 until 80) {
            for (x in 10 until 90) rectangle[y * 100 + x] = 0xFFF5F5F5.toInt()
        }
        val whitePage = IntArray(100 * 100) { 0xFFF5F5F5.toInt() }
        val rectangleScore = ScreenAnalyzer.analyzePixels(100, 100, rectangle).score
        val whiteScore = ScreenAnalyzer.analyzePixels(100, 100, whitePage).score
        assertTrue("rectangle=$rectangleScore white=$whiteScore", rectangleScore > whiteScore)
    }

    @Test
    fun noTextAloneDoesNotMeanUnlike() {
        val decision = RecognitionRules.afterOcr(CoarseEvidence(), OcrEvidence(), false)
        assertEquals(Verdict.Doubtful, decision.verdict)
        assertFalse(decision.needsOcr)
    }

    @Test
    fun strongFaceCanSkipOcrButBoundaryStillGetsOcr() {
        val face = CoarseEvidence(faceCount = 1, maxFaceAreaRatio = .3f)
        assertEquals(Verdict.Unlike, RecognitionRules.afterCoarse(face, false).verdict)
        assertFalse(RecognitionRules.afterCoarse(face, false).needsOcr)
        assertTrue(RecognitionRules.afterCoarse(face, true).needsOcr)
    }

    @Test
    fun denseRecognizedTextBecomesLikely() {
        val decision = RecognitionRules.afterOcr(
            CoarseEvidence(screenScore = .2f),
            slideEvidence(
                List(12) { index -> "第一节 社会运行的条件与机制 $index" },
            ),
            false,
        )
        assertEquals(Verdict.Likely, decision.verdict)
    }

    @Test
    fun screenshotIsSkippedBeforeOcr() {
        val decision = RecognitionRules.afterCoarse(
            CoarseEvidence(screenScore = .3f),
            false,
            displayName = "Screenshot_2026-11-25-09-00-00_com.tencent.mm.jpg",
        )
        assertEquals(Verdict.Unlike, decision.verdict)
        assertFalse(decision.needsOcr)
    }

    @Test
    fun chatScreenshotIsNotScanned() {
        val decision = RecognitionRules.afterOcr(
            CoarseEvidence(screenScore = .3f),
            slideEvidence(List(18) { "好的" }),
            false,
            displayName = "Screenshot_2026-09-22-15-45-29_com.tencent.mm.jpg",
        )
        assertEquals(Verdict.Unlike, decision.verdict)
    }

    @Test
    fun screenshotOfSlideIsNotScanned() {
        val decision = RecognitionRules.afterOcr(
            CoarseEvidence(screenScore = .3f),
            slideEvidence(List(12) { index -> "第三章 思想政治教育的过程与规律 $index" }),
            false,
            displayName = "Screenshot_2026-11-24-10-24-01_com.chaoxing.mooc.jpg",
        )
        assertEquals(Verdict.Unlike, decision.verdict)
    }

    @Test
    fun wechatSavedSlideIsStillLikely() {
        val decision = RecognitionRules.afterOcr(
            CoarseEvidence(screenScore = .3f),
            slideEvidence(List(6) { index -> "马克思主义哲学基本原理与方法的系统阐述 $index" }),
            false,
            displayName = "mmexport1795420013615.jpg",
        )
        assertEquals(Verdict.Likely, decision.verdict)
    }

    @Test
    fun largeGlyphNotesBecomeLikely() {
        val lines = List(11) { index -> if (index < 8) "六个字符内容" else "八个字符的内容啊" }
        val decision = RecognitionRules.afterOcr(
            CoarseEvidence(screenScore = .2f),
            slideEvidence(lines),
            false,
            displayName = "IMG_20260910_101225.jpg",
        )
        assertEquals(Verdict.Likely, decision.verdict)
    }

    @Test
    fun gameCardWithSmallGlyphsStaysDoubtful() {
        val lines = List(32) { index -> if (index < 24) "六个字符内容" else "七个字符的内容" }
        val evidence = slideEvidence(lines, boxHeight = 0.022f)
        val decision = RecognitionRules.afterOcr(
            CoarseEvidence(screenScore = .2f),
            evidence,
            false,
            displayName = "heybox_1790762248483_-1425798064.png",
        )
        assertEquals(Verdict.Doubtful, decision.verdict)
    }

    @Test
    fun statusBarTimesStayDoubtful() {
        val lines = List(12) { index -> "12:${index.toString().padStart(2, '0')} 马克思主义哲学原理" }
        val decision = RecognitionRules.afterOcr(
            CoarseEvidence(screenScore = .3f),
            slideEvidence(lines),
            false,
            displayName = "IMG_20261124_101032.jpg",
        )
        assertEquals(Verdict.Doubtful, decision.verdict)
    }

    @Test
    fun textPinnedToTopEdgeStaysDoubtful() {
        val decision = RecognitionRules.afterOcr(
            CoarseEvidence(screenScore = .3f),
            slideEvidence(
                List(12) { index -> "第三章 思想政治教育的过程与规律 $index" },
                topStart = 0f,
                topStep = 0.004f,
            ),
            false,
            displayName = "IMG_20261124_101032.jpg",
        )
        assertEquals(Verdict.Doubtful, decision.verdict)
    }

    @Test
    fun sparseTextStaysDoubtful() {
        val decision = RecognitionRules.afterOcr(
            CoarseEvidence(screenScore = .2f),
            slideEvidence(listOf("第一节 社会运行", "一、条件")),
            false,
        )
        assertEquals(Verdict.Doubtful, decision.verdict)
    }

    private fun slideEvidence(
        lines: List<String>,
        topStart: Float = 0.08f,
        topStep: Float = 0.055f,
        boxHeight: Float = 0.04f,
    ): OcrEvidence {
        val text = lines.joinToString("\n")
        val boxes = lines.mapIndexed { index, _ ->
            val top = topStart + index * topStep
            TextBox(left = 0.08f, top = top, right = 0.92f, bottom = top + boxHeight)
        }
        return OcrEvidence(
            text = text,
            characterCount = text.count { !it.isWhitespace() },
            textAreaRatio = 0.2f,
            boxes = boxes,
        )
    }
}
