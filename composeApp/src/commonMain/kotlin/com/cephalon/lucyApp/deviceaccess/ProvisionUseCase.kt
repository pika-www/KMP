package com.cephalon.lucyApp.deviceaccess

import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.api.LucyDevice
import com.cephalon.lucyApp.api.channelDeviceId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


enum class ProvisionStage {
    Idle,
    Binding,
    Provisioning,
    Completed,
    Failed,
}

data class ProvisionState(
    val stage: ProvisionStage = ProvisionStage.Idle,
    val channelDeviceId: String? = null,
    val wifiSsid: String? = null,
    val boundDevice: LucyDevice? = null,
    val provisionMessage: String? = null,
    val errorMessage: String? = null,
)

data class ProvisionedDevice(
    val device: LucyDevice,
    val channelDeviceId: String,
    val wifiSsid: String,
    val provisionMessage: String,
)

class ProvisionUseCase(
    private val authRepository: AuthRepository,
) {
    private val _state = MutableStateFlow(ProvisionState())
    val state: StateFlow<ProvisionState> = _state.asStateFlow()

    suspend fun bindDevice(channelDeviceId: String): Result<LucyDevice> {
        val normalized = channelDeviceId.trim()
        _state.value = ProvisionState(
            stage = ProvisionStage.Binding,
            channelDeviceId = normalized,
        )
        val result = authRepository.requireDeviceByChannelDeviceId(normalized)
        result.onSuccess { device ->
            _state.value = ProvisionState(
                stage = ProvisionStage.Binding,
                channelDeviceId = device.channelDeviceId,
                boundDevice = device,
            )
        }
        result.onFailure { error ->
            _state.value = ProvisionState(
                stage = ProvisionStage.Failed,
                channelDeviceId = normalized,
                errorMessage = error.message ?: "绑定失败",
            )
        }
        return result
    }

    suspend fun provisionDevice(
        bleManager: BleManager,
        channelDeviceId: String,
        ssid: String,
        password: String,
    ): Result<ProvisionedDevice> {
        val normalizedCdi = channelDeviceId.trim()
        val normalizedSsid = ssid.trim()
        if (normalizedCdi.isBlank()) {
            val error = IllegalArgumentException("channel_device_id 不能为空")
            _state.value = ProvisionState(
                stage = ProvisionStage.Failed,
                errorMessage = error.message,
            )
            return Result.failure(error)
        }
        if (normalizedSsid.isBlank()) {
            val error = IllegalArgumentException("Wi‑Fi 名称不能为空")
            _state.value = ProvisionState(
                stage = ProvisionStage.Failed,
                channelDeviceId = normalizedCdi,
                errorMessage = error.message,
            )
            return Result.failure(error)
        }

        val boundDevice = bindDevice(normalizedCdi).getOrElse {
            return Result.failure(it)
        }

        _state.value = ProvisionState(
            stage = ProvisionStage.Provisioning,
            channelDeviceId = normalizedCdi,
            wifiSsid = normalizedSsid,
            boundDevice = boundDevice,
        )

        val wifiReady = bleManager.ensureWifiReady()
        if (!wifiReady) {
            val error = IllegalStateException("Wi‑Fi 或定位权限未授权")
            _state.value = ProvisionState(
                stage = ProvisionStage.Failed,
                channelDeviceId = normalizedCdi,
                wifiSsid = normalizedSsid,
                boundDevice = boundDevice,
                errorMessage = error.message,
            )
            return Result.failure(error)
        }

        val connectResult = bleManager.connectPhoneToWifi(
            ssid = normalizedSsid,
            password = password,
        )

        connectResult.onSuccess { message ->
            _state.value = ProvisionState(
                stage = ProvisionStage.Completed,
                channelDeviceId = normalizedCdi,
                wifiSsid = normalizedSsid,
                boundDevice = boundDevice,
                provisionMessage = message,
            )
        }
        connectResult.onFailure { error ->
            _state.value = ProvisionState(
                stage = ProvisionStage.Failed,
                channelDeviceId = normalizedCdi,
                wifiSsid = normalizedSsid,
                boundDevice = boundDevice,
                errorMessage = error.message ?: "配网失败",
            )
        }

        return connectResult.map { message ->
            ProvisionedDevice(
                device = boundDevice,
                channelDeviceId = normalizedCdi,
                wifiSsid = normalizedSsid,
                provisionMessage = message,
            )
        }
    }
}
