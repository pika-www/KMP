package com.cephalon.lucyApp.sdk

import kotlinx.cinterop.ExperimentalForeignApi
import lucy.im.sdk.blob.BlobPutResult
import lucy.im.sdk.blob.BlobTransfer
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.posix.setenv

@OptIn(ExperimentalForeignApi::class)
private val blobTransfer: BlobTransfer by lazy {
    val caches = (NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String) ?: ""
    val storeDir = "$caches/lucy_blob"
    setenv("lucy.blob.store_dir", storeDir, 1)
    BlobTransfer()
}

internal actual suspend fun platformUploadBlob(data: ByteArray, entryName: String): BlobPutResult {
    return blobTransfer.put(data, entryName)
}
