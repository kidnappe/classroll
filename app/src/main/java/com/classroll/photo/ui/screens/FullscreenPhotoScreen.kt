package com.classroll.photo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.classroll.photo.data.ArchiveItem
import com.classroll.photo.ui.ClassrollIcons

/** 全屏看图：左右滑动切换同一组的照片，返回由外层 Dialog 负责。 */
@Composable
fun FullscreenPhotoScreen(
    photos: List<ArchiveItem>,
    startIndex: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (photos.isEmpty()) {
        Box(modifier.fillMaxSize().background(Color.Black))
        return
    }
    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, photos.lastIndex)) { photos.size }
    val current = photos[pagerState.currentPage.coerceIn(0, photos.lastIndex)]

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val item = photos[page]
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (item.available) {
                    AsyncImage(
                        model = item.uri,
                        contentDescription = item.fileName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            ClassrollIcons.Alert,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = .5f),
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("原图已失效", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
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
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    current.course,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        if (current.week > 0) append("第 ${current.week} 周 · ")
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
                current.fileName,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(
                    current.term.ifBlank { null },
                    if (current.week > 0) "第 ${current.week} 周" else null,
                    current.sessionLabel.ifBlank { null },
                ).joinToString(" · "),
                color = Color.White.copy(alpha = .7f),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
