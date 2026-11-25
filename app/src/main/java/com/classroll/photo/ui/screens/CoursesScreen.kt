package com.classroll.photo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.classroll.photo.model.CourseSchedule
import com.classroll.photo.model.CourseSession
import com.classroll.photo.ui.ClassrollCard
import com.classroll.photo.ui.skinShadow
import com.classroll.photo.ui.ClassrollIcons
import com.classroll.photo.ui.PaperTape
import com.classroll.photo.ui.SecondaryButton
import com.classroll.photo.ui.SectionHeading
import com.classroll.photo.ui.theme.corner
import com.classroll.photo.ui.theme.LocalSkin
import com.classroll.photo.ui.theme.courseColor
import com.classroll.photo.ui.theme.PaperCard
import com.classroll.photo.ui.theme.PaperLine
import com.classroll.photo.ui.theme.PaperBackground
import com.classroll.photo.ui.theme.PaperBlue
import com.classroll.photo.ui.theme.PaperGood
import com.classroll.photo.ui.theme.PaperHighlight
import com.classroll.photo.ui.theme.PaperInk
import com.classroll.photo.ui.theme.PaperTerracotta
import com.classroll.photo.ui.theme.PaperWarn
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class QuickScanMode { Scan, Scanning, Resume, Rescan }

private fun weekdayLabel(value: Int): String = listOf("一", "二", "三", "四", "五", "六", "日")[value - 1]

private fun minuteLabel(value: Int): String = "%02d:%02d".format(value / 60, value % 60)

private fun timeLabel(value: LocalDateTime): String = "%02d:%02d".format(value.hour, value.minute)

@Composable
private fun courseColor(name: String): Color = LocalSkin.current.courseColor(name)

