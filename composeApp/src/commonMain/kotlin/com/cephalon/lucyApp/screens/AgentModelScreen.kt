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
import com.cephalon.lucyApp.screens.agentmodel.ChatItem
import com.cephalon.lucyApp.screens.agentmodel.DraftAttachment
import com.cephalon.lucyApp.screens.agentmodel.DraftAttachmentType
import com.cephalon.lucyApp.screens.agentmodel.ImagePreviewState
import com.cephalon.lucyApp.screens.agentmodel.ImageUploadState
import com.cephalon.lucyApp.sdk.MediaAttachment
import com.cephalon.lucyApp.screens.agentmodel.uriDisplayName
import com.cephalon.lucyApp.screens.agentmodel.ConversationItem
import com.cephalon.lucyApp.screens.agentmodel.displayName
import com.cephalon.lucyApp.screens.agentmodel.AgentModelAttachmentPanel
import com.cephalon.lucyApp.screens.agentmodel.AgentModelSearchScreen
import com.cephalon.lucyApp.screens.agentmodel.AgentModelComposer
import com.cephalon.lucyApp.screens.agentmodel.AgentModelImagePreview
import com.cephalon.lucyApp.screens.agentmodel.AgentModelMessageList
import com.cephalon.lucyApp.screens.agentmodel.AgentModelProfileScreen
import com.cephalon.lucyApp.screens.agentmodel.AgentModelTopBar
import com.cephalon.lucyApp.screens.agentmodel.AgentModelVoiceRecordingOverlay
import com.cephalon.lucyApp.screens.agentmodel.asPickedFile
import com.cephalon.lucyApp.api.AuthRepository
import com.cephalon.lucyApp.sdk.SdkSessionManager
import org.koin.compose.koinInject

private const val STREAMING_PLACEHOLDER_TEXT = "思考中..."

