package com.cephalon.lucyApp.sdk

import lucy.im.sdk.blob.BlobPutResult

internal expect suspend fun platformUploadBlob(data: ByteArray, entryName: String): BlobPutResult
