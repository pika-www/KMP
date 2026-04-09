package com.cephalon.lucyApp.screens.agentmodel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.FastOutSlowInEasing
import com.cephalon.lucyApp.components.HalfModalBottomSheet

private enum class ProfilePage {
    Settings,
    Account,
}

private val ProfilePageShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

@Composable
internal fun AgentModelProfileScreen(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onNavigateToNas: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var currentPage by remember { mutableStateOf(ProfilePage.Settings) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var cacheSizeBytes by remember { mutableStateOf(getAppCacheSize()) }

    Box(modifier = modifier) {
        HalfModalBottomSheet(
            isVisible = isVisible,
            onDismissRequest = onDismiss,
            onDismissed = { currentPage = ProfilePage.Settings },
            showBackButton = false,
            showCloseButton = false,
            showTopBar = false,
            topPadding = 72.dp,
            containerShape = RoundedCornerShape(0.dp),
            containerColor = Color.Transparent,
            contentPadding = PaddingValues(0.dp)
        ) {
            AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                val goingForward = initialState == ProfilePage.Settings && targetState == ProfilePage.Account
                val slideSpec = tween<IntOffset>(durationMillis = 320, easing = FastOutSlowInEasing)
                if (goingForward) {
                    ContentTransform(
                        targetContentEnter = slideInHorizontally(
                            animationSpec = slideSpec,
                            initialOffsetX = { it }
                        ),
                        initialContentExit = slideOutHorizontally(
                            animationSpec = slideSpec,
                            targetOffsetX = { -it }
                        ),
                        targetContentZIndex = 1f,
                        sizeTransform = SizeTransform(clip = true)
                    )
                } else {
                    ContentTransform(
                        targetContentEnter = slideInHorizontally(
                            animationSpec = slideSpec,
                            initialOffsetX = { -it }
                        ),
                        initialContentExit = slideOutHorizontally(
                            animationSpec = slideSpec,
                            targetOffsetX = { it }
                        ),
                        targetContentZIndex = 1f,
                        sizeTransform = SizeTransform(clip = true)
                    )
                }
            },
            label = "ProfileSheetPage"
        ) { page ->
            when (page) {
                ProfilePage.Settings -> {
                    ProfilePageContainer {
                        Spacer(modifier = Modifier.height(20.dp))
                        ProfileTopBar(
                            showBack = false,
                            onBack = null,
                            onClose = onDismiss
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 18.dp, vertical = 0.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            ProfileHeaderCard(
                                onHeaderClick = { currentPage = ProfilePage.Account }
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "通用",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF111111)
                                )

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(28.dp),
                                    color = Color.White,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        val menuItems = listOf(
                                            "账号",
                                            "接入方式切换",
                                            "充值",
                                            "我的NAS",
                                            "我的设备",
                                            "清除缓存",
                                            "意见反馈"
                                        )
                                        menuItems.forEachIndexed { index, title ->
                                            ProfileMenuItem(
                                                title = title,
                                                subtitle = if (title == "清除缓存") formatCacheSize(cacheSizeBytes) else null,
                                                onClick = {
                                                    when (title) {
                                                        "账号" -> currentPage = ProfilePage.Account
                                                        "清除缓存" -> showClearCacheDialog = true
                                                        "我的NAS" -> onNavigateToNas()
                                                    }
                                                }
                                            )
                                            if (index != menuItems.lastIndex) {
                                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                ProfilePage.Account -> {
                    ProfilePageContainer {
                        Spacer(modifier = Modifier.height(20.dp))
                        ProfileTopBar(
                            title = "账号",
                            showBack = true,
                            onBack = { currentPage = ProfilePage.Settings },
                            onClose = onDismiss
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AccountDetailContent()
                        }
                    }
                }
            }
        } // end AnimatedContent
        } // end HalfModalBottomSheet

        if (showClearCacheDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                        showClearCacheDialog = false
                    },
                contentAlignment = Alignment.Center
            ) {
                ClearCacheConfirmDialog(
                    cacheSizeText = formatCacheSize(cacheSizeBytes),
                    onDismiss = { showClearCacheDialog = false },
                    onConfirm = {
                        clearAppCache()
                        cacheSizeBytes = getAppCacheSize()
                        showClearCacheDialog = false
                    }
                )
            }
        }
    } // Box
}

private fun formatCacheSize(bytes: Long): String {
    return when {
        bytes < 1024L -> "${bytes}B"
        bytes < 1024L * 1024L -> "${roundTo1(bytes / 1024.0)}KB"
        bytes < 1024L * 1024L * 1024L -> "${roundTo1(bytes / (1024.0 * 1024.0))}MB"
        else -> "${roundTo2(bytes / (1024.0 * 1024.0 * 1024.0))}GB"
    }
}

private fun roundTo1(value: Double): String {
    val rounded = (value * 10).toLong() / 10.0
    return if (rounded == rounded.toLong().toDouble()) "${rounded.toLong()}.0" else rounded.toString()
}

private fun roundTo2(value: Double): String {
    val rounded = (value * 100).toLong() / 100.0
    return rounded.toString()
}

/* ───────── Page Container (same shape as LoginScreen) ───────── */

@Composable
private fun ProfilePageContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                shape = ProfilePageShape
                clip = true
                shadowElevation = 0f
            }
            .background(Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

/* ───────── Top Bar (matches LoginScreen SheetTopBar) ───────── */

@Composable
private fun ProfileTopBar(
    title: String? = null,
    showBack: Boolean,
    onBack: (() -> Unit)?,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            IconButton(
                onClick = { onBack?.invoke() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE6E6E6))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF2D2D2D),
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(40.dp))
        }

        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF111111),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFE6E6E6))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color(0xFF2D2D2D),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/* ───────── Settings page: header card ───────── */

