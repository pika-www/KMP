package com.cephalon.lucyApp.screens.agentmodel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.media.PlatformImageThumbnail

@Composable
internal fun AgentModelAttachmentPanel(
    recentImages: List<String>,
    onOpenCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenFilePicker: () -> Unit,
    onImageClick: (ImagePreviewState) -> Unit,
    onRecentImageSelect: (String) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(color = Color(0xFFEDEDED))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AttachmentTile(modifier = Modifier.weight(1f), title = "相机", onClick = onOpenCamera)
        AttachmentTile(modifier = Modifier.weight(1f), title = "相册", onClick = onOpenGallery)
        AttachmentTile(modifier = Modifier.weight(1f), title = "文件", onClick = onOpenFilePicker)
    }

    val images = recentImages.take(12)
    if (images.isNotEmpty()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(images) { uri ->
                Card(
                    modifier = Modifier.clickable {
                        onRecentImageSelect(uri)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                ) {
                    PlatformImageThumbnail(
                        uri = uri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                    )
                }
            }
        }
    } else {
        Text(
            text = "授权后，这里会展示系统相册的近期照片。",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8A8A8A),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onClearLogs,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("清空记录")
        }
    }
}

@Composable
private fun AttachmentTile(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .height(84.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        border = BorderStroke(1.dp, Color(0xFFE7E7E7))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = Color(0xFF111111))
        }
    }
}
