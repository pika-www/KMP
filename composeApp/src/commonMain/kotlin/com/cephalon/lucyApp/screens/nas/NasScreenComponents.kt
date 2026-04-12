package com.cephalon.lucyApp.screens.nas

import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.img_demo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cephalon.lucyApp.components.LocalDesignScale
import org.jetbrains.compose.resources.painterResource

internal val NasButtonBackgroundColor = Color(0xFFF1F1F3)
internal val NasButtonBorderColor = Color(0xFFD2D2D7)
private val NasGlassButtonBorder = Color(0x28FFFFFF)
private val NasGlassButtonBg = Color(0x2AFFFFFF)
private val NasGlassSelectedBorder = Color(0xFFFFFFFF)
private val NasPressedTextColor = Color(0xFF1F2535)
private val NasGlassPressedOverlay = Brush.verticalGradient(
    colors = listOf(Color(0x2EFFFFFF), Color(0x2EFFFFFF))
)
private val NasGlassPressedGlow = Brush.radialGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0x99FFFFFF)),
    center = androidx.compose.ui.geometry.Offset(0.5f, 0f),
    radius = 1200f
)

internal enum class NasCategory(val title: String) {
    Photos("照片"),
    Recordings("音频"),
    Documents("文档")
}

@Composable
internal fun NasTopCategoryRow(
    selected: NasCategory,
    onSelect: (NasCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NasCategory.values().forEach { category ->
            NasTopTabButton(
                title = category.title,
                selected = selected == category,
                onClick = { onSelect(category) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun NasBottomQuickActions(
    onSelectionClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NasGlassTextButton(text = "选择", onClick = onSelectionClick)
        NasGlassCircleButton(
            imageVector = Icons.Outlined.Add,
            contentDescription = "新增",
            onClick = onAddClick,
            modifier = Modifier.size(44.dp)
        )
    }
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
                NasTopTabButton(
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 图片预览盒子: 236×315, aspect-ratio 233/311, border-radius 2
            Image(
                painter = painterResource(Res.drawable.img_demo),
                contentDescription = image.name,
                modifier = Modifier
                    .size(width = 236.dp, height = 315.dp)
                    .clip(RoundedCornerShape(2.dp)),
                contentScale = ContentScale.Crop
            )

            // 操作按钮盒子: 240×89, border-radius 16, 半透明玻璃背景
            Surface(
                modifier = Modifier.size(width = 240.dp, height = 89.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0x1A000000),
                border = BorderStroke(1.dp, Color(0x0FFFFFFF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 41.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NasPopupActionButton(
                        text = "发送脑花",
                        onClick = onShare,
                        modifier = Modifier.weight(1f)
                    )
                    NasPopupActionButton(
                        text = "下载",
                        onClick = onDownload,
                        modifier = Modifier.weight(1f)
                    )
                    NasPopupActionButton(
                        text = "删除",
                        onClick = onDelete,
                        textColor = Color(0xFFFF3B30),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NasPopupActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // icon 先不写
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = textColor,
            textAlign = TextAlign.Center
        )
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
            .verticalScroll(rememberScrollState())
            .padding(top = 56.dp, bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        imageMonths.forEach { monthGroup ->
            Text(
                text = monthGroup.label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                monthGroup.images.chunked(4).forEach { rowImages ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                        repeat(4 - rowImages.size) {
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
            .verticalScroll(rememberScrollState())
            .padding(top = 56.dp, bottom = 64.dp),
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
            .verticalScroll(rememberScrollState())
            .padding(top = 56.dp, bottom = 64.dp),
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
internal fun NasTopTabButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ds = LocalDesignScale.current
    val shape = RoundedCornerShape(100.dp)
    val border = if (selected) NasGlassSelectedBorder else NasGlassButtonBorder
    val backgroundBrush = if (selected) NasGlassPressedOverlay else Brush.verticalGradient(
        colors = listOf(NasGlassButtonBg, NasGlassButtonBg)
    )

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = ds.sh(32.dp))
            .shadow(
                elevation = if (selected) 12.dp else 0.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(
                backgroundBrush
            )
            .then(
                if (selected) {
                    Modifier.background(NasGlassPressedGlow)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, border)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = ds.sw(16.dp), vertical = ds.sh(9.dp)),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = ds.sp(12f),
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (selected) NasPressedTextColor else Color.White
            )
        }
    }
}

@Composable
internal fun NasGlassCircleButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = CircleShape

    Surface(
        modifier = modifier.size(40.dp),
        shape = shape,
        color = NasGlassButtonBg,
        border = BorderStroke(1.dp, NasGlassButtonBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = Color.White
            )
        }
    }
}

@Composable
internal fun NasTopBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NasGlassCircleButton(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "返回",
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
internal fun NasGlassTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ds = LocalDesignScale.current
    val shape = RoundedCornerShape(100.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val border = if (isPressed) NasGlassSelectedBorder else NasGlassButtonBorder
    val backgroundBrush = if (isPressed) NasGlassPressedOverlay else Brush.verticalGradient(
        colors = listOf(NasGlassButtonBg, NasGlassButtonBg)
    )

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = ds.sh(32.dp))
            .clip(shape)
            .background(backgroundBrush)
            .then(
                if (isPressed) {
                    Modifier.background(NasGlassPressedGlow)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, border)
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = ds.sw(16.dp), vertical = ds.sh(9.dp)),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = ds.sp(12f),
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (isPressed) NasPressedTextColor else Color.White
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
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp))
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
                    .background(Color(0x99000000))
            )
        }

        if (showSelectionIndicator && isSelected) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(18.dp),
                shape = CircleShape,
                color = Color(0xFF2192EF)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "已选择",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
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
