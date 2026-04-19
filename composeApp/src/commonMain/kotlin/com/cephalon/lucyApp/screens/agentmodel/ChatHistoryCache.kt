package com.cephalon.lucyApp.screens.agentmodel

import com.cephalon.lucyApp.logging.appLogD
import com.cephalon.lucyApp.sdk.MediaAttachment
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "ChatHistoryCache"
private const val KEY_PREFIX = "chat_history_"
private const val GUEST_USER_ID = "guest"

private val cacheJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

// ── 可序列化的数据模型 ──

@Serializable
private data class SerializableChatItem(
    val type: String,
    val text: String? = null,
    val messageId: String? = null,
    val attachmentUris: List<String>? = null,
    val attachmentNames: List<String>? = null,
    val recordingId: String? = null,
    val recordingName: String? = null,
    val recordingPath: String? = null,
    val mediaBlobRefs: List<String>? = null,
    val mediaContentTypes: List<String?>? = null,
    val mediaFileNames: List<String?>? = null,
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
    is ChatItem.Assistant -> SerializableChatItem(
        type = "assistant",
        text = text,
        messageId = messageId,
        mediaBlobRefs = attachments.map { it.blobRef }.ifEmpty { null },
        mediaContentTypes = attachments.map { it.contentType }.ifEmpty { null },
        mediaFileNames = attachments.map { it.fileName }.ifEmpty { null },
    )
    is ChatItem.User -> SerializableChatItem(type = "user", text = text, messageId = messageId)
    is ChatItem.UserAttachments -> SerializableChatItem(
        type = "user_attachments",
        text = text,
        messageId = messageId,
        attachmentUris = attachments.map { it.uri },
        attachmentNames = attachments.map { it.displayName ?: "" },
    )
    is ChatItem.System -> SerializableChatItem(type = "system", text = text, messageId = messageId)
    is ChatItem.RecordingItem -> SerializableChatItem(
        type = "recording",
        messageId = messageId,
        recordingId = id,
        recordingName = name,
        recordingPath = path,
    )
    is ChatItem.Error -> SerializableChatItem(type = "error", text = text, messageId = messageId)
    is ChatItem.SkillSuggestions -> SerializableChatItem(type = "skill_suggestions")
}

private fun SerializableChatItem.toChatItem(): ChatItem? = when (type) {
    "assistant" -> {
        val mediaAttachments = mediaBlobRefs?.mapIndexed { i, ref ->
            MediaAttachment(
                blobRef = ref,
                contentType = mediaContentTypes?.getOrNull(i),
                fileName = mediaFileNames?.getOrNull(i),
            )
        } ?: emptyList()
        ChatItem.Assistant(text = text ?: "", messageId = messageId, attachments = mediaAttachments)
    }
    "user" -> ChatItem.User(text = text ?: "", messageId = messageId)
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
        ChatItem.UserAttachments(text = text, attachments = attachments, messageId = messageId)
    }
    "system" -> ChatItem.System(text = text ?: "", messageId = messageId)
    "recording" -> ChatItem.RecordingItem(
        id = recordingId ?: "",
        name = recordingName ?: "",
        path = recordingPath ?: "",
        messageId = messageId,
    )
    "error" -> ChatItem.Error(text = text ?: "", messageId = messageId)
    "skill_suggestions" -> ChatItem.SkillSuggestions
    else -> null
}

/**
 * 聊天记录的存储 key 规则：`chat_history_{userId}_{cdi}`。
 * - `userId` 为空（未登录/匿名）时回退到常量 [GUEST_USER_ID]，避免不同登录账号的对话被混存。
 * - `cdi` 为空会直接抛出非法参数：调用方必须在已经解析到设备 CDI 后再调用保存/加载。
 *
 * 这样同一台手机上"用户 A + 设备 X"、"用户 A + 设备 Y"、"用户 B + 设备 X" 的对话记录
 * 都是各自独立的一份，切换用户或者切换设备不会互相污染。
 */
private fun keyOf(userId: String?, cdi: String): String {
    require(cdi.isNotBlank()) { "cdi 不能为空，无法构建聊天记录存储 key" }
    val normalizedUser = userId?.trim()?.takeIf { it.isNotBlank() } ?: GUEST_USER_ID
    return "$KEY_PREFIX${normalizedUser}_$cdi"
}

