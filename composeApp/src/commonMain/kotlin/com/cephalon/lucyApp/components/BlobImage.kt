package com.cephalon.lucyApp.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.sdk.SdkSessionManager
import org.koin.compose.koinInject

@Composable
fun BlobImage(
    blobRef: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable (() -> Unit)? = null,
    errorContent: @Composable (() -> Unit)? = null,
) {
    val sdkSessionManager = koinInject<SdkSessionManager>()
    var imageBitmap by remember(blobRef) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(blobRef) { mutableStateOf(true) }
    var isError by remember(blobRef) { mutableStateOf(false) }

    LaunchedEffect(blobRef) {
        if (blobRef.isBlank()) {
            isLoading = false
            isError = true
            return@LaunchedEffect
        }
        isLoading = true
        isError = false
        sdkSessionManager.fetchBlobBytes(blobRef)
            .onSuccess { bytes ->
                val bitmap = decodeImageBytes(bytes)
                if (bitmap != null) {
                    imageBitmap = bitmap
                } else {
                    isError = true
                }
            }
            .onFailure {
                isError = true
            }
        isLoading = false
    }

    when {
        imageBitmap != null -> {
            Image(
                painter = BitmapPainter(imageBitmap!!),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
            )
        }
        isLoading -> {
            if (placeholder != null) {
                placeholder()
            } else {
                Box(modifier = modifier, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White.copy(alpha = 0.5f),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
        else -> {
            errorContent?.invoke()
        }
    }
}

expect fun decodeImageBytes(bytes: ByteArray): ImageBitmap?
