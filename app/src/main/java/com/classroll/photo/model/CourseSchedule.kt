package com.classroll.photo.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Course(
    val id: String,
    val name: String,
    val weekday: Int,
    /** 上课节次，允许不连续，例如 [1, 3, 5]；为空时直接用 [startMinute]/[endMinute]。 */
    val sections: List<Int> = emptyList(),
    val startMinute: Int = 0,
    val endMinute: Int = 0,
    val weeks: Set<Int>,
    val room: String,
    val campus: String = "",
    val type: String = "",
    val teacher: String = "",
) {
    val usesSections: Boolean get() = sections.isNotEmpty()

    val sectionLabel: String
        get() = if (usesSections) {
            "第${sections.joinToString("、")}节"
        } else {
            "%02d:%02d–%02d:%02d".format(startMinute / 60, startMinute % 60, endMinute / 60, endMinute % 60)
        }
}

data class TimetableSection(
    val index: Int,
    val startMinute: Int,
    val endMinute: Int,
)

/** 上午/下午/晚上的节数，只用于展示分组，不参与时间匹配。 */
data class TimetableGroups(
    val morning: Int = 4,
    val afternoon: Int = 4,
    val evening: Int = 2,
) {
    val total: Int get() = morning + afternoon + evening
}

enum class SectionPart(val label: String) { Morning("上午"), Afternoon("下午"), Evening("晚上") }

data class TimetableProfile(
    val effectiveFrom: LocalDate,
    val name: String,
    val sections: List<TimetableSection>,
    val groups: TimetableGroups = TimetableGroups(),
) {
    fun partOf(sectionIndex: Int): SectionPart = when {
        sectionIndex <= groups.morning -> SectionPart.Morning
        sectionIndex <= groups.morning + groups.afternoon -> SectionPart.Afternoon
        else -> SectionPart.Evening
    }
}

data class TimeSegment(val startMinute: Int, val endMinute: Int)

data class SkipRange(val start: LocalDate, val end: LocalDate) {
    val days: Int get() = (java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt() + 1).coerceAtLeast(1)

    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    fun dates(): Set<LocalDate> {
        val result = mutableSetOf<LocalDate>()
        var cursor = start
        var guard = 0
        while (!cursor.isAfter(end) && guard < 366) {
            result += cursor
            cursor = cursor.plusDays(1)
            guard++
        }
        return result
    }
}

