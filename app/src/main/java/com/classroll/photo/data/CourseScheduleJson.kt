package com.classroll.photo.data

import com.classroll.photo.model.Course
import com.classroll.photo.model.CourseSchedule
import com.classroll.photo.model.SkipRange
import com.classroll.photo.model.TimetableGroups
import com.classroll.photo.model.TimetableProfile
import com.classroll.photo.model.TimetableSection
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

object CourseScheduleJson {
    const val SCHEMA_VERSION = 2

    fun parseSchedule(raw: String): CourseSchedule {
        val root = runCatching { JSONObject(raw) }.getOrElse { throw IllegalArgumentException("课程文件不是有效的 JSON") }
        val schemaVersion = root.optInt("schemaVersion", 1)
        if (schemaVersion > SCHEMA_VERSION) {
            throw IllegalArgumentException("课程文件版本 $schemaVersion 高于当前 App 支持的 $SCHEMA_VERSION")
        }
        val term = root.optJSONObject("term")
        val semesterRaw = term?.optString("start")?.trim().orEmpty().ifBlank { root.optString("semesterStart").trim() }
        if (semesterRaw.isEmpty()) throw IllegalArgumentException("课程文件缺少第一周周一（term.start 或 semesterStart）")
        val semesterStart = parseDate(semesterRaw, "第一周周一")
        if (semesterStart.dayOfWeek != DayOfWeek.MONDAY) {
            throw IllegalArgumentException("第一周周一必须是周一")
        }
        val termName = term?.optString("name")?.trim().orEmpty().ifBlank { root.optString("termName").trim() }
        val totalWeeks = (term?.optInt("totalWeeks", 0) ?: 0).takeIf { it > 0 } ?: root.optInt("totalWeeks", 0)
        if (totalWeeks !in 0..60) throw IllegalArgumentException("学期总周数必须在 0–60 之间")
        val before = root.optInt("bufferBeforeMin", 10)
        val after = root.optInt("bufferAfterMin", 20)
        if (before !in 0..120 || after !in 0..120) {
            throw IllegalArgumentException("课前、课后缓冲必须在 0–120 分钟之间")
        }
        val skipDates = parseSkipDates(root.optJSONArray("skipDates"))
        val skipRanges = parseSkipRanges(root.optJSONArray("skipRanges"))
        val currentWeek = root.optInt("currentWeek", 0).takeIf { it in 1..60 }
        val profiles = parseProfiles(root.optJSONArray("timeProfiles"))
        val coursesJson = root.optJSONArray("courses")
            ?: throw IllegalArgumentException("课程文件缺少 courses 列表")
        if (coursesJson.length() == 0) throw IllegalArgumentException("课程文件里没有课程")
        val usedIds = mutableSetOf<String>()
        val courses = (0 until coursesJson.length()).map { index ->
            val item = coursesJson.optJSONObject(index)
                ?: throw IllegalArgumentException("第 ${index + 1} 门课程格式不正确")
            val name = item.optString("name").trim()
            if (name.isEmpty()) throw IllegalArgumentException("第 ${index + 1} 门课程没有名称")
            val weekday = item.optInt("weekday", 0)
            if (weekday !in 1..7) throw IllegalArgumentException("「$name」的星期必须在 1–7 之间")
            val weeks = parseWeeks(item.optString("weeks"), name)
            val sections = parseSections(item, name)
            val start: Int
            val end: Int
            if (sections.isEmpty()) {
                if (item.has("start") && item.has("end")) {
                    start = parseMinute(item.optString("start"), "「$name」开始时间")
                    end = parseMinute(item.optString("end"), "「$name」结束时间")
                } else {
                    throw IllegalArgumentException("「$name」缺少上课节次（sections）或起止时间（start/end）")
                }
                if (end <= start) throw IllegalArgumentException("「$name」的结束时间必须晚于开始时间")
            } else {
                if (profiles.isEmpty()) {
                    throw IllegalArgumentException("「$name」使用上课节次，但课程文件没有 timeProfiles 作息")
                }
                val covered = profiles.any { profile ->
                    sections.all { index -> profile.sections.any { it.index == index } }
                }
                if (!covered) {
                    throw IllegalArgumentException("「$name」的节次 ${sections.joinToString("、")} 不在任何一套作息里")
                }
                start = 0
                end = 0
            }
            val explicitId = item.optString("id").trim()
            val baseId = explicitId.ifBlank {
                "c" + Integer.toHexString(
                    "$name|$weekday|${sections.joinToString(",")}|${start}|${end}|${weeks.sorted().joinToString(",")}".hashCode(),
                )
            }
            var id = baseId
            var suffix = 2
            while (!usedIds.add(id)) {
                id = "$baseId-${suffix++}"
            }
            Course(
                id = id,
                name = name,
                weekday = weekday,
                sections = sections,
                startMinute = start,
                endMinute = end,
                weeks = weeks,
                room = item.optString("room").trim(),
                campus = item.optString("campus").trim(),
                type = item.optString("type").trim(),
                teacher = item.optString("teacher").trim(),
            )
        }
        return CourseSchedule(
            semesterStart = semesterStart,
            bufferBeforeMin = before,
            bufferAfterMin = after,
            courses = courses,
            timeProfiles = profiles,
            termName = termName,
            totalWeeks = totalWeeks,
            skipDates = skipDates,
            skipRanges = skipRanges,
            schemaVersion = SCHEMA_VERSION,
            currentWeekOverride = currentWeek,
        )
    }

