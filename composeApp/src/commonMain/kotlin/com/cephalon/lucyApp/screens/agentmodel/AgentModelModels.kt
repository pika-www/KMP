package com.cephalon.lucyApp.screens.agentmodel

import com.cephalon.lucyApp.media.AudioRecording
import com.cephalon.lucyApp.media.PickedFile

internal enum class DraftAttachmentType {
    Image,
    File,
    Audio,
}

internal data class DraftAttachment(
    val type: DraftAttachmentType,
    val uri: String,
    val displayName: String? = null,
)

internal data class ImagePreviewState(
    val images: List<String>,
    val selectedIndex: Int,
)

internal sealed class ChatItem {
    data class Assistant(val text: String, val messageId: String? = null) : ChatItem()
    data class User(val text: String) : ChatItem()
    data class UserAttachments(val text: String?, val attachments: List<DraftAttachment>) : ChatItem()
    data class System(val text: String) : ChatItem()
    data class RecordingItem(val id: String, val name: String, val path: String) : ChatItem()
    data object SkillSuggestions : ChatItem()
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
                append(attachment.displayName())
                append(' ')
            }
        }
        is ChatItem.System -> text
        is ChatItem.RecordingItem -> "$name $path"
        is ChatItem.SkillSuggestions -> "探索脑花的能力"
    }
}

internal fun uriDisplayName(uri: String): String {
    val trimmed = uri.trim()
    if (trimmed.isBlank()) return ""
    val noQuery = trimmed.substringBefore('?')
    return noQuery.substringAfterLast('/').ifBlank { noQuery }
}

internal fun DraftAttachment.displayName(): String {
    return displayName?.trim().takeUnless { it.isNullOrBlank() } ?: uriDisplayName(uri)
}

internal fun DraftAttachment.fileExtensionLabel(): String {
    val name = displayName()
    val extension = name.substringAfterLast('.', "").trim()
    return extension.takeIf { it.isNotBlank() }?.uppercase() ?: "FILE"
}

internal fun DraftAttachment.asPickedFile(): PickedFile {
    return PickedFile(
        uri = uri,
        displayName = displayName()
    )
}

internal fun DraftAttachment.asAudioRecording(): AudioRecording {
    return AudioRecording(
        id = uri,
        name = displayName(),
        path = uri
    )
}
