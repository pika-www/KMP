package com.cephalon.lucyApp.screens.agentmodel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.api.CloseAccountRequest
import com.cephalon.lucyApp.auth.AuthTokenStore
import com.cephalon.lucyApp.components.CodeInput
import com.cephalon.lucyApp.components.HalfModalBottomSheet
import com.cephalon.lucyApp.ws.BalanceWsManager
import com.cephalon.lucyApp.media.PickedFile
import com.cephalon.lucyApp.media.PlatformImageThumbnail
import com.cephalon.lucyApp.media.rememberPlatformMediaAccessController
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private enum class ProfilePage {
    Settings,
    Account,
    DeleteAccount,
    Feedback,
    Recharge,
}

private val ProfilePageShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

@Composable
internal fun AgentModelProfileScreen(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onNavigateToNas: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val tokenStore: AuthTokenStore = koinInject()
    val authRepository: AuthRepository = koinInject()
    val userPhone = remember { tokenStore.getUserPhone() ?: "" }
    val userEmail = remember { tokenStore.getUserEmail() ?: "" }

    var currentPage by remember { mutableStateOf(ProfilePage.Settings) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showFeedbackSuccessDialog by remember { mutableStateOf(false) }
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
                val goingForward = targetState.ordinal > initialState.ordinal
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
                                phone = userPhone,
                                email = userEmail,
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
                                            "我的设备",
                                            "充值",
                                            "我的NAS",
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
                                                        "充值" -> currentPage = ProfilePage.Recharge
                                                        "清除缓存" -> showClearCacheDialog = true
                                                        "我的NAS" -> onNavigateToNas()
                                                        "意见反馈" -> currentPage = ProfilePage.Feedback
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
                            AccountDetailContent(
                                phone = userPhone,
                                email = userEmail,
                                onDeleteAccountClick = { currentPage = ProfilePage.DeleteAccount },
                                onLogoutClick = { showLogoutDialog = true }
                            )
                        }
                    }
                }

                ProfilePage.Recharge -> {
                    ProfilePageContainer {
                        Spacer(modifier = Modifier.height(20.dp))
                        ProfileTopBar(
                            title = "脑力值用量",
                            showBack = true,
                            onBack = { currentPage = ProfilePage.Settings },
                            onClose = onDismiss
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 18.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            RechargeContent()
                        }
                    }
                }

                ProfilePage.Feedback -> {
                    ProfilePageContainer {
                        Spacer(modifier = Modifier.height(20.dp))
                        ProfileTopBar(
                            title = "意见反馈",
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
                        ) {
                            FeedbackContent(
                                onSubmit = { showFeedbackSuccessDialog = true }
                            )
                        }
                    }
                }

                ProfilePage.DeleteAccount -> {
                    ProfilePageContainer {
                        Spacer(modifier = Modifier.height(20.dp))
                        ProfileTopBar(
                            title = "账号",
                            showBack = true,
                            onBack = { currentPage = ProfilePage.Account },
                            onClose = onDismiss
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 18.dp),
                        ) {
                            DeleteAccountContent(
                                phone = userPhone,
                                email = userEmail,
                                authRepository = authRepository,
                                onSuccess = {
                                    onLogout()
                                },
                                onCancel = { currentPage = ProfilePage.Account }
                            )
                        }
                    }
                }
            }
        } // end AnimatedContent
        } // end HalfModalBottomSheet

        if (showLogoutDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                        showLogoutDialog = false
                    },
                contentAlignment = Alignment.Center
            ) {
                LogoutConfirmDialog(
                    onDismiss = { showLogoutDialog = false },
                    onConfirm = {
                        showLogoutDialog = false
                        onLogout()
                    }
                )
            }
        }

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

        if (showFeedbackSuccessDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                        showFeedbackSuccessDialog = false
                    },
                contentAlignment = Alignment.Center
            ) {
                FeedbackSuccessDialog(
                    onDismiss = {
                        showFeedbackSuccessDialog = false
                        currentPage = ProfilePage.Settings
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
    phone: String,
    email: String,
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
                        text = if (phone.isNotEmpty()) phone else if (email.isNotEmpty()) email.substringBefore('@') else "用户",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF111111)
                    )
                    if (email.isNotEmpty()) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF555555),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (phone.isNotEmpty()) {
                        Text(
                            text = phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF555555),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
private fun AccountDetailContent(
    phone: String,
    email: String,
    onDeleteAccountClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
) {
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
                if (phone.isNotEmpty()) {
                    AccountInfoRow(label = "手机号", value = phone)
                }
                if (phone.isNotEmpty() && email.isNotEmpty()) {
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                }
                if (email.isNotEmpty()) {
                    AccountInfoRow(label = "邮箱", value = email)
                }
            }
        }

        Text(
            text = "删除账户",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFFF4444),
            modifier = Modifier
                .clickable { onDeleteAccountClick() }
                .padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "退出登录",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF111111),
            modifier = Modifier
                .clickable { onLogoutClick() }
                .padding(vertical = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/* ───────── Recharge page content ───────── */

@Composable
private fun RechargeContent() {
    val balanceWsManager: BalanceWsManager = koinInject()
    val balanceData by balanceWsManager.balance.collectAsState()

    val paidBalance = balanceData.balances["1"] ?: 0L
    val freeBalance = balanceData.balances["4"] ?: 0L

    // 进入充值页面时主动刷新一次余额
    LaunchedEffect(Unit) {
        balanceWsManager.refreshBalance()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // lucy Pro 卡片
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "lucy Pro",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111111)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF111111),
                        modifier = Modifier.clickable { }
                    ) {
                        Text(
                            text = "充值",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF0F0F0))

                Text(
                    text = "脑力值",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF111111)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "充值剩余脑力值",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "$paidBalance",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111111)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "免费脑力值",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEEEEEE))
                        )
                        Text(
                            text = "$freeBalance",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF111111)
                        )
                    }
                }
            }
        }

        // 用量详情
        Text(
            text = "用量详情",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF111111)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            UsageRecordItem(time = "2026.3.20.11:00-12:00", amount = "-82")
            HorizontalDivider(color = Color(0xFFF0F0F0))
            UsageRecordItem(time = "2026.3.19.14:00-15:00", amount = "-56")
            HorizontalDivider(color = Color(0xFFF0F0F0))
            UsageRecordItem(time = "2026.3.18.09:00-10:00", amount = "-120")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun UsageRecordItem(
    time: String,
    amount: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF666666)
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF111111)
        )
    }
}

