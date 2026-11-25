package com.classroll.photo

import com.classroll.photo.model.AnalysisStatus
import com.classroll.photo.model.CoarseEvidence
import com.classroll.photo.model.OcrEvidence
import com.classroll.photo.model.PhotoGroup
import com.classroll.photo.model.PhotoItem
import com.classroll.photo.model.PhotoTimeSource
import com.classroll.photo.model.ScanProgress
import com.classroll.photo.model.ScanStage
import com.classroll.photo.model.TextBox
import com.classroll.photo.model.Verdict
import com.classroll.photo.recognition.TextLayout
import com.classroll.photo.scan.RecordEnv
import com.classroll.photo.scan.RecordTrigger
import com.classroll.photo.scan.ScanRecordSerializer
import com.classroll.photo.scan.ScanSource
import com.classroll.photo.scan.ScanUiState
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ScanRecordTest {
    private val env = RecordEnv(
        device = "Pixel 8",
        manufacturer = "Google",
        androidRelease = "15",
        androidSdk = 35,
        appVersion = "0.2.0",
        generatedAt = "2026-11-25T12:00:00+08:00",
    )

    private val group = PhotoGroup(
        id = "g1",
        course = "社会学概论",
        session = "第 3 周 周三 3-4",
        meta = "",
        week = 3,
        date = LocalDateTime.of(2026, 11, 25, 9, 0),
        boundary = false,
    )

    private fun photo(
        id: String,
        verdict: Verdict,
        lines: List<String>,
        name: String = "IMG_$id.jpg",
        reason: String = "识别理由",
    ): PhotoItem {
        val text = lines.joinToString("\n")
        return PhotoItem(
            id = id,
            imageUri = "content://media/external/images/media/$id",
            displayName = name,
            mediaStoreId = 1L,
            groupId = "g1",
            title = name.substringBeforeLast('.'),
            timeLabel = "11-25 09:05",
            takenAt = LocalDateTime.of(2026, 11, 25, 9, 5),
            timeSource = PhotoTimeSource.Taken,
            status = AnalysisStatus.OcrDone,
            verdict = verdict,
            coarse = CoarseEvidence(screenScore = .42f, faceCount = 1, maxFaceAreaRatio = .02f, brightness = .7f),
            ocr = OcrEvidence(
                text = text,
                characterCount = text.count { !it.isWhitespace() },
                textAreaRatio = .2f,
                boxes = lines.mapIndexed { index, _ ->
                    TextBox(left = .08f, top = .08f + index * .06f, right = .92f, bottom = .12f + index * .06f)
                },
            ),
            reason = reason,
        )
    }

    private fun state(): ScanUiState {
        val slide = List(10) { index -> "第三章 思想政治教育的过程与规律 $index" }
        val photos = listOf(
            photo("p1", Verdict.Likely, slide),
            photo("p2", Verdict.Likely, slide),
            photo("p3", Verdict.Boundary, slide, reason = "含\"引号\"和\n换行"),
        )
        val chat = List(12) { "好的" }
        return ScanUiState(
            progress = ScanProgress(ScanStage.Done, photos.size, photos.size, "识别完成", 12_000),
            photos = photos + photo("p4", Verdict.Doubtful, chat, name = "Screenshot_2026-11-25-09-05-00_com.tencent.mm.jpg"),
            groups = mapOf("g1" to group),
            selectedIds = setOf("p1", "p3"),
            touchedIds = setOf("p3"),
            source = ScanSource.FullAlbum,
            scanId = "20261125-120000-abcd",
            recordRevision = 4,
            confirmed = true,
        )
    }

    @Test
    fun recordEnvNowTextIncludesOffset() {
        val text = RecordEnv.nowText()
        assertTrue("generatedAt=$text", Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{2}:\d{2}""").matches(text))
        val defaulted = RecordEnv(device = "d", manufacturer = "m", androidRelease = "15", androidSdk = 35, appVersion = "1.0")
        assertTrue(defaulted.generatedAt, Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{2}:\d{2}""").matches(defaulted.generatedAt))
    }

    @Test
    fun recordKeepsLabelPolicyAndCounts() {
        val json = JSONObject(ScanRecordSerializer.build(state(), RecordTrigger.ConfirmDone, env, 12_000))
        assertEquals(1, json.getInt("schemaVersion"))
        assertEquals("classroll_scan_record", json.getString("recordType"))
        assertEquals("20261125-120000-abcd", json.getString("scanId"))
        assertEquals("confirm_done", json.getString("trigger"))
        assertEquals(4, json.getInt("revision"))
        assertEquals("Google", json.getJSONObject("env").getString("manufacturer"))

        val scan = json.getJSONObject("scan")
        assertEquals(4, scan.getInt("candidateCount"))
        assertEquals(2, scan.getInt("selectedCount"))
        assertEquals(3, scan.getInt("reviewedCount"))
        assertEquals(0, scan.getInt("scheduleCourseCount"))
        assertEquals(1, scan.getJSONObject("labelCounts").getInt("positive"))
        assertEquals(1, scan.getJSONObject("labelCounts").getInt("negative"))
        assertEquals(1, scan.getJSONObject("labelCounts").getInt("positive_boundary"))
        assertEquals(1, scan.getJSONObject("labelCounts").getInt("unlabeled"))
        assertEquals(2, scan.getJSONObject("tuningSet").getInt("total"))
        assertTrue(scan.getJSONArray("labelPolicy").length() >= 4)
    }

    @Test
    fun itemCarriesVerdictSelectionAndLayout() {
        val json = JSONObject(ScanRecordSerializer.build(state(), RecordTrigger.ScanComplete, env, 1_000))
        val items = json.getJSONArray("items")
        assertEquals(4, items.length())

        val likely = items.getJSONObject(0)
        assertEquals("Likely", likely.getString("verdictAtScan"))
        assertTrue(likely.getBoolean("selected"))
        assertTrue(likely.getBoolean("reviewed"))
        assertTrue(likely.getBoolean("tuningEligible"))
        assertEquals("positive", likely.getString("label"))
        assertEquals(10, likely.getJSONObject("layout").getInt("lineCount"))

        val boundary = items.getJSONObject(2)
        assertTrue(boundary.getBoolean("touched"))
        assertEquals("positive_boundary", boundary.getString("label"))
        assertFalse(boundary.getBoolean("tuningEligible"))
        assertEquals("含\"引号\"和\n换行", boundary.getString("reason"))

        val doubtful = items.getJSONObject(3)
        assertEquals("unlabeled", doubtful.getString("label"))
        assertTrue(doubtful.getJSONObject("layout").getBoolean("screenshotName"))
    }

    @Test
    fun selectionEventContainsFullSelectedSet() {
        val line = ScanRecordSerializer.buildSelectionEvent(state(), "toggle", setOf("p2"), env)
        val json = JSONObject(line)
        assertEquals("classroll_selection_event", json.getString("recordType"))
        assertEquals(4, json.getInt("revision"))
        assertEquals(2, json.getJSONArray("selectedIds").length())
        assertEquals("p2", json.getJSONArray("changedIds").getString(0))
        assertEquals(2, json.getInt("selectedCount"))
    }

    @Test
    fun realDeviceRecordCanBeReserialized() {
        val candidates = listOf(
            java.io.File("../artifacts/real-device/scan-record.json"),
            java.io.File("artifacts/real-device/scan-record.json"),
        )
        val source = candidates.firstOrNull { it.exists() } ?: return
        val raw = org.json.JSONObject(source.readText())
        val array = raw.getJSONArray("items")
        val selected = mutableSetOf<String>()
        val photos = (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            if (item.getBoolean("selected")) selected += item.getString("id")
            val ocr = item.getJSONObject("ocr")
            val boxes = ocr.getJSONArray("boxes")
            val coarse = item.getJSONObject("coarse")
            photo(
                id = item.getString("id"),
                verdict = Verdict.valueOf(item.getString("verdictAtScan")),
                lines = ocr.getString("text").split("\n").filter { it.isNotBlank() }.ifEmpty { listOf("") },
                name = item.getString("displayName"),
                reason = item.getString("reason"),
            ).copy(
                imageUri = item.getString("imageUri"),
                groupId = item.getString("groupId"),
                title = item.getString("title"),
                timeLabel = item.getString("timeLabel"),
                coarse = CoarseEvidence(
                    screenScore = coarse.getDouble("screenScore").toFloat(),
                    faceCount = coarse.getInt("faceCount"),
                    maxFaceAreaRatio = coarse.getDouble("maxFaceAreaRatio").toFloat(),
                    brightness = coarse.getDouble("brightness").toFloat(),
                    perceptualHash = coarse.getLong("perceptualHash"),
                    nearDuplicateCount = coarse.getInt("nearDuplicateCount"),
                ),
                ocr = OcrEvidence(
                    text = ocr.getString("text"),
                    characterCount = ocr.getInt("characterCount"),
                    textAreaRatio = ocr.getDouble("textAreaRatio").toFloat(),
                    boxes = (0 until boxes.length()).map { boxIndex ->
                        val box = boxes.getJSONObject(boxIndex)
                        TextBox(
                            left = box.getDouble("left").toFloat(),
                            top = box.getDouble("top").toFloat(),
                            right = box.getDouble("right").toFloat(),
                            bottom = box.getDouble("bottom").toFloat(),
                        )
                    },
                ),
            )
        }
        val state = ScanUiState(
            progress = ScanProgress(ScanStage.Done, photos.size, photos.size, "识别完成", 148_800),
            photos = photos,
            selectedIds = selected,
            source = ScanSource.FullAlbum,
            scanId = "20261125-120000-abcd",
        )
        val json = JSONObject(ScanRecordSerializer.build(state, RecordTrigger.ScanComplete, env, 148_800))
        assertEquals(photos.size, json.getJSONObject("scan").getInt("candidateCount"))
        assertEquals(selected.size, json.getJSONObject("scan").getInt("selectedCount"))
        assertEquals(photos.size, json.getJSONArray("items").length())
        assertEquals(
            photos.count { it.verdict == Verdict.Likely },
            json.getJSONObject("scan").getJSONObject("verdictCounts").getInt("Likely"),
        )
        assertTrue(ScanRecordSerializer.buildSummary(state, RecordTrigger.ScanComplete, env).isNotBlank())
    }

    @Test
    fun textLayoutMatchesRuleInputs() {
        val ocr = OcrEvidence(
            text = "12:30 马克思主义哲学原理\n第二节 社会运行的条件与机制\n好的",
            characterCount = 30,
            textAreaRatio = .2f,
            boxes = listOf(
                TextBox(0.05f, 0.01f, .95f, .05f),
                TextBox(0.05f, .2f, .95f, .28f),
                TextBox(0.05f, .3f, .2f, .34f),
            ),
        )
        val layout = TextLayout.from(ocr, "Screenshot_20261125.jpg")
        assertEquals(3, layout.lineCount)
        assertEquals(1, layout.clockHits)
        assertEquals(1, layout.headingCount)
        assertTrue(layout.screenshotName)
        assertEquals(1f / 3f, layout.topEdgeRatio, 0.0001f)
        assertEquals(1f / 3f, layout.shortLineRatio, 0.0001f)
        assertEquals(.0533f, layout.averageBoxHeight, 0.0002f)
    }
}
