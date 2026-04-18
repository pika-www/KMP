package com.cephalon.lucyApp.deviceaccess.gatt

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
     * 协议步骤 7：向 lucy_pairing_request 写入 `{"request_id":"<millis>"}`，固定等待
     * [OTP_READ_AFTER_WRITE_DELAY_MS]（5s）后读取 `lucy_pairing_info`，以 `otp` 字段是否
     * 有值作为成功判据。
     *
     * ## 为什么不信任 write ACK
     * 协议文档声称「write ACK 返回时 OTP 已注入」，但实测 BrainBox 设备即便 write 返回
     * ATT 错误（常见 `code=128` APPLICATION_ERROR / `code=14` UNLIKELY_ERROR），Lucy IPC
     * 仍可能异步完成把 OTP 注入到 `lucy_pairing_info`。把 write ACK 当成败点会漏报成功，
     * 把正常流程搞成 forceReconnect 死循环。
     *
     * 新策略：
     * 1. 正常写入（仍用 WRITE_WITH_RESPONSE 以满足协议 handler 触发条件）
     * 2. **不论 write 是否报错**，等 5s 给设备端 Lucy 插件完成同步 IPC（文档标称 1–3s，
     *    5s 是宽松窗口）
     * 3. 主动 read `lucy_pairing_info`；
     *    - read 失败（status/att/timeout 类）→ 真正的 BLE 链路异常，返回 failure，由上层
     *      forceReconnect 接管
     *    - read 成功 → 看 payload：
     *      - `isBound=true` 或 `isOtpValid=true` → 成功
     *      - 其他 → 业务失败（设备端 IPC 没跑通），返回 failure 让 UI 展示"请检查设备"，
     *        **不触发** forceReconnect（因为重连解决不了 Lucy 插件未就绪等问题）
     *
     * ## 协议错误字符串对照（仅供排查设备端日志时用）
     * bluez 映射 `org.bluez.Error.Failed` → ATT 0x0E；部分设备走自定义映射 → 0x80 段：
     *  - `pairing ipc is not enabled`         → 设备未启用 `--lucy-pairing-sock`
     *  - `lucy pairing client is not connected` → Lucy 插件未连 socket
     *  - `pairing ipc: request timed out`     → 8s 内 Lucy 未返回 OTP
     *  - `pairing ipc: duplicate request id`  → `request_id` 判重（本实现用毫秒时间戳规避）
     *  - `pairing ipc: <Lucy 原始错误>`       → Lucy 侧业务失败
     * 这些字符串不会通过 ATT 回传客户端，细分仍需查设备端日志。
     *
     * ## OTP 有效期
     * 约 5 分钟（见 [LucyPairingInfoPayload.otpExpiresAtMs] / [LucyPairingInfoPayload.isOtpExpired]），
     * 过期后需重新调用本函数申请新 OTP。
     */
    suspend fun requestOtpAndReadPairingInfo(): Result<LucyPairingInfoPayload> {
        val requestId = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString()
        val payload = LucyPairingRequestPayload(requestId = requestId)

        // 1) 写入 lucy_pairing_request。write 的 ATT 错误仅告警不中断——后续读 pairing_info
        //    的结果才是终态判据。
        println("[BrainBox] lucy_pairing_request write (with response): request_id=$requestId")
        val writeResult = writeRoute(
            route = GattRoute.LucyPairingRequest,
            payload = dispatcher.toJsonObject(payload),
        )
        if (writeResult.isFailure) {
            println(
                "[BrainBox] WARN lucy_pairing_request write 返回错误但按策略继续等 " +
                    "${OTP_READ_AFTER_WRITE_DELAY_MS}ms 再读 pairing_info: " +
                    "${writeResult.exceptionOrNull()?.message}",
            )
        }

        // 2) 固定等 5s 给设备端 Lucy 插件完成 IPC。
        println("[BrainBox] 等待 ${OTP_READ_AFTER_WRITE_DELAY_MS}ms 后读 lucy_pairing_info")
        delay(OTP_READ_AFTER_WRITE_DELAY_MS)

        // 3) 主动 read pairing_info。read 失败 = BLE 链路异常，返回 failure 让 UI forceReconnect。
        val info = readLucyPairingInfo().getOrElse { readError ->
            println("[BrainBox] read lucy_pairing_info 失败: ${readError.message}")
            return Result.failure(readError)
        }
        println(
            "[BrainBox] pairing_info after ${OTP_READ_AFTER_WRITE_DELAY_MS}ms: " +
                "state=${info.state}, bindingStatus=${info.bindingStatus}, " +
                "otp=${info.otp.ifBlank { "<empty>" }}, otpExpiresAtMs=${info.otpExpiresAtMs}, " +
                "cdi=${info.channelDeviceId}",
        )

        // 4) 终态判定：设备已绑定 或 拿到未过期 OTP → 成功；否则业务失败。
        return if (info.isBound || info.isOtpValid) {
            Result.success(info)
        } else {
            Result.failure(
                IllegalStateException(
                    "设备未下发 OTP（otp=${info.otp.ifBlank { "空" }}, state=${info.state}, " +
                        "bindingStatus=${info.bindingStatus}），请检查设备配对服务",
                ),
            )
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
     * 协议步骤 6：向 wifi_config 写入凭证。调用方负责等待 5s 后读取 network_status。
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
        println("[BrainBox] writeRoute ${route.name}: props=${props?.toPropertyString() ?: "<none>"}, useWithResponse=$useWithResponse")

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

        /** 协议步骤 6：写入 wifi_config 后等待 5s 再读取 network_status。 */
        const val WIFI_CONFIG_WAIT_MS = 5_000L

        /**
         * 协议步骤 7：写完 lucy_pairing_request 后等多久再 read lucy_pairing_info。
         *
         * 设计依据：
         * - 协议文档标称 Lucy 插件同步 IPC 约 1–3s；5s 留足宽松窗口
         * - 不再依赖 write ACK（见 [requestOtpAndReadPairingInfo] KDoc），write 过早
         *   失败或未携带 OTP 信息时，这个 delay 给设备端一个稳定的 IPC 完成窗口
         * - 不做多轮轮询：5s 后一次 read 判定成败；真正的 BLE 链路问题走 forceReconnect
         */
        const val OTP_READ_AFTER_WRITE_DELAY_MS = 5_000L
    }
}
