package com.cephalon.lucyApp.screens.agentmodel

import com.cephalon.lucyApp.media.AudioRecording
import com.cephalon.lucyApp.media.PickedFile
import com.cephalon.lucyApp.sdk.MediaAttachment

internal sealed class AttachmentUploadState {
    data object Uploading : AttachmentUploadState()
    data class Success(
        val blobRef: String,
        val contentType: String,
        val size: Long,
        val fileName: String,
    ) : AttachmentUploadState()
    data class Failed(val error: String) : AttachmentUploadState()
}

internal enum class DraftAttachmentType {
    Image,
    File,
    Audio,
}

internal data class DraftAttachment(
    val type: DraftAttachmentType,
    val uri: String,
    val displayName: String? = null,
    val nasFileId: Long? = null,
)

internal data class ImagePreviewState(
    val images: List<String>,
    val selectedIndex: Int,
)

internal sealed class ChatItem {
    /**
     * 聊天消息在当前会话里的业务 id（由 SDK 的 generateMessageId19() 产出的 19 位字符串，
     * 或重放历史时从缓存读回的值）。
     *
     * 语义约定：
     * - 用户侧消息（[User] / [UserAttachments]）：发送到 NPC 成功后回填 SDK 返回的 messageId；
     *   发送失败或本地还没派发时为 null。
     * - 助手消息（[Assistant]）：采用所回复的那条用户消息的 messageId（作为 source_message_id），
     *   流式期间通过它在本地找到占位符并 upsert。
     * - [System] / [Error] / [RecordingItem]：本地注入的消息，messageId 一般为 null，
     *   持久化时保留字段以便跨端同步 / 将来与服务端对齐。
     */
    abstract val messageId: String?

    data class Assistant(
        val text: String,
        override val messageId: String? = null,
        val attachments: List<MediaAttachment> = emptyList(),
        val timestamp: Long? = null,
    ) : ChatItem()
    data class User(
        val text: String,
        override val messageId: String? = null,
    ) : ChatItem()
    data class UserAttachments(
        val text: String?,
        val attachments: List<DraftAttachment>,
        override val messageId: String? = null,
    ) : ChatItem()
    data class System(
        val text: String,
        override val messageId: String? = null,
    ) : ChatItem()
    data class RecordingItem(
        val id: String,
        val name: String,
        val path: String,
        override val messageId: String? = null,
    ) : ChatItem()
    data class Error(
        val text: String,
        override val messageId: String? = null,
    ) : ChatItem()
    data object SkillSuggestions : ChatItem() {
        override val messageId: String? = null
    }
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
        is ChatItem.Error -> text
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
