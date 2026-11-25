package com.classroll.photo

import com.classroll.photo.model.CourseSession
import com.classroll.photo.model.TimeMatchKind
import com.classroll.photo.scan.PhotoTimeMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PhotoTimeMatcherTest {
    private val session = CourseSession(
        id = "math-1",
        courseId = "math",
        courseName = "高等数学",
        week = 1,
        number = 1,
        room = "教三-201",
        start = LocalDateTime.of(2026, 9, 7, 8, 0),
        end = LocalDateTime.of(2026, 9, 7, 9, 40),
        windowStart = LocalDateTime.of(2026, 9, 7, 7, 50),
        windowEnd = LocalDateTime.of(2026, 9, 7, 10, 0),
    )

    @Test
    fun prefersCoreTimeOverBuffer() {
        val match = PhotoTimeMatcher.matchPhoto(LocalDateTime.of(2026, 9, 7, 8, 30), listOf(session))
        assertEquals(TimeMatchKind.Core, match?.kind)
    }

    @Test
    fun marksBufferAndRejectsOutsideWindow() {
        val buffer = PhotoTimeMatcher.matchPhoto(LocalDateTime.of(2026, 9, 7, 9, 50), listOf(session))
        assertEquals(TimeMatchKind.Buffer, buffer?.kind)
        assertNull(PhotoTimeMatcher.matchPhoto(LocalDateTime.of(2026, 9, 7, 10, 1), listOf(session)))
    }

    @Test
    fun laterBufferWinsWhenWindowsOverlap() {
        val second = session.copy(
            id = "physics-1",
            courseName = "大学物理",
            start = session.start.plusHours(2).plusMinutes(5),
            end = session.end.plusHours(2).plusMinutes(5),
            windowStart = session.windowStart.plusHours(2).plusMinutes(5),
            windowEnd = session.windowEnd.plusHours(2).plusMinutes(5),
        )
        val match = PhotoTimeMatcher.matchPhoto(LocalDateTime.of(2026, 9, 7, 9, 58), listOf(session, second))
        assertEquals("physics-1", match?.session?.id)
        assertEquals(TimeMatchKind.Buffer, match?.kind)
    }

    @Test
    fun buildsBoundaryGroup() {
        val groups = PhotoTimeMatcher.buildGroups(listOf(session))
        assertEquals(true, groups["${session.id}:boundary"]?.boundary)
        assertEquals(LocalDate.of(2026, 9, 7), groups[session.id]?.date?.toLocalDate())
    }
}
