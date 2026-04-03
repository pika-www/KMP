package com.cephalon.lucyApp.media

import androidx.compose.runtime.Composable

data class AudioRecording(
    val id: String,
    val name: String,
    val path: String
)

interface PlatformMediaAccessController {
    val isRecording: Boolean
    val recordings: List<AudioRecording>

    fun openCamera()
    fun openGallery()
    fun openFilePicker()
    fun toggleRecording()
    fun playRecording(recording: AudioRecording)
}

@Composable
expect fun rememberPlatformMediaAccessController(
    onEvent: (String) -> Unit
): PlatformMediaAccessController
