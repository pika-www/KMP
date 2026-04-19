package com.cephalon.lucyApp.screens.nas

import androidx.compose.runtime.mutableStateListOf

internal enum class NasSendFileType { Image, Audio, Document }

internal data class NasSendItem(
    val fileId: Long,
    val fileName: String,
    val fileType: NasSendFileType,
    val thumbnailBlobRef: String,
    val sizeKB: Int,
    val format: String,
)

internal object NasSendToChatStore {
    val pendingItems = mutableStateListOf<NasSendItem>()

    fun submit(items: List<NasSendItem>) {
        pendingItems.addAll(items)
    }

    fun consume(): List<NasSendItem> {
        val snapshot = pendingItems.toList()
        pendingItems.clear()
        return snapshot
    }
}
