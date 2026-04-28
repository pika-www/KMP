package com.cephalon.lucyApp.screens.nas

import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.ic_audio
import androidios.composeapp.generated.resources.ic_delete
import androidios.composeapp.generated.resources.ic_doc
import androidios.composeapp.generated.resources.ic_download
import androidios.composeapp.generated.resources.ic_image
import androidios.composeapp.generated.resources.ic_share
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.foundation.layout.IntrinsicSize
import com.cephalon.lucyApp.components.BlobImage
import com.cephalon.lucyApp.components.LocalDesignScale
import org.jetbrains.compose.resources.DrawableResource
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
private val NasTabSelectedBackgroundColor = Color(0xFF000000)
private val NasTabUnselectedBackgroundColor = Color(0xFFFFFFFF)
private val NasTabSelectedContentColor = Color(0xFFFFFFFF)
private val NasTabUnselectedContentColor = Color(0xE6000000)
private val NasBottomActionBackdropBrush = Brush.verticalGradient(
    colorStops = arrayOf(
        0.006f to Color(0x00000000),
        0.56f to Color(0x12000000),
        1f to Color(0x33000000)
    )
)
private val NasMonthSectionBackgroundColor = Color(0x0D000000)
private val NasMonthSectionSurfaceColor = Color(0x0D000000)
private val NasMonthSectionShadowColor = Color(0x0D000000)
private val NasMonthItemBackgroundColor = Color(0xFFFFFFFF)

internal enum class NasCategory(val title: String, val icon: DrawableResource) {
    Photos("图片", Res.drawable.ic_image),

    Recordings("音频", Res.drawable.ic_audio),
    Documents("文档", Res.drawable.ic_doc)
}

internal enum class NasUploadTaskType {
    Image,
    Audio,
    Document
}

internal enum class NasTaskDirection {
    Upload,
    Download,
    Delete
}

internal enum class NasUploadTaskStatus {
    Waiting,
    Uploading,
    Downloading,
    Deleting,
    Registering,
    Saving,
    Completed,
    Failed
}

internal data class NasUploadTaskItem(
    val id: String,
    val title: String,
    val type: NasUploadTaskType,
    val progress: Float,
    val status: NasUploadTaskStatus,
    val direction: NasTaskDirection = NasTaskDirection.Upload,
    val batchId: String? = null
)

internal data class NasUploadBatchSummary(
    val id: String,
    val totalCount: Int,
    val completedCount: Int = 0
)

internal data class NasUploadProgressSummary(
    val totalCount: Int,
    val completedCount: Int
) {
    val progressFraction: Float
        get() = if (totalCount <= 0) 0f else completedCount.toFloat() / totalCount.toFloat()

    val progressPercentText: String
        get() = "${(progressFraction * 100).toInt().coerceIn(0, 100)}%"

    val progressDetailText: String
        get() = "$completedCount/$totalCount"
}

@Composable
internal fun NasTopCategoryRow(
    selected: NasCategory,
    onSelect: (NasCategory) -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NasCategory.values().forEach { category ->
                NasTopTabButton(
                    title = category.title,
                    icon = category.icon,
                    selected = selected == category,
                    onClick = { onSelect(category) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        trailingContent()
    }
}

@Composable
internal fun NasUploadProgressEntry(
    summary: NasUploadProgressSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ds = LocalDesignScale.current
    val shape = CircleShape
    Surface(
        modifier = modifier.size(ds.sm(36.dp)),
        shape = shape,
        color = Color(0xFFF5F5F5)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 2.dp.toPx()
                val inset = strokeWidth / 2f + 1.dp.toPx()
                drawArc(
                    color = Color(0x26000000),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidth
                    ),
                    topLeft = Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(
                        width = size.width - inset * 2,
                        height = size.height - inset * 2
                    )
                )
                drawArc(
                    color = Color(0xFF000000),
                    startAngle = -90f,
                    sweepAngle = 360f * summary.progressFraction.coerceIn(0f, 1f),
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidth
                    ),
                    topLeft = Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(
                        width = size.width - inset * 2,
                        height = size.height - inset * 2
                    )
                )
            }
            Text(
                text = summary.progressPercentText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = ds.sp(9f),
                    fontWeight = FontWeight.W500
                ),
                color = Color(0xE6000000)
            )
        }
    }
}