    fun serializeSchedule(schedule: CourseSchedule): String {
        val profiles = JSONArray()
        schedule.timeProfiles.sortedBy { it.effectiveFrom }.forEach { profile ->
            val sections = JSONArray()
            profile.sections.sortedBy { it.index }.forEach { section ->
                sections.put(
                    JSONObject()
                        .put("index", section.index)
                        .put("start", minuteLabel(section.startMinute))
                        .put("end", minuteLabel(section.endMinute)),
                )
            }
            profiles.put(
                JSONObject()
                    .put("effectiveFrom", profile.effectiveFrom.toString())
                    .put("name", profile.name)
                    .put(
                        "groups",
                        JSONObject()
                            .put("morning", profile.groups.morning)
                            .put("afternoon", profile.groups.afternoon)
                            .put("evening", profile.groups.evening),
                    )
                    .put("sections", sections),
            )
        }
        val skip = JSONArray()
        schedule.skipDates.sorted().forEach { skip.put(it.toString()) }
        val skipRanges = JSONArray()
        schedule.skipRanges.sortedBy { it.start }.forEach { range ->
            skipRanges.put(JSONObject().put("start", range.start.toString()).put("end", range.end.toString()))
        }
        val courses = JSONArray()
        schedule.courses.forEach { course ->
            val item = JSONObject()
                .put("id", course.id)
                .put("name", course.name)
                .put("weekday", course.weekday)
                .put("weeks", course.weeks.sorted().joinToString(","))
                .put("room", course.room)
            if (course.campus.isNotBlank()) item.put("campus", course.campus)
            if (course.type.isNotBlank()) item.put("type", course.type)
            if (course.teacher.isNotBlank()) item.put("teacher", course.teacher)
            if (course.usesSections) {
                item.put("sections", JSONArray(course.sections.sorted()))
            } else {
                item.put("start", minuteLabel(course.startMinute))
                item.put("end", minuteLabel(course.endMinute))
            }
            courses.put(item)
        }
        val root = JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put(
                "term",
                JSONObject()
                    .put("name", schedule.termName)
                    .put("start", schedule.semesterStart.toString())
                    .put("totalWeeks", schedule.totalWeeks),
            )
            .put("bufferBeforeMin", schedule.bufferBeforeMin)
            .put("bufferAfterMin", schedule.bufferAfterMin)
            .put("skipDates", skip)
            .put("skipRanges", skipRanges)
            .put("timeProfiles", profiles)
            .put("courses", courses)
        schedule.currentWeekOverride?.let { root.put("currentWeek", it) }
        return root.toString(2)
    }

