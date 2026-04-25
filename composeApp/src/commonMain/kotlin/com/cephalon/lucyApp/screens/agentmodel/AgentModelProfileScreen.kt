package com.cephalon.lucyApp.screens.agentmodel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.account_bg
import androidios.composeapp.generated.resources.cep_bg
import androidios.composeapp.generated.resources.tc_bg
import androidios.composeapp.generated.resources.other_bg
import androidios.composeapp.generated.resources.ic_device_storage
import androidios.composeapp.generated.resources.ic_modal_close
import androidios.composeapp.generated.resources.`return`
import org.jetbrains.compose.resources.painterResource
import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.api.channelDeviceId
import com.cephalon.lucyApp.api.CloseAccountRequest
import com.cephalon.lucyApp.api.ModelRecordItem
import com.cephalon.lucyApp.api.RechargeRuleItem
import com.cephalon.lucyApp.auth.AuthTokenStore
import com.cephalon.lucyApp.components.CodeInput
import com.cephalon.lucyApp.components.HalfModalBottomSheet
import com.cephalon.lucyApp.components.LocalDesignScale
import com.cephalon.lucyApp.components.ToastHost
import com.cephalon.lucyApp.components.rememberToastState
import com.cephalon.lucyApp.getPlatform
import com.cephalon.lucyApp.payment.IAPManager
import com.cephalon.lucyApp.payment.PurchaseOutcome
import com.cephalon.lucyApp.sdk.SdkSessionManager
import com.cephalon.lucyApp.ws.BalanceWsManager
import com.cephalon.lucyApp.media.PickedFile
import com.cephalon.lucyApp.media.PlatformImageThumbnail
import com.cephalon.lucyApp.media.rememberPlatformMediaAccessController
import com.cephalon.lucyApp.scan.rememberOpenWifiSettings
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

internal enum class ProfilePage {
    Settings,
    Account,
    Feedback,
    Recharge,
    RechargePackage,
    MyDevices,
    SwitchDevice,
    WifiConfig,
}

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
    val sdkSessionManager: SdkSessionManager = koinInject()
    val onlineCdis by sdkSessionManager.onlineDeviceCdis.collectAsState()
    val selectedCdi by sdkSessionManager.selectedDeviceCdi.collectAsState()
    val isSelectedDeviceOnline = selectedCdi != null && selectedCdi in onlineCdis
    val userPhone = remember { tokenStore.getUserPhone() ?: "" }
    val userEmail = remember { tokenStore.getUserEmail() ?: "" }

    var currentPage by remember { mutableStateOf(ProfilePage.Settings) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var wifiConfigDevice by remember { mutableStateOf<com.cephalon.lucyApp.api.LucyDevice?>(null) }
    var switchDeviceList by remember { mutableStateOf<List<com.cephalon.lucyApp.api.LucyDevice>>(emptyList()) }
    var switchDeviceCurrentCdi by remember { mutableStateOf("") }
    var showFeedbackSuccessDialog by remember { mutableStateOf(false) }
    var cacheSizeBytes by remember { mutableStateOf(getAppCacheSize()) }
    var currentDeviceType by remember { mutableStateOf("") }

    LaunchedEffect(isVisible, selectedCdi) {
        if (isVisible && selectedCdi != null) {
            val device = authRepository.findDeviceByChannelDeviceId(selectedCdi!!)
            currentDeviceType = device?.deviceType.orEmpty()
        }
    }

    val ds = LocalDesignScale.current
    Box(modifier = modifier) {
        HalfModalBottomSheet(
            isVisible = isVisible,
            onDismissRequest = onDismiss,
            onDismissed = { currentPage = ProfilePage.Settings },
            showBackButton = false,
            showCloseButton = false,
            showTopBar = false,
            topPadding = ds.sh(72.dp),
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
                    val ds = LocalDesignScale.current

                    ProfilePageContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(modifier = Modifier.height(ds.sh(20.dp)))
                            ProfileTopBar(
                                title = "个人中心",
                                showBack = false,
                                onBack = null,
                                onClose = onDismiss
                            )

                            Spacer(modifier = Modifier.height(ds.sh(46.dp)))

                            // ── 头像 ──
                            Box(
                                modifier = Modifier
                                    .size(ds.sm(80.dp))
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.20f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = avatarInitials,
                                    fontSize = ds.sp(28f),
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                            }

                            Spacer(modifier = Modifier.height(ds.sh(16.dp)))

                            // ── 名称 ──
                            Text(
                                text = displayName,
                                fontSize = ds.sp(20f),
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF12192B),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = ds.sw(20.dp)),
                            )

//                            Spacer(modifier = Modifier.height(ds.sh(4.dp)))

                            // ── 账号 ──
//                            if (displayAccount.isNotEmpty()) {
//                                Text(
//                                    text = displayAccount,
//                                    fontSize = ds.sp(14f),
//                                    fontWeight = FontWeight.Normal,
//                                    color = Color(0xFF595E6B),
//                                    textAlign = TextAlign.Center,
//                                    maxLines = 1,
//                                    overflow = TextOverflow.Ellipsis,
//                                    modifier = Modifier.padding(horizontal = ds.sw(20.dp)),
//                                )
//                            }

                            Spacer(modifier = Modifier.height(ds.sh(32.dp)))

                            // ── 操作卡片 ──
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = ds.sw(20.dp)),
                                shape = RoundedCornerShape(ds.sm(16.dp)),
                                color = Color.White,
                                shadowElevation = 0.dp,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = ds.sw(20.dp), vertical = ds.sh(8.dp)),
                                ) {
                                    ProfileMenuItemNew(
                                        icon = WalletIcon,
                                        title = "充值账户",
                                        onClick = { currentPage = ProfilePage.Recharge }
                                    )
                                    if (currentDeviceType == "ai_npc") {
                                        HorizontalDivider(color = Color(0xFFF5F5F5))
                                        ProfileMenuItemNew(
                                            icon = NasIcon,
                                            title = "我的 NAS",
                                            onClick = { onNavigateToNas() }
                                        )
                                    }
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

                            Spacer(modifier = Modifier.height(ds.sh(16.dp)))

                            // ── 退出登录 ──
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = ds.sw(20.dp))
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                    ) { showLogoutDialog = true },
                                shape = RoundedCornerShape(ds.sm(99.dp)),
                                color = Color.White,
                                shadowElevation = 0.dp,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = ds.sh(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "退出登录",
                                        fontSize = ds.sp(16f),
                                        fontWeight = FontWeight.Normal,
                                        color = Color(0xFF1F2535),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(ds.sh(32.dp)))
                        }
                    }
                }

                ProfilePage.Account -> {
                    val ds = LocalDesignScale.current
                    ProfilePageContainer(showBackground = false, backgroundColor = Color(0xFFF3F3F3)) {
                        Spacer(modifier = Modifier.height(ds.sh(20.dp)))
                        ProfileTopBar(
                            title = "账号",
                            showBack = true,
                            onBack = { currentPage = ProfilePage.Settings },
                            onClose = onDismiss
                        )
                        Spacer(modifier = Modifier.height(ds.sh(20.dp)))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = ds.sw(18.dp)),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AccountDetailContent(
                                phone = userPhone,
                                email = userEmail,
                                onLogoutClick = { showLogoutDialog = true }
                            )
                        }
                    }
                }

                ProfilePage.Recharge -> {
                    // 充值 / 脑力值页改造成全屏黑色页面：实际 UI 由同屏叠放在 HalfModalBottomSheet
                    // 外层的 BrainPowerBalancePage 覆盖层绘制（见下方 AnimatedVisibility）。
                    // 这里只保留一个空占位 Box，确保 AnimatedContent 状态切换不抛 IllegalState。
                    Box(modifier = Modifier.fillMaxSize())
                }

                ProfilePage.RechargePackage -> {
                    // 套餐选择页改造成全屏黑色页面：实际 UI 由同屏叠放的
                    // RechargePackagePage 覆盖层绘制（见下方 AnimatedVisibility）。
                    Box(modifier = Modifier.fillMaxSize())
                }

                ProfilePage.Feedback -> {
                    val ds = LocalDesignScale.current
                    ProfilePageContainer(showBackground = false, backgroundColor = Color(0xFFF3F3F3)) {
                        Spacer(modifier = Modifier.height(ds.sh(16.dp)))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = ds.sw(12.dp)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = { currentPage = ProfilePage.Settings },
                                modifier = Modifier.size(ds.sm(40.dp)),
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
                                        painter = painterResource(Res.drawable.`return`),
                                        contentDescription = "Back",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(ds.sm(16.dp)),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(ds.sh(32.dp)))
                        Text(
                            text = "意见反馈",
                            fontSize = ds.sp(24f),
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF12192B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = ds.sw(18.dp)),
                        )

                        Spacer(modifier = Modifier.height(ds.sh(20.dp)))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = ds.sw(18.dp)),
                        ) {
                            FeedbackContent(
                                authRepository = authRepository,
                                onSubmitSuccess = { showFeedbackSuccessDialog = true }
                            )
                        }
                    }
                }

                ProfilePage.MyDevices -> {
                    val ds = LocalDesignScale.current
                    ProfilePageContainer(showBackground = false, backgroundColor = Color(0xFFF3F3F3)) {
                        Spacer(modifier = Modifier.height(ds.sh(20.dp)))
                        ProfileTopBar(
                            title = null,
                            showBack = true,
                            onBack = { currentPage = ProfilePage.Settings },
                            onClose = onDismiss,
                            showClose = false,
                        )
                        Spacer(modifier = Modifier.height(ds.sh(32.dp)))

                        // "当前设备" 标题
                        Text(
                            text = "当前设备",
                            fontSize = ds.sp(24f),
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF12192B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = ds.sw(20.dp)),
                        )
                        Spacer(modifier = Modifier.height(ds.sh(24.dp)))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = ds.sw(20.dp))
                                .verticalScroll(rememberScrollState()),
                        ) {
                            MyDevicesContent(
                                onAddNewDevice = onNavigateToHome,
                                onConfigureWifi = { device ->
                                    wifiConfigDevice = device
                                    currentPage = ProfilePage.WifiConfig
                                },
                                onSwitchDevice = { devices, currentCdi ->
                                    switchDeviceList = devices
                                    switchDeviceCurrentCdi = currentCdi
                                    currentPage = ProfilePage.SwitchDevice
                                },
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            val annotatedText = buildAnnotatedString {
                                withStyle(SpanStyle(color = Color.Black.copy(alpha = 0.40f))) {
                                    append("账号注销意味着彻底失去所有的数据 ")
                                }
                                val link = LinkAnnotation.Clickable(tag = "DELETE") {
                                    showDeleteAccountDialog = true
                                }
                                withLink(link) {
                                    withStyle(SpanStyle(
                                        color = Color.Black.copy(alpha = 0.90f),
                                        textDecoration = TextDecoration.Underline,
                                    )) {
                                        append("立即注销")
                                    }
                                }
                            }
                            Text(
                                text = annotatedText,
                                style = TextStyle(
                                    fontSize = ds.sp(12f),
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = ds.sh(20.dp)),
                            )
                        }
                    }
                }

                ProfilePage.SwitchDevice -> {
                    val ds = LocalDesignScale.current
                    ProfilePageContainer(showBackground = false, backgroundColor = Color(0xFFF3F3F3)) {
                        val sdkSessionManager = koinInject<SdkSessionManager>()
                        var pendingCdi by remember(switchDeviceCurrentCdi) {
                            mutableStateOf(switchDeviceCurrentCdi)
                        }

                        Spacer(modifier = Modifier.height(ds.sh(16.dp)))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = ds.sw(12.dp)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = { currentPage = ProfilePage.MyDevices },
                                modifier = Modifier.size(ds.sm(40.dp)),
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
                                        painter = painterResource(Res.drawable.`return`),
                                        contentDescription = "Back",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(ds.sm(16.dp)),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(ds.sh(32.dp)))
                        Text(
                            text = "选择要使用的设备",
                            fontSize = ds.sp(24f),
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF12192B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = ds.sw(18.dp)),
                        )

                        Spacer(modifier = Modifier.height(ds.sh(20.dp)))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = ds.sw(16.dp))
                                .verticalScroll(rememberScrollState()),
                        ) {
                            SwitchDeviceContent(
                                devices = switchDeviceList,
                                pendingCdi = pendingCdi,
                                currentCdi = switchDeviceCurrentCdi,
                                onDeviceClicked = { selectedDevice ->
                                    pendingCdi = selectedDevice.channelDeviceId
                                },
                            )
                        }

                        // ── 底部确认切换按钮 ──
                        val confirmEnabled = pendingCdi.isNotEmpty() &&
                            switchDeviceList.any { it.channelDeviceId == pendingCdi }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = ds.sw(18.dp), vertical = ds.sh(16.dp))
                                .height(ds.sh(48.dp))
                                .clip(RoundedCornerShape(ds.sm(100.dp)))
                                .background(
                                    if (confirmEnabled) Color(0xFF1F2535)
                                    else Color(0xFF1F2535).copy(alpha = 0.5f)
                                )
                                .clickable(enabled = confirmEnabled) {
                                    val selected = switchDeviceList.firstOrNull {
                                        it.channelDeviceId == pendingCdi
                                    }
                                    if (selected != null) {
                                        sdkSessionManager.selectDevice(pendingCdi)
                                        switchDeviceCurrentCdi = pendingCdi
                                        currentPage = ProfilePage.MyDevices
                                        println("[MyDevices] 确认切换设备: ${selected.name} cdi=$pendingCdi")
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "确认切换",
                                color = Color.White,
                                fontSize = ds.sp(16f),
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                ProfilePage.WifiConfig -> {
                    val ds = LocalDesignScale.current
                    ProfilePageContainer(showBackground = false, backgroundColor = Color(0xFFF3F3F3)) {
                        Spacer(modifier = Modifier.height(ds.sh(20.dp)))
                        ProfileTopBar(
                            title = "配置WI-FI",
                            showBack = true,
                            onBack = { currentPage = ProfilePage.MyDevices },
                            onClose = onDismiss
                        )
                        Spacer(modifier = Modifier.height(ds.sh(20.dp)))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = ds.sw(18.dp))
                                .verticalScroll(rememberScrollState()),
                        ) {
                            WifiConfigContent(
                                device = wifiConfigDevice,
                                onDismiss = { currentPage = ProfilePage.MyDevices }
                            )
                        }
                    }
                }

            }
        } // end AnimatedContent
        } // end HalfModalBottomSheet

        // 脑力值 / 充值页：全屏黑色页面。层叠在 HalfModalBottomSheet 之上，覆盖 sheet 的
        // 40% 黑色背板和 72dp 顶部留白，给用户一个从上到下真正 "满屏" 的视觉（符合设计稿）。
        // 进入：从 Settings 点"充值账户"时淡入；退出：返回 Settings / 进入 RechargePackage
        // 套餐页时淡出（套餐页继续沿用 sheet 中的浅色样式）。
        AnimatedVisibility(
            visible = isVisible && currentPage == ProfilePage.Recharge,
            enter = fadeIn(animationSpec = tween(durationMillis = 220)),
            exit = fadeOut(animationSpec = tween(durationMillis = 160)),
        ) {
            BrainPowerBalancePage(
                modifier = Modifier.fillMaxSize(),
                onBack = { currentPage = ProfilePage.Settings },
                onNavigateToPackage = { currentPage = ProfilePage.RechargePackage },
            )
        }

        // 套餐选择页：全屏黑色页面，覆盖在 sheet 之上。
        AnimatedVisibility(
            visible = isVisible && currentPage == ProfilePage.RechargePackage,
            enter = fadeIn(animationSpec = tween(durationMillis = 220)),
            exit = fadeOut(animationSpec = tween(durationMillis = 160)),
        ) {
            RechargePackagePage(
                modifier = Modifier.fillMaxSize(),
                onBack = { currentPage = ProfilePage.Recharge },
            )
        }

        AnimatedVisibility(
            visible = showLogoutDialog,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                        showLogoutDialog = false
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = showLogoutDialog,
                    enter = scaleIn(initialScale = 0.85f, animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                    exit = scaleOut(targetScale = 0.85f, animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)),
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
        }

        AnimatedVisibility(
            visible = showClearCacheDialog,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                        showClearCacheDialog = false
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = showClearCacheDialog,
                    enter = scaleIn(initialScale = 0.85f, animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                    exit = scaleOut(targetScale = 0.85f, animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)),
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
        }

        AnimatedVisibility(
            visible = showFeedbackSuccessDialog,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                        showFeedbackSuccessDialog = false
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = showFeedbackSuccessDialog,
                    enter = scaleIn(initialScale = 0.85f, animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                    exit = scaleOut(targetScale = 0.85f, animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)),
                ) {
                    FeedbackSuccessDialog(
                        onDismiss = {
                            showFeedbackSuccessDialog = false
                            currentPage = ProfilePage.Settings
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showDeleteAccountDialog,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                        showDeleteAccountDialog = false
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = showDeleteAccountDialog,
                    enter = scaleIn(initialScale = 0.85f, animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                    exit = scaleOut(targetScale = 0.85f, animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)),
                ) {
                    DeleteAccountConfirmDialog(
                        phone = userPhone,
                        email = userEmail,
                        authRepository = authRepository,
                        onDismiss = { showDeleteAccountDialog = false },
                        onSuccess = {
                            showDeleteAccountDialog = false
                            onLogout()
                        },
                    )
                }
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
    showBackground: Boolean = true,
    backgroundColor: Color = Color.White.copy(alpha = 0.80f),
    content: @Composable ColumnScope.() -> Unit,
) {
    val ds = LocalDesignScale.current
    val pageShape = RoundedCornerShape(topStart = ds.sm(32.dp), topEnd = ds.sm(32.dp))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                shape = pageShape
                clip = true
                shadowElevation = 0f
            }
            .background(backgroundColor)
    ) {
        if (showBackground) {
            Image(
                painter = painterResource(Res.drawable.account_bg),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                contentScale = ContentScale.FillWidth,
            )
        }
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
    showClose: Boolean = true,
) {
    val ds = LocalDesignScale.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ds.sw(20.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            IconButton(
                onClick = { onBack?.invoke() },
                modifier = Modifier.size(ds.sm(40.dp))
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
                        painter = painterResource(Res.drawable.`return`),
                        contentDescription = "Back",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(ds.sm(16.dp))
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.size(ds.sm(40.dp)))
        }

        if (title != null) {
            Text(
                text = title,
                fontSize = ds.sp(18f),
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

        if (showClose) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(ds.sm(40.dp))
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_modal_close),
                    contentDescription = "Close",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(ds.sm(32.dp))
                )
            }
        } else {
            Spacer(modifier = Modifier.size(ds.sm(40.dp)))
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
    val ds = LocalDesignScale.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onClick() }
            .padding(vertical = ds.sh(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.Unspecified,
            modifier = Modifier.size(ds.sm(20.dp))
        )

        Spacer(modifier = Modifier.width(ds.sw(12.dp)))

        Text(
            text = title,
            fontSize = ds.sp(14f),
            fontWeight = FontWeight.Normal,
            color = Color(0xFF12192B),
            modifier = Modifier.weight(1f),
        )

        if (showArrow) {
            Icon(
                imageVector = ChevronRightIcon,
                contentDescription = "Arrow",
                tint = Color.Unspecified,
                modifier = Modifier.size(width = ds.sw(12.dp), height = ds.sh(24.dp))
            )
        }
    }
}

