package com.cephalon.lucyApp.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSURL
import platform.Photos.PHAsset
import platform.Photos.PHImageContentModeAspectFill
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeOpportunistic
import platform.Photos.PHImageRequestOptionsResizeModeFast
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIView
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun PlatformImageThumbnail(
    uri: String,
    modifier: Modifier,
) {
    UIKitView(
        modifier = modifier,
        factory = {
            val container = UIView(frame = CGRectMake(0.0, 0.0, 1.0, 1.0))
            val imageView = UIImageView(frame = container.bounds)
            imageView.contentMode = platform.UIKit.UIViewContentMode.UIViewContentModeScaleAspectFill
            imageView.clipsToBounds = true
            container.addSubview(imageView)
            container
        },
        update = { view ->
            val imageView = view.subviews.firstOrNull() as? UIImageView
            if (imageView != null) {
                imageView.setFrame(view.bounds)
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
                            networkAccessAllowed = false
                        }
                        val size = view.bounds.useContents {
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
                }
            }
        }
    )
}
