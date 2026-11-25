package com.classroll.photo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.classroll.photo.model.AnalysisStatus
import com.classroll.photo.model.PhotoGroup
import com.classroll.photo.model.PhotoItem
import com.classroll.photo.model.ScanProgress
import com.classroll.photo.model.ScanStage
import com.classroll.photo.model.Verdict
import com.classroll.photo.data.ArchiveResult
import com.classroll.photo.ui.ClassrollCard
import com.classroll.photo.ui.ClassrollIcons
import com.classroll.photo.ui.InfoChip
import com.classroll.photo.ui.PrimaryButton
import com.classroll.photo.ui.SecondaryButton
import com.classroll.photo.ui.ScreenTitle
import com.classroll.photo.ui.StatSummary
import com.classroll.photo.ui.VerdictBadge
import com.classroll.photo.ui.VerdictTabs
import com.classroll.photo.ui.formatCount
import com.classroll.photo.ui.theme.corner
import com.classroll.photo.ui.theme.LocalSkin
import com.classroll.photo.ui.theme.courseColor
import com.classroll.photo.ui.theme.PaperBackground
import com.classroll.photo.ui.theme.PaperBlue
import com.classroll.photo.ui.theme.PaperGood
import com.classroll.photo.ui.theme.PaperHighlight
import com.classroll.photo.ui.theme.PaperInk
import com.classroll.photo.ui.theme.PaperPurple
import com.classroll.photo.ui.theme.PaperTerracotta
import kotlin.math.roundToInt

@Composable
fun ScanScreen(
    progress: ScanProgress,
    counts: Map<Verdict, Int>,
    hasSchedule: Boolean,
    onStartFullScan: () -> Unit,
    onRescanAll: () -> Unit,
    onCancel: () -> Unit,
    onRescan: () -> Unit,
    onViewResults: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val running = progress.running
    val done = progress.stage == ScanStage.Done
    val failed = progress.stage == ScanStage.Error || progress.stage == ScanStage.Cancelled
    Column(
        modifier = modifier.fillMaxSize().padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 16.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            ScanProgressRing(progress)
        }
        if (!done && !failed) {
            val steps = listOf(
                ScanStep("读取照片"),
                ScanStep("课程时间匹配"),
                ScanStep("粗筛"),
                ScanStep("中文文字识别"),
            )
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                steps.forEachIndexed { index, step ->
                    ScanStepRow(step, index, scanStepState(progress, index))
                }
            }
        } else {
            if (done) {
                ScanResultCard(progress, counts)
                Spacer(Modifier.height(12.dp))
            }
            if (failed) {
                ClassrollCard {
                    Text(progress.message, style = MaterialTheme.typography.bodyMedium, color = PaperTerracotta)
                }
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.weight(1f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                running -> PrimaryButton(
                    text = progress.message,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    loading = true,
                    keepPrimaryColor = true,
                )
                done && progress.total > 0 -> PrimaryButton(
                    text = "查看 ${progress.total} 张结果",
                    onClick = onViewResults,
                    modifier = Modifier.fillMaxWidth(),
                    icon = ClassrollIcons.ArrowRight,
                )
                else -> PrimaryButton(
                    text = "扫描全相册",
                    onClick = onStartFullScan,
                    modifier = Modifier.fillMaxWidth(),
                    icon = ClassrollIcons.Scan,
                )
            }
            if (running) {
                SecondaryButton(
                    text = "取消扫描",
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    icon = ClassrollIcons.Close,
                )
            } else if (done || failed) {
                SecondaryButton(
                    text = "重新识别",
                    onClick = onRescan,
                    modifier = Modifier.fillMaxWidth(),
                    icon = ClassrollIcons.Refresh,
                )
            }
            if (hasSchedule && !running) {
                SecondaryButton(
                    text = "重新全量扫描",
                    onClick = onRescanAll,
                    modifier = Modifier.fillMaxWidth(),
                    icon = ClassrollIcons.Refresh,
                )
            }
        }
    }
}

private data class ScanStep(val title: String)

private enum class ScanStepState { Pending, Active, Done }

