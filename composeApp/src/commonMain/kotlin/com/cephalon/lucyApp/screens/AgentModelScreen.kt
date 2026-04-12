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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.cephalon.lucyApp.screens.agentmodel.ChatItem
import com.cephalon.lucyApp.screens.agentmodel.DraftAttachment
import com.cephalon.lucyApp.screens.agentmodel.DraftAttachmentType
import com.cephalon.lucyApp.screens.agentmodel.ImagePreviewState
import com.cephalon.lucyApp.screens.agentmodel.ConversationItem
import com.cephalon.lucyApp.screens.agentmodel.AgentModelAttachmentPanel
import com.cephalon.lucyApp.screens.agentmodel.AgentModelSearchScreen
import com.cephalon.lucyApp.screens.agentmodel.AgentModelComposer
import com.cephalon.lucyApp.screens.agentmodel.AgentModelImagePreview
import com.cephalon.lucyApp.screens.agentmodel.AgentModelMessageList
import com.cephalon.lucyApp.screens.agentmodel.AgentModelProfileScreen
import com.cephalon.lucyApp.screens.agentmodel.AgentModelTopBar
import com.cephalon.lucyApp.screens.agentmodel.AgentModelVoiceRecordingOverlay
import com.cephalon.lucyApp.screens.agentmodel.asPickedFile
import com.cephalon.lucyApp.sdk.SdkSessionManager
import org.koin.compose.koinInject

private const val STREAMING_PLACEHOLDER_TEXT = "思考中..."
private const val TYPEWRITER_DELAY_MS = 20L

