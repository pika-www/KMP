package com.cephalon.lucyApp.sdk

import com.cephalon.lucyApp.screens.agentmodel.AndroidAppContextHolder
import lucy.im.sdk.blob.BlobPutResult
import lucy.im.sdk.blob.BlobTransfer
import java.io.File

private val blobTransfer by lazy {
    val dir = File(AndroidAppContextHolder.appContext.filesDir, "lucy_blob")
    BlobTransfer.initStoreDir(dir.absolutePath)
    BlobTransfer()
}

internal actual suspend fun platformUploadBlob(data: ByteArray, entryName: String): BlobPutResult {
    return blobTransfer.put(data, entryName)
}

internal actual fun createPlatformBlobTransfer(): BlobTransfer {
    val dir = File(AndroidAppContextHolder.appContext.filesDir, "lucy_blob")
    BlobTransfer.initStoreDir(dir.absolutePath)
    return BlobTransfer()
}
