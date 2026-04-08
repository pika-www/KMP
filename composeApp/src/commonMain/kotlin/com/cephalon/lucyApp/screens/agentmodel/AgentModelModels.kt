package com.cephalon.lucyApp.screens.agentmodel

internal enum class DraftAttachmentType {
    Image,
    File,
}

internal data class DraftAttachment(
    val type: DraftAttachmentType,
    val uri: String,
)

internal data class ImagePreviewState(
    val images: List<String>,
    val selectedIndex: Int,
)

internal sealed class ChatItem {
    data class Assistant(val text: String) : ChatItem()
    data class User(val text: String) : ChatItem()
    data class UserAttachments(val text: String?, val attachments: List<DraftAttachment>) : ChatItem()
    data class System(val text: String) : ChatItem()
    data class RecordingItem(val id: String, val name: String, val path: String) : ChatItem()
}

internal data class ConversationItem(
    val id: String,
    val title: String,
)

internal fun uriDisplayName(uri: String): String {
    val trimmed = uri.trim()
    if (trimmed.isBlank()) return ""
    val noQuery = trimmed.substringBefore('?')
    return noQuery.substringAfterLast('/').ifBlank { noQuery }
}
