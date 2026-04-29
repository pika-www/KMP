package com.cephalon.lucyApp.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSURL
import platform.Photos.PHAsset
import platform.Photos.PHImageContentModeAspectFill
import platform.Photos.PHImageContentModeAspectFit
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeFastFormat
import platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat
import platform.Photos.PHImageRequestOptionsResizeModeFast
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImageView
import platform.UIKit.UIScreen
import platform.posix.memcpy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage

private val thumbnailBitmapCache = HashMap<String, ImageBitmap>(150)
private const val THUMBNAIL_CACHE_MAX = 200

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun PlatformImageThumbnail(
    uri: String,
    modifier: Modifier,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = thumbnailBitmapCache[uri], key1 = uri) {
        thumbnailBitmapCache[uri]?.let { value = it; return@produceState }
        value = withContext(Dispatchers.IO) {
            val uiImage = loadIosImage(
                uri = uri,
                targetPixels = UIScreen.mainScreen.scale * 300.0,
                requestContentMode = PHImageContentModeAspectFill,
                deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat
            ) ?: return@withContext null
            val bmp = uiImageToImageBitmap(uiImage) ?: return@withContext null
            if (thumbnailBitmapCache.size >= THUMBNAIL_CACHE_MAX) {
                thumbnailBitmapCache.keys.firstOrNull()?.let { thumbnailBitmapCache.remove(it) }
            }
            thumbnailBitmapCache[uri] = bmp
            bmp
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            alignment = androidx.compose.ui.Alignment.Center
        )
    } else {
        Box(modifier = modifier.background(Color(0xFFEDEDED)))
    }
}

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun PlatformImagePreview(
    uri: String,
    modifier: Modifier,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = uri) {
        value = withContext(Dispatchers.IO) {
            val uiImage = loadIosImage(
                uri = uri,
                targetPixels = UIScreen.mainScreen.scale * 1200.0,
                requestContentMode = PHImageContentModeAspectFit,
                deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat
            ) ?: return@withContext null
            uiImageToImageBitmap(uiImage)
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Box(modifier = modifier.background(Color.White))
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
private fun IOSPlatformImageView(
    image: UIImage?,
    modifier: Modifier,
    isPreview: Boolean,
) {
    UIKitView(
        modifier = modifier,
        factory = {
            UIImageView(frame = CGRectMake(0.0, 0.0, 1.0, 1.0)).apply {
                contentMode = if (isPreview) {
                    platform.UIKit.UIViewContentMode.UIViewContentModeScaleAspectFit
                } else {
                    platform.UIKit.UIViewContentMode.UIViewContentModeScaleAspectFill
                }
                clipsToBounds = true
                backgroundColor = if (isPreview) UIColor.whiteColor else UIColor.colorWithWhite(0.93, alpha = 1.0)
            }
        },
        update = { imageView ->
            imageView.contentMode = if (isPreview) {
                platform.UIKit.UIViewContentMode.UIViewContentModeScaleAspectFit
            } else {
                platform.UIKit.UIViewContentMode.UIViewContentModeScaleAspectFill
            }
            imageView.backgroundColor = if (isPreview) UIColor.whiteColor else UIColor.colorWithWhite(0.93, alpha = 1.0)
            imageView.image = image
        },
        properties = UIKitInteropProperties(
            isInteractive = false,
            isNativeAccessibilityEnabled = false
        )
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun loadIosImage(
    uri: String,
    targetPixels: Double,
    requestContentMode: Long,
    deliveryMode: Long = PHImageRequestOptionsDeliveryModeFastFormat,
): UIImage? {
    return if (uri.startsWith("ios-phasset://")) {
        val localId = uri.removePrefix("ios-phasset://")
        val result = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(localId), null)
        val asset = result.firstObject as? PHAsset ?: return null

        val options = PHImageRequestOptions().apply {
            resizeMode = PHImageRequestOptionsResizeModeFast
            this.deliveryMode = deliveryMode
            networkAccessAllowed = true
            synchronous = true
        }

        var loadedImage: UIImage? = null

        PHImageManager.defaultManager().requestImageForAsset(
            asset = asset,
            targetSize = CGSizeMake(targetPixels, targetPixels),
            contentMode = requestContentMode,
            options = options
        ) { image, _ ->
            loadedImage = image
        }

        loadedImage
    } else {
        val path = when {
            uri.startsWith("file://") -> NSURL.URLWithString(uri)?.path ?: uri.removePrefix("file://")
            else -> uri
        }
        path.takeIf { it.isNotBlank() }?.let { UIImage.imageWithContentsOfFile(it) }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun uiImageToImageBitmap(uiImage: UIImage): ImageBitmap? {
    val jpegData = UIImageJPEGRepresentation(uiImage, 1.0) ?: return null
    val length = jpegData.length.toInt()
    if (length == 0) return null
    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), jpegData.bytes, jpegData.length)
    }
    return SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
}
