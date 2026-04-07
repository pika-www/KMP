package com.cephalon.lucyApp.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectMake
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIColor
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun QrScannerView(
    modifier: Modifier,
    enabled: Boolean,
    onQrCodeScanned: (String) -> Unit,
) {
    val holder = remember { IosQrScannerHolder(onQrCodeScanned) }

    DisposableEffect(enabled) {
        if (enabled) {
            holder.start()
        } else {
            holder.stop()
        }
        onDispose {
            holder.stop()
        }
    }

    UIKitView(
        modifier = modifier,
        factory = {
            holder.view
        },
        update = { view ->
            holder.updateFrame(view.bounds)
        },
        onResize = { _, rect ->
            holder.updateFrame(rect)
        }
    )
}

@OptIn(ExperimentalForeignApi::class)
private class IosQrScannerHolder(
    private val onQrCodeScanned: (String) -> Unit,
) {
    val session: AVCaptureSession = AVCaptureSession()
    private val metadataOutput: AVCaptureMetadataOutput = AVCaptureMetadataOutput()
    private val delegate = MetadataDelegate(onQrCodeScanned)

    private val statusLabel: UILabel = UILabel(frame = CGRectMake(0.0, 0.0, 1.0, 1.0)).apply {
        textAlignment = NSTextAlignmentCenter
        textColor = UIColor.whiteColor
        numberOfLines = 0
        hidden = true
    }

    private val previewLayer: AVCaptureVideoPreviewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
        videoGravity = AVLayerVideoGravityResizeAspectFill
    }

    val view: UIView = UIView(frame = CGRectMake(0.0, 0.0, 1.0, 1.0)).apply {
        previewLayer.frame = bounds
        layer.addSublayer(previewLayer)
        addSubview(statusLabel)
    }
    private var configured = false
    private var configurationFailed = false

    fun updateFrame(bounds: CValue<platform.CoreGraphics.CGRect>) {
        previewLayer.frame = bounds
        statusLabel.setFrame(bounds)
    }

    fun start() {
        ensureConfigured()
        if (configurationFailed) return
        if (!session.running) {
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
                session.startRunning()
            }
        }
    }

    fun stop() {
        if (session.running) {
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
                session.stopRunning()
            }
        }
        delegate.reset()
    }

    private fun ensureConfigured() {
        if (configured) return
        if (configurationFailed) return

        val device = run {
            val discovery = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
                deviceTypes = listOf(AVCaptureDeviceTypeBuiltInWideAngleCamera),
                mediaType = AVMediaTypeVideo,
                position = AVCaptureDevicePositionBack
            )
            val devices = discovery?.devices as? List<*>
            (devices?.firstOrNull() as? AVCaptureDevice)
                ?: AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        }

        if (device == null) {
            configurationFailed = true
            statusLabel.text = "未找到相机设备\n（iOS 模拟器通常不支持相机预览）"
            statusLabel.hidden = false
            return
        }

        val input = (AVCaptureDeviceInput.deviceInputWithDevice(device, error = null) as? AVCaptureDeviceInput)
        if (input == null) {
            configurationFailed = true
            statusLabel.text = "相机初始化失败"
            statusLabel.hidden = false
            return
        }

        session.beginConfiguration()
        session.sessionPreset = AVCaptureSessionPresetHigh

        if (session.canAddInput(input)) {
            session.addInput(input)
        }

        if (session.canAddOutput(metadataOutput)) {
            session.addOutput(metadataOutput)
            metadataOutput.setMetadataObjectsDelegate(delegate, queue = dispatch_get_main_queue())

            val supportedTypes = metadataOutput.availableMetadataObjectTypes
            if (supportedTypes?.contains(AVMetadataObjectTypeQRCode) == true) {
                metadataOutput.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
            }
        }

        session.commitConfiguration()
        configured = true

        previewLayer.connection?.let { connection ->
            if (connection.supportsVideoOrientation) {
                connection.videoOrientation = AVCaptureVideoOrientationPortrait
            }
        }

        statusLabel.hidden = true
    }
}

@OptIn(ExperimentalForeignApi::class)
private class MetadataDelegate(
    private val onQrCodeScanned: (String) -> Unit,
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    private var scannedOnce = false

    fun reset() {
        scannedOnce = false
    }

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        if (scannedOnce) return

        val first = didOutputMetadataObjects.firstOrNull() as? AVMetadataMachineReadableCodeObject
        val value = first?.stringValue
        if (!value.isNullOrBlank()) {
            scannedOnce = true
            onQrCodeScanned(value)
        }
    }
}
