package com.cephalon.lucyApp.screens.agentmodel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.account_bg
import androidios.composeapp.generated.resources.ic_skill_knowledge
import org.jetbrains.compose.resources.painterResource
import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.api.CloseAccountRequest
import com.cephalon.lucyApp.api.RechargeRuleItem
import com.cephalon.lucyApp.auth.AuthTokenStore
import com.cephalon.lucyApp.components.CodeInput
import com.cephalon.lucyApp.components.HalfModalBottomSheet
import com.cephalon.lucyApp.getPlatform
import com.cephalon.lucyApp.payment.IAPManager
import com.cephalon.lucyApp.sdk.SdkSessionManager
import com.cephalon.lucyApp.sdk.SdkConnectionState
import com.cephalon.lucyApp.ws.BalanceWsManager
import lucy.im.sdk.OnlineDevice
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
    RechargePackage,
    MyDevices,
}

private val ProfilePageShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

@Composable
internal fun AgentModelProfileScreen(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onNavigateToNas: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
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
    var wifiConfigDevice by remember { mutableStateOf<com.cephalon.lucyApp.api.LucyDevice?>(null) }
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
                    val userInfo by authRepository.userInfo.collectAsState()
                    val displayName = userInfo?.nickname
                        ?: userPhone.ifEmpty { userEmail.substringBefore('@').ifEmpty { "用户" } }
                    val displayAccount = userEmail.ifEmpty { userPhone }
                    val avatarInitials = displayName.take(2).uppercase()

                    ProfilePageContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(modifier = Modifier.height(20.dp))
                            ProfileTopBar(
                                title = "个人中心",
                                showBack = false,
                                onBack = null,
                                onClose = onDismiss
                            )

                            Spacer(modifier = Modifier.height(46.dp))

                            // ── 头像 80×80 ──
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.20f))
                                    .then(
                                        Modifier
                                            .clip(CircleShape)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = avatarInitials,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── 名称 ──
                            Text(
                                text = displayName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF12192B),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // ── 账号 ──
                            if (displayAccount.isNotEmpty()) {
                                Text(
                                    text = displayAccount,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF595E6B),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // ── 操作卡片 ──
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                shadowElevation = 0.dp,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 8.dp),
                                ) {
                                    ProfileMenuItemNew(
                                        icon = WalletIcon,
                                        title = "充值账户",
                                        onClick = { currentPage = ProfilePage.Recharge }
                                    )
                                    HorizontalDivider(color = Color(0xFFF5F5F5))
                                    ProfileMenuItemNew(
                                        icon = NasIcon,
                                        title = "我的 NAS",
                                        onClick = { onNavigateToNas() }
                                    )
                                    HorizontalDivider(color = Color(0xFFF5F5F5))
                                    ProfileMenuItemNew(
                                        icon = DevicesIcon,
                                        title = "我的设备",
                                        onClick = { currentPage = ProfilePage.MyDevices }
                                    )
                                    HorizontalDivider(color = Color(0xFFF5F5F5))
                                    ProfileMenuItemNew(
                                        icon = FeedbackIcon,
                                        title = "意见反馈",
                                        onClick = { currentPage = ProfilePage.Feedback }
                                    )
                                    HorizontalDivider(color = Color(0xFFF5F5F5))
                                    ProfileMenuItemNew(
                                        icon = ClearCacheIcon,
                                        title = "清除缓存",
                                        showArrow = false,
                                        onClick = { showClearCacheDialog = true }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── 删除账号 ──
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                    ) { currentPage = ProfilePage.DeleteAccount },
                                shape = RoundedCornerShape(99.dp),
                                color = Color.White,
                                shadowElevation = 0.dp,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "删除账号",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color(0xFFE84026),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── 退出登录 ──
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                    ) { showLogoutDialog = true },
                                shape = RoundedCornerShape(99.dp),
                                color = Color.White,
                                shadowElevation = 0.dp,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "退出登录",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color(0xFF1F2535),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
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
                            RechargeContent(
                                onNavigateToPackage = { currentPage = ProfilePage.RechargePackage }
                            )
                        }
                    }
                }

                ProfilePage.RechargePackage -> {
                    ProfilePageContainer {
                        Spacer(modifier = Modifier.height(20.dp))
                        ProfileTopBar(
                            title = "选择套餐",
                            showBack = true,
                            onBack = { currentPage = ProfilePage.Recharge },
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
                            RechargePackageContent()
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
                                authRepository = authRepository,
                                onSubmitSuccess = { showFeedbackSuccessDialog = true }
                            )
                        }
                    }
                }

                ProfilePage.MyDevices -> {
                    ProfilePageContainer {
                        Spacer(modifier = Modifier.height(20.dp))
                        ProfileTopBar(
                            title = "当前设备",
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
                            MyDevicesContent(
                                onAddNewDevice = onNavigateToHome,
                                onConfigureWifi = { device -> wifiConfigDevice = device },
                            )
                        }
                    }
                }

                ProfilePage.DeleteAccount -> {
                    ProfilePageContainer {
                        Spacer(modifier = Modifier.height(20.dp))
                        ProfileTopBar(
                            title = "删除账号",
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
                            DeleteAccountContent(
                                phone = userPhone,
                                email = userEmail,
                                authRepository = authRepository,
                                onSuccess = {
                                    onLogout()
                                },
                                onCancel = { currentPage = ProfilePage.Settings }
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

        WifiConfigSheet(
            isVisible = wifiConfigDevice != null,
            device = wifiConfigDevice,
            onDismiss = { wifiConfigDevice = null }
        )
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
            .background(Color(0xFFF5F5F7))
    ) {
        Image(
            painter = painterResource(Res.drawable.account_bg),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            contentScale = ContentScale.FillWidth,
        )
        Column(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

/* ───────── Top Bar ───────── */

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
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF12192B),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(40.dp)
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

/* ───────── Menu item with icon + title + chevron ───────── */

@Composable
private fun ProfileMenuItemNew(
    icon: ImageVector,
    title: String,
    showArrow: Boolean = true,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF12192B),
            modifier = Modifier.weight(1f),
        )

        if (showArrow) {
            Icon(
                imageVector = ChevronRightIcon,
                contentDescription = "Arrow",
                tint = Color.Unspecified,
                modifier = Modifier.size(width = 12.dp, height = 24.dp)
            )
        }
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
private fun RechargeContent(
    onNavigateToPackage: () -> Unit = {},
) {
    val balanceWsManager: BalanceWsManager = koinInject()
    val balanceData by balanceWsManager.balance.collectAsState()
    val uriHandler = LocalUriHandler.current
    val isIos = remember { getPlatform().name.startsWith("iOS", ignoreCase = true) }

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
                        modifier = Modifier.clickable {
                            if (isIos) {
                                onNavigateToPackage()
                            } else {
                                uriHandler.openUri("https://cephalon.cloud")
                            }
                        }
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

private data class FixedPackage(
    val price: Double,
    val priceLabel: String,
    val base: Long,
    val tag: String? = null,
)

private val fixedPackages = listOf(
    FixedPackage(9.9, "¥9.9", 7000, tag = "体验"),
    FixedPackage(99.0, "¥99", 70000, tag = "推荐"),
    FixedPackage(999.0, "¥999", 700000, tag = "最佳价值"),
    FixedPackage(39.9, "¥39.9", 28000),
    FixedPackage(69.9, "¥69.9", 49000),
    FixedPackage(299.0, "¥299", 210000),
    FixedPackage(499.0, "¥499", 350000),
    FixedPackage(500.0, "¥500", 350000),
    FixedPackage(699.0, "¥699", 490000),
    FixedPackage(899.0, "¥899", 630000),
    FixedPackage(1000.0, "¥1000", 700000),
)

private fun FixedPackage.appleProductId(): String = when (price) {
    9.9 -> "com.cephalon.lucyApp.9.9"
    39.9 -> "com.cephalon.lucyApp.39.9"
    69.9 -> "com.cephalon.lucyApp.69.9"
    99.0 -> "com.cephalon.lucyApp.99"
    299.0 -> "com.cephalon.lucyApp.299"
    499.0 -> "com.cephalon.lucyApp.499"
    500.0 -> "com.cephalon.lucyApp.500"
    699.0 -> "com.cephalon.lucyApp.699"
    899.0 -> "com.cephalon.lucyApp.899"
    999.0 -> "com.cephalon.lucyApp.999"
    1000.0 -> "com.cephalon.lucyApp.1000"
    else -> ""
}

private fun findGiftPercent(price: Double, rules: List<RechargeRuleItem>): Int {
    return rules.firstOrNull { price >= it.littleValue && price < it.largeValue }?.giftPercent ?: 0
}

private suspend fun handleRechargePackageClick(
    pkg: FixedPackage,
    authRepository: AuthRepository,
    iapManager: IAPManager,
) {
    val productId = pkg.appleProductId()
    println("[IAP][UI] 点击充值卡片: tag=${pkg.tag ?: "none"}, priceLabel=${pkg.priceLabel}, amount=${pkg.price}, base=${pkg.base}, productId=$productId")
    if (productId.isBlank()) {
        println("[IAP][UI] 未找到对应商品ID, amount=${pkg.price}")
        return
    }

    println("[IAP][UI] Step 1: 调用创建订单接口 /v1/orders/transfers, amount=${pkg.base}")
    val orderResponse = authRepository.createRechargeOrder(pkg.base)
    println("[IAP][API] createRechargeOrder 响应: code=${orderResponse.code}, msg=${orderResponse.msg}, data=${orderResponse.data}")
    if (orderResponse.code != 20000 || orderResponse.data == null) {
        println("[IAP][UI] Step 1 失败: 创建订单失败")
        return
    }

    println("[IAP][UI] Step 1.5: 确保商品已加载")
    iapManager.loadProducts()
    println("[IAP][UI] Step 2: 调用 Apple 购买, productId=$productId, orderData=${orderResponse.data}")
    val transactionId = iapManager.initiatePurchase(productId)
    println("[IAP][IAP] initiatePurchase 返回: transactionId=$transactionId")
    if (transactionId.isNullOrBlank()) {
        println("[IAP][UI] Step 2 失败: Apple 购买未返回有效 transactionId")
        return
    }

    println("[IAP][UI] Step 3: 调用验单接口 /v1/orders/apple/verify, transactionId=$transactionId")
    val verifyResponse = authRepository.verifyAppleIAPTransaction(transactionId)
    println("[IAP][API] verifyAppleIAPTransaction 响应: code=${verifyResponse.code}, msg=${verifyResponse.msg}, data=${verifyResponse.data}")
    if (verifyResponse.code != 20000) {
        println("[IAP][UI] Step 3 失败: 验单失败, transactionId=$transactionId")
        return
    }

    println("[IAP][UI] Step 4: finishTransaction, transactionId=$transactionId")
    iapManager.finishTransaction(transactionId)
    println("[IAP][UI] Step 4 完成: 整个购买流程结束, transactionId=$transactionId")
}

@Composable
private fun RechargePackageContent() {
    val authRepository: AuthRepository = koinInject()
    val iapManager: IAPManager = koinInject()
    val coroutineScope = rememberCoroutineScope()

    var rules by remember { mutableStateOf<List<RechargeRuleItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        iapManager.loadProducts()
        val response = authRepository.getRechargeRules()
        if (response.code == 20000 && response.data != null) {
            rules = response.data.sortedBy { it.littleValue }
        }
        isLoading = false
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF111111)
                )
            }
        } else {
            fixedPackages.take(3).forEach { pkg ->
                val giftPercent = findGiftPercent(pkg.price, rules)
                val gift = pkg.base * giftPercent / 100
                val total = pkg.base + gift
                PackageCard(
                    total = "$total",
                    detail = "基础${pkg.base}+${gift}奖励",
                    price = pkg.priceLabel,
                    tag = pkg.tag,
                    onClick = {
                        coroutineScope.launch {
                            handleRechargePackageClick(pkg, authRepository, iapManager)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val gridItems = fixedPackages.drop(3)
            for (i in gridItems.indices step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val pkg1 = gridItems[i]
                    val gp1 = findGiftPercent(pkg1.price, rules)
                    val gift1 = pkg1.base * gp1 / 100
                    PackageCard(
                        total = "${pkg1.base + gift1}",
                        detail = "基础${pkg1.base}+${gift1}",
                        price = pkg1.priceLabel,
                        tag = pkg1.tag,
                        onClick = {
                            coroutineScope.launch {
                                handleRechargePackageClick(pkg1, authRepository, iapManager)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (i + 1 < gridItems.size) {
                        val pkg2 = gridItems[i + 1]
                        val gp2 = findGiftPercent(pkg2.price, rules)
                        val gift2 = pkg2.base * gp2 / 100
                        PackageCard(
                            total = "${pkg2.base + gift2}",
                            detail = "基础${pkg2.base}+${gift2}",
                            price = pkg2.priceLabel,
                            tag = pkg2.tag,
                            onClick = {
                                coroutineScope.launch {
                                    handleRechargePackageClick(pkg2, authRepository, iapManager)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 规则说明
        Text(
            text = "规则和信息",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF111111)
        )
        Text(
            text = "赠送脑力值计入活动脑力值",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF888888)
        )
        Text(
            text = "优先消耗顺序为 每日赠送，活动脑力值，充值脑力值",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF888888)
        )
        Text(
            text = "消费明细按小时展示",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF888888)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PackageCard(
    total: String,
    detail: String,
    price: String,
    tag: String? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF5F5F5),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = total,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = Color(0xFF111111)
                    )
                    Text(
                        text = "脑力值",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
                if (tag != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE0E0E0),
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF555555),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF999999)
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDDDDD)),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
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
    authRepository: AuthRepository,
    onSubmitSuccess: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val mediaController = rememberPlatformMediaAccessController { }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val attachedFiles = remember { mutableStateListOf<PickedFile>() }
    val attachedImages = remember { mutableStateListOf<String>() }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
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

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE53935)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        TextButton(
            onClick = {
                if (description.isBlank()) {
                    errorMessage = "请填写反馈内容"
                    return@TextButton
                }
                errorMessage = null
                isSubmitting = true
                coroutineScope.launch {
                    val allImages = attachedImages.toList() + attachedFiles.map { it.uri }
                    val request = com.cephalon.lucyApp.api.FeedbackRequest(
                        title = title.trim(),
                        content = description.trim(),
                        images = allImages
                    )
                    val resp = authRepository.submitFeedback(request)
                    isSubmitting = false
                    if (resp.code == 20000) {
                        onSubmitSuccess()
                    } else {
                        errorMessage = resp.msg.ifBlank { "提交失败，请稍后重试" }
                    }
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.textButtonColors(
                containerColor = if (isSubmitting) Color(0xFF888888) else Color(0xFF111111)
            )
        ) {
            Text(
                text = if (isSubmitting) "提交中..." else "创建工单",
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
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text(
                text = "清除缓存",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2535),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "缓存数据有助于加快加载速度，清除可能会导致内容重新加载",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF717580),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { onDismiss() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "取消",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF1F2535),
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { onConfirm() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "清除",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFFE84026),
                    )
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
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text(
                text = "退出登录",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2535),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "确定要退出吗？退出登录不会丢失数据，您仍然可以再次登录此账号",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF717580),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { onDismiss() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "取消",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF1F2535),
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { onConfirm() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "退出",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFFE84026),
                    )
                }
            }
        }
    }
}

/* ───────── My Devices page ───────── */

@Composable
private fun MyDevicesContent(
    onAddNewDevice: () -> Unit = {},
    onConfigureWifi: (com.cephalon.lucyApp.api.LucyDevice) -> Unit = {},
) {
    val sdkSessionManager = koinInject<SdkSessionManager>()
    val authRepository = koinInject<AuthRepository>()
    val onlineDevices by sdkSessionManager.onlineDevices.collectAsState()
    val connectionState by sdkSessionManager.connectionState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showSwitchDeviceSheet by remember { mutableStateOf(false) }
    var backendDevices by remember { mutableStateOf<List<com.cephalon.lucyApp.api.LucyDevice>>(emptyList()) }

    LaunchedEffect(Unit) {
        println("[MyDevices] 进入设备页, connectionState=$connectionState, onlineDevices=${onlineDevices.map { it.cdi }}")
        sdkSessionManager.ensureConnectedIfTokenValid()
        // 同时加载后端设备列表以备切换
        backendDevices = authRepository.getDevices()
    }

    if (connectionState == SdkConnectionState.CONNECTING) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("连接中...", color = Color(0xFF999999))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = sdkSessionManager.connectionLog.collectAsState().value,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFBBBBBB)
                )
            }
        }
        return
    }

    if (onlineDevices.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (connectionState == SdkConnectionState.CONNECTED) "当前无在线设备" else "SDK 未连接",
                    color = Color(0xFF999999)
                )
                if (connectionState != SdkConnectionState.CONNECTED) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = sdkSessionManager.connectionLog.collectAsState().value,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFBBBBBB)
                    )
                }
            }
        }
        return
    }

    // 取第一个在线设备作为当前设备展示
    val currentDevice = onlineDevices.first()
    // 从后端设备列表中找到匹配当前 CDI 的设备信息
    val matchedBackendDevice = backendDevices.firstOrNull {
        it.pairingInfo?.channelDeviceId?.trim() == currentDevice.cdi
    }

    OnlineDeviceCard(
        device = currentDevice,
        backendDevice = matchedBackendDevice,
        onSwitchDevice = {
            coroutineScope.launch {
                backendDevices = authRepository.getDevices()
                showSwitchDeviceSheet = true
            }
        },
        onAddNewDevice = onAddNewDevice,
        onConfigureWifi = {
            val dev = matchedBackendDevice ?: com.cephalon.lucyApp.api.LucyDevice(
                name = currentDevice.cdi,
                serialNumber = currentDevice.cdi,
                status = "online",
            )
            onConfigureWifi(dev)
        },
    )

    // 如果有多个在线设备，也显示其他
    if (onlineDevices.size > 1) {
        Spacer(modifier = Modifier.height(16.dp))
        onlineDevices.drop(1).forEach { device ->
            val matched = backendDevices.firstOrNull {
                it.pairingInfo?.channelDeviceId?.trim() == device.cdi
            }
            OnlineDeviceCard(
                device = device,
                backendDevice = matched,
                onSwitchDevice = {
                    coroutineScope.launch {
                        backendDevices = authRepository.getDevices()
                        showSwitchDeviceSheet = true
                    }
                },
                onAddNewDevice = onAddNewDevice,
                onConfigureWifi = {
                    val dev = matched ?: com.cephalon.lucyApp.api.LucyDevice(
                        name = device.cdi,
                        serialNumber = device.cdi,
                        status = "online",
                    )
                    onConfigureWifi(dev)
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 切换设备二级模态框
    if (showSwitchDeviceSheet) {
        SwitchDeviceSheet(
            devices = backendDevices,
            currentCdi = currentDevice.cdi,
            onDismiss = { showSwitchDeviceSheet = false },
            onDeviceSelected = { selectedDevice ->
                showSwitchDeviceSheet = false
                println("[MyDevices] 选择设备: ${selectedDevice.name} cdi=${selectedDevice.pairingInfo?.channelDeviceId}")
            }
        )
    }
}

@Composable
private fun OnlineDeviceCard(
    device: OnlineDevice,
    backendDevice: com.cephalon.lucyApp.api.LucyDevice?,
    onSwitchDevice: () -> Unit = {},
    onAddNewDevice: () -> Unit = {},
    onConfigureWifi: () -> Unit = {},
) {
    val displayName = backendDevice?.serialNumber?.ifBlank { null }
        ?: backendDevice?.name?.ifBlank { null }
        ?: device.cdi

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
                .padding(20.dp),
        ) {
            // ── 设备信息行 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1F2535),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_skill_knowledge),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF111111),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "设备在线",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF999999)
                    )
                }

                Text(
                    text = "已连接",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF3478F6),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 切换设备 + 添加新设备 按钮行 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DeviceActionButton(
                    text = "切换设备",
                    filled = false,
                    modifier = Modifier.weight(1f),
                    onClick = onSwitchDevice
                )
                DeviceActionButton(
                    text = "添加新设备",
                    filled = true,
                    modifier = Modifier.weight(1f),
                    onClick = onAddNewDevice
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── 配置 WIFI 按钮 ──
            DeviceActionButton(
                text = "配置 WIFI",
                filled = false,
                modifier = Modifier.fillMaxWidth(),
                onClick = onConfigureWifi
            )
        }
    }
}