/* ───────── Feedback page content ───────── */

@Composable
private fun FeedbackContent(
    onSubmit: () -> Unit,
) {
    val mediaController = rememberPlatformMediaAccessController { }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val attachedFiles = remember { mutableStateListOf<PickedFile>() }
    val attachedImages = remember { mutableStateListOf<String>() }
    var lastPickedFilesSize by remember { mutableStateOf(0) }
    var lastPickedImagesSize by remember { mutableStateOf(0) }

    LaunchedEffect(mediaController.pickedFiles.size) {
        val size = mediaController.pickedFiles.size
        if (size > lastPickedFilesSize) {
            mediaController.pickedFiles
                .takeLast(size - lastPickedFilesSize)
                .forEach { file ->
                    if (file.uri.isNotBlank() && attachedFiles.none { it.uri == file.uri }) {
                        attachedFiles.add(file)
                    }
                }
        }
        lastPickedFilesSize = size
    }

    LaunchedEffect(mediaController.pickedImages.size) {
        val size = mediaController.pickedImages.size
        if (size > lastPickedImagesSize) {
            mediaController.pickedImages
                .takeLast(size - lastPickedImagesSize)
                .forEach { uri ->
                    if (uri.isNotBlank() && uri !in attachedImages) {
                        attachedImages.add(uri)
                    }
                }
        }
        lastPickedImagesSize = size
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "标题",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF111111)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF5F5F5),
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF111111)),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (title.isEmpty()) {
                                Text(
                                    text = "请输入标题",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFBBBBBB)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Text(
                text = "描述",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF111111)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF5F5F5),
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(14.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF111111)),
                    decorationBox = { innerTextField ->
                        Box {
                            if (description.isEmpty()) {
                                Text(
                                    text = "请描述您遇到的问题或建议",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFBBBBBB)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Text(
                text = "附件",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF111111)
            )

            if (attachedImages.isNotEmpty()) {
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    attachedImages.forEach { uri ->
                        Box(modifier = Modifier.size(72.dp)) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF5F5F5),
                            ) {
                                PlatformImageThumbnail(
                                    uri = uri,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(
                                text = "✕",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x99000000))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                    .clickable { attachedImages.remove(uri) }
                            )
                        }
                    }
                }
            }

            if (attachedFiles.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    attachedFiles.forEach { file ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF5F5F5),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = file.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF333333),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "✕",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF999999),
                                    modifier = Modifier.clickable { attachedFiles.remove(file) }
                                )
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { mediaController.openGallery() },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDDDDD))
                ) {
                    Text("添加图片", color = Color(0xFF666666))
                }

                OutlinedButton(
                    onClick = { mediaController.openFilePicker() },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDDDDD))
                ) {
                    Text("添加文件", color = Color(0xFF666666))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        TextButton(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.textButtonColors(
                containerColor = Color(0xFF111111)
            )
        ) {
            Text(
                text = "创建工单",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun FeedbackSuccessDialog(
    onDismiss: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "提交成功",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF111111)
            )

            Text(
                text = "谢谢你的反馈，工单已经创建成功。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color(0xFF111111)
                )
            ) {
                Text("确定", color = Color.White)
            }
        }
    }
}

