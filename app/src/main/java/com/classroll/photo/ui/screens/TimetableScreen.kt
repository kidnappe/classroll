package com.classroll.photo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.classroll.photo.model.CourseSchedule
import com.classroll.photo.model.SectionPart
import com.classroll.photo.model.TimetableGroups
import com.classroll.photo.model.TimetableProfile
import com.classroll.photo.model.TimetableSection
import com.classroll.photo.ui.AppIconButton
import com.classroll.photo.ui.ChineseLocale
import com.classroll.photo.ui.ClassrollCard
import com.classroll.photo.ui.ClassrollIcons
import com.classroll.photo.ui.InfoChip
import com.classroll.photo.ui.SecondaryButton
import com.classroll.photo.ui.theme.corner
import com.classroll.photo.ui.theme.LocalSkin
import com.classroll.photo.ui.theme.PaperGood
import com.classroll.photo.ui.theme.PaperInk
import com.classroll.photo.ui.theme.PaperTerracotta
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun TimetableScreen(
    schedule: CourseSchedule,
    onBack: () -> Unit,
    onSave: (CourseSchedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    var profiles by remember(schedule.termName, schedule.semesterStart) {
        mutableStateOf(schedule.timeProfiles.sortedBy { it.effectiveFrom })
    }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    fun persist(next: List<TimetableProfile>) {
        profiles = next.sortedBy { it.effectiveFrom }
        onSave(schedule.copy(timeProfiles = profiles))
    }

    val index = editingIndex
    if (index != null && index in profiles.indices) {
        ProfileEditor(
            profile = profiles[index],
            canDelete = profiles.size > 1,
            onBack = { editingIndex = null },
            onApply = { updated ->
                val next = profiles.toMutableList().apply { this[index] = updated }
                persist(next)
                editingIndex = null
            },
            onDelete = {
                val next = profiles.toMutableList().apply { removeAt(index) }
                persist(next)
                editingIndex = null
            },
            modifier = modifier,
        )
    } else {
        ProfileList(
            profiles = profiles,
            today = LocalDate.now(),
            onBack = onBack,
            onEdit = { editingIndex = it },
            onAdd = { persist(profiles + defaultProfile(LocalDate.now())) },
            modifier = modifier,
        )
    }
}

@Composable
private fun ProfileList(
    profiles: List<TimetableProfile>,
    today: LocalDate,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeIndex = profiles.indices
        .filter { !profiles[it].effectiveFrom.isAfter(today) }
        .maxByOrNull { profiles[it].effectiveFrom }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIconButton(icon = ClassrollIcons.ArrowLeft, contentDescription = "返回", onClick = onBack)
                Text("作息时间", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).padding(start = 4.dp))
                InfoChip("按日期切换", ClassrollIcons.Clock)
            }
        }
        if (profiles.isEmpty()) {
            item {
                ClassrollCard {
                    Text("还没有作息时间", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "先新建一套，填好每节课的上下课时间。课程表用节次时，扫描会按这套作息算时间。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        items(profiles.size) { index ->
            val profile = profiles[index]
            ProfileCard(
                profile = profile,
                active = index == activeIndex,
                onClick = { onEdit(index) },
            )
        }
        item {
            SecondaryButton(
                text = "新建一套作息",
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                icon = ClassrollIcons.Plus,
            )
        }
    }
}

@Composable
private fun ProfileCard(profile: TimetableProfile, active: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (active) PaperGood.copy(alpha = .45f) else MaterialTheme.colorScheme.outline),
    ) {
        Column {
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).background(
                        if (active) PaperGood.copy(alpha = .12f) else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(LocalSkin.current.corner(11.dp)),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(ClassrollIcons.Clock, contentDescription = null, tint = if (active) PaperGood else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (active) {
                            Spacer(Modifier.width(7.dp))
                            Surface(shape = RoundedCornerShape(LocalSkin.current.corner(999.dp)), color = PaperGood.copy(alpha = .12f)) {
                                Text(
                                    "当前使用",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PaperGood,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Text(
                        "${profile.effectiveFrom} 起 · ${profile.sections.size} 节课",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(ClassrollIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (profile.sections.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Text(
                    profile.sections.take(4).joinToString("  ") { minuteLabel(it.startMinute) } + if (profile.sections.size > 4) "  …" else "",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
        }
    }
}

@Composable
private fun ProfileEditor(
    profile: TimetableProfile,
    canDelete: Boolean,
    onBack: () -> Unit,
    onApply: (TimetableProfile) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember(profile) { mutableStateOf(profile.name) }
    var effectiveFrom by remember(profile) { mutableStateOf(profile.effectiveFrom) }
    var sections by remember(profile) { mutableStateOf(profile.sections.sortedBy { it.index }) }
    val groups = profile.groups
    var uniform by remember { mutableStateOf(false) }
    var duration by remember { mutableIntStateOf(defaultDuration(profile.sections)) }
    var gap by remember { mutableIntStateOf(10) }
    var editingSection by remember { mutableStateOf<TimetableSection?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun applyUniform() {
        if (sections.isEmpty()) return
        var cursor = sections.first().startMinute
        sections = sections.indices.map { index ->
            val start = cursor
            val end = start + duration
            cursor = end + gap
            TimetableSection(index + 1, start, end)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconButton(icon = ClassrollIcons.ArrowLeft, contentDescription = "返回", onClick = onBack)
            Text("编辑作息", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(start = 2.dp))
            TextButton(
                onClick = {
                    onApply(
                        profile.copy(
                            name = name.trim().ifBlank { "作息" },
                            effectiveFrom = effectiveFrom,
                            sections = sections.sortedBy { it.index },
                            groups = groups,
                        ),
                    )
                },
                enabled = sections.isNotEmpty(),
            ) {
                Text("完成", style = MaterialTheme.typography.labelLarge)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("作息名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ClassrollCard(contentPadding = PaddingValues(0.dp)) {
                SettingLine(
                    icon = ClassrollIcons.Calendar,
                    title = "生效日期",
                    subtitle = "",
                    value = effectiveFrom.toString(),
                    onClick = { showDatePicker = true },
                )
            }
            ClassrollCard(contentPadding = PaddingValues(0.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp).padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("每节课时长相同", style = MaterialTheme.typography.titleMedium)
                    }
                    Switch(
                        checked = uniform,
                        onCheckedChange = {
                            uniform = it
                            if (it) applyUniform()
                        },
                    )
                }
                if (uniform) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 13.dp))
                    StepperLine("每节时长", duration, 25, 90, 5) { duration = it; applyUniform() }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 13.dp))
                    StepperLine("课间休息", gap, 0, 40, 5) { gap = it; applyUniform() }
                }
            }
            if (sections.isEmpty()) {
                ClassrollCard {
                    Text("还没有节次", style = MaterialTheme.typography.titleMedium)
                    Text("先设置上午、下午、晚上各几节。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
            } else {
                SectionPart.entries.forEach { part ->
                    val partSections = sections.filter { partFor(it.index, groups) == part }.sortedBy { it.index }
                    if (partSections.isEmpty()) return@forEach
                    Text(
                        part.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    )
                    ClassrollCard(contentPadding = PaddingValues(0.dp)) {
                        partSections.forEachIndexed { index, section ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 13.dp))
                            SectionRow(section, onClick = { editingSection = section })
                        }
                    }
                }
            }
            if (canDelete) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(ClassrollIcons.Trash, contentDescription = null, tint = PaperTerracotta, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("删除这套作息", style = MaterialTheme.typography.labelMedium, color = PaperTerracotta)
                }
            }
        }
    }

    editingSection?.let { section ->
        TimeRangeDialog(
            section = section,
            onDismiss = { editingSection = null },
            onConfirm = { start, end ->
                sections = sections.map {
                    if (it.index == section.index) section.copy(startMinute = start, endMinute = end) else it
                }.sortedBy { it.index }
                editingSection = null
            },
        )
    }
    if (showDatePicker) {
        ScheduleDatePickerDialog(
            initial = effectiveFrom,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                effectiveFrom = it
                showDatePicker = false
            },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这套作息？") },
            text = { Text("删除后，这个生效日期起会改用更早的那套作息。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("删除", color = PaperTerracotta) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }
}

@Composable
private fun SectionRow(section: TimetableSection, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("第 ${section.index} 节", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(
            "${minuteLabel(section.startMinute)}–${minuteLabel(section.endMinute)}",
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Icon(ClassrollIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(LocalSkin.current.corner(10.dp))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
        Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(ClassrollIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StepperLine(label: String, value: Int, min: Int, max: Int, step: Int, onChange: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        StepperButton("−") { onChange((value - step).coerceAtLeast(min)) }
        Text(
            "$value 分钟",
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(74.dp),
            textAlign = TextAlign.Center,
        )
        StepperButton("+") { onChange((value + step).coerceAtMost(max)) }
    }
}

@Composable
private fun StepperButton(text: String, onClick: () -> Unit) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRangeDialog(
    section: TimetableSection,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    var start by remember { mutableIntStateOf(section.startMinute) }
    var end by remember { mutableIntStateOf(section.endMinute) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("第 ${section.index} 节时间") },
        text = {
            Column {
                TimeTapRow("开始", start) { pickingStart = true }
                Spacer(Modifier.height(8.dp))
                TimeTapRow("结束", end) { pickingEnd = true }
                if (end <= start) {
                    Text("结束时间要晚于开始时间", style = MaterialTheme.typography.bodySmall, color = PaperTerracotta, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(start, end) }, enabled = end > start) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        containerColor = MaterialTheme.colorScheme.background,
    )
    if (pickingStart) {
        ClockDialog("选择开始时间", start, onDismiss = { pickingStart = false }) { start = it; pickingStart = false }
    }
    if (pickingEnd) {
        ClockDialog("选择结束时间", end, onDismiss = { pickingEnd = false }) { end = it; pickingEnd = false }
    }
}

@Composable
private fun TimeTapRow(label: String, minute: Int, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(minuteLabel(minute), style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace)
            Icon(ClassrollIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClockDialog(title: String, initialMinute: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = initialMinute / 60, initialMinute = initialMinute % 60, is24Hour = true)
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
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(10.dp))
                    TimePicker(state = state)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("确定") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDatePickerDialog(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                } else {
                    onDismiss()
                }
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    ) {
        ChineseLocale {
            DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.background))
        }
    }
}

@Composable
fun TimetableSectionCountDialog(groups: TimetableGroups, onDismiss: () -> Unit, onConfirm: (TimetableGroups) -> Unit) {
    var morning by remember { mutableIntStateOf(groups.morning.coerceIn(1, 6)) }
    var afternoon by remember { mutableIntStateOf(groups.afternoon.coerceIn(1, 6)) }
    var evening by remember { mutableIntStateOf(groups.evening.coerceIn(1, 6)) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = .32f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = LocalSkin.current.corner(26.dp), topEnd = LocalSkin.current.corner(26.dp)),
                color = MaterialTheme.colorScheme.background,
                border = LocalSkin.current.cardBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                    Text("课表节数设置", style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CountColumn("上午", morning, Modifier.weight(1f)) { morning = it }
                        CountColumn("下午", afternoon, Modifier.weight(1f)) { afternoon = it }
                        CountColumn("晚上", evening, Modifier.weight(1f)) { evening = it }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton("取消", onDismiss, Modifier.weight(1f))
                        androidx.compose.material3.Button(
                            onClick = {
                                if (morning + afternoon + evening > 0) {
                                    onConfirm(TimetableGroups(morning, afternoon, evening))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text("确定", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountColumn(label: String, value: Int, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (value - 3).coerceAtLeast(0))
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxWidth().height(240.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(10) { index ->
                val number = index + 1
                val selected = number == value
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(if (selected) PaperInk else Color.Transparent, RoundedCornerShape(LocalSkin.current.corner(10.dp)))
                        .clickable { onChange(number) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        number.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

private fun minuteLabel(value: Int): String = "%02d:%02d".format(value / 60 % 24, value % 60)

private fun defaultDuration(sections: List<TimetableSection>): Int {
    val first = sections.firstOrNull() ?: return 45
    return (first.endMinute - first.startMinute).coerceIn(25, 90)
}

private fun partFor(index: Int, groups: TimetableGroups): SectionPart = when {
    index <= groups.morning -> SectionPart.Morning
    index <= groups.morning + groups.afternoon -> SectionPart.Afternoon
    else -> SectionPart.Evening
}

private fun partStart(part: SectionPart): Int = when (part) {
    SectionPart.Morning -> 8 * 60
    SectionPart.Afternoon -> 14 * 60 + 30
    SectionPart.Evening -> 19 * 60 + 30
}

fun resizeSections(
    current: List<TimetableSection>,
    oldGroups: TimetableGroups,
    newGroups: TimetableGroups,
): List<TimetableSection> {
    val rebuilt = SectionPart.entries.flatMap { part ->
        val count = when (part) {
            SectionPart.Morning -> newGroups.morning
            SectionPart.Afternoon -> newGroups.afternoon
            SectionPart.Evening -> newGroups.evening
        }
        val existing = current.filter { partFor(it.index, oldGroups) == part }.sortedBy { it.startMinute }
        val result = existing.take(count).toMutableList()
        var cursor = existing.lastOrNull()?.endMinute
        var start = if (cursor != null) cursor + 10 else partStart(part)
        while (result.size < count) {
            result += TimetableSection(result.size + 1, start, start + 45)
            cursor = start + 45
            start = start + 55
        }
        result
    }
    return rebuilt.mapIndexed { index, section -> section.copy(index = index + 1) }
}

private fun defaultProfile(date: LocalDate): TimetableProfile {
    val morning = listOf(480 to 525, 535 to 580, 610 to 655, 665 to 710)
    val afternoon = listOf(870 to 915, 925 to 970, 1000 to 1045, 1055 to 1100)
    val evening = listOf(1170 to 1215, 1225 to 1270)
    val all = morning + afternoon + evening
    return TimetableProfile(
        effectiveFrom = date,
        name = "新作息",
        sections = all.mapIndexed { index, (start, end) -> TimetableSection(index + 1, start, end) },
        groups = TimetableGroups(4, 4, 2),
    )
}
