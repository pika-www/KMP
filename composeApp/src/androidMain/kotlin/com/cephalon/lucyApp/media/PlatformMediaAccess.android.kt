package com.cephalon.lucyApp.media

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private class AndroidPlatformMediaAccessController(
    override val isRecording: Boolean,
    override val recordings: List<AudioRecording>,
    override val pickedImages: List<String>,
    override val pickedFiles: List<String>,
    override val recentImages: List<String>,
    private val onOpenCamera: () -> Unit,
    private val onOpenGallery: () -> Unit,
    private val onOpenFilePicker: () -> Unit,
    private val onStartRecording: () -> Unit,
    private val onFinishRecording: () -> Unit,
    private val onCancelRecording: () -> Unit,
    private val onPlayRecording: (AudioRecording) -> Unit,
    private val onRefreshRecentImages: () -> Unit
) : PlatformMediaAccessController {
    override fun openCamera() = onOpenCamera()

    override fun openGallery() = onOpenGallery()

    override fun openFilePicker() = onOpenFilePicker()

    override fun startRecording() = onStartRecording()

    override fun finishRecording() = onFinishRecording()

    override fun cancelRecording() = onCancelRecording()

    override fun playRecording(recording: AudioRecording) = onPlayRecording(recording)

    override fun refreshRecentImages() = onRefreshRecentImages()
}

@Composable
actual fun rememberPlatformMediaAccessController(
    onEvent: (String) -> Unit
): PlatformMediaAccessController {
    val context = LocalContext.current
    val currentOnEvent = rememberUpdatedState(onEvent)
    val recordings = remember { mutableStateListOf<AudioRecording>() }
    val pickedImages = remember { mutableStateListOf<String>() }
    val pickedFiles = remember { mutableStateListOf<String>() }
    val recentImages = remember { mutableStateListOf<String>() }
    var isRecording by remember { mutableStateOf(false) }
    var currentRecordingFile by remember { mutableStateOf<File?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

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
            currentOnEvent.value("相册权限被拒绝，无法展示近期照片。")
            recentImages.clear()
        }
    }

    val takePicturePreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap == null) {
            currentOnEvent.value("相机已取消或未返回图片。")
        } else {
            currentOnEvent.value("拍照成功，已返回预览图 ${bitmap.width} x ${bitmap.height}。")
            pickedImages.add(0, "android-bitmap-preview://${UUID.randomUUID()}")
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
            currentOnEvent.value("麦克风权限已授权，开始录音。")
            val result = startAndroidRecording(context)
            mediaRecorder = result.recorder
            currentRecordingFile = result.file
            isRecording = result.recorder != null
            if (result.recorder == null) {
                currentOnEvent.value(result.message)
            } else {
                currentOnEvent.value("录音中: ${result.file?.absolutePath.orEmpty()}")
            }
        } else {
            currentOnEvent.value("麦克风权限被拒绝，无法开始录音。")
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
            pickedFiles.add(0, uri.toString())
        }
    }

    return remember(context, isRecording, recordings.size, pickedImages.size, pickedFiles.size, recentImages.size) {
        AndroidPlatformMediaAccessController(
            isRecording = isRecording,
            recordings = recordings,
            pickedImages = pickedImages,
            pickedFiles = pickedFiles,
            recentImages = recentImages,
            onOpenCamera = {
                val activity = context.findActivity()
                if (activity == null) {
                    currentOnEvent.value("未找到 Activity，上下文异常，无法打开相机。")
                } else {
                    val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (granted) {
                        currentOnEvent.value("相机权限已存在，正在打开相机。")
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
            onOpenFilePicker = {
                currentOnEvent.value("正在打开系统文件选择器。")
                openDocumentLauncher.launch(arrayOf("*/*"))
            },
            onStartRecording = {
                val activity = context.findActivity()
                if (activity == null) {
                    currentOnEvent.value("未找到 Activity，上下文异常，无法录音。")
                    return@AndroidPlatformMediaAccessController
                }

                if (isRecording) return@AndroidPlatformMediaAccessController

                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.RECORD_AUDIO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (granted) {
                    val result = startAndroidRecording(context)
                    mediaRecorder = result.recorder
                    currentRecordingFile = result.file
                    isRecording = result.recorder != null
                    currentOnEvent.value(result.message)
                } else {
                    currentOnEvent.value("正在申请麦克风权限。")
                    requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onFinishRecording = {
                if (!isRecording) return@AndroidPlatformMediaAccessController

                val file = currentRecordingFile
                val recorder = mediaRecorder
                stopAndroidRecording(recorder)
                mediaRecorder = null
                currentRecordingFile = null
                isRecording = false

                if (file != null && file.exists()) {
                    val recording = AudioRecording(
                        id = UUID.randomUUID().toString(),
                        name = file.name,
                        path = file.absolutePath
                    )
                    recordings.add(0, recording)
                    currentOnEvent.value("录音已保存: ${file.absolutePath}")
                } else {
                    currentOnEvent.value("录音已停止，但未找到输出文件。")
                }
            },
            onCancelRecording = {
                if (!isRecording) return@AndroidPlatformMediaAccessController

                val file = currentRecordingFile
                val recorder = mediaRecorder
                stopAndroidRecording(recorder)
                mediaRecorder = null
                currentRecordingFile = null
                isRecording = false

                if (file != null) {
                    try {
                        file.delete()
                    } catch (_: Exception) {
                    }
                }
                currentOnEvent.value("已取消录音")
            },
            onPlayRecording = { recording ->
                try {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(recording.path)
                        prepare()
                        start()
                        setOnCompletionListener {
                            currentOnEvent.value("播放完成: ${recording.name}")
                            it.release()
                            mediaPlayer = null
                        }
                    }
                    currentOnEvent.value("正在播放录音: ${recording.name}")
                } catch (error: Exception) {
                    currentOnEvent.value("播放录音失败: ${error.message.orEmpty()}")
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

                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                    activity,
                    permission
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (granted) {
                    loadRecentImages()
                } else {
                    currentOnEvent.value("正在申请相册权限，用于展示近期照片。")
                    requestReadImagesPermissionLauncher.launch(permission)
                }
            }
        )
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
            setAudioSource(MediaRecorder.AudioSource.MIC)
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

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
