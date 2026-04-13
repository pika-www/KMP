package com.cephalon.lucyApp.media

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidios.composeapp.generated.resources.Res
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.withContext
import java.util.UUID

private class AndroidPlatformMediaAccessController(
    override val isRecording: Boolean,
    override val recordings: List<AudioRecording>,
    override val pickedImages: List<String>,
    override val pickedFiles: List<PickedFile>,
    override val recentImages: List<String>,
    override val playingRecordingId: String?,
    override val audioPlaybackState: AudioPlaybackState,
    private val onOpenCamera: () -> Unit,
    private val onOpenGallery: () -> Unit,
    private val onOpenAudioPicker: () -> Unit,
    private val onOpenFilePicker: () -> Unit,
    private val onOpenFilePreview: (PickedFile) -> Unit,
    private val onStartVoiceInput: () -> Unit,
    private val onFinishVoiceInput: ((VoiceInputResult) -> Unit) -> Unit,
    private val onCancelVoiceInput: () -> Unit,
    private val onToggleRecordingPlayback: (AudioRecording) -> Unit,
    private val onToggleAudioPlayback: (String, String, String) -> Unit,
    private val onSeekAudioPlaybackTo: (Long) -> Unit,
    private val onSkipAudioPlaybackBy: (Long) -> Unit,
    private val onStopAudioPlayback: () -> Unit,
    private val onRefreshRecentImages: () -> Unit,
    private val onReadUriToBytes: suspend (String) -> ByteArray?,
) : PlatformMediaAccessController {
    override fun openCamera() = onOpenCamera()

    override fun openGallery() = onOpenGallery()

    override fun openAudioPicker() = onOpenAudioPicker()

    override fun openFilePicker() = onOpenFilePicker()

    override fun openFilePreview(file: PickedFile) = onOpenFilePreview(file)

    override fun startVoiceInput() = onStartVoiceInput()

    override fun finishVoiceInput(onResult: (VoiceInputResult) -> Unit) = onFinishVoiceInput(onResult)

    override fun cancelVoiceInput() = onCancelVoiceInput()

    override fun toggleRecordingPlayback(recording: AudioRecording) = onToggleRecordingPlayback(recording)

    override fun toggleAudioPlayback(sourceId: String, name: String, source: String) =
        onToggleAudioPlayback(sourceId, name, source)

    override fun seekAudioPlaybackTo(positionMillis: Long) = onSeekAudioPlaybackTo(positionMillis)

    override fun skipAudioPlaybackBy(deltaMillis: Long) = onSkipAudioPlaybackBy(deltaMillis)

    override fun stopAudioPlayback() = onStopAudioPlayback()

    override fun refreshRecentImages() = onRefreshRecentImages()

    override suspend fun readUriToBytes(uri: String): ByteArray? = onReadUriToBytes(uri)
}

