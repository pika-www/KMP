package com.cephalon.lucyApp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.media.rememberPlatformMediaAccessController
import androidx.compose.material3.rememberDrawerState
import kotlinx.coroutines.launch
import com.cephalon.lucyApp.screens.agentmodel.ChatItem
import com.cephalon.lucyApp.screens.agentmodel.DraftAttachment
import com.cephalon.lucyApp.screens.agentmodel.DraftAttachmentType
import com.cephalon.lucyApp.screens.agentmodel.ImagePreviewState
import com.cephalon.lucyApp.screens.agentmodel.ConversationItem
import com.cephalon.lucyApp.screens.agentmodel.AgentModelAttachmentPanel
import com.cephalon.lucyApp.screens.agentmodel.AgentModelDrawerContent
import com.cephalon.lucyApp.screens.agentmodel.AgentModelSearchScreen
import com.cephalon.lucyApp.screens.agentmodel.AgentModelComposer
import com.cephalon.lucyApp.screens.agentmodel.AgentModelImagePreview
import com.cephalon.lucyApp.screens.agentmodel.AgentModelMessageList
import com.cephalon.lucyApp.screens.agentmodel.AgentModelProfileScreen
import com.cephalon.lucyApp.screens.agentmodel.AgentModelTopBar
import com.cephalon.lucyApp.screens.agentmodel.AgentModelVoiceRecordingOverlay

