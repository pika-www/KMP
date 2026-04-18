package com.cephalon.lucyApp.media

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHPhotoLibrary
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
actual suspend fun platformSaveFile(bytes: ByteArray, fileName: String, mimeType: String): String =
    withContext(Dispatchers.Main) {
        val isImage = mimeType.startsWith("image/")

        if (isImage) {
            saveImageToPhotoLibrary(bytes, fileName)
        } else {
            saveFileToDocuments(bytes, fileName)
        }
    }

@OptIn(ExperimentalForeignApi::class)
private suspend fun saveImageToPhotoLibrary(bytes: ByteArray, fileName: String): String =
    suspendCancellableCoroutine { continuation ->
        val nsData = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }

        // 先写到临时文件，再通过 PHPhotoLibrary 导入
        val tempDir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String ?: ""
        val tempPath = "$tempDir/tmp_$fileName"
        nsData.writeToFile(tempPath, atomically = true)

        val fileUrl = platform.Foundation.NSURL.fileURLWithPath(tempPath)

        PHPhotoLibrary.sharedPhotoLibrary().performChanges({
            PHAssetChangeRequest.creationRequestForAssetFromImageAtFileURL(fileUrl)
        }) { success, error ->
            // 清理临时文件
            NSFileManager.defaultManager.removeItemAtPath(tempPath, null)

            if (success) {
                continuation.resume("photos://saved/$fileName")
            } else {
                continuation.resumeWithException(
                    IllegalStateException("保存到相册失败: ${error?.localizedDescription ?: "unknown"}")
                )
            }
        }
    }

@OptIn(ExperimentalForeignApi::class)
private fun saveFileToDocuments(bytes: ByteArray, fileName: String): String {
    val documentsDir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String ?: throw IllegalStateException("无法获取 Documents 目录")

    val subDir = "$documentsDir/NaoHua"
    val fm = NSFileManager.defaultManager
    if (!fm.fileExistsAtPath(subDir)) {
        fm.createDirectoryAtPath(subDir, withIntermediateDirectories = true, attributes = null, error = null)
    }

    val filePath = "$subDir/$fileName"
    val nsData = bytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
    }

    val wrote = nsData.writeToFile(filePath, atomically = true)
    if (!wrote) {
        throw IllegalStateException("写入文件失败: $filePath")
    }
    return filePath
}
