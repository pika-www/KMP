package com.cephalon.lucyApp.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import com.cephalon.lucyApp.components.DesignScaleProvider
import com.cephalon.lucyApp.components.LocalDesignScale
import com.cephalon.lucyApp.media.rememberPlatformMediaAccessController
import org.jetbrains.compose.resources.painterResource
import com.cephalon.lucyApp.time.currentTimeMillis
import com.cephalon.lucyApp.media.platformSaveFile
import kotlinx.coroutines.launch
import com.cephalon.lucyApp.screens.agentmodel.ChatHistoryCache
import com.cephalon.lucyApp.screens.agentmodel.ChatItem
import com.cephalon.lucyApp.screens.agentmodel.DraftAttachment
import com.cephalon.lucyApp.screens.agentmodel.DraftAttachmentType
import com.cephalon.lucyApp.screens.agentmodel.ImagePreviewState
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
import com.cephalon.lucyApp.screens.agentmodel.asPickedFile
import com.cephalon.lucyApp.screens.nas.NasSendToChatStore
import com.cephalon.lucyApp.screens.nas.NasSendFileType
import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.sdk.SdkSessionManager
import org.koin.compose.koinInject

private const val STREAMING_PLACEHOLDER_TEXT = "思考中..."

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

    fun upsertStreamingAssistantMessageInConversation(
        conversationId: String?,
        messageId: String,
        text: String,
        attachments: List<MediaAttachment> = emptyList(),
        timestamp: Long? = null,
    ) {
        // 用 messageId 反查这条流式消息属于哪台设备：
        // - 找得到、且是当前设备 → 内存路径，snapshotFlow 顺手保存；
        // - 找得到、且**不是**当前设备 → 直接落盘到那个设备的存档（用户已经切走了）；
        // - 找不到（兜底，比如重启后回放旧 streaming）→ 当成当前设备处理，至少不丢现场。
        val targetCdi = messageIdToCdi[messageId]
        mutateConversationOnCdi(conversationId, targetCdi) { conversation ->
            val updatedMessages = conversation.messages.toMutableList()
            // ── 查找流式消息的目标位置（三级优先） ──
            // 1. messageId 匹配 + timestamp==null → 正在流式输出的消息
            //    （media event 插入的消息一定有 timestamp，不会被误命中）
            var targetIndex = updatedMessages.indexOfLast {
                it is ChatItem.Assistant && it.messageId == messageId && it.timestamp == null
            }
            // 2. 尚未绑定 messageId 的原始占位符
            if (targetIndex < 0) {
                targetIndex = updatedMessages.indexOfLast {
                    it is ChatItem.Assistant &&
                        it.messageId == null &&
                        it.text == STREAMING_PLACEHOLDER_TEXT
                }
                if (targetIndex >= 0) {
                    println("[Streaming] upsert fallback: 用孤立占位符 idx=$targetIndex 替代")
                }
            }
            // 3. 兜底：messageId 匹配（含已有 timestamp 的消息）
            if (targetIndex < 0) {
                targetIndex = updatedMessages.indexOfLast {
                    it is ChatItem.Assistant && it.messageId == messageId
                }
                if (targetIndex >= 0) {
                    println("[Streaming] upsert fallback: 按 messageId 兜底 idx=$targetIndex")
                }
            }
            println("[Streaming] upsert convId=$conversationId msgId=$messageId targetCdi=$targetCdi targetIdx=$targetIndex textLen=${text.length} totalMsgs=${updatedMessages.size}")
            val existing = updatedMessages.getOrNull(targetIndex) as? ChatItem.Assistant
            // 合并而非替换：新附件与已有附件按 blobRef 去重合并，防止分批到达丢文件
            val mergedAttachments = if (attachments.isEmpty()) {
                existing?.attachments ?: emptyList()
            } else {
                val existingAtts = existing?.attachments.orEmpty()
                val existingRefs = existingAtts.map { it.blobRef }.toSet()
                existingAtts + attachments.filter { it.blobRef !in existingRefs }
            }
            val mergedTimestamp = timestamp ?: existing?.timestamp
            if (targetIndex >= 0) {
                updatedMessages[targetIndex] = ChatItem.Assistant(text, messageId, mergedAttachments, mergedTimestamp)
            } else {
                updatedMessages.add(ChatItem.Assistant(text, messageId, mergedAttachments, mergedTimestamp))
            }
            conversation.copy(
                messages = updatedMessages,
                lastActiveAt = currentTimeMillis()
            )
        }
    }

    fun removeAssistantPlaceholderInConversation(conversationId: String?, messageId: String? = null) {
        // 同样按 messageId 路由到原设备；messageId 为 null 时退化为当前设备（与历史行为一致）。
        val targetCdi = messageId?.let { messageIdToCdi[it] }
        mutateConversationOnCdi(conversationId, targetCdi) { conversation ->
            val updatedMessages = conversation.messages.toMutableList()
            var placeholderIndex =
                updatedMessages.indexOfLast {
                    it is ChatItem.Assistant &&
                        it.text == STREAMING_PLACEHOLDER_TEXT &&
                        (messageId == null || it.messageId == messageId)
                }
            // Fallback: 如果按 messageId 找不到占位符，也清理孤立占位符 (messageId==null)
            if (placeholderIndex < 0 && messageId != null) {
                placeholderIndex = updatedMessages.indexOfLast {
                    it is ChatItem.Assistant &&
                        it.text == STREAMING_PLACEHOLDER_TEXT &&
                        it.messageId == null
                }
            }
            if (placeholderIndex >= 0) {
                updatedMessages.removeAt(placeholderIndex)
            }
            conversation.copy(
                messages = updatedMessages,
                lastActiveAt = currentTimeMillis()
            )
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

    val messageListState = rememberLazyListState()

    // 用户是否已经在（接近）底部：允许约 32px 容差，避免浮点/间距导致误判。
    // 在底部 → streaming 循环会继续跟随；不在底部 → 用户自由滚动，我们不再强拉回底部。
    val isNearBottom by remember(messageListState) {
        derivedStateOf {
            val info = messageListState.layoutInfo
            val visible = info.visibleItemsInfo
            if (visible.isEmpty()) {
                true
            } else {
                val lastVisible = visible.last()
                val lastIndex = info.totalItemsCount - 1
                lastVisible.index >= lastIndex &&
                    lastVisible.offset + lastVisible.size <= info.viewportEndOffset + 32
            }
        }
    }

    // 发送 / 新增消息时动画滚动到底部
    LaunchedEffect(currentMessages.size) {
        if (currentMessages.isNotEmpty()) {
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
            if (!isNearBottom) continue
            if (messageListState.isScrollInProgress) continue
            val total = messageListState.layoutInfo.totalItemsCount
            if (total > 0) {
                messageListState.scrollToItem(total - 1)
            }
        }
    }

    val mediaAccessController = rememberPlatformMediaAccessController { message ->
        logs.add(0, message)
    }

    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    var attachmentsExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { inputText.text }
            .drop(1)
            .collect { if (attachmentsExpanded) attachmentsExpanded = false }
    }
    var previewState by remember { mutableStateOf<ImagePreviewState?>(null) }
    var showProfilePage by remember { mutableStateOf(false) }
    var showSearchPage by remember { mutableStateOf(false) }
    // 0 = 欢迎页, 1 = 技能卡片页, 2 = 已关闭
    var emptyViewState by remember { mutableStateOf(0) }

    val draftAttachments = remember { mutableStateListOf<DraftAttachment>() }
    var lastPickedImagesSize by remember { mutableStateOf(0) }
    var lastPickedFilesSize by remember { mutableStateOf(0) }
    var isVoiceBusy by remember { mutableStateOf(false) }
    var voiceRecordingStartedAtMillis by remember { mutableStateOf<Long?>(null) }
    val activeStreamingRequests = remember { mutableStateMapOf<String, String>() }
    val attachmentUploadStates = remember { mutableStateMapOf<String, AttachmentUploadState>() }

    // ── 轻提示 Toast ──
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // ── 监听 NAS "发送脑花" ──
    LaunchedEffect(Unit) {
        snapshotFlow { NasSendToChatStore.pendingItems.size }
            .collect { size ->
                if (size > 0) {
                    val items = NasSendToChatStore.consume()
                    items.forEach { nasItem ->
                        val attType = when (nasItem.fileType) {
                            NasSendFileType.Image -> DraftAttachmentType.Image
                            NasSendFileType.Audio -> DraftAttachmentType.File
                            NasSendFileType.Document -> DraftAttachmentType.File
                        }
                        val uri = if (nasItem.fileType == NasSendFileType.Image) {
                            nasItem.thumbnailBlobRef
                        } else {
                            "nas-file://${nasItem.fileId}"
                        }
                        draftAttachments.add(
                            DraftAttachment(
                                type = attType,
                                uri = uri,
                                displayName = nasItem.fileName,
                                nasFileId = nasItem.fileId,
                            )
                        )
                    }
                }
            }
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
        coroutineScope.launch {
            val connectResult = sdkSessionManager.ensureConnectedIfTokenValid()
            if (connectResult.isFailure) {
                attachmentUploadStates[uri] = AttachmentUploadState.Failed("连接失败")
                return@launch
            }
            val fileBytes = mediaAccessController.readUriToBytes(uri)
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

    // 监听异步文件/媒体推送（无 source_message_id 或已完成请求的后续 final），
    // 按 (sourceMessageId, timestamp) 去重，按 timestamp 插入正确位置。
    LaunchedEffect(Unit) {
        sdkSessionManager.incomingMediaEvents.collect { event ->
            println("[MediaEvent] 收到异步媒体推送: type=${event.eventType}, text=${event.text?.take(50)}, attachments=${event.attachments.size}, sourceMessageId=${event.sourceMessageId}, timestamp=${event.timestamp}")
            val targetConvId = selectedConversationId
            val chatText = event.text ?: ""
            val chatAttachments = event.attachments
            if (chatAttachments.isEmpty() && chatText.isBlank()) return@collect

            val srcMsgId = event.sourceMessageId
            val ts = event.timestamp

            val conv = conversations.firstOrNull { it.id == targetConvId }
            val existingMessages = conv?.messages.orEmpty()
            val existingAssistants = existingMessages.filterIsInstance<ChatItem.Assistant>()

            // ── 去重：(sourceMessageId, timestamp) 为唯一标识 ──
            // 1. 有 sourceMessageId → (sourceMessageId, timestamp) 精确匹配，或 (sourceMessageId + text) 兜底
            // 2. 无 sourceMessageId → text 完全匹配
            val duplicate: ChatItem.Assistant? = if (srcMsgId != null) {
                existingAssistants.firstOrNull { it.messageId == srcMsgId && ts != null && it.timestamp == ts }
                    ?: (if (chatText.isNotBlank()) existingAssistants.firstOrNull { it.messageId == srcMsgId && it.text == chatText } else null)
            } else {
                if (chatText.isNotBlank()) {
                    existingAssistants.firstOrNull { it.text == chatText }
                } else null
            }

            if (duplicate != null) {
                // 重复消息：仅当有新附件时合并，否则跳过
                if (chatAttachments.isNotEmpty()) {
                    val existingRefs = duplicate.attachments.map { it.blobRef }.toSet()
                    val newAtts = chatAttachments.filter { it.blobRef !in existingRefs }
                    if (newAtts.isNotEmpty()) {
                        println("[MediaEvent] 去重命中，合并 ${newAtts.size} 个新附件")
                        updateConversation(targetConvId) { conversation ->
                            val msgs = conversation.messages.toMutableList()
                            val idx = msgs.indexOf(duplicate)
                            if (idx >= 0) {
                                msgs[idx] = duplicate.copy(attachments = duplicate.attachments + newAtts)
                            }
                            conversation.copy(messages = msgs, lastActiveAt = currentTimeMillis())
                        }
                    } else {
                        println("[MediaEvent] 去重命中，无新附件，跳过")
                    }
                } else {
                    println("[MediaEvent] 去重命中，跳过: text=${chatText.take(50)}")
                }
                return@collect
            }

            // ── 新消息：按 timestamp 全局排序插入 ──
            // 把没有 timestamp 的 Assistant（含流式占位符）视为 Long.MAX_VALUE，
            // 确保有 timestamp 的异步消息总是插到占位符/流式消息之前。
            val newMsg = ChatItem.Assistant(
                text = chatText,
                messageId = srcMsgId,
                attachments = chatAttachments,
                timestamp = ts,
            )
            updateConversation(targetConvId) { conversation ->
                val msgs = conversation.messages.toMutableList()
                if (ts != null) {
                    val insertBeforeIdx = msgs.indexOfFirst { m ->
                        m is ChatItem.Assistant && (m.timestamp ?: Long.MAX_VALUE) > ts
                    }
                    if (insertBeforeIdx >= 0) {
                        println("[MediaEvent] 按 timestamp 插入到 idx=$insertBeforeIdx")
                        msgs.add(insertBeforeIdx, newMsg)
                    } else {
                        println("[MediaEvent] 追加到末尾")
                        msgs.add(newMsg)
                    }
                } else {
                    // 无 timestamp → 追加到末尾
                    println("[MediaEvent] 无 timestamp，追加到末尾")
                    msgs.add(newMsg)
                }
                conversation.copy(messages = msgs, lastActiveAt = currentTimeMillis())
            }
        }
    }

    activeStreamingRequests.forEach { (msgId, convId) ->
        key(msgId) {
            LaunchedEffect(msgId) {
                println("[Streaming] LaunchedEffect 启动: msgId=$msgId, convId=$convId")
                var streamingStarted = false
                var lastText = ""
                var iterCount = 0

                while (isActive) {
                    val state = sdkSessionManager.replyStateMap.value[msgId]
                    val text = state?.text ?: ""
                    val streaming = state?.streaming ?: false

                    if (iterCount < 5 || (iterCount % 20 == 0)) {
                        println("[Streaming] poll #$iterCount msgId=$msgId: stateExists=${state != null}, streaming=$streaming, textLen=${text.length}")
                    }
                    iterCount++

                    if (streaming && !streamingStarted) {
                        println("AgentModel: 流式输出开始 msgId=$msgId, convId=$convId")
                    }
                    if (streaming) streamingStarted = true

                    // 直接渲染 SDK 返回的文本，不做打字机效果
                    if (text.isNotBlank() && text != lastText) {
                        lastText = text
                        upsertStreamingAssistantMessageInConversation(convId, msgId, text, timestamp = state?.timestamp)
                    }

                    // 结束条件：streaming 变为 false 且（曾经开始过 或 已有最终文本 或 有错误 或 已有附件）
                    val errorText = state?.errorText
                    val finalAttachments = state?.attachments ?: emptyList()
                    val finished = !streaming && (streamingStarted || text.isNotBlank() || errorText != null || finalAttachments.isNotEmpty())
                    if (finished) {
                        if ((text.isNotBlank() && lastText != text) || finalAttachments.isNotEmpty()) {
                            upsertStreamingAssistantMessageInConversation(convId, msgId, text, finalAttachments, timestamp = state?.timestamp)
                        }
                        // 如果有错误，追加错误提示到对话中
                        if (errorText != null) {
                            removeAssistantPlaceholderInConversation(convId, msgId)
                            // 错误消息也得走和占位符同一台设备的存档：用户可能已切走。
                            appendMessageToConversationOnCdi(
                                convId,
                                targetCdi = messageIdToCdi[msgId],
                                message = ChatItem.Error(errorText),
                            )
                            println("AgentModel: 流式输出错误 msgId=$msgId, convId=$convId, error=$errorText")
                        } else {
                            removeAssistantPlaceholderInConversation(convId, msgId)
                        }
                        println("AgentModel: 流式输出结束 msgId=$msgId, convId=$convId, textLen=${text.length}, attachments=${finalAttachments.size}, wasStreaming=$streamingStarted")
                        activeStreamingRequests.remove(msgId)
                        // 释放 messageId → cdi 的映射，避免长会话累积。
                        messageIdToCdi.remove(msgId)
                        break
                    }

                    delay(50L)
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
                .takeLast(size - lastPickedImagesSize)
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
                .takeLast(size - lastPickedFilesSize)
                .forEach { pickedFile ->
                    if (
                        pickedFile.uri.isNotBlank() &&
                        draftAttachments.none { it.type == DraftAttachmentType.File && it.uri == pickedFile.uri }
                    ) {
                        draftAttachments.add(
                            DraftAttachment(
                                type = DraftAttachmentType.File,
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
            // ── 上一条消息仍在回复中，禁止连续发送 ──
            if (activeStreamingRequests.isNotEmpty()) {
                toastMessage = "上一条消息正在回复中，请稍候"
                return@Unit
            }

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

            // ── 特殊指令：脑花 功能/能力 → 直接展示技能卡片，不走 publishTextToNpc ──
            if (attachments.isEmpty() && isBrainBoxCapabilityQuery(text)) {
                appendMessageToConversation(targetConversationId, ChatItem.User(text))
                appendMessageToConversation(targetConversationId, ChatItem.SkillSuggestions)
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
                ChatItem.Assistant(STREAMING_PLACEHOLDER_TEXT)
            )
            coroutineScope.launch {
                println("[Chat] ensureConnectedIfTokenValid 开始...")
                val connectResult = sdkSessionManager.ensureConnectedIfTokenValid()
                println("[Chat] ensureConnectedIfTokenValid 结果: isSuccess=${connectResult.isSuccess}, error=${connectResult.exceptionOrNull()?.message}")
                if (connectResult.isFailure) {
                    // 路由到点击发送时所在的设备：用户此刻可能已切到别的设备。
                    mutateConversationOnCdi(targetConversationId, sendingCdi) { conv ->
                        val msgs = conv.messages.toMutableList()
                        val idx = msgs.indexOfLast {
                            it is ChatItem.Assistant && it.text == STREAMING_PLACEHOLDER_TEXT
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
                            it is ChatItem.Assistant && it.text == STREAMING_PLACEHOLDER_TEXT
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
                                    it is ChatItem.Assistant && it.text == STREAMING_PLACEHOLDER_TEXT
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
                                    it is ChatItem.Assistant && it.text == STREAMING_PLACEHOLDER_TEXT
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
                                is ChatItem.UserAttachments -> msgs[userIdx] = old.copy(messageId = messageId)
                                else -> {}
                            }
                        }

                        // 2) 给 Assistant 占位符绑上同一个 messageId（作为 source_message_id），
                        //    流式回调通过它在本地找到占位符并 upsert 成真实回复。
                        val assistantIdx = msgs.indexOfLast {
                            it is ChatItem.Assistant &&
                                    it.messageId == null &&
                                    it.text == STREAMING_PLACEHOLDER_TEXT
                        }
                        println("[Chat] 回填 messageId: userIdx=$userIdx, assistantIdx=$assistantIdx, totalMsgs=${msgs.size}")
                        if (assistantIdx >= 0) {
                            msgs[assistantIdx] = ChatItem.Assistant(STREAMING_PLACEHOLDER_TEXT, messageId)
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
                            it is ChatItem.Assistant && it.text == STREAMING_PLACEHOLDER_TEXT
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
            attachments.forEach { att ->
                if (att.nasFileId == null) {
                    attachmentUploadStates.remove(att.uri)
                }
            }
        }
    }

    DesignScaleProvider {
    Box(modifier = Modifier.fillMaxSize()) {
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
                                .padding(horizontal = ds.sw(20.dp))
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(pass = PointerEventPass.Initial)
                                        val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                        if (up != null) { focusManager.clearFocus() }
                                    }
                                },
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
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(pass = PointerEventPass.Initial)
                                    val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                    if (up != null) {
                                        focusManager.clearFocus()
                                        attachmentsExpanded = false
                                    }
                                }
                            }
                        ) {
                            AgentModelMessageList(
                                messages = currentMessages,
                                playingRecordingId = mediaAccessController.playingRecordingId,
                                onToggleRecordingPlayback = { mediaAccessController.toggleRecordingPlayback(it) },
                                onImageClick = { previewState = it },
                                onFileClick = { mediaAccessController.openFilePreview(it) },
                                onTapMessageArea = {
                                    focusManager.clearFocus()
                                    attachmentsExpanded = false
                                },
                                onSkillClick = { skillText ->
                                    inputText = TextFieldValue(skillText)
                                    sendMessage()
                                },
                                onAttachmentClick = { attachment ->
                                    handleAttachmentDownload(attachment)
                                },
                                onCopySuccess = { toastMessage = "复制成功" },
                                streamingStatusText = streamingStatusText,
                                listState = messageListState,
                                modifier = Modifier
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
                        onFileClick = { mediaAccessController.openFilePreview(it.asPickedFile()) },
                        playingRecordingId = mediaAccessController.playingRecordingId,
                        onToggleRecordingPlayback = { recordingPath ->
                            mediaAccessController.toggleRecordingPlayback(
                                com.cephalon.lucyApp.media.AudioRecording(
                                    id = recordingPath,
                                    name = recordingPath.substringAfterLast('/'),
                                    path = recordingPath
                                )
                            )
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
                        onSuggestionClick = { appendMessageToConversation(selectedConversationId, ChatItem.User(it)) },
                        uploadStates = attachmentUploadStates,
                    )

                
                }

                if (attachmentsExpanded) {
                    AgentModelAttachmentPanel(
                        recentImages = mediaAccessController.recentImages,
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
                .padding(bottom = 120.dp),
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