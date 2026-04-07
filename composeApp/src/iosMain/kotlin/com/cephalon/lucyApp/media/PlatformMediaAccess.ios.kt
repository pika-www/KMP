package com.cephalon.lucyApp.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.cephalon.lucyApp.IOSViewControllerHolder
import androidx.compose.runtime.setValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.setActive
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSSortDescriptor
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.timeIntervalSince1970
import platform.Photos.PHAsset
import platform.Photos.PHAccessLevelReadWrite
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHPhotoLibrary
import platform.Photos.PHFetchOptions
import platform.Photos.PHFetchResult
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImage
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerCameraDevice
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerEditedImage
import platform.UIKit.UIImagePickerControllerImageURL
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private object IOSPickerDelegateStore {
    private val delegates = mutableListOf<Any>()

    fun retain(delegate: Any) {
        delegates += delegate
    }
}

private class IOSPlatformMediaAccessController(
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

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberPlatformMediaAccessController(
    onEvent: (String) -> Unit
): PlatformMediaAccessController {
    val currentOnEvent = rememberUpdatedState(onEvent)
    val recordings = remember { mutableStateListOf<AudioRecording>() }
    val pickedImages = remember { mutableStateListOf<String>() }
    val pickedFiles = remember { mutableStateListOf<String>() }
    val recentImages = remember { mutableStateListOf<String>() }
    var isRecording by remember { mutableStateOf(false) }
    var audioRecorder by remember { mutableStateOf<AVAudioRecorder?>(null) }
    var audioPlayer by remember { mutableStateOf<AVAudioPlayer?>(null) }
    var currentRecordingUrl by remember { mutableStateOf<NSURL?>(null) }

    fun loadRecentImages() {
        try {
            val options = PHFetchOptions().apply {
                sortDescriptors = listOf<NSSortDescriptor>(
                    NSSortDescriptor.sortDescriptorWithKey("creationDate", ascending = false)
                )
                fetchLimit = 24u
            }
            val result: PHFetchResult = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, options)
            val uris = buildList {
                val count = result.count.toInt()
                for (i in 0 until count) {
                    val asset = result.objectAtIndex(i.toULong()) as? PHAsset ?: continue
                    val id = asset.localIdentifier
                    add("ios-phasset://$id")
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

    return remember(isRecording, recordings.size, pickedImages.size, pickedFiles.size, recentImages.size) {
        IOSPlatformMediaAccessController(
            isRecording = isRecording,
            recordings = recordings,
            pickedImages = pickedImages,
            pickedFiles = pickedFiles,
            recentImages = recentImages,
            onOpenCamera = {
                val presenter = currentPresenter()
                if (presenter == null) {
                    currentOnEvent.value("未找到当前页面，无法打开相机。")
                } else {
                    when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
                        AVAuthorizationStatusAuthorized -> {
                            currentOnEvent.value("相机权限已授权，正在打开相机。")
                            presenter.presentCamera(
                                onEvent = currentOnEvent.value,
                                onPickedImage = { url -> pickedImages.add(0, url) }
                            )
                        }

                        AVAuthorizationStatusNotDetermined -> {
                            currentOnEvent.value("正在申请相机权限。")
                            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                                dispatch_async(dispatch_get_main_queue()) {
                                    if (granted) {
                                        currentOnEvent.value("相机权限已授权，正在打开相机。")
                                        presenter.presentCamera(
                                            onEvent = currentOnEvent.value,
                                            onPickedImage = { url -> pickedImages.add(0, url) }
                                        )
                                    } else {
                                        currentOnEvent.value("相机权限被拒绝，无法打开相机。")
                                    }
                                }
                            }
                        }

                        AVAuthorizationStatusDenied -> {
                            currentOnEvent.value("相机权限被拒绝，请到系统设置中开启后重试。")
                        }

                        else -> {
                            currentOnEvent.value("当前设备无法使用相机权限。")
                        }
                    }
                }
            },
            onOpenGallery = {
                val presenter = currentPresenter()
                if (presenter == null) {
                    currentOnEvent.value("未找到当前页面，无法打开图片选择器。")
                } else {
                    when (photoLibraryAuthorizationStatus()) {
                        PHAuthorizationStatusAuthorized,
                        PHAuthorizationStatusLimited -> {
                            currentOnEvent.value("相册权限已授权，正在打开图片选择器。")
                            presenter.presentPhotoLibrary(
                                onEvent = currentOnEvent.value,
                                onPickedImage = { url -> pickedImages.add(0, url) }
                            )
                        }

                        PHAuthorizationStatusNotDetermined -> {
                            currentOnEvent.value("正在申请相册权限。")
                            requestPhotoLibraryAuthorization { status ->
                                dispatch_async(dispatch_get_main_queue()) {
                                    if (status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited) {
                                        currentOnEvent.value("相册权限已授权，正在打开图片选择器。")
                                        presenter.presentPhotoLibrary(
                                            onEvent = currentOnEvent.value,
                                            onPickedImage = { url -> pickedImages.add(0, url) }
                                        )
                                    } else {
                                        currentOnEvent.value("相册权限被拒绝，无法打开图片选择器。")
                                    }
                                }
                            }
                        }

                        else -> {
                            currentOnEvent.value("相册权限被拒绝，请到系统设置中开启后重试。")
                        }
                    }
                }
            },
            onOpenFilePicker = {
                val presenter = currentPresenter()
                if (presenter == null) {
                    currentOnEvent.value("未找到当前页面，无法打开文件选择器。")
                } else {
                    currentOnEvent.value("正在打开系统文件选择器。")
                    presenter.presentDocumentPicker(
                        onEvent = currentOnEvent.value,
                        onPickedFile = { url -> pickedFiles.add(0, url) }
                    )
                }
            },
            onStartRecording = {
                if (isRecording) return@IOSPlatformMediaAccessController

                val session = AVAudioSession.sharedInstance()
                session.requestRecordPermission { granted ->
                    dispatch_async(dispatch_get_main_queue()) {
                        if (!granted) {
                            currentOnEvent.value("麦克风权限被拒绝，无法开始录音。")
                            return@dispatch_async
                        }

                        val result = startIosRecording(session)
                        audioRecorder = result.recorder
                        currentRecordingUrl = result.url
                        isRecording = result.recorder != null
                        currentOnEvent.value(result.message)
                    }
                }
            },
            onFinishRecording = {
                if (!isRecording) return@IOSPlatformMediaAccessController

                val recorder = audioRecorder
                val url = currentRecordingUrl
                recorder?.stop()
                audioRecorder = null
                currentRecordingUrl = null
                isRecording = false

                if (url != null) {
                    recordings.add(
                        0,
                        AudioRecording(
                            id = NSDate().timeIntervalSince1970.toString(),
                            name = url.lastPathComponent ?: "recording.m4a",
                            path = url.path ?: url.absoluteString ?: ""
                        )
                    )
                    currentOnEvent.value("录音已保存: ${url.path ?: url.absoluteString ?: ""}")
                } else {
                    currentOnEvent.value("录音已停止，但未找到输出文件。")
                }
            },
            onCancelRecording = {
                if (!isRecording) return@IOSPlatformMediaAccessController

                val recorder = audioRecorder
                val url = currentRecordingUrl
                recorder?.stop()
                audioRecorder = null
                currentRecordingUrl = null
                isRecording = false

                if (url != null) {
                    try {
                        NSFileManager.defaultManager.removeItemAtURL(url, null)
                    } catch (_: Throwable) {
                    }
                }

                currentOnEvent.value("已取消录音")
            },
            onPlayRecording = { recording ->
                try {
                    val url = NSURL.fileURLWithPath(recording.path)
                    audioPlayer?.stop()
                    audioPlayer = AVAudioPlayer(
                        contentsOfURL = url,
                        error = null
                    )
                    audioPlayer?.prepareToPlay()
                    audioPlayer?.play()
                    currentOnEvent.value("正在播放录音: ${recording.name}")
                } catch (error: Throwable) {
                    currentOnEvent.value("播放录音失败: ${error.message.orEmpty()}")
                }
            },
            onRefreshRecentImages = {
                when (photoLibraryAuthorizationStatus()) {
                    PHAuthorizationStatusAuthorized,
                    PHAuthorizationStatusLimited -> {
                        loadRecentImages()
                    }

                    PHAuthorizationStatusNotDetermined -> {
                        currentOnEvent.value("正在申请相册权限，用于展示近期照片。")
                        requestPhotoLibraryAuthorization { status ->
                            dispatch_async(dispatch_get_main_queue()) {
                                if (status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited) {
                                    loadRecentImages()
                                } else {
                                    currentOnEvent.value("相册权限被拒绝，无法展示近期照片。")
                                    recentImages.clear()
                                }
                            }
                        }
                    }

                    else -> {
                        currentOnEvent.value("相册权限被拒绝，无法展示近期照片。")
                        recentImages.clear()
                    }
                }
            }
        )
    }
}

private fun photoLibraryAuthorizationStatus(): Long {
    return PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite)
}

private fun requestPhotoLibraryAuthorization(onResult: (Long) -> Unit) {
    PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelReadWrite, handler = onResult)
}

