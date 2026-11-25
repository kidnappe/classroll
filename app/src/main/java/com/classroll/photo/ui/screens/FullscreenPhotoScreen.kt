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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.classroll.photo.data.ArchiveItem
import com.classroll.photo.ui.ClassrollIcons

@Composable
fun FullscreenPhotoScreen(
    item: ArchiveItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (item.available) {
            AsyncImage(
                model = item.uri,
                contentDescription = item.fileName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = .35f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(ClassrollIcons.ArrowLeft, contentDescription = "返回", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            Text(item.course, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = .45f))
                .padding(16.dp),
        ) {
            Text(
                item.fileName,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(
                    item.term.ifBlank { null },
                    if (item.week > 0) "第 ${item.week} 周" else null,
                    item.sessionLabel.ifBlank { null },
                ).joinToString(" · "),
                color = Color.White.copy(alpha = .7f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
