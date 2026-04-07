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
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat
import platform.Photos.PHImageRequestOptionsResizeModeExact
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIScreen
import platform.UIKit.UIColor
import platform.UIKit.UIView

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun PlatformImageThumbnail(
    uri: String,
    modifier: Modifier,
) {
    val image by produceState<UIImage?>(initialValue = null, key1 = uri) {
        value = loadIosThumbnail(uri)
    }

    UIKitView(
        modifier = modifier,
        factory = {
<<<<<<< Updated upstream
            val container = UIView(frame = CGRectMake(0.0, 0.0, 1.0, 1.0))
            container.backgroundColor = UIColor.colorWithWhite(0.93, alpha = 1.0)
            val imageView = UIImageView(frame = container.bounds)
            imageView.contentMode = platform.UIKit.UIViewContentMode.UIViewContentModeScaleAspectFill
            imageView.clipsToBounds = true
            imageView.backgroundColor = UIColor.clearColor
            container.addSubview(imageView)
            container
        },
        update = { view ->
            val imageView = view.subviews.firstOrNull() as? UIImageView
            if (imageView != null) {
                imageView.setFrame(view.bounds)
                imageView.image = image
=======
            val imageView = UIImageView(frame = CGRectMake(0.0, 0.0, 1.0, 1.0))
            imageView.contentMode = platform.UIKit.UIViewContentMode.UIViewContentModeScaleAspectFill
            imageView.clipsToBounds = true
            imageView
        },
        update = { imageView ->
            imageView.image = null
            if (uri.startsWith("ios-phasset://")) {
                val localId = uri.removePrefix("ios-phasset://")
                val result = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(localId), null)
                val asset = result.firstObject as? PHAsset
                if (asset == null) {
                    imageView.image = null
                } else {
                    val options = PHImageRequestOptions().apply {
                        resizeMode = PHImageRequestOptionsResizeModeFast
                        deliveryMode = PHImageRequestOptionsDeliveryModeOpportunistic
                        networkAccessAllowed = true
                    }
                    val size = imageView.bounds.useContents {
                        val w = size.width
                        val h = size.height
                        if (w <= 1.0 || h <= 1.0) CGSizeMake(120.0, 120.0) else CGSizeMake(w, h)
                    }
                    PHImageManager.defaultManager().requestImageForAsset(
                        asset = asset,
                        targetSize = size,
                        contentMode = PHImageContentModeAspectFill,
                        options = options
                    ) { image, _ ->
                        dispatch_async(dispatch_get_main_queue()) {
                            imageView.image = image
                        }
                    }
                }
            } else {
                val path = when {
                    uri.startsWith("file://") -> NSURL.URLWithString(uri)?.path ?: uri.removePrefix("file://")
                    else -> uri
                }
                imageView.image = path.takeIf { it.isNotBlank() }?.let { UIImage.imageWithContentsOfFile(it) }
>>>>>>> Stashed changes
            }
        }
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun loadIosThumbnail(uri: String): UIImage? {
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

        val screenScale = UIScreen.mainScreen.scale
        val targetSize = CGSizeMake(120.0 * screenScale, 120.0 * screenScale)
        var loadedImage: UIImage? = null

        PHImageManager.defaultManager().requestImageForAsset(
            asset = asset,
            targetSize = targetSize,
            contentMode = PHImageContentModeAspectFill,
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