data class CourseSchedule(
    val semesterStart: LocalDate,
    val bufferBeforeMin: Int,
    val bufferAfterMin: Int,
    val courses: List<Course>,
    val timeProfiles: List<TimetableProfile> = emptyList(),
    /** 学期名，例如 2026-2027-1；归档目录会用它分学期。 */
    val termName: String = "",
    val totalWeeks: Int = 0,
    val skipDates: Set<LocalDate> = emptySet(),
    val skipRanges: List<SkipRange> = emptyList(),
    val schemaVersion: Int = 2,
    /** 手动指定的当前周次；为空时按今天和学期起点自动计算。 */
    val currentWeekOverride: Int? = null,
) {
    val uniqueCourseCount: Int
        get() = courses.distinctBy { it.name }.size

    /** 单日停课 + 区间停课展开后的全部日期。 */
    val allSkipDates: Set<LocalDate>
        get() = skipDates + skipRanges.flatMap { it.dates() }

    /** 归档时使用的学期目录名；没有学期名就退回开学日期。 */
    val archiveTerm: String
        get() = (termName.ifBlank { semesterStart.toString() })
            .replace(Regex("[\\\\/:*?\"<>|\\r\\n\\t]"), "_")
            .trim()
            .ifBlank { semesterStart.toString() }

    fun sessions(from: LocalDate, to: LocalDate): List<CourseSession> {
        if (courses.isEmpty() || from.isAfter(to)) return emptyList()
        val weeksTo = ((java.time.temporal.ChronoUnit.DAYS.between(semesterStart, to) / 7) + 1).toInt()
        val lastWeek = if (totalWeeks > 0) minOf(weeksTo, totalWeeks) else weeksTo
        val skippedDates = allSkipDates
        return courses.flatMap { course ->
            course.weeks
                .filter { it in 1..lastWeek }
                .sorted()
                .mapNotNull { week ->
                    val date = semesterStart.plusWeeks((week - 1).toLong()).plusDays((course.weekday - 1).toLong())
                    if (date.isBefore(from) || date.isAfter(to)) return@mapNotNull null
                    if (date in skippedDates) return@mapNotNull null
                    val segments = resolveSegments(course, date)
                    if (segments.isEmpty()) return@mapNotNull null
                    val firstStart = segments.first().startMinute
                    val lastEnd = segments.last().endMinute
                    if (lastEnd <= firstStart) return@mapNotNull null
                    val start = date.atTime(firstStart / 60, firstStart % 60)
                    val end = date.atTime(lastEnd / 60, lastEnd % 60)
                    CourseSession(
                        id = "${course.id}-$week",
                        courseId = course.id,
                        courseName = course.name,
                        week = week,
                        number = week,
                        room = course.room,
                        start = start,
                        end = end,
                        windowStart = start.minusMinutes(bufferBeforeMin.toLong()),
                        windowEnd = end.plusMinutes(bufferAfterMin.toLong()),
                        segments = segments.map { segment ->
                            val segStart = date.atTime(segment.startMinute / 60, segment.startMinute % 60)
                            val segEnd = date.atTime(segment.endMinute / 60, segment.endMinute % 60)
                            SessionSegment(
                                start = segStart,
                                end = segEnd,
                                windowStart = segStart.minusMinutes(bufferBeforeMin.toLong()),
                                windowEnd = segEnd.plusMinutes(bufferAfterMin.toLong()),
                            )
                        },
                    )
                }
        }.sortedBy { it.start }
    }

    /**
     * 找出这节课在 [date] 当天使用的时间段。
     * 优先用当天已生效、且包含全部节次的作息；该作息缺节次时依次向前/向后找能覆盖的作息。
     */
    fun resolveSegments(course: Course, date: LocalDate): List<TimeSegment> {
        if (!course.usesSections) {
            return if (course.endMinute > course.startMinute) listOf(TimeSegment(course.startMinute, course.endMinute)) else emptyList()
        }
        if (timeProfiles.isEmpty()) return emptyList()
        val ordered = timeProfiles
            .filter { !it.effectiveFrom.isAfter(date) }
            .sortedByDescending { it.effectiveFrom } +
            timeProfiles
                .filter { it.effectiveFrom.isAfter(date) }
                .sortedBy { it.effectiveFrom }
        for (profile in ordered) {
            val segments = course.sections.mapNotNull { index ->
                profile.sections.firstOrNull { it.index == index }?.let { TimeSegment(it.startMinute, it.endMinute) }
            }
            if (segments.size == course.sections.size) return segments
        }
        return emptyList()
    }
}

data class SessionSegment(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val windowStart: LocalDateTime,
    val windowEnd: LocalDateTime,
)

data class CourseSession(
    val id: String,
    val courseId: String,
    val courseName: String,
    val week: Int,
    val number: Int,
    val room: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val windowStart: LocalDateTime,
    val windowEnd: LocalDateTime,
    /** 不连续节次的多段窗口；为空时按 start/end 单段处理。 */
    val segments: List<SessionSegment> = emptyList(),
) {
    fun effectiveSegments(): List<SessionSegment> = if (segments.isNotEmpty()) {
        segments
    } else {
        listOf(SessionSegment(start, end, windowStart, windowEnd))
    }
}

enum class TimeMatchKind { Core, Buffer, Overlap }

data class CourseTimeMatch(
    val session: CourseSession,
    val kind: TimeMatchKind,
)