/* ───────── Delete Account page content ───────── */

@Composable
private fun DeleteAccountContent(
    phone: String,
    email: String,
    authRepository: AuthRepository,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val usePhone = phone.isNotEmpty()
    val account = if (usePhone) phone else email
    val way = if (usePhone) "phone_code" else "email_code"

    var code by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "确认删除账户",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF111111)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF5F5F5),
        ) {
            Text(
                text = "一旦你的账户被删除，所有数据将被永久移除且无法恢复。你的订阅将在你的账户被删除时自动取消",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                modifier = Modifier.padding(16.dp)
            )
        }

        Text(
            text = "验证您的${if (usePhone) "手机号" else "邮箱"}：\n$account",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color(0xFF111111)
        )

        CodeInput(
            value = code,
            onValueChange = { code = it; errorMsg = "" },
            enabled = !isLoading,
            onSendCode = { startTimer ->
                scope.launch {
                    val resp = authRepository.getCode(
                        phone = if (usePhone) account else null,
                        email = if (!usePhone) account else null,
                        actionType = "to_close_user",
                        appType = "lucy"
                    )
                    if (resp.code == 20000) {
                        startTimer()
                    } else {
                        errorMsg = resp.msg
                    }
                }
            }
        )

        if (errorMsg.isNotEmpty()) {
            Text(
                text = errorMsg,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF4444)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDDDDD))
            ) {
                Text("取消", color = Color(0xFF666666))
            }

            TextButton(
                onClick = {
                    if (code.isBlank()) {
                        errorMsg = "请输入验证码"
                        return@TextButton
                    }
                    isLoading = true
                    scope.launch {
                        val request = CloseAccountRequest(
                            phone = if (usePhone) account else null,
                            email = if (!usePhone) account else null,
                            code = code,
                            way = way
                        )
                        val resp = authRepository.closeAccount(request)
                        isLoading = false
                        when (resp.code) {
                            20000 -> onSuccess()
                            40020 -> errorMsg = "系统检测到您的脑力值账户为欠费状态，暂无法完成注销，请补交欠费后再试。"
                            40004 -> errorMsg = "抱歉，您的账户状态异常，暂时无法注销。请联系客服获取帮助。"
                            40021 -> errorMsg = "系统检测到您有正在运行的应用，暂无法完成注销。请关闭所有应用后，再尝试注销。"
                            30000 -> errorMsg = "邮箱 / 手机号 / 验证码有误"
                            else -> errorMsg = resp.msg
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color(0xFFFF4444)
                )
            ) {
                Text(if (isLoading) "处理中..." else "删除", color = Color.White)
            }
        }
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

/* ───────── Logout Confirm Dialog ───────── */

@Composable
private fun LogoutConfirmDialog(
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
                text = "退出登录",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF111111)
            )

            Text(
                text = "确定要退出吗？退出登录不会丢失数据，您仍然可以再次登录此账号",
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
                    Text("确定", color = Color.White)
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
