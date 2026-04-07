package com.cephalon.lucyApp.scan

import androidx.compose.runtime.Composable

interface CameraPermissionController {
    val hasPermission: Boolean
    fun requestPermission()
}

@Composable
expect fun rememberCameraPermissionController(): CameraPermissionController
