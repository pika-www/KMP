package com.example.androidios.media

import androidx.compose.runtime.Composable

interface PlatformMediaAccessController {
    fun openCamera()
    fun openGallery()
    fun openFilePicker()
}

@Composable
expect fun rememberPlatformMediaAccessController(
    onEvent: (String) -> Unit
): PlatformMediaAccessController