private fun scanStepState(progress: ScanProgress, index: Int): ScanStepState {
    if (progress.stage == ScanStage.Done) return ScanStepState.Done
    val activeIndex = when (progress.stage) {
        ScanStage.Querying -> 0
        ScanStage.Matching -> 1
        ScanStage.Coarse -> 2
        ScanStage.Ocr -> 3
        else -> return ScanStepState.Pending
    }
    return when {
        index < activeIndex -> ScanStepState.Done
        index == activeIndex -> ScanStepState.Active
        else -> ScanStepState.Pending
    }
}

@Composable
private fun ScanProgressRing(progress: ScanProgress) {
    val done = progress.stage == ScanStage.Done
    val ringLine = LocalSkin.current.line
    val ringInk = PaperInk
    val ringAccent = PaperTerracotta
    val ringDone = PaperGood
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.size(262.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 10.dp.toPx()
                val inset = stroke / 2f + 10.dp.toPx()
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                drawCircle(color = ringLine, radius = arcSize.width / 2f, style = Stroke(stroke))
                drawArc(
                    color = if (done) ringDone else ringInk,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.fraction,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                repeat(12) { index ->
                    val angle = Math.toRadians((index * 30).toDouble())
                    val radius = arcSize.width / 2f + 16.dp.toPx()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = if (index / 12f <= progress.fraction) ringAccent else ringLine,
                        radius = if (index / 12f <= progress.fraction) 2.4.dp.toPx() else 1.8.dp.toPx(),
                        center = Offset(
                            center.x + (kotlin.math.cos(angle) * radius).toFloat(),
                            center.y + (kotlin.math.sin(angle) * radius).toFloat(),
                        ),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (done) {
                    Icon(ClassrollIcons.Check, contentDescription = null, tint = PaperGood, modifier = Modifier.size(34.dp))
                }
                Text(formatCount(progress.completed), style = MaterialTheme.typography.displaySmall)
                Text(if (progress.total > 0) "共 ${progress.total} 张" else "尚未读取照片", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (progress.running && progress.total > 0) {
                    Text(
                        "${(progress.fraction * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = PaperTerracotta,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
        if (done || progress.running) {
            Text(
                if (done) "识别完成 · 用时 %.1f 秒".format(progress.elapsedMillis / 1000f) else progress.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ScanStepRow(step: ScanStep, index: Int, state: ScanStepState) {
    val active = state == ScanStepState.Active
    val done = state == ScanStepState.Done
    ClassrollCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).background(
                    when {
                        done -> PaperGood
                        active -> PaperHighlight
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                if (done) {
                    Icon(ClassrollIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                } else {
                    Text("${index + 1}", style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(step.title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                when (state) {
                    ScanStepState.Pending -> "待机"
                    ScanStepState.Active -> "进行中"
                    ScanStepState.Done -> "完成"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (done) PaperGood else if (active) PaperTerracotta else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScanResultCard(progress: ScanProgress, counts: Map<Verdict, Int>) {
    ClassrollCard(contentPadding = PaddingValues(0.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(7.dp).background(PaperTerracotta, CircleShape))
            Spacer(Modifier.width(7.dp))
            Text("找到 ${progress.total} 张候选", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(modifier = Modifier.padding(vertical = 13.dp)) {
            Verdict.entries.forEach { verdict ->
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text((counts[verdict] ?: 0).toString(), style = MaterialTheme.typography.titleLarge)
                    Text(verdict.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ConfirmScreen(
    photos: List<PhotoItem>,
    groups: Map<String, PhotoGroup>,
    filter: Verdict,
    selectedIds: Set<String>,
    onFilterChange: (Verdict) -> Unit,
    onTogglePhoto: (String) -> Unit,
    onOpenPhotos: (List<PhotoItem>, Int) -> Unit,
    onSelectAll: () -> Unit,
    onInvert: () -> Unit,
    onSkipGroup: (String) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val counts = remember(photos) { photos.groupingBy { it.verdict }.eachCount() }
    val visiblePhotos = photos.filter { it.verdict == filter }
    val visibleGroupIds = visiblePhotos.map { it.groupId }.distinct()

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp)) {
            ScreenTitle(
                title = "确认照片",
                trailing = { InfoChip("${photos.size} 张候选") },
            )
            Spacer(Modifier.height(12.dp))
            VerdictTabs(selected = filter, counts = counts, onSelect = onFilterChange)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            visibleGroupIds.forEach { groupId ->
                val group = groups[groupId] ?: return@forEach
                val groupPhotos = visiblePhotos.filter { it.groupId == groupId }
                item(key = "header-$groupId", span = { GridItemSpan(maxLineSpan) }) {
                    PhotoGroupHeader(group, groupColor(group.course)) { onSkipGroup(groupId) }
                }
                items(groupPhotos, key = { it.id }) { photo ->
                    PhotoCard(
                        photo = photo,
                        selected = photo.id in selectedIds,
                        onToggle = { onTogglePhoto(photo.id) },
                        onOpen = { onOpenPhotos(visiblePhotos, visiblePhotos.indexOf(photo)) },
                    )
                }
            }
            if (visibleGroupIds.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Text("这一档是空的。", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        BatchBar(
            totalCount = photos.size,
            selectedCount = selectedIds.size,
            onSelectAll = onSelectAll,
            onInvert = onInvert,
            onFinish = onFinish,
        )
    }
}

@Composable
private fun groupColor(course: String): Color = LocalSkin.current.courseColor(course)

@Composable
private fun PhotoGroupHeader(
    group: PhotoGroup,
    color: Color,
    onSkip: () -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(LocalSkin.current.corner(3.dp))))
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(group.course, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(7.dp))
                    Text(group.session, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(group.meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onSkip, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                Text("整组不选", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun PhotoCard(
    photo: PhotoItem,
    selected: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(LocalSkin.current.corner(14.dp)),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) PaperInk else MaterialTheme.colorScheme.outline),
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1.32f).clip(RoundedCornerShape(topStart = LocalSkin.current.corner(13.dp), topEnd = LocalSkin.current.corner(13.dp))),
            ) {
                AsyncImage(
                    model = photo.imageUri,
                    contentDescription = photo.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(7.dp),
                    shape = RoundedCornerShape(LocalSkin.current.corner(999.dp)),
                    color = Color.Black.copy(alpha = .55f),
                ) {
                    Text(
                        when (photo.status) {
                            AnalysisStatus.OcrDone -> "文字已识别"
                            AnalysisStatus.OcrSkipped -> "已跳过文字"
                            AnalysisStatus.Failed -> "识别失败"
                            else -> "识别中"
                        },
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (selected) PaperInk else Color.White.copy(alpha = .94f))
                        .clickable(onClick = onToggle)
                        .semantics { contentDescription = if (selected) "取消选择 ${photo.title}" else "选择 ${photo.title}" },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(ClassrollIcons.Check, contentDescription = null, tint = PaperBackground, modifier = Modifier.size(18.dp))
                    } else {
                        Box(modifier = Modifier.size(16.dp).border(1.5.dp, PaperInk.copy(alpha = .35f), RoundedCornerShape(LocalSkin.current.corner(4.dp))))
                    }
                }
                VerdictBadge(photo.verdict, modifier = Modifier.align(Alignment.BottomStart).padding(7.dp))
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(photo.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(4.dp))
                    Text(photo.timeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    photo.evidenceSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (photo.error != null) PaperTerracotta else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun BatchBar(
    totalCount: Int,
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onInvert: () -> Unit,
    onFinish: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 10.dp) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onSelectAll, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)) {
                Text("全选", style = MaterialTheme.typography.labelMedium)
            }
            TextButton(onClick = onInvert, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)) {
                Text("反选", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.width(5.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("$selectedCount 张已选", style = MaterialTheme.typography.titleMedium)
                Text("共 $totalCount 张候选", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(6.dp))
            PrimaryButton(text = "完成", onClick = onFinish, modifier = Modifier.width(88.dp))
        }
    }
}

@Composable
fun ResultScreen(
    photos: List<PhotoItem>,
    selectedCount: Int,
    elapsedMillis: Long,
    archive: ArchiveResult?,
    archiving: Boolean,
    archiveDone: Int,
    archiveTotal: Int,
    onViewLibrary: () -> Unit,
    onBackConfirm: () -> Unit,
    onBackHome: () -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val counts = remember(photos) { photos.groupingBy { it.verdict }.eachCount() }
    val failed = photos.count { it.status == AnalysisStatus.Failed }
    val finished = archive != null
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenTitle(
                title = when {
                    finished -> "归档完成"
                    archiving -> "正在归档"
                    else -> "识别完成"
                },
                trailing = if (finished) {
                    {
                        TextButton(onClick = onRescan) {
                            Text("再次扫描", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else {
                    null
                },
            )
        }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                SuccessMark()
                Spacer(Modifier.height(12.dp))
                Text(
                    when {
                        finished -> "已加入归档"
                        archiving -> "正在写入归档…"
                        else -> "识别完成"
                    },
                    style = MaterialTheme.typography.displaySmall,
                )
                if (archiving) {
                    Text(
                        "已归档 $archiveDone / $archiveTotal 张",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
        if (archiving) {
            item {
                ClassrollCard {
                    LinearProgressIndicator(
                        progress = { if (archiveTotal > 0) archiveDone.toFloat() / archiveTotal else 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = PaperInk,
                        trackColor = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "正在写入 Pictures/课程归档/，请保持 App 在前台。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            StatSummary(
                listOf(
                    photos.size.toString() to "候选照片",
                    selectedCount.toString() to "已勾选",
                    if (finished) (archive?.copied ?: 0).toString() to "已归档" else failed.toString() to "识别失败",
                ),
            )
        }
        item {
            ClassrollCard {
                Text("四档结果", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Verdict.entries.forEach { verdict ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(verdict.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text((counts[verdict] ?: 0).toString(), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        if (archive != null) {
            item {
                ClassrollCard(contentPadding = PaddingValues(0.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).background(PaperGood, CircleShape))
                        Spacer(Modifier.width(7.dp))
                        Text("归档记录", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(archive.term, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        ArchiveCount("已归档", archive.copied, PaperGood, Modifier.weight(1f))
                        ArchiveCount("课程", archive.albums.size, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                        ArchiveCount("失败", archive.failed, if (archive.failed > 0) PaperTerracotta else MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
                    }
                }
            }
            val byCourse = archive.entries.filter { it.copied }.groupBy { it.course }
            if (byCourse.isNotEmpty()) {
                item {
                    ClassrollCard {
                        Text("课程相册", style = MaterialTheme.typography.titleMedium)
                        byCourse.forEach { (course, files) ->
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(course, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    Text("${files.size} 张", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                files.take(4).forEach { file ->
                                    Text(
                                        file.fileName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (files.size > 4) {
                                    Text("另有 ${files.size - 4} 张", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            if (archive.failed > 0) {
                item {
                    Surface(shape = MaterialTheme.shapes.small, color = PaperTerracotta.copy(alpha = .08f), border = BorderStroke(1.dp, PaperTerracotta.copy(alpha = .25f))) {
                        Column(modifier = Modifier.padding(13.dp)) {
                            Text("${archive.failed} 张没有复制成功", style = MaterialTheme.typography.titleMedium, color = PaperTerracotta)
                            archive.entries.filter { it.error != null }.take(3).forEach { entry ->
                                Text("${entry.sourceName}：${entry.error}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
        item {
            Surface(shape = MaterialTheme.shapes.small, color = PaperGood.copy(alpha = .09f), border = BorderStroke(1.dp, PaperGood.copy(alpha = .22f))) {
                Row(modifier = Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(ClassrollIcons.Shield, contentDescription = null, tint = PaperGood, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("原图未改动", style = MaterialTheme.typography.titleMedium, color = PaperGood)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (finished) {
                    PrimaryButton("查看归档相册", onViewLibrary, Modifier.fillMaxWidth(), icon = ClassrollIcons.FolderCheck)
                    SecondaryButton("回到课程", onBackHome, Modifier.fillMaxWidth(), icon = ClassrollIcons.Book)
                } else if (!archiving) {
                    PrimaryButton("返回确认", onBackConfirm, Modifier.fillMaxWidth(), icon = ClassrollIcons.ArrowLeft)
                    SecondaryButton("回到课程", onBackHome, Modifier.fillMaxWidth(), icon = ClassrollIcons.Book)
                }
            }
        }
    }
}

@Composable
private fun ArchiveCount(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value.toString(), style = MaterialTheme.typography.titleLarge, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SuccessMark() {
    val good = PaperGood
    Box(modifier = Modifier.size(82.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 5.dp.toPx()
            drawCircle(good.copy(alpha = .10f), radius = size.minDimension / 2f - stroke / 2f, style = Stroke(stroke))
            drawArc(
                color = good,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Icon(ClassrollIcons.Check, contentDescription = null, tint = PaperGood, modifier = Modifier.size(29.dp))
    }
}
