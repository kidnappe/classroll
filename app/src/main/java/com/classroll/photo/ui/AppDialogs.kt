package com.classroll.photo.ui

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.classroll.photo.data.ArchiveAlbum
import com.classroll.photo.data.ArchivePhoto
import com.classroll.photo.model.AnalysisStatus
import com.classroll.photo.model.CourseSchedule
import com.classroll.photo.model.PhotoGroup
import com.classroll.photo.model.PhotoItem
import com.classroll.photo.model.Verdict
import com.classroll.photo.ui.theme.corner
import com.classroll.photo.ui.theme.LocalSkin
import com.classroll.photo.ui.theme.PaperGood
import com.classroll.photo.ui.theme.PaperTerracotta
import com.classroll.photo.ui.theme.PaperWarn
import kotlin.math.roundToInt

@Composable
fun ImportCourseDialog(
    schedule: CourseSchedule?,
    onDismiss: () -> Unit,
    onPickFile: () -> Unit,
) {
    DialogFrame(onDismiss = onDismiss) {
        DialogHeader(title = "导入课程表", onClose = onDismiss)
        Text(
            "选择本地 JSON 文件。文件只在本机读取，校验通过后保存；不会上传。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )
        if (schedule == null) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("还没有已导入的课程表", style = MaterialTheme.typography.titleMedium)
                    Text("需要开学日期、课程星期、上下课时间、周次和教室。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else {
            Text("当前课表 · ${schedule.courses.size} 门课程", style = MaterialTheme.typography.titleMedium)
            Text(
                "第一周周一 ${schedule.semesterStart} · 课前 ${schedule.bufferBeforeMin} 分钟 · 课后 ${schedule.bufferAfterMin} 分钟",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp, bottom = 10.dp),
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = LocalSkin.current.cardBorder,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    schedule.courses.forEachIndexed { index, course ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 12.dp))
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(course.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Text(course.room, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                "周${weekdayLabel(course.weekday)} ${course.sectionLabel} · ${course.weeks.size} 个教学周" +
                                    course.teacher.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(ClassrollIcons.Shield, contentDescription = null, tint = PaperGood, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text("只读本地文件 · 不联网", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(if (schedule == null) "选择课程 JSON" else "重新选择课程 JSON", onPickFile, Modifier.fillMaxWidth(), icon = ClassrollIcons.File)
    }
}

private fun weekdayLabel(value: Int): String = listOf("一", "二", "三", "四", "五", "六", "日")[value - 1]

@Composable
fun AlbumDialog(
    album: ArchiveAlbum,
    photos: List<ArchivePhoto>,
    onDismiss: () -> Unit,
    onOpenSystem: () -> Unit,
) {
    DialogFrame(onDismiss = onDismiss) {
        DialogHeader(title = album.course, onClose = onDismiss)
        Text(
            "${album.relativePath} · ${formatCount(album.count)} 张",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
        )
        if (photos.isEmpty()) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(ClassrollIcons.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("正在读取课程相册", style = MaterialTheme.typography.titleMedium)
                    Text("这个相册里暂时没有可预览的照片。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 5.dp))
                }
            }
        } else {
            photos.chunked(2).forEach { rowPhotos ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowPhotos.forEach { photo ->
                        Column(modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.35f).clip(RoundedCornerShape(LocalSkin.current.corner(10.dp)))) {
                                AsyncImage(
                                    model = photo.uri,
                                    contentDescription = photo.displayName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                )
                            }
                            Text(
                                photo.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 5.dp),
                            )
                        }
                    }
                    if (rowPhotos.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(ClassrollIcons.FolderCheck, contentDescription = null, tint = PaperGood, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text("系统相册里会显示为「${album.course}」", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("在系统相册打开", onOpenSystem, Modifier.fillMaxWidth(), icon = ClassrollIcons.Image)
        Text(
            "会打开系统相册的“课程归档”目录",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
        )
    }
}

@Composable
fun PhotoViewerDialog(
    photo: PhotoItem,
    group: PhotoGroup?,
    selected: Boolean,
    onDismiss: () -> Unit,
    onReject: () -> Unit,
    onKeep: () -> Unit,
    onRetry: () -> Unit,
) {
    DialogFrame(onDismiss = onDismiss) {
        DialogHeader(title = "照片依据", onClose = onDismiss)
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(LocalSkin.current.corner(13.dp)))) {
            AsyncImage(
                model = photo.imageUri,
                contentDescription = photo.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
        Spacer(Modifier.height(13.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(photo.title, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            VerdictBadge(photo.verdict)
        }
        if (group != null) {
            Text(
                "${group.course} · ${group.session} · ${group.meta}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 5.dp, bottom = 12.dp),
            )
        }
        InfoChip(if (selected) "已勾选" else "未勾选", if (selected) ClassrollIcons.Check else ClassrollIcons.Eye)
        Spacer(Modifier.height(12.dp))
        EvidenceRow(
            "文字量 · 主信号",
            "${photo.ocr.characterCount} 字 / ${(photo.ocr.textAreaRatio * 100).roundToInt()}%",
            if (photo.status == AnalysisStatus.OcrDone) "已识别" else "未识别",
            if (photo.ocr.characterCount > 0) PaperGood else PaperWarn,
        )
        EvidenceRow("屏幕 / 投影", "${(photo.coarse.screenScore * 100).roundToInt()}%", "辅助", if (photo.coarse.screenScore >= .45f) PaperGood else PaperWarn)
        EvidenceRow(
            "人脸面积 · 负信号",
            "${photo.coarse.faceCount} 张 / 最大 ${(photo.coarse.maxFaceAreaRatio * 100).roundToInt()}%",
            "辅助",
            if (photo.coarse.maxFaceAreaRatio >= .18f) PaperTerracotta else PaperGood,
        )
        EvidenceRow(
            "组内图像指纹",
            if (photo.coarse.outlierDistance == null) "样本不足" else "中位距离 ${photo.coarse.outlierDistance}/64",
            if (photo.coarse.nearDuplicateCount > 0) "${photo.coarse.nearDuplicateCount} 张近似" else "参考",
            if ((photo.coarse.outlierDistance ?: 0) >= 22) PaperWarn else PaperGood,
        )
        Spacer(Modifier.height(8.dp))
        Text(photo.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (photo.ocr.text.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("识别出的文字", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    SelectionContainer {
                        Text(photo.ocr.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (photo.error != null) {
            Spacer(Modifier.height(10.dp))
            Text(photo.error, style = MaterialTheme.typography.bodySmall, color = PaperTerracotta)
            Spacer(Modifier.height(8.dp))
            SecondaryButton("重新识别这张", onRetry, Modifier.fillMaxWidth(), icon = ClassrollIcons.Refresh)
        } else if (photo.status == AnalysisStatus.OcrSkipped) {
            Spacer(Modifier.height(10.dp))
            SecondaryButton("仍然识别这张的文字", onRetry, Modifier.fillMaxWidth(), icon = ClassrollIcons.Scan)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("设为不选", onReject, Modifier.weight(1f), icon = ClassrollIcons.Trash)
            PrimaryButton("保留这张", onKeep, Modifier.weight(1f), icon = ClassrollIcons.Check)
        }
        Text(
            "只改变勾选，不删除原图",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
    }
}

@Composable
private fun EvidenceRow(label: String, value: String, weight: String, tint: Color) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(10.dp))
            Text(weight, style = MaterialTheme.typography.labelMedium, color = tint, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DialogFrame(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = .32f))
                .clickable(onClick = onDismiss)
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(.94f)
                    .fillMaxHeight(.90f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.background,
                border = LocalSkin.current.cardBorder,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun DialogHeader(title: String, onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        AppIconButton(icon = ClassrollIcons.Close, contentDescription = "关闭", onClick = onClose)
    }
}
