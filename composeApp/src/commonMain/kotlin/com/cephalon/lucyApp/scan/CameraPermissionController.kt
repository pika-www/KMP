package com.cephalon.lucyApp.scan

import androidx.compose.runtime.Composable

interface CameraPermissionController {
    val hasPermission: Boolean
    /** Incremented every time the runtime delivers a permission-request result. */
    val responseCount: Int
    fun requestPermission()
}

@Composable
expect fun rememberCameraPermissionController(): CameraPermissionController
