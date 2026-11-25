package com.classroll.photo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.classroll.photo.BuildConfig
import com.classroll.photo.model.CourseSchedule
import com.classroll.photo.model.SkipRange
import com.classroll.photo.model.TimetableGroups
import com.classroll.photo.ui.AppIconButton
import com.classroll.photo.ui.ChineseLocale
import com.classroll.photo.ui.ClassrollIcons
import com.classroll.photo.ui.ClassrollCard
import com.classroll.photo.ui.HorizontalDashedDivider
import com.classroll.photo.ui.InfoChip
import com.classroll.photo.ui.PrimaryButton
import com.classroll.photo.ui.SecondaryButton
import com.classroll.photo.ui.ScreenTitle
import com.classroll.photo.ui.SkinGlyph
import com.classroll.photo.ui.theme.corner
import com.classroll.photo.ui.theme.AllSkins
import com.classroll.photo.ui.theme.LocalSkin
import com.classroll.photo.ui.theme.PaperGood
import com.classroll.photo.ui.theme.PaperTerracotta
import com.classroll.photo.ui.theme.Skin
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Composable
fun SettingsScreen(
    schedule: CourseSchedule?,
    permissionLabel: String,
    includeSharedImages: Boolean,
    onImport: () -> Unit,
    onOpenSchedule: () -> Unit,
    onPermission: () -> Unit,
    onToggleIncludeShared: (Boolean) -> Unit,
    onTimetable: () -> Unit,
    archiveCount: Int,
    onManageArchive: () -> Unit,
    onSaveSchedule: (CourseSchedule) -> Unit,
    skinId: String,
    onSkinChange: (String) -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showBuffer by remember { mutableStateOf(false) }
    var showSkipDates by remember { mutableStateOf(false) }
    var showSectionCount by remember { mutableStateOf(false) }
    var showWeekPicker by remember { mutableStateOf(false) }
    var showSkinPicker by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val activeProfile = schedule?.let { s ->
        s.timeProfiles.filter { !it.effectiveFrom.isAfter(today) }.maxByOrNull { it.effectiveFrom }
            ?: s.timeProfiles.minByOrNull { it.effectiveFrom }
    }
    val currentWeek = schedule?.let { s ->
        val days = ChronoUnit.DAYS.between(s.semesterStart, today)
        if (days < 0) {
            null
        } else {
            val week = (days / 7 + 1).toInt()
            if (s.totalWeeks > 0 && week > s.totalWeeks) null else week
        }
    }
    val displayWeek = schedule?.currentWeekOverride ?: currentWeek
    val weekValue = when {
        schedule == null -> "—"
        displayWeek != null -> buildString {
            append(if (schedule.totalWeeks > 0) "第 $displayWeek / ${schedule.totalWeeks} 周" else "第 $displayWeek 周")
            if (schedule.currentWeekOverride != null) append(" · 手动")
        }
        today.isBefore(schedule.semesterStart) -> "未开学"
        else -> "学期已结束"
    }
    val groupSets = schedule?.timeProfiles?.map { it.groups }?.distinct().orEmpty()
    val firstGroups = groupSets.firstOrNull()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenTitle(title = "设置")
        }
        item { SectionLabel("课程与扫描") }
        item {
            ClassrollCard(contentPadding = PaddingValues(0.dp)) {
                SettingRow(
                    icon = ClassrollIcons.Upload,
                    title = "课程表",
                    subtitle = schedule?.let { "${it.uniqueCourseCount} 门 · ${it.courses.size} 条" } ?: "还没有导入",
                    value = if (schedule == null) "未导入" else "已导入",
                    valueColor = if (schedule == null) PaperTerracotta else PaperGood,
                    onClick = onOpenSchedule,
                )
                SettingDivider()
                SettingRow(
                    icon = ClassrollIcons.Clock,
                    title = "学期与周次",
                    subtitle = schedule?.termName?.ifBlank { "未命名学期" } ?: "导入课程表后自动识别",
                    value = weekValue,
                    onClick = schedule?.let { { showWeekPicker = true } },
                )
                SettingDivider()
                SettingRow(
                    icon = ClassrollIcons.Clock,
                    title = "作息时间",
                    subtitle = when {
                        schedule == null -> "导入课程表后可手动编辑"
                        schedule.timeProfiles.isEmpty() -> "还没有作息；上课节次需要先补作息时间"
                        activeProfile == null -> "没有生效的作息"
                        else -> "${activeProfile.name} · ${activeProfile.sections.size} 节"
                    },
                    value = when {
                        schedule == null -> "—"
                        activeProfile == null -> "${schedule.timeProfiles.size} 套"
                        !activeProfile.effectiveFrom.isAfter(today) -> "当前使用"
                        else -> "${activeProfile.effectiveFrom} 起"
                    },
                    valueColor = when {
                        schedule == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        activeProfile?.effectiveFrom?.isAfter(today) == false -> PaperGood
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    onClick = schedule?.let { { onTimetable() } },
                )
                SettingDivider()
                SettingRow(
                    icon = ClassrollIcons.Layers,
                    title = "课表节数",
                    subtitle = firstGroups?.let { "上午 ${it.morning} · 下午 ${it.afternoon} · 晚上 ${it.evening}" }.orEmpty(),
                    value = when {
                        groupSets.isEmpty() -> "—"
                        groupSets.size == 1 -> "${firstGroups!!.morning} / ${firstGroups.afternoon} / ${firstGroups.evening}"
                        else -> "各套不同"
                    },
                    onClick = if (schedule != null && schedule.timeProfiles.isNotEmpty()) {
                        { showSectionCount = true }
                    } else {
                        null
                    },
                )
                SettingDivider()
                SettingRow(
                    icon = ClassrollIcons.Calendar,
                    title = "停课日期",
                    subtitle = "",
                    value = when {
                        schedule == null -> "—"
                        schedule.allSkipDates.isEmpty() -> "未设置"
                        else -> "${schedule.allSkipDates.size} 天"
                    },
                    valueColor = if (schedule != null && schedule.skipDates.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else PaperGood,
                    onClick = schedule?.let { { showSkipDates = true } },
                )
                SettingDivider()
                SettingRow(
                    icon = ClassrollIcons.Layers,
                    title = "缓冲时间",
                    subtitle = "课前 / 课后",
                    value = schedule?.let { "${it.bufferBeforeMin} / ${it.bufferAfterMin}" } ?: "—",
                    onClick = schedule?.let { { showBuffer = true } },
                )
                SettingDivider()
                SettingRow(
                    icon = ClassrollIcons.Shield,
                    title = "相册权限",
                    subtitle = "",
                    value = permissionLabel,
                    valueColor = if (permissionLabel == "全部照片") PaperGood else PaperTerracotta,
                    onClick = onPermission,
                )
            }
        }
        item { SectionLabel("扫描范围") }
        item {
            ClassrollCard(contentPadding = PaddingValues(0.dp)) {
                SettingSwitchRow(
                    icon = ClassrollIcons.Image,
                    title = "包含微信/分享图片",
                    subtitle = "",
                    checked = includeSharedImages,
                    onCheckedChange = onToggleIncludeShared,
                )
            }
        }
        item { SectionLabel("存储与隐私") }
        item {
            ClassrollCard(contentPadding = PaddingValues(0.dp)) {
                SettingRow(
                    icon = ClassrollIcons.Trash,
                    title = "归档管理",
                    subtitle = "",
                    value = if (archiveCount == 0) "无" else "$archiveCount 个相册",
                    onClick = onManageArchive,
                )
                SettingDivider()
                SettingRow(
                    icon = ClassrollIcons.File,
                    title = "课页 Classroll",
                    subtitle = "",
                    value = BuildConfig.VERSION_NAME,
                    onClick = onAbout,
                )
            }
        }
        item { SectionLabel("外观") }
        item {
            val skin = LocalSkin.current
            ClassrollCard(contentPadding = PaddingValues(0.dp)) {
                SettingRow(
                    iconContent = { SkinGlyph() },
                    title = "皮肤",
                    subtitle = skin.tagline,
                    value = skin.label,
                    onClick = { showSkinPicker = true },
                )
            }
        }
        item {
            SecondaryButton(
                text = "导入课程表",
                onClick = onImport,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                icon = ClassrollIcons.Upload,
            )
        }
    }

    if (showSkinPicker) {
        SkinPickerDialog(
            currentId = skinId,
            onSelect = { id ->
                onSkinChange(id)
                showSkinPicker = false
            },
            onDismiss = { showSkinPicker = false },
        )
    }

    if (schedule != null) {
        if (showBuffer) {
            BufferDialog(
                before = schedule.bufferBeforeMin,
                after = schedule.bufferAfterMin,
                onDismiss = { showBuffer = false },
                onConfirm = { before, after ->
                    onSaveSchedule(schedule.copy(bufferBeforeMin = before, bufferAfterMin = after))
                    showBuffer = false
                },
            )
        }
        if (showSkipDates) {
            SkipDatesDialog(
                dates = schedule.skipDates,
                ranges = schedule.skipRanges,
                onDismiss = { showSkipDates = false },
                onChange = { dates, ranges ->
                    onSaveSchedule(schedule.copy(skipDates = dates, skipRanges = ranges))
                },
            )
        }
        if (showWeekPicker) {
            CurrentWeekDialog(
                autoWeek = currentWeek,
                totalWeeks = schedule.totalWeeks,
                selected = schedule.currentWeekOverride,
                onDismiss = { showWeekPicker = false },
                onSelect = { week ->
                    onSaveSchedule(schedule.copy(currentWeekOverride = week))
                    showWeekPicker = false
                },
            )
        }
        if (showSectionCount && schedule.timeProfiles.isNotEmpty()) {
            TimetableSectionCountDialog(
                groups = firstGroups ?: TimetableGroups(),
                onDismiss = { showSectionCount = false },
                onConfirm = { nextGroups ->
                    val profiles = schedule.timeProfiles.map { profile ->
                        profile.copy(
                            groups = nextGroups,
                            sections = resizeSections(profile.sections, profile.groups, nextGroups),
                        )
                    }
                    onSaveSchedule(schedule.copy(timeProfiles = profiles))
                    showSectionCount = false
                },
            )
        }
    }
}

