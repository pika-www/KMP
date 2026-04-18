package com.cephalon.lucyApp.deviceaccess.gatt

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.cephalon.lucyApp.deviceaccess.BleScanDevice
import com.cephalon.lucyApp.scan.rememberOpenAppSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

// 主动 disconnect 后等待 STATE_DISCONNECTED 平台回调的上限；超时强制走清理路径。
private const val DISCONNECT_TIMEOUT_MS = 3_000L

/**
 * Android `BluetoothGattCallback` 的 status 只是一个 int，默认打出来就是个数字，
 * 定位需要再查表。这个表把常见 ATT / GATT 错误码映射到 BLE 规范或 Android 内部名称，
 * 特别是 0x0E UNLIKELY_ERROR（对应 bluez `org.bluez.Error.Failed`，等价于 lucy_pairing_request
 * 协议文档里的五种 IPC 错误）会直接标注出来，省翻 ATT 规范。
 */
private fun gattStatusName(status: Int): String = when (status) {
    BluetoothGatt.GATT_SUCCESS -> "SUCCESS"
    0x01 -> "INVALID_HANDLE"
    BluetoothGatt.GATT_READ_NOT_PERMITTED -> "READ_NOT_PERMITTED"
    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "WRITE_NOT_PERMITTED"
    0x04 -> "INVALID_PDU"
    BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> "INSUFFICIENT_AUTHENTICATION"
    BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> "REQUEST_NOT_SUPPORTED"
    BluetoothGatt.GATT_INVALID_OFFSET -> "INVALID_OFFSET"
    0x08 -> "INSUFFICIENT_AUTHORIZATION"
    0x09 -> "PREPARE_QUEUE_FULL"
    0x0A -> "ATTRIBUTE_NOT_FOUND"
    0x0B -> "ATTRIBUTE_NOT_LONG"
    0x0C -> "INSUFFICIENT_KEY_SIZE"
    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "INVALID_ATTRIBUTE_LENGTH"
    0x0E -> "UNLIKELY_ERROR (bluez Error.Failed 标准映射)"
    BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> "INSUFFICIENT_ENCRYPTION"
    0x10 -> "UNSUPPORTED_GROUP_TYPE"
    0x11 -> "INSUFFICIENT_RESOURCES"
    0x13 -> "CONNECTION_TERMINATED_BY_PEER"
    0x16 -> "CONNECTION_TERMINATED_BY_LOCAL_HOST"
    0x22 -> "LMP_RESPONSE_TIMEOUT"
    0x3E -> "CONNECTION_FAILED_ESTABLISH"
    // Android BluetoothGatt 自定义常量：系统级通用 GATT 错误
    0x85 -> "GATT_ERROR (Android generic)"
    0x8F -> "LMP_TIMEOUT"
    // BLE ATT 规范 0x80-0x9F 段：Application Error 保留区，设备端自定义。
    // BrainBox 的 Lucy pairing IPC 错误（pairing ipc is not enabled / lucy pairing
    // client is not connected / pairing ipc: request timed out / duplicate request
    // id / Lucy 原始错误）即落在此区间，细分仍需查设备端 Lucy/bluez 日志。
    in 0x80..0x9F -> "APPLICATION_ERROR (设备端自定义,需看 Lucy/bluez 日志)"
    else -> "UNKNOWN"
}

private fun formatGattStatus(status: Int): String =
    "0x${status.toString(16).uppercase(Locale.US).padStart(2, '0')}($status) ${gattStatusName(status)}"

private fun Context.hasBlePermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

