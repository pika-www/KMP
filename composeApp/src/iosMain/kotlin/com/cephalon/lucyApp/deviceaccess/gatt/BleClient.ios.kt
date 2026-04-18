package com.cephalon.lucyApp.deviceaccess.gatt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cephalon.lucyApp.deviceaccess.BleScanDevice
import com.cephalon.lucyApp.scan.rememberOpenAppSettings
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreBluetooth.CBAdvertisementDataLocalNameKey
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCentralManagerScanOptionAllowDuplicatesKey
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBCharacteristicProperties
import platform.CoreBluetooth.CBCharacteristicWriteWithResponse
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBManagerStateUnauthorized
import platform.CoreBluetooth.CBManagerStateUnsupported
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegateProtocol
import platform.CoreBluetooth.CBService
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.create
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume

fun normalizedUuid(uuid: String): String {
    return uuid.replace("-", "").lowercase()
}

private fun String.normalizedUuid(): String {
    return normalizedUuid(this)
}

@Composable
actual fun rememberBleClient(): BleClient {
    val openAppSettings = rememberOpenAppSettings()
    val scanState = remember { MutableStateFlow(BleScanSnapshot()) }
    val discoveredDevices = remember { linkedMapOf<String, BleScanDevice>() }
    val peripherals = remember { mutableMapOf<String, CBPeripheral>() }
    var activeConnection by remember { mutableStateOf<IosBleGattConnection?>(null) }
    var centralManagerState by remember { mutableStateOf<CBCentralManager?>(null) }

    val delegate = remember {
        object : NSObject(), CBCentralManagerDelegateProtocol {
            override fun centralManagerDidUpdateState(central: CBCentralManager) {
                val ready = central.state == CBManagerStatePoweredOn
                scanState.update {
                    it.copy(
                        bluetoothPermissionGranted = central.state != CBManagerStateUnauthorized &&
                            central.state != CBManagerStateUnsupported,
                        bluetoothEnabled = ready,
                        isScanning = if (ready) it.isScanning else false,
                    )
                }
                if (ready && scanState.value.isScanning) {
                    central.scanForPeripheralsWithServices(
                        serviceUUIDs = listOf(CBUUID.UUIDWithString(BrainBoxGattProtocol.SERVICE_UUID)),
                        options = mapOf(CBCentralManagerScanOptionAllowDuplicatesKey to false),
                    )
                }
            }

            override fun centralManager(
                central: CBCentralManager,
                didDiscoverPeripheral: CBPeripheral,
                advertisementData: Map<Any?, *>,
                RSSI: NSNumber,
            ) {
                val identifier = didDiscoverPeripheral.identifier.UUIDString
                if (identifier.isBlank()) return
                peripherals[identifier] = didDiscoverPeripheral
                val localName = advertisementData[CBAdvertisementDataLocalNameKey] as? String
                val displayName = localName ?: didDiscoverPeripheral.name ?: "BLE ${identifier.takeLast(4)}"
                discoveredDevices[identifier] = BleScanDevice(
                    id = identifier,
                    name = displayName,
                    subtitle = identifier,
                    rssi = RSSI.intValue,
                )
                scanState.update {
                    it.copy(
                        devices = discoveredDevices.values.sortedByDescending { device -> device.rssi ?: Int.MIN_VALUE },
                        errorMessage = null,
                    )
                }
            }

            override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
                activeConnection?.onConnected(didConnectPeripheral)
            }

            @ObjCSignatureOverride
            override fun centralManager(
                central: CBCentralManager,
                didFailToConnectPeripheral: CBPeripheral,
                error: NSError?,
            ) {
                activeConnection?.onConnectionFailed(error)
            }

            @ObjCSignatureOverride
            override fun centralManager(
                central: CBCentralManager,
                didDisconnectPeripheral: CBPeripheral,
                error: NSError?,
            ) {
                activeConnection?.onDisconnected(error)
            }
        }
    }

    fun ensureCentralManager(): CBCentralManager {
        val existing = centralManagerState
        if (existing != null) return existing
        val created = CBCentralManager(delegate = delegate, queue = null)
        centralManagerState = created
        return created
    }

    DisposableEffect(delegate) {
        ensureCentralManager()
        onDispose {
            centralManagerState?.stopScan()
            activeConnection = null
        }
    }

    return remember(delegate, openAppSettings) {
        object : BleClient {
            override val scanState: StateFlow<BleScanSnapshot> = scanState.asStateFlow()

            override suspend fun ensureBluetoothReady(): Boolean {
                ensureCentralManager()
                return scanState.value.bluetoothPermissionGranted && scanState.value.bluetoothEnabled
            }

            override fun openBluetoothSettings() {
                openAppSettings()
            }

            override fun startScan(serviceUuid: String?) {
                val manager = ensureCentralManager()
                // 不再清空 discoveredDevices：同一次 Sheet 会话里 startScan 可能被反复调用
                // (例如探测 pairing_info 结束后恢复扫描)，若每次清空会导致 UI 列表闪烁。
                scanState.update {
                    it.copy(
                        isScanning = manager.state == CBManagerStatePoweredOn,
                        errorMessage = null,
                    )
                }
                if (manager.state == CBManagerStatePoweredOn) {
                    val serviceList = serviceUuid?.takeIf { it.isNotBlank() }?.let { listOf(CBUUID.UUIDWithString(it)) }
                    manager.scanForPeripheralsWithServices(
                        serviceUUIDs = serviceList,
                        options = mapOf(CBCentralManagerScanOptionAllowDuplicatesKey to false),
                    )
                }
            }

            override fun stopScan() {
                centralManagerState?.stopScan()
                scanState.update { it.copy(isScanning = false) }
            }

            override suspend fun connectGatt(device: BleScanDevice): Result<BleGattConnection> {
                val manager = ensureCentralManager()
                if (manager.state != CBManagerStatePoweredOn) {
                    return Result.failure(IllegalStateException("蓝牙未开启"))
                }
                val peripheral = peripherals[device.id]
                    ?: return Result.failure(IllegalStateException("未找到蓝牙设备 ${device.id}"))
                val connection = IosBleGattConnection(manager = manager, peripheral = peripheral, device = device)
                activeConnection = connection
                return connection.connect()
            }
        }
    }
}

