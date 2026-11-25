package com.classroll.photo

import com.classroll.photo.data.XlsScheduleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XlsScheduleParserTest {
    private val schedule by lazy { XlsScheduleParser.parse(readFixture(), null) }

    private fun readFixture(): ByteArray =
        javaClass.classLoader!!.getResourceAsStream("sample-timetable.xls")!!.readBytes()

    @Test
    fun `导入 xls 时使用内置的河南师大两套作息`() {
        val profiles = schedule.timeProfiles
        assertEquals(2, profiles.size)
        assertEquals("2026-05-06", profiles[0].effectiveFrom.toString())
        assertEquals("2026-10-08", profiles[1].effectiveFrom.toString())
        assertEquals(14 * 60 + 30, profiles[1].sections.first { it.index == 5 }.startMinute)
        assertEquals(16 * 60 + 40, profiles[1].sections.first { it.index == 7 }.startMinute)
    }

    @Test
    fun `旧版默认作息迁移为内置两套`() {
        val legacy = schedule.copy(
            timeProfiles = listOf(
                com.classroll.photo.model.TimetableProfile(
                    effectiveFrom = java.time.LocalDate.of(2000, 1, 1),
                    name = "默认作息",
                    sections = emptyList(),
                ),
            ),
        )
        assertEquals(2, com.classroll.photo.data.BuiltinTimetables.migrateLegacyDefault(legacy)?.timeProfiles?.size)
        assertEquals(null, com.classroll.photo.data.BuiltinTimetables.migrateLegacyDefault(schedule))
    }

    @Test
    fun `周一7-8马克思主义政治经济学覆盖全部周次`() {
        val courses = schedule.courses.filter {
            it.name == "马克思主义政治经济学" && it.weekday == 1 && it.sections == listOf(7, 8)
        }
        assertEquals(1, courses.size)
        assertEquals((1..19).toSet(), courses[0].weeks)
    }

    @Test
    fun `周四5-6大学体育是单独一条且教室为空`() {
        val courses = schedule.courses.filter {
            it.name.startsWith("大学体育") && it.weekday == 4 && it.sections == listOf(5, 6)
        }
        assertEquals(1, courses.size)
        assertEquals("", courses[0].room)
        assertEquals((1..19).toSet(), courses[0].weeks)
    }

    @Test
    fun `同一格里的第二门课不会被吞掉`() {
        val world = schedule.courses.filter { it.name == "世界近现代史" }
        assertEquals(1, world.size)
        assertEquals(4, world[0].weekday)
        assertEquals(listOf(5, 6), world[0].sections)
        assertTrue(world[0].weeks.containsAll(listOf(5, 8, 11, 14, 16, 18)))
        assertEquals("求是中楼202", world[0].room)
        assertFalse(schedule.courses.any { it.room.contains("世界近现代史") })
        assertFalse(schedule.courses.any { it.room.contains("[理论]") || it.room.contains("[技能]") })
    }
}
