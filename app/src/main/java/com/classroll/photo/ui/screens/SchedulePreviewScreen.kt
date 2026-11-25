package com.classroll.photo.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.classroll.photo.model.Course
import com.classroll.photo.model.CourseSchedule
import com.classroll.photo.ui.AppIconButton
import com.classroll.photo.ui.ClassrollCard
import com.classroll.photo.ui.ClassrollIcons
import com.classroll.photo.ui.InfoChip
import com.classroll.photo.ui.PrimaryButton
import com.classroll.photo.ui.SecondaryButton
import com.classroll.photo.ui.SectionHeading
import com.classroll.photo.ui.StatSummary
import com.classroll.photo.ui.theme.corner
import com.classroll.photo.ui.theme.LocalSkin
import com.classroll.photo.ui.theme.courseColor
import com.classroll.photo.ui.theme.PaperBlue
import com.classroll.photo.ui.theme.PaperGood
import com.classroll.photo.ui.theme.PaperInk
import com.classroll.photo.ui.theme.PaperTerracotta
import com.classroll.photo.ui.theme.PaperWarn

@Composable
fun SchedulePreviewScreen(
    initial: CourseSchedule,
    onSave: (CourseSchedule) -> Unit,
    onDiscard: () -> Unit,
    onImportFile: () -> Unit,
    onDeleteSchedule: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    var editing by remember { mutableStateOf<Course?>(null) }
    var editingSiblings by remember { mutableStateOf<List<Course>>(emptyList()) }
    var editingIsNew by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var editingTerm by remember { mutableStateOf(false) }
    var termInput by remember { mutableStateOf("") }
    var newSeq by remember { mutableIntStateOf(0) }
    val conflicts = remember(draft.courses) { countConflicts(draft.courses) }
    val maxWeek = draft.totalWeeks.takeIf { it > 0 } ?: 19
    val maxSection = maxOf(
        draft.timeProfiles.maxOfOrNull { profile -> profile.sections.maxOfOrNull { it.index } ?: 0 } ?: 0,
        draft.courses.flatMap { it.sections }.maxOrNull() ?: 0,
        8,
    )

    BackHandler { confirmDiscard = true }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconButton(icon = ClassrollIcons.ArrowLeft, contentDescription = "返回", onClick = { confirmDiscard = true })
            Text("课表", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(start = 2.dp))
            IconButton(onClick = { confirmDeleteAll = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    ClassrollIcons.Trash,
                    contentDescription = "删除课表配置",
                    tint = PaperTerracotta,
                    modifier = Modifier.size(18.dp),
                )
            }
            TextButton(onClick = onImportFile) {
                Text("导入", style = MaterialTheme.typography.labelLarge)
            }
            TextButton(onClick = { onSave(draft) }, enabled = draft.courses.isNotEmpty()) {
                Text("保存", style = MaterialTheme.typography.labelLarge)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                StatSummary(
                    listOf(
                        draft.courses.size.toString() to "课程安排",
                        draft.uniqueCourseCount.toString() to "课程门数",
                        conflicts.toString() to "时间冲突",
                    ),
                )
            }
            if (conflicts > 0) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = PaperWarn.copy(alpha = .10f),
                        border = BorderStroke(1.dp, PaperWarn.copy(alpha = .30f)),
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(ClassrollIcons.Alert, contentDescription = null, tint = PaperWarn, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "$conflicts 处时间冲突",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item {
                ClassrollCard(contentPadding = PaddingValues(10.dp)) {
                    Text(
                        draft.termName.ifBlank { "未命名学期" },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .padding(start = 4.dp, bottom = 8.dp)
                            .clickable {
                                termInput = draft.termName
                                editingTerm = true
                            },
                    )
                    ScheduleGrid(
                        schedule = draft,
                        maxSection = maxSection,
                        onEdit = { course, siblings ->
                            editing = course
                            editingSiblings = siblings
                            editingIsNew = false
                        },
                        onAdd = { weekday, section ->
                            newSeq++
                            val created = Course(
                                id = "new-${System.currentTimeMillis()}-$newSeq",
                                name = "新课程",
                                weekday = weekday,
                                sections = listOf(section),
                                weeks = (1..maxWeek).toSet(),
                                room = "",
                            )
                            editing = created
                            editingSiblings = listOf(created)
                            editingIsNew = true
                        },
                    )
                }
            }
            item { SectionHeading("课程列表", modifier = Modifier.padding(top = 6.dp)) }
            val grouped = (1..7).mapNotNull { day ->
                draft.courses.filter { it.weekday == day }
                    .sortedBy { it.sections.firstOrNull() ?: 99 }
                    .takeIf { it.isNotEmpty() }
                    ?.let { day to it }
            }
            grouped.forEach { (day, list) ->
                item(key = "day-$day") {
                    Text(
                        "周${weekdayShort(day)} · ${list.size} 条",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    )
                }
                items(list.size, key = { index -> "c-${list[index].id}" }) { index ->
                    val course = list[index]
                    CourseListRow(course) {
                        editing = course
                        editingSiblings = draft.courses.filter {
                            it.weekday == course.weekday && it.sections.any { section -> section in course.sections }
                        }
                        editingIsNew = false
                    }
                }
            }
            item {
                SecondaryButton(
                    text = "添加一门课程",
                    onClick = {
                        newSeq++
                        editing = Course(
                            id = "new-${System.currentTimeMillis()}-$newSeq",
                            name = "新课程",
                            weekday = 1,
                            sections = listOf(1, 2),
                            weeks = (1..maxWeek).toSet(),
                            room = "",
                        )
                        editingIsNew = true
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    icon = ClassrollIcons.Plus,
                )
            }
        }
    }

    editing?.let { course ->
        CourseEditorDialog(
            course = course,
            siblings = editingSiblings,
            isNew = editingIsNew,
            maxWeek = maxWeek,
            maxSection = maxSection,
            onDismiss = { editing = null },
            onSave = { updated ->
                val exists = draft.courses.any { it.id == updated.id }
                draft = draft.copy(
                    courses = if (exists) {
                        draft.courses.map { if (it.id == updated.id) updated else it }
                    } else {
                        draft.courses + updated
                    },
                )
                editing = null
            },
            onDelete = { target ->
                draft = draft.copy(courses = draft.courses.filterNot { it.id == target.id })
                editing = null
            },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("放弃这次导入？") },
            text = { Text("预览里的修改不会保存，当前课表保持不变。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onDiscard()
                }) { Text("放弃", color = PaperTerracotta) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("继续编辑") } },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }
    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("删除整份课表？") },
            text = { Text("所有课程和作息配置都会清空，原图不受影响。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteAll = false
                    onDeleteSchedule()
                }) { Text("删除", color = PaperTerracotta) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("取消") } },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }
    if (editingTerm) {
        AlertDialog(
            onDismissRequest = { editingTerm = false },
            title = { Text("学期名称") },
            text = {
                OutlinedTextField(
                    value = termInput,
                    onValueChange = { termInput = it },
                    singleLine = true,
                    label = { Text("例如 2026-2027-1") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    draft = draft.copy(termName = termInput.trim())
                    editingTerm = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { editingTerm = false }) { Text("取消") } },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }
}

@Composable
private fun ScheduleGrid(
    schedule: CourseSchedule,
    maxSection: Int,
    onEdit: (Course, List<Course>) -> Unit,
    onAdd: (weekday: Int, section: Int) -> Unit,
) {
    val activeDays = (1..7).filter { day -> schedule.courses.any { it.weekday == day } }.ifEmpty { listOf(1, 2, 3, 4, 5) }
    val rowHeight = 50.dp
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(24.dp))
            activeDays.forEach { day ->
                Text(
                    "周${weekdayShort(day)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.width(24.dp)) {
                (1..maxSection).forEach { section ->
                    Box(modifier = Modifier.height(rowHeight), contentAlignment = Alignment.TopCenter) {
                        Text("$section", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            activeDays.forEach { day ->
                Column(modifier = Modifier.weight(1f)) {
                    var section = 1
                    while (section <= maxSection) {
                        val currentSection = section
                        val here = schedule.courses.filter { it.weekday == day && currentSection in it.sections }
                        val course = here.firstOrNull()
                        if (course == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(rowHeight)
                                    .clickable { onAdd(day, currentSection) },
                            )
                            section++
                        } else {
                            val span = generateSequence(currentSection) { it + 1 }.takeWhile { it in course.sections }.count()
                            CourseBlock(
                                course = course,
                                extra = here.size - 1,
                                height = rowHeight * span,
                            ) { onEdit(course, here) }
                            section += span
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseBlock(course: Course, extra: Int, height: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val color = courseColor(course.name)
    Box(modifier = Modifier.fillMaxWidth().height(height)) {
        Surface(
            modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
            shape = RoundedCornerShape(LocalSkin.current.corner(6.dp)),
            color = color.copy(alpha = .14f),
            border = BorderStroke(1.dp, color.copy(alpha = .30f)),
        ) {
            Column(modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)) {
            Text(
                course.name,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                color = PaperInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (course.room.isNotBlank()) {
                Text(
                    course.room,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (extra > 0) {
                Text(
                    "+$extra",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = PaperTerracotta,
                )
            }
            }
        }
    }
}

@Composable
private fun CourseListRow(course: Course, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(LocalSkin.current.corner(12.dp)),
        color = MaterialTheme.colorScheme.surface,
        border = LocalSkin.current.cardBorder,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(courseColor(course.name), RoundedCornerShape(LocalSkin.current.corner(3.dp))),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(course.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    buildString {
                        append(sectionLabel(course))
                        append(" · ${course.weeks.size} 周")
                        if (course.room.isNotBlank()) append(" · ${course.room}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            Icon(ClassrollIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CourseEditorDialog(
    course: Course,
    siblings: List<Course>,
    isNew: Boolean,
    maxWeek: Int,
    maxSection: Int,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit,
    onDelete: (Course) -> Unit,
) {
    val ordered = remember(course, siblings) {
        (if (siblings.isEmpty()) listOf(course) else siblings).distinctBy { it.id }
    }
    var current by remember(course) { mutableStateOf(course) }
    val index = ordered.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
    var name by remember(current) { mutableStateOf(current.name) }
    var teacher by remember(current) { mutableStateOf(current.teacher) }
    var room by remember(current) { mutableStateOf(current.room) }
    var weekday by remember(current) { mutableIntStateOf(current.weekday) }
    var sections by remember(current) { mutableStateOf(current.sections.toSet()) }
    var weeks by remember(current) { mutableStateOf(current.weeks) }
    var error by remember(current) { mutableStateOf<String?>(null) }
    val weekLimit = maxWeek.coerceIn(1, 30)
    val sectionLimit = maxOf(maxSection, 10).coerceAtMost(15)

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
                    .fillMaxWidth(.95f)
                    .fillMaxSize(.92f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isNew) "添加课程" else "编辑课程", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                        if (!isNew && ordered.size > 1) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { current = ordered[(index - 1 + ordered.size) % ordered.size] },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(ClassrollIcons.ChevronLeft, contentDescription = "上一门", modifier = Modifier.size(18.dp))
                                }
                                Text("${index + 1}/${ordered.size}", style = MaterialTheme.typography.labelMedium)
                                IconButton(
                                    onClick = { current = ordered[(index + 1) % ordered.size] },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(ClassrollIcons.ChevronRight, contentDescription = "下一门", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        AppIconButton(icon = ClassrollIcons.Close, contentDescription = "关闭", onClick = onDismiss)
                    }
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("课程名称") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedTextField(
                                value = teacher,
                                onValueChange = { teacher = it },
                                label = { Text("教师") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = room,
                                onValueChange = { room = it },
                                label = { Text("教室") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        EditorLabel("星期")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..7).forEach { day ->
                                ChoiceCell(
                                    text = "周${weekdayShort(day)}",
                                    selected = weekday == day,
                                    modifier = Modifier.weight(1f),
                                ) { weekday = day }
                            }
                        }
                        EditorLabel("节次（可多选，支持跳节）")
                        ChipsGrid(
                            values = (1..sectionLimit).toList(),
                            selected = sections,
                            label = { "$it" },
                        ) { value ->
                            sections = if (value in sections) sections - value else sections + value
                        }
                        EditorLabel("周次")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { weeks = (1..weekLimit).toSet() }) { Text("全选") }
                            TextButton(onClick = { weeks = (1..weekLimit).filter { it % 2 == 1 }.toSet() }) { Text("单周") }
                            TextButton(onClick = { weeks = (1..weekLimit).filter { it % 2 == 0 }.toSet() }) { Text("双周") }
                            TextButton(onClick = { weeks = emptySet() }) { Text("清空") }
                        }
                        ChipsGrid(
                            values = (1..weekLimit).toList(),
                            selected = weeks,
                            label = { "$it" },
                        ) { value ->
                            weeks = if (value in weeks) weeks - value else weeks + value
                        }
                        if (error != null) {
                            Text(error!!, style = MaterialTheme.typography.bodySmall, color = PaperTerracotta, modifier = Modifier.padding(top = 8.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isNew) {
                            TextButton(onClick = { onDelete(current) }) {
                                Icon(ClassrollIcons.Trash, contentDescription = null, tint = PaperTerracotta, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("删除", color = PaperTerracotta)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        SecondaryButton("取消", onDismiss, Modifier.width(88.dp))
                        PrimaryButton(
                            "确定",
                            onClick = {
                                val trimmed = name.trim()
                                error = when {
                                    trimmed.isEmpty() -> "请填写课程名称"
                                    sections.isEmpty() && current.endMinute <= current.startMinute -> "请至少选一个节次"
                                    weeks.isEmpty() -> "请至少选一个周次"
                                    else -> null
                                }
                                if (error == null) {
                                    onSave(
                                        current.copy(
                                            name = trimmed,
                                            teacher = teacher.trim(),
                                            room = room.trim(),
                                            weekday = weekday,
                                            sections = sections.sorted(),
                                            weeks = weeks,
                                        ),
                                    )
                                }
                            },
                            modifier = Modifier.width(96.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun ChipsGrid(values: List<Int>, selected: Set<Int>, label: (Int) -> String, onToggle: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        values.chunked(7).forEach { rowValues ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                rowValues.forEach { value ->
                    ChoiceCell(
                        text = label(value),
                        selected = value in selected,
                        modifier = Modifier.weight(1f),
                    ) { onToggle(value) }
                }
                repeat(7 - rowValues.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ChoiceCell(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.heightIn(min = 38.dp),
        shape = RoundedCornerShape(LocalSkin.current.corner(9.dp)),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

private fun weekdayShort(value: Int): String = listOf("一", "二", "三", "四", "五", "六", "日")[value - 1]

private fun sectionLabel(course: Course): String = if (course.sections.isNotEmpty()) {
    "第${course.sections.joinToString("、")}节"
} else {
    "%02d:%02d–%02d:%02d".format(course.startMinute / 60, course.startMinute % 60, course.endMinute / 60, course.endMinute % 60)
}

@Composable
private fun courseColor(name: String): Color = LocalSkin.current.courseColor(name)

private fun countConflicts(courses: List<Course>): Int {
    var count = 0
    for (i in courses.indices) {
        for (j in i + 1 until courses.size) {
            val a = courses[i]
            val b = courses[j]
            if (a.name == b.name || a.weekday != b.weekday) continue
            if (a.sections.isEmpty() || b.sections.isEmpty()) continue
            if (a.sections.any { it in b.sections } && a.weeks.any { it in b.weeks }) count++
        }
    }
    return count
}
