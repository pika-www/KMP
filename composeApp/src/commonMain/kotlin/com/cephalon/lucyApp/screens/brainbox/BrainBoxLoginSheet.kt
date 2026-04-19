package com.cephalon.lucyApp.screens.brainbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.api.LucyDevice
import com.cephalon.lucyApp.api.channelDeviceId
import com.cephalon.lucyApp.time.currentTimeMillis
import com.cephalon.lucyApp.brainbox.BrainBoxBleDevice
import com.cephalon.lucyApp.brainbox.BrainBoxProvisionController
import com.cephalon.lucyApp.brainbox.BrainBoxWifiMode
import com.cephalon.lucyApp.brainbox.BrainBoxWifiNetwork
import com.cephalon.lucyApp.brainbox.PhoneWifiState
import com.cephalon.lucyApp.brainbox.WifiCredentialCache
import com.cephalon.lucyApp.brainbox.rememberBrainBoxProvisionController
import com.cephalon.lucyApp.components.HalfModalBottomSheet
import com.cephalon.lucyApp.components.ToastHost
import com.cephalon.lucyApp.components.ToastState
import com.cephalon.lucyApp.components.rememberToastState
import com.cephalon.lucyApp.deviceaccess.BleScanDevice
import com.cephalon.lucyApp.deviceaccess.gatt.GattWifiNetwork
import com.cephalon.lucyApp.deviceaccess.gatt.ProvisionFlowStage
import com.cephalon.lucyApp.deviceaccess.gatt.ProvisionManager
import com.cephalon.lucyApp.deviceaccess.gatt.rememberProvisionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private enum class BrainBoxStep(
    val title: String,
    val subtitle: String,
) {
    Scan(
        title = "扫描设备",
        subtitle = "发现附近的脑花设备，确保蓝牙已开启且设备通电",
    ),
    Wifi(
        title = "连接Wi‑Fi",
        subtitle = "为选中的设备完成当前网络配置",
    ),
    Bind(
        title = "绑定Channel",
        subtitle = "将此设备绑定到你的脑花账号",
    ),
}

/**
 * 扫描列表里每一台 BLE 设备的探测结果。
 * - [Probing]：正在后台 GATT 读取 pairing_info。
 * - [Free]：未绑定（binding_status != "bound"），可正常走绑定流程。
 * - [OwnedByMe]：固件已绑定且 cdi 属于当前账号，可直接进入。
 * - [Occupied]：固件已绑定但 cdi 不属于当前账号，置灰不可点。
 * - [Failed]：探测失败（断开 / 超时等），允许用户手动点连接重试。
 */
internal sealed interface DeviceProbeState {
    data object Probing : DeviceProbeState
    data object Free : DeviceProbeState
    data class OwnedByMe(val cdi: String) : DeviceProbeState
    data object Occupied : DeviceProbeState
    data object Failed : DeviceProbeState
}

// ATT / GATT 层错误触发 forceReconnect 的单轮上限。
// 超过该上限往往表明设备端的 Lucy IPC 永久异常（例如 pairing ipc not enabled /
// lucy pairing client not connected），再重连也不会更好，不如提示用户回扫描步人工排查。
private const val OTP_FORCE_RECONNECT_MAX = 3

