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
import com.cephalon.lucyApp.media.rememberPlatformMediaAccessController
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

    val initialAssistantMessage = ChatItem.Assistant(
        "去系统设置里改，非常方便。\n\n你可以在这里测试：相机、相册、文件选择、录音。"
    )

    val conversations = remember {
        mutableStateListOf(
            ConversationItem(
                id = "1",
                title = "云浏览器怎么让用户接管云端电脑",
                messages = listOf(initialAssistantMessage),
                lastActiveAt = currentTimeMillis()
            ),
            ConversationItem(
                id = "2",
                title = "分析特斯拉股票最近的表现",
                messages = listOf(
                    ChatItem.User("分析特斯拉股票最近的表现"),
                    ChatItem.Assistant("可以从营收、交付量和估值三个维度先看。")
                ),
                lastActiveAt = currentTimeMillis() - 1_000L
            ),
            ConversationItem(
                id = "3",
                title = "Mac卡住了，鼠标一直转圈如何解决",
                messages = listOf(
                    ChatItem.User("Mac卡住了，鼠标一直转圈如何解决"),
                    ChatItem.Assistant("可以先尝试强制退出当前应用，再检查活动监视器。")
                ),
                lastActiveAt = currentTimeMillis() - 2_000L
            ),
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
        appendMessageToSelectedConversation(ChatItem.System(message))
    }

    var inputText by remember { mutableStateOf("") }
    var attachmentsExpanded by remember { mutableStateOf(false) }
    var previewState by remember { mutableStateOf<ImagePreviewState?>(null) }
    var showProfilePage by remember { mutableStateOf(false) }
    var showSearchPage by remember { mutableStateOf(false) }

    val draftAttachments = remember { mutableStateListOf<DraftAttachment>() }
    var lastPickedImagesSize by remember { mutableStateOf(0) }
    var lastPickedFilesSize by remember { mutableStateOf(0) }
    var isVoiceBusy by remember { mutableStateOf(false) }
    var voiceRecordingStartedAtMillis by remember { mutableStateOf<Long?>(null) }
    var activeStreamingConversationId by remember { mutableStateOf<String?>(null) }
    var typedAssistantReply by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        sdkSessionManager.connectIfTokenValid()
        mediaAccessController.refreshRecentImages()
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

    Box(modifier = Modifier.fillMaxSize()) {
    if (previewState == null) {
        Scaffold(
            containerColor = Color.White,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
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
                    Surface(color = Color.White) {
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
                            .padding(horizontal = 12.dp)
                    )

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
                        onSuggestionClick = { appendMessageToSelectedConversation(ChatItem.User(it)) }
                    )

                    if (attachmentsExpanded) {
                        AgentModelAttachmentPanel(
                            recentImages = mediaAccessController.recentImages,
                            onOpenCamera = {
                                appendMessageToSelectedConversation(ChatItem.System("打开相机"))
                                mediaAccessController.openCamera()
                            },
                            onOpenGallery = {
                                appendMessageToSelectedConversation(ChatItem.System("选择图片"))
                                mediaAccessController.openGallery()
                            },
                            onOpenFilePicker = {
                                appendMessageToSelectedConversation(ChatItem.System("选择系统文件"))
                                mediaAccessController.openFilePicker()
                            },
                            onImageClick = { previewState = it },
                            onClearLogs = {
                                logs.clear()
                                appendMessageToSelectedConversation(ChatItem.System("已清空记录"))
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

    // 搜索页 - 全屏覆盖
    if (showSearchPage) {
        AgentModelSearchScreen(
            conversations = orderedConversations,
            onSelect = { item ->
                selectedConversationId = item.id
                inputText = ""
                draftAttachments.clear()
                attachmentsExpanded = false
                showSearchPage = false
            },
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
    }
}