/* ───────── Account detail page content ───────── */

@Composable
private fun AccountDetailContent(
    phone: String,
    email: String,
    onLogoutClick: () -> Unit = {},
) {
    val ds = LocalDesignScale.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ds.sh(20.dp))
    ) {
        Spacer(modifier = Modifier.height(ds.sh(8.dp)))

        Surface(
            shape = CircleShape,
            color = Color(0xFFE6E6E6),
            modifier = Modifier.size(ds.sm(80.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }

        Text(
            text = "点击更换头像",
            fontSize = ds.sp(14f),
            color = Color(0xFF888888)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(ds.sm(20.dp)),
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ds.sw(18.dp), vertical = ds.sh(14.dp)),
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

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "退出登录",
            fontSize = ds.sp(16f),
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF111111),
            modifier = Modifier
                .clickable { onLogoutClick() }
                .padding(vertical = ds.sh(16.dp))
        )

        Spacer(modifier = Modifier.height(ds.sh(24.dp)))
    }
}

/* ───────── Brain-power balance full-screen page ───────── */

/**
 * 脑力值 / 充值详情页（从"充值账户"入口打开）。由 AgentModelProfileScreen
 * 内部以 AnimatedVisibility 叠放在 HalfModalBottomSheet 之上，保证视觉上是全屏页面：
 * 纯黑背景、贴到系统状态栏 / 底部手势区外缘。
 *
 * 顶部返回 icon（32dp 圆，10% 白填充 + 0.06 白描边）+ 23dp 间隔 + 32sp 标题"脑力值"，
 * 27dp 间隔后是带渐变背景的余额大卡片（0.5dp #FF5800 描边、1%→10% 白纵向渐变、16dp 圆角），
 * 32dp 间隔后是"用量详情"小标题 + ⓘ，16dp 后是用量列表卡片。
 *
 * 分页加载：借 rememberScrollState 的 value 贴近 maxValue 时触发 loadMore；与旧版 LazyColumn
 * 的 layoutInfo 驱动方式等价但更适配 verticalScroll 里嵌套整段卡片的场景。
 */
