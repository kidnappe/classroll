package com.classroll.photo.data

import com.classroll.photo.model.Course
import com.classroll.photo.model.CourseSchedule
import com.classroll.photo.model.TimetableProfile
import com.classroll.photo.model.TimetableSection
import jxl.Workbook
import java.io.ByteArrayInputStream
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 解析教务系统导出的周视图 .xls：
 * 每个格子形如 课程名[类型]\n教师\n周次-节次\n教室，列按 周一..周日 循环。
 * xls 不含作息时间，所以沿用当前课表的作息；没有就生成一套默认作息。
 */
object XlsScheduleParser {
    private val tokenPattern = Regex("""^(\d+)-(\d{4})$""")
    private val nameTypePattern = Regex("""^(.*?)\s*\[(.*?)]\s*$""")
    private val termPattern = Regex("""(\d{4})-(\d{4})-(\d+)""")
    private val weekHeaderPattern = Regex("""第\s*(\d+)\s*周""")
    private val datePattern = Regex("""^(\d{1,2})-(\d{1,2})$""")

    private data class ParsedCell(
        val week: Int,
        val weekday: Int,
        val sectionStart: Int,
        val sectionEnd: Int,
        val name: String,
        val teacher: String,
        val room: String,
        val type: String,
    )

    fun parse(bytes: ByteArray, base: CourseSchedule?): CourseSchedule {
        val workbook = try {
            Workbook.getWorkbook(ByteArrayInputStream(bytes))
        } catch (error: Exception) {
            throw IllegalArgumentException("读不了这个 Excel，请确认是教务导出的 .xls 课表")
        }
        try {
            val sheet = workbook.sheets.firstOrNull { it.rows > 0 && it.columns > 0 }
                ?: throw IllegalArgumentException("Excel 里没有内容")
            var title = ""
            var headerMaxWeek = 0
            var firstDate: String? = null
            val grouped = LinkedHashMap<String, Pair<ParsedCell, MutableSet<Int>>>()

            for (row in 0 until sheet.rows) {
                for (col in 0 until sheet.columns) {
                    val text = sheet.getCell(col, row).contents?.trim().orEmpty()
                    if (text.isEmpty()) continue
                    if (title.isEmpty() && text.contains("课表")) title = text
                    weekHeaderPattern.findAll(text).forEach { match ->
                        headerMaxWeek = maxOf(headerMaxWeek, match.groupValues[1].toIntOrNull() ?: 0)
                    }
                    if (col == 1 && firstDate == null && datePattern.matches(text)) {
                        firstDate = text
                    }
                    if (col < 1) continue
                    for (cell in parseCourseCells(text, (col - 1) % 7 + 1)) {
                        val key = listOf(
                            cell.name, cell.weekday, cell.sectionStart, cell.sectionEnd,
                            cell.teacher, cell.room, cell.type,
                        ).joinToString("|")
                        val existing = grouped[key]
                        if (existing == null) {
                            grouped[key] = cell to mutableSetOf(cell.week)
                        } else {
                            existing.second += cell.week
                        }
                    }
                }
            }

            if (grouped.isEmpty()) {
                throw IllegalArgumentException("没有识别到课程，请确认是教务系统导出的周课表")
            }

            val termName = termPattern.find(title)?.value
            val startYear = termName?.take(4)?.toIntOrNull() ?: LocalDate.now().year
            val semesterStart = parseFirstMonday(firstDate, startYear)
            val courses = grouped.values.mapIndexed { index, (cell, weeks) ->
                Course(
                    id = "xls-" + Integer.toHexString("${cell.name}|${cell.weekday}|${cell.sectionStart}|${cell.sectionEnd}|$index".hashCode()),
                    name = cell.name,
                    weekday = cell.weekday,
                    sections = (cell.sectionStart..cell.sectionEnd).toList(),
                    weeks = weeks.toSet(),
                    room = cell.room,
                    type = cell.type,
                    teacher = cell.teacher,
                )
            }.sortedWith(compareBy({ it.weekday }, { it.sections.firstOrNull() ?: 0 }, { it.name }))
            val totalWeeks = courses.maxOfOrNull { it.weeks.maxOrNull() ?: 0 } ?: headerMaxWeek
            val profiles = base?.timeProfiles?.takeIf { it.isNotEmpty() }
                ?: BuiltinTimetables.henanNormal

            return CourseSchedule(
                semesterStart = semesterStart,
                bufferBeforeMin = base?.bufferBeforeMin ?: 10,
                bufferAfterMin = base?.bufferAfterMin ?: 20,
                courses = courses,
                timeProfiles = profiles,
                termName = termName ?: "$startYear-${startYear + 1}-1",
                totalWeeks = totalWeeks,
                skipDates = emptySet(),
                skipRanges = emptyList(),
                schemaVersion = CourseScheduleJson.SCHEMA_VERSION,
                currentWeekOverride = null,
            )
        } finally {
            workbook.close()
        }
    }

    /**
     * 一个格子可能叠放多门课（如「第6周周一7-8」同时有马哲和马政经），
     * 每门课固定为 课程名/教师/周次-节次/[教室] 四行，教室行可能为空。
     */
    private fun parseCourseCells(text: String, weekday: Int): List<ParsedCell> {
        if (!text.contains('\n') && !Regex("""\d-\d{4}""").containsMatchIn(text)) return emptyList()
        val values = text.replace("\r", "").split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        val cells = mutableListOf<ParsedCell>()
        values.forEachIndexed { index, line ->
            val match = tokenPattern.matchEntire(line) ?: return@forEachIndexed
            if (index < 2) return@forEachIndexed
            val week = match.groupValues[1].toIntOrNull() ?: return@forEachIndexed
            val sectionText = match.groupValues[2]
            val sectionStart = sectionText.take(2).toIntOrNull() ?: return@forEachIndexed
            val sectionEnd = sectionText.takeLast(2).toIntOrNull() ?: return@forEachIndexed
            if (sectionStart <= 0 || sectionEnd < sectionStart) return@forEachIndexed
            val nameLine = values[index - 2]
            val nameMatch = nameTypePattern.matchEntire(nameLine)
            val name = (nameMatch?.groupValues?.get(1) ?: nameLine).trim()
            if (name.isEmpty()) return@forEachIndexed
            val type = nameMatch?.groupValues?.get(2)?.trim().orEmpty()
            val teacher = values[index - 1].trim()
            // 下一门课的教师在两行之后；若隔两行仍是节次标记，则当前行是下一门课名，不是教室。
            val nextIsCourseName = index + 3 < values.size && tokenPattern.matches(values[index + 3])
            val room = if (!nextIsCourseName) values.getOrNull(index + 1).orEmpty() else ""
            cells += ParsedCell(week, weekday, sectionStart, sectionEnd, name, teacher, room, type)
        }
        return cells
    }

    private fun parseFirstMonday(raw: String?, year: Int): LocalDate {
        val match = raw?.let { datePattern.matchEntire(it) } ?: return LocalDate.of(year, 9, 7)
        val month = match.groupValues[1].toIntOrNull() ?: return LocalDate.of(year, 9, 7)
        val day = match.groupValues[2].toIntOrNull() ?: return LocalDate.of(year, 9, 7)
        val actualYear = if (month >= 8) year else year + 1
        val date = runCatching { LocalDate.of(actualYear, month, day) }.getOrElse { return LocalDate.of(year, 9, 7) }
        return date.with(DayOfWeek.MONDAY)
    }

}
