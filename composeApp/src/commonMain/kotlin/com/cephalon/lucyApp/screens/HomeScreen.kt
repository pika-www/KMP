package com.cephalon.lucyApp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.naohua
import androidx.compose.runtime.snapshotFlow
import com.cephalon.lucyApp.components.DesignScaleProvider
import com.cephalon.lucyApp.components.LocalDesignScale
import com.cephalon.lucyApp.components.ToastHost
import com.cephalon.lucyApp.components.rememberToastState
import com.cephalon.lucyApp.scan.rememberCameraPermissionController
import com.cephalon.lucyApp.scan.rememberOpenAppSettings
import com.cephalon.lucyApp.screens.agentmodel.BackIcon
import com.cephalon.lucyApp.screens.agentmodel.ChevronRightIcon
import com.cephalon.lucyApp.screens.brainbox.BrainBoxLoginSheet
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeScreen(
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    onLogout: () -> Unit,
    onOpenSdkTest: () -> Unit,
    onOpenWsTest: () -> Unit,
    onOpenBrainBoxGuide: () -> Unit,
    onOpenBrainBoxLoginSuccess: (cdi: String) -> Unit,
    onOpenAgentModel: (onLoading: (Boolean) -> Unit, onStep: (Int) -> Unit, onError: (String) -> Unit) -> Unit,
    onOpenScanBindChannel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showBrainBoxLoginSheet by remember { mutableStateOf(false) }
    var isCloudLoading by remember { mutableStateOf(false) }
    var cloudDeployStep by remember { mutableStateOf(0) }
    var cloudDeployError by remember { mutableStateOf<String?>(null) }
    val toastState = rememberToastState()
    val cameraPermission = rememberCameraPermissionController()
    val openAppSettings = rememberOpenAppSettings()

    val openScanWithPermission: () -> Unit = {
        if (cameraPermission.hasPermission) {
            onOpenScanBindChannel()
        } else {
            val countBefore = cameraPermission.responseCount
            cameraPermission.requestPermission()
            scope.launch {
                snapshotFlow { cameraPermission.responseCount }
                    .filter { it > countBefore }
                    .first()
                if (cameraPermission.hasPermission) {
                    onOpenScanBindChannel()
                } else {
                    openAppSettings()
                }
            }
        }
    }

    DesignScaleProvider(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFC))
    ) {
        val ds = LocalDesignScale.current

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                Spacer(modifier = Modifier.height(ds.sh(12.dp)))

                // ── 顶部栏：返回 + 接入方式 ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ds.sw(20.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(ds.sm(32.dp))
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.05f))
                            .border(
                                width = 0.5.dp,
                                color = Color.Black.copy(alpha = 0.06f),
                                shape = CircleShape,
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { onBack() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = BackIcon,
                            contentDescription = "返回",
                            tint = Color.Black.copy(alpha = 0.60f),
                            modifier = Modifier.size(
                                width = ds.sw(11.dp),
                                height = ds.sh(17.dp),
                            ),
                        )
                    }
                    Text(
                        text = "接入方式",
                        fontSize = ds.sp(20f),
                        fontWeight = FontWeight.Medium,
                        color = Color.Black.copy(alpha = 0.90f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // 占位，保持标题居中
                    Spacer(modifier = Modifier.size(ds.sm(32.dp)))
                }

                Spacer(modifier = Modifier.height(ds.sh(28.dp)))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ds.sw(20.dp)),
                ) {
                    // ── 添加新设备 ──
                    Text(
                        text = "添加新设备",
                        fontSize = ds.sp(18f),
                        fontWeight = FontWeight.Medium,
                        color = Color.Black.copy(alpha = 0.90f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(ds.sh(4.dp)))

                    // ── 小标题 ──
                    Text(
                        text = "发现附近的脑花设备，确保蓝牙已开启且设备通电",
                        fontSize = ds.sp(14f),
                        fontWeight = FontWeight.Normal,
                        color = Color.Black.copy(alpha = 0.60f),
                    )

                    Spacer(modifier = Modifier.height(ds.sh(24.dp)))

                    // ── 脑花盒子用户 ──
                    AccessOptionCard(
                        icon = { NaoHuaBoxIcon(modifier = Modifier.size(ds.sm(20.dp))) },
                        title = "脑花盒子用户",
                        subtitle = "我拥有 AI NPC/龙虾派 CEPi",
                        onClick = { showBrainBoxLoginSheet = true },
                    )

                    Spacer(modifier = Modifier.height(ds.sh(12.dp)))

                    // ── 本地部署用户 ──
                    AccessOptionCard(
                        icon = {
                            Icon(
                                imageVector = LocalDeployIcon,
                                contentDescription = null,
                                modifier = Modifier.size(ds.sm(20.dp)),
                                tint = Color.Black.copy(alpha = 0.90f),
                            )
                        },
                        title = "本地部署用户",
                        subtitle = "我自己有本地龙虾",
                        onClick = { openScanWithPermission() },
                    )

                    Spacer(modifier = Modifier.height(ds.sh(12.dp)))

                    // ── 端脑云用户 ──
                    AccessOptionCard(
                        icon = {
                            Icon(
                                imageVector = CloudDeployIcon,
                                contentDescription = null,
                                modifier = Modifier.size(ds.sm(20.dp)),
                                tint = Color.Black.copy(alpha = 0.90f),
                            )
                        },
                        title = "端脑云用户",
                        subtitle = "我没有龙虾",
                        onClick = {
                            if (!isCloudLoading) {
                                cloudDeployStep = 0
                                cloudDeployError = null
                                onOpenAgentModel(
                                    { isCloudLoading = it },
                                    { step -> cloudDeployStep = step },
                                    { msg ->
                                        cloudDeployError = msg
                                        toastState.show(msg)
                                    },
                                )
                            }
                        },
                    )
                }
            }

            // 端脑云部署进度弹窗
            CloudDeploySheet(
                isVisible = isCloudLoading,
                completedStep = cloudDeployStep,
                errorMessage = cloudDeployError,
                onDismiss = { /* 部署中不允许关闭 */ },
            )

            ToastHost(state = toastState, modifier = Modifier.align(Alignment.TopCenter))

            BrainBoxLoginSheet(
                isVisible = showBrainBoxLoginSheet,
                onDismiss = { showBrainBoxLoginSheet = false },
                onBindSuccess = { cdi ->
                    showBrainBoxLoginSheet = false
                    onOpenBrainBoxLoginSuccess(cdi)
                }
            )
        }
    }
}