@Composable
internal fun BrainPowerBalancePage(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onNavigateToPackage: () -> Unit,
) {
    val authRepository: AuthRepository = koinInject()
    val balanceWsManager: BalanceWsManager = koinInject()
    val balanceData by balanceWsManager.balance.collectAsState()
    val uriHandler = LocalUriHandler.current
    val isIos = remember { getPlatform().name.startsWith("iOS", ignoreCase = true) }
    val coroutineScope = rememberCoroutineScope()
    val ds = LocalDesignScale.current

    val paidBalance = balanceData.balances["1"] ?: 0L
    val freeBalance = balanceData.balances["4"] ?: 0L
    val totalBalance = paidBalance + freeBalance

    // 用量记录分页状态：
    //  - [records] 累积列表，随滚动不断 append；
    //  - [nextPage] 下一次要请求的页码（从 1 开始），请求成功后 +1；
    //  - [hasMore]  用 "本页长度 < pageSize" 作为 "已到末页" 的启发式判断；
    //  - [isLoading]/[loadError] 驱动 UI 显示 loading footer / 错误 footer。
    val records = remember { mutableStateListOf<ModelRecordItem>() }
    var nextPage by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // 触发下一页加载。**`isLoading=true` 必须同步写**（在 launch 外面），否则 LaunchedEffect(Unit)
    // 的 loadMore 和 LaunchedEffect(shouldLoadMore,...) 的 loadMore 可能在同一帧先后触发：
    // 第一次 load 把 `isLoading=true` 放在协程里等调度，LazyColumn 首次 layout 时 `shouldLoadMore`
    // 会瞬间为 true（records 还空、总项数只有 4），第二个 LaunchedEffect 检查到 `isLoading=false`
    // 直接放行，两次 loadMore 都以 pageIndex=1 发出去 → `records.addAll` 同一批两次
    // → LazyColumn `key` 冲突 → IllegalArgumentException 崩溃。
    //
    // 失败时 hasMore 仍保留为 true，方便用户点 "重试" footer；
    // 成功返回空页或不足一页时关掉 hasMore，UI 切到 "已加载全部" footer。
    val loadMore: () -> Unit = load@{
        if (isLoading || !hasMore) return@load
        isLoading = true
        loadError = null
        val pageIndex = nextPage
        coroutineScope.launch {
            try {
                val resp = authRepository.getModelRecords(
                    pageIndex = pageIndex,
                    pageSize = MODEL_RECORD_PAGE_SIZE,
                )
                val data = resp.data
                if (resp.code == 20000 && data != null) {
                    // 兜底去重：后端极端情况下可能因分页边界返回重叠行；LazyColumn key
                    // 用的是 record.id，重复 id 会直接抛 IllegalArgumentException。
                    val existingIds = records.mapNotNullTo(HashSet()) {
                        it.id.takeIf { id -> id.isNotBlank() }
                    }
                    val fresh = data.list.filter { it.id.isBlank() || it.id !in existingIds }
                    records.addAll(fresh)
                    nextPage = pageIndex + 1
                    hasMore = data.list.size >= MODEL_RECORD_PAGE_SIZE
                } else {
                    loadError = resp.msg.ifBlank { "加载失败" }
                }
            } finally {
                isLoading = false
            }
        }
    }

    // 首次进入：刷新余额 + 拉第一页。
    LaunchedEffect(Unit) {
        balanceWsManager.refreshBalance()
        loadMore()
    }

    // 分页触底检测：scrollState 绑定到 UsageRecordsCard 内部的 verticalScroll，
    // 页面其余部分（返回按钮、标题、余额卡片、用量详情标题）固定不动。
    // scroll.maxValue = 可滚距离，value = 当前偏移，
    // 剩余 < 240dp 时就提前预取下一页，避免用户滚到底看到 loading。
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState.value, scrollState.maxValue, hasMore, isLoading) {
        if (scrollState.maxValue > 0 &&
            scrollState.value >= scrollState.maxValue - 240 &&
            hasMore && !isLoading
        ) {
            loadMore()
        }
    }

    // 扣费规则弹窗开合状态。放在 Page 里而不是父级：关闭 sheet 时整段 Page 被 unmount，
    // 弹窗也就自动消失；不需要外层再额外持有 / 同步状态。
    var showRulesDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.background(Color.Black),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (showRulesDialog) Modifier.blur(ds.sm(2.dp)) else Modifier)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = ds.sw(18.dp)),
        ) {
            Spacer(modifier = Modifier.height(ds.sh(8.dp)))

            // ── 返回按钮：32dp 圆（10% 白填充 + 0.5dp 6% 白描边） ──
            Box(
                modifier = Modifier
                    .size(ds.sm(32.dp))
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.06f),
                        shape = CircleShape,
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    ) { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(ds.sm(20.dp)),
                )
            }

            Spacer(modifier = Modifier.height(ds.sh(23.dp)))

            // ── 标题"脑力值"：32sp / 600 / letterSpacing 2sp ──
            Text(
                text = "脑力值",
                fontSize = ds.sp(32f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = ds.sp(2f),
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(ds.sh(27.dp)))

            // ── 余额大卡片 ──
            BrainPowerBalanceCard(
                totalBalance = totalBalance,
                paidBalance = paidBalance,
                freeBalance = freeBalance,
                onRechargeClick = {
                    if (isIos) {
                        onNavigateToPackage()
                    } else {
                        uriHandler.openUri("https://cephalon.cloud")
                    }
                },
            )

            Spacer(modifier = Modifier.height(ds.sh(32.dp)))

            // ── "用量详情" 小标题 + ⓘ（点击打开脑力值扣费规则弹窗） ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "用量详情",
                    fontSize = ds.sp(16f),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(ds.sw(8.dp)))
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "脑力值扣费规则",
                    tint = Color.White.copy(alpha = 0.60f),
                    modifier = Modifier
                        .size(ds.sm(20.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        ) { showRulesDialog = true },
                )
            }

            Spacer(modifier = Modifier.height(ds.sh(16.dp)))

            // ── 用量列表卡片（records 全部内嵌；卡片内部滚动） ──
            UsageRecordsCard(
                modifier = Modifier.weight(1f),
                scrollState = scrollState,
                records = records,
                isLoading = isLoading,
                loadError = loadError,
                hasMore = hasMore,
                onRetry = { loadMore() },
            )
        }

        // 脑力值扣费规则弹窗：覆盖整屏（含状态栏），60% 黑色背板 + 点击背板关闭。
        // matchParentSize() 让 overlay Box 与外层 Box 等尺寸，因此状态栏也会被遮罩覆盖，
        // 视觉上与设计稿保持一致。
        if (showRulesDialog) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0x99000000))
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    ) { showRulesDialog = false },
                contentAlignment = Alignment.Center,
            ) {
                BrainPowerRulesDialog(onDismiss = { showRulesDialog = false })
            }
        }
    }
}

/* ───────── Brain-power balance card (dark) ───────── */

/**
 * 余额卡：padding 20dp、16dp 圆角、0.5dp #FF5800 描边、白色 1%→10% 纵向渐变。
 * 结构：
 *  - 上排：左侧「账户余额/脑力值」(10sp 60% 白) + 8dp 间隔 + 大数（32sp 白 600）；
 *    右侧「充值」按钮（100dp 圆角、1dp 6% 白描边、10% 白填充、16sp 白文字）。
 *  - 20dp 间隔 → 0.5dp 10% 白细分割线 → 16dp 间隔。
 *  - 下排：左「充值脑力值余额」(10sp 60% 白) + 4dp + 值（12sp 白）；
 *    右「免费脑力值余额」(10sp 60% 白) + 4dp + 值（12sp 白）。
 */
@Composable
private fun BrainPowerBalanceCard(
    totalBalance: Long,
    paidBalance: Long,
    freeBalance: Long,
    onRechargeClick: () -> Unit,
) {
    val ds = LocalDesignScale.current
    val cardShape = RoundedCornerShape(ds.sm(16.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.06f), // ⭐ 透明度
                shape = cardShape
            )
    ) {
        Image(
            painter = painterResource(Res.drawable.cep_bg),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.fillMaxWidth().padding(ds.sm(20.dp))) {
            // ── 顶部行：余额 + 充值按钮 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "账户余额/脑力值",
                        fontSize = ds.sp(10f),
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.60f),
                    )
                    Spacer(modifier = Modifier.height(ds.sh(8.dp)))
                    Text(
                        text = totalBalance.toString(),
                        fontSize = ds.sp(32f),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(ds.sw(12.dp)))

                Box(
                    modifier = Modifier
                        .width(ds.sw(96.dp))
                        .height(ds.sh(32.dp))
                        .glassButton(cornerRadius = ds.sm(100.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        ) { onRechargeClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "充值",
                        fontSize = ds.sp(16f),
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                    )
                }
            }

            Spacer(modifier = Modifier.height(ds.sh(20.dp)))

            // ── 0.5dp / 10% 白细分割线 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.White.copy(alpha = 0.10f)),
            )

            Spacer(modifier = Modifier.height(ds.sh(16.dp)))

            // ── 底部双列：充值脑力值余额 / 免费脑力值余额 ──
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "充值脑力值余额",
                        fontSize = ds.sp(10f),
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.60f),
                    )
                    Spacer(modifier = Modifier.height(ds.sh(4.dp)))
                    Text(
                        text = paidBalance.toString(),
                        fontSize = ds.sp(12f),
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.width(ds.sw(12.dp)))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "免费脑力值余额",
                        fontSize = ds.sp(10f),
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.60f),
                    )
                    Spacer(modifier = Modifier.height(ds.sh(4.dp)))
                    Text(
                        text = freeBalance.toString(),
                        fontSize = ds.sp(12f),
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/* ───────── Usage records card (dark) ───────── */

/**
 * 用量卡：padding 20dp、16dp 圆角、1dp 6% 白描边、10% 白填充。records 整串放在一个
 * Column 里，相邻两行之间用 0.5dp 10% 白分割线隔开。尾部 footer 互斥渲染
 * loading / error / empty / end 之一。
 */
@Composable
private fun UsageRecordsCard(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    records: List<ModelRecordItem>,
    isLoading: Boolean,
    loadError: String?,
    hasMore: Boolean,
    onRetry: () -> Unit,
) {
    val ds = LocalDesignScale.current
    val cardShape = RoundedCornerShape(ds.sm(16.dp))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = cardShape,
            )
            .padding(ds.sm(20.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
        ) {
            records.forEachIndexed { idx, record ->
                if (idx > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(Color.White.copy(alpha = 0.10f)),
                    )
                }
                DarkUsageRecordRow(
                    time = formatRecordTime(record.createdAt),
                    modelName = record.edges?.model?.name.orEmpty(),
                    amount = "-${record.inputCepCost + record.outputCepCost}",
                )
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ds.sh(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ds.sm(20.dp)),
                            strokeWidth = 2.dp,
                            color = Color.White.copy(alpha = 0.60f),
                        )
                    }
                }
                loadError != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ds.sh(12.dp)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = loadError,
                            fontSize = ds.sp(12f),
                            color = Color.White.copy(alpha = 0.60f),
                        )
                        TextButton(onClick = onRetry) {
                            Text(text = "点击重试", color = Color.White)
                        }
                    }
                }
                records.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ds.sh(24.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "暂无用量记录",
                            fontSize = ds.sp(12f),
                            color = Color.White.copy(alpha = 0.60f),
                        )
                    }
                }
                !hasMore -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ds.sh(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "已加载全部",
                            fontSize = ds.sp(12f),
                            color = Color.White.copy(alpha = 0.40f),
                        )
                    }
                }
            }
        }
    }
}

/* ───────── Usage record row (dark) ───────── */

/**
 * 单行用量记录：左侧模型名（14sp / 500 / 白）+ 2dp + 时间（12sp 60% 白）；
 * 右侧扣除额度（12sp 60% 白 右对齐）。
 */
@Composable
private fun DarkUsageRecordRow(
    time: String,
    modelName: String,
    amount: String,
) {
    val ds = LocalDesignScale.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ds.sh(14.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (modelName.isNotBlank()) {
                Text(
                    text = modelName,
                    fontSize = ds.sp(14f),
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (time.isNotBlank()) {
                Spacer(modifier = Modifier.height(ds.sh(2.dp)))
                Text(
                    text = time,
                    fontSize = ds.sp(12f),
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.60f),
                )
            }
        }
        Spacer(modifier = Modifier.width(ds.sw(12.dp)))
        Text(
            text = amount,
            fontSize = ds.sp(12f),
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.60f),
            textAlign = TextAlign.End,
        )
    }
}

/* ───────── Glass button modifier ───────── */

/**
 * 玻璃质感按钮修饰器，对应设计稿 CSS：
 *  - border: 1px solid #FFF
 *  - background: rgba(0, 0, 0, 0.05)
 *  - box-shadow: 0 4px 10px 0 rgba(0,0,0,0.20) inset,
 *                0 10px 66px 0 rgba(255,255,255,0.50) inset,
 *                0 15px 20px -10px rgba(0,0,0,0.30)
 *
 * 外阴影用 [shadow]，内阴影用 [drawWithContent] + 半透明垂直渐变近似。
 */
@Composable
private fun Modifier.glassButton(
    cornerRadius: Dp,
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .shadow(
            elevation = 10.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.30f),
            spotColor = Color.Black.copy(alpha = 0.30f),
        )
        .clip(shape)
        .background(Color.Black.copy(alpha = 0.05f))
        .border(width = 1.dp, color = Color.White, shape = shape)
        .drawWithContent {
            drawContent()
            // inset shadow 1: 0 4px 10px rgba(0,0,0,0.20) — 顶部暗压
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.20f),
                    0.5f to Color.Transparent,
                ),
            )
            // inset shadow 2: 0 10px 66px rgba(255,255,255,0.50) — 内发光
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.10f),
                    0.3f to Color.White.copy(alpha = 0.25f),
                    0.7f to Color.White.copy(alpha = 0.10f),
                    1f to Color.Transparent,
                ),
            )
        }
}

/* ───────── Brain-power billing rules dialog ───────── */

/**
 * 脑力值按模型扣费的静态规则。目前设计稿里直接硬编码这几条，如果后端有 /v1/billing/rules
 * 之类的接口，后续换成 Flow 即可，UI 结构不变。
 */
private data class BrainPowerRule(
    val modelName: String,
    val tokensDesc: String,
)

private val brainPowerRules: List<BrainPowerRule> = listOf(
    BrainPowerRule("kimi k2.5", "输入：3680/M Tokens  输出：19320/M Tokens"),
    BrainPowerRule("Kimi-K2-Instruct-INT4MIX", "输入：3200/M Tokens  输出：12800/M Tokens"),
    BrainPowerRule("DeepSeek R1", "输入：3200/M Tokens  输出：12800/M Tokens"),
    BrainPowerRule("QwQ-32B", "输入：800/M Tokens  输出：3200/M Tokens"),
    BrainPowerRule("Qwen2.5-72B-Instruct-AWQ", "输入：3304/M Tokens  输出：3304/M Tokens"),
)

/**
 * "脑力值扣费规则" 弹窗。样式完全按设计稿（375×812）落地，通过 [LocalDesignScale]
 * 做等比缩放：
 *  - 外框 padding 24dp×20dp（竖×横）、24dp 圆角、0.5dp 纯白描边、10% 白背景；
 *  - 标题"脑力值扣费规则" 20sp / 500；下方 20dp → 第一条模型；
 *  - 每条：模型名 16sp / 600 + 4dp + 描述 12sp / 60% 白 400；两条之间 16dp；
 *  - 最后一条之后 32dp → 居中 "知道了" 胶囊：200×48、100dp 圆角、1dp 6% 白描边、10% 白填充、16sp / 400 白字。
 *
 * 卡片整体再挂一个 no-op clickable，消耗卡内任意点击，避免穿透到覆盖层把弹窗关掉。
 */
