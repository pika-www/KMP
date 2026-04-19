package com.cephalon.lucyApp.deviceaccess.gatt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

object BrainBoxGattProtocol {
    const val SERVICE_UUID = "7f0c0000-4f31-4a32-a917-9a4ec0b20001"
    const val DEVICE_INFO_UUID = "7f0c0001-4f31-4a32-a917-9a4ec0b20001"
    const val NETWORK_STATUS_UUID = "7f0c0002-4f31-4a32-a917-9a4ec0b20001"
    const val WIFI_CONFIG_UUID = "7f0c0003-4f31-4a32-a917-9a4ec0b20001"
    const val WIFI_SCAN_UUID = "7f0c0004-4f31-4a32-a917-9a4ec0b20001"
    const val LUCY_PAIRING_INFO_UUID = "7f0c0005-4f31-4a32-a917-9a4ec0b20001"
    const val LUCY_PAIRING_REQUEST_UUID = "7f0c0006-4f31-4a32-a917-9a4ec0b20001"
    const val CLIENT_CONFIG_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"
}

enum class GattRoute(
    val characteristicUuid: String,
    val writeWithResponse: Boolean = true,
) {
    DeviceInfo(BrainBoxGattProtocol.DEVICE_INFO_UUID),
    NetworkStatus(BrainBoxGattProtocol.NETWORK_STATUS_UUID),
    // 改动 1：WifiConfig 使用 WRITE_WITH_RESPONSE。
    // 原因：wifi_config JSON（ssid + password + hidden）常 > 20B，必须让 iOS/Android
    //       BLE 栈在超 ATT MTU 时自动走 Prepare Write + Execute Write，把整条 payload
    //       重组成一次 WriteValue 交给服务端 agent 做 json.Unmarshal；
    //       WRITE_NO_RESPONSE 没有 ACK 链，BLE 栈不会触发 Prepare/Execute 组装，
    //       会退化为多次独立的 ATT Write Command，服务端按每片独立解析 JSON 必然失败。
    //       应用层不做分片（见 GattRouter.writeRoute 的改动 4）。
    WifiConfig(BrainBoxGattProtocol.WIFI_CONFIG_UUID, writeWithResponse = true),
    WifiScan(BrainBoxGattProtocol.WIFI_SCAN_UUID),
    LucyPairingInfo(BrainBoxGattProtocol.LUCY_PAIRING_INFO_UUID),
    LucyPairingRequest(BrainBoxGattProtocol.LUCY_PAIRING_REQUEST_UUID, writeWithResponse = true),
}

@Serializable
data class GattWifiNetwork(
    val ssid: String = "",
    val signal: Int? = null,
    val security: String = "",
    val active: Boolean = false,
) {
    val isSecure: Boolean
        get() = !security.equals("open", ignoreCase = true)
}

@Serializable
data class WifiScanCommand(
    val action: String = "scan",
)

@Serializable
data class WifiScanResponse(
    val state: String = "",
    val networks: List<GattWifiNetwork> = emptyList(),
    val error: String = "",
    val updatedAt: String = "",
) {
    val isReady: Boolean
        get() = state.equals("ready", ignoreCase = true) ||
            (state.isBlank() && networks.isNotEmpty())

    val isScanning: Boolean
        get() = state.equals("scanning", ignoreCase = true)

    val isFailed: Boolean
        get() = state.equals("failed", ignoreCase = true)
}

@Serializable
data class WifiConfigCommand(
    val ssid: String,
    val password: String,
    val hidden: Boolean = false,
)

@Serializable
data class LucyPairingInfoPayload(
    val version: Int? = null,
    val channel: String = "",
    val state: String = "",
    @SerialName("channel_device_id")
    val channelDeviceId: String = "",
    val otp: String = "",
    @SerialName("binding_status")
    val bindingStatus: String = "",
    @SerialName("created_at_ms")
    val createdAtMs: Long? = null,
    @SerialName("otp_expires_at_ms")
    val otpExpiresAtMs: Long? = null,
) {
    val isReady: Boolean
        get() = state.equals("ready", ignoreCase = true) || state.isBlank()

    val isUnavailable: Boolean
        get() = state.equals("unavailable", ignoreCase = true)

    val isBound: Boolean
        get() = bindingStatus.equals("bound", ignoreCase = true)

    val isPendingBinding: Boolean
        get() = bindingStatus.equals("pending", ignoreCase = true) || bindingStatus.isBlank()

    val isOtpExpired: Boolean
        get() {
            val expiresAt = otpExpiresAtMs ?: return false
            return kotlinx.datetime.Clock.System.now().toEpochMilliseconds() >= expiresAt
        }

    val isOtpValid: Boolean
        get() = !otp.isNullOrBlank() && !isOtpExpired
}

@Serializable
data class LucyPairingRequestPayload(
    @SerialName("request_id")
    val requestId: String,
)

@Serializable
data class NetworkStatusPayload(
    val state: String = "",
    val connected: Boolean? = null,
    val status: String = "",
    @SerialName("wifi_status")
    val wifiStatus: String = "",
    val ssid: String = "",
    val ip: String = "",
    val error: String = "",
    val updatedAt: String = "",
) {
    val isConnected: Boolean
        get() = state.equals("connected", ignoreCase = true) ||
            connected == true ||
            status.equals("connected", ignoreCase = true) ||
            wifiStatus.equals("connected", ignoreCase = true)

    val isFailed: Boolean
        get() = state.equals("failed", ignoreCase = true)
}

@Serializable
data class DeviceInfoPayload(
    val hostname: String = "",
    val model: String = "",
    val serial: String = "",
    @SerialName("interface")
    val networkInterface: String = "",
    val ip: String = "",
    val ssid: String = "",
    val state: String = "",
    val error: String = "",
) {
    val name: String
        get() = hostname

    val isConnected: Boolean
        get() = state.equals("connected", ignoreCase = true)

    val isDisconnected: Boolean
        get() = state.equals("disconnected", ignoreCase = true)
}

data class RoutedJsonPayload(
    val route: GattRoute,
    val payload: JsonObject,
)