    private fun parseProfiles(array: JSONArray?): List<TimetableProfile> {
        if (array == null || array.length() == 0) return emptyList()
        return (0 until array.length()).map { index ->
            val item = array.optJSONObject(index)
                ?: throw IllegalArgumentException("第 ${index + 1} 套作息格式不正确")
            val effectiveFrom = parseDate(item.optString("effectiveFrom"), "第 ${index + 1} 套作息的生效日期")
            val sectionsJson = item.optJSONArray("sections")
                ?: throw IllegalArgumentException("「${item.optString("name", "作息")}」缺少 sections")
            if (sectionsJson.length() == 0) throw IllegalArgumentException("「${item.optString("name", "作息")}」没有节次时间")
            val sections = (0 until sectionsJson.length()).map { sectionIndex ->
                val section = sectionsJson.optJSONObject(sectionIndex)
                    ?: throw IllegalArgumentException("作息节次格式不正确")
                val number = section.optInt("index", 0)
                if (number !in 1..40) throw IllegalArgumentException("作息节次编号必须在 1–40 之间")
                val start = parseMinute(section.optString("start"), "作息第 $number 节开始时间")
                val end = parseMinute(section.optString("end"), "作息第 $number 节结束时间")
                if (end <= start) throw IllegalArgumentException("作息第 $number 节时间不正确")
                TimetableSection(number, start, end)
            }
            if (sections.map { it.index }.distinct().size != sections.size) {
                throw IllegalArgumentException("作息里有重复节次")
            }
            TimetableProfile(
                effectiveFrom = effectiveFrom,
                name = item.optString("name").trim().ifBlank { "作息" },
                sections = sections.sortedBy { it.index },
                groups = parseGroups(item.optJSONObject("groups")),
            )
        }.sortedBy { it.effectiveFrom }
    }

    private fun parseGroups(json: JSONObject?): TimetableGroups {
        if (json == null) return TimetableGroups()
        val morning = json.optInt("morning", 4).coerceAtLeast(0)
        val afternoon = json.optInt("afternoon", 4).coerceAtLeast(0)
        val evening = json.optInt("evening", 2).coerceAtLeast(0)
        val default = TimetableGroups()
        return if (morning + afternoon + evening <= 0) default else TimetableGroups(morning, afternoon, evening)
    }

    private fun parseSections(item: JSONObject, courseName: String): List<Int> {
        val array = item.optJSONArray("sections")
        if (array != null && array.length() > 0) {
            val result = (0 until array.length()).map { index ->
                val value = array.optInt(index, 0)
                if (value !in 1..40) throw IllegalArgumentException("「$courseName」的节次必须在 1–40 之间")
                value
            }
            if (result.distinct().size != result.size) throw IllegalArgumentException("「$courseName」的节次有重复")
            return result.sorted()
        }
        val text = item.optString("sections").trim()
        if (text.isNotEmpty()) {
            val result = text.split(',', '，', '、').mapNotNull { part ->
                part.trim().toIntOrNull()?.takeIf { it in 1..40 }
                    ?: throw IllegalArgumentException("「$courseName」的节次 $part 不正确")
            }
            if (result.isEmpty()) throw IllegalArgumentException("「$courseName」没有有效节次")
            return result.distinct().sorted()
        }
        val start = item.optInt("sectionStart", 0)
        val end = item.optInt("sectionEnd", 0)
        if (start > 0 && end >= start) {
            if (start !in 1..40 || end !in 1..40) throw IllegalArgumentException("「$courseName」的节次必须在 1–40 之间")
            return (start..end).toList()
        }
        return emptyList()
    }

    private fun parseSkipDates(array: JSONArray?): Set<LocalDate> {
        if (array == null || array.length() == 0) return emptySet()
        return (0 until array.length()).mapNotNull { index ->
            val raw = array.optString(index).trim()
            if (raw.isEmpty()) null else parseDate(raw, "停课日期")
        }.toSet()
    }