private data class IOSRecordingResult(
    val recorder: AVAudioRecorder?,
    val url: NSURL?,
    val message: String
)

@OptIn(ExperimentalForeignApi::class)
private fun startIosRecording(session: AVAudioSession): IOSRecordingResult {
    return try {
        session.setCategory(
            category = AVAudioSessionCategoryPlayAndRecord,
            mode = AVAudioSessionModeDefault,
            options = AVAudioSessionCategoryOptionDefaultToSpeaker or AVAudioSessionCategoryOptionMixWithOthers,
            error = null
        )
        session.setActive(true, error = null)

        val url = createRecordingUrl()
        val settings = mapOf<Any?, Any>(
            "AVFormatIDKey" to NSNumber(1633772320),
            "AVSampleRateKey" to NSNumber(44100.0),
            "AVNumberOfChannelsKey" to NSNumber(1),
            "AVEncoderAudioQualityKey" to NSNumber(2)
        )
        val recorder = AVAudioRecorder(
            uRL = url,
            settings = settings,
            error = null
        )
        recorder.prepareToRecord()
        recorder.record()

        IOSRecordingResult(
            recorder = recorder,
            url = url,
            message = "录音已开始。"
        )
    } catch (error: Throwable) {
        IOSRecordingResult(
            recorder = null,
            url = null,
            message = "开始录音失败: ${error.message.orEmpty()}"
        )
    }
}

