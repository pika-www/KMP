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
    ): Result<LucyPairingInfoPayload> {
        requestOtp().getOrElse { return Result.failure(it) }
        println("[BrainBox] requestOtp: 等待 ${delayMillis}ms 后读取 pairing_info...")
        delay(delayMillis)
        val pairingInfo = readLucyPairingInfo().getOrElse { return Result.failure(it) }
        println("[BrainBox] requestOtp result: otp=${pairingInfo.otp}, otpExpiresAtMs=${pairingInfo.otpExpiresAtMs}, bindingStatus=${pairingInfo.bindingStatus}")
        return Result.success(pairingInfo)
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
            println("[BrainBox] wifi_config: 写入成功，等待 ${INITIAL_DELAY_BEFORE_READ_MS}ms 后轮询 network_status...")
            delay(INITIAL_DELAY_BEFORE_READ_MS)
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
        return connection.writeCharacteristic(
            serviceUuid = BrainBoxGattProtocol.SERVICE_UUID,
            characteristicUuid = route.characteristicUuid,
            payload = dispatcher.encodeJsonObject(payload),
        )
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
        private const val OTP_READ_DELAY_MS = 2_500L
    }
}
