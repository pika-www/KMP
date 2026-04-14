package com.cephalon.lucyApp.deviceaccess

import com.cephalon.lucyApp.sdk.SdkConnectionState
import com.cephalon.lucyApp.sdk.SdkSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import lucy.im.sdk.OnlineDevice

data class DeviceChatState(
    val connectionState: SdkConnectionState = SdkConnectionState.DISCONNECTED,
    val onlineDevices: List<OnlineDevice> = emptyList(),
    val activeChannelDeviceId: String? = null,
    val assistantReplyText: String = "",
    val isAssistantReplyStreaming: Boolean = false,
)

class DeviceChatManager(
    private val sdkSessionManager: SdkSessionManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(DeviceChatState())
    val state: StateFlow<DeviceChatState> = _state.asStateFlow()

    init {
        sdkSessionManager.connectionState
            .collectIntoState { current, connectionState ->
                current.copy(connectionState = connectionState)
            }
        sdkSessionManager.onlineDevices
            .collectIntoState { current, devices ->
                current.copy(
                    onlineDevices = devices,
                    activeChannelDeviceId = current.activeChannelDeviceId ?: devices.firstOrNull()?.cdi,
                )
            }
        sdkSessionManager.assistantReplyText
            .collectIntoState { current, reply ->
                current.copy(assistantReplyText = reply)
            }
        sdkSessionManager.assistantReplyStreaming
            .collectIntoState { current, streaming ->
                current.copy(isAssistantReplyStreaming = streaming)
            }
    }

    suspend fun ensureImConnected(): Result<Unit> {
        return sdkSessionManager.ensureConnectedIfTokenValid()
    }

    suspend fun awaitDeviceOnline(
        channelDeviceId: String,
        timeoutMillis: Long = DEFAULT_ONLINE_TIMEOUT_MS,
    ): Result<OnlineDevice> {
        val normalized = channelDeviceId.trim()
        if (normalized.isBlank()) {
            return Result.failure(IllegalArgumentException("channel_device_id 不能为空"))
        }
        val onlineDevice = withTimeoutOrNull(timeoutMillis) {
            sdkSessionManager.onlineDevices
                .map { devices -> devices.firstOrNull { it.cdi == normalized } }
                .filter { it != null }
                .first()
        }
        return if (onlineDevice != null) {
            _state.value = _state.value.copy(activeChannelDeviceId = normalized)
            Result.success(onlineDevice)
        } else {
            Result.failure(IllegalStateException("等待设备上线超时：$normalized"))
        }
    }

    suspend fun sendText(
        channelDeviceId: String,
        text: String,
    ): Result<String> {
        val normalized = channelDeviceId.trim()
        if (normalized.isBlank()) {
            return Result.failure(IllegalArgumentException("channel_device_id 不能为空"))
        }
        return sdkSessionManager.publishTextToNpc(
            cdi = normalized,
            text = text,
        )
    }

    private fun <T> StateFlow<T>.collectIntoState(
        reducer: (DeviceChatState, T) -> DeviceChatState,
    ) {
        scope.launch {
            collect { value ->
                _state.value = reducer(_state.value, value)
            }
        }
    }

    companion object {
        private const val DEFAULT_ONLINE_TIMEOUT_MS = 30_000L
    }
}