@Composable
fun BrainBoxLoginSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onBindSuccess: (cdi: String) -> Unit,
) {
    val authRepository = koinInject<AuthRepository>()
    val wifiCredentialCache = koinInject<WifiCredentialCache>()
    val controller = rememberBrainBoxProvisionController()
    val provisionManager = rememberProvisionManager()
    val scope = rememberCoroutineScope()
    val toastState = rememberToastState()
    val bleScanState by provisionManager.scanState.collectAsState()
    val provisionState by provisionManager.state.collectAsState()
    val bleDevices = bleScanState.devices.map { it.toBrainBoxBleDevice() }
    val isBleScanning = bleScanState.isScanning
    // 设备当前连接的 Wi‑Fi SSID（来自 connectDevice 阶段读到的 network_status）。
    // 用来：(1) 在 Wi‑Fi 列表里给这条加上"已连接"标记；(2) 当扫描结果里没包含它时
    // 兜底 prepend 一条占位项，保证用户总能看到并选回当前网络。
    val currentSsid = provisionState.networkStatus
        ?.takeIf { it.isConnected }
        ?.ssid
        ?.trim()
        ?.takeIf { it.isNotBlank() }
    // 改动：Wi‑Fi 列表改用"手机端扫描"（controller.wifiNetworks），不再走 BLE 让设备扫网
    // （原来是 provisionState.wifiNetworks，来自 provisionManager.refreshWifiNetworks →
    // router.scanWifi()）。原因：BLE 扫网耗时/不稳定，而手机本身就连在同一个 Wi‑Fi 环境里，
    // 列表用手机看到的足够准确；拿到 SSID 后仍走原逻辑向设备写 wifi_config。
    val phoneScannedWifi by controller.wifiNetworks.collectAsState()
    val phoneWifiLoading by controller.isWifiLoading.collectAsState()
    val scannedWifiNetworks = phoneScannedWifi.map { raw ->
        // phoneScannedWifi 里的 isCurrent 是手机的当前连接标记；脑花盒子当前连接的 SSID
        // 要以 network_status 为准（设备视角），所以用 currentSsid 重新算 isCurrent。
        raw.copy(
            isCurrent = currentSsid != null && raw.ssid.equals(currentSsid, ignoreCase = true),
        )
    }
    val wifiNetworks: List<BrainBoxWifiNetwork> = if (
        currentSsid != null &&
        scannedWifiNetworks.none { it.ssid.equals(currentSsid, ignoreCase = true) }
    ) {
        listOf(
            BrainBoxWifiNetwork(
                ssid = currentSsid,
                strengthLevel = 0,
                isSecure = true,
                isCurrent = true,
            ),
        ) + scannedWifiNetworks
    } else {
        scannedWifiNetworks
    }
    // 改动：把 controller.isWifiLoading（手机扫描中）并入 loading 条件。ScanningWifi 分支
    // 现在理论上不会再被触发（不再调 provisionManager.refreshWifiNetworks），保留不影响。
    val isWifiLoading = phoneWifiLoading ||
        provisionState.stage == ProvisionFlowStage.Connecting ||
        provisionState.stage == ProvisionFlowStage.Discovering ||
        provisionState.stage == ProvisionFlowStage.ReadingDeviceInfo ||
        provisionState.stage == ProvisionFlowStage.ReadingNetworkStatus ||
        provisionState.stage == ProvisionFlowStage.ReadingPairingInfo ||
        provisionState.stage == ProvisionFlowStage.RequestingOtp ||
        provisionState.stage == ProvisionFlowStage.ScanningWifi ||
        provisionState.stage == ProvisionFlowStage.Reconnecting

    var currentStep by remember { mutableStateOf(BrainBoxStep.Scan) }
    var selectedBleDevice by remember { mutableStateOf<BrainBoxBleDevice?>(null) }
    var selectedWifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var manualSsid by remember { mutableStateOf("") }
    var wifiConnectedSsid by remember { mutableStateOf<String?>(null) }
    var isConnectingWifi by remember { mutableStateOf(false) }
    var isBinding by remember { mutableStateOf(false) }
    var connectingDeviceId by remember { mutableStateOf<String?>(null) }
    var isLoadingDevices by remember { mutableStateOf(false) }
    var otpReconnectCount by remember { mutableStateOf(0) }
    val serverDevices = remember { mutableStateListOf<LucyDevice>() }
    // 按 device.id 记录每台蓝牙设备的探测结果；缺省（map 中无 key）表示尚未探测或正在等待下次重试。
    val probeStates = remember { mutableStateMapOf<String, DeviceProbeState>() }
    // 按 device.id 记录失败重试次数（用于指数退避）和下次允许重试的时间戳。
    val probeAttempts = remember { mutableStateMapOf<String, Int>() }
    val probeNextRetryAtMs = remember { mutableStateMapOf<String, Long>() }

    fun resetState() {
        currentStep = BrainBoxStep.Scan
        selectedBleDevice = null
        selectedWifiSsid = ""
        wifiPassword = ""
        manualSsid = ""
        wifiConnectedSsid = null
        isConnectingWifi = false
        isBinding = false
        connectingDeviceId = null
        isLoadingDevices = false
        otpReconnectCount = 0
        serverDevices.clear()
        probeStates.clear()
        probeAttempts.clear()
        probeNextRetryAtMs.clear()
        provisionManager.stopScan()
        scope.launch { provisionManager.cancel() }
    }

    fun goPrevious() {
        currentStep = when (currentStep) {
            BrainBoxStep.Scan -> BrainBoxStep.Scan
            BrainBoxStep.Wifi -> BrainBoxStep.Scan
            BrainBoxStep.Bind -> BrainBoxStep.Wifi
        }
    }

    val wifiMode = BrainBoxWifiMode.NearbyScan
    val selectedWifiNetwork = wifiNetworks.firstOrNull { it.ssid == selectedWifiSsid }
    val selectedWifiName = when (wifiMode) {
        BrainBoxWifiMode.NearbyScan -> selectedWifiSsid.trim()
        BrainBoxWifiMode.ManualOnly -> manualSsid.trim()
    }
    // 当前选中的 SSID 就是设备此刻连接的那一个：无需重写 wifi_config，走短路径直接进入 Bind。
    val isSelectedCurrent = currentSsid != null &&
        selectedWifiName.isNotBlank() &&
        selectedWifiName.equals(currentSsid, ignoreCase = true)
    val currentSheetStepIndex = when (currentStep) {
        BrainBoxStep.Scan -> 0
        BrainBoxStep.Wifi -> 1
        BrainBoxStep.Bind -> 2
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            resetState()
        } else {
            provisionManager.stopScan()
            provisionManager.cancel()
        }
    }

    // 进入 Wi‑Fi 步骤且用户还没做选择时，自动默认选中设备当前连接的 SSID。
    // 这样默认按钮会呈现"使用当前 Wi‑Fi 并继续"，符合"保持当前网络"的最常见诉求；
    // 用户点击列表里其它 SSID 即可切换为"连接新 Wi‑Fi"的分支。
    LaunchedEffect(currentStep, currentSsid) {
        val ssid = currentSsid
        if (currentStep == BrainBoxStep.Wifi &&
            selectedWifiSsid.isBlank() &&
            !ssid.isNullOrBlank()
        ) {
            selectedWifiSsid = ssid
        }
    }

    // 蓝牙状态变化只影响扫描步骤
    LaunchedEffect(isVisible, currentStep, controller.bluetoothPermissionGranted, controller.bluetoothEnabled) {
        if (!isVisible) return@LaunchedEffect
        if (currentStep == BrainBoxStep.Scan) {
            val granted = controller.requestBluetoothPermission()
            if (granted && controller.bluetoothEnabled) {
                provisionManager.startScan().onFailure {
                    toastState.show(it.message ?: "开始扫描失败")
                }
            }
        }
    }

    // 扫描步骤后台探测调度器：
    // - 扫描全程不停，probe 与 scan 并行运行（iOS Core Bluetooth 天然支持，Android 亦允许）；
    // - probe 之间仍然串行（共用 provisionManager.gattMutex），一次只连一台；
    // - 用户点"连接"后 connectingDeviceId 非空，调度器暂停 probe 让出 GATT 通道；
    // - 失败自动重试：指数退避 500ms → 1s → 2s → 4s → 8s，只延迟这一台，不阻塞其他设备；
    //   UI 在重试期间仍显示"检测中…"，直到拿到 Free/Occupied/OwnedByMe 等确定结果。
    // - key 只盯 (isVisible, currentStep)，蓝牙开关/权限在 loop 内动态查，避免广播抖动重启循环。
    LaunchedEffect(isVisible, currentStep) {
        if (!isVisible || currentStep != BrainBoxStep.Scan) return@LaunchedEffect
        while (true) {
            if (connectingDeviceId != null ||
                !controller.bluetoothPermissionGranted ||
                !controller.bluetoothEnabled
            ) {
                delay(300L)
                continue
            }
            val now = currentTimeMillis()
            val next = provisionManager.scanState.value.devices.firstOrNull { dev ->
                probeStates[dev.id] == null && (probeNextRetryAtMs[dev.id] ?: 0L) <= now
            }
            if (next == null) {
                delay(300L)
                continue
            }
            probeStates[next.id] = DeviceProbeState.Probing
            val attempt = (probeAttempts[next.id] ?: 0) + 1
            probeAttempts[next.id] = attempt
            println("[BrainBox] probe for ${next.id} (${next.name}) attempt #$attempt")
            val probeResult = provisionManager.probeDevice(next)
            val resolved: DeviceProbeState = probeResult.fold(
                onSuccess = { info ->
                    when {
                        !info.isBound || info.channelDeviceId.isBlank() -> DeviceProbeState.Free
                        else -> {
                            val cdi = info.channelDeviceId
                            val lookup = runCatching { authRepository.findDeviceByChannelDeviceId(cdi) }
                            val owned = lookup.getOrNull()
                            when {
                                lookup.isFailure -> {
                                    println("[BrainBox] probe owner lookup failed for $cdi: ${lookup.exceptionOrNull()?.message}")
                                    DeviceProbeState.Failed
                                }
                                owned != null -> DeviceProbeState.OwnedByMe(cdi)
                                else -> DeviceProbeState.Occupied
                            }
                        }
                    }
                },
                onFailure = { DeviceProbeState.Failed },
            )
            if (resolved is DeviceProbeState.Failed) {
                // 未拿到确定结果：安排下次重试，UI 仍显示"检测中…"（probeStates 清掉即回到 null 态）
                val shiftBits = (attempt - 1).coerceAtMost(4)
                val backoff = (500L shl shiftBits).coerceAtMost(8_000L)
                probeNextRetryAtMs[next.id] = currentTimeMillis() + backoff
                probeStates.remove(next.id)
                println("[BrainBox] probe failed for ${next.id} (attempt #$attempt), retry after ${backoff}ms")
            } else {
                probeStates[next.id] = resolved
                probeAttempts.remove(next.id)
                probeNextRetryAtMs.remove(next.id)
                println("[BrainBox] probe resolved ${next.id} -> $resolved (attempts=$attempt)")
            }
        }
    }

    // 当发现当前账号无权使用这台盒子（固件已绑给别人 / 查询失败）时，干净地断开 BLE、
    // 清空 UI 状态、回到扫描步并重新开始扫描。必须在 scope.launch 内调用。
    suspend fun goBackToScanAfterRejection(message: String) {
        toastState.show(message)
        provisionManager.cancel()
        currentStep = BrainBoxStep.Scan
        selectedBleDevice = null
        selectedWifiSsid = ""
        wifiPassword = ""
        wifiConnectedSsid = null
        isConnectingWifi = false
        connectingDeviceId = null
        isBinding = false
        otpReconnectCount = 0
        if (controller.bluetoothPermissionGranted && controller.bluetoothEnabled) {
            provisionManager.startScan().onFailure {
                toastState.show(it.message ?: "重新扫描失败")
            }
        }
    }

    fun requestOtpAndBind() {
        if (isBinding) return
        isBinding = true
        scope.launch {
            println("[BrainBox] 开始请求 OTP 并绑定...")

            // 路由层已经采用 "write → delay 5s → read → 按 otp 字段判定" 策略
            // （见 GattRouter.requestOtpAndReadPairingInfo）：
            //   - write 的 ATT 错误（code=128/14）被吞掉仅告警，不再作为失败信号
            //   - 仅当 read lucy_pairing_info 失败（status=/att=/链路异常）时，UI 才会
            //     收到 GATT 层错误 → 触发 forceReconnect（上限 OTP_FORCE_RECONNECT_MAX）
            //   - read 成功但 otp 缺失 → 业务失败，直接报错不重连（重连解决不了 Lucy
            //     插件未就绪 / pairing ipc 未启用等设备端问题）
            val otpResult = provisionManager.requestOtpAfterWifi()
            val pairingInfo = otpResult.getOrNull()
            if (otpResult.isFailure || pairingInfo == null) {
                val error = otpResult.exceptionOrNull()
                val errorMsg = error?.message.orEmpty()
                val isGattLayerError = errorMsg.contains("status=", ignoreCase = true) ||
                    errorMsg.contains("att", ignoreCase = true) ||
                    errorMsg.contains("gatt", ignoreCase = true) ||
                    errorMsg.contains("尚未建立", ignoreCase = true) ||
                    // iOS 特有：CoreBluetooth 的 peripheral.services 被 Services Changed
                    // indication / 隐式重连清空后，会在 BleClient.ios 里抛"未找到特征"。
                    // 这条与"status=/att/gatt"同属 BLE 链路层异常，应该走 forceReconnect
                    // 兜底，而不是当作业务失败只 toast 一下然后卡住。
                    errorMsg.contains("未找到特征") ||
                    errorMsg.contains("发现 GATT Service 超时")
                println("[BrainBox] OTP 请求失败: $errorMsg (isGattLayer=$isGattLayerError)")
                if (isGattLayerError) {
                    otpReconnectCount++
                    if (otpReconnectCount >= OTP_FORCE_RECONNECT_MAX) {
                        println("[BrainBox] OTP 触发 forceReconnect 已达上限 $OTP_FORCE_RECONNECT_MAX 次，放弃重连，回扫描步")
                        goBackToScanAfterRejection(
                            "设备配对服务持续异常（已重试 $OTP_FORCE_RECONNECT_MAX 次），请检查设备状态或联系厂家",
                        )
                        return@launch
                    }
                    // 主动断开 → disconnectWatcher → attemptReconnect → reconnectedEvents
                    // → UI 在 Bind 步收到事件后会重置 isBinding 并重新调 requestOtpAndBind。
                    toastState.show("蓝牙链路异常，正在重新连接设备…（第 $otpReconnectCount/$OTP_FORCE_RECONNECT_MAX 次）")
                    isBinding = false
                    provisionManager.forceReconnect()
                    return@launch
                }
                toastState.show(errorMsg.ifBlank { "获取 OTP 失败，请重试" })
                isBinding = false
                return@launch
            }

            // 拿到有效 pairing_info：重置本轮 forceReconnect 计数，下次 ATT 错误恢复完整上限。
            otpReconnectCount = 0

            // 固件侧已绑定：必须校验 cdi 是否属于当前账号，避免把别人绑的盒子误当自己的打开。
            if (pairingInfo.isBound && pairingInfo.channelDeviceId.isNotBlank()) {
                val cdi = pairingInfo.channelDeviceId
                println("[BrainBox] 固件侧已绑定 cdi=$cdi，校验是否属于当前账号…")
                val lookup = runCatching { authRepository.findDeviceByChannelDeviceId(cdi) }
                val ownedDevice = lookup.getOrNull()
                if (lookup.isFailure) {
                    println("[BrainBox] 查询当前账号设备列表失败: ${lookup.exceptionOrNull()?.message}")
                    val msg = lookup.exceptionOrNull()?.message ?: "校验设备归属失败，请检查网络后重试"
                    goBackToScanAfterRejection(msg)
                    return@launch
                }
                if (ownedDevice != null) {
                    println("[BrainBox] 设备属于当前账号 (name=${ownedDevice.name})，直接跳转 cdi=$cdi")
                    toastState.show("设备已绑定")
                    onBindSuccess(cdi)
                    isBinding = false
                    return@launch
                }
                // 固件 bound 但当前账号没有这个 cdi → 是别人绑的盒子，固件不允许再发 OTP，只能提示并退出。
                println("[BrainBox] cdi=$cdi 不在当前账号设备列表，拒绝跳转并回到扫描步")
                goBackToScanAfterRejection("此设备已被其他账号绑定，请联系原持有人解绑后再试")
                return@launch
            }

            val otp = pairingInfo.otp
            if (otp.isBlank()) {
                toastState.show("获取 OTP 失败，请重试")
                isBinding = false
                return@launch
            }
            println("[BrainBox] OTP 获取成功: otp=$otp, 开始调用绑定接口...")
            authRepository.bindDeviceWithOtp(otp)
                .onSuccess { bindingData ->
                    println("[BrainBox] UI: 服务端绑定接口返回成功 cdi=${bindingData.cdi}, status=${bindingData.status}, msg=${bindingData.serverMsg}")
                    // 按协议再读一次 lucy_pairing_info，确认 binding_status == "bound" 后再跳转。
                    provisionManager.verifyBindingStatus()
                        .onSuccess { verifiedInfo ->
                            println("[BrainBox] UI: 绑定确认成功 binding_status=${verifiedInfo.bindingStatus}, cdi=${verifiedInfo.channelDeviceId}")
                            toastState.show(bindingData.serverMsg.ifBlank { "绑定成功" })
                            onBindSuccess(verifiedInfo.channelDeviceId.ifBlank { bindingData.cdi })
                        }
                        .onFailure { verifyError ->
                            println("[BrainBox] UI: 绑定确认失败 - ${verifyError.message}")
                            toastState.show(verifyError.message ?: "读取 pairing_info 失败，绑定未确认")
                        }
                }
                .onFailure { error ->
                    toastState.show(error.message ?: "绑定失败，请稍后重试")
                }
            isBinding = false
        }
    }

    // WiFi / 绑定步骤只在 step 切换时执行一次，不受蓝牙状态重触发
    LaunchedEffect(isVisible, currentStep) {
        if (!isVisible) return@LaunchedEffect
        when (currentStep) {
            BrainBoxStep.Scan -> { /* 由上面的 LaunchedEffect 处理 */ }
            BrainBoxStep.Wifi -> {
                provisionManager.stopScan()
                // 改动：不再让设备用 BLE 扫网（provisionManager.refreshWifiNetworks / wifi_scan）。
                // 改用手机端扫描（controller.refreshWifiNetworks）直接读"这台手机此刻能看到的 Wi‑Fi"，
                // 原因是手机跟目标盒子一般处于同一空间，结果更快也更稳；拿到 SSID 后用户选中 +
                // 输密码仍走原有 provisionManager.configureWifi 向设备下发 wifi_config。
                // 注：设备未必有 selectedBleDevice 依赖（手机扫网不需要设备参与），但保留 stopScan
                // 以复用"退出扫描态"的原有副作用。
                val networkStatus = provisionManager.state.value.networkStatus
                println(
                    "[BrainBox] Wi‑Fi 步骤: networkStatus=${networkStatus?.state}, " +
                        "ssid=${networkStatus?.ssid}, ip=${networkStatus?.ip}，开始刷新手机端附近 Wi‑Fi 列表",
                )
                controller.refreshWifiNetworks().onFailure {
                    toastState.show(it.message ?: "查询附近 Wi‑Fi 失败")
                }
            }
            BrainBoxStep.Bind -> {
                provisionManager.stopScan()
            }
        }
    }

    // 首次 BLE 断开由 ProvisionManager 自动重连；重连成功后按"断开时所在的步骤"继续执行：
    //  - Scan 步：不做任何主动推进，让扫描逻辑自己恢复（用户还在挑设备）。
    //  - Wifi 步：重新 refreshWifiNetworks，让用户继续在当前网络环境里挑 SSID。
    //  - Bind 步：重新 requestOtpAndBind（协议步骤 7，写新的 request_id、读 OTP、绑定）。
    LaunchedEffect(isVisible) {
        if (!isVisible) return@LaunchedEffect
        provisionManager.reconnectedEvents.collect {
            val stepAtReconnect = currentStep
            println("[BrainBox] 收到自动重连成功事件，currentStep=$stepAtReconnect，按当前步继续")
            when (stepAtReconnect) {
                BrainBoxStep.Scan -> {
                    // 扫描步骤通常不会触发 reconnect；真到这里也不强推，交给扫描循环自愈。
                    toastState.show("设备已重连")
                }
                BrainBoxStep.Wifi -> {
                    toastState.show("设备已重连，正在重新查询附近 Wi‑Fi…")
                    // 改动：重连后同样走手机扫网（controller.refreshWifiNetworks），不再向设备下发 wifi_scan。
                    controller.refreshWifiNetworks().onFailure {
                        toastState.show(it.message ?: "查询附近 Wi‑Fi 失败")
                    }
                }
                BrainBoxStep.Bind -> {
                    toastState.show("设备已重连，正在重新请求 OTP…")
                    isBinding = false
                    requestOtpAndBind()
                }
            }
        }
    }

    // 再次 BLE 断开（重连配额已用尽），回到扫描步骤。
    LaunchedEffect(isVisible) {
        if (!isVisible) return@LaunchedEffect
        provisionManager.disconnectEvents.collect { message ->
            println("[BrainBox] 收到 BLE 断开事件: $message，UI 回到扫描步骤")
            toastState.show(message)
            currentStep = BrainBoxStep.Scan
            selectedBleDevice = null
            selectedWifiSsid = ""
            wifiPassword = ""
            wifiConnectedSsid = null
            isConnectingWifi = false
            isBinding = false
            connectingDeviceId = null
            otpReconnectCount = 0
            // 自动重新开始扫描
            if (controller.bluetoothPermissionGranted && controller.bluetoothEnabled) {
                provisionManager.startScan().onFailure {
                    toastState.show(it.message ?: "重新扫描失败")
                }
            }
        }
    }

    val displayDevice = remember(selectedBleDevice, serverDevices.toList(), wifiConnectedSsid, provisionState.channelDeviceId) {
        serverDevices.firstOrNull { candidate ->
            candidate.channelDeviceId == provisionState.channelDeviceId
        } ?: serverDevices.firstOrNull { candidate ->
            val currentName = selectedBleDevice?.name.orEmpty()
            currentName.isNotBlank() && candidate.name.contains(currentName, ignoreCase = true)
        } ?: serverDevices.firstOrNull()
    }

    fun requestBluetoothAgain() {
        scope.launch {
            val granted = controller.requestBluetoothPermission()
            if (!granted) {
                toastState.show("请先授权蓝牙权限")
            } else if (!controller.bluetoothEnabled) {
                toastState.show("请先打开系统蓝牙")
            } else {
                provisionManager.startScan().onFailure {
                    toastState.show(it.message ?: "开始扫描失败")
                }
            }
        }
    }

    fun refreshWifiAgain() {
        // 改动：UI 上的"刷新"按钮也走手机端扫描（controller.refreshWifiNetworks），
        // 不再依赖 selectedBleDevice。手机扫 Wi‑Fi 不需要跟脑花盒子产生 BLE 交互，
        // 所以即使 BLE 暂时闪断也能独立跑。
        scope.launch {
            controller.refreshWifiNetworks().onFailure {
                toastState.show(it.message ?: "查询附近 Wi‑Fi 失败")
            }
        }
    }

    fun connectWifi() {
        val ssid = selectedWifiName
        if (ssid.isBlank()) {
            toastState.show("请选择要连接的 Wi‑Fi")
            return
        }
        // 短路径：用户选的就是设备当前连接的那一个 SSID —— 不下发 wifi_config，直接进入 Bind。
        // connectDevice 阶段已读过 network_status 并确认联网；再走 configureWifi 会触发固件切网，
        // 可能引发 BLE 断连-重连，体验更差。
        if (isSelectedCurrent) {
            println("[BrainBox] 用户沿用当前 Wi‑Fi ($ssid)，跳过 wifi_config 直接进入 Bind")
            wifiConnectedSsid = ssid
            currentStep = BrainBoxStep.Bind
            requestOtpAndBind()
            return
        }
        val networkRequiresPassword = when (wifiMode) {
            BrainBoxWifiMode.NearbyScan -> selectedWifiNetwork?.isSecure == true
            BrainBoxWifiMode.ManualOnly -> true
        }
        if (networkRequiresPassword && wifiPassword.isBlank()) {
            toastState.show("请输入 Wi‑Fi 密码")
            return
        }
        scope.launch {
            isConnectingWifi = true
            provisionManager.configureWifi(ssid = ssid, password = wifiPassword)
                .onSuccess { networkStatus ->
                    wifiConnectedSsid = ssid
                    // 用户手动配网成功：把 (ssid → 密码) 落盘，下次同 SSID 时自动注入免输。
                    // 空密码（开放 Wi‑Fi）由 WifiCredentialCache.save 内部过滤，不会污染缓存。
                    if (wifiPassword.isNotBlank()) {
                        wifiCredentialCache.save(ssid, wifiPassword)
                    }
                    toastState.show("设备已连接到 ${networkStatus.ssid.ifBlank { ssid }}")
                    currentStep = BrainBoxStep.Bind
                    // WiFi 连接成功后请求 OTP 并绑定
                    requestOtpAndBind()
                }
                .onFailure {
                    toastState.show(it.message ?: "连接 Wi‑Fi 失败")
                }
            isConnectingWifi = false
        }
    }

    HalfModalBottomSheet(
        isVisible = isVisible,
        onDismissRequest = {
            provisionManager.stopScan()
            scope.launch { provisionManager.cancel() }
            onDismiss()
        },
        onDismissed = {
            provisionManager.stopScan()
        },
        showTopBar = true,
        showBackButton = currentStep != BrainBoxStep.Scan,
        showCloseButton = true,
        onBack = { goPrevious() },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
            ) {
                BrainBoxStepIndicator(currentIndex = currentSheetStepIndex)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = currentStep.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF12192B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = currentStep.subtitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF595E6B),
                )

                Spacer(modifier = Modifier.height(24.dp))

                when (currentStep) {
                    BrainBoxStep.Scan -> {
                        BrainBoxScanStep(
                            controller = controller,
                            devices = bleDevices,
                            isBleScanning = isBleScanning,
                            selectedDevice = selectedBleDevice,
                            onSelectDevice = { selectedBleDevice = it },
                            onRequestPermission = ::requestBluetoothAgain,
                            onNext = {
                                val device = selectedBleDevice?.toBleScanDevice() ?: return@BrainBoxScanStep
                                // 已占用的设备 UI 层根本点不到，这里再做一次 guard，避免竞态
                                if (probeStates[device.id] is DeviceProbeState.Occupied) return@BrainBoxScanStep
                                connectingDeviceId = device.id
                                scope.launch {
                                    provisionManager.stopScan()
                                    provisionManager.connectDevice(device)
                                        .onSuccess {
                                            connectingDeviceId = null
                                            autoInjectPhoneWifiAfterConnect(
                                                provisionManager = provisionManager,
                                                controller = controller,
                                                wifiCredentialCache = wifiCredentialCache,
                                                toastState = toastState,
                                                onGoToBind = {
                                                    currentStep = BrainBoxStep.Bind
                                                    requestOtpAndBind()
                                                },
                                                onGoToWifiStep = {
                                                    currentStep = BrainBoxStep.Wifi
                                                },
                                                setWifiConnectedSsid = { wifiConnectedSsid = it },
                                                setSelectedWifiSsid = { selectedWifiSsid = it },
                                                setWifiPassword = { wifiPassword = it },
                                                setIsConnectingWifi = { isConnectingWifi = it },
                                            )
                                        }
                                        .onFailure { error ->
                                            connectingDeviceId = null
                                            val msg = error.message.orEmpty()
                                            toastState.show(
                                                if ("133" in msg || "timeout" in msg.lowercase() || "超时" in msg)
                                                    "连接失败，设备可能已被其他手机连接"
                                                else
                                                    msg.ifBlank { "连接设备失败" }
                                            )
                                            provisionManager.startScan()
                                        }
                                }
                            },
                            probeStates = probeStates,
                            connectingDeviceId = connectingDeviceId,
                            onStopScan = { provisionManager.stopScan() },
                        )
                    }
                    BrainBoxStep.Wifi -> {
                        BrainBoxWifiStep(
                            wifiMode = wifiMode,
                            wifiPermissionGranted = true,
                            selectedDevice = selectedBleDevice,
                            wifiNetworks = wifiNetworks,
                            isWifiLoading = isWifiLoading,
                            selectedWifiSsid = selectedWifiSsid,
                            onSelectWifi = { selectedWifiSsid = it },
                            manualSsid = manualSsid,
                            onManualSsidChange = { manualSsid = it },
                            wifiPassword = wifiPassword,
                            onWifiPasswordChange = { wifiPassword = it },
                            isConnectingWifi = isConnectingWifi,
                            onRefreshWifi = ::refreshWifiAgain,
                            onConnectWifi = ::connectWifi,
                            currentSsid = currentSsid,
                            isSelectedCurrent = isSelectedCurrent,
                        )
                    }
                    BrainBoxStep.Bind -> {
                        BrainBoxBindStep(
                            selectedDevice = selectedBleDevice,
                            serverDevice = displayDevice,
                            connectedWifi = wifiConnectedSsid,
                            isLoadingDevices = isLoadingDevices,
                            isBinding = isBinding,
                            onBind = ::requestOtpAndBind,
                        )
                    }
                }
            }

            ToastHost(
                state = toastState,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun BleScanDevice.toBrainBoxBleDevice(): BrainBoxBleDevice {
    return BrainBoxBleDevice(
        id = id,
        name = name,
        subtitle = subtitle,
        rssi = rssi,
    )
}

private fun BrainBoxBleDevice.toBleScanDevice(): BleScanDevice {
    return BleScanDevice(
        id = id,
        name = name,
        subtitle = subtitle,
        rssi = rssi,
    )
}

/**
 * BLE 连上脑花盒子后"自动把本机 Wi‑Fi 注入给设备"的入口。
 *
 * 根据「手机 Wi‑Fi 状态 × 本地密码缓存 × 设备当前联网 SSID」，把用户从尽量多的情况
 * 里接到 Bind 步骤，实在接不上才退回传统的 Wi‑Fi 手动步骤：
 *
 *  1. 手机 Wi‑Fi 关 → toast + 打开系统 Wi‑Fi 设置 → 进 Wi‑Fi 步兜底。
 *  2. 本机 SSID == 设备 network_status.ssid（设备已连）→ 直接 Bind，不下发 wifi_config。
 *  3. 本机 SSID 有本地缓存密码 → 自动 configureWifi → 成功进 Bind，失败回 Wi‑Fi 步。
 *  4. 本机 SSID 无缓存密码 → 进 Wi‑Fi 步，预选本机 SSID，让用户仅需输密码。
 *  5. 读不到本机 SSID（Unknown：iOS 缺 entitlement / Android 拒权限）→ 进 Wi‑Fi 步。
 */
private suspend fun autoInjectPhoneWifiAfterConnect(
    provisionManager: ProvisionManager,
    controller: BrainBoxProvisionController,
    wifiCredentialCache: WifiCredentialCache,
    toastState: ToastState,
    onGoToBind: () -> Unit,
    onGoToWifiStep: () -> Unit,
    setWifiConnectedSsid: (String?) -> Unit,
    setSelectedWifiSsid: (String) -> Unit,
    setWifiPassword: (String) -> Unit,
    setIsConnectingWifi: (Boolean) -> Unit,
) {
    val networkStatus = provisionManager.state.value.networkStatus
    val deviceSsid = networkStatus?.ssid?.trim()?.takeIf { it.isNotBlank() }
    val deviceConnected = networkStatus?.isConnected == true

    println(
        "[BrainBox] connectDevice 完成：设备 isConnected=$deviceConnected, " +
            "ssid=${deviceSsid ?: "<空>"}，开始读本机 Wi‑Fi 尝试自动注入"
    )

    when (val phoneWifi = controller.readCurrentPhoneWifi()) {
        is PhoneWifiState.Disabled -> {
            // 路径 1：手机 Wi‑Fi 未开。告知用户 + 打开系统 Wi‑Fi 设置；UI 兜底到 Wi‑Fi 步，
            // 让用户开启后回来可以继续（设备此刻若已联网，Wi‑Fi 步里会把那条 SSID 标"已连接"）。
            println("[BrainBox] 手机 Wi‑Fi 未开启，提示用户打开并兜底到 Wi‑Fi 步")
            toastState.show("请先开启手机 Wi‑Fi 并连接到目标网络")
            controller.openWifiSettings()
            if (deviceConnected && deviceSsid != null) setWifiConnectedSsid(deviceSsid)
            onGoToWifiStep()
        }
        is PhoneWifiState.Connected -> {
            val phoneSsid = phoneWifi.ssid
            println("[BrainBox] 读到本机 SSID=$phoneSsid，设备 SSID=${deviceSsid ?: "<空>"}")
            // 不管走哪条路径，都把 selectedWifiSsid 预选成本机 SSID，确保用户即便被退回
            // Wi‑Fi 步骤也能看到"预选上"的那一条。
            setSelectedWifiSsid(phoneSsid)

            // 路径 2：设备已连到本机同 SSID → 直接 Bind。
            if (deviceConnected && deviceSsid != null &&
                deviceSsid.equals(phoneSsid, ignoreCase = true)
            ) {
                println("[BrainBox] 设备已连到本机同 SSID ($phoneSsid)，跳过配网直接进入 Bind")
                setWifiConnectedSsid(deviceSsid)
                onGoToBind()
                return
            }

            // 路径 3：有缓存密码 → 自动下发。
            val cachedPwd = wifiCredentialCache.get(phoneSsid)
            if (cachedPwd != null) {
                setWifiPassword(cachedPwd)
                toastState.show("正在用本机 Wi‑Fi ($phoneSsid) 为设备配网…")
                setIsConnectingWifi(true)
                val result = provisionManager.configureWifi(ssid = phoneSsid, password = cachedPwd)
                setIsConnectingWifi(false)
                result.onSuccess { ns ->
                    println("[BrainBox] 自动配网成功 (ssid=${ns.ssid}, ip=${ns.ip})，进入 Bind")
                    setWifiConnectedSsid(ns.ssid.ifBlank { phoneSsid })
                    onGoToBind()
                }.onFailure { error ->
                    // 不主动清缓存：密码多数情况下仍正确，失败更可能是链路抖动。
                    // 把用户接到 Wi‑Fi 步（SSID + 密码已预填），让他"直接再点一次连接"即可重试。
                    println("[BrainBox] 自动配网失败: ${error.message}，回到 Wi‑Fi 步骤让用户手动确认")
                    toastState.show(error.message ?: "自动配网失败，请手动确认 Wi‑Fi 设置")
                    if (deviceConnected && deviceSsid != null) setWifiConnectedSsid(deviceSsid)
                    onGoToWifiStep()
                }
                return
            }

            // 路径 4：无缓存密码 → Wi‑Fi 步，SSID 预选。
            println("[BrainBox] 本机 SSID=$phoneSsid 无缓存密码，进入 Wi‑Fi 步等用户输入密码")
            if (deviceConnected && deviceSsid != null) setWifiConnectedSsid(deviceSsid)
            onGoToWifiStep()
        }
        is PhoneWifiState.Unknown -> {
            // 路径 5：读不到本机 SSID（iOS 缺 entitlement / Android 拒权限），兜底原有手动步骤。
            println("[BrainBox] 读不到本机 SSID（Unknown），进入 Wi‑Fi 步骤让用户手动配置")
            if (deviceConnected && deviceSsid != null) setWifiConnectedSsid(deviceSsid)
            onGoToWifiStep()
        }
    }
}

private fun GattWifiNetwork.toBrainBoxWifiNetwork(isCurrent: Boolean = false): BrainBoxWifiNetwork {
    return BrainBoxWifiNetwork(
        ssid = ssid,
        strengthLevel = when (val currentSignal = signal ?: 0) {
            in 80..100 -> 4
            in 60..79 -> 3
            in 40..59 -> 2
            in 20..39 -> 1
            else -> 0
        },
        isSecure = isSecure,
        isCurrent = isCurrent || active,
    )
}