@Composable
fun CoursesScreen(
    schedule: CourseSchedule?,
    quickMode: QuickScanMode,
    selectedCount: Int,
    candidateCount: Int,
    onQuickScan: () -> Unit,
    onScanSession: (String) -> Unit,
    onScanToday: (Set<String>) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var nowDateTime by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowDateTime = LocalDateTime.now()
        }
    }
    val today = nowDateTime.toLocalDate()
    val now = nowDateTime.toLocalTime()
    val tomorrow = today.plusDays(1)
    val todayAll = schedule?.sessions(today, today).orEmpty()
    val todaySessions = todayAll
    val dayFinished = todayAll.isNotEmpty() && todayAll.all { now.isAfter(it.end.toLocalTime()) }
    val tomorrowSessions = schedule?.sessions(tomorrow, tomorrow).orEmpty()
    val weekStart = today.with(DayOfWeek.MONDAY)
    val weekEnd = weekStart.plusDays(6)
    val weekSessions = schedule?.sessions(weekStart, weekEnd).orEmpty()
        .filter { it.start.toLocalDate() != today && it.start.toLocalDate() != tomorrow }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            val highlight = PaperHighlight
            Text(
                "课程照片，自动归位。",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .drawBehind {
                        drawRect(
                            color = highlight.copy(alpha = .52f),
                            topLeft = Offset(0f, size.height * .58f),
                            size = Size(size.width, size.height * .32f),
                        )
                    },
            )
        }
        item { QuickScanCard(quickMode, selectedCount, candidateCount, onQuickScan) }

        item {
            SectionHeading(
                title = "今日课程",
                trailing = {
                    Text(
                        if (schedule == null) "课表未导入" else "周${weekdayLabel(today.dayOfWeek.value)} · ${todaySessions.size} 节",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        if (schedule == null) {
            item {
                ClassrollCard {
                    Text("还没有导入课程表", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else if (todayAll.isEmpty()) {
            item { EmptyLessonHint("今天没有课") }
        } else {
            items(todayAll.size) { index ->
                val session = todayAll[index]
                val course = schedule.courses.firstOrNull { it.id == session.courseId }
                TodaySessionCard(
                    session = session,
                    teacher = course?.teacher.orEmpty(),
                    color = courseColor(session.courseName),
                    now = now,
                    onScan = { onScanSession(session.id) },
                )
            }
            if (dayFinished) {
                item {
                    SecondaryButton(
                        text = "扫描本日课程",
                        onClick = { onScanToday(todayAll.map { it.id }.toSet()) },
                        icon = ClassrollIcons.Scan,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (schedule != null) {
            item {
                SectionHeading(
                    title = "明日课程",
                    trailing = {
                        Text(
                            "周${weekdayLabel(tomorrow.dayOfWeek.value)} · ${tomorrowSessions.size} 节",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            if (tomorrowSessions.isEmpty()) {
                item { EmptyLessonHint("明天没有课") }
            } else {
                items(tomorrowSessions.size) { index ->
                    SessionRow(tomorrowSessions[index], showWeekday = false)
                }
            }

            item {
                SectionHeading(
                    title = "本周课程",
                    trailing = {
                        Text(
                            "${weekSessions.size} 节",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            if (weekSessions.isEmpty()) {
                item { EmptyLessonHint("本周其余几天没有课") }
            } else {
                items(weekSessions.size) { index ->
                    SessionRow(weekSessions[index], showWeekday = true)
                }
            }
        }

        item {
            SecondaryButton(
                text = "导入课程表",
                onClick = onImport,
                icon = ClassrollIcons.Upload,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun EmptyLessonHint(text: String) {
    ClassrollCard {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun QuickScanCard(mode: QuickScanMode, selectedCount: Int, candidateCount: Int, onClick: () -> Unit) {
    val lineColor = PaperLine
    val title = when (mode) {
        QuickScanMode.Scan -> if (candidateCount > 0) "$candidateCount 张已识别，等待确认" else "开始识别课程照片"
        QuickScanMode.Scanning -> "正在本机识别…"
        QuickScanMode.Resume -> "$selectedCount 张已勾选，等待确认"
        QuickScanMode.Rescan -> if (candidateCount > 0) "$candidateCount 张候选可继续查看" else "本轮识别完成"
    }
    val action = when (mode) {
        QuickScanMode.Scan -> "去扫描"
        QuickScanMode.Scanning -> "查看进度"
        QuickScanMode.Resume -> "继续确认"
        QuickScanMode.Rescan -> "查看结果"
    }

    val skin = LocalSkin.current
    val hard = skin.hardShadow
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(if (hard != null) Modifier.skinShadow(6.dp, .10f, .26f, MaterialTheme.shapes.medium) else Modifier),
        shape = MaterialTheme.shapes.medium,
        color = if (hard != null) Color.White else Color.Transparent,
        border = if (hard != null) skin.cardBorder else BorderStroke(1.dp, PaperInk.copy(alpha = .12f)),
    ) {
        Box(
            modifier = Modifier.background(
                if (hard != null) {
                    Brush.linearGradient(listOf(Color.White, Color.White))
                } else {
                    Brush.linearGradient(
                        listOf(
                            PaperHighlight.copy(alpha = if (mode == QuickScanMode.Scanning) .45f else .72f),
                            PaperCard,
                        ),
                    )
                },
                MaterialTheme.shapes.medium,
            ),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = lineColor.copy(alpha = .55f),
                    radius = size.width * .2f,
                    center = Offset(size.width * 1.03f, size.height * .12f),
                    style = Stroke(1.dp.toPx()),
                )
            }
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(42.dp).background(PaperInk, RoundedCornerShape(LocalSkin.current.corner(13.dp))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(ClassrollIcons.Scan, contentDescription = null, tint = PaperBackground, modifier = Modifier.size(21.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(action, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Icon(ClassrollIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
            PaperTape(Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun TodaySessionCard(
    session: CourseSession,
    teacher: String,
    color: Color,
    now: LocalTime,
    onScan: () -> Unit,
) {
    val status = when {
        now.isBefore(session.start.toLocalTime()) -> "待开始"
        now.isAfter(session.end.toLocalTime()) -> "已结束"
        else -> "进行中"
    }
    val statusColor = when (status) {
        "进行中" -> PaperTerracotta
        "待开始" -> PaperWarn
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val meta = buildString {
        append("第 ${session.week} 周")
        if (session.room.isNotBlank()) append(" · ${session.room}")
        if (teacher.isNotBlank()) append(" · $teacher")
    }
    ClassrollCard(
        modifier = Modifier.alpha(if (status == "已结束") .72f else 1f),
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(62.dp)) {
                Text(timeLabel(session.start), style = MaterialTheme.typography.titleMedium)
                Text(timeLabel(session.end), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(session.courseName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(7.dp))
                    Surface(shape = CircleShape, color = statusColor.copy(alpha = .12f)) {
                        Text(
                            status,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                        )
                    }
                }
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onScan) {
                Icon(ClassrollIcons.Scan, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("扫描本节课", style = MaterialTheme.typography.labelMedium)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(color))
    }
}

@Composable
private fun SessionRow(session: CourseSession, showWeekday: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = LocalSkin.current.cardBorder,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.width(52.dp)) {
                Text(
                    timeLabel(session.start),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    timeLabel(session.end),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(session.courseName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    buildString {
                        if (showWeekday) append("周${weekdayLabel(session.start.dayOfWeek.value)} · ")
                        append("第 ${session.week} 周")
                        if (session.room.isNotBlank()) append(" · ${session.room}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}