@Composable
fun AgentModelScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val logs = remember {
        mutableStateListOf(
            "点击下面的按钮测试系统能力。",
            "相机会先申请系统权限，图库和文件会通过系统选择器打开。",
            "录音会先申请麦克风权限，结束后可在下方播放。"
        )
    }

    val messages = remember {
        mutableStateListOf<ChatItem>(
            ChatItem.Assistant(
                "去系统设置里改，非常方便。\n\n你可以在这里测试：相机、相册、文件选择、录音。"
            )
        )
    }

    val mediaAccessController = rememberPlatformMediaAccessController { message ->
        logs.add(0, message)
        messages.add(ChatItem.System(message))
    }

    var inputText by remember { mutableStateOf("") }
    var attachmentsExpanded by remember { mutableStateOf(false) }
    var previewState by remember { mutableStateOf<ImagePreviewState?>(null) }
    var showProfilePage by remember { mutableStateOf(false) }
    var showSearchPage by remember { mutableStateOf(false) }

    val conversations = remember {
        mutableStateListOf(
            ConversationItem(id = "1", title = "云浏览器怎么让用户接管云端电脑"),
            ConversationItem(id = "2", title = "分析特斯拉股票最近的表现"),
            ConversationItem(id = "3", title = "Mac卡住了，鼠标一直转圈如何解决"),
        )
    }
    var selectedConversationId by remember { mutableStateOf<String?>("1") }

    val draftAttachments = remember { mutableStateListOf<DraftAttachment>() }
    var lastPickedImagesSize by remember { mutableStateOf(0) }
    var lastPickedFilesSize by remember { mutableStateOf(0) }

    var voiceCancelBySlide by remember { mutableStateOf(false) }
    val voiceCancelThreshold: Dp = 72.dp

    LaunchedEffect(Unit) {
        mediaAccessController.refreshRecentImages()
    }

    LaunchedEffect(attachmentsExpanded) {
        if (attachmentsExpanded) {
            mediaAccessController.refreshRecentImages()
        }
    }

    LaunchedEffect(mediaAccessController.recordings.size) {
        val latest = mediaAccessController.recordings.lastOrNull() ?: return@LaunchedEffect
        val exists = messages.any { it is ChatItem.RecordingItem && it.id == latest.id }
        if (!exists) {
            messages.add(ChatItem.System("录音完成，可以点击播放。"))
            messages.add(ChatItem.RecordingItem(id = latest.id, name = latest.name, path = latest.path))
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
                .forEach { uri ->
                    if (
                        uri.isNotBlank() &&
                        draftAttachments.none { it.type == DraftAttachmentType.File && it.uri == uri }
                    ) {
                        draftAttachments.add(DraftAttachment(DraftAttachmentType.File, uri))
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
            if (attachments.isNotEmpty()) {
                messages.add(
                    ChatItem.UserAttachments(
                        text = text.ifBlank { null },
                        attachments = attachments
                    )
                )
                draftAttachments.clear()
            } else {
                messages.add(ChatItem.User(text))
            }

            inputText = ""
            attachmentsExpanded = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    if (previewState == null) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White
            ) {
                AgentModelDrawerContent(
                    conversations = conversations,
                    selectedId = selectedConversationId,
                    onSelect = { item ->
                        selectedConversationId = item.id
                        coroutineScope.launch { drawerState.close() }
                    },
                    onDelete = { item ->
                        conversations.remove(item)
                        if (selectedConversationId == item.id) {
                            selectedConversationId = conversations.firstOrNull()?.id
                        }
                    },
                    onNewChat = {
                        val newId = (conversations.size + 1).toString()
                        val newItem = ConversationItem(id = newId, title = "新对话")
                        conversations.add(0, newItem)
                        selectedConversationId = newId
                        coroutineScope.launch { drawerState.close() }
                    },
                    onOpenSearch = {
                        coroutineScope.launch { drawerState.close() }
                        showSearchPage = true
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = Color.White,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(padding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        }
                )

                if (showProfilePage) {
                    AgentModelProfileScreen(
                        onBack = { showProfilePage = false },
                        onCall = { uriHandler.openUri("tel:") },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Surface(color = Color.White) {
                            AgentModelTopBar(
                                title = "脑花",
                                subtitle = "内容由 AI 生成",
                                onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
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
                            messages = messages,
                            recordings = mediaAccessController.recordings,
                            onPlayRecording = { mediaAccessController.playRecording(it) },
                            onImageClick = { previewState = it },
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
                            isRecording = mediaAccessController.isRecording,
                            isCancelBySlide = voiceCancelBySlide,
                            voiceCancelThreshold = voiceCancelThreshold,
                            onVoiceStart = {
                                focusManager.clearFocus()
                                voiceCancelBySlide = false
                                mediaAccessController.startRecording()
                            },
                            onVoiceFinish = {
                                voiceCancelBySlide = false
                                mediaAccessController.finishRecording()
                            },
                            onVoiceCancel = {
                                voiceCancelBySlide = false
                                mediaAccessController.cancelRecording()
                            },
                            onVoiceCancelStateChange = { voiceCancelBySlide = it },
                            attachmentsExpanded = attachmentsExpanded,
                            onToggleAttachments = {
                                attachmentsExpanded = !attachmentsExpanded
                                if (attachmentsExpanded) focusManager.clearFocus()
                            },
                            onSend = sendMessage,
                            onSuggestionClick = { messages.add(ChatItem.User(it)) }
                        )

                        if (attachmentsExpanded) {
                            AgentModelAttachmentPanel(
                                recentImages = mediaAccessController.recentImages,
                                onOpenCamera = {
                                    messages.add(ChatItem.System("打开相机"))
                                    mediaAccessController.openCamera()
                                },
                                onOpenGallery = {
                                    messages.add(ChatItem.System("选择图片"))
                                    mediaAccessController.openGallery()
                                },
                                onOpenFilePicker = {
                                    messages.add(ChatItem.System("选择系统文件"))
                                    mediaAccessController.openFilePicker()
                                },
                                onImageClick = { previewState = it },
                                onClearLogs = {
                                    logs.clear()
                                    messages.add(ChatItem.System("已清空记录"))
                                }
                            )
                        }
                    }
                }

                if (mediaAccessController.isRecording && !showProfilePage) {
                    AgentModelVoiceRecordingOverlay(
                        isCancelBySlide = voiceCancelBySlide,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

            }
        }
    }

    } // end if previewState == null

    // 搜索页 - 全屏覆盖
    if (showSearchPage) {
        AgentModelSearchScreen(
            conversations = conversations,
            onSelect = { item ->
                selectedConversationId = item.id
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
    } // end outer Box
}