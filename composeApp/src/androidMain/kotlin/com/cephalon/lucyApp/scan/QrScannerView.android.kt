package com.cephalon.lucyApp.scan

import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
actual fun QrScannerView(
    modifier: Modifier,
    enabled: Boolean,
    onQrCodeScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isActive = enabled
    val scanner = remember { BarcodeScanning.getClient() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            scanner.close()
        }
    }

    var scannedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) {
        if (!enabled) {
            scannedOnce = false
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            }
        },
        update = { previewView ->
            if (!isActive) {
                return@AndroidView
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener(
                {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analysis.setAnalyzer(executor) { imageProxy ->
                        if (scannedOnce || !enabled) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                val raw = barcodes.firstOrNull()?.rawValue
                                if (!raw.isNullOrBlank() && !scannedOnce) {
                                    scannedOnce = true
                                    onQrCodeScanned(raw)
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    }

                    val selector = CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                },
                ContextCompat.getMainExecutor(context)
            )
        }
    )
}