// ── 接入方式卡片 ──
@Composable
private fun AccessOptionCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val ds = LocalDesignScale.current
    val cardShape = RoundedCornerShape(ds.sm(16.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 30.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f),
            )
            .clip(cardShape)
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(ds.sm(16.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // icon 外框
        Box(
            modifier = Modifier
                .size(ds.sm(40.dp))
                .clip(RoundedCornerShape(ds.sm(12.dp)))
                .background(Color.Black.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }

        Spacer(modifier = Modifier.width(ds.sw(12.dp)))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = ds.sp(16f),
                fontWeight = FontWeight.Medium,
                color = Color.Black.copy(alpha = 0.90f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(ds.sh(2.dp)))
            Text(
                text = subtitle,
                fontSize = ds.sp(12f),
                fontWeight = FontWeight.Normal,
                color = Color.Black.copy(alpha = 0.40f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            imageVector = ChevronRightIcon,
            contentDescription = null,
            tint = Color.Black.copy(alpha = 0.20f),
            modifier = Modifier.size(
                width = ds.sw(12.dp),
                height = ds.sh(24.dp),
            ),
        )
    }
}

// ── 脑花盒子 icon（使用 drawable/naohua.svg）──
@Composable
private fun NaoHuaBoxIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.naohua),
        contentDescription = null,
        modifier = modifier,
    )
}