@Composable
private fun BufferDialog(
    before: Int,
    after: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    var beforeMin by remember { mutableIntStateOf(before.coerceIn(0, 120)) }
    var afterMin by remember { mutableIntStateOf(after.coerceIn(0, 120)) }
    SettingsDialog(title = "缓冲时间", onDismiss = onDismiss, height = 320.dp) {
        Spacer(Modifier.height(6.dp))
        BufferStepperRow("课前", beforeMin) { beforeMin = it }
        BufferStepperRow("课后", afterMin) { afterMin = it }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("取消", onDismiss, Modifier.weight(1f))
            PrimaryButton("保存", { onConfirm(beforeMin, afterMin) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun BufferStepperRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        StepRoundButton("－") { onChange((value - 5).coerceAtLeast(0)) }
        Text(
            "$value 分钟",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(86.dp),
        )
        StepRoundButton("＋") { onChange((value + 5).coerceAtMost(120)) }
    }
}

@Composable
private fun StepRoundButton(text: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = LocalSkin.current.cardBorder,
        modifier = Modifier.size(32.dp).clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SkipDatesDialog(
    dates: Set<LocalDate>,
    ranges: List<SkipRange>,
    onDismiss: () -> Unit,
    onChange: (Set<LocalDate>, List<SkipRange>) -> Unit,
) {
    var showRange by remember { mutableStateOf(false) }
    val visibleDates = dates.filterNot { date -> ranges.any { it.contains(date) } }.sorted()
    val sortedRanges = ranges.sortedBy { it.start }
    val totalDays = visibleDates.size + ranges.sumOf { it.days }

    SettingsDialog(title = "停课日期", onDismiss = onDismiss, height = 440.dp) {
        Text(
            "共 $totalDays 天",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        if (totalDays == 0) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "还没有停课日期",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    var first = true
                    sortedRanges.forEach { range ->
                        if (!first) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                        }
                        first = false
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rangeLabel(range), style = MaterialTheme.typography.bodyLarge)
                                Text("连续 ${range.days} 天", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { onChange(dates, ranges - range) }) {
                                Text("删除", style = MaterialTheme.typography.labelMedium, color = PaperTerracotta)
                            }
                        }
                    }
                    visibleDates.forEach { date ->
                        if (!first) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                        }
                        first = false
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dayLabel(date), style = MaterialTheme.typography.bodyLarge)
                                Text("单日", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { onChange(dates - date, ranges) }) {
                                Text("删除", style = MaterialTheme.typography.labelMedium, color = PaperTerracotta)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SecondaryButton(
            text = "选择停课日期",
            onClick = { showRange = true },
            modifier = Modifier.fillMaxWidth(),
            icon = ClassrollIcons.Calendar,
        )
        Text(
            "点一天是单日；点两天选一段。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            textAlign = TextAlign.Center,
        )
    }
    if (showRange) {
        RangeCalendarDialog(
            onDismiss = { showRange = false },
            onConfirm = { start, end ->
                if (start == end) {
                    if (start !in dates && ranges.none { it.contains(start) }) {
                        onChange(dates + start, ranges)
                    }
                } else {
                    onChange(
                        dates.filterNot { !it.isBefore(start) && !it.isAfter(end) }.toSet(),
                        mergeRange(ranges, SkipRange(start, end)),
                    )
                }
                showRange = false
            },
        )
    }
}

private fun dayLabel(date: LocalDate): String {
    val weekday = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")[date.dayOfWeek.value - 1]
    return "${date.monthValue}月${date.dayOfMonth}日 $weekday"
}

private fun rangeLabel(range: SkipRange): String =
    "${range.start.monthValue}月${range.start.dayOfMonth}日–${range.end.monthValue}月${range.end.dayOfMonth}日"

private fun mergeRange(ranges: List<SkipRange>, added: SkipRange): List<SkipRange> {
    var merged = added
    val result = mutableListOf<SkipRange>()
    ranges.forEach { range ->
        if (range.end.plusDays(1).isBefore(merged.start) || range.start.minusDays(1).isAfter(merged.end)) {
            result += range
        } else {
            merged = SkipRange(minOf(range.start, merged.start), maxOf(range.end, merged.end))
        }
    }
    result += merged
    return result.sortedBy { it.start }
}

@Composable
private fun CurrentWeekDialog(
    autoWeek: Int?,
    totalWeeks: Int,
    selected: Int?,
    onDismiss: () -> Unit,
    onSelect: (Int?) -> Unit,
) {
    val maxWeek = totalWeeks.takeIf { it in 1..60 } ?: 30
    SettingsDialog(title = "当前周次", onDismiss = onDismiss, height = 460.dp) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().clickable { onSelect(null) },
        ) {
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("自动识别", style = MaterialTheme.typography.titleMedium)
                    Text(
                        autoWeek?.let { "按开学日期算，现在是第 $it 周" } ?: "当前不在学期范围内",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (selected == null) {
                    Icon(ClassrollIcons.Check, contentDescription = null, tint = PaperGood, modifier = Modifier.size(18.dp))
                }
            }
        }
        Text(
            "也可以手动指定：",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
        )
        (1..maxWeek).chunked(6).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { week ->
                    val active = week == selected
                    Surface(
                        modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                        shape = RoundedCornerShape(LocalSkin.current.corner(10.dp)),
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        onClick = { onSelect(week) },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                week.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun RangeCalendarDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var start by remember { mutableStateOf<LocalDate?>(null) }
    var end by remember { mutableStateOf<LocalDate?>(null) }
    var mode by remember { mutableIntStateOf(0) }
    var yearPage by remember { mutableIntStateOf((month.year / 12) * 12) }

    fun pick(date: LocalDate) {
        val currentStart = start
        if (currentStart == null || end != null) {
            start = date
            end = null
        } else if (date.isBefore(currentStart)) {
            start = date
            end = currentStart
        } else {
            end = date
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = .32f)).clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.background,
                border = LocalSkin.current.cardBorder,
                modifier = Modifier
                    .fillMaxWidth(.92f)
                    .height(460.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (mode) {
                            0 -> {
                                IconButton(onClick = { month = month.minusMonths(1) }, modifier = Modifier.size(36.dp)) {
                                    Icon(ClassrollIcons.ChevronLeft, contentDescription = "上个月", modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    "${month.year}年",
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f).clickable {
                                        yearPage = (month.year / 12) * 12
                                        mode = 1
                                    }.padding(vertical = 6.dp),
                                )
                                Text(
                                    "${month.monthValue}月",
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f).clickable { mode = 2 }.padding(vertical = 6.dp),
                                )
                                IconButton(onClick = { month = month.plusMonths(1) }, modifier = Modifier.size(36.dp)) {
                                    Icon(ClassrollIcons.ChevronRight, contentDescription = "下个月", modifier = Modifier.size(18.dp))
                                }
                            }
                            1 -> {
                                IconButton(onClick = { yearPage -= 12 }, modifier = Modifier.size(36.dp)) {
                                    Icon(ClassrollIcons.ChevronLeft, contentDescription = "上一页", modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    "$yearPage–${yearPage + 11}",
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { yearPage += 12 }, modifier = Modifier.size(36.dp)) {
                                    Icon(ClassrollIcons.ChevronRight, contentDescription = "下一页", modifier = Modifier.size(18.dp))
                                }
                            }
                            else -> {
                                Text(
                                    "选择月份",
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    when (mode) {
                        0 -> {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            val firstDay = month.atDay(1)
                            val offset = (firstDay.dayOfWeek.value + 6) % 7
                            val daysInMonth = month.lengthOfMonth()
                            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                for (row in 0 until 6) {
                                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        for (col in 0 until 7) {
                                            Box(
                                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                val dayNumber = row * 7 + col - offset + 1
                                                if (dayNumber in 1..daysInMonth) {
                                                    val date = month.atDay(dayNumber)
                                                    val isStart = date == start
                                                    val isEnd = date == end
                                                    val inRange = start != null && end != null && date.isAfter(start) && date.isBefore(end)
                                                    Surface(
                                                        onClick = { pick(date) },
                                                        modifier = Modifier.size(38.dp),
                                                        shape = CircleShape,
                                                        color = when {
                                                            isStart || isEnd -> MaterialTheme.colorScheme.primary
                                                            inRange -> MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                                                            else -> Color.Transparent
                                                        },
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                dayNumber.toString(),
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = if (isStart || isEnd) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                for (row in 0 until 4) {
                                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        for (col in 0 until 3) {
                                            val year = yearPage + row * 3 + col
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                                Surface(
                                                    onClick = {
                                                        month = month.withYear(year)
                                                        mode = 0
                                                    },
                                                    modifier = Modifier.fillMaxWidth(.72f).height(44.dp),
                                                    shape = RoundedCornerShape(LocalSkin.current.corner(10.dp)),
                                                    color = if (year == month.year) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            year.toString(),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = if (year == month.year) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                for (row in 0 until 4) {
                                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        for (col in 0 until 3) {
                                            val m = row * 3 + col + 1
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                                Surface(
                                                    onClick = {
                                                        month = month.withMonth(m)
                                                        mode = 0
                                                    },
                                                    modifier = Modifier.fillMaxWidth(.72f).height(44.dp),
                                                    shape = RoundedCornerShape(LocalSkin.current.corner(10.dp)),
                                                    color = if (m == month.monthValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            "$m 月",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = if (m == month.monthValue) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton("取消", onDismiss, Modifier.weight(1f))
                        PrimaryButton(
                            "确定",
                            onClick = {
                                val chosenStart = start
                                if (chosenStart != null) onConfirm(chosenStart, end ?: chosenStart)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = start != null,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
) {
    val state = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val startMillis = state.selectedStartDateMillis
                    val endMillis = state.selectedEndDateMillis ?: startMillis
                    if (startMillis != null && endMillis != null) {
                        val start = Instant.ofEpochMilli(startMillis).atZone(ZoneOffset.UTC).toLocalDate()
                        val end = Instant.ofEpochMilli(endMillis).atZone(ZoneOffset.UTC).toLocalDate()
                        onConfirm(minOf(start, end), maxOf(start, end))
                    }
                },
                enabled = state.selectedStartDateMillis != null,
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    ) {
        ChineseLocale {
            DateRangePicker(
                state = state,
                modifier = Modifier.heightIn(max = 480.dp),
                colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )
        }
    }
}

@Composable
private fun SettingsDialog(
    title: String,
    onDismiss: () -> Unit,
    height: androidx.compose.ui.unit.Dp = 460.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = .32f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.background,
                border = LocalSkin.current.cardBorder,
                modifier = Modifier
                    .fillMaxWidth(.92f)
                    .height(height)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                        AppIconButton(icon = ClassrollIcons.Close, contentDescription = "关闭", onClick = onDismiss)
                    }
                    Spacer(Modifier.height(6.dp))
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 1.dp),
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector? = null,
    iconContent: (@Composable () -> Unit)? = null,
    title: String,
    subtitle: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(LocalSkin.current.corner(10.dp))),
            contentAlignment = Alignment.Center,
        ) {
            if (iconContent != null) {
                iconContent()
            } else if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.labelMedium, color = valueColor)
        if (onClick != null) {
            Spacer(Modifier.width(2.dp))
            Icon(ClassrollIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(LocalSkin.current.corner(10.dp))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        val skin = LocalSkin.current
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = if (skin.hardShadow != null) {
                SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = skin.accent,
                    checkedBorderColor = skin.ink,
                    uncheckedThumbColor = skin.ink,
                    uncheckedTrackColor = Color.White,
                    uncheckedBorderColor = skin.ink,
                )
            } else {
                SwitchDefaults.colors()
            },
        )
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDashedDivider(
        modifier = Modifier.padding(start = 58.dp, end = 13.dp),
        color = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun SkinPickerDialog(
    currentId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    SettingsDialog(title = "皮肤", onDismiss = onDismiss, height = 320.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AllSkins.forEach { skin ->
                SkinPreviewRow(
                    skin = skin,
                    selected = skin.id == currentId,
                    onClick = { onSelect(skin.id) },
                )
            }
        }
    }
}

@Composable
private fun SkinPreviewRow(skin: Skin, selected: Boolean, onClick: () -> Unit) {
    val current = LocalSkin.current
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = skin.card,
        border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) skin.accent else current.line),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(skin.cardAlt, RoundedCornerShape(skin.corner(10.dp))),
                contentAlignment = Alignment.Center,
            ) {
                SkinGlyph(skin = skin, glyphSize = 26.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(skin.label, style = skin.typography.titleMedium, color = skin.ink)
                Text(skin.tagline, style = skin.typography.bodySmall, color = skin.muted)
            }
            if (selected) {
                Icon(ClassrollIcons.Check, contentDescription = null, tint = skin.accent, modifier = Modifier.size(20.dp))
            }
        }
    }
}