private fun inferImageContentType(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "heic", "heif" -> "image/heic"
        "bmp" -> "image/bmp"
        "svg" -> "image/svg+xml"
        else -> "image/jpeg"
    }
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

    val logs = remember {
        mutableStateListOf(
            "点击下面的按钮测试系统能力。",
            "相机会先申请系统权限，图库和文件会通过系统选择器打开。",
            "录音会先申请麦克风权限，结束后可在下方播放。"
        )
    }

    // 纯内存对话列表，不做本地持久化
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

    fun updateConversation(
        conversationId: String?,
        persist: Boolean = false,
        transform: (ConversationItem) -> ConversationItem,
    ) {
        val targetId = conversationId ?: return
        val index = conversations.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            conversations[index] = transform(conversations[index])
            // no-op: 不再持久化
        }
    }

    fun updateSelectedConversation(transform: (ConversationItem) -> ConversationItem) {
        updateConversation(selectedConversationId, transform = transform)
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
    ) {
        updateConversation(conversationId, persist = false) { conversation ->
            val updatedMessages = conversation.messages.toMutableList()
            var targetIndex = updatedMessages.indexOfLast {
                it is ChatItem.Assistant && it.messageId == messageId
            }
            // Fallback: 如果找不到已绑定 messageId 的消息，尝试替换孤立占位符
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
            println("[Streaming] upsert convId=$conversationId msgId=$messageId targetIdx=$targetIndex textLen=${text.length} totalMsgs=${updatedMessages.size}")
            val existing = updatedMessages.getOrNull(targetIndex) as? ChatItem.Assistant
            val mergedAttachments = attachments.ifEmpty { existing?.attachments ?: emptyList() }
            if (targetIndex >= 0) {
                updatedMessages[targetIndex] = ChatItem.Assistant(text, messageId, mergedAttachments)
            } else {
                updatedMessages.add(ChatItem.Assistant(text, messageId, mergedAttachments))
            }
            conversation.copy(
                messages = updatedMessages,
                lastActiveAt = currentTimeMillis()
            )
        }
    }

    fun removeAssistantPlaceholderInConversation(conversationId: String?, messageId: String? = null) {
        updateConversation(conversationId) { conversation ->
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

    fun appendMessageToConversation(conversationId: String?, message: ChatItem) {
        updateConversation(conversationId) { conversation ->
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

    var inputText by remember { mutableStateOf("") }
    var attachmentsExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { inputText }
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
    val imageUploadStates = remember { mutableStateMapOf<String, ImageUploadState>() }

    // ── 轻提示 Toast ──
    var toastMessage by remember { mutableStateOf<String?>(null) }

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

    fun startImageUpload(uri: String, displayName: String? = null) {
        if (imageUploadStates[uri] is ImageUploadState.Success) return
        imageUploadStates[uri] = ImageUploadState.Uploading
        coroutineScope.launch {
            val connectResult = sdkSessionManager.ensureConnectedIfTokenValid()
            if (connectResult.isFailure) {
                imageUploadStates[uri] = ImageUploadState.Failed("连接失败")
                return@launch
            }
            val imageBytes = mediaAccessController.readUriToBytes(uri)
            if (imageBytes == null) {
                imageUploadStates[uri] = ImageUploadState.Failed("读取图片失败")
                return@launch
            }
            val fileName = displayName?.trim()?.takeIf { it.isNotBlank() }
                ?: uriDisplayName(uri).ifBlank { "image.jpg" }
            val uploadResult = sdkSessionManager.uploadImage(imageBytes, fileName)
            uploadResult.onSuccess { putResult ->
                imageUploadStates[uri] = ImageUploadState.Success(
                    blobRef = putResult.blobRef,
                    contentType = inferImageContentType(fileName),
                    size = imageBytes.size.toLong(),
                    fileName = fileName,
                )
            }
            uploadResult.onFailure { error ->
                imageUploadStates[uri] = ImageUploadState.Failed(error.message ?: "上传失败")
            }
        }
    }

    LaunchedEffect(Unit) {
        sdkSessionManager.connectIfTokenValid()
    }

    // 监听异步文件/媒体推送（无 source_message_id 的消息），追加到当前对话底部
    LaunchedEffect(Unit) {
        sdkSessionManager.incomingMediaEvents.collect { event ->
            println("[MediaEvent] 收到异步媒体推送: type=${event.eventType}, text=${event.text?.take(50)}, attachments=${event.attachments.size}")
            val targetConvId = selectedConversationId
            val chatText = event.text ?: ""
            val chatAttachments = event.attachments
            if (chatAttachments.isNotEmpty() || chatText.isNotBlank()) {
                appendMessageToConversation(
                    targetConvId,
                    ChatItem.Assistant(
                        text = chatText,
                        attachments = chatAttachments,
                    )
                )
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
                        upsertStreamingAssistantMessageInConversation(convId, msgId, text)
                    }

                    // 结束条件：streaming 变为 false 且（曾经开始过 或 已有最终文本 或 有错误 或 已有附件）
                    val errorText = state?.errorText
                    val finalAttachments = state?.attachments ?: emptyList()
                    val finished = !streaming && (streamingStarted || text.isNotBlank() || errorText != null || finalAttachments.isNotEmpty())
                    if (finished) {
                        if ((text.isNotBlank() && lastText != text) || finalAttachments.isNotEmpty()) {
                            upsertStreamingAssistantMessageInConversation(convId, msgId, text, finalAttachments)
                        }
                        // 如果有错误，追加错误提示到对话中
                        if (errorText != null) {
                            removeAssistantPlaceholderInConversation(convId, msgId)
                            appendMessageToConversation(convId, ChatItem.Error(errorText))
                            println("AgentModel: 流式输出错误 msgId=$msgId, convId=$convId, error=$errorText")
                        } else {
                            removeAssistantPlaceholderInConversation(convId, msgId)
                        }
                        println("AgentModel: 流式输出结束 msgId=$msgId, convId=$convId, textLen=${text.length}, attachments=${finalAttachments.size}, wasStreaming=$streamingStarted")
                        activeStreamingRequests.remove(msgId)
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
                        startImageUpload(uri)
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
                    }
                }
            if (size > lastPickedFilesSize) {
                attachmentsExpanded = false
            }
        }
        lastPickedFilesSize = size
    }

    val sendMessage = Unit@{
        val text = inputText.trim()
        val attachments = draftAttachments.toList()

        if (text.isNotEmpty() || attachments.isNotEmpty()) {
            val targetConversationId = selectedConversationId

            // ── 特殊指令：脑花 功能/能力 → 直接展示技能卡片，不走 publishTextToNpc ──
            if (attachments.isEmpty() && isBrainBoxCapabilityQuery(text)) {
                appendMessageToConversation(targetConversationId, ChatItem.User(text))
                appendMessageToConversation(targetConversationId, ChatItem.SkillSuggestions)
                inputText = ""
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

            val imageAttachments = attachments.filter { it.type == DraftAttachmentType.Image }
            val outgoingText =
                text.ifBlank {
                    if (imageAttachments.isNotEmpty()) "" else ""
                }
            println("[Chat] 发送消息: text=\"$outgoingText\", initialTargetCdi=$initialTargetCdi, snapshotOnlineCdis=$onlineDeviceCdis, imageCount=${imageAttachments.size}")
            appendMessageToConversation(
                targetConversationId,
                ChatItem.Assistant(STREAMING_PLACEHOLDER_TEXT)
            )
            coroutineScope.launch {
                println("[Chat] ensureConnectedIfTokenValid 开始...")
                val connectResult = sdkSessionManager.ensureConnectedIfTokenValid()
                println("[Chat] ensureConnectedIfTokenValid 结果: isSuccess=${connectResult.isSuccess}, error=${connectResult.exceptionOrNull()?.message}")
                if (connectResult.isFailure) {
                    removeAssistantPlaceholderInConversation(targetConversationId)
                    appendMessageToConversation(
                        targetConversationId,
                        ChatItem.System("连接失败：${connectResult.exceptionOrNull()?.message ?: "unknown"}")
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
                    removeAssistantPlaceholderInConversation(targetConversationId)
                    appendMessageToConversation(
                        targetConversationId,
                        ChatItem.System("没有可用的设备，请等待设备上线后重试")
                    )
                    return@launch
                }
                println("[Chat] 解析到 targetCdi=$targetCdi")

                val sendResult = if (imageAttachments.isNotEmpty()) {
                    // 收集所有图片的 MediaItem
                    val mediaItems = mutableListOf<SdkSessionManager.MediaItem>()
                    for (att in imageAttachments) {
                        // 等待预上传完成
                        var uploadState = imageUploadStates[att.uri]
                        while (uploadState is ImageUploadState.Uploading) {
                            delay(100)
                            uploadState = imageUploadStates[att.uri]
                        }

                        if (uploadState is ImageUploadState.Success) {
                            mediaItems.add(SdkSessionManager.MediaItem(
                                blobRef = uploadState.blobRef,
                                contentType = uploadState.contentType,
                                size = uploadState.size,
                                fileName = uploadState.fileName,
                            ))
                        } else {
                            // 预上传失败或未开始，回退到发送时上传
                            val imageBytes = mediaAccessController.readUriToBytes(att.uri)
                            if (imageBytes == null) {
                                removeAssistantPlaceholderInConversation(targetConversationId)
                                appendMessageToConversation(
                                    targetConversationId,
                                    ChatItem.System("读取图片失败，无法发送")
                                )
                                return@launch
                            }
                            val fileName = att.displayName()
                            val uploadResult = sdkSessionManager.uploadImage(imageBytes, fileName)
                            if (uploadResult.isFailure) {
                                removeAssistantPlaceholderInConversation(targetConversationId)
                                appendMessageToConversation(
                                    targetConversationId,
                                    ChatItem.System("图片上传失败：${uploadResult.exceptionOrNull()?.message ?: "unknown"}")
                                )
                                return@launch
                            }
                            val putResult = uploadResult.getOrThrow()
                            mediaItems.add(SdkSessionManager.MediaItem(
                                blobRef = putResult.blobRef,
                                contentType = inferImageContentType(fileName),
                                size = imageBytes.size.toLong(),
                                fileName = fileName,
                            ))
                        }
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
                    println("[Chat] 发送成功: messageId=$messageId, targetCdi=$targetCdi, convId=$targetConversationId")
                    updateConversation(targetConversationId) { conv ->
                        val msgs = conv.messages.toMutableList()
                        val idx = msgs.indexOfLast {
                            it is ChatItem.Assistant &&
                                    it.messageId == null &&
                                    it.text == STREAMING_PLACEHOLDER_TEXT
                        }
                        println("[Chat] 查找占位符: idx=$idx, totalMsgs=${msgs.size}")
                        if (idx >= 0) {
                            msgs[idx] = ChatItem.Assistant(STREAMING_PLACEHOLDER_TEXT, messageId)
                        }
                        conv.copy(messages = msgs)
                    }
                    if (targetConversationId != null) {
                        activeStreamingRequests[messageId] = targetConversationId
                        println("[Chat] activeStreamingRequests 已添加: msgId=$messageId → convId=$targetConversationId, size=${activeStreamingRequests.size}")
                    }
                }

                sendResult.onFailure { error ->
                    println("[Chat] 发送失败: targetCdi=$targetCdi, error=${error.message}")
                    removeAssistantPlaceholderInConversation(targetConversationId)
                    appendMessageToConversation(
                        targetConversationId,
                        ChatItem.System("发送失败：${error.message ?: "unknown"}")
                    )
                }
            }

            inputText = ""
            attachmentsExpanded = false
            attachments.forEach { att ->
                if (att.type == DraftAttachmentType.Image) {
                    imageUploadStates.remove(att.uri)
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
                    .pointerInput(attachmentsExpanded) {
                        if (attachmentsExpanded) return@pointerInput
                        awaitEachGesture {
                            awaitFirstDown(pass = PointerEventPass.Initial)
                            val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                            if (up != null) {
                                focusManager.clearFocus()
                            }
                        }
                    }
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
                            }
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
                                onImageClick = { previewState = it },
                                onFileClick = { mediaAccessController.openFilePreview(it) },
                                onTapMessageArea = {
                                    focusManager.clearFocus()
                                    attachmentsExpanded = false
                                },
                                onSkillClick = { skillText ->
                                    inputText = skillText
                                    sendMessage()
                                },
                                onAttachmentClick = { attachment ->
                                    handleAttachmentDownload(attachment)
                                },
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
                            if (att.type == DraftAttachmentType.Image) {
                                imageUploadStates.remove(att.uri)
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
                        onSuggestionClick = { appendMessageToConversation(selectedConversationId, ChatItem.User(it)) }
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
                                    startImageUpload(uri)
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
                                    val currentText = inputText.trim()
                                    val newText = if (currentText.isBlank()) trimmedText else "$currentText $trimmedText"
                                    inputText = newText
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