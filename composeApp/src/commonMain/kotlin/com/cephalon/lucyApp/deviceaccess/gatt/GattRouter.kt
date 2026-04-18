package com.cephalon.lucyApp.deviceaccess.gatt

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject

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
     * 协议步骤 7：向 lucy_pairing_request 写入 {"request_id": "<millis>"}，等待 [delayMillis] 后单次读取 pairing_info。
     * 不做多轮轮询；如果读回的 pairing_info 没有 OTP（且设备未 bound），由调用方决定是否重试。
     */
    suspend fun requestOtpAndReadPairingInfo(
        delayMillis: Long = OTP_READ_DELAY_MS,
    ): Result<LucyPairingInfoPayload> {
        val requestId = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString()
        val payload = LucyPairingRequestPayload(requestId = requestId)
        println("[BrainBox] lucy_pairing_request write: request_id=$requestId")
        writeRoute(
            route = GattRoute.LucyPairingRequest,
            payload = dispatcher.toJsonObject(payload),
        ).getOrElse { return Result.failure(it) }
        println("[BrainBox] lucy_pairing_request: 等待 ${delayMillis}ms 后读取 pairing_info...")
        delay(delayMillis)
        return readLucyPairingInfo().onSuccess { info ->
            println("[BrainBox] pairing_info after OTP: state=${info.state}, bindingStatus=${info.bindingStatus}, otp=${info.otp}, otpExpiresAtMs=${info.otpExpiresAtMs}, cdi=${info.channelDeviceId}")
        }
    }

    /**
     * 协议步骤 5：向 wifi_scan 写入 {"action":"scan"}，然后轮询读回结果直到拿到 ready/failed
     * 或超过 [overallTimeoutMs]。
     *
     * 设计原因：WifiScan 特征用 WRITE_WITHOUT_RESPONSE，Android 侧可能在链路拥塞时静默丢掉
     * 首包，固件永远停在 idle 状态；同时不同网络环境下固件完成扫描的时间差异很大（从 <2s
     * 到 >10s 都有可能）。所以改为：
     *   1. 写入 scan 命令
     *   2. 首次读取前等 [initialWaitMs]（默认 1.5s）让固件有时间进入 scanning
     *   3. 每 [pollIntervalMs]（默认 1.5s）读一次 state
     *   4. 若直到 [idleResendDeadlineMs]（默认 6s）state 一直是空/idle，重新写一次 scan
     *      命令（兜底首包丢失）
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
                println("[BrainBox] wifi_scan 在 ${elapsed}ms 仍是 idle，重发一次 scan 命令兜底首包丢失")
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
        return if (route.writeWithResponse) {
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

    private suspend fun toggleNotify(route: GattRoute, enabled: Boolean): Result<Unit> {
        val connection = requireConnection()
        return connection.setCharacteristicNotification(
            serviceUuid = BrainBoxGattProtocol.SERVICE_UUID,
            characteristicUuid = route.characteristicUuid,
            enabled = enabled,
        )
    }

    companion object {
        /** 协议步骤 5：写入 wifi_scan 后先等 1.5s 让固件进入 scanning，然后开始轮询。 */
        const val WIFI_SCAN_INITIAL_WAIT_MS = 1_500L

        /** 协议步骤 5：轮询间隔 1.5s（上一个 read 完成后等多久读下一次）。 */
        const val WIFI_SCAN_POLL_INTERVAL_MS = 1_500L

        /**
         * 协议步骤 5：从首次写入到 6s 内如果 state 一直是空/idle，兜底重发一次 scan 命令
         * （WRITE_WITHOUT_RESPONSE 首包在链路拥塞时可能静默丢失）。
         */
        const val WIFI_SCAN_IDLE_RESEND_DEADLINE_MS = 6_000L

        /** 协议步骤 5：整体超时上限 20s；仍未 ready 就抛错，让 UI 或调用方决定重试。 */
        const val WIFI_SCAN_OVERALL_TIMEOUT_MS = 20_000L

        /** 协议步骤 6：写入 wifi_config 后等待 5s 再读取 network_status。 */
        const val WIFI_CONFIG_WAIT_MS = 5_000L

        /** 协议步骤 7：写入 lucy_pairing_request 后等待 3s 再读取 pairing_info。 */
        const val OTP_READ_DELAY_MS = 3_000L
    }
}
