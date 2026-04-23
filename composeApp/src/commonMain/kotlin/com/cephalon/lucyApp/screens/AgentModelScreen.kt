package com.cephalon.lucyApp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.ic_close_circle
import androidios.composeapp.generated.resources.ic_skill_image
import androidios.composeapp.generated.resources.ic_skill_voice
import androidios.composeapp.generated.resources.ic_skill_document
import androidios.composeapp.generated.resources.ic_skill_chat
import androidios.composeapp.generated.resources.ic_skill_knowledge
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.cephalon.lucyApp.components.DesignScaleProvider
import com.cephalon.lucyApp.components.LocalDesignScale
import com.cephalon.lucyApp.media.rememberPlatformMediaAccessController
import org.jetbrains.compose.resources.painterResource
import com.cephalon.lucyApp.time.currentTimeMillis
import com.cephalon.lucyApp.media.platformSaveCacheFile
import com.cephalon.lucyApp.media.platformSaveFile
import kotlinx.coroutines.launch
import com.cephalon.lucyApp.screens.agentmodel.ChatHistoryCache
import com.cephalon.lucyApp.screens.agentmodel.ChatItem
import com.cephalon.lucyApp.screens.agentmodel.DraftAttachment
import com.cephalon.lucyApp.screens.agentmodel.DraftAttachmentType
import com.cephalon.lucyApp.screens.agentmodel.ImagePreviewState
import com.cephalon.lucyApp.screens.agentmodel.StreamEvent
import com.cephalon.lucyApp.screens.agentmodel.AttachmentUploadState
import com.cephalon.lucyApp.sdk.MediaAttachment
import com.cephalon.lucyApp.screens.agentmodel.uriDisplayName
import com.cephalon.lucyApp.screens.agentmodel.ConversationItem
import com.cephalon.lucyApp.screens.agentmodel.displayName
import com.cephalon.lucyApp.screens.agentmodel.AgentModelAttachmentPanel
import com.cephalon.lucyApp.screens.agentmodel.AgentModelSearchScreen
import androidx.compose.ui.text.input.TextFieldValue
import com.cephalon.lucyApp.screens.agentmodel.AgentModelComposer
import com.cephalon.lucyApp.screens.agentmodel.AgentModelImagePreview
import com.cephalon.lucyApp.screens.agentmodel.AgentModelMessageList
import com.cephalon.lucyApp.screens.agentmodel.AgentModelProfileScreen
import com.cephalon.lucyApp.screens.agentmodel.AgentModelTopBar
import com.cephalon.lucyApp.screens.agentmodel.AgentModelVoiceRecordingOverlay
import com.cephalon.lucyApp.media.AudioRecording
import com.cephalon.lucyApp.screens.agentmodel.BrainPowerBalancePage
import com.cephalon.lucyApp.screens.agentmodel.RechargePackagePage
import com.cephalon.lucyApp.screens.agentmodel.asPickedFile
import com.cephalon.lucyApp.screens.nas.NasSendToChatStore
import com.cephalon.lucyApp.screens.nas.NasSendFileType
import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.sdk.NpcReplyEvent
import com.cephalon.lucyApp.sdk.SdkSessionManager
import com.cephalon.lucyApp.ws.BalanceWsManager
import com.russhwolf.settings.Settings
import org.koin.compose.koinInject
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.style.TextOverflow
import com.cephalon.lucyApp.screens.agentmodel.asAudioRecording
import com.cephalon.lucyApp.screens.nas.NasAudioDetailScreen
import com.cephalon.lucyApp.screens.nas.NasAudioItem
import com.cephalon.lucyApp.screens.nas.NasDocumentDetailScreen
import com.cephalon.lucyApp.screens.nas.NasDocumentItem
import com.cephalon.lucyApp.screens.nas.NasImageDetailScreen
import com.cephalon.lucyApp.screens.nas.NasImageItem
import kotlin.math.PI
import kotlin.math.sin

private fun mergeStreamingAssistantText(existing: String, incoming: String): String {
    if (existing.isEmpty()) return incoming
    if (incoming.isEmpty()) return existing
    if (incoming.startsWith(existing)) return incoming
    if (existing.startsWith(incoming)) return existing

    val maxOverlap = minOf(existing.length, incoming.length)
    for (overlap in maxOverlap downTo 1) {
        if (existing.endsWith(incoming.substring(0, overlap))) {
            return existing + incoming.substring(overlap)
        }
    }

    return existing + incoming
}

private fun mergeIndependentFinalText(existing: String, incoming: String): String {
    val trimmedIncoming = incoming.trim()
    if (trimmedIncoming.isBlank()) return existing
    if (existing.isBlank()) return trimmedIncoming
    if (existing.contains(trimmedIncoming)) return existing
    return existing.trimEnd() + "\n\n" + trimmedIncoming
}

private fun List<StreamEvent>.addOrUpdate(event: StreamEvent): List<StreamEvent> {
    if (event.type == "tool") {
        // 工具事件按 label 去重
        return if (any { it.type == "tool" && it.label == event.label }) {
            map { if (it.type == "tool" && it.label == event.label) event else it }
        } else this + event
    }
    // 状态事件按 type 更新已有或追加
    val idx = indexOfFirst { it.type == event.type }
    return if (idx >= 0) {
        toMutableList().apply { this[idx] = event }
    } else this + event
}

private fun List<StreamEvent>.markAllInactive(): List<StreamEvent> =
    map { if (it.isActive) it.copy(isActive = false) else it }

private fun inferContentType(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "heic", "heif" -> "image/heic"
        "bmp" -> "image/bmp"
        "svg" -> "image/svg+xml"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "aac" -> "audio/aac"
        "m4a" -> "audio/mp4"
        "ogg" -> "audio/ogg"
        "flac" -> "audio/flac"
        "pdf" -> "application/pdf"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "txt" -> "text/plain"
        "zip" -> "application/zip"
        "mp4" -> "video/mp4"
        "mov" -> "video/quicktime"
        else -> "application/octet-stream"
    }
}

private fun inferContentTypeFromBytes(bytes: ByteArray): String? {
    if (bytes.size < 4) return null
    return when {
        bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> "image/jpeg"
        bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "image/png"
        bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() && bytes[3] == 0x38.toByte() -> "image/gif"
        bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte() -> "image/bmp"
        bytes.size >= 12 && bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() &&
            bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() && bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte() -> "image/webp"
        bytes.size >= 12 && bytes[4] == 0x66.toByte() && bytes[5] == 0x74.toByte() && bytes[6] == 0x79.toByte() && bytes[7] == 0x70.toByte() -> {
            val brand = bytes.sliceArray(8..11).decodeToString()
            when {
                brand.startsWith("heic") || brand.startsWith("heix") || brand.startsWith("mif1") -> "image/heic"
                brand.startsWith("avif") -> "image/avif"
                else -> null
            }
        }
        bytes[0] == 0x25.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x44.toByte() && bytes[3] == 0x46.toByte() -> "application/pdf"
        else -> null
    }
}

private fun detectDraftAttachmentType(fileName: String): DraftAttachmentType {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp", "svg", "tiff", "ico" -> DraftAttachmentType.Image
        "mp3", "m4a", "aac", "wav", "flac", "ogg", "oga", "opus", "amr", "caf", "aiff", "aif" -> DraftAttachmentType.Audio
        else -> DraftAttachmentType.File
    }
}

private fun extensionForContentType(contentType: String): String? = when (contentType) {
    "image/jpeg" -> "jpg"
    "image/png" -> "png"
    "image/gif" -> "gif"
    "image/webp" -> "webp"
    "image/heic" -> "heic"
    "image/avif" -> "avif"
    "image/bmp" -> "bmp"
    "application/pdf" -> "pdf"
    else -> null
}

private fun isBrainBoxCapabilityQuery(text: String): Boolean {
    val normalized = text.trim()
    if (normalized.isEmpty()) return false
    if (!normalized.contains("脑花")) return false
    return normalized.contains("能力") || normalized.contains("功能")
}

