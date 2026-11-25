package com.classroll.photo.data

import com.classroll.photo.model.CourseSchedule
import com.classroll.photo.model.TimetableGroups
import com.classroll.photo.model.TimetableProfile
import com.classroll.photo.model.TimetableSection
import java.time.LocalDate

/**
 * 河南师范大学 2026-2027-1 学期的两套作息。
 * 教务导出 .xls 不带上课时间，导入时直接使用这两套；
 * 已有课表或 JSON 自带作息时以现有数据为准。
 */
object BuiltinTimetables {
    val henanNormal: List<TimetableProfile> = listOf(
        profile(
            effectiveFrom = "2026-05-06",
            name = "夏季作息时间（夏令时）",
            times = listOf(
                "08:00" to "08:45",
                "08:55" to "09:40",
                "10:10" to "10:55",
                "11:05" to "11:50",
                "15:00" to "15:45",
                "15:55" to "16:40",
                "17:10" to "17:55",
                "18:05" to "18:50",
                "20:00" to "20:45",
                "20:55" to "21:40",
            ),
        ),
        profile(
            effectiveFrom = "2026-10-08",
            name = "秋季作息时间（冬令时）",
            times = listOf(
                "08:00" to "08:45",
                "08:55" to "09:40",
                "10:10" to "10:55",
                "11:05" to "11:50",
                "14:30" to "15:15",
                "15:25" to "16:10",
                "16:40" to "17:25",
                "17:35" to "18:20",
                "19:30" to "20:15",
                "20:25" to "21:10",
            ),
        ),
    )

    /** 旧版本 xls 导入生成的「默认作息」（2000-01-01）替换成内置两套，返回 null 表示无需迁移。 */
    fun migrateLegacyDefault(schedule: CourseSchedule?): CourseSchedule? {
        val legacy = schedule?.timeProfiles?.singleOrNull()?.effectiveFrom == LocalDate.of(2000, 1, 1)
        return if (legacy) schedule.copy(timeProfiles = henanNormal) else null
    }

    private fun profile(
        effectiveFrom: String,
        name: String,
        times: List<Pair<String, String>>,
    ): TimetableProfile = TimetableProfile(
        effectiveFrom = LocalDate.parse(effectiveFrom),
        name = name,
        sections = times.mapIndexed { index, (start, end) ->
            TimetableSection(index + 1, minuteOf(start), minuteOf(end))
        },
        groups = TimetableGroups(4, 4, 2),
    )

    private fun minuteOf(text: String): Int {
        val (hour, minute) = text.split(":").map { it.toInt() }
        return hour * 60 + minute
    }
}