@Composable
private fun ProfileHeaderCard(
    onHeaderClick: () -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHeaderClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE6E6E6),
                    modifier = Modifier.size(62.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize())
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "ELENA",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF111111)
                    )
                    Text(
                        text = "ELENAZHANG@GMAIL.COM",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF555555),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = ">",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

/* ───────── Settings page: menu item row ───────── */

@Composable
private fun ProfileMenuItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFE6E6E6),
            modifier = Modifier.size(42.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE6E6E6))
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF111111),
            modifier = Modifier.weight(1f)
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF999999)
            )
        }

        Text(
            text = ">",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF666666)
        )
    }
}

/* ───────── Account detail page content ───────── */

@Composable
private fun AccountDetailContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = CircleShape,
            color = Color(0xFFE6E6E6),
            modifier = Modifier.size(80.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }

        Text(
            text = "点击更换头像",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF888888)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                AccountInfoRow(label = "手机号", value = "122222222222")
                HorizontalDivider(color = Color(0xFFF0F0F0))
                AccountInfoRow(label = "邮箱", value = "12345@Gmail.com")
            }
        }

        Text(
            text = "删除账户",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFFF4444),
            modifier = Modifier
                .clickable { }
                .padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "退出登录",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF111111),
            modifier = Modifier
                .clickable { }
                .padding(vertical = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/* ───────── Clear Cache Dialog (inline, matches flow chart) ───────── */

@Composable
private fun ClearCacheConfirmDialog(
    cacheSizeText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { /* consume click */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "清除缓存",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF111111)
            )

            Text(
                text = "缓存数据有助于加快加载速度，清除可能会导致内容重新加载\n当前缓存：$cacheSizeText",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDDDDD))
                ) {
                    Text("取消", color = Color(0xFF666666))
                }

                TextButton(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color(0xFF111111)
                    )
                ) {
                    Text("清除", color = Color.White)
                }
            }
        }
    }
}

/* ───────── Platform cache clearing ───────── */

expect fun clearAppCache()
expect fun getAppCacheSize(): Long

/* ───────── Account detail helpers ───────── */

@Composable
private fun AccountInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF666666),
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color(0xFF111111),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
