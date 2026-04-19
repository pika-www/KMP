package com.cephalon.lucyApp.deviceaccess.gatt

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject

// BLE 特性 property 位掩码，Android BluetoothGattCharacteristic.PROPERTY_* 与
// iOS CBCharacteristicProperties 在这些位上定义一致。
private const val PROP_READ = 0x02
private const val PROP_WRITE_NO_RESPONSE = 0x04
private const val PROP_WRITE = 0x08
private const val PROP_NOTIFY = 0x10
private const val PROP_INDICATE = 0x20

private fun Int.toPropertyString(): String = buildList {
    if (this@toPropertyString and PROP_READ != 0) add("READ")
    if (this@toPropertyString and PROP_WRITE != 0) add("WRITE")
    if (this@toPropertyString and PROP_WRITE_NO_RESPONSE != 0) add("WRITE_NR")
    if (this@toPropertyString and PROP_NOTIFY != 0) add("NOTIFY")
    if (this@toPropertyString and PROP_INDICATE != 0) add("INDICATE")
}.joinToString("|").ifEmpty { "NONE" }

class GattRouter(
    private val connectionManager: GattConnectionManager,
    private val dispatcher: JsonCommandDispatcher,
) {
    private fun requireConnection(): BleGattConnection {
        return connectionManager.currentConnection()
            ?: throw IllegalStateException("尚未建立 GATT 连接")
    }

    suspend fun discoverCharacteristics(): Result<List<BleGattCharacteristic>> {
        return connectionManager.discoverProtocol()
    }

    suspend fun readDeviceInfo(): Result<DeviceInfoPayload> {
        return readRoute(GattRoute.DeviceInfo)
    }

    suspend fun readLucyPairingInfo(): Result<LucyPairingInfoPayload> {
        return readRoute(GattRoute.LucyPairingInfo)
    }

    /**
     * 协议步骤 7：订阅 `lucy_pairing_info` 的 notify → 写 `lucy_pairing_request` → 等 notify 推回
     * 含 OTP 的 payload。Notify 未到时兜底 read，enable notify 失败时降级为"固定 delay + read"。
     *
     * ## 为什么用 notify 替代固定 delay + read
     * 目标硬件（Intel AX210 Wi-Fi 6E + BT 5.3 combo 模块）的射频前端共享,gateway 的
     * lucy-im-sdk presence 心跳（10s 一次）+ HTTP/2 长连接几乎不给 BLE 留空窗,BLE 每
     * 10~20s 被 Wi-Fi 抢占一次。旧实现需要 BLE 连续在线 ≥5s 才能完成 write→delay(5s)→read,
     * 在这种硬件下几乎总在 delay 区间掉线 → UI forceReconnect → 死循环。
     *
     * 新流程把 BLE 必需在线时间从 ≥5s 砍到 ~1-2s：
     *   1. `enableLucyPairingInfoNotify()` 写 CCCD（和 attemptReconnect 的 enable 幂等）
     *   2. `CoroutineStart.UNDISPATCHED` 在本线程就位一个 `first()` 订阅,
     *      **保证订阅在 write 触发的 notify 到达之前已生效**
     *   3. write `lucy_pairing_request`（服务端 InjectOtp 后会 setValue →
     *      emitPropertiesChanged,BlueZ 把新值 notify 给客户端）
     *   4. 等第一个 `isBound || isOtpValid` 的 notify 或 [OTP_NOTIFY_TIMEOUT_MS] 超时
     *   5. 命中 → 立刻返回；超时 → 兜底短 delay + read（处理服务端确实没 notify 的场景）
     *   6. 一开始 enable notify 就失败 → 走旧路径 [OTP_READ_AFTER_WRITE_DELAY_MS] + read,
     *      保底兼容不支持 notify 的设备。
     *
     * ## 为什么不信任 write ACK
     * 实测服务端即便 write 返回 ATT 错误（code=128/14）,Lucy IPC 仍可能异步完成 OTP 注入；
     * write 失败**不中断**,仍按 notify/read 终态判定。
     *
     * ## 协议错误字符串对照（仅供排查设备端日志时用）
     *  - `pairing ipc is not enabled`         → 设备未启用 `--lucy-pairing-sock`
     *  - `lucy pairing client is not connected` → Lucy 插件未连 socket
     *  - `pairing ipc: request timed out`     → 8s 内 Lucy 未返回 OTP
     *  - `pairing ipc: duplicate request id`  → `request_id` 判重（毫秒时间戳规避）
     *  - `pairing ipc: <Lucy 原始错误>`       → Lucy 侧业务失败
     *
     * ## OTP 有效期
     * 约 5 分钟（见 [LucyPairingInfoPayload.otpExpiresAtMs] / [LucyPairingInfoPayload.isOtpExpired]）,
     * 过期后需重新调用本函数申请新 OTP。
     */
    suspend fun requestOtpAndReadPairingInfo(): Result<LucyPairingInfoPayload> = coroutineScope {
        val requestId = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString()
        val payload = LucyPairingRequestPayload(requestId = requestId)

        // 1) 首选 notify 路径：CCCD 写入幂等,enable 失败时降级到"固定 delay + read"。
        val enableNotifyResult = enableLucyPairingInfoNotify()
        val notifyEnabled = enableNotifyResult.isSuccess
        if (!notifyEnabled) {
            println(
                "[BrainBox] enableLucyPairingInfoNotify 失败,降级到固定 delay + read 路径: " +
                    "${enableNotifyResult.exceptionOrNull()?.message}",
            )
        }

        // 2) UNDISPATCHED：让 observer.collect 在本线程立即进入等待,避免 write 触发的
        //    notify 先于 await 到达而被丢掉。仅在 notify enable 成功时订阅。
        val observer = if (notifyEnabled) {
            async(start = CoroutineStart.UNDISPATCHED) {
                observeLucyPairingInfo()
                    .filter { it.isBound || it.isOtpValid }
                    .first()
            }
        } else {
            null
        }

        try {
            // 3) 写 lucy_pairing_request。write 失败仍按策略继续等 notify/fallback 判定终态。
            println("[BrainBox] lucy_pairing_request write (with response): request_id=$requestId")
            val writeResult = writeRoute(
                route = GattRoute.LucyPairingRequest,
                payload = dispatcher.toJsonObject(payload),
            )
            if (writeResult.isFailure) {
                println(
                    "[BrainBox] WARN lucy_pairing_request write 返回错误但继续等 notify/fallback read: " +
                        "${writeResult.exceptionOrNull()?.message}",
                )
            }

            // 4) 首选路径：等 notify 推送。observer == null 说明 enable 未成功,直接跳到降级。
            val viaNotify = if (observer != null) {
                println("[BrainBox] 等待 lucy_pairing_info notify (最长 ${OTP_NOTIFY_TIMEOUT_MS}ms)...")
                withTimeoutOrNull(OTP_NOTIFY_TIMEOUT_MS) { observer.await() }
            } else {
                null
            }
            if (viaNotify != null) {
                println(
                    "[BrainBox] pairing_info via notify: state=${viaNotify.state}, " +
                        "bindingStatus=${viaNotify.bindingStatus}, " +
                        "otp=${viaNotify.otp.ifBlank { "<empty>" }}, " +
                        "otpExpiresAtMs=${viaNotify.otpExpiresAtMs}, cdi=${viaNotify.channelDeviceId}",
                )
                return@coroutineScope Result.success(viaNotify)
            }

            // 5) 兜底：notify 未到或未订阅。若 notify 已订阅仅短等 [OTP_FALLBACK_READ_DELAY_MS]
            //    再 read（合理假设服务端已注入但没 emit notify）；否则按旧的宽松窗口。
            val fallbackWaitMs = if (notifyEnabled) OTP_FALLBACK_READ_DELAY_MS else OTP_READ_AFTER_WRITE_DELAY_MS
            println("[BrainBox] notify 未达,等待 ${fallbackWaitMs}ms 后兜底 read lucy_pairing_info")
            delay(fallbackWaitMs)

            val info = readLucyPairingInfo().getOrElse { readError ->
                println("[BrainBox] fallback read lucy_pairing_info 失败: ${readError.message}")
                return@coroutineScope Result.failure(readError)
            }
            println(
                "[BrainBox] pairing_info fallback read: state=${info.state}, " +
                    "bindingStatus=${info.bindingStatus}, otp=${info.otp.ifBlank { "<empty>" }}, " +
                    "otpExpiresAtMs=${info.otpExpiresAtMs}, cdi=${info.channelDeviceId}",
            )

            if (info.isBound || info.isOtpValid) {
                Result.success(info)
            } else {
                Result.failure(
                    IllegalStateException(
                        "设备未下发 OTP（otp=${info.otp.ifBlank { "空" }}, state=${info.state}, " +
                            "bindingStatus=${info.bindingStatus}）,请检查设备配对服务",
                    ),
                )
            }
        } finally {
            observer?.cancel()
            if (notifyEnabled) {
                runCatching { disableLucyPairingInfoNotify() }
            }
        }
    }

    /**
     * 协议步骤 5：向 wifi_scan 写入 {"action":"scan"}，然后轮询读回结果直到拿到 ready/failed
     * 或超过 [overallTimeoutMs]。
     *
     * 设计原因：wifi_scan 写入使用 WRITE_WITH_RESPONSE（协议要求），写成功即设备已收到命令；
     * 但不同网络环境下固件完成扫描的时间差异很大（从 <2s 到 >10s 都有可能），
     * 因此不能用固定延时单次读取：
     *   1. 写入 scan 命令（带 Response，写成功即表明设备收到）
     *   2. 首次读取前等 [initialWaitMs]（默认 5s，对齐协议标称时间）让固件完成扫描
     *   3. 若首读已经是 ready 就直接返回；否则每 [pollIntervalMs]（默认 1.5s）补读一次
     *   4. 若直到 [idleResendDeadlineMs]（默认 6s）state 仍为空/idle，防御性再补发一次 scan
     *      命令（正常 WRITE_WITH_RESPONSE 链路下不会进入，仅当固件丢失命令时写送）
     *   5. 命中 ready 就返回网络列表；命中 failed 就报错；超时也报错
     *
     * 不重置"已经重发过一次"的标志——最多只会重发一次，避免无限循环。
     */
    suspend fun scanWifi(
        initialWaitMs: Long = WIFI_SCAN_INITIAL_WAIT_MS,
        pollIntervalMs: Long = WIFI_SCAN_POLL_INTERVAL_MS,
        idleResendDeadlineMs: Long = WIFI_SCAN_IDLE_RESEND_DEADLINE_MS,
        overallTimeoutMs: Long = WIFI_SCAN_OVERALL_TIMEOUT_MS,
    ): Result<List<GattWifiNetwork>> {
        val scanCmd = WifiScanCommand()
        val scanJson = dispatcher.toJsonObject(scanCmd)
        println("[BrainBox] wifi_scan write (first attempt): $scanJson")
        writeRoute(route = GattRoute.WifiScan, payload = scanJson).getOrElse {
            println("[BrainBox] wifi_scan write failed: ${it.message}")
            return Result.failure(it)
        }
        println("[BrainBox] wifi_scan: 等待 ${initialWaitMs}ms 后开始轮询结果...")
        delay(initialWaitMs)

        val startMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        var resendDone = false
        var attempt = 0

        while (true) {
            attempt++
            val elapsed = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - startMs

            val response = readRoute<WifiScanResponse>(GattRoute.WifiScan).getOrElse {
                println("[BrainBox] wifi_scan poll#$attempt read failed: ${it.message}")
                return Result.failure(it)
            }
            val stateLower = response.state.trim().lowercase()
            println(
                "[BrainBox] wifi_scan poll#$attempt (elapsed=${elapsed}ms): state=${response.state}, " +
                    "networks=${response.networks.size}, error=${response.error}",
            )

            if (response.isReady) {
                println("[BrainBox] wifi_scan ready，返回 ${response.networks.size} 个网络")
                return Result.success(response.networks)
            }
            if (response.isFailed) {
                return Result.failure(IllegalStateException(response.error.ifBlank { "设备 Wi‑Fi 扫描失败" }))
            }

            // 仍在 idle / scanning / 空字符串：判断是否需要重发一次 scan 命令
            val isIdle = stateLower.isEmpty() || stateLower == "idle"
            if (isIdle && !resendDone && elapsed >= idleResendDeadlineMs) {
                println("[BrainBox] wifi_scan 在 ${elapsed}ms 仍是 idle，防御性再补发一次 scan 命令（固件可能丢失命令）")
                writeRoute(route = GattRoute.WifiScan, payload = scanJson).onFailure {
                    println("[BrainBox] wifi_scan resend failed: ${it.message}")
                }
                resendDone = true
            }

            if (elapsed >= overallTimeoutMs) {
                return Result.failure(
                    IllegalStateException(
                        "Wi‑Fi 扫描超时（${overallTimeoutMs}ms），设备最后状态=${response.state.ifBlank { "idle" }}",
                    ),
                )
            }
            delay(pollIntervalMs)
        }
        @Suppress("UNREACHABLE_CODE")
        return Result.failure(IllegalStateException("Wi‑Fi 扫描逻辑异常退出"))
    }

    suspend fun readWifiScanResult(): Result<WifiScanResponse> {
        return readRoute(GattRoute.WifiScan)
    }

    /**
     * 协议步骤 6：向 wifi_config 写入凭证。
     *
     * 调用方（[com.cephalon.lucyApp.deviceaccess.gatt.ProvisionManager.configureWifi]）
     * 的新协议：write 之前先预读一次 network_status（快通道）；write 之后订阅
     * network_status 通知等第一个 connected 事件，超时再做一次 fallback read。
     * 不再有固定 5s 延时。
     */
    suspend fun writeWifiConfig(
        ssid: String,
        password: String,
        hidden: Boolean = false,
    ): Result<Unit> {
        val cmd = WifiConfigCommand(ssid = ssid, password = password, hidden = hidden)
        println("[BrainBox] wifi_config write: ${dispatcher.toJsonObject(cmd)}")
        return writeRoute(
            route = GattRoute.WifiConfig,
            payload = dispatcher.toJsonObject(cmd),
        )
    }

    suspend fun readNetworkStatus(): Result<NetworkStatusPayload> {
        return readRoute(GattRoute.NetworkStatus)
    }

    suspend fun enableNetworkStatusNotify(): Result<Unit> {
        return toggleNotify(GattRoute.NetworkStatus, enabled = true)
    }

    suspend fun disableNetworkStatusNotify(): Result<Unit> {
        return toggleNotify(GattRoute.NetworkStatus, enabled = false)
    }

    fun observeNetworkStatus(): Flow<NetworkStatusPayload> {
        val connection = requireConnection()
        return connection.observeCharacteristic(
            serviceUuid = BrainBoxGattProtocol.SERVICE_UUID,
            characteristicUuid = GattRoute.NetworkStatus.characteristicUuid,
        ).map { bytes -> dispatcher.decode<NetworkStatusPayload>(bytes) }
    }

    fun observeLucyPairingInfo(): Flow<LucyPairingInfoPayload> {
        val connection = requireConnection()
        return connection.observeCharacteristic(
            serviceUuid = BrainBoxGattProtocol.SERVICE_UUID,
            characteristicUuid = GattRoute.LucyPairingInfo.characteristicUuid,
        ).map { bytes -> dispatcher.decode<LucyPairingInfoPayload>(bytes) }
    }

    suspend fun readJson(route: GattRoute): Result<RoutedJsonPayload> {
        return readJsonObject(route).map { RoutedJsonPayload(route = route, payload = it) }
    }

    suspend fun writeJson(route: GattRoute, payload: JsonObject): Result<Unit> {
        return writeRoute(route, payload)
    }

    private suspend inline fun <reified T> readRoute(route: GattRoute): Result<T> {
        return readJsonObject(route).map { dispatcher.decode<T>(it) }
    }

    private suspend fun readJsonObject(route: GattRoute): Result<JsonObject> {
        val connection = requireConnection()
        return connection.readCharacteristic(
            serviceUuid = BrainBoxGattProtocol.SERVICE_UUID,
            characteristicUuid = route.characteristicUuid,
        ).map { bytes -> dispatcher.decodeJsonObject(bytes) }
    }

    private suspend fun writeRoute(route: GattRoute, payload: JsonObject): Result<Unit> {
        val connection = requireConnection()
        val data = dispatcher.encodeJsonObject(payload)

        // 自适应选择写入类型：按 route 声明优先，但若设备实际只暴露另一种写入模式则回退，
        // 防止 "协议说要带 response / 设备只支持 no-response" 这种错配直接撞上 ATT 错误。
        val props = findCharacteristicProperties(route)
        val supportsWrite = props != null && (props and PROP_WRITE) != 0
        val supportsWriteNR = props != null && (props and PROP_WRITE_NO_RESPONSE) != 0
        val useWithResponse = when {
            props == null -> {
                println("[BrainBox] writeRoute ${route.name}: 设备未声明任何 property（服务未发现？），按 route.writeWithResponse=${route.writeWithResponse} 尝试")
                route.writeWithResponse
            }
            route.writeWithResponse && supportsWrite -> true
            route.writeWithResponse && supportsWriteNR -> {
                println("[BrainBox] WARN writeRoute ${route.name}: 协议要求 WRITE_WITH_RESPONSE，但设备只声明 ${props.toPropertyString()}，回退到 WRITE_NO_RESPONSE")
                false
            }
            !route.writeWithResponse && supportsWriteNR -> false
            !route.writeWithResponse && supportsWrite -> {
                println("[BrainBox] NOTE writeRoute ${route.name}: 协议期望 WRITE_NO_RESPONSE，但设备只声明 ${props.toPropertyString()}，使用 WRITE_WITH_RESPONSE")
                true
            }
            supportsWrite -> true
            supportsWriteNR -> false
            else -> {
                println("[BrainBox] WARN writeRoute ${route.name}: 设备未声明任何 write property（props=${props.toPropertyString()}），按 route.writeWithResponse=${route.writeWithResponse} 强行尝试")
                route.writeWithResponse
            }
        }
        println("[BrainBox] writeRoute ${route.name}: props=${props?.toPropertyString() ?: "<none>"}, useWithResponse=$useWithResponse, totalBytes=${data.size}")

        // 改动 4：撤销应用层 20 字节分片循环。BLE 分片是 ATT/L2CAP 层的职责，不应在应用层切。
        //
        // - iOS Core Bluetooth 的 writeValue(type:.withResponse) 对超出 ATT MTU 的 payload
        //   自动走 Prepare Write + Execute Write，服务端只会看到一次 WriteValue。
        // - Android 的 BluetoothGatt.writeCharacteristic(WRITE_TYPE_DEFAULT) 同样由系统
        //   栈组装；如需单包发更大 payload 可在连接后先 requestMtu(185) 抬高上限。
        //
        // 先前按 20B 做应用层分片循环的后果：每片都会独立触发一次 ATT Write Request，
        // 服务端 BlueZ 把每片作为独立的 WriteValue 回调交给 agent：
        //   - wifi_config 的 JSON 被切成 3 段，agent 对每段独立 json.Unmarshal，
        //     在服务端日志里显示 3 次 "invalid wifi config json"。
        //   - lucy_pairing_request 的 {"request_id":"..."} 被切成 2 段，每段都独立
        //     触发 requestPairingOtp()，导致一次 UI 请求变成两次云端 pre-bind、
        //     日志里成对出现 "pairing-ipc: otp injected request_id=ble-...-1/-2"。
        return if (useWithResponse) {
            connection.writeCharacteristic(
                serviceUuid = BrainBoxGattProtocol.SERVICE_UUID,
                characteristicUuid = route.characteristicUuid,
                payload = data,
            )
        } else {
            connection.writeCharacteristicNoResponse(
                serviceUuid = BrainBoxGattProtocol.SERVICE_UUID,
                characteristicUuid = route.characteristicUuid,
                payload = data,
            )
        }
    }

    private fun findCharacteristicProperties(route: GattRoute): Int? {
        val connection = connectionManager.currentConnection() ?: return null
        return connection.services.value
            .firstOrNull { it.uuid.equals(BrainBoxGattProtocol.SERVICE_UUID, ignoreCase = true) }
            ?.characteristics
            ?.firstOrNull { it.uuid.equals(route.characteristicUuid, ignoreCase = true) }
            ?.properties
    }

    /**
     * 打印 BrainBox 服务下所有特性的声明 property flags，用于排查 "Unknown ATT error" 是否由
     * 客户端写入类型 (WRITE / WRITE_NO_RESPONSE) 与设备声明不匹配导致。建议在 discoverServices
     * 成功后调一次。
     */
    fun logCharacteristicProperties() {
        val connection = connectionManager.currentConnection() ?: run {
            println("[BrainBox] logCharacteristicProperties: 当前无活动连接，跳过")
            return
        }
        val service = connection.services.value
            .firstOrNull { it.uuid.equals(BrainBoxGattProtocol.SERVICE_UUID, ignoreCase = true) }
        if (service == null) {
            println("[BrainBox] logCharacteristicProperties: 未发现 BrainBox 服务 ${BrainBoxGattProtocol.SERVICE_UUID}")
            return
        }
        println("[BrainBox] ===== BrainBox 服务特性属性清单 =====")
        GattRoute.values().forEach { route ->
            val char = service.characteristics.firstOrNull { it.uuid.equals(route.characteristicUuid, ignoreCase = true) }
            if (char == null) {
                println("[BrainBox]   ${route.name} (${route.characteristicUuid}): NOT FOUND")
            } else {
                val supportsWrite = (char.properties and PROP_WRITE) != 0
                val supportsWriteNR = (char.properties and PROP_WRITE_NO_RESPONSE) != 0
                val mismatch = when {
                    route.writeWithResponse && !supportsWrite && supportsWriteNR -> " [MISMATCH: route 要求 WRITE，设备只有 WRITE_NR]"
                    !route.writeWithResponse && !supportsWriteNR && supportsWrite -> " [MISMATCH: route 要求 WRITE_NR，设备只有 WRITE]"
                    else -> ""
                }
                println("[BrainBox]   ${route.name}: props=${char.properties.toPropertyString()} (0x${char.properties.toString(16)}), routeWriteWithResponse=${route.writeWithResponse}$mismatch")
            }
        }
        println("[BrainBox] =====================================")
    }

    private suspend fun toggleNotify(route: GattRoute, enabled: Boolean): Result<Unit> {
        val connection = requireConnection()
        return connection.setCharacteristicNotification(
            serviceUuid = BrainBoxGattProtocol.SERVICE_UUID,
            characteristicUuid = route.characteristicUuid,
            enabled = enabled,
        )
    }

    /**
     * 供外部（如 ProvisionManager 的重连流程）在 UI 触发写入之前主动启用
     * `lucy_pairing_info` 的 notify 通道。底层会写 CCCD 并等待 onDescriptorWrite 回调；
     * 失败会返回 Result.failure，调用方可以据此判断连接是否真的就绪。
     *
     * 注意：`requestOtpAndReadPairingInfo` 内部仍会做一次 enable → write → await → disable
     * 的 per-call 生命周期，所以即便这里已经 enable，也不会造成 double-subscribe 的问题
     * （CCCD 重复写入同样的值 0x0100 在设备侧是幂等的）。
     */
    suspend fun enableLucyPairingInfoNotify(): Result<Unit> =
        toggleNotify(GattRoute.LucyPairingInfo, enabled = true)

    suspend fun disableLucyPairingInfoNotify(): Result<Unit> =
        toggleNotify(GattRoute.LucyPairingInfo, enabled = false)

    companion object {
        /**
         * 协议步骤 5：写入 wifi_scan 后先等 5s（对齐协议标称扫描时间）再读取，Happy Path 即 write → 5s → 读一次。
         * 若固件还没 ready，则进入 [WIFI_SCAN_POLL_INTERVAL_MS] 轮询补读。
         */
        const val WIFI_SCAN_INITIAL_WAIT_MS = 5_000L

        /** 协议步骤 5：首读未 ready 时的补读间隔 1.5s（上一次 read 完成后等多久读下一次）。 */
        const val WIFI_SCAN_POLL_INTERVAL_MS = 1_500L

        /**
         * 协议步骤 5：首读后若 6s 内 state 仍为空/idle，兜底再补发一次 scan 命令（防御性，
         * 正常 WRITE_WITH_RESPONSE 链路下不会用到；仅当固件丢失命令时写送）。
         */
        const val WIFI_SCAN_IDLE_RESEND_DEADLINE_MS = 6_000L

        /** 协议步骤 5：整体超时上限 20s；仍未 ready 就抛错，让 UI 或调用方决定重试。 */
        const val WIFI_SCAN_OVERALL_TIMEOUT_MS = 20_000L

        /**
         * 协议步骤 7 的首选路径：等 `lucy_pairing_info` notify 推送的上限。
         *
         * 设计依据：
         * - Lucy 插件同步 IPC 标称 1–3s；8s 留足余量覆盖 HTTP/2 pre-bind 慢回合
         * - 超时即视为"notify 没到",走 [OTP_FALLBACK_READ_DELAY_MS] 兜底 read
         * - 在 AX210 combo 芯片等"BLE 每 10~20s 被 Wi-Fi 挤掉一次"的硬件下,把
         *   首选路径封顶在 8s 是为了尽量落在 BLE 在线间歇期内
         */
        const val OTP_NOTIFY_TIMEOUT_MS = 8_000L

        /**
         * 协议步骤 7 的 notify 兜底路径：notify 已订阅但未按期到达时,短等后再 read。
         *
         * 设计依据：
         * - notify 未达 ≠ OTP 没注入；此时服务端端可能已经 InjectOtp 但 emit notify 失败
         *   或被 BLE 链路抖动吞掉。短等 500ms 给可能的竞态窗口一个机会,再主动 read。
         * - 比旧的 [OTP_READ_AFTER_WRITE_DELAY_MS]（5s）激进,前提是 notify 已订阅
         */
        const val OTP_FALLBACK_READ_DELAY_MS = 500L

        /**
         * 协议步骤 7 的降级路径：当 `enableLucyPairingInfoNotify()` 一开始就失败（设备
         * 未声明 NOTIFY / CCCD 写异常）时,退回到旧版"固定 delay + read"行为。
         *
         * 设计依据：协议文档标称 Lucy 插件 IPC 约 1–3s；5s 是兼容降级的宽松窗口。
         */
        const val OTP_READ_AFTER_WRITE_DELAY_MS = 5_000L
    }
}