    private fun parseSkipRanges(array: JSONArray?): List<SkipRange> {
        if (array == null || array.length() == 0) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val start = parseDate(item.optString("start"), "停课开始日期")
            val end = parseDate(item.optString("end"), "停课结束日期")
            if (end.isBefore(start)) throw IllegalArgumentException("停课结束日期不能早于开始日期")
            if (java.time.temporal.ChronoUnit.DAYS.between(start, end) > 365) {
                throw IllegalArgumentException("停课区间不能超过 365 天")
            }
            SkipRange(start, end)
        }
    }

    private fun parseDate(value: String, label: String): LocalDate {
        val cleaned = value.trim()
            .replace('年', '-')
            .replace('月', '-')
            .replace("日", "")
            .replace('/', '-')
            .replace('.', '-')
        val parts = cleaned.split('-').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size != 3) throw IllegalArgumentException("${label}必须写成日期，例如 2026-09-07")
        val year = parts[0].toIntOrNull()
        val month = parts[1].toIntOrNull()
        val day = parts[2].toIntOrNull()
        if (year == null || month == null || day == null) {
            throw IllegalArgumentException("${label}必须写成日期，例如 2026-09-07")
        }
        return runCatching { LocalDate.of(year, month, day) }
            .getOrElse { throw IllegalArgumentException("${label}不是有效日期：$value") }
    }

    private fun parseMinute(value: String, label: String): Int {
        val raw = value.trim().replace('：', ':')
        val colon = Regex("^(\\d{1,2}):(\\d{1,2})$").matchEntire(raw)
        if (colon != null) {
            val hour = colon.groupValues[1].toInt()
            val minute = colon.groupValues[2].toInt()
            if (hour in 0..23 && minute in 0..59) return hour * 60 + minute
            throw IllegalArgumentException("${label} $value 不正确")
        }
        val digits = Regex("^(\\d{3,4})$").matchEntire(raw)
        if (digits != null) {
            val number = digits.value.toInt()
            val hour = number / 100
            val minute = number % 100
            if (hour in 0..23 && minute in 0..59) return hour * 60 + minute
        }
        throw IllegalArgumentException("${label}必须写成 HH:mm，例如 08:00（当前：$value）")
    }

    private fun parseWeeks(raw: String, courseName: String): Set<Int> {
        val value = raw.trim()
            .replace(" ", "")
            .replace("　", "")
            .replace("，", ",")
            .replace("、", ",")
            .replace("第", "")
            .replace("周", "")
            .replace("（", "(")
            .replace("）", ")")
            .replace("(单)", "单")
            .replace("(双)", "双")
        if (value.isEmpty()) throw IllegalArgumentException("「$courseName」没有填写周次")
        if (value == "单") return (1..52 step 2).toSet()
        if (value == "双") return (2..52 step 2).toSet()
        val result = mutableSetOf<Int>()
        value.split(',').forEach { part ->
            val match = Regex("^(\\d+)(?:-(\\d+))?([单双]?)$").matchEntire(part)
                ?: throw IllegalArgumentException("「$courseName」的周次 $part 不正确")
            val start = match.groupValues[1].toInt()
            val end = match.groupValues[2].ifEmpty { match.groupValues[1] }.toInt()
            if (start !in 1..52 || end !in 1..52 || start > end) {
                throw IllegalArgumentException("「$courseName」的周次范围 $part 不正确")
            }
            val parity = when (match.groupValues[3]) {
                "单" -> 1
                "双" -> 0
                else -> null
            }
            (start..end).filter { parity == null || it % 2 == parity }.forEach(result::add)
        }
        if (result.isEmpty()) throw IllegalArgumentException("「$courseName」没有有效周次")
        return result
    }

    private fun minuteLabel(value: Int): String = "%02d:%02d".format(value / 60, value % 60)
}