// ── 公开 API ──

internal class ChatHistoryCache(private val settings: Settings) {

    fun save(
        userId: String?,
        cdi: String,
        conversations: List<ConversationItem>,
        selectedConversationId: String?,
    ) {
        if (cdi.isBlank()) {
            appLogD(TAG, "save 被跳过：cdi 为空，userId=$userId")
            return
        }
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
        settings.putString(keyOf(userId, cdi), jsonStr)
        appLogD(TAG, "已保存 userId=$userId cdi=$cdi 的聊天记录: ${conversations.size}个对话")
    }

    fun load(
        userId: String?,
        cdi: String,
    ): Pair<List<ConversationItem>, String?>? {
        if (cdi.isBlank()) {
            appLogD(TAG, "load 被跳过：cdi 为空，userId=$userId")
            return null
        }
        val jsonStr = settings.getStringOrNull(keyOf(userId, cdi)) ?: return null
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
            appLogD(TAG, "已加载 userId=$userId cdi=$cdi 的聊天记录: ${conversations.size}个对话")
            conversations to history.selectedConversationId
        } catch (e: Exception) {
            appLogD(TAG, "加载 userId=$userId cdi=$cdi 聊天记录失败: ${e.message}")
            null
        }
    }

    fun clear(userId: String?, cdi: String) {
        if (cdi.isBlank()) return
        settings.remove(keyOf(userId, cdi))
        appLogD(TAG, "已清除 userId=$userId cdi=$cdi 的聊天记录缓存")
    }

    /**
     * 跨 (userId, cdi) 直接对磁盘存档里某一条对话做 read-modify-write。
     * 典型用法：用户发消息后切了设备，此时流式回复/发送失败的写入目标不在当前内存里，
     * 需要绕过 in-memory conversations，直接把更新落到原设备的存档里。
     *
     * - 存档不存在：返回 false，不创建新文件（外部调用是"更新"不是"新建"）。
     * - 找不到对应 conversationId：返回 false。
     * - 序列化/反序列化出错：返回 false。
     * - 写入成功：返回 true。
     */
    fun mutateConversation(
        userId: String?,
        cdi: String,
        conversationId: String,
        transform: (ConversationItem) -> ConversationItem,
    ): Boolean {
        if (cdi.isBlank() || conversationId.isBlank()) {
            appLogD(TAG, "mutateConversation 跳过：cdi 或 convId 为空 userId=$userId cdi=$cdi convId=$conversationId")
            return false
        }
        val key = keyOf(userId, cdi)
        val jsonStr = settings.getStringOrNull(key)
        if (jsonStr.isNullOrBlank()) {
            appLogD(TAG, "mutateConversation 跳过：存档不存在 key=$key")
            return false
        }
        val history = runCatching {
            cacheJson.decodeFromString<SerializableChatHistory>(jsonStr)
        }.getOrElse {
            appLogD(TAG, "mutateConversation 反序列化失败 key=$key err=${it.message}")
            return false
        }
        val convs = history.conversations.toMutableList()
        val idx = convs.indexOfFirst { it.id == conversationId }
        if (idx < 0) {
            appLogD(TAG, "mutateConversation 跳过：找不到 conversationId=$conversationId key=$key")
            return false
        }
        val oldConv = ConversationItem(
            id = convs[idx].id,
            title = convs[idx].title,
            messages = convs[idx].messages.mapNotNull { it.toChatItem() },
            lastActiveAt = convs[idx].lastActiveAt,
        )
        val newConv = transform(oldConv)
        convs[idx] = SerializableConversation(
            id = newConv.id,
            title = newConv.title,
            messages = newConv.messages.mapNotNull { it.toSerializable() },
            lastActiveAt = newConv.lastActiveAt,
        )
        val newHistory = history.copy(conversations = convs)
        settings.putString(key, cacheJson.encodeToString(newHistory))
        appLogD(TAG, "mutateConversation 写回 key=$key convId=$conversationId msgs=${newConv.messages.size}")
        return true
    }
}