@Composable
private fun BrainPowerRulesDialog(
    onDismiss: () -> Unit,
) {
    val ds = LocalDesignScale.current
    val cardShape = RoundedCornerShape(ds.sm(24.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ds.sw(20.dp))
            .clip(cardShape)
            .background(Color(0xFF1A1A1A))
            .border(width = 0.5.dp, color = Color.White, shape = cardShape)
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            ) { /* consume click so backdrop doesn't dismiss */ }
            .padding(
                horizontal = ds.sw(20.dp),
                vertical = ds.sh(24.dp),
            ),
    ) {
        Text(
            text = "脑力值扣费规则",
            fontSize = ds.sp(20f),
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(ds.sh(20.dp)))

        brainPowerRules.forEachIndexed { idx, rule ->
            if (idx > 0) Spacer(modifier = Modifier.height(ds.sh(16.dp)))
            Text(
                text = rule.modelName,
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(ds.sh(4.dp)))
            Text(
                text = rule.tokensDesc,
                fontSize = ds.sp(12f),
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.60f),
            )
        }

        Spacer(modifier = Modifier.height(ds.sh(32.dp)))

        // 居中 "知道了" 按钮
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(ds.sw(200.dp))
                .height(ds.sh(48.dp))
                .glassButton(cornerRadius = ds.sm(100.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                ) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "知道了",
                fontSize = ds.sp(16f),
                fontWeight = FontWeight.Normal,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// 用量记录一次 10 条，够覆盖 5~6 屏、又不会让首屏等太久。
private const val MODEL_RECORD_PAGE_SIZE = 10

/**
 * 把后端返回的 ISO 8601 时间字符串转成 `yyyy.MM.dd HH:mm:ss`。
 *
 * 典型输入：`"2026-04-19T15:52:46.508505+08:00"`、`"2026-04-19T15:52:46Z"`、
 * `"2026-04-19T15:52:46+08:00"`。策略是**纯字符串切片**（不做时区换算）：
 *  - 取 `T` 前 10 位作日期，把 `-` 换 `.` 得到 `"2026.04.19"`；
 *  - 取 `T` 后首个 `.`/`+`/`-`/`Z` 之前的部分作时间，得到 `"15:52:46"`；
 *  - 不符合这个格式就原样返回（不要在 UI 里把错误时间再"美化"掩盖解析问题）。
 * 之所以不过 kotlinx-datetime：后端时间戳自带 `+08:00`，对齐服务端显示即可，
 * 再做 `Instant.parse → toLocalDateTime(systemDefault)` 反而会把换到用户手机本地时区。
 */
private fun formatRecordTime(raw: String): String {
    if (raw.length < 19) return raw
    val tIndex = raw.indexOf('T')
    if (tIndex != 10) return raw
    val datePart = raw.substring(0, 10).replace('-', '.')
    val rest = raw.substring(11)
    val timeEnd = rest.indexOfFirst { it == '.' || it == '+' || it == 'Z' || it == '-' }
    val timePart = if (timeEnd in 1..rest.length) rest.substring(0, timeEnd) else rest
    return "$datePart $timePart"
}

private data class FixedPackage(
    val price: Double,
    val priceLabel: String,
    val base: Long,
    val tag: String? = null,
)

/** 残留 Apple 交易自动清理重试次数上限 */
private const val MAX_STALE_TX_RETRIES = 2

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

// 注：前端不再维护 price → appleProductId 的映射。所有 productId 以后端
// /orders/transfers 响应的 data.productId 为准；Swift 侧 purchase 时会按需一次性拉取。

private fun findGiftPercent(price: Double, rules: List<RechargeRuleItem>): Int {
    return rules.firstOrNull { price >= it.littleValue && price < it.largeValue }?.giftPercent ?: 0
}

/**
 * 充值卡片的点击编排：
 *  1. POST /v1/orders/transfers             → 拿 orderId + productId
 *  2. Apple 拉起支付                        → 拿 transactionId（仅 PurchaseOutcome.Success 进下一步）
 *  3. POST /v1/orders/apple/verify          → 把 transactionId 交给服务端
 *     - `code=20000`     → 入账成功，finishTransaction + 提示成功
 *     - `code=40003`     → 残留交易已绑旧订单，finish 后自动 retry Step 2
 *     - 其他失败         → 不 finish，保留 Apple 未完成交易给启动补偿
 *
 * **残留交易自动清理**：
 *  StoreKit 2 的 product.purchase() 在本地有"未完成交易"时不弹支付 sheet，直接返回该
 *  旧交易。如果该旧交易已被服务端绑到其他 order（40003）或金额不匹配（verified=false），
 *  Step 2-3 会自动 finish 掉残留交易并重试 purchase，最多 [MAX_STALE_TX_RETRIES] 次。
 *  用户无需多次手动点击。
 *
 * 所有 productId 以后端 `/v1/orders/transfers` 返回的 `data.productId` 为准；前端不再硬编码。
 * 返回 true 表示最终 succeed；false 表示失败/取消。
 */
private suspend fun handleRechargePackageClick(
    pkg: FixedPackage,
    authRepository: AuthRepository,
    iapManager: IAPManager,
    onResult: (String) -> Unit = {},
): Boolean {
    println("[IAP][UI] 点击充值卡片: tag=${pkg.tag ?: "none"}, priceLabel=${pkg.priceLabel}, amount=${pkg.price}, base=${pkg.base}")

    // ────────── Step 1: 创建订单 ──────────
    println("[IAP][UI] Step 1: POST /v1/orders/transfers, amount=${pkg.base}")
    val orderResponse = authRepository.createRechargeOrder(pkg.base)
    println("[IAP][API] createRechargeOrder 响应: code=${orderResponse.code}, msg=${orderResponse.msg}, data=${orderResponse.data}")
    if (orderResponse.code != 20000 || orderResponse.data == null) {
        val msg = orderResponse.msg.ifBlank { "创建订单失败" }
        println("[IAP][UI] Step 1 失败: $msg")
        onResult(msg)
        return false
    }
    val orderId = orderResponse.data.orderId

    val productId = orderResponse.data.productId
    if (orderId.isBlank() || productId.isBlank()) {
        println("[IAP][UI] Step 1 异常: 后端未返回 orderId/productId, data=${orderResponse.data}")
        onResult("订单创建异常")
        return false
    }

    // ────────── Step 2 + 3: 拉起 Apple 支付 → 服务端 verify ──────────
    var staleRetries = 0
    while (true) {
        // ── Step 2: 拉起 Apple 支付 ──
        println("[IAP][UI] Step 2: 调用 Apple 购买 (attempt ${staleRetries + 1}), orderId=$orderId, productId=$productId")
        val outcome = iapManager.initiatePurchase(productId)
        println("[IAP][IAP] initiatePurchase 返回: $outcome")
        val transactionId: String = when (outcome) {
            is PurchaseOutcome.Success -> outcome.transactionId
            is PurchaseOutcome.Cancelled -> {
                println("[IAP][UI] Step 2 结束: 用户主动取消 Apple 支付")
                onResult("已取消支付")
                return false
            }
            is PurchaseOutcome.Pending -> {
                println("[IAP][UI] Step 2 结束: Apple 返回 pending")
                onResult("支付审核中，请稍后查看")
                return false
            }
            is PurchaseOutcome.Failure -> {
                println("[IAP][UI] Step 2 失败: ${outcome.message}")
                onResult(outcome.message.ifBlank { "支付失败" })
                return false
            }
        }

        // ── Step 3: 把 Apple transactionId 报给服务端 ──
        println("[IAP][UI] Step 3: POST /v1/orders/apple/verify, transactionId=$transactionId, orderId=$orderId")
        val verifyResponse = authRepository.verifyAppleIAPTransaction(
            transactionId = transactionId,
            orderId = orderId,
        )
        println("[IAP][API] verifyAppleIAPTransaction 响应: code=${verifyResponse.code}, msg=${verifyResponse.msg}, data=${verifyResponse.data}")
        val verifyData = verifyResponse.data

        // ── 40003: 残留交易已绑到旧订单 → finish 后重试 ──
        if (verifyResponse.code == 40003) {
            println("[IAP][UI] Step 3 识别 40003：主动 finishTransaction=$transactionId 以释放 Apple 残留交易")
            runCatching { iapManager.finishTransaction(transactionId) }
                .onFailure { println("[IAP][UI] Step 3 finishTransaction 失败: ${it.message}") }
            if (staleRetries < MAX_STALE_TX_RETRIES) {
                staleRetries++
                println("[IAP][UI] Step 3 残留交易已清理，自动重试 purchase (retry $staleRetries/$MAX_STALE_TX_RETRIES)")
                continue
            }
            onResult("充值失败，请重试")
            return false
        }

        // ── 非 20000 / data==null ──
        if (verifyResponse.code != 20000 || verifyData == null) {
            val msg = verifyResponse.msg.ifBlank { "充值失败" }
            println("[IAP][UI] Step 3 失败（code=${verifyResponse.code}）: $msg")
            onResult(msg)
            return false
        }

        // ── 成功 ──
        println("[IAP][UI] Step 3 成功（code=20000）: orderId=$orderId, transactionId=$transactionId")
        iapManager.finishTransaction(transactionId)
        println("[IAP][UI] 流程完成: finishTransaction done, transactionId=$transactionId")
        onResult("充值成功")
        return true
    }
}

/**
 * 充值套餐选择 —— 全屏黑色页面（同 BrainPowerBalancePage 风格）。
 * 顶部 back + "选择套餐" 标题固定；下方套餐卡片列表 + 规则说明可滚动。
 */
@Composable
internal fun RechargePackagePage(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val ds = LocalDesignScale.current
    val authRepository: AuthRepository = koinInject()
    val iapManager: IAPManager = koinInject()
    val balanceWsManager: BalanceWsManager = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val toastState = rememberToastState()

    var rules by remember { mutableStateOf<List<RechargeRuleItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isPurchasing by remember { mutableStateOf(false) }
    var purchasingPkgPrice by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(Unit) {
        val response = authRepository.getRechargeRules()
        if (response.code == 20000 && response.data != null) {
            rules = response.data.sortedBy { it.littleValue }
        }
        isLoading = false
    }

    var resultMessage by remember { mutableStateOf("") }

    val onPackageClick: (FixedPackage) -> Unit = { pkg ->
        if (!isPurchasing) {
            coroutineScope.launch {
                purchasingPkgPrice = pkg.price
                isPurchasing = true
                try {
                    val ok = handleRechargePackageClick(
                        pkg = pkg,
                        authRepository = authRepository,
                        iapManager = iapManager,
                        onResult = { msg -> resultMessage = msg },
                    )
                    toastState.show(resultMessage)
                    if (ok) {
                        runCatching { balanceWsManager.refreshBalance() }
                    }
                } finally {
                    isPurchasing = false
                    purchasingPkgPrice = null
                    resultMessage = ""
                }
            }
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = ds.sw(18.dp)),
        ) {
            Spacer(modifier = Modifier.height(ds.sh(8.dp)))

            // ── 返回按钮 ──
            Box(
                modifier = Modifier
                    .size(ds.sm(32.dp))
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.06f),
                        shape = CircleShape,
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    ) { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(ds.sm(20.dp)),
                )
            }

            Spacer(modifier = Modifier.height(ds.sh(23.dp)))

            // ── 标题"选择套餐" ──
            Text(
                text = "选择套餐",
                fontSize = ds.sp(32f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = ds.sp(2f),
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(ds.sh(27.dp)))

            // ── 可滚动内容 ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ds.sh(200.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ds.sm(32.dp)),
                            strokeWidth = 2.dp,
                            color = Color.White.copy(alpha = 0.60f),
                        )
                    }
                } else {
                    // ── 前 3 张特色卡（体验 / 推荐 / 最佳价值）── 全宽，橙色边框
                    fixedPackages.take(3).forEachIndexed { idx, pkg ->
                        if (idx > 0) Spacer(modifier = Modifier.height(ds.sh(16.dp)))
                        val giftPercent = findGiftPercent(pkg.price, rules)
                        val gift = pkg.base * giftPercent / 100
                        val total = pkg.base + gift
                        DarkFeaturedPackageCard(
                            total = "$total",
                            base = pkg.base,
                            gift = gift,
                            price = pkg.priceLabel,
                            tag = pkg.tag,
                            isProcessing = purchasingPkgPrice == pkg.price,
                            enabled = purchasingPkgPrice == null,
                            onClick = { onPackageClick(pkg) },
                        )
                    }

                    Spacer(modifier = Modifier.height(ds.sh(16.dp)))

                    // ── 后续卡片：一排两个 ──
                    val gridItems = fixedPackages.drop(3)
                    for (i in gridItems.indices step 2) {
                        if (i > 0) Spacer(modifier = Modifier.height(ds.sh(16.dp)))
                        Row(
                            modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(ds.sw(15.dp)),
                        ) {
                            val pkg1 = gridItems[i]
                            val gp1 = findGiftPercent(pkg1.price, rules)
                            val gift1 = pkg1.base * gp1 / 100
                            DarkGridPackageCard(
                                total = "${pkg1.base + gift1}",
                                base = pkg1.base,
                                gift = gift1,
                                price = pkg1.priceLabel,
                                isProcessing = purchasingPkgPrice == pkg1.price,
                                enabled = purchasingPkgPrice == null,
                                onClick = { onPackageClick(pkg1) },
                                modifier = Modifier.weight(1f),
                            )
                            if (i + 1 < gridItems.size) {
                                val pkg2 = gridItems[i + 1]
                                val gp2 = findGiftPercent(pkg2.price, rules)
                                val gift2 = pkg2.base * gp2 / 100
                                DarkGridPackageCard(
                                    total = "${pkg2.base + gift2}",
                                    base = pkg2.base,
                                    gift = gift2,
                                    price = pkg2.priceLabel,
                                    isProcessing = purchasingPkgPrice == pkg2.price,
                                    enabled = purchasingPkgPrice == null,
                                    onClick = { onPackageClick(pkg2) },
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(ds.sh(32.dp)))

                    // ── 规则和信息 ──
                    Text(
                        text = "规则和信息",
                        fontSize = ds.sp(20f),
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(ds.sh(16.dp)))
                    val ruleTexts = listOf(
                        "当日免费脑力值会每天刷新，次日失效",
                        "活动脑力值可以通过参加活动获得，活动结束时过期",
                        "充值脑力值永不过期",
                        "赠送随力值计入活动脑力值",
                        "脑力值按以下顺序消耗：活动脑力值,当日免费脑力值，充值脑力值",
                    )
                    ruleTexts.forEach { txt ->
                        Text(
                            text = txt,
                            fontSize = ds.sp(14f),
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.60f),
                        )
                    }

                    Spacer(modifier = Modifier.height(ds.sh(40.dp)))
                }
            }
        }

        // ── 支付校验中 loading 遮罩（Apple 支付返回后才显示） ──
        if (isPurchasing) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.70f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    ) { /* 消费点击，防止穿透 */ },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ds.sm(40.dp)),
                    strokeWidth = 3.dp,
                    color = Color.White,
                )
            }
        }

        ToastHost(state = toastState, modifier = Modifier.align(Alignment.TopCenter))
    }
}