@Composable
fun AgentModelScreen(
    onBack: () -> Unit,
    onNavigateToNas: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val sdkSessionManager = koinInject<SdkSessionManager>()
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val assistantReplyText by sdkSessionManager.assistantReplyText.collectAsState()
    val assistantReplyStreaming by sdkSessionManager.assistantReplyStreaming.collectAsState()
    val onlineDeviceCdis by sdkSessionManager.onlineDeviceCdis.collectAsState()

    val logs = remember {
        mutableStateListOf(
            "点击下面的按钮测试系统能力。",
            "相机会先申请系统权限，图库和文件会通过系统选择器打开。",
            "录音会先申请麦克风权限，结束后可在下方播放。"
        )
    }

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
    var selectedConversationId by remember { mutableStateOf<String?>("1") }

    fun updateConversation(conversationId: String?, transform: (ConversationItem) -> ConversationItem) {
        val targetId = conversationId ?: return
        val index = conversations.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            conversations[index] = transform(conversations[index])
        }
    }

    fun updateSelectedConversation(transform: (ConversationItem) -> ConversationItem) {
        updateConversation(selectedConversationId, transform)
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

    fun upsertStreamingAssistantMessageInConversation(conversationId: String?, text: String) {
        updateConversation(conversationId) { conversation ->
            val updatedMessages = conversation.messages.toMutableList()
            val lastItem = updatedMessages.lastOrNull()
            if (lastItem is ChatItem.Assistant && lastItem.text != STREAMING_PLACEHOLDER_TEXT) {
                updatedMessages[updatedMessages.lastIndex] = ChatItem.Assistant(text)
            } else {
                updatedMessages.add(ChatItem.Assistant(text))
            }
            conversation.copy(
                messages = updatedMessages,
                lastActiveAt = currentTimeMillis()
            )
        }
    }

    fun removeAssistantPlaceholderInConversation(conversationId: String?) {
        updateConversation(conversationId) { conversation ->
            val updatedMessages = conversation.messages.toMutableList()
            val placeholderIndex =
                updatedMessages.indexOfLast {
                    it is ChatItem.Assistant && it.text == STREAMING_PLACEHOLDER_TEXT
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

    val mediaAccessController = rememberPlatformMediaAccessController { message ->
        logs.add(0, message)
    }

    var inputText by remember { mutableStateOf("") }
    var attachmentsExpanded by remember { mutableStateOf(false) }
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
    var activeStreamingConversationId by remember { mutableStateOf<String?>(null) }
    var typedAssistantReply by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        sdkSessionManager.connectIfTokenValid()
    }

    LaunchedEffect(assistantReplyText, assistantReplyStreaming, activeStreamingConversationId, selectedConversationId) {
        val targetConversationId = activeStreamingConversationId ?: return@LaunchedEffect
        if (targetConversationId != selectedConversationId) {
            if (!assistantReplyStreaming) {
                activeStreamingConversationId = null
                typedAssistantReply = ""
            }
            return@LaunchedEffect
        }

        if (assistantReplyText.isBlank()) {
            // 等待首个 assistant.partial/assistant.final，不要在空文本且未流式时提前清理目标会话
            return@LaunchedEffect
        }

        val current = typedAssistantReply
        val target = assistantReplyText
        val shouldType =
            assistantReplyStreaming &&
                target.length >= current.length &&
                target.startsWith(current)

        if (!shouldType) {
            typedAssistantReply = target
            upsertStreamingAssistantMessageInConversation(targetConversationId, target)
        } else {
            for (index in (current.length + 1)..target.length) {
                val nextText = target.take(index)
                typedAssistantReply = nextText
                upsertStreamingAssistantMessageInConversation(targetConversationId, nextText)
                delay(TYPEWRITER_DELAY_MS)
            }
        }

        if (!assistantReplyStreaming) {
            removeAssistantPlaceholderInConversation(targetConversationId)
            activeStreamingConversationId = null
            typedAssistantReply = ""
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

    val sendMessage = {
        val text = inputText.trim()
        val attachments = draftAttachments.toList()

        if (text.isNotEmpty() || attachments.isNotEmpty()) {
            val targetConversationId = selectedConversationId
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

            val outgoingText =
                text.ifBlank {
                    "请基于我发送的附件内容，提炼重点并给出下一步建议。"
                }
            val targetCdi = onlineDeviceCdis.firstOrNull() ?: SdkSessionManager.DEFAULT_TARGET_CDI
            typedAssistantReply = ""
            appendMessageToConversation(
                targetConversationId,
                ChatItem.Assistant(STREAMING_PLACEHOLDER_TEXT)
            )
            coroutineScope.launch {
                val connectResult = sdkSessionManager.ensureConnectedIfTokenValid()
                if (connectResult.isFailure) {
                    removeAssistantPlaceholderInConversation(targetConversationId)
                    appendMessageToConversation(
                        targetConversationId,
                        ChatItem.System("连接失败：${connectResult.exceptionOrNull()?.message ?: "unknown"}")
                    )
                    return@launch
                }

                activeStreamingConversationId = targetConversationId
                sdkSessionManager.publishTextToNpc(cdi = targetCdi, text = outgoingText)
                    .onFailure { error ->
                        if (activeStreamingConversationId == targetConversationId) {
                            activeStreamingConversationId = null
                        }
                        typedAssistantReply = ""
                        removeAssistantPlaceholderInConversation(targetConversationId)
                        appendMessageToConversation(
                            targetConversationId,
                            ChatItem.System("发送失败：${error.message ?: "unknown"}")
                        )
                    }
            }

            inputText = ""
            attachmentsExpanded = false
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
                    .pointerInput(Unit) {
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

                        if (emptyViewState == 0) {
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
                                            // 文字区域可点击进入技能页
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { emptyViewState = 1 },
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
                            // ── 技能卡片页 ──
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = ds.sw(20.dp))
                                    .verticalScroll(rememberScrollState()),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(8.dp, cardShape, ambientColor = Color.Black.copy(alpha = 0.15f), spotColor = Color.Black.copy(alpha = 0.20f))
                                        .clip(cardShape)
                                        .background(glassBrush)
                                        .border(0.5.dp, Color.White, cardShape)
                                        .padding(ds.sw(24.dp)),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(ds.sh(16.dp))
                                ) {
                                    // 标题 + 小字
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = "Hi，我是脑花",
                                            color = Color(0xFF12192B),
                                            fontSize = ds.sp(20f),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(ds.sh(4.dp)))
                                        Text(
                                            text = "试试输入以下 Skill 来帮助完成工作细节",
                                            color = Color(0xFF595E6B),
                                            fontSize = ds.sp(14f),
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                    // 技能按钮列表
                                    val skillItems = listOf(
                                        Res.drawable.ic_skill_image to "脑花找图片，模糊的信息也能找",
                                        Res.drawable.ic_skill_voice to "脑花翻录音 记得一句就能翻出来",
                                        Res.drawable.ic_skill_document to "脑花调文档 文件名忘了也能调",
                                        Res.drawable.ic_skill_chat to "脑花搞内容 从想法到发出不断更",
                                        Res.drawable.ic_skill_knowledge to "脑花帮解答 知识库检索专属答案",
                                    )
                                    skillItems.forEach { (iconRes, text) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(ds.sh(44.dp))
                                                .border(
                                                    width = 0.5.dp,
                                                    color = Color(0xFF1F2535).copy(alpha = 0.20f),
                                                    shape = RoundedCornerShape(99.dp)
                                                )
                                                .clip(RoundedCornerShape(99.dp))
                                                .background(Color.White)
                                                .clickable { /* TODO: skill action */ }
                                                .padding(horizontal = ds.sw(16.dp)),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(ds.sw(8.dp))
                                        ) {
                                            Icon(
                                                painter = painterResource(iconRes),
                                                contentDescription = null,
                                                tint = Color.Unspecified,
                                                modifier = Modifier.size(ds.sm(20.dp))
                                            )
                                            Text(
                                                text = text,
                                                color = Color(0xFF12192B),
                                                fontSize = ds.sp(14f),
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 20.dp)
                        )
                    }

                    AgentModelComposer(
                        inputText = inputText,
                        onInputChange = {
                            inputText = it
                            if (attachmentsExpanded) attachmentsExpanded = false
                        },
                        draftAttachments = draftAttachments,
                        onRemoveDraftAttachment = { draftAttachments.remove(it) },
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

                    if (attachmentsExpanded) {
                        AgentModelAttachmentPanel(
                            recentImages = mediaAccessController.recentImages,
                            onOpenCamera = {
                                appendMessageToConversation(selectedConversationId, ChatItem.System("打开相机"))
                                mediaAccessController.openCamera()
                            },
                            onOpenGallery = {
                                appendMessageToConversation(selectedConversationId, ChatItem.System("选择图片"))
                                mediaAccessController.openGallery()
                            },
                            onOpenFilePicker = {
                                appendMessageToConversation(selectedConversationId, ChatItem.System("选择系统文件"))
                                mediaAccessController.openFilePicker()
                            },
                            onImageClick = { previewState = it },
                            onClearLogs = {
                                logs.clear()
                                appendMessageToConversation(selectedConversationId, ChatItem.System("已清空记录"))
                            }
                        )
                    }
                }

                AgentModelProfileScreen(
                    isVisible = showProfilePage,
                    onDismiss = { showProfilePage = false },
                    onNavigateToNas = onNavigateToNas,
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
                                    inputText = if (inputText.isBlank()) {
                                        trimmedText
                                    } else {
                                        "${inputText.trim()} $trimmedText".trim()
                                    }
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