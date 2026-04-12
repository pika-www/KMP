package com.cephalon.lucyApp.screens.agentmodel

import com.cephalon.lucyApp.logging.appLogD
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "ChatHistoryCache"
private const val KEY_PREFIX = "chat_history_"

private val cacheJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

// ── 可序列化的数据模型 ──

@Serializable
private data class SerializableChatItem(
    val type: String,
    val text: String? = null,
    val attachmentUris: List<String>? = null,
    val attachmentNames: List<String>? = null,
    val recordingId: String? = null,
    val recordingName: String? = null,
    val recordingPath: String? = null,
)

@Serializable
private data class SerializableConversation(
    val id: String,
    val title: String,
    val messages: List<SerializableChatItem>,
    val lastActiveAt: Long,
)

@Serializable
private data class SerializableChatHistory(
    val conversations: List<SerializableConversation>,
    val selectedConversationId: String?,
)

// ── ChatItem ↔ Serializable 转换 ──

private fun ChatItem.toSerializable(): SerializableChatItem? = when (this) {
    is ChatItem.Assistant -> SerializableChatItem(type = "assistant", text = text)
    is ChatItem.User -> SerializableChatItem(type = "user", text = text)
    is ChatItem.UserAttachments -> SerializableChatItem(
        type = "user_attachments",
        text = text,
        attachmentUris = attachments.map { it.uri },
        attachmentNames = attachments.map { it.displayName ?: "" },
    )
    is ChatItem.System -> SerializableChatItem(type = "system", text = text)
    is ChatItem.RecordingItem -> SerializableChatItem(
        type = "recording",
        recordingId = id,
        recordingName = name,
        recordingPath = path,
    )
    is ChatItem.SkillSuggestions -> SerializableChatItem(type = "skill_suggestions")
}

private fun SerializableChatItem.toChatItem(): ChatItem? = when (type) {
    "assistant" -> ChatItem.Assistant(text = text ?: "")
    "user" -> ChatItem.User(text = text ?: "")
    "user_attachments" -> {
        val uris = attachmentUris.orEmpty()
        val names = attachmentNames.orEmpty()
        val attachments = uris.mapIndexed { i, uri ->
            DraftAttachment(
                type = DraftAttachmentType.Image,
                uri = uri,
                displayName = names.getOrNull(i)?.ifBlank { null },
            )
        }
        ChatItem.UserAttachments(text = text, attachments = attachments)
    }
    "system" -> ChatItem.System(text = text ?: "")
    "recording" -> ChatItem.RecordingItem(
        id = recordingId ?: "",
        name = recordingName ?: "",
        path = recordingPath ?: "",
    )
    "skill_suggestions" -> ChatItem.SkillSuggestions
    else -> null
}

// ── 公开 API ──

internal class ChatHistoryCache(private val settings: Settings) {

    fun save(
        cdi: String,
        conversations: List<ConversationItem>,
        selectedConversationId: String?,
    ) {
        val history = SerializableChatHistory(
            conversations = conversations.map { conv ->
                SerializableConversation(
                    id = conv.id,
                    title = conv.title,
                    messages = conv.messages.mapNotNull { it.toSerializable() },
                    lastActiveAt = conv.lastActiveAt,
                )
            },
            selectedConversationId = selectedConversationId,
        )
        val jsonStr = cacheJson.encodeToString(history)
        settings.putString(KEY_PREFIX + cdi, jsonStr)
        appLogD(TAG, "已保存 cdi=$cdi 的聊天记录: ${conversations.size}个对话")
    }

    fun load(cdi: String): Pair<List<ConversationItem>, String?>? {
        val jsonStr = settings.getStringOrNull(KEY_PREFIX + cdi) ?: return null
        return try {
            val history = cacheJson.decodeFromString<SerializableChatHistory>(jsonStr)
            val conversations = history.conversations.map { conv ->
                ConversationItem(
                    id = conv.id,
                    title = conv.title,
                    messages = conv.messages.mapNotNull { it.toChatItem() },
                    lastActiveAt = conv.lastActiveAt,
                )
            }
            appLogD(TAG, "已加载 cdi=$cdi 的聊天记录: ${conversations.size}个对话")
            conversations to history.selectedConversationId
        } catch (e: Exception) {
            appLogD(TAG, "加载 cdi=$cdi 聊天记录失败: ${e.message}")
            null
        }
    }

    fun clear(cdi: String) {
        settings.remove(KEY_PREFIX + cdi)
        appLogD(TAG, "已清除 cdi=$cdi 的聊天记录缓存")
    }
}
