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
    val pickedImages: List<String>
    val pickedFiles: List<String>
    val recentImages: List<String>

    fun openCamera()
    fun openGallery()
    fun openFilePicker()

    fun startRecording()
    fun finishRecording()
    fun cancelRecording()

    fun toggleRecording() {
        if (isRecording) finishRecording() else startRecording()
    }
    fun playRecording(recording: AudioRecording)

    fun refreshRecentImages()
}

@Composable
expect fun rememberPlatformMediaAccessController(
    onEvent: (String) -> Unit
): PlatformMediaAccessController