/* ───────── Featured package card (full-width, orange border) ───────── */

@Composable
private fun DarkFeaturedPackageCard(
    total: String,
    base: Long,
    gift: Long,
    price: String,
    tag: String? = null,
    isProcessing: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    val ds = LocalDesignScale.current
    val cardShape = RoundedCornerShape(ds.sm(16.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = cardShape,
            )
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            ) { onClick() },
    ) {
        Image(
            painter = painterResource(Res.drawable.tc_bg),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = ds.sw(20.dp),
                    end = ds.sw(20.dp),
                    top = ds.sh(20.dp),
                    bottom = ds.sh(20.dp),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧信息
            Column(modifier = Modifier.weight(1f)) {
                // tag 标签
                if (tag != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(ds.sm(10.dp)))
                            .background(Color(0xFFFF4D00))
                            .padding(horizontal = ds.sw(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tag,
                            fontSize = ds.sp(10f),
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.height(ds.sh(2.dp)))
                }
                // 数字 + 脑力值（同一行，基线对齐）
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = total,
                        fontSize = ds.sp(32f),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(modifier = Modifier.width(ds.sw(4.dp)))
                    Text(
                        text = "脑力值",
                        fontSize = ds.sp(12f),
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
                Spacer(modifier = Modifier.height(ds.sh(4.dp)))
                Text(
                    text = "基础 ${base}+${gift} 奖励",
                    fontSize = ds.sp(12f),
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.60f),
                )
            }

            // 右侧价格按钮
            DarkPriceButton(price = price, isProcessing = isProcessing, width = ds.sw(120.dp), height = ds.sh(40.dp))
        }
    }
}

/* ───────── Grid package card (half-width, white border) ───────── */

@Composable
private fun DarkGridPackageCard(
    total: String,
    base: Long,
    gift: Long,
    price: String,
    isProcessing: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val ds = LocalDesignScale.current
    val cardShape = RoundedCornerShape(ds.sm(16.dp))
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(cardShape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = cardShape,
            )
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            ) { onClick() }
            .padding(vertical = ds.sh(20.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 金额 + 脑力值（同一行）
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = total,
                fontSize = ds.sp(32f),
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = "脑力值",
                fontSize = ds.sp(8f),
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.60f),
                modifier = Modifier.padding(bottom = ds.sh(4.dp)),
            )
        }
        Spacer(modifier = Modifier.height(ds.sh(4.dp)))
        // 基础
        Text(
            text = "基础 ${base}+${gift} 奖励",
            fontSize = ds.sp(12f),
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.60f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.weight(1f).height(ds.sh(14.dp)))
        // 价格按钮
        DarkPriceButton(price = price, isProcessing = isProcessing, width = ds.sw(120.dp), height = ds.sh(40.dp))
    }
}

/* ───────── Dark price button (glass pill) ───────── */

@Composable
private fun DarkPriceButton(
    price: String,
    isProcessing: Boolean = false,
    width: Dp,
    height: Dp,
) {
    val ds = LocalDesignScale.current
    val buttonShape = RoundedCornerShape(ds.sm(80.dp))
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(buttonShape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = buttonShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isProcessing) {
            Text(
                text = "拉起支付中",
                fontSize = ds.sp(12f),
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                val priceParts = price.split("¥", "￥")
                val symbol = if (price.contains("¥")) "¥" else "￥"
                val number = priceParts.lastOrNull()?.trim() ?: price
                Text(
                    text = symbol,
                    fontSize = ds.sp(10f),
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = number,
                    fontSize = ds.sp(20f),
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
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
                .take(size - lastPickedFilesSize)
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
                .take(size - lastPickedImagesSize)
                .forEach { uri ->
                    if (uri.isNotBlank() && uri !in attachedImages) {
                        attachedImages.add(uri)
                    }
                }
        }
        lastPickedImagesSize = size
    }

    val ds = LocalDesignScale.current
    val labelColor = Color(0xFF1F2535)
    val asteriskColor = Color(0xFFE84026)
    val inputTextColor = Color(0xFF1F2535)
    val placeholderColor = Color(0xFFBBBBBB)
    val labelTextStyle = TextStyle(
        fontSize = ds.sp(16f),
        fontWeight = FontWeight.Medium,
        color = labelColor,
    )
    val inputTextStyle = TextStyle(
        fontSize = ds.sp(14f),
        fontWeight = FontWeight.Normal,
        color = inputTextColor,
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── 反馈标题 label ──
            Row {
                Text(text = "反馈标题", style = labelTextStyle)
                Text(
                    text = "*",
                    style = labelTextStyle.copy(color = asteriskColor),
                )
            }

            Spacer(modifier = Modifier.height(ds.sh(12.dp)))

            // ── 反馈标题 输入框 ──
            FeedbackInputBox {
                androidx.compose.foundation.text.BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = inputTextStyle,
                    singleLine = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(inputTextColor),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (title.isEmpty()) {
                                Text(
                                    text = "请输入反馈标题",
                                    style = inputTextStyle.copy(color = placeholderColor),
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(ds.sh(20.dp)))

            // ── 反馈描述 label ──
            Row {
                Text(text = "反馈描述", style = labelTextStyle)
                Text(
                    text = "*",
                    style = labelTextStyle.copy(color = asteriskColor),
                )
            }

            Spacer(modifier = Modifier.height(ds.sh(12.dp)))

            // ── 反馈描述 输入框（高度随内容增长） ──
            FeedbackInputBox {
                androidx.compose.foundation.text.BasicTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = inputTextStyle,
                    minLines = 3,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(inputTextColor),
                    decorationBox = { innerTextField ->
                        Box {
                            if (description.isEmpty()) {
                                Text(
                                    text = "请输入反馈描述",
                                    style = inputTextStyle.copy(color = placeholderColor),
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(ds.sh(20.dp)))

            // ── 添加附件 ──
            Row(
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { mediaController.openGallery() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ds.sw(8.dp)),
            ) {
                Icon(
                    imageVector = Icons.Filled.AttachFile,
                    contentDescription = null,
                    tint = Color(0xFF1A73E9),
                    modifier = Modifier.size(ds.sm(22.dp)),
                )
                Text(
                    text = "添加附件",
                    fontSize = ds.sp(16f),
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A73E9),
                )
            }

            // ── 已选图片预览 ──
            if (attachedImages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(ds.sh(12.dp)))
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ds.sw(8.dp)),
                    verticalArrangement = Arrangement.spacedBy(ds.sh(8.dp))
                ) {
                    attachedImages.forEach { uri ->
                        Box(modifier = Modifier.size(ds.sm(72.dp))) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(ds.sm(10.dp)),
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
                                    .padding(ds.sm(2.dp))
                                    .clip(CircleShape)
                                    .background(Color(0x99000000))
                                    .padding(horizontal = ds.sw(4.dp), vertical = ds.sh(1.dp))
                                    .clickable { attachedImages.remove(uri) }
                            )
                        }
                    }
                }
            }

            // ── 已选文件列表 ──
            if (attachedFiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(ds.sh(12.dp)))
                Column(verticalArrangement = Arrangement.spacedBy(ds.sh(8.dp))) {
                    attachedFiles.forEach { file ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(ds.sm(10.dp)),
                            color = Color(0xFFF5F5F5),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = ds.sw(12.dp), vertical = ds.sh(10.dp)),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(ds.sw(8.dp))
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

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(ds.sh(12.dp)))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE53935)
                )
            }

            Spacer(modifier = Modifier.height(ds.sh(16.dp)))
        }

        // ── 底部 创建工单 按钮 ──
        val canSubmit = title.isNotBlank() && description.isNotBlank() && !isSubmitting
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = ds.sh(16.dp))
                .height(ds.sh(48.dp))
                .clip(RoundedCornerShape(ds.sm(100.dp)))
                .background(
                    if (canSubmit) Color(0xFF1F2535)
                    else Color(0xFF1F2535).copy(alpha = 0.5f)
                )
                .clickable(enabled = canSubmit) {
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
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isSubmitting) "提交中..." else "创建工单",
                color = Color.White,
                fontSize = ds.sp(16f),
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/* 反馈表单输入框容器：白底、12dp 圆角、14dp 纵向 + 12dp 横向 padding、柔和阴影 */
@Composable
private fun FeedbackInputBox(content: @Composable () -> Unit) {
    val ds = LocalDesignScale.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(ds.sm(12.dp)),
                ambientColor = Color.Black.copy(alpha = 0.02f),
                spotColor = Color.Black.copy(alpha = 0.04f),
            )
            .clip(RoundedCornerShape(ds.sm(12.dp)))
            .background(Color.White)
            .padding(horizontal = ds.sw(12.dp), vertical = ds.sh(14.dp)),
    ) {
        content()
    }
}

@Composable
private fun FeedbackSuccessDialog(
    onDismiss: () -> Unit,
) {
    val ds = LocalDesignScale.current
    Surface(
        shape = RoundedCornerShape(ds.sm(20.dp)),
        color = Color.White,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ds.sw(20.dp), vertical = ds.sh(24.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ds.sh(16.dp))
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
                shape = RoundedCornerShape(ds.sm(12.dp)),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color(0xFF111111)
                )
            ) {
                Text("确定", color = Color.White)
            }
        }
    }
}

