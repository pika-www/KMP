package com.cephalon.lucyApp.sdk

import lucy.im.sdk.blob.BlobPutResult
import lucy.im.sdk.blob.BlobTransfer

private val blobTransfer = BlobTransfer()

internal actual suspend fun platformUploadBlob(data: ByteArray, entryName: String): BlobPutResult {
    return blobTransfer.put(data, entryName)
}
