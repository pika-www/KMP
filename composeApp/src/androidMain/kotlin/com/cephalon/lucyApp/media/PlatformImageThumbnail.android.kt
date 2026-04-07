package com.cephalon.lucyApp.media

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun PlatformImageThumbnail(
    uri: String,
    modifier: Modifier,
) {
    val bitmap = rememberPlatformImageBitmap(uri)
    if (bitmap == null) {
        Box(modifier = modifier.background(Color(0xFFEDEDED)))
    } else {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier)
    }
}

@Composable
actual fun PlatformImagePreview(
    uri: String,
    modifier: Modifier,
) {
    val bitmap = rememberPlatformImageBitmap(uri)
    if (bitmap == null) {
        Box(modifier = modifier.background(Color(0xFF111111)))
    } else {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun rememberPlatformImageBitmap(uri: String): ImageBitmap? {
    val context = LocalContext.current

    val imageBitmapState by produceState<ImageBitmap?>(initialValue = null, key1 = uri) {
        value = try {
            if (uri.startsWith("android-bitmap-preview://")) {
                null
            } else {
                val parsed = Uri.parse(uri)
                context.contentResolver.openInputStream(parsed)?.use { input ->
                    val bmp = BitmapFactory.decodeStream(input) ?: return@use null
                    bmp.asImageBitmap()
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    return imageBitmapState
}
