package com.classroll.photo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.classroll.photo.model.AnalysisStatus
import com.classroll.photo.model.PhotoGroup
import com.classroll.photo.model.PhotoItem
import com.classroll.photo.ui.ClassrollIcons
import com.classroll.photo.ui.theme.LocalSkin

/** 确认页缩略图的全屏查看：左右滑动切换当前筛选下的全部候选照片。 */
@Composable
fun CandidateFullscreenScreen(
    photos: List<PhotoItem>,
    groups: Map<String, PhotoGroup>,
    startIndex: Int,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    onRetry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (photos.isEmpty()) {
        Box(modifier.fillMaxSize().background(Color.Black))
        return
    }
    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, photos.lastIndex)) { photos.size }
    val current = photos[pagerState.currentPage.coerceIn(0, photos.lastIndex)]
    val group = groups[current.groupId]
    val skin = LocalSkin.current
    val checked = current.id in selectedIds
    val checkShape = if (skin.hardShadow != null) RoundedCornerShape(0.dp) else CircleShape

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model = photos[page].imageUri,
                contentDescription = photos[page].title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = .35f))
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(ClassrollIcons.ArrowLeft, contentDescription = "返回", tint = Color.White)
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(checkShape)
                    .background(if (checked) Color.White else Color.Black.copy(alpha = .35f))
                    .border(1.5.dp, Color.White, checkShape)
                    .clickable { onToggle(current.id) }
                    .semantics {
                        contentDescription = if (checked) "取消选择" else "选择这张照片"
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (checked) {
                    Icon(ClassrollIcons.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(19.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    group?.course ?: current.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        group?.week?.takeIf { it > 0 }?.let { append("第 $it 周 · ") }
                        append("${pagerState.currentPage + 1} / ${photos.size}")
                    },
                    color = Color.White.copy(alpha = .7f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = .45f))
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Text(
                current.title,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(
                    current.timeLabel.ifBlank { null },
                    current.verdict.label,
                    group?.session?.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                color = Color.White.copy(alpha = .75f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                current.evidenceSummary,
                color = Color.White.copy(alpha = .55f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (current.status == AnalysisStatus.Failed || current.status == AnalysisStatus.OcrSkipped) {
                TextButton(
                    onClick = { onRetry(current.id) },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                ) {
                    Text("重新识别", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }
    }
}
