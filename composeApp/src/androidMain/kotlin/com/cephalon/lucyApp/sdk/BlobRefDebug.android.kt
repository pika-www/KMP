package com.cephalon.lucyApp.sdk

internal actual fun parseBlobRefDebugInfo(blobRef: String): BlobRefDebugInfo? {
    if (blobRef.isBlank()) return null
    return try {
        val arr = BlobTicketNative.parseOrNull(blobRef) ?: return null
        val nodeId = arr.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return null
        val relayUrl = arr.getOrNull(1)?.takeIf { it.isNotBlank() }
        val directCsv = arr.getOrNull(2).orEmpty()
        val direct = directCsv.split(',').mapNotNull { it.trim().takeIf(String::isNotEmpty) }
        BlobRefDebugInfo(nodeId = nodeId, relayUrl = relayUrl, directAddresses = direct)
    } catch (_: Throwable) {
        null
    }
}

internal object BlobTicketNative {
    @Volatile private var available: Boolean? = null

    init {
        // BlobTransfer already loads this in most cases; keep this best-effort to avoid ordering issues.
        runCatching { System.loadLibrary("lucy_blob_core") }
    }

    fun setRelayFallbackTimeoutSecs(timeoutSecs: Int): Boolean {
        val ok = available
        if (ok == false) return false
        return try {
            nativeSetRelayFallbackTimeoutSecs(timeoutSecs)
            available = true
            true
        } catch (_: UnsatisfiedLinkError) {
            available = false
            false
        } catch (_: Throwable) {
            // Be defensive: config failures should never crash the app.
            false
        }
    }

    fun parseOrNull(blobRef: String): Array<String>? {
        val ok = available
        if (ok == false) return null
        return try {
            nativeParse(blobRef).also { available = true }
        } catch (_: UnsatisfiedLinkError) {
            available = false
            null
        }
    }

    private external fun nativeParse(blobRef: String): Array<String>?
    private external fun nativeSetRelayFallbackTimeoutSecs(timeoutSecs: Int)
}

