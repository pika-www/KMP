package com.cephalon.lucyApp.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSURL
import platform.Photos.PHAsset
import platform.Photos.PHImageContentModeAspectFill
import platform.Photos.PHImageContentModeAspectFit
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat
import platform.Photos.PHImageRequestOptionsResizeModeExact
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIScreen

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun PlatformImageThumbnail(
    uri: String,
    modifier: Modifier,
) {
    val image by produceState<UIImage?>(initialValue = null, key1 = uri) {
        value = loadIosImage(
            uri = uri,
            targetPixels = UIScreen.mainScreen.scale * 160.0,
            requestContentMode = PHImageContentModeAspectFill
        )
    }

    IOSPlatformImageView(
        image = image,
        modifier = modifier,
        isPreview = false
    )
}

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun PlatformImagePreview(
    uri: String,
    modifier: Modifier,
) {
    val image by produceState<UIImage?>(initialValue = null, key1 = uri) {
        value = loadIosImage(
            uri = uri,
            targetPixels = UIScreen.mainScreen.scale * 1600.0,
            requestContentMode = PHImageContentModeAspectFit
        )
    }

    IOSPlatformImageView(
        image = image,
        modifier = modifier,
        isPreview = true
    )
}

@Composable
@OptIn(ExperimentalForeignApi::class)
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
                backgroundColor = if (isPreview) UIColor.clearColor else UIColor.colorWithWhite(0.93, alpha = 1.0)
            }
        },
        update = { imageView ->
            imageView.contentMode = if (isPreview) {
                platform.UIKit.UIViewContentMode.UIViewContentModeScaleAspectFit
            } else {
                platform.UIKit.UIViewContentMode.UIViewContentModeScaleAspectFill
            }
            imageView.backgroundColor = if (isPreview) UIColor.clearColor else UIColor.colorWithWhite(0.93, alpha = 1.0)
            imageView.image = image
        }
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun loadIosImage(
    uri: String,
    targetPixels: Double,
    requestContentMode: Long,
): UIImage? {
    return if (uri.startsWith("ios-phasset://")) {
        val localId = uri.removePrefix("ios-phasset://")
        val result = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(localId), null)
        val asset = result.firstObject as? PHAsset ?: return null

        val options = PHImageRequestOptions().apply {
            resizeMode = PHImageRequestOptionsResizeModeExact
            deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat
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