@Composable
actual fun rememberPlatformMediaAccessController(
    onEvent: (String) -> Unit
): PlatformMediaAccessController {
    val context = LocalContext.current
    val currentOnEvent = rememberUpdatedState(onEvent)
    val recordings = remember { mutableStateListOf<AudioRecording>() }
    val pickedImages = remember { mutableStateListOf<String>() }
    val pickedFiles = remember { mutableStateListOf<PickedFile>() }
    val recentImages = remember { mutableStateListOf<String>() }
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var currentRecordingFile by remember { mutableStateOf<File?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingRecordingId by remember { mutableStateOf<String?>(null) }
    var audioPlaybackState by remember { mutableStateOf(AudioPlaybackState()) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    var latestVoiceText by remember { mutableStateOf("") }
    var latestVoiceRecording by remember { mutableStateOf<AudioRecording?>(null) }
    var voiceResultCallback by remember { mutableStateOf<((VoiceInputResult) -> Unit)?>(null) }
    var voiceSessionAction by remember { mutableStateOf(AndroidVoiceSessionAction.Idle) }
    var previousAudioMode by remember { mutableStateOf(AudioManager.MODE_NORMAL) }
    var previousSpeakerphoneOn by remember { mutableStateOf(false) }
    var bluetoothScoStarted by remember { mutableStateOf(false) }

    fun configureVoiceInputRoute() {
        audioManager ?: return
        previousAudioMode = audioManager.mode
        previousSpeakerphoneOn = audioManager.isSpeakerphoneOn
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false
        runCatching {
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true
            bluetoothScoStarted = true
        }
    }

    fun restoreVoiceInputRoute() {
        audioManager ?: return
        if (bluetoothScoStarted) {
            runCatching {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }
            bluetoothScoStarted = false
        }
        audioManager.isSpeakerphoneOn = previousSpeakerphoneOn
        audioManager.mode = previousAudioMode
    }

    fun stopCurrentRecordingFile(): File? {
        val file = currentRecordingFile
        stopAndroidRecording(mediaRecorder)
        mediaRecorder = null
        currentRecordingFile = null
        restoreVoiceInputRoute()
        return file?.takeIf { it.exists() }
    }

    fun stopAndBuildCurrentRecording(): AudioRecording? {
        val file = stopCurrentRecordingFile()
        return if (file != null && file.exists()) {
            AudioRecording(
                id = file.absolutePath,
                name = file.name,
                path = file.absolutePath
            )
        } else {
            null
        }
    }

    fun discardCurrentRecording() {
        val file = stopCurrentRecordingFile()
        if (file != null) {
            try {
                file.delete()
            } catch (_: Throwable) {
            }
        }
        latestVoiceRecording = null
    }

    fun resetVoiceSession() {
        isRecording = false
        latestVoiceText = ""
        voiceResultCallback = null
        latestVoiceRecording = null
        voiceSessionAction = AndroidVoiceSessionAction.Idle
    }

    fun dispatchVoiceResult(text: String) {
        val callback = voiceResultCallback
        val resultText = text.trim()
        discardCurrentRecording()
        resetVoiceSession()
        if (resultText.isNotBlank()) {
            currentOnEvent.value("语音转文字完成。")
        } else {
            currentOnEvent.value("未识别到语音内容。")
        }
        callback?.invoke(
            VoiceInputResult(
                transcribedText = resultText,
                recording = null
            )
        )
    }

    fun ensureSpeechRecognizer(): SpeechRecognizer? {
        speechRecognizer?.let { return it }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            currentOnEvent.value("当前设备不支持语音转文字。")
            return null
        }
        return SpeechRecognizer.createSpeechRecognizer(context).also { recognizer ->
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit

                override fun onBeginningOfSpeech() = Unit

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    when (voiceSessionAction) {
                        AndroidVoiceSessionAction.Cancelling -> resetVoiceSession()
                        AndroidVoiceSessionAction.Finishing -> {
                            val fallbackText = latestVoiceText.trim()
                            dispatchVoiceResult(fallbackText)
                        }
                        AndroidVoiceSessionAction.Active -> {
                            discardCurrentRecording()
                            resetVoiceSession()
                            currentOnEvent.value("语音输入中断，请重试。")
                        }
                        AndroidVoiceSessionAction.Idle -> Unit
                    }
                }

                override fun onResults(results: Bundle?) {
                    val resultText = extractSpeechText(results).ifBlank { latestVoiceText }.trim()
                    if (voiceSessionAction == AndroidVoiceSessionAction.Finishing) {
                        dispatchVoiceResult(resultText)
                    } else {
                        resetVoiceSession()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    latestVoiceText = extractSpeechText(partialResults)
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            speechRecognizer = recognizer
        }
    }

    fun resetAudioPlaybackState() {
        audioPlaybackState = AudioPlaybackState()
        playingRecordingId = null
    }

    fun releaseAudioPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun stopAudioPlayback(resetState: Boolean = true) {
        val player = mediaPlayer
        mediaPlayer = null
        if (player != null) {
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (_: Throwable) {
            } finally {
                player.release()
            }
        }
        if (resetState) {
            resetAudioPlaybackState()
        }
    }

    fun updateAudioPlaybackSnapshot(player: MediaPlayer? = mediaPlayer) {
        if (player == null) return
        val currentSourceId = audioPlaybackState.sourceId ?: return
        audioPlaybackState = audioPlaybackState.copy(
            sourceId = currentSourceId,
            isPlaying = player.isPlaying,
            currentPositionMillis = runCatching { player.currentPosition.toLong() }
                .getOrDefault(audioPlaybackState.currentPositionMillis)
                .coerceAtLeast(0L),
            durationMillis = runCatching { player.duration.toLong() }
                .getOrDefault(audioPlaybackState.durationMillis)
                .coerceAtLeast(0L)
        )
    }

    fun prepareMediaPlayer(source: String, fileName: String): MediaPlayer {
        return MediaPlayer().apply {
            when {
                source.startsWith("content://") -> setDataSource(context, Uri.parse(source))
                source.startsWith("file://") -> setDataSource(context, Uri.parse(source))
                source.startsWith("/") -> setDataSource(source)
                else -> setDataSource(materializeAudioFile(context, source, fileName).absolutePath)
            }
            prepare()
        }
    }

    fun startAudioPlayback(sourceId: String, name: String, source: String) {
        val currentPlayer = mediaPlayer
        if (audioPlaybackState.sourceId == sourceId && currentPlayer != null) {
            if (currentPlayer.isPlaying) {
                currentPlayer.pause()
                updateAudioPlaybackSnapshot(currentPlayer)
                currentOnEvent.value("已暂停录音: $name")
            } else {
                currentPlayer.start()
                updateAudioPlaybackSnapshot(currentPlayer)
                currentOnEvent.value("继续播放录音: $name")
            }
            return
        }

        stopAudioPlayback(resetState = false)
        val player = prepareMediaPlayer(source = source, fileName = name)
        player.setOnCompletionListener { completedPlayer ->
            val completedState = audioPlaybackState
            audioPlaybackState = completedState.copy(
                isPlaying = false,
                currentPositionMillis = completedState.durationMillis
            )
            currentOnEvent.value("播放完成: $name")
            if (mediaPlayer === completedPlayer) {
                releaseAudioPlayer()
            } else {
                completedPlayer.release()
            }
            resetAudioPlaybackState()
        }
        mediaPlayer = player
        playingRecordingId = sourceId
        audioPlaybackState = AudioPlaybackState(
            sourceId = sourceId,
            sourceName = name,
            isPlaying = false,
            currentPositionMillis = 0L,
            durationMillis = player.duration.toLong().coerceAtLeast(0L)
        )
        player.start()
        updateAudioPlaybackSnapshot(player)
        currentOnEvent.value("正在播放录音: $name")
    }

    DisposableEffect(Unit) {
        onDispose {
            restoreVoiceInputRoute()
            stopAndroidRecording(mediaRecorder)
            mediaRecorder = null
            stopAudioPlayback()
        }
    }

    LaunchedEffect(mediaPlayer, audioPlaybackState.sourceId) {
        while (true) {
            val player = mediaPlayer ?: break
            if (audioPlaybackState.sourceId == null) break
            updateAudioPlaybackSnapshot(player)
            delay(250L)
        }
    }

    fun loadRecentImages() {
        try {
            val resolver = context.contentResolver
            val projection = arrayOf(
                MediaStore.Images.Media._ID
            )
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            val uris = buildList {
                resolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    var count = 0
                    while (cursor.moveToNext() && count < 24) {
                        val id = cursor.getLong(idCol)
                        val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                        add(uri.toString())
                        count++
                    }
                }
            }
            recentImages.clear()
            recentImages.addAll(uris)
            currentOnEvent.value(
                if (uris.isEmpty()) {
                    "已授权相册读取，但未读取到近期照片。"
                } else {
                    "已加载 ${uris.size} 张近期照片。"
                }
            )
        } catch (t: Throwable) {
            currentOnEvent.value("读取近期照片失败: ${t.message.orEmpty()}")
        }
    }

    val requestReadImagesPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            currentOnEvent.value("相册权限已授权，正在加载近期照片。")
            loadRecentImages()
        } else {
            // Android 14+ 部分授权: 用户可能选择了"仅选择部分照片"
            val partialGranted = if (Build.VERSION.SDK_INT >= 34) {
                val activity = context.findActivity()
                activity != null && androidx.core.content.ContextCompat.checkSelfPermission(
                    activity,
                    "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else false

            if (partialGranted) {
                currentOnEvent.value("相册部分授权，正在加载可访问的照片。")
                loadRecentImages()
            } else {
                currentOnEvent.value("相册权限被拒绝，无法展示近期照片。")
                recentImages.clear()
            }
        }
    }

    val takePicturePreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap == null) {
            currentOnEvent.value("相机已取消或未返回图片。")
        } else {
            val previewUri = saveBitmapPreviewToCache(context, bitmap)
            if (previewUri.isNullOrBlank()) {
                currentOnEvent.value("拍照成功，但保存图片失败，无法展示预览。")
            } else {
                currentOnEvent.value("拍照成功，已保存图片 ${bitmap.width} x ${bitmap.height}。")
                pickedImages.add(0, previewUri)
            }
        }
    }

    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            currentOnEvent.value("相机权限已授权，正在打开相机。")
            takePicturePreviewLauncher.launch(null)
        } else {
            currentOnEvent.value("相机权限被拒绝，无法打开相机。")
        }
    }

    val requestAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val recognizer = ensureSpeechRecognizer()
            if (recognizer != null) {
                try {
                    configureVoiceInputRoute()
                    val recordingResult = startAndroidRecording(context)
                    mediaRecorder = recordingResult.recorder
                    currentRecordingFile = recordingResult.file
                    if (recordingResult.recorder == null) {
                        restoreVoiceInputRoute()
                        currentOnEvent.value(recordingResult.message)
                    } else {
                        latestVoiceText = ""
                        latestVoiceRecording = null
                        voiceResultCallback = null
                        voiceSessionAction = AndroidVoiceSessionAction.Active
                        isRecording = true
                        recognizer.startListening(buildSpeechRecognizerIntent())
                        currentOnEvent.value("语音输入已开始。")
                    }
                } catch (error: Throwable) {
                    discardCurrentRecording()
                    resetVoiceSession()
                    currentOnEvent.value("开始语音输入失败: ${error.message.orEmpty()}")
                }
            }
        } else {
            currentOnEvent.value("麦克风权限被拒绝，无法开始语音输入。")
        }
    }

    val pickVisualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) {
            currentOnEvent.value("未选择图片。")
        } else {
            currentOnEvent.value("已选择图片: $uri")
            pickedImages.add(0, uri.toString())
        }
    }

    val pickMultipleVisualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) {
            currentOnEvent.value("未选择图片。")
        } else {
            currentOnEvent.value("已选择 ${uris.size} 张图片。")
            uris.asReversed().forEach { pickedImages.add(0, it.toString()) }
        }
    }

    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            currentOnEvent.value("未选择图片。")
        } else {
            currentOnEvent.value("已选择图片: $uri")
            pickedImages.add(0, uri.toString())
        }
    }

    val getMultipleContentsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) {
            currentOnEvent.value("未选择图片。")
        } else {
            currentOnEvent.value("已选择 ${uris.size} 张图片。")
            uris.asReversed().forEach { pickedImages.add(0, it.toString()) }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            currentOnEvent.value("未选择文件。")
        } else {
            currentOnEvent.value("已选择文件: $uri")
            pickedFiles.add(
                0,
                PickedFile(
                    uri = uri.toString(),
                    displayName = resolveDisplayName(context, uri)
                )
            )
        }
    }

    val openAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            currentOnEvent.value("未选择音频文件。")
        } else {
            currentOnEvent.value("已选择音频文件: $uri")
            pickedFiles.add(
                0,
                PickedFile(
                    uri = uri.toString(),
                    displayName = resolveDisplayName(context, uri)
                )
            )
        }
    }

    return remember(
        isRecording,
        recordings.size,
        pickedImages.size,
        pickedFiles.size,
        recentImages.size,
        playingRecordingId,
        audioPlaybackState
    ) {
        AndroidPlatformMediaAccessController(
            isRecording = isRecording,
            recordings = recordings,
            pickedImages = pickedImages,
            pickedFiles = pickedFiles,
            recentImages = recentImages,
            playingRecordingId = playingRecordingId,
            audioPlaybackState = audioPlaybackState,
            onOpenCamera = {
                val activity = context.findActivity()
                if (activity == null) {
                    currentOnEvent.value("未找到 Activity，上下文异常，无法打开相机。")
                } else {
                    val cameraGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    android.util.Log.d("MediaAccess", "[相机] CAMERA权限=$cameraGranted, activity=$activity")
                    currentOnEvent.value("[相机] CAMERA权限=$cameraGranted")
                    if (cameraGranted) {
                        takePicturePreviewLauncher.launch(null)
                    } else {
                        currentOnEvent.value("正在申请相机权限。")
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            },
            onOpenGallery = {
                currentOnEvent.value("正在打开系统图片选择器。")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pickMultipleVisualMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                } else {
                    getMultipleContentsLauncher.launch("image/*")
                }
            },
            onOpenAudioPicker = {
                currentOnEvent.value("正在打开系统音频选择器。")
                openAudioLauncher.launch(arrayOf("audio/*"))
            },
            onOpenFilePicker = {
                currentOnEvent.value("正在打开系统文件选择器。")
                openDocumentLauncher.launch(arrayOf("*/*"))
            },
            onOpenFilePreview = { pickedFile ->
                openPickedFilePreview(context, pickedFile) { message -> currentOnEvent.value(message) }
            },
            onStartVoiceInput = startVoiceInput@{
                val activity = context.findActivity()
                if (activity == null) {
                    currentOnEvent.value("未找到 Activity，上下文异常，无法语音输入。")
                    return@startVoiceInput
                }

                if (isRecording) return@startVoiceInput

                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.RECORD_AUDIO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (granted) {
                    val recognizer = ensureSpeechRecognizer()
                    if (recognizer != null) {
                        try {
                            configureVoiceInputRoute()
                            val recordingResult = startAndroidRecording(context)
                            mediaRecorder = recordingResult.recorder
                            currentRecordingFile = recordingResult.file
                            if (recordingResult.recorder == null) {
                                restoreVoiceInputRoute()
                                currentOnEvent.value(recordingResult.message)
                            } else {
                                latestVoiceText = ""
                                latestVoiceRecording = null
                                voiceResultCallback = null
                                voiceSessionAction = AndroidVoiceSessionAction.Active
                                isRecording = true
                                recognizer.startListening(buildSpeechRecognizerIntent())
                                currentOnEvent.value("语音输入已开始。")
                            }
                        } catch (error: Throwable) {
                            discardCurrentRecording()
                            resetVoiceSession()
                            currentOnEvent.value("开始语音输入失败: ${error.message.orEmpty()}")
                        }
                    }
                } else {
                    currentOnEvent.value("正在申请麦克风权限。")
                    requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onFinishVoiceInput = finishVoiceInput@{ onResult ->
                if (!isRecording) return@finishVoiceInput
                isRecording = false
                stopCurrentRecordingFile()
                latestVoiceRecording = null
                voiceResultCallback = onResult
                voiceSessionAction = AndroidVoiceSessionAction.Finishing
                try {
                    speechRecognizer?.stopListening()
                    currentOnEvent.value("正在进行语音转文字。")
                } catch (error: Throwable) {
                    currentOnEvent.value("语音转文字失败: ${error.message.orEmpty()}")
                    dispatchVoiceResult(latestVoiceText.trim())
                }
            },
            onCancelVoiceInput = cancelVoiceInput@{
                if (!isRecording) return@cancelVoiceInput
                isRecording = false
                voiceSessionAction = AndroidVoiceSessionAction.Cancelling
                latestVoiceText = ""
                voiceResultCallback = null
                discardCurrentRecording()
                try {
                    speechRecognizer?.cancel()
                } catch (_: Throwable) {
                }
                resetVoiceSession()
                currentOnEvent.value("已取消语音输入")
            },
            onToggleRecordingPlayback = toggleRecordingPlayback@{ recording ->
                try {
                    startAudioPlayback(
                        sourceId = recording.id,
                        name = recording.name,
                        source = recording.path
                    )
                } catch (error: Exception) {
                    stopAudioPlayback()
                    currentOnEvent.value("播放录音失败: ${error.message.orEmpty()}")
                }
            },
            onToggleAudioPlayback = { sourceId, name, source ->
                try {
                    startAudioPlayback(
                        sourceId = sourceId,
                        name = name,
                        source = source
                    )
                } catch (error: Throwable) {
                    stopAudioPlayback()
                    currentOnEvent.value("播放录音失败: ${error.message.orEmpty()}")
                }
            },
            onSeekAudioPlaybackTo = { positionMillis ->
                val player = mediaPlayer
                if (player != null) {
                    val targetPosition = positionMillis.coerceIn(0L, player.duration.toLong().coerceAtLeast(0L))
                    player.seekTo(targetPosition.toInt())
                    updateAudioPlaybackSnapshot(player)
                }
            },
            onSkipAudioPlaybackBy = { deltaMillis ->
                val player = mediaPlayer
                if (player != null) {
                    val targetPosition = (player.currentPosition.toLong() + deltaMillis)
                        .coerceIn(0L, player.duration.toLong().coerceAtLeast(0L))
                    player.seekTo(targetPosition.toInt())
                    updateAudioPlaybackSnapshot(player)
                }
            },
            onStopAudioPlayback = {
                stopAudioPlayback()
            },
            onReadUriToBytes = readUri@{ uri ->
                withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
                    }.getOrNull()
                }
            },
            onRefreshRecentImages = refresh@{
                val activity = context.findActivity()
                if (activity == null) {
                    currentOnEvent.value("未找到 Activity，上下文异常，无法读取近期照片。")
                    return@refresh
                }

                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    @Suppress("DEPRECATION")
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                val fullGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                    activity,
                    permission
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                // Android 14+ 部分授权: READ_MEDIA_VISUAL_USER_SELECTED
                val partialGranted = if (Build.VERSION.SDK_INT >= 34) {
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        activity,
                        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else false

                android.util.Log.d("MediaAccess", "[相册] SDK=${Build.VERSION.SDK_INT}, permission=$permission, fullGranted=$fullGranted, partialGranted=$partialGranted")
                currentOnEvent.value("[相册] SDK=${Build.VERSION.SDK_INT}, 全量=$fullGranted, 部分=$partialGranted")

                if (fullGranted || partialGranted) {
                    loadRecentImages()
                } else {
                    currentOnEvent.value("正在申请相册权限，用于展示近期照片。")
                    requestReadImagesPermissionLauncher.launch(permission)
                }
            }
        )
    }
}

private enum class AndroidVoiceSessionAction {
    Idle,
    Active,
    Finishing,
    Cancelling,
}

private fun buildSpeechRecognizerIntent(): Intent {
    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.SIMPLIFIED_CHINESE.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.SIMPLIFIED_CHINESE.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
    }
}