/* ───────── Delete Account Dialog ───────── */

@Composable
private fun DeleteAccountConfirmDialog(
    phone: String,
    email: String,
    authRepository: AuthRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    val ds = LocalDesignScale.current
    val scope = rememberCoroutineScope()
    val usePhone = phone.isNotEmpty()
    val account = if (usePhone) phone else email
    val way = if (usePhone) "phone_code" else "email_code"

    var code by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(ds.sm(24.dp)),
        color = Color.White,
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White),
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ds.sw(20.dp), vertical = ds.sh(24.dp)),
        ) {
            Text(
                text = "确认删除账户",
                fontSize = ds.sp(20f),
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2535),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(ds.sh(4.dp)))

            Text(
                text = "一旦你的账户被删除，所有数据将被永久移除且无法恢复",
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Normal,
                color = Color(0xFF717580),
            )
            Text(
                text = "你的订阅将在你的账户被删除时自动取消",
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Normal,
                color = Color(0xFF717580),
            )

            Spacer(modifier = Modifier.height(ds.sh(16.dp)))

            Text(
                text = "验证您的${if (usePhone) "手机号" else "邮箱"}：",
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Normal,
                color = Color(0xFF1F2535),
            )
            Text(
                text = account,
                fontSize = ds.sp(12f),
                fontWeight = FontWeight.Normal,
                color = Color(0xFF1F2535),
            )

            Spacer(modifier = Modifier.height(ds.sh(12.dp)))

            CodeInput(
                value = code,
                onValueChange = { code = it; errorMsg = "" },
                enabled = !isLoading,
                containerColor = Color.Black.copy(alpha = 0.05f),
                containerShadowElevation = 20.dp,
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
                Spacer(modifier = Modifier.height(ds.sh(4.dp)))
                Text(
                    text = errorMsg,
                    fontSize = ds.sp(12f),
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFFF4444),
                )
            }

            Spacer(modifier = Modifier.height(ds.sh(32.dp)))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ds.sw(11.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(ds.sh(48.dp))
                        .clip(RoundedCornerShape(ds.sm(100.dp)))
                        .border(1.dp, Color.White, RoundedCornerShape(ds.sm(100.dp)))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "取消",
                        fontSize = ds.sp(16f),
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF1F2535),
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(ds.sh(48.dp))
                        .clip(RoundedCornerShape(ds.sm(100.dp)))
                        .border(1.dp, Color.White, RoundedCornerShape(ds.sm(100.dp)))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) {
                            if (code.isBlank()) {
                                errorMsg = "请输入验证码"
                                return@clickable
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
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isLoading) "处理中..." else "清除",
                        fontSize = ds.sp(16f),
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFFE84026),
                    )
                }
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
    val ds = LocalDesignScale.current
    Surface(
        shape = RoundedCornerShape(ds.sm(20.dp)),
        color = Color.White,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { /* consume click */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ds.sw(20.dp), vertical = ds.sh(24.dp)),
        ) {
            Text(
                text = "清除缓存",
                fontSize = ds.sp(20f),
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2535),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(ds.sh(4.dp)))

            Text(
                text = "缓存数据有助于加快加载速度，清除可能会导致内容重新加载",
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Normal,
                color = Color(0xFF717580),
            )

            Spacer(modifier = Modifier.height(ds.sh(24.dp)))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ds.sw(11.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(ds.sm(100.dp)))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { onDismiss() }
                        .padding(vertical = ds.sh(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "取消",
                        fontSize = ds.sp(16f),
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF1F2535),
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(ds.sm(100.dp)))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { onConfirm() }
                        .padding(vertical = ds.sh(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "清除",
                        fontSize = ds.sp(16f),
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
    val ds = LocalDesignScale.current
    Surface(
        shape = RoundedCornerShape(ds.sm(20.dp)),
        color = Color.White,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { /* consume click */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ds.sw(20.dp), vertical = ds.sh(24.dp)),
        ) {
            Text(
                text = "退出登录",
                fontSize = ds.sp(20f),
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2535),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(ds.sh(4.dp)))

            Text(
                text = "确定要退出吗？退出登录不会丢失数据，您仍然可以再次登录此账号",
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Normal,
                color = Color(0xFF717580),
            )

            Spacer(modifier = Modifier.height(ds.sh(24.dp)))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ds.sw(11.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(ds.sm(100.dp)))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { onDismiss() }
                        .padding(vertical = ds.sh(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "取消",
                        fontSize = ds.sp(16f),
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF1F2535),
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(ds.sm(100.dp)))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { onConfirm() }
                        .padding(vertical = ds.sh(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "退出",
                        fontSize = ds.sp(16f),
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
    onSwitchDevice: (devices: List<com.cephalon.lucyApp.api.LucyDevice>, currentCdi: String) -> Unit = { _, _ -> },
) {
    val ds = LocalDesignScale.current
    val sdkSessionManager = koinInject<SdkSessionManager>()
    val authRepository = koinInject<AuthRepository>()
    val onlineCdis by sdkSessionManager.onlineDeviceCdis.collectAsState()
    val selectedCdi by sdkSessionManager.selectedDeviceCdi.collectAsState()
    val observerHasEmitted by sdkSessionManager.deviceObserverHasEmitted.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var backendDevices by remember { mutableStateOf<List<com.cephalon.lucyApp.api.LucyDevice>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        println("[MyDevices] 进入设备页, selectedCdi=$selectedCdi, onlineCdis=$onlineCdis")
        sdkSessionManager.ensureConnectedIfTokenValid()
        backendDevices = authRepository.getDevices()
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxWidth().height(ds.sh(200.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("加载中...", color = Color(0xFF999999))
        }
        return
    }

    if (backendDevices.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(ds.sh(200.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("暂无设备", color = Color(0xFF999999))
        }
        return
    }

    // 用户手动选择的设备优先，否则取列表第一个
    val currentDevice = selectedCdi?.let { sel -> backendDevices.firstOrNull { it.channelDeviceId == sel } }
        ?: backendDevices.first()
    val currentCdi = currentDevice.channelDeviceId
    val currentIsOnline = currentCdi.isNotEmpty() && currentCdi in onlineCdis

    DeviceCard(
        device = currentDevice,
        isOnline = currentIsOnline,
        isCheckingOnline = !observerHasEmitted,
        onSwitchDevice = {
            coroutineScope.launch {
                backendDevices = authRepository.getDevices()
                onSwitchDevice(backendDevices, currentCdi)
            }
        },
        onAddNewDevice = onAddNewDevice,
        onConfigureWifi = { onConfigureWifi(currentDevice) },
    )

    Spacer(modifier = Modifier.height(ds.sh(20.dp)))
}

private fun deviceTypeDisplayName(deviceType: String): String = when (deviceType) {
    "ai_npc" -> "AI NPC"
    "claw_pi" -> "龙虾派"
    "cloud" -> "端脑云"
    "claw_self" -> "其他"
    else -> ""
}

@Composable
private fun DeviceCard(
    device: com.cephalon.lucyApp.api.LucyDevice,
    isOnline: Boolean,
    isCheckingOnline: Boolean = false,
    onSwitchDevice: () -> Unit = {},
    onAddNewDevice: () -> Unit = {},
    onConfigureWifi: () -> Unit = {},
) {
    val ds = LocalDesignScale.current
    // 主标题显示设备 id（按设计稿要求，取 channelDeviceId → serialNumber → id 兜底）
    val deviceIdDisplay = device.channelDeviceId.ifBlank {
        device.serialNumber.ifBlank { device.id }
    }

    // 在首次 runPingAndEmit 结果到达前，展示 "检测中..." 中性灰色，避免从"离线"瞬间跳到"在线"
    val typeLabel = deviceTypeDisplayName(device.deviceType)
    val statusText = when {
        isCheckingOnline -> if (typeLabel.isNotEmpty()) "$typeLabel · 检测中…" else "检测中…"
        isOnline -> if (typeLabel.isNotEmpty()) "$typeLabel · 设备在线" else "设备在线"
        else -> if (typeLabel.isNotEmpty()) "$typeLabel · 设备离线" else "设备离线"
    }

    // ── 设备盒子 ──
    // padding 15/16/17、圆角 16、1dp 白边、阴影（无纯色底，透出容器背景）
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 15.dp,
                shape = RoundedCornerShape(ds.sm(16.dp)),
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.3f),
            )
            .clip(RoundedCornerShape(ds.sm(16.dp)))
            .background(Color(0xFFF3F3F3))
            .border(1.dp, Color.White, RoundedCornerShape(ds.sm(16.dp)))
            .padding(start = 16.dp, top = 15.dp, end = 16.dp, bottom = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 56x56 深色图标容器
        Box(
            modifier = Modifier
                .size(ds.sm(56.dp))
                .clip(RoundedCornerShape(ds.sm(16.dp)))
                .background(Color(0xFF1F2535)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_device_storage),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(width = ds.sm(26.dp), height = ds.sm(21.dp))
            )
        }

        // 设备 ID + 在线状态
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = deviceIdDisplay,
                fontSize = ds.sp(18f),
                fontWeight = FontWeight.Medium,
                color = Color(0xFF12192B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(ds.sh(4.dp)))
            Text(
                text = statusText,
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Normal,
                color = Color(0xFF595E6B),
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // ── 切换设备 + 添加新设备 ──
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

    Spacer(modifier = Modifier.height(ds.sh(12.dp)))

    // ── 配置 WIFI ──
    DeviceActionButton(
        text = "配置 WIFI",
        filled = false,
        modifier = Modifier.fillMaxWidth(),
        onClick = onConfigureWifi
    )
}

@Composable
private fun DeviceActionButton(
    text: String,
    filled: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val ds = LocalDesignScale.current
    val shape = RoundedCornerShape(ds.sm(100.dp))
    val bgBrush = if (filled) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF2A3244),
                Color(0xFF1F2535),
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFEBEBEB),
                Color(0xFFEBEBEB),
            )
        )
    }
    val textColor = if (filled) Color.White else Color(0xFF1F2535)

    Box(
        modifier = modifier
            .shadow(
                elevation = 15.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.3f),
            )
            .clip(shape)
            .background(bgBrush)
            .border(1.dp, Color.White, shape)
            .clickable { onClick() }
            .padding(vertical = ds.sh(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = ds.sp(16f),
            fontWeight = FontWeight.Normal,
            color = textColor
        )
    }
}

/* ───────── Switch Device Content ───────── */

