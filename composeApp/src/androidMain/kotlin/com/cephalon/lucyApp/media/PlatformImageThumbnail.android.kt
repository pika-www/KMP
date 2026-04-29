package com.cephalon.lucyApp.media

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

@Composable
actual fun PlatformImageThumbnail(
    uri: String,
    modifier: Modifier,
) {
    val bitmap = rememberPlatformImageBitmap(uri)
    if (bitmap == null) {
        Box(modifier = modifier.background(Color(0xFFEDEDED)))
    } else {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            alignment = androidx.compose.ui.Alignment.Center
        )
    }
}

@Composable
actual fun PlatformImagePreview(
    uri: String,
    modifier: Modifier,
) {
    val bitmap = rememberPlatformImageBitmap(uri)
    if (bitmap == null) {
        Box(modifier = modifier.background(Color.White))
    } else {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}

private val thumbnailCache = LruCache<String, ImageBitmap>(150)

@Composable
private fun rememberPlatformImageBitmap(uri: String): ImageBitmap? {
    val context = LocalContext.current

    val imageBitmapState by produceState<ImageBitmap?>(initialValue = thumbnailCache.get(uri), key1 = uri) {
        thumbnailCache.get(uri)?.let { value = it; return@produceState }
        value = withContext(Dispatchers.IO) {
            try {
                if (uri.startsWith("android-bitmap-preview://")) {
                    null
                } else {
                    val parsed = Uri.parse(uri)
                    val bitmap = context.contentResolver.openInputStream(parsed)?.use { input ->
                        BitmapFactory.decodeStream(input)?.asImageBitmap()
                    }
                    bitmap?.also { thumbnailCache.put(uri, it) }
                }
            } catch (_: Throwable) {
                null
            }
        }
    }

    return imageBitmapState
}
