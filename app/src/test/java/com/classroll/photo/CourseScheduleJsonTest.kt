package com.classroll.photo

import com.classroll.photo.data.CourseScheduleJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class CourseScheduleJsonTest {
    @Test
    fun parsesChineseScheduleAndWeekPatterns() {
        val schedule = CourseScheduleJson.parseSchedule(
            """
            {
              "semesterStart": "2026-09-07",
              "bufferBeforeMin": 10,
              "bufferAfterMin": 20,
              "courses": [
                {"name":"高等数学","weekday":1,"start":"08:00","end":"09:40","weeks":"1-16单","room":"教三-201"},
                {"name":"大学物理","weekday":3,"start":"10:00","end":"11:40","weeks":"1-8,10-16","room":"教二-105"}
              ]
            }
            """.trimIndent(),
        )
        assertEquals(2, schedule.courses.size)
        assertEquals(setOf(1, 3, 5, 7, 9, 11, 13, 15), schedule.courses[0].weeks)
        assertEquals(setOf(1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 15, 16), schedule.courses[1].weeks)
    }

    @Test
    fun rejectsNonMondaySemesterStart() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            CourseScheduleJson.parseSchedule(scheduleJson("2026-09-08"))
        }
        assertEquals("第一周周一必须是周一", error.message)
    }

    @Test
    fun switchesTimetableByActualClassDate() {
        val schedule = CourseScheduleJson.parseSchedule(
            """
            {
              "semesterStart": "2026-09-07",
              "bufferBeforeMin": 10,
              "bufferAfterMin": 20,
              "timeProfiles": [
                {"effectiveFrom":"2026-05-06","name":"夏季作息","sections":[
                  {"index":5,"start":"15:00","end":"15:45"},
                  {"index":6,"start":"15:55","end":"16:40"}
                ]},
                {"effectiveFrom":"2026-10-08","name":"秋季作息","sections":[
                  {"index":5,"start":"14:30","end":"15:15"},
                  {"index":6,"start":"15:25","end":"16:10"}
                ]}
              ],
              "courses": [
                {"name":"跨作息课程","weekday":4,"sectionStart":5,"sectionEnd":6,"weeks":"1-19","room":"测试楼","teacher":"测试老师"}
              ]
            }
            """.trimIndent(),
        )
        val sessions = schedule.sessions(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 10, 8))
        assertEquals("15:00", sessions.first { it.week == 4 }.start.toLocalTime().toString().take(5))
        assertEquals("14:30", sessions.first { it.week == 5 }.start.toLocalTime().toString().take(5))
        assertEquals("测试老师", schedule.courses.single().teacher)
    }

    @Test
    fun rejectsSectionCourseWithoutProfiles() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            CourseScheduleJson.parseSchedule(
                """
                {"semesterStart":"2026-09-07","courses":[
                  {"name":"没有作息","weekday":1,"sectionStart":1,"sectionEnd":2,"weeks":"1-19","room":""}
                ]}
                """.trimIndent(),
            )
        }
        assertEquals(true, error.message.orEmpty().contains("timeProfiles"))
    }

    @Test
    fun rejectsInvalidTimeAndWeekRange() {
        val badTime = scheduleJson("2026-09-07").replace("09:40", "08:00")
        assertThrows(IllegalArgumentException::class.java) { CourseScheduleJson.parseSchedule(badTime) }
        val badWeek = scheduleJson("2026-09-07").replace("1-16", "16-1")
        assertThrows(IllegalArgumentException::class.java) { CourseScheduleJson.parseSchedule(badWeek) }
    }

    private fun scheduleJson(date: String) = """
        {
          "semesterStart": "$date",
          "bufferBeforeMin": 10,
          "bufferAfterMin": 20,
          "courses": [
            {"name":"高等数学","weekday":1,"start":"08:00","end":"09:40","weeks":"1-16","room":"教三-201"}
          ]
        }
    """.trimIndent()
}