private const val CONNECT_TIMEOUT_MS = 15_000L
private const val DISCOVER_TIMEOUT_MS = 15_000L
private const val READ_TIMEOUT_MS = 10_000L
private const val WRITE_TIMEOUT_MS = 10_000L
// 主动 disconnect 后等 didDisconnectPeripheral 回调的上限；超时强制将 state 置为 Disconnected。
private const val DISCONNECT_TIMEOUT_MS = 3_000L

/**
 * 把 NSError 的 domain / code / localizedDescription 一并拼出来，便于区分“设备返回的
 * ATT 错误”（CBATTErrorDomain）和“iOS 本地的 Core Bluetooth 错误”（CBErrorDomain）。
 *
 * 对 BrainBox OTP 流程来说两种 code 最关键：
 *  - `code=14 (0x0E)` UNLIKELY_ERROR：BLE 规范通用错误，标准 bluez 把
 *    `org.bluez.Error.Failed` 默认映射到这里。
 *  - `code=128..159 (0x80-0x9F)` APPLICATION_ERROR：BLE 规范留给上层应用/厂商
 *    自定义的错误区间。BrainBox 设备 Lucy pairing IPC 失败落在这里，说明设备端
 *    用了自定义的 ATT 错误码（非原生 bluez 映射，可能是打过补丁的 bluez 或
 *    btstack/zephyr 等其他协议栈）。
 *
 * 两种情况客户端都只能看到一个数字，细分仍然需要去设备端日志看 Lucy 插件的
 * 错误字符串（pairing ipc is not enabled / lucy pairing client is not connected /
 * pairing ipc: request timed out / pairing ipc: duplicate request id /
 * pairing ipc: <Lucy 原始错误>）。
 *
 * 格式例：
 *  "[domain=CBATTErrorDomain, code=14 UNLIKELY_ERROR->bluez Error.Failed] Unknown ATT error."
 *  "[domain=CBATTErrorDomain, code=128 (0x80) APPLICATION_ERROR->需看设备端日志] Unknown ATT error."
 */
private fun NSError.describe(): String {
    val hint = when {
        domain == "CBATTErrorDomain" && code == 14L ->
            " UNLIKELY_ERROR->bluez Error.Failed"
        domain == "CBATTErrorDomain" && code in 0x80L..0x9FL ->
            " (0x${code.toString(16).uppercase()}) APPLICATION_ERROR->设备端自定义错误,需看 Lucy/bluez 日志"
        else -> ""
    }
    return "[domain=$domain, code=$code$hint] $localizedDescription"
}

