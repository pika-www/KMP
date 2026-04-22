package com.cephalon.lucyApp.sdk

internal actual fun parseBlobRefDebugInfo(blobRef: String): BlobRefDebugInfo? {
    // iOS BlobTransfer is bridged via Kotlin/Native; we don't currently expose a ticket parser symbol here.
    return null
}

