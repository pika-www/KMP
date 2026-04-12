package com.cephalon.lucyApp.media

import androidx.compose.runtime.Composable

data class PickedFile(
    val uri: String,
    val displayName: String
)

data class AudioRecording(
    val id: String,
    val name: String,
    val path: String
)

data class AudioPlaybackState(
    val sourceId: String? = null,
    val sourceName: String = "",
    val isPlaying: Boolean = false,
    val currentPositionMillis: Long = 0L,
    val durationMillis: Long = 0L
)

data class VoiceInputResult(
    val transcribedText: String,
    val recording: AudioRecording?
)

interface PlatformMediaAccessController {
    val isRecording: Boolean
    val recordings: List<AudioRecording>
    val pickedImages: List<String>
    val pickedFiles: List<PickedFile>
    val recentImages: List<String>
    val playingRecordingId: String?
    val audioPlaybackState: AudioPlaybackState

    fun openCamera()
    fun openGallery()
    fun openAudioPicker()
    fun openFilePicker()
    fun openFilePreview(file: PickedFile)

    fun startVoiceInput()
    fun finishVoiceInput(onResult: (VoiceInputResult) -> Unit)
    fun cancelVoiceInput()

    fun toggleRecordingPlayback(recording: AudioRecording)
    fun toggleAudioPlayback(sourceId: String, name: String, source: String)
    fun seekAudioPlaybackTo(positionMillis: Long)
    fun skipAudioPlaybackBy(deltaMillis: Long)
    fun stopAudioPlayback()

    fun refreshRecentImages()

    suspend fun readUriToBytes(uri: String): ByteArray?
}

@Composable
expect fun rememberPlatformMediaAccessController(
    onEvent: (String) -> Unit
): PlatformMediaAccessController
