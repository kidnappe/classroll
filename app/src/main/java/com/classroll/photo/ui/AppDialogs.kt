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
