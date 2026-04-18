package com.cephalon.lucyApp.media

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.cephalon.lucyApp.screens.agentmodel.AndroidAppContextHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun platformSaveFile(bytes: ByteArray, fileName: String, mimeType: String): String =
    withContext(Dispatchers.IO) {
        val context = AndroidAppContextHolder.appContext
        val isImage = mimeType.startsWith("image/")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用 MediaStore
            val collection = if (isImage) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (isImage) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/脑花")
                } else {
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/脑花")
                }
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = context.contentResolver.insert(collection, values)
                ?: throw IllegalStateException("无法创建文件: $fileName")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(bytes)
            } ?: throw IllegalStateException("无法写入文件: $fileName")

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)

            uri.toString()
        } else {
            // Android 9 及以下
            val dir = if (isImage) {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            } else {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }
            val subDir = java.io.File(dir, "脑花")
            if (!subDir.exists()) subDir.mkdirs()

            val file = java.io.File(subDir, fileName)
            file.writeBytes(bytes)

            // 通知媒体库扫描
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf(mimeType),
                null,
            )

            file.absolutePath
        }
    }