@Composable
fun AgentModelScreen(
    onBack: () -> Unit,
    onNavigateToNas: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onLogout: () -> Unit = {},
    initialTargetCdi: String? = null,
) {
    val sdkSessionManager = koinInject<SdkSessionManager>()
    val chatHistoryCache = koinInject<ChatHistoryCache>()
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val assistantReplyText by sdkSessionManager.assistantReplyText.collectAsState()
    val assistantReplyStreaming by sdkSessionManager.assistantReplyStreaming.collectAsState()
    val streamingStatusText by sdkSessionManager.streamingStatusText.collectAsState()
    val onlineDeviceCdis by sdkSessionManager.onlineDeviceCdis.collectAsState()
    val selectedDeviceCdi by sdkSessionManager.selectedDeviceCdi.collectAsState()
    val authRepository = koinInject<AuthRepository>()
    val userInfo by authRepository.userInfo.collectAsState()
    // 解析优先级：用户显式选择 > 路由传入 > 当前首个在线设备
    // 避免"设备在线但未被显式选过" → 发送时误报"没有可用的设备"
    val currentCdi = selectedDeviceCdi ?: initialTargetCdi ?: onlineDeviceCdis.firstOrNull()
    // 聊天记录按 (userId, cdi) 双键持久化；未登录用户走 guest 桶，不同账号的对话不会串。
    val effectiveUserId = userInfo?.userId?.trim()?.takeIf { it.isNotBlank() }

    val logs = remember {
        mutableStateListOf(
            "点击下面的按钮测试系统能力。",
            "相机会先申请系统权限，图库和文件会通过系统选择器打开。",
            "录音会先申请麦克风权限，结束后可在下方播放。"
        )
    }

    val mediaAccessController = rememberPlatformMediaAccessController { message ->
        logs.add(0, message)
    }

    // 对话列表是本地持久化的：按 (userId, cdi) 落到 Settings；当任一 key 变化会重新 load。
    // remember 里先放一个占位的"新对话"，等第一次 load 完成后会被真实历史替换（若存在）。
    val conversations = remember {
        mutableStateListOf(
            ConversationItem(
                id = "1",
                title = "新对话",
                messages = emptyList(),
                lastActiveAt = currentTimeMillis()
            )
        )
    }
    var selectedConversationId by remember { mutableStateOf("1") }
    // didLoad 用于：第一次 load 完成之前，snapshotFlow 的自动 save 不启动，避免拿占位对话覆盖磁盘历史。
    // key(effectiveUserId, currentCdi) 在登录账号/设备切换时重置为 false，走一轮新的 load-save 循环。
    var didLoad by remember(effectiveUserId, currentCdi) { mutableStateOf(false) }

    // ── 初始加载：每次 (effectiveUserId, currentCdi) 组合变化，从磁盘拉出对应历史对话 ──
    // 没有 cdi（设备尚未解析到）时跳过，等拿到 cdi 再 load。
    // - 有历史：整体替换内存 conversations，恢复 selectedConversationId。
    // - 没历史（新设备 / 新账号 / 首次使用）：清空内存列表并放一条干净的"新对话"占位，
    //   关键点：**不能原样保留上一台设备的对话**，否则 UI 上切换设备像是"没切换"，
    //   随后 snapshotFlow 的自动保存还会把旧设备的对话污染到新设备的 key 下。
    LaunchedEffect(effectiveUserId, currentCdi) {
        val cdi = currentCdi
        if (cdi.isNullOrBlank()) {
            // 还没解析到 cdi（比如设备刚进页面还在上线），等下一次触发
            return@LaunchedEffect
        }
        val loaded = chatHistoryCache.load(effectiveUserId, cdi)
        val convs = loaded?.first.orEmpty()
        if (convs.isNotEmpty()) {
            conversations.clear()
            conversations.addAll(convs)
            val savedSelId = loaded?.second
            selectedConversationId = savedSelId?.takeIf { id -> convs.any { it.id == id } }
                ?: convs.first().id
        } else {
            // 新设备/新账号首次进入：换一套全新的占位对话，避免沿用上一组 (userId, cdi) 的内存残留。
            val freshId = currentTimeMillis().toString()
            conversations.clear()
            conversations.add(
                ConversationItem(
                    id = freshId,
                    title = "新对话",
                    messages = emptyList(),
                    lastActiveAt = currentTimeMillis(),
                )
            )
            selectedConversationId = freshId
        }
        didLoad = true
    }

    // ── 自动保存：load 完成后才启动，监听 conversations/selectedConversationId 任一变化就写回磁盘 ──
    // 用 snapshotFlow 感知 mutableStateListOf 的变化；.drop(1) 跳过第一帧（load 刚完成时的那次 emit），
    // 避免白写一次（load 下来的内容再写回去）。
    LaunchedEffect(effectiveUserId, currentCdi, didLoad) {
        if (!didLoad) return@LaunchedEffect
        val cdi = currentCdi ?: return@LaunchedEffect
        if (cdi.isBlank()) return@LaunchedEffect
        snapshotFlow { conversations.toList() to selectedConversationId }
            .drop(1)
            .collect { (convs, selId) ->
                chatHistoryCache.save(effectiveUserId, cdi, convs, selId)
            }
    }

    // 记录 SDK messageId 真正归属的 cdi。
    // 用户在 A 上发了消息后切到 B：内存 conversations 已经被 B 的历史替换，
    // 但 A 的流式回复仍会通过 LaunchedEffect(msgId) 不断回调到本地。
    // 通过 messageIdToCdi[msgId] = "A"，所有流式 upsert / remove / append 就能识别出
    // "目标设备不是当前设备"，转而把更新直接写到 A 的磁盘存档里，避免回复丢失。
    // 必须声明在使用它的 helper 函数（upsert/remove/append）之前 ——
    // Kotlin 局部声明不支持前向引用。
    val messageIdToCdi = remember { mutableStateMapOf<String, String>() }
    val messageIdToConversationId = remember { mutableStateMapOf<String, String>() }
    val messageIdToReplyHostAssistantId = remember { mutableStateMapOf<String, String>() }

    fun updateConversation(
        conversationId: String?,
        persist: Boolean = false,
        transform: (ConversationItem) -> ConversationItem,
    ) {
        val targetId = conversationId ?: return
        val index = conversations.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            conversations[index] = transform(conversations[index])
            // 下方的 snapshotFlow LaunchedEffect 会监听 conversations 变化自动保存，
            // 这里不再直接调用 cache.save，避免与 debounce 逻辑重复。
        }
    }

    fun updateSelectedConversation(transform: (ConversationItem) -> ConversationItem) {
        updateConversation(selectedConversationId, transform = transform)
    }

    // 读取"实时的 currentCdi"（直接从 StateFlow.value 取，绕过 compose 捕获），
    // 避免在 LaunchedEffect 里被旧闭包锁住的 currentCdi 做决策。
    fun liveCurrentCdi(): String? =
        sdkSessionManager.selectedDeviceCdi.value
            ?: initialTargetCdi
            ?: sdkSessionManager.onlineDeviceCdis.value.firstOrNull()

    // 按目标 cdi 路由的 update：
    // - targetCdi == null / blank / 当前设备：走内存路径，snapshotFlow 会自动 save；
    // - targetCdi 指向其它设备：跳过内存，直接 read-modify-write 那个 cdi 的磁盘存档。
    // 这样即使用户切换了设备，原设备上正在发生的流式回复/错误提示仍能落到正确的存档里。
    fun mutateConversationOnCdi(
        conversationId: String?,
        targetCdi: String?,
        transform: (ConversationItem) -> ConversationItem,
    ) {
        val convId = conversationId ?: return
        val normalizedTarget = targetCdi?.trim()?.takeIf { it.isNotEmpty() }
        val liveCdi = liveCurrentCdi()
        if (normalizedTarget == null || normalizedTarget == liveCdi) {
            updateConversation(convId, transform = transform)
        } else {
            chatHistoryCache.mutateConversation(effectiveUserId, normalizedTarget, convId, transform)
        }
    }

    fun appendMessageToSelectedConversation(message: ChatItem) {
        updateSelectedConversation { conversation ->
            val updatedMessages = conversation.messages + message
            val nextTitle = when {
                conversation.title != "新对话" && conversation.title.isNotBlank() -> conversation.title
                message is ChatItem.User && message.text.isNotBlank() -> message.text.trim()
                message is ChatItem.UserAttachments && !message.text.isNullOrBlank() -> message.text.trim()
                else -> conversation.title
            }
            conversation.copy(
                title = nextTitle,
                messages = updatedMessages,
                lastActiveAt = currentTimeMillis()
            )
        }
    }

    fun updateAssistantMessage(
        conversationId: String?,
        messageId: String,
        transform: (ChatItem.Assistant) -> ChatItem.Assistant,
    ) {
        val targetCdi = messageIdToCdi[messageId]
        mutateConversationOnCdi(conversationId, targetCdi) { conversation ->
            val msgs = conversation.messages.toMutableList()
            // 1. 优先更新仍在流式中的同 messageId assistant，避免覆盖已经完成的历史 final。
            var idx = msgs.indexOfLast {
                it is ChatItem.Assistant &&
                    it.messageId == messageId &&
                    it.isStreaming
            }
            // 2. 其次更新尚未绑定 messageId 的空占位符。
            // 2. 兜底：刚添加的占位符（messageId 尚未回填）
            if (idx < 0) {
                idx = msgs.indexOfLast { it is ChatItem.Assistant && it.messageId == null && it.text.isBlank() }
            }
            // 3. 最后才更新最后一条同 messageId assistant，用于历史恢复 / 延迟到达的非 final 事件。
            if (idx < 0) {
                idx = msgs.indexOfLast { it is ChatItem.Assistant && it.messageId == messageId }
            }
            if (idx >= 0) {
                msgs[idx] = transform(msgs[idx] as ChatItem.Assistant)
            } else {
                // 没有匹配的 assistant 消息 → 创建新的（跨会话恢复 / 延迟到达）
                msgs.add(transform(ChatItem.Assistant(text = "", messageId = messageId)))
                println("[Event] updateAssistantMessage 未找到匹配消息，创建新 assistant msgId=$messageId")
            }
            conversation.copy(messages = msgs, lastActiveAt = currentTimeMillis())
        }
    }

    fun upsertAssistantFinalMessage(
        conversationId: String?,
        sourceMessageId: String,
        event: NpcReplyEvent,
    ) {
        val targetCdi = messageIdToCdi[sourceMessageId]
        mutateConversationOnCdi(conversationId, targetCdi) { conversation ->
            val msgs = conversation.messages.toMutableList()
            val replyHostAssistantId = messageIdToReplyHostAssistantId[sourceMessageId]
            val replyHostIdx =
                replyHostAssistantId
                    ?.let { hostId ->
                        msgs.indexOfLast {
                            it is ChatItem.Assistant && it.assistantId == hostId
                        }
                    }
                    ?: -1
            val targetIdx =
                when {
                    replyHostIdx >= 0 -> replyHostIdx
                    else -> msgs.indexOfLast {
                        it is ChatItem.Assistant &&
                            it.messageId == sourceMessageId &&
                            (it.isStreaming || it.text.isBlank())
                    }.takeIf { it >= 0 }
                        ?: msgs.indexOfLast {
                            it is ChatItem.Assistant && it.messageId == sourceMessageId
                        }
                }

            if (targetIdx >= 0) {
                val current = msgs[targetIdx] as ChatItem.Assistant
                msgs[targetIdx] = current.copy(
                    text = event.text?.let { mergeIndependentFinalText(current.text, it) } ?: current.text,
                    messageId = sourceMessageId,
                    attachments = (current.attachments + event.attachments).distinctBy { it.blobRef },
                    timestamp = event.timestamp ?: current.timestamp,
                )
                messageIdToReplyHostAssistantId[sourceMessageId] = current.assistantId
            } else {
                val newMessage = ChatItem.Assistant(
                    text = event.text.orEmpty(),
                    messageId = sourceMessageId,
                    attachments = event.attachments,
                    timestamp = event.timestamp,
                    isStreaming = true,
                )
                msgs.add(newMessage)
                messageIdToReplyHostAssistantId[sourceMessageId] = newMessage.assistantId
            }

            conversation.copy(messages = msgs, lastActiveAt = currentTimeMillis())
        }
    }

    fun appendAttachmentsToLatestAssistant(
        conversationId: String?,
        attachments: List<MediaAttachment>,
        text: String? = null,
        timestamp: Long? = null,
    ) {
        if (attachments.isEmpty() && text.isNullOrBlank()) return
        mutateConversationOnCdi(conversationId, targetCdi = null) { conversation ->
            val msgs = conversation.messages.toMutableList()
            val assistantIdx = msgs.indexOfLast { it is ChatItem.Assistant }
            if (assistantIdx >= 0) {
                val assistant = msgs[assistantIdx] as ChatItem.Assistant
                val mergedAttachments = (assistant.attachments + attachments)
                    .distinctBy { it.blobRef }
                val mergedText = when {
                    text.isNullOrBlank() -> assistant.text
                    assistant.text.isBlank() -> text
                    assistant.text.contains(text) -> assistant.text
                    else -> "${assistant.text}\n\n$text"
                }
                msgs[assistantIdx] = assistant.copy(
                    text = mergedText,
                    attachments = mergedAttachments,
                    timestamp = timestamp ?: assistant.timestamp,
                )
            } else {
                msgs.add(
                    ChatItem.Assistant(
                        text = text ?: "",
                        attachments = attachments.distinctBy { it.blobRef },
                        timestamp = timestamp,
                    )
                )
            }
            conversation.copy(messages = msgs, lastActiveAt = currentTimeMillis())
        }
    }

    fun removeEmptyAssistantPlaceholder(conversationId: String?, messageId: String? = null) {
        val targetCdi = messageId?.let { messageIdToCdi[it] }
        mutateConversationOnCdi(conversationId, targetCdi) { conversation ->
            val msgs = conversation.messages.toMutableList()
            val idx = msgs.indexOfLast {
                it is ChatItem.Assistant &&
                    (it as ChatItem.Assistant).text.isBlank() &&
                    (messageId == null || it.messageId == messageId || it.messageId == null)
            }
            if (idx >= 0) msgs.removeAt(idx)
            conversation.copy(messages = msgs, lastActiveAt = currentTimeMillis())
        }   
    }

    fun appendMessageToConversationOnCdi(
        conversationId: String?,
        targetCdi: String?,
        message: ChatItem,
    ) {
        mutateConversationOnCdi(conversationId, targetCdi) { conversation ->
            val updatedMessages = conversation.messages + message
            val nextTitle = when {
                conversation.title != "新对话" && conversation.title.isNotBlank() -> conversation.title
                message is ChatItem.User && message.text.isNotBlank() -> message.text.trim()
                message is ChatItem.UserAttachments && !message.text.isNullOrBlank() -> message.text.trim()
                else -> conversation.title
            }
            conversation.copy(
                title = nextTitle,
                messages = updatedMessages,
                lastActiveAt = currentTimeMillis()
            )
        }
    }

    fun appendMessageToConversation(conversationId: String?, message: ChatItem) {
        // 默认走当前设备。需要把消息落到指定 cdi 时调用 [appendMessageToConversationOnCdi]。
        appendMessageToConversationOnCdi(conversationId, targetCdi = null, message = message)
    }

    val orderedConversations = conversations
        .sortedWith(compareByDescending<ConversationItem> { it.id == selectedConversationId }
            .thenByDescending { it.lastActiveAt })

    val currentConversation = conversations.firstOrNull { it.id == selectedConversationId }
        ?: orderedConversations.firstOrNull()
    val currentMessages = currentConversation?.messages.orEmpty()
    var hasInitializedBottomForConversation by remember(selectedConversationId) { mutableStateOf(false) }


    val messageListState = rememberLazyListState()
    var shouldAutoFollowBottom by remember { mutableStateOf(true) }
    var lastMessageListInteractionAt by remember { mutableStateOf(0L) }
    val autoFollowBottomThresholdPx = with(LocalDensity.current) { 120.dp.roundToPx() }

    // 计算距离底部的像素距离：
    // - 小于阈值：认为仍在底部附近，允许流式输出继续自动跟随。
    // - 大于阈值：认为用户正在浏览历史，停止自动拉到底部。
    val bottomDistancePx by remember(messageListState) {
        derivedStateOf {
            val info = messageListState.layoutInfo
            val visible = info.visibleItemsInfo
            if (visible.isEmpty()) {
                0
            } else {
                val lastVisible = visible.last()
                val itemsBelowLastVisible = (info.totalItemsCount - 1 - lastVisible.index).coerceAtLeast(0)
                val viewportGap = (lastVisible.offset + lastVisible.size - info.viewportEndOffset).coerceAtLeast(0)
                if (itemsBelowLastVisible > 0) {
                    autoFollowBottomThresholdPx + 1
                } else {
                    viewportGap
                }
            }
        }
    }

    val isNearBottom by remember(messageListState) {
        derivedStateOf {
            bottomDistancePx <= autoFollowBottomThresholdPx
        }
    }

    LaunchedEffect(isNearBottom) {
        if (isNearBottom) {
            shouldAutoFollowBottom = true
        }
    }

    LaunchedEffect(messageListState, isNearBottom) {
        snapshotFlow { messageListState.isScrollInProgress }
            .collect { scrolling ->
                if (scrolling) {
                    lastMessageListInteractionAt = currentTimeMillis()
                    if (!isNearBottom) {
                        shouldAutoFollowBottom = false
                    }
                }
            }
    }

    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    var attachmentsExpanded by remember { mutableStateOf(false) }
    var previewState by remember { mutableStateOf<ImagePreviewState?>(null) }
    var selectedChatImageId by remember { mutableStateOf<String?>(null) }
    var selectedChatImages by remember { mutableStateOf<List<NasImageItem>>(emptyList()) }
    var selectedRecordingAudio by remember { mutableStateOf<NasAudioItem?>(null) }
    var selectedChatAudio by remember { mutableStateOf<NasAudioItem?>(null) }
    var selectedChatDocument by remember { mutableStateOf<NasDocumentItem?>(null) }
    var showProfilePage by remember { mutableStateOf(false) }
    var showRechargePage by remember { mutableStateOf(false) }
    var showRechargePackagePage by remember { mutableStateOf(false) }
    var showNasNotSupportedDialog by remember { mutableStateOf(false) }
    var taobaoLinkUrl by remember { mutableStateOf<String?>(null) }
    var showSearchPage by remember { mutableStateOf(false) }
    var emptyViewState by remember { mutableStateOf(0) }
    val draftAttachments = remember { mutableStateListOf<DraftAttachment>() }
    var lastPickedImagesSize by remember { mutableStateOf(0) }
    var lastPickedFilesSize by remember { mutableStateOf(0) }
    var isVoiceBusy by remember { mutableStateOf(false) }
    var voiceRecordingStartedAtMillis by remember { mutableStateOf<Long?>(null) }
    val activeStreamingRequests = remember { mutableStateMapOf<String, String>() }
    val isStopMode by remember { derivedStateOf { activeStreamingRequests.isNotEmpty() } }
    val attachmentUploadStates = remember { mutableStateMapOf<String, AttachmentUploadState>() }
    val pendingStopMessageIds = remember { mutableStateListOf<String>() }
    val hiddenStopReplyMessageIds = remember { mutableStateListOf<String>() }
    val discardedReplyMessageIds = remember { mutableStateListOf<String>() }
    val processedEventIds = remember { mutableStateListOf<String>() }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var composerHeightPx by remember { mutableStateOf(0) }
    val audioBlobCacheMap = remember { mutableStateMapOf<String, String>() }
    val loadingAudioBlobRefs = remember { mutableStateListOf<String>() }
    val density = LocalDensity.current
    val toastBottomPadding = with(density) { composerHeightPx.toDp() + 16.dp }

    // ── 余额不足提醒（仅一次） ──
    val balanceWsManager = koinInject<BalanceWsManager>()
    val settings = koinInject<Settings>()
    var showLowBalanceDialog by remember { mutableStateOf(false) }
    val balanceData by balanceWsManager.balance.collectAsState()
    val totalBalance = (balanceData.balances["1"] ?: 0L) + (balanceData.balances["4"] ?: 0L)

    val lowBalanceDismissedKey = effectiveUserId?.let { "$LOW_BALANCE_DISMISSED_KEY.$it" }

    LaunchedEffect(totalBalance, lowBalanceDismissedKey) {
        if (totalBalance in 1..499) {
            val key = lowBalanceDismissedKey ?: return@LaunchedEffect
            val dismissed = settings.getBoolean(key, false)
            if (!dismissed) {
                showLowBalanceDialog = true
            }
        }
    }

    LaunchedEffect(selectedConversationId, currentMessages.size) {
        if (hasInitializedBottomForConversation) return@LaunchedEffect
        if (currentMessages.isEmpty()) {
            hasInitializedBottomForConversation = true
            return@LaunchedEffect
        }
        val lastIndex = currentMessages.size + 1
        messageListState.scrollToItem(lastIndex)
        shouldAutoFollowBottom = true
        hasInitializedBottomForConversation = true
    }

    // 发送 / 新增消息时动画滚动到底部
    LaunchedEffect(currentMessages.size, shouldAutoFollowBottom) {
        if (shouldAutoFollowBottom && currentMessages.isNotEmpty()) {
            // +2: top_spacer + bottom_spacer
            val lastIndex = currentMessages.size + 1
            messageListState.animateScrollToItem(lastIndex)
        }
    }

    // streaming 期间持续跟随底部（item 高度增长时视口不会自动跟）。
    // 仅当用户已经停留在底部、且当前没有在进行手势滚动时才跟随；
    // 用户一旦向上滑动离开底部，本循环会自动停止追随，让用户可以自由浏览历史。
    LaunchedEffect(assistantReplyStreaming) {
        if (!assistantReplyStreaming) return@LaunchedEffect
        while (isActive) {
            delay(200)
            if (!shouldAutoFollowBottom) continue
            if (messageListState.isScrollInProgress) continue
            if (currentTimeMillis() - lastMessageListInteractionAt < 350L) continue
            val total = messageListState.layoutInfo.totalItemsCount
            if (total > 0) {
                messageListState.scrollToItem(total - 1)
            }
        }
    }

    fun scrollToLatestMessage() {
        coroutineScope.launch {
            withTimeoutOrNull(500L) {
                snapshotFlow { messageListState.layoutInfo.totalItemsCount }
                    .filter { it > 0 }
                    .first()
            }
            val total = messageListState.layoutInfo.totalItemsCount
            if (total > 0) {
                messageListState.scrollToItem(total - 1)
            }
        }
    }

    fun finishComposerEditing() {
        focusManager.clearFocus(force = true)
    }

    fun AudioRecording.toNasAudioItem(): NasAudioItem {
        val format = name.substringAfterLast('.', "").lowercase().ifBlank { "m4a" }
        val resolvedBlobRef = blobRef?.takeIf { it.isNotBlank() }
            ?: error("音频 blobRef 缺失")
        return NasAudioItem(
            id = id,
            name = name,
            type = inferContentType(name) ?: "audio/*",
            format = format,
            sizeKB = 0,
            path = resolvedBlobRef,
            time = "",
            durationSec = 0,
        )
    }

    fun handleRecordingOpen(recording: AudioRecording) {
        focusManager.clearFocus()
        attachmentsExpanded = false
        selectedRecordingAudio = recording.toNasAudioItem()
    }

    fun attachmentFileName(attachment: MediaAttachment): String {
        val explicit = attachment.fileName?.trim().orEmpty()
        if (explicit.isNotBlank()) return explicit
        val ext = attachment.contentType?.let(::extensionForContentType)
        return if (ext != null) {
            "attachment.$ext"
        } else {
            "attachment"
        }
    }

    fun attachmentFormat(attachment: MediaAttachment): String {
        val fileName = attachmentFileName(attachment)
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isNotBlank()) return ext
        return attachment.contentType?.substringAfterLast('/')?.substringBefore('+')?.lowercase().orEmpty().ifBlank { "file" }
    }

    fun MediaAttachment.toNasImageItem(index: Int): NasImageItem {
        val fileName = attachmentFileName(this)
        val format = attachmentFormat(this)
        return NasImageItem(
            id = "chat-image-$index-$blobRef",
            name = fileName,
            type = contentType ?: "image/*",
            format = format,
            sizeKB = 0,
            path = blobRef,
            time = "",
            location = null,
            resolution = "",
        )
    }

    fun MediaAttachment.toNasAudioItem(): NasAudioItem {
        val fileName = attachmentFileName(this)
        val format = attachmentFormat(this)
        return NasAudioItem(
            id = "chat-audio-$blobRef",
            name = fileName,
            type = contentType ?: "audio/*",
            format = format,
            sizeKB = 0,
            path = blobRef,
            time = "",
            durationSec = 0,
        )
    }

    fun MediaAttachment.toNasDocumentItem(): NasDocumentItem {
        val fileName = attachmentFileName(this)
        val format = attachmentFormat(this)
        return NasDocumentItem(
            id = "chat-document-$blobRef",
            name = fileName,
            type = contentType ?: "application/octet-stream",
            format = format,
            sizeKB = 0,
            path = blobRef,
            time = "",
        )
    }

    fun handleAttachmentDownload(attachment: MediaAttachment) {
        coroutineScope.launch {
            toastMessage = "正在下载…"
            val result = sdkSessionManager.fetchBlobBytes(attachment.blobRef)
            result.onSuccess { bytes ->
                val fileName = attachment.fileName ?: "file_${currentTimeMillis()}"
                val mimeType = attachment.contentType ?: "application/octet-stream"
                runCatching {
                    platformSaveFile(bytes, fileName, mimeType)
                }.onSuccess {
                    toastMessage = "下载成功"
                }.onFailure { e ->
                    toastMessage = "保存失败: ${e.message}"
                }
            }.onFailure { e ->
                toastMessage = "下载失败: ${e.message}"
            }
        }
    }

    fun handleToggleAudioAttachmentPlayback(attachment: MediaAttachment) {
        val blobRef = attachment.blobRef
        val sourceId = "chat-audio-$blobRef"
        val fileName = attachment.fileName?.trim()?.takeIf { it.isNotBlank() }
            ?: "audio_${blobRef.take(8)}"
        val playbackState = mediaAccessController.audioPlaybackState

        // 当前正在播放/暂停同一个源 → 直接 toggle
        if (playbackState.sourceId == sourceId) {
            val cachedPath = audioBlobCacheMap[blobRef] ?: return
            mediaAccessController.toggleAudioPlayback(sourceId, fileName, cachedPath)
            return
        }

        // 已有缓存 → 直接播放
        val cachedPath = audioBlobCacheMap[blobRef]
        if (cachedPath != null) {
            mediaAccessController.toggleAudioPlayback(sourceId, fileName, cachedPath)
            return
        }

        // 需要下载 blob → 缓存 → 播放
        if (blobRef in loadingAudioBlobRefs) return
        loadingAudioBlobRefs.add(blobRef)
        coroutineScope.launch {
            runCatching {
                val bytes = sdkSessionManager.fetchBlobBytes(blobRef).getOrThrow()
                val path = platformSaveCacheFile(bytes, fileName)
                audioBlobCacheMap[blobRef] = path
                mediaAccessController.toggleAudioPlayback(sourceId, fileName, path)
            }.onFailure { e ->
                toastMessage = "音频加载失败: ${e.message}"
            }
            loadingAudioBlobRefs.remove(blobRef)
        }
    }

    fun handleToggleUserAudioAttachmentPlayback(attachment: DraftAttachment) {
        val initialBlobRef = attachment.blobRef
            ?: (attachmentUploadStates[attachment.uri] as? AttachmentUploadState.Success)?.blobRef
        val nasFileId = attachment.nasFileId
        val fileName = attachment.displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: uriDisplayName(attachment.uri)
        coroutineScope.launch {
            val blobRef = initialBlobRef ?: run {
                if (nasFileId == null) {
                    toastMessage = "音频仍在上传，暂不可播放"
                    return@launch
                }
                runCatching {
                    sdkSessionManager.getFileFromNas(
                        targetCdi = currentCdi.orEmpty(),
                        fileId = nasFileId,
                    ).getOrThrow().item?.blobRef
                }.getOrNull()?.takeIf { it.isNotBlank() }
            }

            if (blobRef.isNullOrBlank()) {
                toastMessage = "音频仍在上传，暂不可播放"
                return@launch
            }

            val sourceId = "chat-audio-$blobRef"
            val playbackState = mediaAccessController.audioPlaybackState

            if (playbackState.sourceId == sourceId) {
                val cachedPath = audioBlobCacheMap[blobRef] ?: return@launch
                mediaAccessController.toggleAudioPlayback(sourceId, fileName, cachedPath)
                return@launch
            }

            val cachedPath = audioBlobCacheMap[blobRef]
            if (cachedPath != null) {
                mediaAccessController.toggleAudioPlayback(sourceId, fileName, cachedPath)
                return@launch
            }

            if (blobRef in loadingAudioBlobRefs) return@launch
            loadingAudioBlobRefs.add(blobRef)
            runCatching {
                val bytes = sdkSessionManager.fetchBlobBytes(blobRef).getOrThrow()
                val path = platformSaveCacheFile(bytes, fileName)
                audioBlobCacheMap[blobRef] = path
                mediaAccessController.toggleAudioPlayback(sourceId, fileName, path)
            }.onFailure { e ->
                toastMessage = "音频加载失败: ${e.message}"
            }
            loadingAudioBlobRefs.remove(blobRef)
        }
    }

    fun handleUserAttachmentOpen(attachment: DraftAttachment) {
        focusManager.clearFocus()
        attachmentsExpanded = false
        val nasFileId = attachment.nasFileId
        val blobRef = attachment.blobRef
            ?: (attachmentUploadStates[attachment.uri] as? AttachmentUploadState.Success)?.blobRef
        if (nasFileId == null && blobRef.isNullOrBlank()) {
            toastMessage = when (attachment.type) {
                DraftAttachmentType.Audio -> "音频仍在上传，暂不可预览"
                DraftAttachmentType.Image -> "图片仍在上传，暂不可预览"
                DraftAttachmentType.File -> "文件仍在上传，暂不可预览"
            }
            return
        }
        val fileName = attachment.displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: uriDisplayName(attachment.uri)
        val contentType = inferContentType(fileName)
        val format = fileName.substringAfterLast('.', "").lowercase().ifBlank { "file" }

        when (attachment.type) {
            DraftAttachmentType.Image -> {
                val imageItem = NasImageItem(
                    id = "user-image-${attachment.uri.hashCode()}",
                    fileId = nasFileId,
                    name = fileName,
                    type = contentType,
                    format = format,
                    sizeKB = 0,
                    path = blobRef ?: attachment.uri,
                    time = "",
                    location = null,
                    resolution = "",
                )
                selectedChatImages = listOf(imageItem)
                selectedChatImageId = imageItem.id
            }
            DraftAttachmentType.Audio -> {
                selectedChatAudio = NasAudioItem(
                    id = "user-audio-${attachment.uri.hashCode()}",
                    fileId = nasFileId,
                    name = fileName,
                    type = contentType,
                    format = format,
                    sizeKB = 0,
                    path = blobRef ?: attachment.uri,
                    time = "",
                    durationSec = 0,
                )
            }
            DraftAttachmentType.File -> {
                selectedChatDocument = NasDocumentItem(
                    id = "user-doc-${attachment.uri.hashCode()}",
                    fileId = nasFileId,
                    name = fileName,
                    type = contentType,
                    format = format,
                    sizeKB = 0,
                    path = blobRef ?: attachment.uri,
                    time = "",
                )
            }
        }
    }

    fun handleAttachmentOpen(attachment: MediaAttachment, gallery: List<MediaAttachment>) {
        focusManager.clearFocus()
        attachmentsExpanded = false
        val inferred = inferContentType(attachmentFileName(attachment))
        val contentType = if (inferred != "application/octet-stream") {
            inferred
        } else {
            attachment.contentType?.takeIf { it.isNotBlank() } ?: inferred
        }
        when {
            contentType.startsWith("image/") -> {
                val imageItems = gallery.mapIndexed { index, mediaAttachment -> mediaAttachment.toNasImageItem(index) }
                val currentId = imageItems.firstOrNull { it.path == attachment.blobRef }?.id ?: imageItems.firstOrNull()?.id
                if (currentId != null) {
                    selectedChatImages = imageItems
                    selectedChatImageId = currentId
                }
            }
            contentType.startsWith("audio/") -> {
                selectedChatAudio = attachment.toNasAudioItem()
            }
            else -> {
                selectedChatDocument = attachment.toNasDocumentItem()
            }
        }
    }

    // 自动消失 toast
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2000L)
            toastMessage = null
        }
    }

    fun startAttachmentUpload(uri: String, displayName: String? = null) {
        if (attachmentUploadStates[uri] is AttachmentUploadState.Success) return
        attachmentUploadStates[uri] = AttachmentUploadState.Uploading
        println("[AttachUpload] 开始上传附件 uri=$uri displayName=$displayName")
        coroutineScope.launch {
            val connectResult = sdkSessionManager.ensureConnectedIfTokenValid()
            if (connectResult.isFailure) {
                println("[AttachUpload] 连接失败: ${connectResult.exceptionOrNull()?.message}")
                attachmentUploadStates[uri] = AttachmentUploadState.Failed("连接失败")
                return@launch
            }
            println("[AttachUpload] 连接成功，开始读取文件字节...")
            val fileBytes = mediaAccessController.readUriToBytes(uri)
            println("[AttachUpload] 读取结果: bytes=${fileBytes?.size ?: "null"}")
            if (fileBytes == null) {
                attachmentUploadStates[uri] = AttachmentUploadState.Failed("读取文件失败")
                return@launch
            }
            var fileName = displayName?.trim()?.takeIf { it.isNotBlank() }
                ?: uriDisplayName(uri).ifBlank { "file" }
            var contentType = inferContentType(fileName)
            if (contentType == "application/octet-stream") {
                val detected = inferContentTypeFromBytes(fileBytes)
                if (detected != null) {
                    contentType = detected
                    val hasExt = fileName.contains('.')
                    if (!hasExt) {
                        val ext = extensionForContentType(detected)
                        if (ext != null) fileName = "$fileName.$ext"
                    }
                }
            }
            val uploadResult = sdkSessionManager.uploadImage(fileBytes, fileName)
            uploadResult.onSuccess { putResult ->
                attachmentUploadStates[uri] = AttachmentUploadState.Success(
                    blobRef = putResult.blobRef,
                    contentType = contentType,
                    size = fileBytes.size.toLong(),
                    fileName = fileName,
                )
            }
            uploadResult.onFailure { error ->
                attachmentUploadStates[uri] = AttachmentUploadState.Failed(error.message ?: "上传失败")
            }
        }
    }

    LaunchedEffect(Unit) {
        sdkSessionManager.connectIfTokenValid()
    }

    // ── 统一事件驱动：收到一条服务端推送就渲染一条 ──
    LaunchedEffect(Unit) {
        sdkSessionManager.npcReplyEvents.collect { event ->
            val msgId = event.messageId
            val eventId = event.eventId?.trim().orEmpty()
            if (msgId.isNotBlank() && msgId in discardedReplyMessageIds) {
                return@collect
            }
            val isStopReply = msgId.isNotBlank() && msgId in hiddenStopReplyMessageIds
            if (isStopReply && pendingStopMessageIds.remove(msgId)) {
            }
            if (isStopReply && event.type != "assistant.partial" && event.type != "assistant.final" && event.type != "assistant.complete") {
                if (pendingStopMessageIds.remove(msgId)) {
                }
                return@collect
            }
            if (event.type != "assistant.final" && event.type != "assistant.complete" && eventId.isNotEmpty()) {
                if (processedEventIds.contains(eventId)) {
                    return@collect
                }
                processedEventIds.add(eventId)
                if (processedEventIds.size > 500) {
                    processedEventIds.removeAt(0)
                }
            }
            val convId = if (msgId.isNotBlank()) {
                activeStreamingRequests[msgId]
                    ?: messageIdToConversationId[msgId]
                    ?: selectedConversationId
            } else {
                selectedConversationId
            }

            println("[Event] 收到事件 type=${event.type}, msgId=$msgId, convId=$convId, textLen=${event.text?.length ?: 0}, attachments=${event.attachments.size}")

            // 过滤不需要渲染的事件类型
            if (event.type in setOf("config.updated", "config.error", "restart.scheduled", "restart.completed")) {
                return@collect
            }

            // 无 sourceMessageId 的事件 → 独立消息，去重后追加
            if (msgId.isBlank()) {
                if (event.attachments.isEmpty() && event.text.isNullOrBlank()) return@collect
                appendAttachmentsToLatestAssistant(
                    conversationId = convId,
                    attachments = event.attachments,
                    text = event.text,
                    timestamp = event.timestamp,
                )
                return@collect
            }

            when (event.type) {
                "inbound.accepted" -> {
                    updateAssistantMessage(convId, msgId) { a ->
                        a.copy(
                            messageId = msgId, isStreaming = true,
                            streamEvents = a.streamEvents.addOrUpdate(StreamEvent("delivered", "已送达")),
                        )
                    }
                }
                "assistant.start" -> {
                    updateAssistantMessage(convId, msgId) { a ->
                        a.copy(
                            messageId = msgId, isStreaming = true,
                            streamEvents = a.streamEvents.addOrUpdate(StreamEvent("typing", "正在输入", isActive = true)),
                        )
                    }
                }
                "tool.start" -> {
                    val toolName = event.toolName ?: event.text ?: "工具"
                    updateAssistantMessage(convId, msgId) { a ->
                        a.copy(
                            messageId = msgId, isStreaming = true,
                            streamEvents = a.streamEvents
                                .addOrUpdate(StreamEvent("typing", "正在输入")) // 标记 typing 完成
                                .addOrUpdate(StreamEvent("tool", toolName, isActive = true)),
                        )
                    }
                }
                "tool.end" -> {
                    val toolName = event.toolName ?: event.text ?: "工具"
                    updateAssistantMessage(convId, msgId) { a ->
                        a.copy(
                            messageId = msgId, isStreaming = true,
                            streamEvents = a.streamEvents.addOrUpdate(StreamEvent("tool", toolName, isActive = false)),
                        )
                    }
                }
                "reasoning.partial" -> {
                    updateAssistantMessage(convId, msgId) { a ->
                        a.copy(
                            messageId = msgId, isStreaming = true,
                            reasoningText = event.text ?: a.reasoningText,
                            streamEvents = a.streamEvents.addOrUpdate(StreamEvent("reasoning", "Thinks", isActive = true)),
                        )
                    }
                }
                "reasoning.final" -> {
                    updateAssistantMessage(convId, msgId) { a ->
                        a.copy(
                            messageId = msgId, isStreaming = true,
                            reasoningText = event.text ?: a.reasoningText,
                            // streamEvents = a.streamEvents.addOrUpdate(StreamEvent("reasoning", "Thinks")),
                        )
                    }
                }
                "assistant.partial" -> {
                    if (event.text != null) {
                        updateAssistantMessage(convId, msgId) { a ->
                            a.copy(
                                text = mergeStreamingAssistantText(a.text, event.text), messageId = msgId, isStreaming = true,
                                streamEvents = a.streamEvents
                                    .addOrUpdate(StreamEvent("typing", "正在输入")), // 标记完成
                            )
                        }
                    }
                }
                "assistant.final" -> {
                    upsertAssistantFinalMessage(convId, msgId, event)
                }
                "assistant.complete" -> {
                    if (pendingStopMessageIds.isNotEmpty()) {
                        pendingStopMessageIds.clear()
                    }
                    updateAssistantMessage(convId, msgId) { a ->
                        val completedStreamEvents = a.streamEvents.markAllInactive().let { events ->
                            if (events.any { it.type == "reasoning" }) {
                                events
                                // .addOrUpdate(StreamEvent("reasoning", "reasoning"))
                            } 
                            else {
                                events
                            }
                        }.addOrUpdate(StreamEvent("finish", "Finish"))
                        a.copy(
                            text = event.text?.let { mergeIndependentFinalText(a.text, it) } ?: a.text,
                            messageId = msgId,
                            attachments = (a.attachments + event.attachments).distinctBy { it.blobRef },
                            timestamp = event.timestamp ?: a.timestamp,
                            isStreaming = false,
                            streamEvents = completedStreamEvents,
                        )
                    }
                    activeStreamingRequests.remove(msgId)
                    println("[Event] assistant.complete 结束流式 msgId=$msgId, convId=$convId, eventId=${event.eventId}")
                }
                "error" -> {
                    updateAssistantMessage(convId, msgId) { a ->
                        val completedStreamEvents = a.streamEvents.markAllInactive().let { events ->
                            if (events.any { it.type == "reasoning" }) {
                                events.addOrUpdate(StreamEvent("reasoning", "done"))
                            } else {
                                events
                            }
                        }
                        a.copy(
                            isStreaming = false,
                            streamEvents = completedStreamEvents.addOrUpdate(StreamEvent("finish", "Finish")),
                        )
                    }
                    removeEmptyAssistantPlaceholder(convId, msgId)
                    appendMessageToConversationOnCdi(
                        convId,
                        targetCdi = messageIdToCdi[msgId],
                        message = ChatItem.Error(event.text ?: "未知错误"),
                    )
                    println("[Event] error msgId=$msgId, convId=$convId, error=${event.text}")
                    activeStreamingRequests.remove(msgId)
                }
            }
        }
    }

    LaunchedEffect(attachmentsExpanded) {
        if (attachmentsExpanded) {
            mediaAccessController.refreshRecentImages()
        }
    }

    LaunchedEffect(mediaAccessController.isRecording) {
        voiceRecordingStartedAtMillis = if (mediaAccessController.isRecording) {
            currentTimeMillis()
        } else {
            null
        }
    }

    LaunchedEffect(mediaAccessController.pickedImages.size) {
        val size = mediaAccessController.pickedImages.size
        if (size > lastPickedImagesSize) {
            mediaAccessController.pickedImages
                .take(size - lastPickedImagesSize)
                .forEach { uri ->
                    if (
                        uri.isNotBlank() &&
                        draftAttachments.none { it.type == DraftAttachmentType.Image && it.uri == uri }
                    ) {
                        draftAttachments.add(DraftAttachment(DraftAttachmentType.Image, uri))
                        startAttachmentUpload(uri)
                    }
                }
            if (size > lastPickedImagesSize) {
                attachmentsExpanded = false
            }
        }
        lastPickedImagesSize = size
    }

    LaunchedEffect(mediaAccessController.pickedFiles.size) {
        val size = mediaAccessController.pickedFiles.size
        if (size > lastPickedFilesSize) {
            mediaAccessController.pickedFiles
                .take(size - lastPickedFilesSize)
                .forEach { pickedFile ->
                    if (
                        pickedFile.uri.isNotBlank() &&
                        draftAttachments.none { it.uri == pickedFile.uri }
                    ) {
                        val detectedType = detectDraftAttachmentType(
                            pickedFile.displayName.ifBlank { pickedFile.uri }
                        )
                        draftAttachments.add(
                            DraftAttachment(
                                type = detectedType,
                                uri = pickedFile.uri,
                                displayName = pickedFile.displayName
                            )
                        )
                        startAttachmentUpload(pickedFile.uri, pickedFile.displayName)
                    }
                }
            if (size > lastPickedFilesSize) {
                attachmentsExpanded = false
            }
        }
        lastPickedFilesSize = size
    }

    val sendMessage = Unit@{
        val text = inputText.text.trim()
        val attachments = draftAttachments.toList()

        if (text.isNotEmpty() || attachments.isNotEmpty()) {
            // ── 设备离线检查 ──
            if (currentCdi == null || currentCdi !in onlineDeviceCdis) {
                toastMessage = "设备不在线，请等待设备上线后重试"
                return@Unit
            }

            // ── 发送前校验本地附件上传状态 ──
            val localAttachments = attachments.filter { it.nasFileId == null }
            if (localAttachments.any { attachmentUploadStates[it.uri] is AttachmentUploadState.Failed }) {
                toastMessage = "文件上传失败请重新上传"
                return@Unit
            }
            if (localAttachments.any { attachmentUploadStates[it.uri] is AttachmentUploadState.Uploading }) {
                toastMessage = "文件上传中"
                return@Unit
            }
            if (localAttachments.any { attachmentUploadStates[it.uri] !is AttachmentUploadState.Success }) {
                toastMessage = "文件上传未完成，请稍后重试"
                return@Unit
            }

            val targetConversationId = selectedConversationId
            // 点击发送的瞬间捕获"归属设备"：用户点击后异步去连接/解析 targetCdi，期间如果
            // 切换了设备，后续的占位符清理、发送失败提示、成功后的 messageId 回填都必须
            // 落到原设备的存档里，而不是新设备。所以这里把 currentCdi 快照一份，全流程
            // 都用 sendingCdi 做路由 key。
            val sendingCdi = currentCdi

            shouldAutoFollowBottom = true

            // ── 特殊指令：脑花 功能/能力 → 直接展示技能卡片，不走 publishTextToNpc ──
            if (attachments.isEmpty() && isBrainBoxCapabilityQuery(text)) {
                appendMessageToConversation(targetConversationId, ChatItem.User(text))
                appendMessageToConversation(targetConversationId, ChatItem.SkillSuggestions)
                scrollToLatestMessage()
                finishComposerEditing()
                inputText = TextFieldValue("")
                attachmentsExpanded = false
                return@Unit
            }

            if (attachments.isNotEmpty()) {
                appendMessageToConversation(
                    targetConversationId,
                    ChatItem.UserAttachments(
                        text = text.ifBlank { null },
                        attachments = attachments
                    )
                )
                draftAttachments.clear()
            } else {
                appendMessageToConversation(targetConversationId, ChatItem.User(text))
            }

            val localMediaAttachments = localAttachments
            val nasAttachments = attachments.filter { it.nasFileId != null }
            val hasMediaAttachments = localMediaAttachments.isNotEmpty() || nasAttachments.isNotEmpty()
            val outgoingText =
                text.ifBlank {
                    if (hasMediaAttachments) "" else ""
                }
            // 在 launch 之前快照上传状态，避免下方同步 remove 导致协程内读到 null
            val uploadStatesSnapshot = attachmentUploadStates.toMap()
            println("[Chat] 发送消息: text=\"$outgoingText\", initialTargetCdi=$initialTargetCdi, snapshotOnlineCdis=$onlineDeviceCdis, localCount=${localMediaAttachments.size}, nasCount=${nasAttachments.size}, sendingCdi=$sendingCdi")
            appendMessageToConversation(
                targetConversationId,
                ChatItem.Assistant(text = "")
            )
            scrollToLatestMessage()

            finishComposerEditing()
            inputText = TextFieldValue("")
            attachmentsExpanded = false
            coroutineScope.launch {
                println("[Chat] ensureConnectedIfTokenValid 开始...")
                val connectResult = sdkSessionManager.ensureConnectedIfTokenValid()
                println("[Chat] ensureConnectedIfTokenValid 结果: isSuccess=${connectResult.isSuccess}, error=${connectResult.exceptionOrNull()?.message}")
                if (connectResult.isFailure) {
                    // 路由到点击发送时所在的设备：用户此刻可能已切到别的设备。
                    mutateConversationOnCdi(targetConversationId, sendingCdi) { conv ->
                        val msgs = conv.messages.toMutableList()
                        val idx = msgs.indexOfLast {
                            it is ChatItem.Assistant && it.messageId == null && (it as ChatItem.Assistant).text.isBlank()
                        }
                        if (idx >= 0) msgs.removeAt(idx)
                        conv.copy(messages = msgs, lastActiveAt = currentTimeMillis())
                    }
                    appendMessageToConversationOnCdi(
                        targetConversationId,
                        targetCdi = sendingCdi,
                        message = ChatItem.System("连接失败：${connectResult.exceptionOrNull()?.message ?: "unknown"}")
                    )
                    return@launch
                }

                // 解析发送目标 CDI（放在连接完成后、且必要时等待 observer 首次 emit）：
                // 优先级：用户显式选择 > 路由传入 > 当前首个在线设备；
                // 若 observer 尚未完成第一次 ping（刚进入页面），最多等 6s，避免"设备其实在线但刚进页面瞬发即报离线"。
                val targetCdi = run {
                    fun immediate(): String? =
                        sdkSessionManager.selectedDeviceCdi.value
                            ?: initialTargetCdi
                            ?: sdkSessionManager.onlineDeviceCdis.value.firstOrNull()

                    immediate() ?: run {
                        if (!sdkSessionManager.deviceObserverHasEmitted.value) {
                            println("[Chat] CDI 暂不可用，等待 observer 首次 emit (<= 6s)...")
                            withTimeoutOrNull(6_000L) {
                                sdkSessionManager.deviceObserverHasEmitted
                                    .filter { it }
                                    .first()
                            }
                        }
                        immediate()
                    }
                }

                if (targetCdi == null) {
                    println("[Chat] 解析 CDI 失败: observerHasEmitted=${sdkSessionManager.deviceObserverHasEmitted.value}, onlineCdis=${sdkSessionManager.onlineDeviceCdis.value}")
                    // 路由到发送瞬间捕获的 sendingCdi：通常此时 sendingCdi 同样为 null，
                    // mutateConversationOnCdi 会退化到当前内存路径，行为和原来一致；
                    // 但极少数情况下 sendingCdi 存在而 targetCdi=null（比如 observer 瞬时掉线），
                    // 这时消息应该留在原设备的存档里。
                    mutateConversationOnCdi(targetConversationId, sendingCdi) { conv ->
                        val msgs = conv.messages.toMutableList()
                        val idx = msgs.indexOfLast {
                            it is ChatItem.Assistant && it.messageId == null && (it as ChatItem.Assistant).text.isBlank()
                        }
                        if (idx >= 0) msgs.removeAt(idx)
                        conv.copy(messages = msgs, lastActiveAt = currentTimeMillis())
                    }
                    appendMessageToConversationOnCdi(
                        targetConversationId,
                        targetCdi = sendingCdi,
                        message = ChatItem.System("没有可用的设备，请等待设备上线后重试")
                    )
                    return@launch
                }
                println("[Chat] 解析到 targetCdi=$targetCdi")

                val sendResult = if (localMediaAttachments.isNotEmpty() || nasAttachments.isNotEmpty()) {
                    // 收集所有附件的 MediaItem
                    val mediaItems = mutableListOf<SdkSessionManager.MediaItem>()

                    // ── 处理本地附件（图片/文件/音频，已在选取时预上传）──
                    for (att in localMediaAttachments) {
                        val uploadState = uploadStatesSnapshot[att.uri]
                        if (uploadState is AttachmentUploadState.Success) {
                            mediaItems.add(SdkSessionManager.MediaItem(
                                blobRef = uploadState.blobRef,
                                contentType = uploadState.contentType,
                                size = uploadState.size,
                                fileName = uploadState.fileName,
                            ))
                        }
                        // 发送前已校验过全部 Success，此处不应到达 else 分支
                    }

                    // ── 处理 NAS 附件（通过 getFileFromNas 获取 blobRef，无需重新上传）──
                    for (att in nasAttachments) {
                        val nasFileId = att.nasFileId ?: continue
                        val getResult = sdkSessionManager.getFileFromNas(targetCdi, nasFileId)
                        if (getResult.isFailure) {
                            println("[Chat] NAS 文件获取失败 fileId=$nasFileId: ${getResult.exceptionOrNull()?.message}")
                            mutateConversationOnCdi(targetConversationId, sendingCdi) { conv ->
                                val msgs = conv.messages.toMutableList()
                                val idx = msgs.indexOfLast {
                                    it is ChatItem.Assistant && it.messageId == null && (it as ChatItem.Assistant).text.isBlank()
                                }
                                if (idx >= 0) msgs.removeAt(idx)
                                conv.copy(messages = msgs, lastActiveAt = currentTimeMillis())
                            }
                            appendMessageToConversationOnCdi(
                                targetConversationId,
                                targetCdi = sendingCdi,
                                message = ChatItem.System("NAS 文件获取失败：${getResult.exceptionOrNull()?.message ?: "unknown"}")
                            )
                            return@launch
                        }
                        val nasResponse = getResult.getOrThrow()
                        val blobRef = nasResponse.item?.blobRef
                        if (blobRef.isNullOrBlank()) {
                            println("[Chat] NAS 文件缺少 blobRef fileId=$nasFileId")
                            mutateConversationOnCdi(targetConversationId, sendingCdi) { conv ->
                                val msgs = conv.messages.toMutableList()
                                val idx = msgs.indexOfLast {
                                    it is ChatItem.Assistant && it.messageId == null && (it as ChatItem.Assistant).text.isBlank()
                                }
                                if (idx >= 0) msgs.removeAt(idx)
                                conv.copy(messages = msgs, lastActiveAt = currentTimeMillis())
                            }
                            appendMessageToConversationOnCdi(
                                targetConversationId,
                                targetCdi = sendingCdi,
                                message = ChatItem.System("NAS 文件数据异常（缺少 blobRef），请重试")
                            )
                            return@launch
                        }
                        val fileName = att.displayName ?: "file"
                        val contentType = inferContentType(fileName)
                        val size = (nasResponse.item?.size ?: 0L)
                        mediaItems.add(SdkSessionManager.MediaItem(
                            blobRef = blobRef,
                            contentType = contentType,
                            size = size,
                            fileName = fileName,
                        ))
                    }

                    sdkSessionManager.publishTextWithAttachmentsToNpc(
                        cdi = targetCdi,
                        text = outgoingText,
                        mediaItems = mediaItems,
                    )
                } else {
                    println("[Chat] 发送消息: publishTextToNpc cdi=$targetCdi text=$outgoingText")
                    sdkSessionManager.publishTextToNpc(cdi = targetCdi, text = outgoingText)
                }

                sendResult.onSuccess { messageId ->
                    println("[Chat] 发送成功: messageId=$messageId, targetCdi=$targetCdi, sendingCdi=$sendingCdi, convId=$targetConversationId")

                    // 关键：把 messageId → sendingCdi 登记下来，后续流式回复/错误会用这个映射
                    // 把更新路由到原设备的存档里，即使此刻用户已经切换到其它设备。
                    if (sendingCdi != null) {
                        messageIdToCdi[messageId] = sendingCdi
                    }
                    if (targetConversationId != null) {
                        messageIdToConversationId[messageId] = targetConversationId
                    }

                    // 回填用户消息 + Assistant 占位符的 messageId；同样按 sendingCdi 路由：
                    // 当前设备还是它就走内存，不是就直接落到它的磁盘存档。
                    mutateConversationOnCdi(targetConversationId, sendingCdi) { conv ->
                        val msgs = conv.messages.toMutableList()

                        // 1) 回填最后一条"尚未绑定 messageId"的用户消息（就是刚刚发出去的那条）。
                        //    持久化时这条消息就能带上 SDK 返回的 19 位 messageId，和助手回复配对。
                        val userIdx = msgs.indexOfLast {
                            (it is ChatItem.User && it.messageId == null) ||
                                (it is ChatItem.UserAttachments && it.messageId == null)
                        }
                        if (userIdx >= 0) {
                            when (val old = msgs[userIdx]) {
                                is ChatItem.User -> msgs[userIdx] = old.copy(messageId = messageId)
                                is ChatItem.UserAttachments -> {
                                    val resolvedAttachments = old.attachments.map { attachment ->
                                        if (attachment.nasFileId != null) {
                                            attachment.copy(blobRef = attachment.uri)
                                        } else {
                                            val uploadedBlobRef = uploadStatesSnapshot[attachment.uri]
                                                ?.let { it as? AttachmentUploadState.Success }
                                                ?.blobRef
                                            attachment.copy(blobRef = uploadedBlobRef ?: attachment.blobRef)
                                        }
                                    }
                                    msgs[userIdx] = old.copy(
                                        attachments = resolvedAttachments,
                                        messageId = messageId,
                                    )
                                }
                                else -> {}
                            }
                        }

                        // 2) 给 Assistant 占位符绑上同一个 messageId（作为 source_message_id），
                        //    流式回调通过它在本地找到占位符并 upsert 成真实回复。
                        val assistantIdx = msgs.indexOfLast {
                            it is ChatItem.Assistant &&
                                    it.messageId == null &&
                                    (it as ChatItem.Assistant).text.isBlank()
                        }
                        println("[Chat] 回填 messageId: userIdx=$userIdx, assistantIdx=$assistantIdx, totalMsgs=${msgs.size}")
                        if (assistantIdx >= 0) {
                            val placeholder = msgs[assistantIdx] as ChatItem.Assistant
                            msgs[assistantIdx] = placeholder.copy(messageId = messageId)
                        }
                        conv.copy(messages = msgs)
                    }

                    if (targetConversationId != null) {
                        activeStreamingRequests[messageId] = targetConversationId
                        println("[Chat] activeStreamingRequests 已添加: msgId=$messageId → convId=$targetConversationId, cdi=$sendingCdi, size=${activeStreamingRequests.size}")
                    }
                }

                sendResult.onFailure { error ->
                    println("[Chat] 发送失败: targetCdi=$targetCdi, sendingCdi=$sendingCdi, error=${error.message}")
                    // 同样路由到 sendingCdi：失败提示要落到用户发送时所在的那台设备的存档。
                    mutateConversationOnCdi(targetConversationId, sendingCdi) { conv ->
                        val msgs = conv.messages.toMutableList()
                        val idx = msgs.indexOfLast {
                            it is ChatItem.Assistant && it.messageId == null && (it as ChatItem.Assistant).text.isBlank()
                        }
                        if (idx >= 0) msgs.removeAt(idx)
                        conv.copy(messages = msgs, lastActiveAt = currentTimeMillis())
                    }
                    appendMessageToConversationOnCdi(
                        targetConversationId,
                        targetCdi = sendingCdi,
                        message = ChatItem.System("发送失败：${error.message ?: "unknown"}")
                    )
                }
            }

            inputText = TextFieldValue("")
            attachmentsExpanded = false
            focusManager.clearFocus()
            attachments.forEach { att ->
                if (att.nasFileId == null) {
                    attachmentUploadStates.remove(att.uri)
                }
            }
        }
    }

    val sendStopMessage = Unit@{
        if (!isStopMode) return@Unit
        val activeReplyMessageId = activeStreamingRequests.keys.lastOrNull()
        if (activeReplyMessageId.isNullOrBlank()) return@Unit
        activeStreamingRequests.remove(activeReplyMessageId)
        discardedReplyMessageIds.remove(activeReplyMessageId)
        discardedReplyMessageIds.add(activeReplyMessageId)
        if (discardedReplyMessageIds.size > 50) {
            discardedReplyMessageIds.removeAt(0)
        }
        println("[Stop] 标记丢弃回复 source_message_id=$activeReplyMessageId")
        val targetCdi = currentCdi
        if (targetCdi == null || targetCdi !in onlineDeviceCdis) {
            toastMessage = "设备不在线，请等待设备上线后重试"
            return@Unit
        }
        coroutineScope.launch {
            println("[Chat] 发送停止指令: cdi=$targetCdi text=/stop")
            val connectResult = sdkSessionManager.ensureConnectedIfTokenValid()
            if (connectResult.isFailure) {
                toastMessage = "连接失败：${connectResult.exceptionOrNull()?.message ?: "unknown"}"
                return@launch
            }
            val stopResult = sdkSessionManager.publishTextToNpc(cdi = targetCdi, text = "/stop")
            stopResult.onSuccess { messageId ->
                println("[Stop] 停止指令发送成功 stopMessageId=$messageId")
                pendingStopMessageIds.clear()
                pendingStopMessageIds.add(messageId)
                hiddenStopReplyMessageIds.remove(messageId)
                hiddenStopReplyMessageIds.add(messageId)
                if (hiddenStopReplyMessageIds.size > 20) {
                    hiddenStopReplyMessageIds.removeAt(0)
                }
            }
            stopResult.onFailure { error ->
                toastMessage = "停止失败：${error.message ?: "unknown"}"
            }
        }
    }

    // 详情页过渡动画参数
    val detailEnterSlide = slideInHorizontally(
        animationSpec = tween(220, easing = FastOutSlowInEasing)
    ) { it / 5 } + fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))
    val detailExitSlide = slideOutHorizontally(
        animationSpec = tween(200, easing = FastOutSlowInEasing)
    ) { it / 5 } + fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing))
    val imageEnter = fadeIn(animationSpec = tween(260)) + scaleIn(
        initialScale = 0.92f, animationSpec = tween(260, easing = FastOutSlowInEasing)
    )
    val imageExit = fadeOut(animationSpec = tween(220)) + scaleOut(
        targetScale = 0.92f, animationSpec = tween(220, easing = FastOutSlowInEasing)
    )

    // remember snapshot：AnimatedVisibility exit 动画期间 state 已被清空，
    // 需要缓存最后一次非 null 的值，让退场动画仍能渲染内容。
    val rememberedChatImages = remember { mutableStateOf<List<NasImageItem>>(emptyList()) }
    val rememberedChatImageId = remember { mutableStateOf<String?>(null) }
    val rememberedChatAudio = remember { mutableStateOf<NasAudioItem?>(null) }
    val rememberedChatDocument = remember { mutableStateOf<NasDocumentItem?>(null) }
    val rememberedRecordingAudio = remember { mutableStateOf<NasAudioItem?>(null) }

    if (selectedChatImages.isNotEmpty()) rememberedChatImages.value = selectedChatImages
    if (selectedChatImageId != null) rememberedChatImageId.value = selectedChatImageId
    if (selectedChatAudio != null) rememberedChatAudio.value = selectedChatAudio
    if (selectedChatDocument != null) rememberedChatDocument.value = selectedChatDocument
    if (selectedRecordingAudio != null) rememberedRecordingAudio.value = selectedRecordingAudio

    DesignScaleProvider {
    Box(modifier = Modifier.fillMaxSize()) {

    // ── 主聊天始终渲染 ──
    if (previewState == null) {
        Scaffold(
            containerColor = Color(0xFFF5F5F7),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F7))
                    .padding(padding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(color = Color(0xFFF5F5F7)) {
                        AgentModelTopBar(
                            title = "脑花",
                            subtitle = "内容由 AI 生成",
                            onOpenProfile = {
                                focusManager.clearFocus()
                                attachmentsExpanded = false
                                previewState = null
                                showProfilePage = true
                            },
                            onCall = {
                                attachmentsExpanded = false
                                uriHandler.openUri("tel:")
                            },
                            onPillClick = {
                                focusManager.clearFocus()
                                attachmentsExpanded = false
                                previewState = null
                                if (currentMessages.isNotEmpty()) {
                                    coroutineScope.launch {
                                        val cdi = currentCdi
                                        val device = if (!cdi.isNullOrBlank())
                                            authRepository.findDeviceByChannelDeviceId(cdi) else null
                                        if (device?.deviceType == "ai_npc") {
                                            onNavigateToNas()
                                        } else {
                                            showNasNotSupportedDialog = true
                                        }
                                    }
                                } else {
                                    showRechargePage = true
                                }
                            },
                            hasMessages = currentMessages.isNotEmpty(),
                            isDeviceOnline = currentCdi != null && currentCdi in onlineDeviceCdis
                        )
                    }
                    if (currentMessages.isEmpty() && emptyViewState != 2) {
                        val ds = LocalDesignScale.current
                        val glassBrush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFDFDFDF).copy(alpha = 0.10f),
                                Color.White
                            )
                        )
                        val cardShape = RoundedCornerShape(ds.sm(16.dp))

                        // ── 欢迎页 ──
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = ds.sw(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "探索您的精力上线",
                                    color = Color(0xFF1F2535),
                                    fontSize = ds.sp(28f),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(ds.sh(33.dp)))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(8.dp, cardShape, ambientColor = Color.Black.copy(alpha = 0.15f), spotColor = Color.Black.copy(alpha = 0.20f))
                                        .clip(cardShape)
                                        .background(glassBrush)
                                        .border(1.dp, Color.White, cardShape)
                                        .padding(horizontal = ds.sw(16.dp), vertical = ds.sh(14.dp)),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // 文字区域可点击 → 以对话形式展示技能列表
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    appendMessageToConversation(
                                                        selectedConversationId,
                                                        ChatItem.SkillSuggestions
                                                    )
                                                    emptyViewState = 2
                                                },
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "探索脑花的能力",
                                                color = Color(0xFF1F2535),
                                                fontSize = ds.sp(16f),
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                        // 关闭按钮
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_close_circle),
                                            contentDescription = "Close",
                                            tint = Color.Unspecified,
                                            modifier = Modifier
                                                .size(ds.sm(24.dp))
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) { emptyViewState = 2 }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            AgentModelMessageList(
                                messages = currentMessages,
                                playingRecordingId = mediaAccessController.playingRecordingId,
                                onToggleRecordingPlayback = { mediaAccessController.toggleRecordingPlayback(it) },
                                onRecordingOpen = ::handleRecordingOpen,
                                onImageClick = { previewState = it },
                                onFileClick = { mediaAccessController.openFilePreview(it) },
                                onAudioFileOpen = ::handleRecordingOpen,
                                onUserAttachmentOpen = ::handleUserAttachmentOpen,
                                onToggleUserAudioAttachmentPlayback = ::handleToggleUserAudioAttachmentPlayback,
                                onTapMessageArea = {
                                    focusManager.clearFocus()
                                    attachmentsExpanded = false
                                },
                                onSkillClick = { skillText ->
                                    inputText = TextFieldValue(skillText)
                                    sendMessage()
                                },
                                onAttachmentOpen = ::handleAttachmentOpen,
                                onAttachmentDownload = ::handleAttachmentDownload,
                                audioPlaybackState = mediaAccessController.audioPlaybackState,
                                loadingAudioBlobRefs = loadingAudioBlobRefs.toSet(),
                                onToggleAudioAttachmentPlayback = ::handleToggleAudioAttachmentPlayback,
                                onCopySuccess = { toastMessage = "复制成功" },
                                streamingStatusText = streamingStatusText,
                                isStopMode = isStopMode,
                                activeThinkingMessageIds = activeStreamingRequests.keys,
                                hiddenStatusMessageIds = hiddenStopReplyMessageIds.toSet(),
                                listState = messageListState,
                                modifier = Modifier
                                    .pointerInput(Unit) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(pass = PointerEventPass.Final)
                                            lastMessageListInteractionAt = currentTimeMillis()
                                            val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                                            if (up != null) {
                                                lastMessageListInteractionAt = currentTimeMillis()
                                                val isShortTap = up.uptimeMillis - down.uptimeMillis < 200L
                                                if (isShortTap && !messageListState.isScrollInProgress) {
                                                    focusManager.clearFocus()
                                                    attachmentsExpanded = false
                                                }
                                            }
                                        }
                                    }
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp)
                            )


                            // 底部渐变遮罩
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFFF5F5F7).copy(alpha = 0f),
                                                Color(0xFFF5F5F7),
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    AgentModelComposer(
                        inputText = inputText,
                        onInputTextChange = { inputText = it },
                        draftAttachments = draftAttachments,
                        onRemoveDraftAttachment = { att ->
                            draftAttachments.remove(att)
                            if (att.nasFileId == null) {
                                attachmentUploadStates.remove(att.uri)
                            }
                        },
                        onImageClick = { previewState = it },
                        onFileClick = ::handleUserAttachmentOpen,
                        onAudioClick = { attachment ->
                            coroutineScope.launch {
                                val blobRef = attachment.blobRef
                                    ?: (attachmentUploadStates[attachment.uri] as? AttachmentUploadState.Success)?.blobRef
                                    ?: attachment.nasFileId?.let { fileId ->
                                        runCatching {
                                            sdkSessionManager.getFileFromNas(
                                                targetCdi = currentCdi.orEmpty(),
                                                fileId = fileId,
                                            ).getOrThrow().item?.blobRef
                                        }.getOrNull()?.takeIf { it.isNotBlank() }
                                    }
                                if (blobRef.isNullOrBlank()) {
                                    toastMessage = "音频仍在上传，暂不可预览"
                                    return@launch
                                }
                                handleRecordingOpen(
                                    attachment.asAudioRecording().copy(blobRef = blobRef)
                                )
                            }
                        },
                        audioPlaybackState = mediaAccessController.audioPlaybackState,
                        loadingAudioBlobRefs = loadingAudioBlobRefs.toSet(),
                        onToggleRecordingPlayback = { attachment ->
                            handleToggleUserAudioAttachmentPlayback(attachment)
                        },
                        isRecording = mediaAccessController.isRecording,
                        isVoiceBusy = isVoiceBusy,
                        onVoiceStart = {
                            if (isVoiceBusy || mediaAccessController.isRecording) return@AgentModelComposer
                            focusManager.clearFocus()
                            attachmentsExpanded = false
                            mediaAccessController.startVoiceInput()
                        },
                        attachmentsExpanded = attachmentsExpanded,
                        onToggleAttachments = {
                            attachmentsExpanded = !attachmentsExpanded
                            if (attachmentsExpanded) focusManager.clearFocus()
                        },
                        onSend = sendMessage,
                        onStop = sendStopMessage,
                        isStopMode = isStopMode,
                        isSendDisabled = false,
                        onSuggestionClick = { appendMessageToConversation(selectedConversationId, ChatItem.User(it)) },
                        uploadStates = attachmentUploadStates,
                        modifier = Modifier.onSizeChanged { composerHeightPx = it.height },
                    )

                
                }

                if (attachmentsExpanded) {
                    AgentModelAttachmentPanel(
                        recentImages = mediaAccessController.recentImages,
                        hasMoreRecentImages = mediaAccessController.hasMoreRecentImages,
                        onLoadMoreRecentImages = { mediaAccessController.loadMoreRecentImages() },
                        onOpenCamera = {
                            mediaAccessController.openCamera()
                        },
                        onOpenFilePicker = {
                            mediaAccessController.openFilePicker()
                        },
                        onImagesSelected = { uris ->
                            uris.forEach { uri ->
                                if (uri.isNotBlank() && draftAttachments.none { it.type == DraftAttachmentType.Image && it.uri == uri }) {
                                    draftAttachments.add(DraftAttachment(DraftAttachmentType.Image, uri))
                                    startAttachmentUpload(uri)
                                }
                            }
                            attachmentsExpanded = false
                        },
                        onDismiss = { attachmentsExpanded = false },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AgentModelProfileScreen(
                    isVisible = showProfilePage,
                    onDismiss = { showProfilePage = false },
                    onNavigateToNas = onNavigateToNas,
                    onNavigateToHome = onNavigateToHome,
                    onLogout = onLogout,
                    modifier = Modifier.fillMaxSize()
                )

                AnimatedVisibility(
                    visible = showRechargePage,
                    enter = fadeIn(animationSpec = tween(durationMillis = 220)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 160)),
                ) {
                    BrainPowerBalancePage(
                        modifier = Modifier.fillMaxSize(),
                        onBack = { showRechargePage = false },
                        onNavigateToPackage = {
                            showRechargePackagePage = true
                        },
                    )
                }

                AnimatedVisibility(
                    visible = showRechargePackagePage,
                    enter = fadeIn(animationSpec = tween(durationMillis = 220)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 160)),
                ) {
                    RechargePackagePage(
                        modifier = Modifier.fillMaxSize(),
                        onBack = { showRechargePackagePage = false },
                    )
                }

                AnimatedVisibility(
                    visible = showNasNotSupportedDialog,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(150)),
                ) {
                    LaunchedEffect(Unit) {
                        val links = authRepository.getTaobaoLinks()
                        taobaoLinkUrl = links?.aiNpc?.takeIf { it.isNotBlank() }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x66000000))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { showNasNotSupportedDialog = false },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedVisibility(
                            visible = showNasNotSupportedDialog,
                            enter = scaleIn(initialScale = 0.85f, animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                            exit = scaleOut(targetScale = 0.85f, animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)),
                        ) {
                            NasNotSupportedDialog(
                                onDismiss = { showNasNotSupportedDialog = false },
                                onBuy = {
                                    showNasNotSupportedDialog = false
                                    val url = taobaoLinkUrl ?: ""
                                    uriHandler.openUri(url)
                                }
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showLowBalanceDialog,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(150)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x66000000))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                lowBalanceDismissedKey?.let { settings.putBoolean(it, true) }
                                showLowBalanceDialog = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedVisibility(
                            visible = showLowBalanceDialog,
                            enter = scaleIn(initialScale = 0.85f, animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                            exit = scaleOut(targetScale = 0.85f, animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)),
                        ) {
                            LowBalanceReminderDialog(
                                onDismiss = {
                                    lowBalanceDismissedKey?.let { settings.putBoolean(it, true) }
                                    showLowBalanceDialog = false
                                },
                                onRecharge = {
                                    lowBalanceDismissedKey?.let { settings.putBoolean(it, true) }
                                    showLowBalanceDialog = false
                                    showRechargePage = true
                                }
                            )
                        }
                    }
                }

                if (mediaAccessController.isRecording && !showProfilePage) {
                    AgentModelVoiceRecordingOverlay(
                        startedAtMillis = voiceRecordingStartedAtMillis ?: currentTimeMillis(),
                        onCancel = {
                            mediaAccessController.cancelVoiceInput()
                        },
                        onConfirm = {
                            isVoiceBusy = true
                            mediaAccessController.finishVoiceInput { result ->
                                isVoiceBusy = false
                                val trimmedText = result.transcribedText.trim()
                                if (trimmedText.isNotBlank()) {
                                    val currentText = inputText.text.trim()
                                    val newText = if (currentText.isBlank()) trimmedText else "$currentText $trimmedText"
                                    inputText = TextFieldValue(newText)
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

            }
        }

    } // end if previewState == null

    // ── 图片详情 (淡入 + 缩放) ──
    AnimatedVisibility(
        visible = selectedChatImageId != null && selectedChatImages.isNotEmpty(),
        enter = imageEnter,
        exit = imageExit,
    ) {
        val imgs = rememberedChatImages.value
        val imgId = rememberedChatImageId.value
        if (imgs.isNotEmpty() && imgId != null) {
            NasImageDetailScreen(
                images = imgs,
                initialImageId = imgId,
                targetCdi = currentCdi.orEmpty(),
                onBack = {
                    selectedChatImageId = null
                    selectedChatImages = emptyList()
                },
                onShare = {},
                onDownload = { currentImage ->
                    currentMessages
                        .filterIsInstance<ChatItem.Assistant>()
                        .flatMap { it.attachments }
                        .firstOrNull { it.blobRef == currentImage.path }
                        ?.let(::handleAttachmentDownload)
                },
                onDelete = {},
                isChatMode = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    // ── 音频详情 (从右侧滑入) ──
    AnimatedVisibility(
        visible = selectedChatAudio != null,
        enter = detailEnterSlide,
        exit = detailExitSlide,
    ) {
        val chatAudio = rememberedChatAudio.value
        if (chatAudio != null) {
            NasAudioDetailScreen(
                audio = chatAudio,
                targetCdi = currentCdi.orEmpty(),
                mediaController = mediaAccessController,
                onBack = {
                    mediaAccessController.stopAudioPlayback()
                    selectedChatAudio = null
                },
                onShare = {},
                onDownload = {
                    currentMessages
                        .filterIsInstance<ChatItem.Assistant>()
                        .flatMap { it.attachments }
                        .firstOrNull { it.blobRef == chatAudio.path }
                        ?.let(::handleAttachmentDownload)
                },
                onDelete = {},
                isChatMode = true,
                resolveAudioFile = {
                    val source = chatAudio.path
                    if (source.isBlank()) error("音频源为空")
                    val bytes = sdkSessionManager.fetchBlobBytes(source).getOrThrow()
                    platformSaveCacheFile(bytes, chatAudio.name)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    // ── 文档详情 (从右侧滑入) ──
    AnimatedVisibility(
        visible = selectedChatDocument != null,
        enter = detailEnterSlide,
        exit = detailExitSlide,
    ) {
        val chatDoc = rememberedChatDocument.value
        if (chatDoc != null) {
            NasDocumentDetailScreen(
                document = chatDoc,
                targetCdi = currentCdi.orEmpty(),
                onBack = { selectedChatDocument = null },
                onShare = {},
                onDownload = {
                    currentMessages
                        .filterIsInstance<ChatItem.Assistant>()
                        .flatMap { it.attachments }
                        .firstOrNull { it.blobRef == chatDoc.path }
                        ?.let(::handleAttachmentDownload)
                },
                onDelete = {},
                isChatMode = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    // ── 录音详情 (从右侧滑入) ──
    AnimatedVisibility(
        visible = selectedRecordingAudio != null,
        enter = detailEnterSlide,
        exit = detailExitSlide,
    ) {
        val recAudio = rememberedRecordingAudio.value
        if (recAudio != null) {
            NasAudioDetailScreen(
                audio = recAudio,
                targetCdi = currentCdi.orEmpty(),
                mediaController = mediaAccessController,
                onBack = {
                    mediaAccessController.stopAudioPlayback()
                    selectedRecordingAudio = null
                },
                onShare = {},
                onDownload = {
                    coroutineScope.launch {
                        toastMessage = "正在下载…"
                        runCatching {
                            val bytes = mediaAccessController.readUriToBytes(recAudio.path)
                                ?: error("读取录音失败")
                            platformSaveFile(bytes, recAudio.name, recAudio.type)
                        }.onSuccess {
                            toastMessage = "下载成功"
                        }.onFailure { e ->
                            toastMessage = "保存失败: ${e.message}"
                        }
                    }
                },
                onDelete = {},
                isChatMode = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    // 搜索页（侧边栏）- 保持隐藏
    if (showSearchPage) {
        AgentModelSearchScreen(
            conversations = conversations,
            onSelect = { showSearchPage = false },
            onCancel = { showSearchPage = false }
        )
    }

    // ── 轻提示 Toast 覆盖层 ──
    toastMessage?.let { msg ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = toastBottomPadding)
                .zIndex(100f),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xE6333333),
                shadowElevation = 4.dp,
            ) {
                Text(
                    text = msg,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }

    // 图片预览层 - 完全独立于 Drawer 和 Scaffold
    previewState?.let { currentPreviewState ->
        AgentModelImagePreview(
            previewState = currentPreviewState,
            onDismiss = { previewState = null }
        )
    }
    } // Box
    } // DesignScaleProvider
}

private const val LOW_BALANCE_DISMISSED_KEY = "low_balance_reminder_dismissed"

@Composable
private fun LowBalanceReminderDialog(
    onDismiss: () -> Unit,
    onRecharge: () -> Unit,
) {
    val ds = LocalDesignScale.current
    Surface(
        shape = RoundedCornerShape(ds.sm(20.dp)),
        color = Color.White,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { /* consume click */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ds.sw(20.dp), vertical = ds.sh(24.dp)),
        ) {
            Text(
                text = "脑力值余额不足",
                fontSize = ds.sp(20f),
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2535),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(ds.sh(4.dp)))

            Text(
                text = "脑力值当前已用尽，请尽快去充值，点击下方按钮充值或者去〈个人中心〉充值。",
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Normal,
                color = Color(0xFF717580),
            )

            Spacer(modifier = Modifier.height(ds.sh(24.dp)))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ds.sw(11.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(ds.sm(100.dp)))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onDismiss() }
                        .padding(vertical = ds.sh(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "取消",
                        fontSize = ds.sp(16f),
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF1F2535),
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(ds.sm(100.dp)))
                        .background(Color(0xFF1F2535))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onRecharge() }
                        .padding(vertical = ds.sh(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "去充值",
                        fontSize = ds.sp(16f),
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun NasNotSupportedDialog(
    onDismiss: () -> Unit,
    onBuy: () -> Unit,
) {
    val ds = LocalDesignScale.current
    Surface(
        shape = RoundedCornerShape(ds.sm(20.dp)),
        color = Color.White,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { /* consume click */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ds.sw(20.dp), vertical = ds.sh(24.dp)),
        ) {
            Text(
                text = "NAS 功能提示",
                fontSize = ds.sp(20f),
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2535),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(ds.sh(4.dp)))

            Text(
                text = "只有 AI NPC 支持 NAS 功能",
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Normal,
                color = Color(0xFF717580),
            )

            Spacer(modifier = Modifier.height(ds.sh(24.dp)))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ds.sw(11.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(ds.sm(100.dp)))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onDismiss() }
                        .padding(vertical = ds.sh(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "知道了",
                        fontSize = ds.sp(16f),
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF1F2535),
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(ds.sm(100.dp)))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onBuy() }
                        .padding(vertical = ds.sh(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "去购买",
                        fontSize = ds.sp(16f),
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFFE84026),
                    )
                }
            }
        }
    }
}