private fun extractSpeechText(bundle: Bundle?): String {
    if (bundle == null) return ""
    return bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        ?.firstOrNull()
        .orEmpty()
}

private fun resolveDisplayName(context: Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                } else {
                    null
                }
            }
    }.getOrNull().orEmpty().ifBlank { uri.lastPathSegment.orEmpty().ifBlank { uri.toString() } }
}

@OptIn(ExperimentalResourceApi::class)
private fun materializeAudioFile(
    context: Context,
    source: String,
    fileName: String,
): File {
    val targetFile = File(context.cacheDir, "nas_audio_${source.hashCode()}_${sanitizeFileName(fileName)}")
    if (targetFile.exists()) {
        return targetFile
    }
    val bytes = runBlocking { Res.readBytes(source) }
    targetFile.outputStream().use { output ->
        output.write(bytes)
    }
    return targetFile
}

private fun openPickedFilePreview(
    context: Context,
    file: PickedFile,
    onEvent: (String) -> Unit,
) {
    val rawUri = file.uri.trim()
    if (rawUri.isBlank()) {
        onEvent("文件路径无效，无法预览。")
        return
    }

    val targetUri = runCatching {
        val parsed = Uri.parse(rawUri)
        if (parsed.scheme.isNullOrBlank()) {
            val targetFile = File(rawUri)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetFile
            )
        } else {
            parsed
        }
    }.getOrElse {
        onEvent("无法打开文件预览: ${it.message.orEmpty()}")
        return
    }

    val mimeType = context.contentResolver.getType(targetUri)
        ?: android.webkit.MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.displayName.substringAfterLast('.', "").lowercase())
        ?: "*/*"

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(targetUri, mimeType)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(intent, file.displayName)
    val canOpen = intent.resolveActivity(context.packageManager) != null
    if (canOpen) {
        runCatching { context.startActivity(chooser) }
            .onFailure { onEvent("打开文件失败: ${it.message.orEmpty()}") }
    } else {
        onEvent("当前设备没有可用于预览该文件的应用。")
    }
}