@Composable
private fun DeviceActionButton(
    text: String,
    filled: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val bgColor = if (filled) Color(0xFF1F2535) else Color(0xFFF5F5F7)
    val textColor = if (filled) Color.White else Color(0xFF111111)

    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(99.dp),
        color = bgColor
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = textColor
            )
        }
    }
}

/* ───────── Switch Device Sheet ───────── */

@Composable
private fun SwitchDeviceSheet(
    devices: List<com.cephalon.lucyApp.api.LucyDevice>,
    currentCdi: String,
    onDismiss: () -> Unit,
    onDeviceSelected: (com.cephalon.lucyApp.api.LucyDevice) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { /* block clicks */ },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color(0xFFF5F5F7),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // ── 拖拽指示器 ──
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = Color(0xFFDDDDDD),
                        modifier = Modifier.size(width = 36.dp, height = 4.dp)
                    ) {}
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "切换设备",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF111111)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无可用设备", color = Color(0xFF999999))
                    }
                } else {
                    devices.forEach { device ->
                        val isCurrent = device.pairingInfo?.channelDeviceId?.trim() == currentCdi
                        SwitchDeviceItem(
                            device = device,
                            isCurrent = isCurrent,
                            onClick = {
                                if (!isCurrent) onDeviceSelected(device)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SwitchDeviceItem(
    device: com.cephalon.lucyApp.api.LucyDevice,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val displayName = device.serialNumber.ifBlank { device.name.ifBlank { device.id } }
    val isOnline = device.status == "online" || device.status == "free"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCurrent) { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1F2535),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_skill_knowledge),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF111111),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isOnline) "设备在线" else "设备离线",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOnline) Color(0xFF999999) else Color(0xFFCC3333)
                )
            }

            if (isCurrent) {
                Text(
                    text = "当前",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF3478F6),
                )
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