private fun createRecordingUrl(): NSURL {
    val directory = NSFileManager.defaultManager.URLsForDirectory(
        directory = NSDocumentDirectory,
        inDomains = NSUserDomainMask
    ).firstOrNull() as? NSURL
        ?: error("无法获取文稿目录。")
    val fileName = "audio_${NSDate().timeIntervalSince1970.toLong()}.m4a"
    return directory.URLByAppendingPathComponent(fileName)!!
}

private fun currentPresenter(): UIViewController? {
    var controller = IOSViewControllerHolder.rootViewController
        ?: UIApplication.sharedApplication.keyWindow?.rootViewController

    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }

    return controller
}

@OptIn(ExperimentalForeignApi::class)
private fun UIViewController.presentCamera(
    onEvent: (String) -> Unit,
    onPickedImage: (String) -> Unit,
) {
    if (!UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
        onEvent("当前设备不支持相机。")
        return
    }

    val picker = UIImagePickerController().apply {
        sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        cameraDevice = UIImagePickerControllerCameraDevice.UIImagePickerControllerCameraDeviceRear
        allowsEditing = false
    }

    val delegate = ImagePickerDelegate(onEvent = onEvent, onPickedImage = onPickedImage)
    picker.delegate = delegate
    IOSPickerDelegateStore.retain(delegate)
    presentViewController(picker, true, null)
}

@OptIn(ExperimentalForeignApi::class)
private fun UIViewController.presentPhotoLibrary(
    onEvent: (String) -> Unit,
    onPickedImage: (String) -> Unit,
) {
    val configuration = PHPickerConfiguration().apply {
        selectionLimit = 0
        filter = PHPickerFilter.imagesFilter()
    }

    val picker = PHPickerViewController(configuration)
    val delegate = PhotoPickerDelegate(onEvent = onEvent, onPickedImage = onPickedImage)
    picker.delegate = delegate
    IOSPickerDelegateStore.retain(delegate)
    presentViewController(picker, true, null)
}

