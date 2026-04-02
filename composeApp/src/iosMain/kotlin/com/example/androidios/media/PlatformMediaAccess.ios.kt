package com.example.androidios.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.example.androidios.IOSViewControllerHolder
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHPhotoLibrary
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
    private val onOpenCamera: () -> Unit,
    private val onOpenGallery: () -> Unit,
    private val onOpenFilePicker: () -> Unit
) : PlatformMediaAccessController {
    override fun openCamera() = onOpenCamera()

    override fun openGallery() = onOpenGallery()

    override fun openFilePicker() = onOpenFilePicker()
}

@Composable
actual fun rememberPlatformMediaAccessController(
    onEvent: (String) -> Unit
): PlatformMediaAccessController {
    val currentOnEvent = rememberUpdatedState(onEvent)

    return remember {
        IOSPlatformMediaAccessController(
            onOpenCamera = {
                val presenter = currentPresenter()
                if (presenter == null) {
                    currentOnEvent.value("未找到当前页面，无法打开相机。")
                } else {
                    when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
                        AVAuthorizationStatusAuthorized -> {
                            currentOnEvent.value("相机权限已授权，正在打开相机。")
                            presenter.presentCamera(currentOnEvent.value)
                        }

                        AVAuthorizationStatusNotDetermined -> {
                            currentOnEvent.value("正在申请相机权限。")
                            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                                dispatch_async(dispatch_get_main_queue()) {
                                    if (granted) {
                                        currentOnEvent.value("相机权限已授权，正在打开相机。")
                                        presenter.presentCamera(currentOnEvent.value)
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
                    when (PHPhotoLibrary.authorizationStatus()) {
                        PHAuthorizationStatusAuthorized,
                        PHAuthorizationStatusLimited -> {
                            currentOnEvent.value("相册权限已授权，正在打开图片选择器。")
                            presenter.presentPhotoLibrary(currentOnEvent.value)
                        }

                        PHAuthorizationStatusNotDetermined -> {
                            currentOnEvent.value("正在申请相册权限。")
                            PHPhotoLibrary.requestAuthorization { status ->
                                dispatch_async(dispatch_get_main_queue()) {
                                    if (status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited) {
                                        currentOnEvent.value("相册权限已授权，正在打开图片选择器。")
                                        presenter.presentPhotoLibrary(currentOnEvent.value)
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
                    presenter.presentDocumentPicker(currentOnEvent.value)
                }
            }
        )
    }
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
private fun UIViewController.presentCamera(onEvent: (String) -> Unit) {
    if (!UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
        onEvent("当前设备不支持相机。")
        return
    }

    val picker = UIImagePickerController().apply {
        sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        cameraDevice = UIImagePickerControllerCameraDevice.UIImagePickerControllerCameraDeviceRear
        allowsEditing = false
    }

    val delegate = ImagePickerDelegate(onEvent)
    picker.delegate = delegate
    IOSPickerDelegateStore.retain(delegate)
    presentViewController(picker, true, null)
}

@OptIn(ExperimentalForeignApi::class)
private fun UIViewController.presentPhotoLibrary(onEvent: (String) -> Unit) {
    if (!UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)) {
        onEvent("当前设备不支持图片选择。")
        return
    }

    val picker = UIImagePickerController().apply {
        sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
        allowsEditing = false
    }

    val delegate = ImagePickerDelegate(onEvent)
    picker.delegate = delegate
    IOSPickerDelegateStore.retain(delegate)
    presentViewController(picker, true, null)
}

@OptIn(ExperimentalForeignApi::class)
private fun UIViewController.presentDocumentPicker(onEvent: (String) -> Unit) {
    val picker = UIDocumentPickerViewController(
        documentTypes = listOf("public.item"),
        inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
    )
    val delegate = DocumentPickerDelegate(onEvent)
    picker.delegate = delegate
    IOSPickerDelegateStore.retain(delegate)
    presentViewController(picker, true, null)
}

private class ImagePickerDelegate(
    private val onEvent: (String) -> Unit
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
            imageUrl != null -> onEvent("已选择图片: ${imageUrl.absoluteString ?: "未知路径"}")
            image != null -> onEvent("已获取图片对象，尺寸 ")
            else -> onEvent("已完成图片选择，但未读取到结果。")
        }
    }
}

private class DocumentPickerDelegate(
    private val onEvent: (String) -> Unit
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
        onEvent("已选择文件: ${didPickDocumentAtURL.absoluteString ?: "未知路径"}")
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {
        controller.dismissViewControllerAnimated(true, null)
        val first = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        if (first != null) {
            onEvent("已选择文件: ${first.absoluteString ?: "未知路径"}")
        } else {
            onEvent("已完成文件选择，但未读取到结果。")
        }
    }
}