// ── 本地部署 icon ──
private val LocalDeployIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "LocalDeploy", defaultWidth = 20.dp, defaultHeight = 20.dp,
        viewportWidth = 20f, viewportHeight = 20f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            fillAlpha = 0.9f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(1.6485f, 5.356f)
            curveTo(1.6485f, 4.3705f, 2.04f, 3.4254f, 2.7368f, 2.7286f)
            curveTo(3.4336f, 2.0317f, 4.3787f, 1.6403f, 5.3642f, 1.6403f)
            horizontalLineTo(14.6531f)
            curveTo(15.629f, 1.6546f, 16.5601f, 2.0523f, 17.2452f, 2.7475f)
            curveTo(17.9303f, 3.4427f, 18.3144f, 4.3795f, 18.3144f, 5.3556f)
            curveTo(18.3144f, 6.3316f, 17.9303f, 7.2685f, 17.2452f, 7.9637f)
            curveTo(16.5601f, 8.6589f, 15.629f, 9.0566f, 14.6531f, 9.0709f)
            horizontalLineTo(5.3634f)
            curveTo(4.3781f, 9.0709f, 3.4331f, 8.6795f, 2.7363f, 7.9828f)
            curveTo(2.0395f, 7.2862f, 1.6479f, 6.3413f, 1.6477f, 5.356f)
            horizontalLineTo(1.6485f)
            close()
            moveTo(3.0418f, 5.356f)
            curveTo(3.0418f, 5.9969f, 3.2687f, 6.5446f, 3.7225f, 6.9977f)
            curveTo(3.9352f, 7.2171f, 4.1906f, 7.3907f, 4.473f, 7.5076f)
            curveTo(4.7553f, 7.6245f, 5.0586f, 7.6824f, 5.3642f, 7.6776f)
            horizontalLineTo(14.6531f)
            curveTo(14.9586f, 7.6824f, 15.262f, 7.6245f, 15.5443f, 7.5076f)
            curveTo(15.8267f, 7.3907f, 16.0821f, 7.2171f, 16.2948f, 6.9977f)
            curveTo(16.5142f, 6.785f, 16.6878f, 6.5296f, 16.8047f, 6.2472f)
            curveTo(16.9216f, 5.9649f, 16.9795f, 5.6615f, 16.9747f, 5.356f)
            curveTo(16.9796f, 5.0503f, 16.9218f, 4.7468f, 16.8049f, 4.4643f)
            curveTo(16.6879f, 4.1818f, 16.5143f, 3.9263f, 16.2948f, 3.7135f)
            curveTo(16.0821f, 3.494f, 15.8267f, 3.3205f, 15.5443f, 3.2035f)
            curveTo(15.262f, 3.0866f, 14.9586f, 3.0287f, 14.6531f, 3.0336f)
            horizontalLineTo(5.3634f)
            curveTo(5.0578f, 3.0287f, 4.7545f, 3.0866f, 4.4722f, 3.2035f)
            curveTo(4.1898f, 3.3205f, 3.9344f, 3.494f, 3.7217f, 3.7135f)
            curveTo(3.502f, 3.9262f, 3.3283f, 4.1817f, 3.2112f, 4.4642f)
            curveTo(3.0941f, 4.7467f, 3.0362f, 5.0502f, 3.041f, 5.356f)
            horizontalLineTo(3.0418f)
            close()
            moveTo(7.4541f, 4.6593f)
            curveTo(7.6389f, 4.6593f, 7.8161f, 4.7327f, 7.9467f, 4.8634f)
            curveTo(8.0774f, 4.994f, 8.1508f, 5.1712f, 8.1508f, 5.356f)
            curveTo(8.1508f, 5.5407f, 8.0774f, 5.7179f, 7.9467f, 5.8486f)
            curveTo(7.8161f, 5.9792f, 7.6389f, 6.0526f, 7.4541f, 6.0526f)
            horizontalLineTo(6.0609f)
            curveTo(5.8761f, 6.0526f, 5.6989f, 5.9792f, 5.5683f, 5.8486f)
            curveTo(5.4376f, 5.7179f, 5.3642f, 5.5407f, 5.3642f, 5.356f)
            curveTo(5.3642f, 5.1712f, 5.4376f, 4.994f, 5.5683f, 4.8634f)
            curveTo(5.6989f, 4.7327f, 5.8761f, 4.6593f, 6.0609f, 4.6593f)
            horizontalLineTo(7.4541f)
            close()
            moveTo(1.6485f, 14.644f)
            curveTo(1.6487f, 13.6587f, 2.0403f, 12.7138f, 2.7371f, 12.0171f)
            curveTo(3.4339f, 11.3205f, 4.3789f, 10.9291f, 5.3642f, 10.9291f)
            horizontalLineTo(14.6531f)
            curveTo(15.629f, 10.9434f, 16.5601f, 11.3411f, 17.2452f, 12.0363f)
            curveTo(17.9303f, 12.7315f, 18.3144f, 13.6684f, 18.3144f, 14.6444f)
            curveTo(18.3144f, 15.6205f, 17.9303f, 16.5573f, 17.2452f, 17.2525f)
            curveTo(16.5601f, 17.9477f, 15.629f, 18.3454f, 14.6531f, 18.3597f)
            horizontalLineTo(5.3634f)
            curveTo(4.3779f, 18.3597f, 3.4328f, 17.9682f, 2.736f, 17.2714f)
            curveTo(2.0392f, 16.5746f, 1.6477f, 15.6295f, 1.6477f, 14.644f)
            horizontalLineTo(1.6485f)
            close()
            moveTo(3.0418f, 14.644f)
            curveTo(3.0418f, 15.2857f, 3.2687f, 15.8327f, 3.7225f, 16.2865f)
            curveTo(3.9352f, 16.506f, 4.1906f, 16.6795f, 4.473f, 16.7965f)
            curveTo(4.7553f, 16.9134f, 5.0586f, 16.9712f, 5.3642f, 16.9664f)
            horizontalLineTo(14.6531f)
            curveTo(14.9586f, 16.9712f, 15.262f, 16.9134f, 15.5443f, 16.7965f)
            curveTo(15.8267f, 16.6795f, 16.0821f, 16.506f, 16.2948f, 16.2865f)
            curveTo(16.5143f, 16.0737f, 16.6879f, 15.8182f, 16.8049f, 15.5357f)
            curveTo(16.9218f, 15.2532f, 16.9796f, 14.9497f, 16.9747f, 14.644f)
            curveTo(16.9795f, 14.3384f, 16.9216f, 14.0351f, 16.8047f, 13.7528f)
            curveTo(16.6878f, 13.4704f, 16.5142f, 13.215f, 16.2948f, 13.0023f)
            curveTo(16.0821f, 12.7829f, 15.8267f, 12.6093f, 15.5443f, 12.4924f)
            curveTo(15.262f, 12.3754f, 14.9586f, 12.3176f, 14.6531f, 12.3224f)
            horizontalLineTo(5.3634f)
            curveTo(5.0578f, 12.3176f, 4.7545f, 12.3754f, 4.4722f, 12.4924f)
            curveTo(4.1898f, 12.6093f, 3.9344f, 12.7829f, 3.7217f, 13.0023f)
            curveTo(3.5021f, 13.215f, 3.3284f, 13.4703f, 3.2113f, 13.7527f)
            curveTo(3.0943f, 14.035f, 3.0363f, 14.3384f, 3.041f, 14.644f)
            horizontalLineTo(3.0418f)
            close()
            moveTo(7.4541f, 13.9474f)
            curveTo(7.6389f, 13.9474f, 7.8161f, 14.0208f, 7.9467f, 14.1514f)
            curveTo(8.0774f, 14.2821f, 8.1508f, 14.4593f, 8.1508f, 14.644f)
            curveTo(8.1508f, 14.8288f, 8.0774f, 15.006f, 7.9467f, 15.1366f)
            curveTo(7.8161f, 15.2673f, 7.6389f, 15.3407f, 7.4541f, 15.3407f)
            horizontalLineTo(6.0609f)
            curveTo(5.8761f, 15.3407f, 5.6989f, 15.2673f, 5.5683f, 15.1366f)
            curveTo(5.4376f, 15.006f, 5.3642f, 14.8288f, 5.3642f, 14.644f)
            curveTo(5.3642f, 14.4593f, 5.4376f, 14.2821f, 5.5683f, 14.1514f)
            curveTo(5.6989f, 14.0208f, 5.8761f, 13.9474f, 6.0609f, 13.9474f)
            horizontalLineTo(7.4541f)
            close()
        }
    }.build()
}

