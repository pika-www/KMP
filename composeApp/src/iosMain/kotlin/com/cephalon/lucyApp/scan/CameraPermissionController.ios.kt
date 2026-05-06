package com.cephalon.lucyApp.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberCameraPermissionController(): CameraPermissionController {
    var hasCameraPermission by remember {
        mutableStateOf(
            AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized
        )
    }
    var permissionResponseCount by remember { mutableStateOf(0) }

    return object : CameraPermissionController {
        override val hasPermission: Boolean
            get() = hasCameraPermission

        override val responseCount: Int
            get() = permissionResponseCount

        override fun requestPermission() {
            val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
            if (status == AVAuthorizationStatusAuthorized) {
                hasCameraPermission = true
                permissionResponseCount++
                return
            }

            if (status == AVAuthorizationStatusNotDetermined) {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted: Boolean ->
                    dispatch_async(dispatch_get_main_queue()) {
                        hasCameraPermission = granted
                        permissionResponseCount++
                    }
                }
            } else {
                hasCameraPermission = false
                permissionResponseCount++
            }
        }
    }
}
