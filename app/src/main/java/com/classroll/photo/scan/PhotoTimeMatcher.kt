package com.classroll.photo.scan

import com.classroll.photo.model.CourseSession
import com.classroll.photo.model.CourseTimeMatch
import com.classroll.photo.model.PhotoGroup
import com.classroll.photo.model.TimeMatchKind
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object PhotoTimeMatcher {
    fun matchPhoto(time: LocalDateTime, sessions: List<CourseSession>): CourseTimeMatch? {
        val hits = sessions.flatMap { session -> session.effectiveSegments().map { session to it } }
            .filter { (_, segment) -> !time.isBefore(segment.windowStart) && !time.isAfter(segment.windowEnd) }
        if (hits.isEmpty()) return null
        val core = hits.filter { (_, segment) -> !time.isBefore(segment.start) && !time.isAfter(segment.end) }
        if (core.isNotEmpty()) {
            val session = core.first().first
            val overlap = core.map { it.first.id }.distinct().size > 1
            return CourseTimeMatch(session, if (overlap) TimeMatchKind.Overlap else TimeMatchKind.Core)
        }
        return CourseTimeMatch(hits.maxBy { it.second.start }.first, TimeMatchKind.Buffer)
    }

    fun buildGroups(sessions: List<CourseSession>): Map<String, PhotoGroup> = buildMap {
        val dateFormat = DateTimeFormatter.ofPattern("MM月dd日 EEEE HH:mm–HH:mm")
        sessions.forEach { session ->
            val meta = "${dateFormat.format(session.start)} · ${session.room}"
            put(
                session.id,
                PhotoGroup(
                    id = session.id,
                    course = session.courseName,
                    session = "第 ${session.number} 次课",
                    meta = meta,
                    week = session.week,
                    date = session.start.toLocalDate().atStartOfDay(),
                ),
            )
            put(
                "${session.id}:boundary",
                PhotoGroup(
                    id = "${session.id}:boundary",
                    course = session.courseName,
                    session = "边界 · 课间缓冲",
                    meta = meta,
                    week = session.week,
                    date = session.start.toLocalDate().atStartOfDay(),
                    boundary = true,
                ),
            )
        }
    }
}
