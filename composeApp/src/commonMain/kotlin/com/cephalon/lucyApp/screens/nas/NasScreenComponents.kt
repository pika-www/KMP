package com.cephalon.lucyApp.screens.nas

import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.img_demo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.painterResource

internal val NasButtonBackgroundColor = Color(0xFFF1F1F3)
internal val NasButtonBorderColor = Color(0xFFD2D2D7)

internal enum class NasCategory(val title: String) {
    Photos("照片"),
    Recordings("音频"),
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
    onSelectionClick: () -> Unit,
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

        NasActionTextButton(text = "选择", onClick = onSelectionClick)

        Spacer(modifier = Modifier.width(8.dp))

        NasCircularIconButton(
            imageVector = Icons.Outlined.Add,
            contentDescription = "新增",
            onClick = onAddClick,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
internal fun NasSearchBar(
    onClick: () -> Unit
) {
    val searchBarShape = RoundedCornerShape(999.dp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = searchBarShape,
        color = NasButtonBackgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(searchBarShape)
                .clickable(onClick = onClick)
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
internal fun NasImageActionPopup(
    image: NasImageItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.8f),
            shape = RoundedCornerShape(18.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(Res.drawable.img_demo),
                    contentDescription = image.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.25f)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NasActionTextButton(
                        text = "发送朋友",
                        onClick = onShare
                    )
                    NasActionTextButton(
                        text = "下载",
                        onClick = onDownload
                    )
                    NasActionTextButton(
                        text = "删除",
                        onClick = onDelete,
                        textColor = Color(0xFFFF3B30)
                    )
                }
            }
        }
    }
}

@Composable
internal fun NasPhotoSelectionBottomBar(
    onShareClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NasActionTextButton(text = "发送朋友", onClick = onShareClick)
            NasActionTextButton(text = "下载", onClick = onDownloadClick)
            NasActionTextButton(text = "删除", onClick = onDeleteClick, textColor = Color(0xFFFF3B30))
        }

        NasCircularIconButton(
            imageVector = Icons.Outlined.Search,
            contentDescription = "搜索",
            onClick = onSearchClick,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
internal fun NasPhotoSelectionRow(
    selectedCount: Int,
    onSelectAllClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            NasActionTextButton(text = "全选", onClick = onSelectAllClick)
        }

        Text(
            text = "已选择${selectedCount}项",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF111111),
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            NasActionTextButton(text = "取消", onClick = onCancelClick)
        }
    }
}

@Composable
internal fun NasPhotosContent(
    imageMonths: List<NasImageMonthGroup>,
    selectionMode: Boolean = false,
    selectedImageIds: Collection<String> = emptyList(),
    onImageClick: (NasImageItem) -> Unit = {},
    onImageLongClick: (NasImageItem) -> Unit = {},
    onImageSelectionToggle: (NasImageItem) -> Unit = {}
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
                                modifier = Modifier.weight(1f),
                                showSelectionIndicator = selectionMode,
                                isSelected = selectedImageIds.contains(image.id),
                                onClick = {
                                    if (selectionMode) {
                                        onImageSelectionToggle(image)
                                    } else {
                                        onImageClick(image)
                                    }
                                },
                                onLongClick = {
                                    if (!selectionMode) {
                                        onImageLongClick(image)
                                    }
                                }
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
    audios: List<NasAudioItem>,
    selectionMode: Boolean = false,
    selectedAudioIds: Collection<String> = emptyList(),
    onAudioClick: (NasAudioItem) -> Unit = {},
    onAudioSelectionToggle: (NasAudioItem) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        audios.forEach { audio ->
            NasAudioCard(
                audio = audio,
                showSelectionIndicator = selectionMode,
                isSelected = selectedAudioIds.contains(audio.id),
                onClick = {
                    if (selectionMode) {
                        onAudioSelectionToggle(audio)
                    } else {
                        onAudioClick(audio)
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
internal fun NasDocumentsContent(
    documents: List<NasDocumentItem>,
    selectionMode: Boolean = false,
    selectedDocumentIds: Collection<String> = emptyList(),
    onDocumentClick: (NasDocumentItem) -> Unit = {},
    onDocumentSelectionToggle: (NasDocumentItem) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        documents.forEach { document ->
            NasDocumentCard(
                document = document,
                showSelectionIndicator = selectionMode,
                isSelected = selectedDocumentIds.contains(document.id),
                onClick = {
                    if (selectionMode) {
                        onDocumentSelectionToggle(document)
                    } else {
                        onDocumentClick(document)
                    }
                }
            )
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
    val chipShape = RoundedCornerShape(999.dp)

    Surface(
        shape = chipShape,
        color = if (selected) Color.Black else NasButtonBackgroundColor,
        border = null
    ) {
        Box(
            modifier = Modifier
                .clip(chipShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) Color.White else Color(0xFF1F1F1F)
            )
        }
    }
}

@Composable
internal fun NasActionTextButton(
    text: String,
    onClick: () -> Unit,
    textColor: Color = Color(0xFF1F1F1F)
) {
    val buttonShape = RoundedCornerShape(999.dp)

    Surface(
        shape = buttonShape,
        color = NasButtonBackgroundColor,
        border = null
    ) {
        Box(
            modifier = Modifier
                .clip(buttonShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = textColor
            )
        }
    }
}

@Composable
internal fun NasCircularIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = Color(0xFF3A3A3A)
) {
    val buttonShape = CircleShape

    Surface(
        modifier = modifier,
        shape = buttonShape,
        color = NasButtonBackgroundColor,
        border = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(buttonShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = iconTint
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun NasImageThumbnail(
    image: NasImageItem,
    modifier: Modifier = Modifier,
    showSelectionIndicator: Boolean = false,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val cardShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(cardShape)
            .background(Color(0xFFF4F4F5))
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Image(
            painter = painterResource(Res.drawable.img_demo),
            contentDescription = image.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (showSelectionIndicator && isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x26000000))
            )
        }

        if (showSelectionIndicator) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(20.dp),
                shape = CircleShape,
                color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.96f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "已选择",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun NasAudioCard(
    audio: NasAudioItem,
    showSelectionIndicator: Boolean = false,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val cardShape = RoundedCornerShape(12.dp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        color = NasButtonBackgroundColor,
        border = BorderStroke(1.dp, NasButtonBorderColor)
    ) {
        Row(
            modifier = Modifier
                .clip(cardShape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${audio.time} - ${formatAudioDuration(audio.durationSec)}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF222222)
            )

            if (showSelectionIndicator) {
                Surface(
                    modifier = Modifier.size(20.dp),
                    shape = CircleShape,
                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.96f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "已选择",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NasDocumentCard(
    document: NasDocumentItem,
    showSelectionIndicator: Boolean = false,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val cardShape = RoundedCornerShape(12.dp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        color = NasButtonBackgroundColor,
        border = BorderStroke(1.dp, NasButtonBorderColor)
    ) {
        Row(
            modifier = Modifier
                .clip(cardShape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = document.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF222222)
            )

            if (showSelectionIndicator) {
                Surface(
                    modifier = Modifier.size(20.dp),
                    shape = CircleShape,
                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.96f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "已选择",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
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
