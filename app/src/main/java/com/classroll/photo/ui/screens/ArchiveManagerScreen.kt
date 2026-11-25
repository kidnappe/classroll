package com.classroll.photo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.classroll.photo.data.ArchiveAlbum
import com.classroll.photo.ui.AppIconButton
import com.classroll.photo.ui.ClassrollCard
import com.classroll.photo.ui.ClassrollIcons
import com.classroll.photo.ui.InfoChip
import com.classroll.photo.ui.SecondaryButton
import com.classroll.photo.ui.theme.corner
import com.classroll.photo.ui.theme.LocalSkin
import com.classroll.photo.ui.theme.PaperTerracotta

@Composable
fun ArchiveManagerScreen(
    albums: List<ArchiveAlbum>,
    onBack: () -> Unit,
    onDelete: (term: String, course: String) -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<ArchiveAlbum?>(null) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIconButton(icon = ClassrollIcons.ArrowLeft, contentDescription = "返回", onClick = onBack)
                Text("归档管理", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).padding(start = 4.dp))
                InfoChip("只删副本", ClassrollIcons.Shield)
            }
        }
        if (albums.isEmpty()) {
            item {
                ClassrollCard {
                    Text("还没有归档记录", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            items(albums.size) { index ->
                val album = albums[index]
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LocalSkin.current.corner(12.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    border = LocalSkin.current.cardBorder,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(album.course, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${album.term} · ${album.count} 张",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { pendingDelete = album }) {
                            Icon(ClassrollIcons.Trash, contentDescription = null, tint = PaperTerracotta, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("移除", style = MaterialTheme.typography.labelMedium, color = PaperTerracotta)
                        }
                    }
                }
            }
        }
        item {
            SecondaryButton(
                text = "导出索引 JSON",
                onClick = onExport,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                icon = ClassrollIcons.File,
            )
        }
    }

    val target = pendingDelete
    if (target != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("从归档移除「${target.course}」？") },
            text = {
                Text("只移除 ${target.term} 学期下的 $target.count 条索引记录，原图不受影响；下次扫描会重新出现。")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.term, target.course)
                    pendingDelete = null
                }) {
                    Text("移除", color = PaperTerracotta)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }
}
