package com.cephalon.lucyApp.deviceaccess.gatt

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
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

    suspend fun requestOtp(): Result<Unit> {
        val requestId = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString()
        val payload = LucyPairingRequestPayload(requestId = requestId)
        println("[BrainBox] requestOtp write: request_id=$requestId")
        return writeRoute(
            route = GattRoute.LucyPairingRequest,
            payload = dispatcher.toJsonObject(payload),
        )
    }

    suspend fun requestOtpAndAwaitPairingInfo(
        delayMillis: Long = OTP_READ_DELAY_MS,
        maxRetries: Int = OTP_POLL_MAX_RETRIES,
    ): Result<LucyPairingInfoPayload> {
        // 先读取当前 pairing_info 和 network_status，确认设备状态
        val preInfo = readLucyPairingInfo().getOrNull()
        println("[BrainBox] requestOtp 前状态: state=${preInfo?.state}, bindingStatus=${preInfo?.bindingStatus}, otp=${preInfo?.otp}, cdi=${preInfo?.channelDeviceId}")
        val netStatus = readRoute<NetworkStatusPayload>(GattRoute.NetworkStatus).getOrNull()
        println("[BrainBox] requestOtp 前网络: state=${netStatus?.state}, ssid=${netStatus?.ssid}, ip=${netStatus?.ip}")

        // 如果已有有效 OTP，直接返回
        if (preInfo != null && preInfo.otp.isNotBlank() && !preInfo.isOtpExpired) {
            println("[BrainBox] 设备已有有效 OTP，直接返回")
            return Result.success(preInfo)
        }

        // 设备已绑定且无 OTP → 固件不允许再次请求 OTP，直接返回（上层用 CDI 跳转）
        if (preInfo != null && preInfo.isBound && preInfo.channelDeviceId.isNotBlank()) {
            println("[BrainBox] 设备已绑定 (bindingStatus=${preInfo.bindingStatus}, cdi=${preInfo.channelDeviceId})，跳过 OTP 请求")
            return Result.success(preInfo)
        }

        // Write With Response: 写入阻塞直到服务端处理完成，OTP 已注入 pairing_info
        requestOtp().getOrElse { return Result.failure(it) }
        println("[BrainBox] requestOtp 写入成功（Write With Response），立即读取 pairing_info...")
        val immediateInfo = readLucyPairingInfo().getOrNull()
        println("[BrainBox] requestOtp 写入后 pairing_info: state=${immediateInfo?.state}, otp=${immediateInfo?.otp}, otpExpiresAtMs=${immediateInfo?.otpExpiresAtMs}, bindingStatus=${immediateInfo?.bindingStatus}, cdi=${immediateInfo?.channelDeviceId}")
        if (immediateInfo != null && immediateInfo.otp.isNotBlank()) {
            return Result.success(immediateInfo)
        }

        // 轮询读取 pairing_info，直到获取到有效 OTP 或达到最大重试次数
        repeat(maxRetries) { attempt ->
            val pairingInfo = readLucyPairingInfo().getOrElse { return Result.failure(it) }
            println("[BrainBox] requestOtp result (attempt ${attempt + 1}/$maxRetries): otp=${pairingInfo.otp}, otpExpiresAtMs=${pairingInfo.otpExpiresAtMs}, bindingStatus=${pairingInfo.bindingStatus}")
            if (pairingInfo.otp.isNotBlank()) {
                return Result.success(pairingInfo)
            }
            if (attempt < maxRetries - 1) {
                println("[BrainBox] requestOtp: OTP 为空，等待 ${OTP_POLL_INTERVAL_MS}ms 后重试...")
                delay(OTP_POLL_INTERVAL_MS)
            }
        }
        // 最后一次兜底读取
        val finalInfo = readLucyPairingInfo().getOrElse { return Result.failure(it) }
        println("[BrainBox] requestOtp final: otp=${finalInfo.otp}, otpExpiresAtMs=${finalInfo.otpExpiresAtMs}")
        if (finalInfo.otp.isBlank()) {
            return Result.failure(IllegalStateException("写入 request_id 后多次读取 pairing_info 仍未获取到 OTP"))
        }
        return Result.success(finalInfo)
    }

    suspend fun scanWifi(
        timeoutMillis: Long = DEFAULT_WIFI_SCAN_TIMEOUT_MS,
        pollIntervalMillis: Long = DEFAULT_WIFI_SCAN_POLL_INTERVAL_MS,
    ): Result<List<GattWifiNetwork>> {
        val scanCmd = WifiScanCommand()
        println("[BrainBox] wifi_scan write: ${dispatcher.toJsonObject(scanCmd)}")
        writeRoute(
            route = GattRoute.WifiScan,
            payload = dispatcher.toJsonObject(scanCmd),
        ).getOrElse { return Result.failure(it) }
        println("[BrainBox] wifi_scan: 等待 ${INITIAL_DELAY_BEFORE_READ_MS}ms 后开始轮询...")
        delay(INITIAL_DELAY_BEFORE_READ_MS)
        val response = awaitWifiScanResult(
            timeoutMillis = timeoutMillis,
            pollIntervalMillis = pollIntervalMillis,
        ).getOrElse { return Result.failure(it) }
        return Result.success(response.networks)
    }

    suspend fun readWifiScanResult(): Result<WifiScanResponse> {
        return readRoute(GattRoute.WifiScan)
    }

    suspend fun writeWifiConfig(
        ssid: String,
        password: String,
        hidden: Boolean = false,
    ): Result<Unit> {
        val cmd = WifiConfigCommand(ssid = ssid, password = password, hidden = hidden)
        println("[BrainBox] wifi_config write: ${dispatcher.toJsonObject(cmd)}")
        val result = writeRoute(
            route = GattRoute.WifiConfig,
            payload = dispatcher.toJsonObject(cmd),
        )
        if (result.isSuccess) {
            println("[BrainBox] wifi_config: 写入成功，等待 ${WIFI_CONFIG_READ_DELAY_MS}ms 后轮询 network_status...")
            delay(WIFI_CONFIG_READ_DELAY_MS)
        }
        return result
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

    private suspend fun awaitWifiScanResult(
        timeoutMillis: Long,
        pollIntervalMillis: Long,
    ): Result<WifiScanResponse> {
        val response = withTimeoutOrNull(timeoutMillis) {
            pollWifiScanUntilReady(pollIntervalMillis)
        }
        return if (response != null) {
            Result.success(response)
        } else {
            Result.failure(IllegalStateException("等待设备 Wi‑Fi 扫描结果超时"))
        }
    }

    private suspend fun pollWifiScanUntilReady(
        pollIntervalMillis: Long,
    ): WifiScanResponse {
        var consecutiveReadFailures = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            val current = readRoute<WifiScanResponse>(GattRoute.WifiScan).getOrElse { error ->
                consecutiveReadFailures++
                println("[BrainBox] wifi_scan read 失败 ($consecutiveReadFailures/3): ${error.message}")
                if (consecutiveReadFailures >= 3) throw error
                delay(pollIntervalMillis)
                return@getOrElse null
            } ?: continue
            consecutiveReadFailures = 0
            println("[BrainBox] wifi_scan read: state=${current.state}, networks=${current.networks.size}, error=${current.error}")
            if (current.isReady) {
                current.networks.forEach { net ->
                    println("[BrainBox]   network: ssid=${net.ssid}, signal=${net.signal}, security=${net.security}, active=${net.active}")
                }
                return current
            }
            if (current.isFailed) {
                throw IllegalStateException(current.error.ifBlank { "设备 Wi‑Fi 扫描失败" })
            }
            delay(pollIntervalMillis)
        }
    }

    companion object {
        private const val DEFAULT_WIFI_SCAN_TIMEOUT_MS = 15_000L
        private const val DEFAULT_WIFI_SCAN_POLL_INTERVAL_MS = 500L
        private const val INITIAL_DELAY_BEFORE_READ_MS = 1_000L
        private const val WIFI_CONFIG_READ_DELAY_MS = 5_000L
        private const val OTP_READ_DELAY_MS = 5_000L
        private const val OTP_POLL_INTERVAL_MS = 2_000L
        private const val OTP_POLL_MAX_RETRIES = 5
    }
}
