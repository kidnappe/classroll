package com.classroll.photo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.classroll.photo.data.ArchiveItem
import com.classroll.photo.ui.ClassrollCard
import com.classroll.photo.ui.ClassrollIcons
import com.classroll.photo.ui.PaperTape
import com.classroll.photo.ui.ScreenTitle
import com.classroll.photo.ui.VerticalDashedDivider
import com.classroll.photo.ui.formatCount
import com.classroll.photo.ui.theme.corner
import com.classroll.photo.ui.theme.LocalSkin
import com.classroll.photo.ui.theme.PaperInk
import com.classroll.photo.ui.theme.PaperTerracotta
import java.time.LocalDate

private enum class LibraryMode(val label: String) { Week("按周"), Course("按课程") }

@Composable
fun LibraryScreen(
    items: List<ArchiveItem>,
    added: Int,
    onScan: () -> Unit,
    onOpenPhotos: (List<ArchiveItem>, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf(LibraryMode.Week) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val weekGroups = remember(items) {
        items.groupBy { it.term to it.week }
            .toList()
            .sortedWith(
                compareByDescending<Pair<Pair<String, Int>, List<ArchiveItem>>> { it.first.first }
                    .thenByDescending { it.first.second },
            )
    }
    val courseGroups = remember(items) {
        items.groupBy { it.term to it.course }
            .toList()
            .sortedWith(
                compareByDescending<Pair<Pair<String, String>, List<ArchiveItem>>> { it.first.first }
                    .thenBy { it.first.second },
            )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenTitle(title = "归档相册")
        }
        item {
            LibrarySummary(total = items.size, albumCount = courseGroups.size, added = added)
        }
        item {
            LibraryModeTabs(mode) { mode = it }
        }
        if (items.isEmpty()) {
            item {
                ClassrollCard {
                    Text("还没有归档过照片", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onScan) {
                        Text("去扫描", style = MaterialTheme.typography.labelMedium)
                        Icon(ClassrollIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                }
            }
        } else if (mode == LibraryMode.Week) {
            weekGroups.forEach { (key, list) ->
                val week = key.second
                val keyStr = "week-${key.first}-$week"
                val dates = list.mapNotNull { it.date }
                val courses = list.map { it.course }.distinct().size
                item(key = keyStr) {
                    GroupCard(
                        title = if (week > 0) "第 $week 周" else "未识别周次",
                        subtitle = listOfNotNull(rangeLabel(dates).ifBlank { null }, "${list.size} 张").joinToString(" · "),
                        badge = "$courses 门课",
                        expanded = expanded[keyStr] == true,
                        onToggle = { expanded[keyStr] = !(expanded[keyStr] ?: false) },
                    ) {
                        val sessions = list.groupBy { it.course to it.date }.toList()
                            .sortedWith(compareBy({ it.first.second ?: LocalDate.MAX }, { it.first.first }))
                        sessions.forEachIndexed { index, (sessionKey, photos) ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                            SessionBlock(
                                title = sessionKey.first,
                                subtitle = dateLabel(sessionKey.second),
                                photos = photos,
                                onOpen = { index -> onOpenPhotos(photos, index) },
                            )
                        }
                    }
                }
            }
        } else {
            courseGroups.forEach { (key, list) ->
                val keyStr = "course-${key.first}-${key.second}"
                val sessionCount = list.map { it.week to it.date }.distinct().size
                item(key = keyStr) {
                    GroupCard(
                        title = key.second,
                        subtitle = "${list.size} 张 · $sessionCount 次课",
                        badge = key.first,
                        expanded = expanded[keyStr] == true,
                        onToggle = { expanded[keyStr] = !(expanded[keyStr] ?: false) },
                    ) {
                        val sessions = list.groupBy { it.week to it.date }.toList()
                            .sortedWith(compareBy({ it.first.first }, { it.first.second ?: LocalDate.MAX }))
                        sessions.forEachIndexed { index, (sessionKey, photos) ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                            SessionBlock(
                                title = if (sessionKey.first > 0) "第 ${sessionKey.first} 周" else "未识别周次",
                                subtitle = dateLabel(sessionKey.second),
                                photos = photos,
                                onOpen = { index -> onOpenPhotos(photos, index) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySummary(total: Int, albumCount: Int, added: Int) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = MaterialTheme.shapes.medium,
                    ambientColor = PaperInk.copy(alpha = .08f),
                    spotColor = PaperInk.copy(alpha = .18f),
                ),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = LocalSkin.current.cardBorder,
        ) {
            Row(modifier = Modifier.padding(vertical = 16.dp)) {
                listOf(
                    formatCount(total) to "已归档照片",
                    albumCount.toString() to "课程相册",
                    formatCount(added) to "本次新增",
                ).forEachIndexed { index, (value, label) ->
                    if (index > 0) VerticalDashedDivider(height = 38.dp)
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(value, style = MaterialTheme.typography.titleLarge, color = if (index == 0) PaperTerracotta else MaterialTheme.colorScheme.onSurface)
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
        PaperTape(Modifier.align(Alignment.TopCenter))
        Text(
            "本周",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = PaperTerracotta,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 14.dp).rotate(5f),
        )
    }
}

@Composable
private fun LibraryModeTabs(selected: LibraryMode, onSelect: (LibraryMode) -> Unit) {
    val skin = LocalSkin.current
    val shell = RoundedCornerShape(LocalSkin.current.corner(14.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, shell)
            .then(if (skin.hardShadow != null) Modifier.border(skin.hairline, shell) else Modifier)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LibraryMode.entries.forEach { mode ->
            val active = mode == selected
            Surface(
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(LocalSkin.current.corner(11.dp)),
                color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                onClick = { onSelect(mode) },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        mode.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupCard(
    title: String,
    subtitle: String,
    badge: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = LocalSkin.current.cardBorder,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                }
                Text(badge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (expanded) ClassrollIcons.ChevronDown else ClassrollIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), content = content)
            }
        }
    }
}

@Composable
private fun SessionBlock(
    title: String,
    subtitle: String,
    photos: List<ArchiveItem>,
    onOpen: (Int) -> Unit,
) {
    val missing = photos.count { !it.available }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${photos.size} 张", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
        if (missing > 0) {
            Text(
                "$missing 张原图已失效",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            photos.take(5).forEachIndexed { index, photo ->
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(LocalSkin.current.corner(8.dp)))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onOpen(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (photo.available) {
                        AsyncImage(
                            model = photo.uri,
                            contentDescription = photo.fileName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            ClassrollIcons.Alert,
                            contentDescription = "原图已失效",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            if (photos.size > 5) {
                Box(
                    modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(LocalSkin.current.corner(8.dp))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+${photos.size - 5}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

private fun weekdayLabel(value: Int): String = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")[value - 1]

private fun dateLabel(date: LocalDate?): String = date?.let {
    "${it.monthValue}月${it.dayOfMonth}日 ${weekdayLabel(it.dayOfWeek.value)}"
}.orEmpty()

private fun rangeLabel(dates: List<LocalDate>): String {
    if (dates.isEmpty()) return ""
    val min = dates.min()
    val max = dates.max()
    return if (min == max) {
        "${min.monthValue}月${min.dayOfMonth}日"
    } else {
        "${min.monthValue}月${min.dayOfMonth}日–${max.monthValue}月${max.dayOfMonth}日"
    }
}