@Composable
internal fun NasUploadBanner(
    activeUploadCount: Int,
    activeDownloadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ds = LocalDesignScale.current
    val shape = RoundedCornerShape(999.dp)
    val bannerText = buildString {
        append("目前有")
        val parts = mutableListOf<String>()
        if (activeUploadCount > 0) parts += "上传${activeUploadCount}个"
        if (activeDownloadCount > 0) parts += "下载${activeDownloadCount}个"
        append(parts.joinToString("/"))
        append("任务进行中 ")
    }
    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = Color(0x1FFFFFFF),
        border = BorderStroke(1.dp, Color(0x33FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ds.sm(14.dp), vertical = ds.sm(8.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = bannerText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = ds.sp(12f),
                    fontWeight = FontWeight.Normal
                ),
                color = Color.White.copy(alpha = 0.78f)
            )
            Text(
                text = "点击此处查看",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = ds.sp(12f),
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White
            )
        }
    }
}

@Composable
internal fun NasUploadProgressDialog(
    tasks: List<NasUploadTaskItem>,
    uploadSummary: NasUploadProgressSummary?,
    onDismiss: () -> Unit
) {
    val ds = LocalDesignScale.current
    val visibleTasks = tasks.filter { it.status != NasUploadTaskStatus.Completed }
    val taskListScrollState = rememberScrollState()
    val taskListMaxHeight = ds.sm(28.dp * 6 + 12.dp * 5)
    Surface(
        modifier = Modifier
            .width(ds.sw(260.dp))
            .offset(x = (-20).dp),
        shape = RoundedCornerShape(ds.sm(24.dp)),
        color = Color.White,
        shadowElevation = ds.sm(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ds.sm(16.dp), vertical = ds.sm(20.dp)),
            verticalArrangement = Arrangement.spacedBy(ds.sm(12.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ds.sm(6.dp))
            ) {
                Text(
                    text = "任务进度",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = ds.sp(14f),
                        fontWeight = FontWeight.W500
                    ),
                    color = Color(0xE6000000)
                )
                Text(
                    text = "以下是所有的任务进度",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = ds.sp(10f),
                        fontWeight = FontWeight.W400
                    ),
                    color = Color(0x99000000)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = taskListMaxHeight)
                    .verticalScroll(taskListScrollState),
                verticalArrangement = Arrangement.spacedBy(ds.sm(12.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(ds.sm(36.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .size(ds.sm(32.dp))
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.06f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = com.cephalon.lucyApp.screens.agentmodel.BackIcon,
                                contentDescription = "关闭上传进度",
                                tint = Color.Black.copy(alpha = 0.60f),
                                modifier = Modifier.size(
                                    width = ds.sw(11.dp),
                                    height = ds.sh(17.dp),
                                ),
                            )
                        }
                    }
                    Text(
                        text = "任务进度",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = ds.sp(20f),
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(ds.sm(36.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .size(ds.sm(32.dp))
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.06f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "完成上传进度",
                                tint = Color.White,
                                modifier = Modifier.size(ds.sm(18.dp))
                            )
                        }
                    }
                }
                if (visibleTasks.isEmpty() && uploadSummary != null && uploadSummary.totalCount > 0) {
                    Text(
                        text = "本次上传已完成",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = ds.sp(10f),
                            fontWeight = FontWeight.W400,
                            textAlign = TextAlign.Center
                        ),
                        color = Color(0x99000000)
                    )
                }
            }
        }
    }
}

