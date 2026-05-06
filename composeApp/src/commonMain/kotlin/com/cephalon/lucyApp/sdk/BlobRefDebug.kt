package com.cephalon.lucyApp.sdk

internal data class BlobRefDebugInfo(
    val nodeId: String,
    val relayUrl: String?,
    val directAddresses: List<String>,
)

internal expect fun parseBlobRefDebugInfo(blobRef: String): BlobRefDebugInfo?

