package com.cephalon.lucyApp.sdk

import lucy.im.sdk.blob.BlobPutResult
import lucy.im.sdk.blob.BlobTransfer

internal expect suspend fun platformUploadBlob(data: ByteArray, entryName: String): BlobPutResult

internal expect fun createPlatformBlobTransfer(): BlobTransfer