@Composable
internal fun NasBottomQuickActions(
    onSelectionClick: () -> Unit,
    onAddClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ds = LocalDesignScale.current
    val selectionActionTextStyle = TextStyle(
        fontSize = ds.sp(18f),
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.W600,
        lineHeight = TextUnit(0f, TextUnitType.Unspecified)
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NasBottomQuickActionTextButton(
            text = "选择",
            onClick = onSelectionClick,
            modifier = Modifier
                .width(ds.sw(140.dp))
                .height(ds.sh(40.dp))
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NasBottomQuickActionIconButton(
                imageVector = Icons.Outlined.Add,
                contentDescription = "新增",
                onClick = onAddClick,
                modifier = Modifier.size(40.dp)
            )
            NasBottomQuickActionIconButton(
                imageVector = Icons.Outlined.Search,
                contentDescription = "搜索",
                onClick = onSearchClick,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
internal fun NasBottomQuickActionsBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val ds = LocalDesignScale.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ds.sh(102.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(ds.sm(3.dp))
                .background(NasBottomActionBackdropBrush)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = ds.sh(18.dp), bottom = ds.sh(44.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            content()
        }
    }
}

@Composable
internal fun NasBottomQuickActionIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = CircleShape

    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.White
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
                tint = Color(0xE6000000)
            )
        }
    }
}

