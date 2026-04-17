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
     * 协议步骤 5：向 wifi_scan 写入 {"action":"scan"}，等待 [waitMillis]（默认 5s）后单次读取。
     * 不循环轮询。
     */
    suspend fun scanWifi(
        waitMillis: Long = WIFI_SCAN_WAIT_MS,
    ): Result<List<GattWifiNetwork>> {
        val scanCmd = WifiScanCommand()
        println("[BrainBox] wifi_scan write: ${dispatcher.toJsonObject(scanCmd)}")
        writeRoute(
            route = GattRoute.WifiScan,
            payload = dispatcher.toJsonObject(scanCmd),
        ).getOrElse { return Result.failure(it) }
        println("[BrainBox] wifi_scan: 等待 ${waitMillis}ms 后读取结果...")
        delay(waitMillis)
        val response = readRoute<WifiScanResponse>(GattRoute.WifiScan).getOrElse { return Result.failure(it) }
        println("[BrainBox] wifi_scan result: state=${response.state}, networks=${response.networks.size}, error=${response.error}")
        if (response.isFailed) {
            return Result.failure(IllegalStateException(response.error.ifBlank { "设备 Wi‑Fi 扫描失败" }))
        }
        return Result.success(response.networks)
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
        /** 协议步骤 5：写入 wifi_scan 后等待 5s 再读取一次。 */
        const val WIFI_SCAN_WAIT_MS = 5_000L

        /** 协议步骤 6：写入 wifi_config 后等待 5s 再读取 network_status。 */
        const val WIFI_CONFIG_WAIT_MS = 5_000L

        /** 协议步骤 7：写入 lucy_pairing_request 后等待 3s 再读取 pairing_info。 */
        const val OTP_READ_DELAY_MS = 3_000L
    }
}
