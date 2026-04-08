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
    val messages: List<ChatItem> = emptyList(),
    val lastActiveAt: Long = 0L,
)

internal fun ConversationItem.displayTitle(): String = title.ifBlank { "新对话" }

internal fun ConversationItem.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    return displayTitle().contains(query, ignoreCase = true) ||
        messages.any { it.searchableText().contains(query, ignoreCase = true) }
}

private fun ChatItem.searchableText(): String {
    return when (this) {
        is ChatItem.Assistant -> text
        is ChatItem.User -> text
        is ChatItem.UserAttachments -> buildString {
            if (!text.isNullOrBlank()) {
                append(text)
                append(' ')
            }
            attachments.forEach { attachment ->
                append(uriDisplayName(attachment.uri))
                append(' ')
            }
        }
        is ChatItem.System -> text
        is ChatItem.RecordingItem -> "$name $path"
    }
}

internal fun uriDisplayName(uri: String): String {
    val trimmed = uri.trim()
    if (trimmed.isBlank()) return ""
    val noQuery = trimmed.substringBefore('?')
    return noQuery.substringAfterLast('/').ifBlank { noQuery }
}
