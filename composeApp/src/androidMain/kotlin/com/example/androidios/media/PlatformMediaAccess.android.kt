package com.example.androidios.media

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

private class AndroidPlatformMediaAccessController(
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
    val context = LocalContext.current
    val currentOnEvent = rememberUpdatedState(onEvent)

    val takePicturePreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap == null) {
            currentOnEvent.value("相机已取消或未返回图片。")
        } else {
            currentOnEvent.value("拍照成功，已返回预览图 ${bitmap.width} x ${bitmap.height}。")
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

    val pickVisualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) {
            currentOnEvent.value("未选择图片。")
        } else {
            currentOnEvent.value("已选择图片: $uri")
        }
    }

    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            currentOnEvent.value("未选择图片。")
        } else {
            currentOnEvent.value("已选择图片: $uri")
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            currentOnEvent.value("未选择文件。")
        } else {
            currentOnEvent.value("已选择文件: $uri")
        }
    }

    return remember(context) {
        AndroidPlatformMediaAccessController(
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
                    pickVisualMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                } else {
                    getContentLauncher.launch("image/*")
                }
            },
            onOpenFilePicker = {
                currentOnEvent.value("正在打开系统文件选择器。")
                openDocumentLauncher.launch(arrayOf("*/*"))
            }
        )
    }
}

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