private data class AndroidRecordingResult(
    val recorder: MediaRecorder?,
    val file: File?,
    val message: String
)

private fun startAndroidRecording(context: Context): AndroidRecordingResult {
    return try {
        val file = File(
            context.filesDir,
            "audio_${timestamp()}.m4a"
        )
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        AndroidRecordingResult(
            recorder = recorder,
            file = file,
            message = "录音已开始。"
        )
    } catch (error: Exception) {
        AndroidRecordingResult(
            recorder = null,
            file = null,
            message = "开始录音失败: ${error.message.orEmpty()}"
        )
    }
}

private fun stopAndroidRecording(recorder: MediaRecorder?) {
    if (recorder == null) return

    try {
        recorder.stop()
    } catch (_: Exception) {
    } finally {
        recorder.reset()
        recorder.release()
    }
}

private fun timestamp(): String {
    return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
}

private fun sanitizeFileName(fileName: String): String {
    return fileName.map { char ->
        when {
            char.isLetterOrDigit() || char == '.' || char == '_' || char == '-' -> char
            else -> '_'
        }
    }.joinToString(separator = "")
}

private fun saveBitmapPreviewToCache(context: Context, bitmap: Bitmap): String? {
    val file = File(
        context.cacheDir,
        "camera_preview_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
    )

    return try {
        val success = file.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        }
        if (success) Uri.fromFile(file).toString() else null
    } catch (_: Throwable) {
        null
    }
}

private fun Context.findActivity(): ComponentActivity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    return current as? ComponentActivity
}
