package com.classroll.photo

import com.classroll.photo.data.CourseScheduleJson
import com.classroll.photo.model.TimeMatchKind
import com.classroll.photo.scan.PhotoTimeMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ScheduleV2Test {
    private val v2 = """
        {
          "schemaVersion": 2,
          "term": {"name": "2026-2027-1", "start": "2026-09-07", "totalWeeks": 19},
          "bufferBeforeMin": 10,
          "bufferAfterMin": 20,
          "skipDates": ["2026-09-14"],
          "timeProfiles": [
            {"effectiveFrom":"2026-05-06","name":"夏季","groups":{"morning":4,"afternoon":4,"evening":2},"sections":[
              {"index":1,"start":"08:00","end":"08:45"},
              {"index":2,"start":"08:55","end":"09:40"},
              {"index":3,"start":"10:10","end":"10:55"},
              {"index":4,"start":"11:05","end":"11:50"}
            ]},
            {"effectiveFrom":"2026-10-08","name":"秋季","sections":[
              {"index":1,"start":"08:00","end":"08:45"},
              {"index":2,"start":"08:55","end":"09:40"},
              {"index":3,"start":"10:10","end":"10:55"},
              {"index":4,"start":"11:05","end":"11:50"}
            ]}
          ],
          "courses": [
            {"id":"jump","name":"跳节课","weekday":1,"sections":[1,3],"weeks":"1-19","room":"A","campus":"西校区","type":"理论","teacher":"张"},
            {"id":"normal","name":"普通课","weekday":2,"sections":[1,2],"weeks":"1-19","room":"B"}
          ]
        }
    """.trimIndent()

    @Test
    fun parsesV2Metadata() {
        val schedule = CourseScheduleJson.parseSchedule(v2)
        assertEquals(2, schedule.schemaVersion)
        assertEquals("2026-2027-1", schedule.termName)
        assertEquals(19, schedule.totalWeeks)
        assertEquals(setOf(LocalDate.of(2026, 9, 14)), schedule.skipDates)
        val jump = schedule.courses.first { it.id == "jump" }
        assertEquals(listOf(1, 3), jump.sections)
        assertEquals("西校区", jump.campus)
        assertEquals("理论", jump.type)
        assertEquals(4, schedule.timeProfiles.first().groups.morning)
        assertEquals("2026-2027-1", schedule.archiveTerm)
    }

    @Test
    fun jumpSectionsMatchOnlyRealSegments() {
        val schedule = CourseScheduleJson.parseSchedule(v2)
        val sessions = schedule.sessions(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13))
        val jump = sessions.first { it.courseId == "jump" }
        assertEquals(2, jump.segments.size)
        // 09:20 在两段之间（第 1 节课后缓冲 09:05 已过、第 3 节课前缓冲 10:00 未到），不应匹配
        assertNull(PhotoTimeMatcher.matchPhoto(LocalDateTime.of(2026, 9, 7, 9, 20), sessions))
        assertEquals(TimeMatchKind.Core, PhotoTimeMatcher.matchPhoto(LocalDateTime.of(2026, 9, 7, 10, 30), sessions)?.kind)
    }

    @Test
    fun skipDatesExcludedFromSessions() {
        val schedule = CourseScheduleJson.parseSchedule(v2)
        val sessions = schedule.sessions(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 20))
        assertTrue(sessions.none { it.start.toLocalDate() == LocalDate.of(2026, 9, 14) })
        assertTrue(sessions.any { it.start.toLocalDate() == LocalDate.of(2026, 9, 8) })
    }

    @Test
    fun partialProfileFallsBackToAnotherTimetable() {
        val json = """
            {"schemaVersion":2,"term":{"start":"2026-09-07"},"skipDates":[],
             "timeProfiles":[
               {"effectiveFrom":"2026-05-06","name":"只有1节","sections":[{"index":1,"start":"08:00","end":"08:45"}]},
               {"effectiveFrom":"2026-10-08","name":"完整","sections":[{"index":1,"start":"08:00","end":"08:45"},{"index":2,"start":"08:55","end":"09:40"}]}
             ],
             "courses":[{"name":"跨作息","weekday":1,"sections":[1,2],"weeks":"1-19","room":""}]}
        """.trimIndent()
        val schedule = CourseScheduleJson.parseSchedule(json)
        val session = schedule.sessions(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 7)).single()
        assertEquals("08:55", session.segments[1].start.toLocalTime().toString().take(5))
    }

    @Test
    fun acceptsLenientDateAndTimeFormats() {
        val json = """
            {"semesterStart":"2026/9/7","courses":[
              {"name":"宽松","weekday":1,"start":"8:00","end":"0940","weeks":"第1-3周","room":""},
              {"name":"顿号","weekday":2,"start":"0800","end":"09:40","weeks":"1、3、5","room":""}
            ]}
        """.trimIndent()
        val schedule = CourseScheduleJson.parseSchedule(json)
        assertEquals(LocalDate.of(2026, 9, 7), schedule.semesterStart)
        assertEquals(setOf(1, 2, 3), schedule.courses[0].weeks)
        assertEquals(setOf(1, 3, 5), schedule.courses[1].weeks)
        assertEquals(480, schedule.courses[1].startMinute)
        assertEquals(580, schedule.courses[1].endMinute)
    }

    @Test
    fun rejectsSectionsMissingFromEveryProfile() {
        val json = """
            {"semesterStart":"2026-09-07","timeProfiles":[
              {"effectiveFrom":"2026-05-06","name":"x","sections":[{"index":1,"start":"08:00","end":"08:45"}]}
            ],"courses":[{"name":"缺","weekday":1,"sections":[1,2],"weeks":"1","room":""}]}
        """.trimIndent()
        assertThrows(IllegalArgumentException::class.java) { CourseScheduleJson.parseSchedule(json) }
    }

    @Test
    fun roundTripKeepsV2Fields() {
        val schedule = CourseScheduleJson.parseSchedule(v2)
        val again = CourseScheduleJson.parseSchedule(CourseScheduleJson.serializeSchedule(schedule))
        assertEquals(schedule.termName, again.termName)
        assertEquals(schedule.totalWeeks, again.totalWeeks)
        assertEquals(schedule.skipDates, again.skipDates)
        assertEquals(schedule.courses.first { it.id == "jump" }.sections, again.courses.first { it.id == "jump" }.sections)
        assertEquals(schedule.timeProfiles.first().groups.evening, again.timeProfiles.first().groups.evening)
        assertEquals(schedule.archiveTerm, again.archiveTerm)
    }
}