@Composable
private fun SwitchDeviceContent(
    devices: List<com.cephalon.lucyApp.api.LucyDevice>,
    pendingCdi: String,
    currentCdi: String,
    onDeviceClicked: (com.cephalon.lucyApp.api.LucyDevice) -> Unit,
) {
    val ds = LocalDesignScale.current
    val sdkSessionManager = koinInject<SdkSessionManager>()
    val onlineCdis by sdkSessionManager.onlineDeviceCdis.collectAsState()
    val observerHasEmitted by sdkSessionManager.deviceObserverHasEmitted.collectAsState()
    val isCheckingOnline = !observerHasEmitted

    if (devices.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(ds.sh(120.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("暂无可用设备", color = Color(0xFF999999))
        }
    } else {
        devices.forEachIndexed { index, device ->
            val deviceCdi = device.channelDeviceId
            val isSelected = deviceCdi.isNotEmpty() && deviceCdi == pendingCdi
            val isCurrent = deviceCdi.isNotEmpty() && deviceCdi == currentCdi
            val isOnline = deviceCdi.isNotEmpty() && deviceCdi in onlineCdis
            SwitchDeviceItem(
                device = device,
                isSelected = isSelected,
                isCurrent = isCurrent,
                isOnline = isOnline,
                isCheckingOnline = isCheckingOnline,
                onClick = { onDeviceClicked(device) }
            )
            if (index != devices.lastIndex) {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/* ───────── Wifi Config Content ───────── */

/**
 * Wi‑Fi 配置页的阶段状态。
 */
private sealed interface WifiConfigState {
    /** 初始：正在读取本机 Wi‑Fi */
    data object Loading : WifiConfigState
    /** 本机 Wi‑Fi 与设备当前连接的 Wi‑Fi 一致 */
    data class SsidMatch(val ssid: String) : WifiConfigState
    /** 不一致：展示本机 SSID + 密码输入 */
    data class SsidMismatch(val phoneSsid: String, val deviceSsid: String?) : WifiConfigState
    /** 正在通过蓝牙配置 Wi‑Fi */
    data class Configuring(val phoneSsid: String) : WifiConfigState
    /** 配置成功 */
    data class Success(val ssid: String) : WifiConfigState
    /** 配置失败 */
    data class Error(val message: String, val phoneSsid: String) : WifiConfigState
    /** 无法获取本机 Wi‑Fi */
    data object PhoneWifiUnavailable : WifiConfigState
    /** BLE 连接断开 */
    data object BleDisconnected : WifiConfigState
}

@Composable
private fun WifiConfigContent(
    device: com.cephalon.lucyApp.api.LucyDevice?,
    onDismiss: () -> Unit,
) {
    if (device == null) return

    val ds = LocalDesignScale.current
    val controller = com.cephalon.lucyApp.brainbox.rememberBrainBoxProvisionController()
    val provisionManager = com.cephalon.lucyApp.deviceaccess.gatt.rememberProvisionManager()
    val wifiCredentialCache = koinInject<com.cephalon.lucyApp.brainbox.WifiCredentialCache>()
    val scope = rememberCoroutineScope()

    var configState by remember { mutableStateOf<WifiConfigState>(WifiConfigState.Loading) }
    var wifiPassword by remember { mutableStateOf("") }
    var deviceNetworkStatus by remember { mutableStateOf<com.cephalon.lucyApp.deviceaccess.gatt.NetworkStatusPayload?>(null) }

    // Android 专用：BLE 断开 → 立即停止所有操作（iOS Core Bluetooth 自行管理，不干预）
    val isAndroid = remember { getPlatform().name.startsWith("Android", ignoreCase = true) }
    // 跟踪当前 BLE 操作的 Job，断开时立即 cancel（仅 Android）
    var bleOperationJob by remember { mutableStateOf<Job?>(null) }
    // 用于重试时重新触发初始化 LaunchedEffect
    var retryKey by remember { mutableStateOf(0) }

    // ── 初始化：读本机 Wi‑Fi 并和设备对比 ──
    LaunchedEffect(device.id, retryKey) {
        configState = WifiConfigState.Loading
        when (val phoneWifi = controller.readCurrentPhoneWifi()) {
            is com.cephalon.lucyApp.brainbox.PhoneWifiState.Connected -> {
                val phoneSsid = phoneWifi.ssid
                // 尝试 BLE 读设备当前 Wi‑Fi
                val ns = readDeviceNetworkStatus(provisionManager, device)
                if (isAndroid) coroutineContext.ensureActive()
                deviceNetworkStatus = ns
                val deviceSsid = ns?.ssid?.trim()?.takeIf { it.isNotBlank() }
                if (deviceSsid != null && deviceSsid.equals(phoneSsid, ignoreCase = true)) {
                    configState = WifiConfigState.SsidMatch(phoneSsid)
                } else {
                    // 预填缓存密码
                    val cached = wifiCredentialCache.get(phoneSsid)
                    if (cached != null) wifiPassword = cached
                    configState = WifiConfigState.SsidMismatch(phoneSsid, deviceSsid)
                }
            }
            is com.cephalon.lucyApp.brainbox.PhoneWifiState.Disabled -> {
                configState = WifiConfigState.PhoneWifiUnavailable
            }
            is com.cephalon.lucyApp.brainbox.PhoneWifiState.Unknown -> {
                configState = WifiConfigState.PhoneWifiUnavailable
            }
        }
    }

    // ── Android 专用：BLE 断开监听，一旦检测到断开立即取消所有操作 ──
    if (isAndroid) {
        LaunchedEffect(provisionManager) {
            provisionManager.disconnectEvents.collect { reason ->
                println("[WifiConfig] BLE 断开事件: $reason")
                bleOperationJob?.cancel()
                bleOperationJob = null
                provisionManager.stopScan()
                provisionManager.cancel()
                val current = configState
                if (current is WifiConfigState.Loading || current is WifiConfigState.Configuring) {
                    configState = WifiConfigState.BleDisconnected
                }
            }
        }

        // 监听 ProvisionManager stage 变为 Reconnecting/Failed
        val provisionState by provisionManager.state.collectAsState()
        LaunchedEffect(provisionState.stage) {
            val stage = provisionState.stage
            if (stage == com.cephalon.lucyApp.deviceaccess.gatt.ProvisionFlowStage.Reconnecting ||
                stage == com.cephalon.lucyApp.deviceaccess.gatt.ProvisionFlowStage.Failed
            ) {
                val current = configState
                if (current is WifiConfigState.Loading || current is WifiConfigState.Configuring) {
                    println("[WifiConfig] ProvisionManager stage=$stage，取消 BLE 操作")
                    bleOperationJob?.cancel()
                    bleOperationJob = null
                    provisionManager.stopScan()
                    provisionManager.cancel()
                    configState = WifiConfigState.BleDisconnected
                }
            }
        }
    }

    // ── 清理 ──
    DisposableEffect(Unit) {
        onDispose {
            if (isAndroid) {
                bleOperationJob?.cancel()
                bleOperationJob = null
                CoroutineScope(Dispatchers.Default).launch { provisionManager.cancel() }
            }
            provisionManager.stopScan()
        }
    }

    // ── 设备信息卡片（始终显示） ──
    val deviceIdDisplay = device.channelDeviceId.ifBlank { device.id }
    val ns = deviceNetworkStatus
    val wifiSsid = ns?.ssid?.trim()?.takeIf { it.isNotBlank() }
    val wifiIp = ns?.ip?.trim()?.takeIf { it.isNotBlank() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ds.sm(16.dp)),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ds.sm(14.dp)),
            verticalArrangement = Arrangement.spacedBy(ds.sh(8.dp)),
        ) {
            // 设备 ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "设备 ID",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF999999),
                    modifier = Modifier.width(ds.sw(56.dp)),
                )
                Text(
                    text = deviceIdDisplay,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF111111),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            // Wi-Fi
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Wi‑Fi",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF999999),
                    modifier = Modifier.width(ds.sw(56.dp)),
                )
                Text(
                    text = wifiSsid ?: if (ns == null) "读取中…" else "未连接",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (wifiSsid != null) Color(0xFF111111) else Color(0xFFBBBBBB),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            // IP
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "IP",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF999999),
                    modifier = Modifier.width(ds.sw(56.dp)),
                )
                Text(
                    text = wifiIp ?: if (ns == null) "读取中…" else "—",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (wifiIp != null) Color(0xFF111111) else Color(0xFFBBBBBB),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(ds.sh(20.dp)))

    // ── 根据状态展示不同内容 ──
    when (val state = configState) {
        is WifiConfigState.Loading -> {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = ds.sh(40.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color(0xFF1F2535),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(ds.sm(24.dp)),
                    )
                    Spacer(modifier = Modifier.height(ds.sh(12.dp)))
                    Text(
                        text = "正在检测 Wi‑Fi 状态…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF999999),
                    )
                }
            }
        }

        is WifiConfigState.SsidMatch -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ds.sm(16.dp)),
                color = Color(0xFFE8F5E9),
            ) {
                Column(modifier = Modifier.padding(ds.sm(16.dp))) {
                    Text(
                        text = "Wi‑Fi 配置一致",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF34C759),
                    )
                    Spacer(modifier = Modifier.height(ds.sh(8.dp)))
                    Text(
                        text = "设备当前连接的 Wi‑Fi「${state.ssid}」与本机一致，无需重新配置。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF333333),
                    )
                }
            }
        }

        is WifiConfigState.SsidMismatch -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ds.sm(16.dp)),
                color = Color(0xFFFFF8E1),
            ) {
                Column(modifier = Modifier.padding(ds.sm(16.dp))) {
                    Text(
                        text = "Wi‑Fi 不一致",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFFF9800),
                    )
                    Spacer(modifier = Modifier.height(ds.sh(8.dp)))
                    Text(
                        text = buildString {
                            append("本机已连接「${state.phoneSsid}」")
                            if (!state.deviceSsid.isNullOrBlank()) {
                                append("，设备当前连接「${state.deviceSsid}」")
                            }
                            append("。\n可将设备切换到本机所在的 Wi‑Fi 网络。")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF333333),
                    )
                }
            }

            Spacer(modifier = Modifier.height(ds.sh(16.dp)))

            // 本机 Wi‑Fi 名称展示
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ds.sm(16.dp)),
                color = Color.White,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ds.sm(14.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "当前手机 Wi‑Fi",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF999999),
                        )
                        Spacer(modifier = Modifier.height(ds.sh(4.dp)))
                        Text(
                            text = state.phoneSsid,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF111111),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(ds.sh(12.dp)))

            // 密码输入
            androidx.compose.material3.OutlinedTextField(
                value = wifiPassword,
                onValueChange = { wifiPassword = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Wi‑Fi 密码") },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                ),
            )

            Spacer(modifier = Modifier.height(ds.sh(20.dp)))

            // 配置按钮
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val ssid = state.phoneSsid
                        val pwd = wifiPassword
                        configState = WifiConfigState.Configuring(ssid)
                        if (isAndroid) bleOperationJob?.cancel()
                        val job = scope.launch {
                            configureDeviceWifi(
                                provisionManager = provisionManager,
                                wifiCredentialCache = wifiCredentialCache,
                                device = device,
                                ssid = ssid,
                                password = pwd,
                                onState = { configState = it },
                                onNetworkStatus = { deviceNetworkStatus = it },
                            )
                        }
                        if (isAndroid) bleOperationJob = job
                    },
                shape = RoundedCornerShape(ds.sm(14.dp)),
                color = Color(0xFF1F2535),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ds.sh(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "配置 Wi‑Fi",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.White,
                    )
                }
            }
        }

        is WifiConfigState.Configuring -> {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = ds.sh(40.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color(0xFF1F2535),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(ds.sm(24.dp)),
                    )
                    Spacer(modifier = Modifier.height(ds.sh(12.dp)))
                    Text(
                        text = "正在为设备配置 Wi‑Fi「${state.phoneSsid}」…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF999999),
                    )
                    Spacer(modifier = Modifier.height(ds.sh(4.dp)))
                    Text(
                        text = "正在搜索蓝牙设备并连接，请保持设备通电",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFBBBBBB),
                    )
                }
            }
        }

        is WifiConfigState.Success -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ds.sm(16.dp)),
                color = Color(0xFFE8F5E9),
            ) {
                Column(modifier = Modifier.padding(ds.sm(16.dp))) {
                    Text(
                        text = "配置成功",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF34C759),
                    )
                    Spacer(modifier = Modifier.height(ds.sh(8.dp)))
                    Text(
                        text = "设备已成功连接到 Wi‑Fi「${state.ssid}」，与本机网络一致。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF333333),
                    )
                }
            }

            Spacer(modifier = Modifier.height(ds.sh(20.dp)))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss() },
                shape = RoundedCornerShape(ds.sm(14.dp)),
                color = Color(0xFF1F2535),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ds.sh(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "完成",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.White,
                    )
                }
            }
        }

        is WifiConfigState.Error -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ds.sm(16.dp)),
                color = Color(0xFFFFEBEE),
            ) {
                Column(modifier = Modifier.padding(ds.sm(16.dp))) {
                    Text(
                        text = "配置失败",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFE84026),
                    )
                    Spacer(modifier = Modifier.height(ds.sh(8.dp)))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF333333),
                    )
                }
            }

            Spacer(modifier = Modifier.height(ds.sh(20.dp)))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // 重试：回到 Mismatch 状态
                        configState = WifiConfigState.SsidMismatch(state.phoneSsid, null)
                    },
                shape = RoundedCornerShape(ds.sm(14.dp)),
                color = Color(0xFF1F2535),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ds.sh(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "重试",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.White,
                    )
                }
            }
        }

        is WifiConfigState.PhoneWifiUnavailable -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ds.sm(16.dp)),
                color = Color(0xFFFFF8E1),
            ) {
                Column(modifier = Modifier.padding(ds.sm(16.dp))) {
                    Text(
                        text = "无法获取本机 Wi‑Fi",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFFF9800),
                    )
                    Spacer(modifier = Modifier.height(ds.sh(8.dp)))
                    Text(
                        text = "请确认手机已开启 Wi‑Fi 并连接到目标网络，然后重新打开此页面。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF333333),
                    )
                }
            }
        }

        is WifiConfigState.BleDisconnected -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ds.sm(16.dp)),
                color = Color(0xFFFFEBEE),
            ) {
                Column(modifier = Modifier.padding(ds.sm(16.dp))) {
                    Text(
                        text = "蓝牙连接已断开",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFE84026),
                    )
                    Spacer(modifier = Modifier.height(ds.sh(8.dp)))
                    Text(
                        text = "与设备的蓝牙连接已中断，所有操作已停止。请确认设备已通电且在附近，然后重试。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF333333),
                    )
                }
            }

            Spacer(modifier = Modifier.height(ds.sh(20.dp)))

            // 重试按钮
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // 重新进入 Loading 状态，递增 retryKey 触发 LaunchedEffect 重新初始化
                        configState = WifiConfigState.Loading
                        retryKey++
                    },
                shape = RoundedCornerShape(ds.sm(14.dp)),
                color = Color(0xFF1F2535),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ds.sh(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "重试",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

/** 持续扫描 + probe 的总超时 */
private const val SCAN_AND_MATCH_TIMEOUT_MS = 60_000L
/** 每轮扫描列表不变时的等待间隔 */
private const val SCAN_POLL_INTERVAL_MS = 2_000L

/**
 * 持续 BLE 扫描 + probe，直到找到 channelDeviceId == [targetCdi] 的设备或超时。
 *
 * 匹配策略（每当扫描列表新增设备时重新执行）：
 * 1. BLE name 包含 serialNumber 或 device.name → 命中。
 * 2. 逐台 probe 读 pairing_info.channelDeviceId == targetCdi → 命中（最终确认方式）。
 *
 * 返回 null 表示超时仍未匹配到。
 */
private suspend fun scanAndMatchDeviceByCdi(
    provisionManager: com.cephalon.lucyApp.deviceaccess.gatt.ProvisionManager,
    device: com.cephalon.lucyApp.api.LucyDevice,
): com.cephalon.lucyApp.deviceaccess.BleScanDevice? {
    val targetCdi = device.channelDeviceId
    val serial = device.serialNumber.trim().takeIf { it.isNotBlank() && it != "unknown" }
    val deviceName = device.name.trim().takeIf { it.isNotBlank() && it != "默认设备名称" }

    println("[WifiConfig] scanAndMatchDeviceByCdi: 目标 cdi=$targetCdi, serial=$serial, name=$deviceName")

    provisionManager.startScan()

    val probedIds = mutableSetOf<String>() // 已经 probe 过的 BLE device id
    val startTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

    while (kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - startTime < SCAN_AND_MATCH_TIMEOUT_MS) {
        coroutineContext.ensureActive()
        val allDevices = provisionManager.scanState.value.devices
        if (allDevices.isEmpty()) {
            kotlinx.coroutines.delay(SCAN_POLL_INTERVAL_MS)
            continue
        }

        println("[WifiConfig] 当前扫描到 ${allDevices.size} 台: ${allDevices.map { "${it.name}(${it.id})" }}")

        // 策略 1：按名称匹配（BLE name 包含 serial 或 device.name）
        val nameMatch = allDevices.firstOrNull { ble ->
            (serial != null && ble.name.contains(serial, ignoreCase = true)) ||
                (deviceName != null && ble.name.contains(deviceName, ignoreCase = true))
        }
        if (nameMatch != null) {
            println("[WifiConfig] 按名称匹配到设备: ${nameMatch.name}")
            return nameMatch
        }

        // 策略 2：逐台 probe 未探测过的设备，用 channelDeviceId 确认
        val newDevices = allDevices.filter { it.id !in probedIds }
        for (candidate in newDevices) {
            coroutineContext.ensureActive()
            probedIds.add(candidate.id)
            val probeResult = provisionManager.probeDevice(candidate).getOrNull()
            println("[WifiConfig] probe ${candidate.name}(${candidate.id}): cdi=${probeResult?.channelDeviceId}")
            if (probeResult != null && probeResult.channelDeviceId.equals(targetCdi, ignoreCase = true)) {
                println("[WifiConfig] ✓ 匹配到目标设备: ${candidate.name}")
                return candidate
            }
        }

        // 所有已知设备都 probe 过且没命中 → 等新设备出现
        println("[WifiConfig] 已探测 ${probedIds.size} 台均未命中，等待更多设备...")
        kotlinx.coroutines.delay(SCAN_POLL_INTERVAL_MS)
    }

    println("[WifiConfig] ${SCAN_AND_MATCH_TIMEOUT_MS}ms 超时，未匹配到目标设备")
    return null
}

/**
 * BLE 扫描 → 连接 → 读 network_status，返回完整的 NetworkStatusPayload（含 ssid + ip）。
 * 超时/失败返回 null。
 */
private suspend fun readDeviceNetworkStatus(
    provisionManager: com.cephalon.lucyApp.deviceaccess.gatt.ProvisionManager,
    device: com.cephalon.lucyApp.api.LucyDevice,
): com.cephalon.lucyApp.deviceaccess.gatt.NetworkStatusPayload? {
    println("[WifiConfig] readDeviceNetworkStatus: 开始扫描, name=${device.name}, serial=${device.serialNumber}, cdi=${device.channelDeviceId}")
    return try {
        coroutineContext.ensureActive()
        val target = scanAndMatchDeviceByCdi(provisionManager, device)
        coroutineContext.ensureActive()
        if (target == null) {
            println("[WifiConfig] readDeviceNetworkStatus: 未匹配到目标设备")
            return null
        }

        println("[WifiConfig] 连接目标设备: ${target.name} (${target.id})")
        provisionManager.connectDevice(target).getOrThrow()
        val networkStatus = provisionManager.state.value.networkStatus
        println("[WifiConfig] 设备当前 SSID: ${networkStatus?.ssid}, IP: ${networkStatus?.ip}")
        networkStatus
    } catch (e: CancellationException) {
        println("[WifiConfig] readDeviceNetworkStatus cancelled (BLE 断开)")
        throw e
    } catch (e: Exception) {
        println("[WifiConfig] readDeviceNetworkStatus failed: ${e.message}")
        null
    } finally {
        provisionManager.stopScan()
    }
}

/**
 * 完整 BLE 配网流程：扫描 → 连接 → configureWifi → 验证 → 回调状态。
 */
private suspend fun configureDeviceWifi(
    provisionManager: com.cephalon.lucyApp.deviceaccess.gatt.ProvisionManager,
    wifiCredentialCache: com.cephalon.lucyApp.brainbox.WifiCredentialCache,
    device: com.cephalon.lucyApp.api.LucyDevice,
    ssid: String,
    password: String,
    onState: (WifiConfigState) -> Unit,
    onNetworkStatus: (com.cephalon.lucyApp.deviceaccess.gatt.NetworkStatusPayload) -> Unit = {},
) {
    try {
        coroutineContext.ensureActive()
        // 如果 provisionManager 当前没有已连接的设备，需要重新扫描连接
        val currentDevice = provisionManager.state.value.selectedDevice
        if (currentDevice == null) {
            println("[WifiConfig] configureDeviceWifi: 无已连接设备，开始扫描匹配")
            val target = scanAndMatchDeviceByCdi(provisionManager, device)
            coroutineContext.ensureActive()
            if (target == null) {
                onState(WifiConfigState.Error("未找到目标设备蓝牙信号，请确认设备已通电且在附近", ssid))
                return
            }
            println("[WifiConfig] configureDeviceWifi: 连接 ${target.name}")
            provisionManager.connectDevice(target).getOrElse {
                onState(WifiConfigState.Error("蓝牙连接失败: ${it.message}", ssid))
                return
            }
        }

        coroutineContext.ensureActive()
        // 下发 Wi‑Fi 配置
        val result = provisionManager.configureWifi(ssid = ssid, password = password)
        coroutineContext.ensureActive()
        result.onSuccess { ns ->
            onNetworkStatus(ns)
            val deviceSsid = ns.ssid.trim().takeIf { it.isNotBlank() }
            // 必须确认 network_status 返回的 SSID 与目标 SSID 一致才算成功
            if (deviceSsid != null && deviceSsid.equals(ssid, ignoreCase = true)) {
                if (password.isNotBlank()) {
                    wifiCredentialCache.save(ssid, password)
                }
                onState(WifiConfigState.Success(ssid))
            } else {
                val hint = if (deviceSsid != null) {
                    "设备当前连接的是「$deviceSsid」而非目标「$ssid」，网络未切换成功"
                } else {
                    "设备未返回有效的 Wi‑Fi 名称，网络切换可能未生效"
                }
                println("[WifiConfig] SSID 不匹配: target=$ssid, actual=$deviceSsid")
                onState(WifiConfigState.Error(hint, ssid))
            }
        }.onFailure { error ->
            onState(WifiConfigState.Error(
                error.message ?: "Wi‑Fi 配置失败，请检查密码是否正确",
                ssid,
            ))
        }
    } catch (e: CancellationException) {
        // BLE 断开导致 Job 被 cancel，不回调 onState（由断开监听器处理）
        println("[WifiConfig] configureDeviceWifi cancelled (BLE 断开)")
        throw e
    } catch (e: Exception) {
        onState(WifiConfigState.Error(e.message ?: "配置过程异常", ssid))
    } finally {
        provisionManager.stopScan()
    }
}

@Composable
private fun SwitchDeviceItem(
    device: com.cephalon.lucyApp.api.LucyDevice,
    isSelected: Boolean,
    isCurrent: Boolean,
    isOnline: Boolean,
    isCheckingOnline: Boolean = false,
    onClick: () -> Unit,
) {
    val ds = LocalDesignScale.current
    // 主标题显示设备 id（channelDeviceId → serialNumber → id 兜底）
    val deviceIdDisplay = device.channelDeviceId.ifBlank {
        device.serialNumber.ifBlank { device.id }
    }

    val shape = RoundedCornerShape(ds.sm(16.dp))
    // 选中：深色 #1F2535；默认：浅色半透明白（页面底色为白，故用轻微灰色呈现玻璃质感）
    val cardBgBrush = if (isSelected) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF1F2535),
                Color(0xFF1F2535),
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFF8F8F8),
                Color(0xFFF0F1F5),
            )
        )
    }
    val idColor = if (isSelected) Color.White else Color(0xFF12192B)
    val statusColor = if (isSelected) Color.White.copy(alpha = 0.70f) else Color(0xFF595E6B)
    // 状态文本：在 observer 尚未 emit 时显示"检测中..."，否则按在线/离线
    val typeLabel = deviceTypeDisplayName(device.deviceType)
    val statusText = when {
        isCheckingOnline -> if (typeLabel.isNotEmpty()) "$typeLabel · 检测中…" else "检测中…"
        isOnline -> if (typeLabel.isNotEmpty()) "$typeLabel · 设备在线" else "设备在线"
        else -> if (typeLabel.isNotEmpty()) "$typeLabel · 设备离线" else "设备离线"
    }
    // 选中态图标盒变为白色，图标 tint 变为深色 #1F2535（与设计稿 SVG fill 一致）
    val iconBoxColor = if (isSelected) Color.White else Color(0xFF1F2535)
    val iconTint = if (isSelected) Color(0xFF1F2535) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 15.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.3f),
            )
            .clip(shape)
            .background(cardBgBrush)
            .border(1.dp, Color.White, shape)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 56x56 图标容器（选中态为白色，默认为深色）
        Box(
            modifier = Modifier
                .size(ds.sm(56.dp))
                .clip(RoundedCornerShape(ds.sm(16.dp)))
                .background(iconBoxColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_device_storage),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(width = ds.sm(26.dp), height = ds.sm(21.dp)),
            )
        }

        // 设备 ID + 在线状态
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(ds.sm(8.dp))
                            .background(Color(0xFF19D166), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = deviceIdDisplay,
                    fontSize = ds.sp(18f),
                    fontWeight = FontWeight.Medium,
                    color = idColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusText,
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Normal,
                color = statusColor,
            )
        }
    }
}

/* ───────── Platform cache clearing ───────── */

expect fun clearAppCache()
expect fun getAppCacheSize(): Long

/* ───────── Account detail helpers ───────── */

@Composable
private fun AccountInfoRow(label: String, value: String) {
    val ds = LocalDesignScale.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ds.sh(14.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = ds.sp(14f),
            color = Color(0xFF666666),
            modifier = Modifier.width(ds.sw(72.dp))
        )
        Text(
            text = value,
            fontSize = ds.sp(14f),
            fontWeight = FontWeight.Medium,
            color = Color(0xFF111111),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}  