private fun androidBluetoothPermissionList(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

private fun hasAndroidBluetoothPermissions(context: Context): Boolean {
    return androidBluetoothPermissionList().all(context::hasBlePermission)
}

@SuppressLint("MissingPermission")
@Composable
actual fun rememberBleClient(): BleClient {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val bluetoothManager = remember(appContext) {
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    val openAppSettings = rememberOpenAppSettings()
    val scanState = remember {
        MutableStateFlow(
            BleScanSnapshot(
                bluetoothPermissionGranted = hasAndroidBluetoothPermissions(appContext),
                bluetoothEnabled = bluetoothManager.adapter?.isEnabled == true,
            )
        )
    }
    val discoveredDevices = remember { linkedMapOf<String, BleScanDevice>() }
    var pendingBluetoothPermission by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }

    val refreshState = {
        scanState.update {
            it.copy(
                bluetoothPermissionGranted = hasAndroidBluetoothPermissions(appContext),
                bluetoothEnabled = bluetoothManager.adapter?.isEnabled == true,
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            refreshState()
            pendingBluetoothPermission?.complete(scanState.value.bluetoothPermissionGranted)
            pendingBluetoothPermission = null
        }
    )

    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                handleScanResult(result, discoveredDevices, scanState)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { handleScanResult(it, discoveredDevices, scanState) }
            }

            override fun onScanFailed(errorCode: Int) {
                scanState.update {
                    it.copy(isScanning = false, errorMessage = "BLE 扫描失败(error=$errorCode)")
                }
            }
        }
    }

    DisposableEffect(bluetoothManager, scanCallback) {
        onDispose {
            runCatching { bluetoothManager.adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
        }
    }

    return remember(appContext, bluetoothManager, openAppSettings, scanCallback) {
        object : BleClient {
            override val scanState: StateFlow<BleScanSnapshot> = scanState.asStateFlow()

            override suspend fun ensureBluetoothReady(): Boolean {
                refreshState()
                if (scanState.value.bluetoothPermissionGranted) {
                    return scanState.value.bluetoothEnabled
                }
                pendingBluetoothPermission?.let { return it.await() && scanState.value.bluetoothEnabled }
                val deferred = CompletableDeferred<Boolean>()
                pendingBluetoothPermission = deferred
                permissionLauncher.launch(androidBluetoothPermissionList())
                return deferred.await() && scanState.value.bluetoothEnabled
            }

            override fun openBluetoothSettings() {
                openAppSettings()
            }

            override fun startScan(serviceUuid: String?) {
                refreshState()
                if (!scanState.value.bluetoothPermissionGranted || !scanState.value.bluetoothEnabled) {
                    scanState.update { it.copy(isScanning = false) }
                    return
                }
                // 不再清空 discoveredDevices：同一次 Sheet 会话里 startScan 可能被反复调用
                // (例如探测 pairing_info 结束后恢复扫描)，若每次清空会导致 UI 列表闪烁。
                scanState.update {
                    it.copy(
                        isScanning = true,
                        errorMessage = null,
                    )
                }
                val filters = serviceUuid
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        listOf(
                            ScanFilter.Builder()
                                .setServiceUuid(ParcelUuid(UUID.fromString(it)))
                                .build()
                        )
                    }
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                runCatching {
                    bluetoothManager.adapter?.bluetoothLeScanner?.startScan(filters, settings, scanCallback)
                }.onFailure { error ->
                    scanState.update {
                        it.copy(isScanning = false, errorMessage = error.message ?: "BLE 扫描启动失败")
                    }
                }
            }

            override fun stopScan() {
                runCatching { bluetoothManager.adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
                scanState.update { it.copy(isScanning = false) }
            }

            override suspend fun connectGatt(device: BleScanDevice): Result<BleGattConnection> {
                refreshState()
                if (!scanState.value.bluetoothPermissionGranted || !scanState.value.bluetoothEnabled) {
                    return Result.failure(IllegalStateException("蓝牙权限未授权或蓝牙未开启"))
                }
                val bluetoothDevice = bluetoothManager.adapter?.getRemoteDevice(device.id)
                    ?: return Result.failure(IllegalStateException("未找到蓝牙设备 ${device.id}"))
                return AndroidBleGattConnection.connect(appContext, bluetoothDevice, device)
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun handleScanResult(
    result: ScanResult,
    discoveredDevices: MutableMap<String, BleScanDevice>,
    scanState: MutableStateFlow<BleScanSnapshot>,
) {
    val address = runCatching { result.device.address }.getOrNull().orEmpty()
    if (address.isBlank()) return
    val name = runCatching { result.device.name }.getOrNull().takeIf { !it.isNullOrBlank() }
        ?: result.scanRecord?.deviceName?.takeIf { it.isNotBlank() }
        ?: "BLE ${address.takeLast(4)}"
    val record = result.scanRecord
    val serviceUuids = record?.serviceUuids?.map { it.toString() } ?: emptyList()
    val serviceData = record?.serviceData?.entries?.map { (k, v) -> "${k}: ${v.size}bytes" } ?: emptyList()
    val manufacturerData = buildList {
        record?.manufacturerSpecificData?.let { data ->
            for (i in 0 until data.size()) {
                val key = data.keyAt(i)
                val value = data.valueAt(i)
                add("0x${key.toString(16)}: ${value?.size ?: 0}bytes")
            }
        }
    }
    val txPower = record?.txPowerLevel ?: Int.MIN_VALUE
    val bondState = runCatching { result.device.bondState }.getOrNull() ?: -1
    val deviceType = runCatching { result.device.type }.getOrNull() ?: -1
    println("[BLE_SCAN] addr=$address, name=$name, rssi=${result.rssi}, txPower=$txPower, bondState=$bondState, deviceType=$deviceType, serviceUuids=$serviceUuids, serviceData=$serviceData, mfgData=$manufacturerData, advBytes=${record?.bytes?.size ?: 0}")
    discoveredDevices[address] = BleScanDevice(
        id = address,
        name = name,
        subtitle = address,
        rssi = result.rssi,
    )
    scanState.update {
        it.copy(
            devices = discoveredDevices.values.sortedByDescending { candidate -> candidate.rssi ?: Int.MIN_VALUE },
            errorMessage = null,
        )
    }
}

private class AndroidBleGattConnection private constructor(
    override val device: BleScanDevice,
    private val bluetoothDevice: BluetoothDevice,
) : BleGattConnection {
    override val state = MutableStateFlow(BleGattConnectionState.Idle)
    override val services = MutableStateFlow<List<BleGattService>>(emptyList())

    private lateinit var gatt: BluetoothGatt
    private val notifications = mutableMapOf<String, MutableSharedFlow<ByteArray>>()
    private var connectContinuation: kotlin.coroutines.Continuation<Result<BleGattConnection>>? = null
    private var discoverContinuation: kotlin.coroutines.Continuation<Result<List<BleGattService>>>? = null
    private val readContinuations = mutableMapOf<String, kotlin.coroutines.Continuation<Result<ByteArray>>>()
    private val writeContinuations = mutableMapOf<String, kotlin.coroutines.Continuation<Result<Unit>>>()
    private val descriptorContinuations = mutableMapOf<String, kotlin.coroutines.Continuation<Result<Unit>>>()
    private var disconnectContinuation: kotlin.coroutines.Continuation<Unit>? = null

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            this@AndroidBleGattConnection.gatt = gatt
            when {
                status != BluetoothGatt.GATT_SUCCESS -> {
                    state.value = BleGattConnectionState.Error
                    connectContinuation?.resume(Result.failure(IllegalStateException("GATT 连接失败(status=${formatGattStatus(status)})")))
                    connectContinuation = null
                    failAllPending("GATT 连接异常断开(status=${formatGattStatus(status)})")
                    runCatching { gatt.close() }
                }
                newState == BluetoothProfile.STATE_CONNECTED -> {
                    state.value = BleGattConnectionState.Connected
                    connectContinuation?.resume(Result.success(this@AndroidBleGattConnection))
                    connectContinuation = null
                }
                newState == BluetoothProfile.STATE_DISCONNECTED -> {
                    state.value = BleGattConnectionState.Disconnected
                    if (connectContinuation != null) {
                        connectContinuation?.resume(Result.failure(IllegalStateException("GATT 连接已断开")))
                        connectContinuation = null
                    }
                    failAllPending("GATT 已断开")
                    runCatching { gatt.close() }
                    // 主动 disconnect() 在等待平台回调确认掉线，此刻 resume。
                    disconnectContinuation?.resume(Unit)
                    disconnectContinuation = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                state.value = BleGattConnectionState.Error
                discoverContinuation?.resume(Result.failure(IllegalStateException("发现服务失败(status=${formatGattStatus(status)})")))
                discoverContinuation = null
                return
            }
            val mappedServices = gatt.services.orEmpty().map { it.toCommonService() }
            services.value = mappedServices
            state.value = BleGattConnectionState.Ready
            discoverContinuation?.resume(Result.success(mappedServices))
            discoverContinuation = null
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            completeRead(characteristic.uuid.toString(), status, value)
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            completeRead(characteristic.uuid.toString(), status, characteristic.value ?: byteArrayOf())
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            val key = characteristic.uuid.toString().normalizedUuid()
            val continuation = writeContinuations.remove(key) ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                continuation.resume(Result.success(Unit))
            } else {
                continuation.resume(Result.failure(IllegalStateException("写入特征失败(status=${formatGattStatus(status)}, uuid=${characteristic.uuid})")))
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            val key = descriptor.characteristic.uuid.toString().normalizedUuid()
            val continuation = descriptorContinuations.remove(key) ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                continuation.resume(Result.success(Unit))
            } else {
                continuation.resume(Result.failure(IllegalStateException("设置通知失败(status=${formatGattStatus(status)}, uuid=${descriptor.characteristic.uuid})")))
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            emitNotification(characteristic.uuid.toString(), value)
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            emitNotification(characteristic.uuid.toString(), characteristic.value ?: byteArrayOf())
        }
    }

    override suspend fun discoverServices(): Result<List<BleGattService>> {
        return suspendCancellableCoroutine { continuation ->
            discoverContinuation = continuation
            state.value = BleGattConnectionState.Discovering
            val started = runCatching { gatt.discoverServices() }.getOrDefault(false)
            if (!started) {
                discoverContinuation = null
                continuation.resume(Result.failure(IllegalStateException("无法开始发现 GATT 服务")))
            }
        }
    }

    override suspend fun readCharacteristic(serviceUuid: String, characteristicUuid: String): Result<ByteArray> {
        val characteristic = findCharacteristic(serviceUuid, characteristicUuid)
            ?: return Result.failure(IllegalStateException("未找到特征 $characteristicUuid"))
        return suspendCancellableCoroutine { continuation ->
            readContinuations[characteristic.uuid.toString().normalizedUuid()] = continuation
            val started = runCatching { gatt.readCharacteristic(characteristic) }.getOrDefault(false)
            if (!started) {
                readContinuations.remove(characteristic.uuid.toString().normalizedUuid())
                continuation.resume(Result.failure(IllegalStateException("无法开始读取特征 $characteristicUuid")))
            }
        }
    }

    override suspend fun writeCharacteristic(
        serviceUuid: String,
        characteristicUuid: String,
        payload: ByteArray,
    ): Result<Unit> {
        val characteristic = findCharacteristic(serviceUuid, characteristicUuid)
            ?: return Result.failure(IllegalStateException("未找到特征 $characteristicUuid"))
        return suspendCancellableCoroutine { continuation ->
            writeContinuations[characteristic.uuid.toString().normalizedUuid()] = continuation
            val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching {
                    gatt.writeCharacteristic(
                        characteristic,
                        payload,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                    ) == android.bluetooth.BluetoothStatusCodes.SUCCESS
                }.getOrDefault(false)
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = payload
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                runCatching { gatt.writeCharacteristic(characteristic) }.getOrDefault(false)
            }
            if (!started) {
                writeContinuations.remove(characteristic.uuid.toString().normalizedUuid())
                continuation.resume(Result.failure(IllegalStateException("无法开始写入特征 $characteristicUuid")))
            }
        }
    }

    override suspend fun writeCharacteristicNoResponse(
        serviceUuid: String,
        characteristicUuid: String,
        payload: ByteArray,
    ): Result<Unit> {
        val characteristic = findCharacteristic(serviceUuid, characteristicUuid)
            ?: return Result.failure(IllegalStateException("未找到特征 $characteristicUuid"))
        val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                gatt.writeCharacteristic(
                    characteristic,
                    payload,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
                ) == android.bluetooth.BluetoothStatusCodes.SUCCESS
            }.getOrDefault(false)
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = payload
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            runCatching { gatt.writeCharacteristic(characteristic) }.getOrDefault(false)
        }
        return if (started) Result.success(Unit)
        else Result.failure(IllegalStateException("无法写入特征(NoResponse) $characteristicUuid"))
    }

    override suspend fun setCharacteristicNotification(
        serviceUuid: String,
        characteristicUuid: String,
        enabled: Boolean,
    ): Result<Unit> {
        val characteristic = findCharacteristic(serviceUuid, characteristicUuid)
            ?: return Result.failure(IllegalStateException("未找到特征 $characteristicUuid"))
        return suspendCancellableCoroutine { continuation ->
            descriptorContinuations[characteristic.uuid.toString().normalizedUuid()] = continuation
            val notifyResult = runCatching { gatt.setCharacteristicNotification(characteristic, enabled) }.getOrDefault(false)
            if (!notifyResult) {
                descriptorContinuations.remove(characteristic.uuid.toString().normalizedUuid())
                continuation.resume(Result.failure(IllegalStateException("无法设置特征通知 $characteristicUuid")))
                return@suspendCancellableCoroutine
            }
            val descriptor = characteristic.getDescriptor(UUID.fromString(BrainBoxGattProtocol.CLIENT_CONFIG_DESCRIPTOR_UUID))
            if (descriptor == null) {
                descriptorContinuations.remove(characteristic.uuid.toString().normalizedUuid())
                continuation.resume(Result.success(Unit))
                return@suspendCancellableCoroutine
            }
            val value = if (enabled) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching {
                    gatt.writeDescriptor(descriptor, value) == android.bluetooth.BluetoothStatusCodes.SUCCESS
                }.getOrDefault(false)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = value
                @Suppress("DEPRECATION")
                runCatching { gatt.writeDescriptor(descriptor) }.getOrDefault(false)
            }
            if (!started) {
                descriptorContinuations.remove(characteristic.uuid.toString().normalizedUuid())
                continuation.resume(Result.failure(IllegalStateException("无法写入通知描述符 $characteristicUuid")))
            }
        }
    }

    override fun observeCharacteristic(serviceUuid: String, characteristicUuid: String): Flow<ByteArray> {
        val key = characteristicUuid.normalizedUuid()
        return notifications.getOrPut(key) {
            MutableSharedFlow(extraBufferCapacity = 32, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        }
    }

    override suspend fun disconnect() {
        // 已经断开（例如 disconnectWatcher 先检测到）直接返回，避免无谓等待。
        val current = state.value
        if (current == BleGattConnectionState.Disconnected || current == BleGattConnectionState.Idle) {
            runCatching { gatt.close() }
            return
        }
        // 挂起直到 onConnectionStateChange(STATE_DISCONNECTED) 回调，不再像以前那样同步
        // 反手将 state 设为 Disconnected（那样后续 waitUntilDisconnected 是空操作）。
        val completed = withTimeoutOrNull(DISCONNECT_TIMEOUT_MS) {
            suspendCancellableCoroutine<Unit> { continuation ->
                disconnectContinuation = continuation
                val started = runCatching { gatt.disconnect() }.isSuccess
                if (!started) {
                    // gatt.disconnect() 没启动，回调不会到，直接 resume 避免挂死。
                    disconnectContinuation = null
                    continuation.resume(Unit)
                }
            }
        }
        if (completed == null) {
            println("[BrainBox] disconnect 超时未收到 STATE_DISCONNECTED 回调，强制清理")
            disconnectContinuation = null
        }
        runCatching { gatt.close() }
        state.value = BleGattConnectionState.Disconnected
    }

    private fun emitNotification(characteristicUuid: String, value: ByteArray) {
        notifications.getOrPut(characteristicUuid.normalizedUuid()) {
            MutableSharedFlow(extraBufferCapacity = 32, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        }.tryEmit(value)
    }

    private fun completeRead(characteristicUuid: String, status: Int, value: ByteArray) {
        val key = characteristicUuid.normalizedUuid()
        val continuation = readContinuations.remove(key) ?: return
        if (status == BluetoothGatt.GATT_SUCCESS) {
            continuation.resume(Result.success(value))
        } else {
            continuation.resume(Result.failure(IllegalStateException("读取特征失败(status=${formatGattStatus(status)}, uuid=$characteristicUuid)")))
        }
    }

    private fun failAllPending(message: String) {
        val error = IllegalStateException(message)
        discoverContinuation?.resume(Result.failure(error))
        discoverContinuation = null
        readContinuations.values.toList().forEach { it.resume(Result.failure(error)) }
        readContinuations.clear()
        writeContinuations.values.toList().forEach { it.resume(Result.failure(error)) }
        writeContinuations.clear()
        descriptorContinuations.values.toList().forEach { it.resume(Result.failure(error)) }
        descriptorContinuations.clear()
    }

    private fun findCharacteristic(serviceUuid: String, characteristicUuid: String): BluetoothGattCharacteristic? {
        val normalizedService = serviceUuid.normalizedUuid()
        val normalizedCharacteristic = characteristicUuid.normalizedUuid()
        return gatt.services.orEmpty()
            .firstOrNull { it.uuid.toString().normalizedUuid() == normalizedService }
            ?.characteristics
            ?.firstOrNull { it.uuid.toString().normalizedUuid() == normalizedCharacteristic }
    }

    companion object {
        @SuppressLint("MissingPermission")
        suspend fun connect(
            context: Context,
            bluetoothDevice: BluetoothDevice,
            device: BleScanDevice,
        ): Result<BleGattConnection> {
            val connection = AndroidBleGattConnection(device = device, bluetoothDevice = bluetoothDevice)
            return suspendCancellableCoroutine { continuation ->
                connection.connectContinuation = continuation
                connection.state.value = BleGattConnectionState.Connecting
                connection.gatt = bluetoothDevice.connectGatt(
                    context,
                    false,
                    connection.callback,
                    BluetoothDevice.TRANSPORT_LE,
                )
                continuation.invokeOnCancellation {
                    runCatching { connection.gatt.disconnect() }
                    runCatching { connection.gatt.close() }
                }
            }
        }
    }
}

private fun BluetoothGattService.toCommonService(): BleGattService {
    return BleGattService(
        uuid = uuid.toString(),
        characteristics = characteristics.orEmpty().map { it.toCommonCharacteristic(uuid.toString()) },
    )
}

private fun BluetoothGattCharacteristic.toCommonCharacteristic(serviceUuid: String): BleGattCharacteristic {
    return BleGattCharacteristic(
        serviceUuid = serviceUuid,
        uuid = uuid.toString(),
        properties = properties,
    )
}

private fun String.normalizedUuid(): String = lowercase(Locale.US)