@Composable
internal fun NasBottomQuickActionTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ds = LocalDesignScale.current
    val shape = RoundedCornerShape(100.dp)

    Surface(
        modifier = modifier,
        shape = shape,
        color = NasTabUnselectedBackgroundColor,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = ds.sp(18f),
                    fontStyle = FontStyle.Normal,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = TextUnit(0f, TextUnitType.Unspecified)
                ),
                color = Color(0xE6000000),
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
internal fun NasLightPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: DrawableResource? = null,
    textStyle: TextStyle? = null,
    contentPadding: PaddingValues? = null
) {
    val ds = LocalDesignScale.current
    val shape = RoundedCornerShape(100.dp)

    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = ds.sm(40.dp))
                .clip(shape)
                .clickable(onClick = onClick)
                .padding(contentPadding ?: PaddingValues(horizontal = ds.sm(16.dp), vertical = ds.sm(8.dp))),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(ds.sm(18.dp)),
                    tint = Color(0xE6000000)
                )
                Spacer(modifier = Modifier.width(ds.sm(6.dp)))
            }
            Text(
                text = text,
                style = textStyle ?: MaterialTheme.typography.labelLarge.copy(
                    fontSize = ds.sp(16f),
                    fontStyle = FontStyle.Normal,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = TextUnit(0f, TextUnitType.Unspecified)
                ),
                color = Color(0xE6000000),
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

internal data class NasImageMonthGroup(
    val label: String,
    val images: List<NasImageItem>
)

internal data class NasImageItem(
    val id: String,
    val fileId: Long? = null,
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
    val fileId: Long? = null,
    val name: String,
    val type: String,
    val format: String,
    val sizeKB: Int,
    val path: String,
    val time: String,
    val durationSec: Int
)

internal data class NasAudioMonthGroup(
    val label: String,
    val audios: List<NasAudioItem>
)

internal data class NasDocumentItem(
    val id: String,
    val fileId: Long? = null,
    val name: String,
    val type: String,
    val format: String,
    val sizeKB: Int,
    val path: String,
    val time: String
)

internal data class NasDocumentMonthGroup(
    val label: String,
    val documents: List<NasDocumentItem>
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
                    icon = category.icon,
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
            if (image.path.isNotBlank()) {
                BlobImage(
                    blobRef = image.path,
                    contentDescription = image.name,
                    modifier = Modifier
                        .size(width = 236.dp, height = 315.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    contentScale = ContentScale.Crop,
                    errorContent = {
                        Box(
                            modifier = Modifier
                                .size(width = 236.dp, height = 315.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF1A1A1A))
                        )
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 236.dp, height = 315.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF1A1A1A))
                )
            }

            // 操作按钮盒子改为截图同款的竖排白底菜单
            Surface(
                modifier = Modifier.widthIn(min = 132.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 18.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 26.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    NasPopupActionButton(
                        text = "发送脑花",
                        onClick = onShare
                    )
                    NasPopupActionButton(
                        text = "下载",
                        onClick = onDownload
                    )
                    NasPopupActionButton(
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
private fun NasPopupActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color(0xFF111111)
) {
    Box(
        modifier = modifier.clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp
            ),
            color = textColor,
            textAlign = TextAlign.Start
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
    bottomPadding: Dp = 64.dp,
    scrollState: ScrollState = rememberScrollState(),
    showMonthHeaders: Boolean = true,
    selectionMode: Boolean = false,
    selectedImageIds: Collection<String> = emptyList(),
    onImageClick: (NasImageItem) -> Unit = {},
    onImageLongClick: (NasImageItem) -> Unit = {},
    onImageSelectionToggle: (NasImageItem) -> Unit = {},
    emptyText: String? = null,
    footer: (@Composable () -> Unit)? = null
) {
    val ds = LocalDesignScale.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(top = 56.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(ds.sm(16.dp))
    ) {
        if (imageMonths.isEmpty() && !emptyText.isNullOrBlank()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = ds.sp(14f)),
                color = Color.White.copy(alpha = 0.72f)
            )
        } else {
            Spacer(modifier = Modifier.height(ds.sm(16.dp)))
            imageMonths.forEach { monthGroup ->
                if (showMonthHeaders) {
                    Text(
                        text = monthGroup.label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = ds.sp(16f),
                            fontStyle = FontStyle.Normal,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = TextUnit(0f, TextUnitType.Unspecified)
                        ),
                        color = Color(0xE6000000)
                    )
                }
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
        }
        footer?.invoke()
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
internal fun NasRecordingsContent(
    audioMonths: List<NasAudioMonthGroup>,
    bottomPadding: Dp = 64.dp,
    scrollState: ScrollState = rememberScrollState(),
    selectionMode: Boolean = false,
    selectedAudioIds: Collection<String> = emptyList(),
    onAudioClick: (NasAudioItem) -> Unit = {},
    onAudioSelectionToggle: (NasAudioItem) -> Unit = {},
    emptyText: String? = null,
    footer: (@Composable () -> Unit)? = null
) {
    val ds = LocalDesignScale.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(top = ds.sm(56.dp), bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(ds.sm(16.dp))
    ) {
        if (audioMonths.isEmpty() && !emptyText.isNullOrBlank()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = ds.sp(14f)),
                color = Color.White.copy(alpha = 0.72f)
            )
        } else {
            Spacer(modifier = Modifier.height(ds.sm(16.dp)))
            audioMonths.forEach { monthGroup ->
                NasMonthCapsuleSection(
                    label = monthGroup.label,
                    items = monthGroup.audios,
                    itemKey = { it.id }
                ) { audio ->
                    NasMediaCapsuleRow(
                        title = audio.name,
                        icon = Res.drawable.ic_audio,
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
            }
        }
        footer?.invoke()
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
internal fun NasDocumentsContent(
    documentMonths: List<NasDocumentMonthGroup>,
    bottomPadding: Dp = 64.dp,
    scrollState: ScrollState = rememberScrollState(),
    selectionMode: Boolean = false,
    selectedDocumentIds: Collection<String> = emptyList(),
    onDocumentClick: (NasDocumentItem) -> Unit = {},
    onDocumentSelectionToggle: (NasDocumentItem) -> Unit = {},
    emptyText: String? = null,
    footer: (@Composable () -> Unit)? = null
) {
    val ds = LocalDesignScale.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(top = ds.sm(56.dp), bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(ds.sm(16.dp))
    ) {
        if (documentMonths.isEmpty() && !emptyText.isNullOrBlank()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = ds.sp(14f)),
                color = Color.White.copy(alpha = 0.72f)
            )
        } else {
            Spacer(modifier = Modifier.height(ds.sm(16.dp)))
            documentMonths.forEach { monthGroup ->
                NasMonthCapsuleSection(
                    label = monthGroup.label,
                    items = monthGroup.documents,
                    itemKey = { it.id }
                ) { document ->
                    NasMediaCapsuleRow(
                        title = document.name,
                        icon = Res.drawable.ic_doc,
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
            }
        }
        footer?.invoke()
        Spacer(modifier = Modifier.height(ds.sm(6.dp)))
    }
}

@Composable
internal fun NasTopTabButton(
    title: String,
    icon: DrawableResource? = null,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ds = LocalDesignScale.current
    val shape = RoundedCornerShape(100.dp)
    val backgroundColor = if (selected) NasTabSelectedBackgroundColor else NasTabUnselectedBackgroundColor
    val contentColor = if (selected) NasTabSelectedContentColor else NasTabUnselectedContentColor

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(ds.sm(40.dp))
            .shadow(
                elevation = if (selected) 15.dp else 0.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = backgroundColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ds.sm(16.dp), vertical = ds.sm(9.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(ds.sm(20.dp)),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(ds.sm(6.dp)))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = ds.sp(16f),
                    fontWeight = FontWeight.SemiBold
                ),
                color = contentColor
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
        imageVector = com.cephalon.lucyApp.screens.agentmodel.BackIcon,
        contentDescription = "返回",
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
internal fun NasGlassTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: DrawableResource? = null,
    selected: Boolean = false,
    textStyle: TextStyle? = null
) {
    val ds = LocalDesignScale.current
    val shape = RoundedCornerShape(100.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val highlighted = isPressed || selected
    val border = if (highlighted) NasGlassSelectedBorder else NasGlassButtonBorder
    val backgroundBrush = if (highlighted) NasGlassPressedOverlay else Brush.verticalGradient(
        colors = listOf(NasGlassButtonBg, NasGlassButtonBg)
    )

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = ds.sh(32.dp))
            .clip(shape)
            .background(backgroundBrush)
            .then(
                if (highlighted) {
                    Modifier.background(NasGlassPressedGlow)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier
                .clip(shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = ds.sm(16.dp), vertical = ds.sm(9.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(ds.sm(14.dp)),
                    tint = if (highlighted) NasPressedTextColor else Color.White
                )
                Spacer(modifier = Modifier.width(ds.sm(6.dp)))
            }
            Text(
                text = text,
                style = textStyle ?: MaterialTheme.typography.labelLarge.copy(
                    fontSize = ds.sp(12f),
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (highlighted) NasPressedTextColor else Color.White
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
        if (image.path.isNotBlank()) {
            BlobImage(
                blobRef = image.path,
                contentDescription = image.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                errorContent = {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))
                    )
                }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))
            )
        }

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
    NasMediaCapsuleRow(
        title = audio.name,
        icon = Res.drawable.ic_audio,
        showSelectionIndicator = showSelectionIndicator,
        isSelected = isSelected,
        onClick = onClick
    )
}

@Composable
internal fun NasDocumentCard(
    document: NasDocumentItem,
    showSelectionIndicator: Boolean = false,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    NasMediaCapsuleRow(
        title = document.name,
        icon = Res.drawable.ic_doc,
        showSelectionIndicator = showSelectionIndicator,
        isSelected = isSelected,
        onClick = onClick
    )
}

@Composable
private fun <T> NasMonthCapsuleSection(
    label: String,
    items: List<T>,
    itemKey: (T) -> String,
    itemContent: @Composable (T) -> Unit
) {
    val ds = LocalDesignScale.current
    Column(
        verticalArrangement = Arrangement.spacedBy(ds.sm(8.dp))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = ds.sp(16f),
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.SemiBold,
                lineHeight = TextUnit(0f, TextUnitType.Unspecified)
            ),
            color = Color(0xE6000000)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = ds.sm(30.dp),
                    shape = RoundedCornerShape(ds.sm(16.dp)),
                    ambientColor = NasMonthSectionShadowColor,
                    spotColor = NasMonthSectionShadowColor,
                    clip = false
                )
                .clip(RoundedCornerShape(ds.sm(16.dp)))
                .background(NasMonthSectionSurfaceColor)
                .padding(horizontal = ds.sm(16.dp), vertical = ds.sm(16.dp)),
            verticalArrangement = Arrangement.spacedBy(ds.sm(12.dp))
        ) {
            items.forEach { item ->
                key(itemKey(item)) {
                    itemContent(item)
                }
            }
        }
    }
}

@Composable
private fun NasMediaCapsuleRow(
    title: String,
    icon: DrawableResource,
    showSelectionIndicator: Boolean = false,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val ds = LocalDesignScale.current
    val pillShape = RoundedCornerShape(999.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(ds.sm(40.dp)),
        shape = pillShape,
        color = NasMonthItemBackgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(pillShape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = ds.sm(16.dp), vertical = ds.sm(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(ds.sm(20.dp)),
                tint = Color(0xE6000000)
            )
            Spacer(modifier = Modifier.width(ds.sm(8.dp)))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = ds.sp(14f),
                    fontWeight = FontWeight.Normal,
                    lineHeight = ds.sp(20f),
                    letterSpacing = ds.sp(0.56f)
                ),
                color = Color(0xE6000000),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showSelectionIndicator) {
                Spacer(modifier = Modifier.width(ds.sm(8.dp)))
                Surface(
                    modifier = Modifier.size(ds.sm(16.dp)),
                    shape = CircleShape,
                    color = if (isSelected) Color(0xFF2192EF) else Color.Transparent,
                    border = if (isSelected) null else BorderStroke(1.dp, Color(0x26000000))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "已选择",
                                tint = Color.White,
                                modifier = Modifier.size(ds.sm(10.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NasUploadTaskCard(
    task: NasUploadTaskItem,
    modifier: Modifier = Modifier
) {
    val ds = LocalDesignScale.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(percent = 50),
        color = Color(0xFFF5F5F5)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ds.sh(28.dp))
                .padding(horizontal = ds.sw(16.dp), vertical = ds.sh(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(task.iconRes()),
                contentDescription = null,
                modifier = Modifier.size(ds.sm(18.dp)),
                tint = Color(0xFF6F6F73)
            )
            Spacer(modifier = Modifier.width(ds.sm(8.dp)))
            Text(
                text = task.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = ds.sp(10f),
                    fontWeight = FontWeight.W400
                ),
                color = Color(0x99000000),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(ds.sm(8.dp)))
            Text(
                text = task.statusText(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = ds.sp(10f),
                    fontWeight = FontWeight.W400
                ),
                color = Color(0x99000000),
                maxLines = 1
            )
        }
    }
}

private fun NasUploadTaskItem.iconRes(): DrawableResource =
    when (type) {
        NasUploadTaskType.Image -> Res.drawable.ic_image
        NasUploadTaskType.Audio -> Res.drawable.ic_audio
        NasUploadTaskType.Document -> Res.drawable.ic_doc
    }

private fun NasUploadTaskItem.progressForDisplay(): Float =
    when (status) {
        NasUploadTaskStatus.Waiting -> 0f
        NasUploadTaskStatus.Uploading -> progress.coerceIn(0f, 1f)
        NasUploadTaskStatus.Downloading -> progress.coerceIn(0f, 1f)
        NasUploadTaskStatus.Deleting -> progress.coerceIn(0f, 1f)
        NasUploadTaskStatus.Registering -> progress.coerceIn(0f, 1f)
        NasUploadTaskStatus.Saving -> progress.coerceIn(0f, 1f)
        NasUploadTaskStatus.Completed -> 1f
        NasUploadTaskStatus.Failed -> progress.coerceIn(0f, 1f)
    }

private fun NasUploadTaskItem.statusText(): String {
    val isDownload = direction == NasTaskDirection.Download
    val isDelete = direction == NasTaskDirection.Delete
    return when (status) {
        NasUploadTaskStatus.Waiting -> when {
            isDelete -> "等待删除"
            isDownload -> "等待下载"
            else -> "等待上传"
        }
        NasUploadTaskStatus.Uploading -> "${(progress.coerceIn(0f, 1f) * 100).toInt()}%"
        NasUploadTaskStatus.Downloading -> "${(progress.coerceIn(0f, 1f) * 100).toInt()}%"
        NasUploadTaskStatus.Deleting -> "${(progress.coerceIn(0f, 1f) * 100).toInt()}%"
        NasUploadTaskStatus.Registering -> "登记中"
        NasUploadTaskStatus.Saving -> "保存中"
        NasUploadTaskStatus.Completed -> "已完成"
        NasUploadTaskStatus.Failed -> when {
            isDelete -> "删除失败"
            isDownload -> "下载失败"
            else -> "上传失败"
        }
    }
}

internal fun List<NasUploadBatchSummary>.toProgressSummary(): NasUploadProgressSummary? {
    if (isEmpty()) return null
    val totalCount = sumOf { it.totalCount }
    if (totalCount <= 0) return null
    val completedCount = sumOf { it.completedCount }.coerceIn(0, totalCount)
    return NasUploadProgressSummary(
        totalCount = totalCount,
        completedCount = completedCount
    )
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

@Composable
internal fun NasDeleteConfirmPopup(
    count: Int,
    categoryName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val ds = LocalDesignScale.current
    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(
            with(LocalDensity.current) { (6).dp.roundToPx() },
            with(LocalDensity.current) { ds.sm(52.dp).roundToPx() }
        ),
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(ds.sm(16.dp)),
            color = Color.White,
            shadowElevation = ds.sm(30.dp),
            modifier = Modifier.width(IntrinsicSize.Max)
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        start = ds.sw(20.dp),
                        top = ds.sh(18.dp),
                        end = ds.sw(20.dp),
                        bottom = ds.sh(18.dp)
                    ),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(ds.sm(12.dp))
            ) {
                Text(
                    text = "确认删除",
                    style = TextStyle(
                        color = Color(0xE6000000),
                        fontSize = ds.sp(14f),
                        fontStyle = FontStyle.Normal,
                        fontWeight = FontWeight.W500,
                        lineHeight = TextUnit(0f, TextUnitType.Unspecified)
                    )
                )
                Text(
                    text = "这 $count ${categoryName}确认在 NAS 里删掉吗",
                    style = TextStyle(
                        color = Color(0x99000000),
                        fontSize = ds.sp(12f),
                        fontStyle = FontStyle.Normal,
                        fontWeight = FontWeight.W400,
                        lineHeight = TextUnit(0f, TextUnitType.Unspecified)
                    )
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ds.sw(12.dp))
                ) {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = Color(0xFFF5F5F5)
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(min = ds.sw(93.dp))
                                .height(ds.sh(28.dp))
                                .clickable(onClick = onDismiss)
                                .padding(horizontal = ds.sw(21.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "取消",
                                style = TextStyle(
                                    color = Color(0xE6000000),
                                    fontSize = ds.sp(12f),
                                    fontStyle = FontStyle.Normal,
                                    fontWeight = FontWeight.W500,
                                    lineHeight = TextUnit(0f, TextUnitType.Unspecified),
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = Color(0xFF1F1F1F)
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(min = ds.sw(93.dp))
                                .height(ds.sh(28.dp))
                                .clickable(onClick = onConfirm)
                                .padding(horizontal = ds.sw(21.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "删除",
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = ds.sp(12f),
                                    fontStyle = FontStyle.Normal,
                                    fontWeight = FontWeight.W500,
                                    lineHeight = TextUnit(0f, TextUnitType.Unspecified),
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
