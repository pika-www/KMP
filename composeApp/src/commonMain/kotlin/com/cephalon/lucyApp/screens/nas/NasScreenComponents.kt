package com.cephalon.lucyApp.screens.nas

import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.img_demo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource

internal enum class NasCategory(val title: String) {
    Photos("照片"),
    Recordings("录音"),
    Documents("文档")
}

internal data class NasImageMonthGroup(
    val label: String,
    val images: List<NasImageItem>
)

internal data class NasImageItem(
    val id: String,
    val name: String,
    val type: String,
    val format: String,
    val sizeKB: Int,
    val path: String,
    val time: String,
    val location: String?,
    val resolution: String
)

internal data class NasAudioItem(
    val id: String,
    val name: String,
    val type: String,
    val format: String,
    val sizeKB: Int,
    val path: String,
    val time: String,
    val durationSec: Int
)

internal data class NasDocumentItem(
    val id: String,
    val name: String,
    val type: String,
    val format: String,
    val sizeKB: Int,
    val path: String,
    val time: String
)

@Composable
internal fun NasCategoryAndAddRow(
    selected: NasCategory,
    onSelect: (NasCategory) -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NasCategory.values().forEach { category ->
                NasCategoryChip(
                    title = category.title,
                    selected = selected == category,
                    onClick = { onSelect(category) }
                )
            }
        }

        IconButton(
            onClick = onAddClick,
            modifier = Modifier
                .size(36.dp)
                .background(Color.White, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "新增",
                tint = Color(0xFF3A3A3A)
            )
        }
    }
}

@Composable
internal fun NasSearchBar(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "搜索资源文件",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8E8E93)
            )
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "搜索",
                tint = Color(0xFF3A3A3A)
            )
        }
    }
}

@Composable
internal fun NasPhotosContent(
    imageMonths: List<NasImageMonthGroup>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        imageMonths.forEach { monthGroup ->
            Text(
                text = monthGroup.label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF222222)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                monthGroup.images.chunked(3).forEach { rowImages ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowImages.forEach { image ->
                            NasImageThumbnail(
                                image = image,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - rowImages.size) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
internal fun NasRecordingsContent(
    audios: List<NasAudioItem>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        audios.forEach { audio ->
            NasAudioCard(audio = audio)
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
internal fun NasDocumentsContent(
    documents: List<NasDocumentItem>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        documents.forEach { document ->
            NasDocumentCard(document = document)
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun NasCategoryChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) Color.Black else Color.White
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) Color.White else Color(0xFF1F1F1F)
        )
    }
}

@Composable
private fun NasImageThumbnail(
    image: NasImageItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF4F4F5))
    ) {
        Image(
            painter = painterResource(Res.drawable.img_demo),
            contentDescription = image.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun NasAudioCard(audio: NasAudioItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${audio.time} - ${formatAudioDuration(audio.durationSec)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF222222)
            )
        }
    }
}

@Composable
private fun NasDocumentCard(document: NasDocumentItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = document.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF222222)
            )
        }
    }
}

private fun formatAudioDuration(durationSec: Int): String {
    val minutes = durationSec / 60
    val seconds = durationSec % 60
    return if (minutes > 0) {
        if (seconds > 0) {
            "${minutes}分${seconds}秒"
        } else {
            "${minutes}分钟"
        }
    } else {
        "${seconds}秒"
    }
}