@OptIn(ExperimentalForeignApi::class)
private fun UIViewController.presentDocumentPicker(
    onEvent: (String) -> Unit,
    onPickedFile: (String) -> Unit,
) {
    val picker = UIDocumentPickerViewController(
        documentTypes = listOf("public.item"),
        inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
    )
    val delegate = DocumentPickerDelegate(onEvent, onPickedFile)
    picker.delegate = delegate
    IOSPickerDelegateStore.retain(delegate)
    presentViewController(picker, true, null)
}

private class ImagePickerDelegate(
    private val onEvent: (String) -> Unit,
    private val onPickedImage: (String) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
        onEvent("已取消选择图片。")
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        picker.dismissViewControllerAnimated(true, null)

        val imageUrl = didFinishPickingMediaWithInfo[UIImagePickerControllerImageURL] as? NSURL
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
            ?: didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage] as? UIImage

        when {
            imageUrl != null -> {
                val value = imageUrl.absoluteString ?: ""
                onEvent("已选择图片: ${value.ifBlank { "未知路径" }}")
                if (value.isNotBlank()) {
                    onPickedImage(value)
                }
            }
            image != null -> onEvent("已获取图片对象，尺寸 ")
            else -> onEvent("已完成图片选择，但未读取到结果。")
        }
    }
}

private class PhotoPickerDelegate(
    private val onEvent: (String) -> Unit,
    private val onPickedImage: (String) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>
    ) {
        picker.dismissViewControllerAnimated(true, null)

        val results = didFinishPicking.mapNotNull { it as? PHPickerResult }
        if (results.isEmpty()) {
            onEvent("已完成图片选择，但未读取到结果。")
            return
        }

        var handledCount = 0
        var successCount = 0

        fun finishOne(success: Boolean) {
            handledCount += 1
            if (success) {
                successCount += 1
            }
            if (handledCount == results.size) {
                if (successCount > 0) {
                    onEvent("已选择 ${successCount} 张图片。")
                } else {
                    onEvent("已完成图片选择，但未读取到结果。")
                }
            }
        }

        results.forEach { result ->
            val assetId = result.assetIdentifier
            if (!assetId.isNullOrBlank()) {
                onPickedImage("ios-phasset://$assetId")
                finishOne(success = true)
            } else {
                result.itemProvider.loadFileRepresentationForTypeIdentifier("public.image") { url, _ ->
                    dispatch_async(dispatch_get_main_queue()) {
                        val copiedUrl = url?.let { copyPickedImageToTemporaryDirectory(it) }
                        val value = copiedUrl?.absoluteString ?: ""
                        if (value.isNotBlank()) {
                            onPickedImage(value)
                            finishOne(success = true)
                        } else {
                            finishOne(success = false)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun copyPickedImageToTemporaryDirectory(sourceUrl: NSURL): NSURL? {
    val sourcePath = sourceUrl.path ?: return null
    val fileExtension = sourcePath.substringAfterLast('.', "").ifBlank { "jpg" }
    val targetDirectory = NSTemporaryDirectory()
    if (targetDirectory.isBlank()) return null

    val targetUrl = NSURL.fileURLWithPath(targetDirectory)
        ?.URLByAppendingPathComponent("picked_${NSDate().timeIntervalSince1970}_${sourcePath.hashCode()}.$fileExtension")
        ?: return null

    return try {
        NSFileManager.defaultManager.removeItemAtURL(targetUrl, null)
        NSFileManager.defaultManager.copyItemAtURL(sourceUrl, targetUrl, null)
        targetUrl
    } catch (_: Throwable) {
        null
    }
}

private class DocumentPickerDelegate(
    private val onEvent: (String) -> Unit,
    private val onPickedFile: (String) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        controller.dismissViewControllerAnimated(true, null)
        onEvent("已取消选择文件。")
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentAtURL: NSURL
    ) {
        controller.dismissViewControllerAnimated(true, null)
        val value = didPickDocumentAtURL.absoluteString ?: ""
        onEvent("已选择文件: ${value.ifBlank { "未知路径" }}")
        if (value.isNotBlank()) {
            onPickedFile(value)
        }
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {
        controller.dismissViewControllerAnimated(true, null)
        val first = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        if (first != null) {
            val value = first.absoluteString ?: ""
            onEvent("已选择文件: ${value.ifBlank { "未知路径" }}")
            if (value.isNotBlank()) {
                onPickedFile(value)
            }
        } else {
            onEvent("已完成文件选择，但未读取到结果。")
        }
    }
}