private class IosBleGattConnection(
    private val manager: CBCentralManager,
    private val peripheral: CBPeripheral,
    override val device: BleScanDevice,
) : BleGattConnection {
    override val state = MutableStateFlow(BleGattConnectionState.Idle)
    override val services = MutableStateFlow<List<BleGattService>>(emptyList())

    private val notifications = mutableMapOf<String, MutableSharedFlow<ByteArray>>()
    private var connectContinuation: kotlin.coroutines.Continuation<Result<BleGattConnection>>? = null
    private var discoverContinuation: kotlin.coroutines.Continuation<Result<List<BleGattService>>>? = null
    private val readContinuations = mutableMapOf<String, kotlin.coroutines.Continuation<Result<ByteArray>>>()
    private val writeContinuations = mutableMapOf<String, kotlin.coroutines.Continuation<Result<Unit>>>()
    private val notifyContinuations = mutableMapOf<String, kotlin.coroutines.Continuation<Result<Unit>>>()
    private var disconnectContinuation: kotlin.coroutines.Continuation<Unit>? = null

    private val delegate = object : NSObject(), CBPeripheralDelegateProtocol {
        override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
            if (didDiscoverServices != null) {
                state.value = BleGattConnectionState.Error
                discoverContinuation?.resume(Result.failure(IllegalStateException("discoverServices 失败 ${didDiscoverServices.describe()}")))
                discoverContinuation = null
                return
            }
            val servicesList = peripheral.services?.filterIsInstance<CBService>().orEmpty()
            if (servicesList.isEmpty()) {
                state.value = BleGattConnectionState.Error
                discoverContinuation?.resume(Result.failure(IllegalStateException("未发现任何 GATT Service")))
                discoverContinuation = null
                return
            }
            servicesList.forEach { service ->
                peripheral.discoverCharacteristics(null, forService = service)
            }
        }

        @ObjCSignatureOverride
        override fun peripheral(
            peripheral: CBPeripheral,
            didDiscoverCharacteristicsForService: CBService,
            error: NSError?,
        ) {
            if (error != null) {
                state.value = BleGattConnectionState.Error
                discoverContinuation?.resume(Result.failure(IllegalStateException("discoverCharacteristics 失败 ${error.describe()}")))
                discoverContinuation = null
                return
            }
            val currentServices = peripheral.services?.filterIsInstance<CBService>().orEmpty()
            val mapped = currentServices.map { it.toCommonService() }
            val allReady = currentServices.all { it.characteristics != null }
            if (allReady) {
                services.value = mapped
                state.value = BleGattConnectionState.Ready
                discoverContinuation?.resume(Result.success(mapped))
                discoverContinuation = null
            }
        }

        @ObjCSignatureOverride
        override fun peripheral(
            peripheral: CBPeripheral,
            didUpdateValueForCharacteristic: CBCharacteristic,
            error: NSError?,
        ) {
            val uuid = didUpdateValueForCharacteristic.UUID.UUIDString
            if (error != null) {
                readContinuations.remove(uuid.normalizedUuid())
                    ?.resume(Result.failure(IllegalStateException("读取特征失败 ${error.describe()}, uuid=$uuid")))
                return
            }
            val value = didUpdateValueForCharacteristic.value?.toByteArray() ?: byteArrayOf()
            val resumed = readContinuations.remove(uuid.normalizedUuid())
            if (resumed != null) {
                resumed.resume(Result.success(value))
            } else {
                notifications.getOrPut(uuid.normalizedUuid()) {
                    MutableSharedFlow(extraBufferCapacity = 32, onBufferOverflow = BufferOverflow.DROP_OLDEST)
                }.tryEmit(value)
            }
        }

        @ObjCSignatureOverride
        override fun peripheral(
            peripheral: CBPeripheral,
            didWriteValueForCharacteristic: CBCharacteristic,
            error: NSError?,
        ) {
            val uuid = didWriteValueForCharacteristic.UUID.UUIDString.normalizedUuid()
            val continuation = writeContinuations.remove(uuid) ?: return
            if (error == null) {
                continuation.resume(Result.success(Unit))
            } else {
                continuation.resume(Result.failure(IllegalStateException("写入特征失败 ${error.describe()}, uuid=$uuid")))
            }
        }

        @ObjCSignatureOverride
        override fun peripheral(
            peripheral: CBPeripheral,
            didUpdateNotificationStateForCharacteristic: CBCharacteristic,
            error: NSError?,
        ) {
            val uuid = didUpdateNotificationStateForCharacteristic.UUID.UUIDString.normalizedUuid()
            val continuation = notifyContinuations.remove(uuid) ?: return
            if (error == null) {
                continuation.resume(Result.success(Unit))
            } else {
                continuation.resume(Result.failure(IllegalStateException("设置通知失败 ${error.describe()}, uuid=$uuid")))
            }
        }
    }

    suspend fun connect(): Result<BleGattConnection> {
        val result = withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
            suspendCancellableCoroutine<Result<BleGattConnection>> { continuation ->
                connectContinuation = continuation
                state.value = BleGattConnectionState.Connecting
                peripheral.delegate = delegate
                manager.connectPeripheral(peripheral, options = null)
                continuation.invokeOnCancellation {
                    connectContinuation = null
                    runCatching { manager.cancelPeripheralConnection(peripheral) }
                }
            }
        }
        if (result != null) return result
        connectContinuation = null
        runCatching { manager.cancelPeripheralConnection(peripheral) }
        state.value = BleGattConnectionState.Error
        return Result.failure(IllegalStateException("BLE 连接超时断开"))
    }

    fun onConnected(connectedPeripheral: CBPeripheral) {
        if (connectedPeripheral != peripheral) return
        peripheral.delegate = delegate
        state.value = BleGattConnectionState.Connected
        connectContinuation?.resume(Result.success(this))
        connectContinuation = null
    }

    fun onConnectionFailed(error: NSError?) {
        state.value = BleGattConnectionState.Error
        connectContinuation?.resume(
            Result.failure(
                IllegalStateException(error?.localizedDescription ?: "GATT 连接失败")
            )
        )
        connectContinuation = null
    }

    fun onDisconnected(error: NSError?) {
        state.value = BleGattConnectionState.Disconnected
        val disconnectError = error?.localizedDescription?.takeIf { it.isNotBlank() }
            ?.let { IllegalStateException(it) }
            ?: IllegalStateException("GATT 已断开")
        failAllPending(disconnectError)
        // 主动 disconnect() 在等待平台回调确认掉线，此刻 resume。
        disconnectContinuation?.resume(Unit)
        disconnectContinuation = null
    }

    override suspend fun discoverServices(): Result<List<BleGattService>> {
        val result = withTimeoutOrNull(DISCOVER_TIMEOUT_MS) {
            suspendCancellableCoroutine<Result<List<BleGattService>>> { continuation ->
                discoverContinuation = continuation
                state.value = BleGattConnectionState.Discovering
                peripheral.discoverServices(listOf(CBUUID.UUIDWithString(BrainBoxGattProtocol.SERVICE_UUID)))
                continuation.invokeOnCancellation {
                    discoverContinuation = null
                }
            }
        }
        if (result != null) return result
        discoverContinuation = null
        state.value = BleGattConnectionState.Error
        return Result.failure(IllegalStateException("发现 GATT Service 超时断开"))
    }

    override suspend fun readCharacteristic(serviceUuid: String, characteristicUuid: String): Result<ByteArray> {
        val characteristic = findCharacteristic(serviceUuid, characteristicUuid)
            ?: return Result.failure(IllegalStateException("未找到特征 $characteristicUuid"))
        val key = characteristicUuid.normalizedUuid()
        val result = withTimeoutOrNull(READ_TIMEOUT_MS) {
            suspendCancellableCoroutine<Result<ByteArray>> { continuation ->
                readContinuations[key] = continuation
                peripheral.readValueForCharacteristic(characteristic)
                continuation.invokeOnCancellation {
                    readContinuations.remove(key)
                }
            }
        }
        if (result != null) return result
        readContinuations.remove(key)
        return Result.failure(IllegalStateException("读取特征 $characteristicUuid 超时断开"))
    }

    override suspend fun writeCharacteristic(
        serviceUuid: String,
        characteristicUuid: String,
        payload: ByteArray,
    ): Result<Unit> {
        val characteristic = findCharacteristic(serviceUuid, characteristicUuid)
            ?: return Result.failure(IllegalStateException("未找到特征 $characteristicUuid"))
        val key = characteristicUuid.normalizedUuid()
        val result = withTimeoutOrNull(WRITE_TIMEOUT_MS) {
            suspendCancellableCoroutine<Result<Unit>> { continuation ->
                writeContinuations[key] = continuation
                peripheral.writeValue(payload.toNSData(), forCharacteristic = characteristic, type = CBCharacteristicWriteWithResponse)
                continuation.invokeOnCancellation {
                    writeContinuations.remove(key)
                }
            }
        }
        if (result != null) return result
        writeContinuations.remove(key)
        return Result.failure(IllegalStateException("写入特征 $characteristicUuid 超时断开"))
    }

    override suspend fun writeCharacteristicNoResponse(
        serviceUuid: String,
        characteristicUuid: String,
        payload: ByteArray,
    ): Result<Unit> {
        val characteristic = findCharacteristic(serviceUuid, characteristicUuid)
            ?: return Result.failure(IllegalStateException("未找到特征 $characteristicUuid"))
        return runCatching {
            peripheral.writeValue(payload.toNSData(), forCharacteristic = characteristic, type = 1) // CBCharacteristicWriteWithoutResponse
        }
    }

    override suspend fun setCharacteristicNotification(
        serviceUuid: String,
        characteristicUuid: String,
        enabled: Boolean,
    ): Result<Unit> {
        val characteristic = findCharacteristic(serviceUuid, characteristicUuid)
            ?: return Result.failure(IllegalStateException("未找到特征 $characteristicUuid"))
        return suspendCancellableCoroutine { continuation ->
            notifyContinuations[characteristicUuid.normalizedUuid()] = continuation
            peripheral.setNotifyValue(enabled, forCharacteristic = characteristic)
        }
    }

    override fun observeCharacteristic(serviceUuid: String, characteristicUuid: String): Flow<ByteArray> {
        return notifications.getOrPut(characteristicUuid.normalizedUuid()) {
            MutableSharedFlow(extraBufferCapacity = 32, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        }
    }

    override suspend fun disconnect() {
        // 已经断开（例如 disconnectWatcher 先检测到）直接返回，避免无谓等待。
        val current = state.value
        if (current == BleGattConnectionState.Disconnected || current == BleGattConnectionState.Idle) {
            return
        }
        // 挂起直到 didDisconnectPeripheral 回调（onDisconnected resume），确保上层
        // 的 waitUntilDisconnected 语义详实有效。
        val completed = withTimeoutOrNull(DISCONNECT_TIMEOUT_MS) {
            suspendCancellableCoroutine<Unit> { continuation ->
                disconnectContinuation = continuation
                manager.cancelPeripheralConnection(peripheral)
            }
        }
        if (completed == null) {
            println("[BrainBox] disconnect 超时未收到 didDisconnectPeripheral 回调，强制清理")
            disconnectContinuation = null
        }
        state.value = BleGattConnectionState.Disconnected
    }

    private fun failAllPending(error: Throwable) {
        discoverContinuation?.resume(Result.failure(error))
        discoverContinuation = null
        readContinuations.values.toList().forEach { it.resume(Result.failure(error)) }
        readContinuations.clear()
        writeContinuations.values.toList().forEach { it.resume(Result.failure(error)) }
        writeContinuations.clear()
        notifyContinuations.values.toList().forEach { it.resume(Result.failure(error)) }
        notifyContinuations.clear()
    }

    private fun findCharacteristic(serviceUuid: String, characteristicUuid: String): CBCharacteristic? {
        return peripheral.services
            ?.filterIsInstance<CBService>()
            ?.firstOrNull { it.UUID.UUIDString.normalizedUuid() == serviceUuid.normalizedUuid() }
            ?.characteristics
            ?.filterIsInstance<CBCharacteristic>()
            ?.firstOrNull { it.UUID.UUIDString.normalizedUuid() == characteristicUuid.normalizedUuid() }
    }
}

private fun CBService.toCommonService(): BleGattService {
    return BleGattService(
        uuid = UUID.UUIDString,
        characteristics = characteristics
            ?.filterIsInstance<CBCharacteristic>()
            .orEmpty()
            .map { characteristic ->
                BleGattCharacteristic(
                    serviceUuid = UUID.UUIDString,
                    uuid = characteristic.UUID.UUIDString,
                    properties = characteristic.properties.toInt(),
                )
            },
    )
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)
    return ByteArray(length).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, this@toByteArray.length)
        }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return memScoped {
        NSData.create(
            bytes = allocArrayOf(this@toNSData),
            length = this@toNSData.size.toULong(),
        )
    }
}
