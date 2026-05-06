package com.cephalon.lucyApp.sdk

import lucy.im.sdk.blob.BlobPutResult
import lucy.im.sdk.blob.BlobTransfer

internal expect suspend fun platformUploadBlob(data: ByteArray, entryName: String): BlobPutResult

internal expect fun createPlatformBlobTransfer(): BlobTransfer

/**
 * Configure blob relay-related runtime env/config.
 *
 * Must be called before the first `BlobTransfer()` construction / put / fetch in the process.
 * This is best-effort: failures should not crash the app.
 */
internal expect fun configurePlatformBlobRelay()