// ── 端脑云 icon ──
private val CloudDeployIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CloudDeploy", defaultWidth = 20.dp, defaultHeight = 20.dp,
        viewportWidth = 20f, viewportHeight = 20f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            fillAlpha = 0.9f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(10.5525f, 2.4061f)
            curveTo(12.1125f, 2.4525f, 13.5999f, 3.0767f, 14.7253f, 4.158f)
            curveTo(15.8394f, 5.2285f, 16.5194f, 6.6714f, 16.6384f, 8.2108f)
            lineTo(16.7712f, 8.2654f)
            curveTo(17.5742f, 8.6112f, 18.264f, 9.1757f, 18.7605f, 9.8953f)
            curveTo(19.257f, 10.6149f, 19.5406f, 11.4604f, 19.5789f, 12.3338f)
            lineTo(19.5837f, 12.5486f)
            lineTo(19.5808f, 12.7088f)
            curveTo(19.537f, 13.8788f, 19.0525f, 14.9895f, 18.2253f, 15.8182f)
            curveTo(17.3982f, 16.6468f, 16.2888f, 17.1325f, 15.1189f, 17.1785f)
            lineTo(15.0134f, 17.1815f)
            horizontalLineTo(5.574f)
            lineTo(5.408f, 17.1727f)
            curveTo(2.6097f, 16.9809f, 0.4169f, 14.6314f, 0.4167f, 11.8045f)
            curveTo(0.4167f, 10.6088f, 0.8149f, 9.4469f, 1.5486f, 8.5028f)
            curveTo(2.2823f, 7.5587f, 3.3099f, 6.8857f, 4.4685f, 6.5906f)
            lineTo(4.4304f, 6.6004f)
            lineTo(4.4851f, 6.452f)
            curveTo(4.9252f, 5.3001f, 5.6942f, 4.3027f, 6.696f, 3.5838f)
            curveTo(7.6979f, 2.865f, 8.8887f, 2.456f, 10.1208f, 2.408f)
            lineTo(10.366f, 2.4031f)
            lineTo(10.5525f, 2.4061f)
            close()
            moveTo(10.365f, 3.8328f)
            curveTo(9.3817f, 3.8323f, 8.4217f, 4.1307f, 7.6111f, 4.6873f)
            curveTo(6.8005f, 5.244f, 6.1776f, 6.0331f, 5.825f, 6.951f)
            lineTo(5.7673f, 7.1063f)
            lineTo(5.655f, 7.4324f)
            curveTo(5.6163f, 7.5446f, 5.55f, 7.6452f, 5.4627f, 7.7254f)
            curveTo(5.3753f, 7.8056f, 5.2691f, 7.8633f, 5.1541f, 7.8924f)
            lineTo(4.821f, 7.9774f)
            curveTo(3.0762f, 8.4198f, 1.8464f, 9.9958f, 1.8464f, 11.8045f)
            curveTo(1.8466f, 13.8801f, 3.4564f, 15.6047f, 5.4929f, 15.7449f)
            lineTo(5.6101f, 15.7518f)
            horizontalLineTo(14.991f)
            lineTo(15.0671f, 15.7488f)
            curveTo(15.8739f, 15.7171f, 16.6392f, 15.3827f, 17.2107f, 14.8123f)
            curveTo(17.7823f, 14.2416f, 18.1182f, 13.4758f, 18.1511f, 12.6688f)
            lineTo(18.1531f, 12.5359f)
            curveTo(18.1539f, 11.9092f, 17.9711f, 11.2956f, 17.6277f, 10.7713f)
            curveTo(17.2843f, 10.247f, 16.7953f, 9.8346f, 16.2205f, 9.5848f)
            lineTo(16.0837f, 9.5291f)
            lineTo(15.7029f, 9.3836f)
            curveTo(15.5755f, 9.3348f, 15.4646f, 9.2501f, 15.3835f, 9.1404f)
            curveTo(15.3026f, 9.0309f, 15.2542f, 8.9004f, 15.2449f, 8.7645f)
            lineTo(15.2166f, 8.3553f)
            curveTo(15.1327f, 7.1542f, 14.6056f, 6.0271f, 13.738f, 5.1922f)
            curveTo(12.8705f, 4.3573f, 11.7237f, 3.8735f, 10.5203f, 3.8358f)
            lineTo(10.366f, 3.8328f)
            horizontalLineTo(10.365f)
            close()
            moveTo(11.907f, 11.6981f)
            curveTo(12.0882f, 11.6981f, 12.2629f, 11.7677f, 12.3953f, 11.8914f)
            curveTo(12.5275f, 12.0151f, 12.6075f, 12.1844f, 12.6199f, 12.3651f)
            curveTo(12.6322f, 12.5457f, 12.576f, 12.7246f, 12.4617f, 12.8651f)
            curveTo(12.3474f, 13.0055f, 12.184f, 13.0971f, 12.0046f, 13.1219f)
            lineTo(11.907f, 13.1287f)
            horizontalLineTo(8.0935f)
            curveTo(7.9124f, 13.1287f, 7.7376f, 13.06f, 7.6052f, 12.9363f)
            curveTo(7.4729f, 12.8127f, 7.3921f, 12.6433f, 7.3796f, 12.4627f)
            curveTo(7.3673f, 12.282f, 7.4246f, 12.1032f, 7.5388f, 11.9627f)
            curveTo(7.6531f, 11.8222f, 7.8164f, 11.7296f, 7.9959f, 11.7049f)
            lineTo(8.0935f, 11.6981f)
            horizontalLineTo(11.907f)
            close()
        }
    }.build()
}
