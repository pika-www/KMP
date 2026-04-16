package com.cephalon.lucyApp.screens.nas

import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.ic_delete
import androidios.composeapp.generated.resources.ic_download
import androidios.composeapp.generated.resources.ic_share
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.components.LocalDesignScale
import com.cephalon.lucyApp.time.currentTimeMillis
import com.cephalon.lucyApp.sdk.NasFileListItem
import com.cephalon.lucyApp.sdk.NasRegisterBlobItem
import com.cephalon.lucyApp.sdk.FileTransferDeviceKind
import com.cephalon.lucyApp.sdk.SdkSessionManager
import com.cephalon.lucyApp.sdk.TransferUploadItem
import com.cephalon.lucyApp.media.rememberPlatformMediaAccessController
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.abs

@Composable
fun NasScreen(onBack: () -> Unit) {
    val ds = LocalDesignScale.current
    val density = LocalDensity.current
    val swipeStartEdgePx = with(density) { 28.dp.toPx() }
    val swipeBackThresholdPx = with(density) { 72.dp.toPx() }
    val sdkSessionManager = koinInject<SdkSessionManager>()
    val coroutineScope = rememberCoroutineScope()
    val onlineDeviceCdis by sdkSessionManager.onlineDeviceCdis.collectAsState()

    var selectedCategory by remember { mutableStateOf(NasCategory.Photos) }
    var isSearchMode by remember { mutableStateOf(false) }
    var isSearchSelectionMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isPhotoSelectionMode by remember { mutableStateOf(false) }
    var isAudioSelectionMode by remember { mutableStateOf(false) }
    var isDocumentSelectionMode by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<NasImageItem?>(null) }
    var previewImage by remember { mutableStateOf<NasImageItem?>(null) }
    var selectedAudio by remember { mutableStateOf<NasAudioItem?>(null) }
    var selectedDocument by remember { mutableStateOf<NasDocumentItem?>(null) }
    var showUploadProgressDialog by remember { mutableStateOf(false) }
    var lastPickerCategory by remember { mutableStateOf<NasCategory?>(null) }
    var lastPickedImagesSize by remember { mutableIntStateOf(0) }
    var lastPickedFilesSize by remember { mutableIntStateOf(0) }
    val selectedPhotoIds = remember { mutableStateListOf<String>() }
    val selectedAudioIds = remember { mutableStateListOf<String>() }
    val selectedDocumentIds = remember { mutableStateListOf<String>() }
    val uploadTasks = remember { mutableStateListOf<NasUploadTaskItem>() }
    val imageItems = remember { mutableStateListOf<NasImageItem>() }
    val audioItems = remember { mutableStateListOf<NasAudioItem>() }
    val documentItems = remember { mutableStateListOf<NasDocumentItem>() }
    val nextCursorMap = remember { mutableStateMapOf<NasCategory, String?>() }
    val errorMap = remember { mutableStateMapOf<NasCategory, String?>() }
    val hasLoadedMap = remember { mutableStateMapOf<NasCategory, Boolean>() }
    val loadingMap = remember { mutableStateMapOf<NasCategory, Boolean>() }
    val loadingMoreMap = remember { mutableStateMapOf<NasCategory, Boolean>() }
    val activeUploadCount = uploadTasks.count {
        it.status == NasUploadTaskStatus.Uploading ||
            it.status == NasUploadTaskStatus.Registering ||
            it.status == NasUploadTaskStatus.Waiting
    }
    val targetCdi = onlineDeviceCdis.firstOrNull() ?: ""
    val mediaController = rememberPlatformMediaAccessController(
        onEvent = { message -> println("NAS Media Event: $message") }
    )

    fun appendUploadTasks(tasks: List<NasUploadTaskItem>) {
        if (tasks.isEmpty()) return
        uploadTasks.addAll(0, tasks.asReversed())
        showUploadProgressDialog = true
    }

    fun replaceUploadTask(taskId: String, transform: (NasUploadTaskItem) -> NasUploadTaskItem) {
        val index = uploadTasks.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            uploadTasks[index] = transform(uploadTasks[index])
        }
    }

    fun launchBatchUpload(
        category: NasCategory,
        items: List<Pair<String, String>>,
    ) {
        if (items.isEmpty()) return

        val preparedTasks =
            items.mapIndexed { index, (uri, displayName) ->
                NasUploadTaskItem(
                    id = "nas_upload_${currentTimeMillisSafe()}_${index}_${displayName.hashCode().toString().replace('-', '0')}",
                    title = displayName,
                    type = category.toUploadTaskType(),
                    progress = if (index == 0) 0.08f else 0f,
                    status = if (index == 0) NasUploadTaskStatus.Uploading else NasUploadTaskStatus.Waiting,
                )
            }
        appendUploadTasks(preparedTasks)

        coroutineScope.launch {
            val uploadPayloads = mutableListOf<TransferUploadItem>()

            preparedTasks.forEachIndexed { index, task ->
                val uri = items[index].first
                val bytes = mediaController.readUriToBytes(uri)
                if (bytes == null || bytes.isEmpty()) {
                    replaceUploadTask(task.id) {
                        it.copy(status = NasUploadTaskStatus.Failed, progress = 0f)
                    }
                    return@forEachIndexed
                }

                uploadPayloads += TransferUploadItem(
                    entryId = task.id,
                    entryName = task.title,
                    bytes = bytes,
                )
                replaceUploadTask(task.id) {
                    it.copy(
                        status = if (uploadPayloads.size == 1) NasUploadTaskStatus.Uploading else NasUploadTaskStatus.Waiting,
                        progress = if (uploadPayloads.size == 1) 0.12f else 0f,
                    )
                }
            }

            if (uploadPayloads.isEmpty()) return@launch

            sdkSessionManager
                .sendFilesToDevice(
                    targetCdi = targetCdi,
                    items = uploadPayloads,
                    deviceKind = FileTransferDeviceKind.Nas,
                    onProgress = { frame ->
                        val orderedIds = uploadPayloads.map { it.entryId }
                        val completedCount = frame.completedEntries.coerceIn(0, orderedIds.size)
                        orderedIds.forEachIndexed { index, entryId ->
                            when {
                                index < completedCount -> {
                                    replaceUploadTask(entryId) {
                                        it.copy(status = NasUploadTaskStatus.Uploading, progress = 0.92f)
                                    }
                                }
                                index == completedCount -> {
                                    replaceUploadTask(entryId) {
                                        it.copy(status = NasUploadTaskStatus.Uploading, progress = 0.35f)
                                    }
                                }
                                else -> {
                                    replaceUploadTask(entryId) {
                                        it.copy(status = NasUploadTaskStatus.Waiting, progress = 0f)
                                    }
                                }
                            }
                        }
                    },
                )
                .onSuccess { outcome ->
                    val doneMap = outcome.done.items.associateBy { it.entryId }
                    val registerItems = mutableListOf<NasRegisterBlobItem>()
                    outcome.sentItems.forEach { sentItem ->
                        val result = doneMap[sentItem.entryId]
                        if (result?.ok == true) {
                            replaceUploadTask(sentItem.entryId) { current ->
                                current.copy(status = NasUploadTaskStatus.Registering, progress = 0.96f)
                            }
                            registerItems += NasRegisterBlobItem(
                                blobRef = sentItem.blobRef,
                                entryId = sentItem.entryId,
                                fileName = sentItem.entryName,
                                kind = sentItem.entryName.toNasRegisterKind(),
                                contentType = sentItem.entryName.toMimeType(),
                            )
                        } else {
                            replaceUploadTask(sentItem.entryId) { current ->
                                current.copy(
                                    status = NasUploadTaskStatus.Failed,
                                    progress = current.progress.coerceAtLeast(0f),
                                )
                            }
                        }
                    }

                    if (registerItems.isEmpty()) {
                        return@onSuccess
                    }

                    sdkSessionManager
                        .registerBlobsToNas(
                            targetCdi = targetCdi,
                            items = registerItems,
                        )
                        .onSuccess { registerResponse ->
                            val resultMap = registerResponse.results.associateBy { it.blobRef }
                            registerItems.forEach { registerItem ->
                                val registerResult = resultMap[registerItem.blobRef]
                                replaceUploadTask(registerItem.entryId.orEmpty()) { current ->
                                    if (registerResult?.ok == true) {
                                        current.copy(status = NasUploadTaskStatus.Completed, progress = 1f)
                                    } else {
                                        current.copy(
                                            status = NasUploadTaskStatus.Failed,
                                            progress = current.progress.coerceAtLeast(0.96f),
                                        )
                                    }
                                }
                            }
                        }
                        .onFailure {
                            registerItems.forEach { registerItem ->
                                replaceUploadTask(registerItem.entryId.orEmpty()) { current ->
                                    current.copy(
                                        status = NasUploadTaskStatus.Failed,
                                        progress = current.progress.coerceAtLeast(0.96f),
                                    )
                                }
                            }
                        }
                }
                .onFailure {
                    uploadPayloads.forEach { item ->
                        replaceUploadTask(item.entryId) { current ->
                            current.copy(status = NasUploadTaskStatus.Failed)
                        }
                    }
                }
        }
    }

    LaunchedEffect(mediaController.pickedImages.size, lastPickerCategory) {
        val size = mediaController.pickedImages.size
        if (size > lastPickedImagesSize && lastPickerCategory == NasCategory.Photos) {
            val newUris = mediaController.pickedImages.take(size - lastPickedImagesSize)
            val items =
                newUris
                    .filter { it.isNotBlank() }
                    .map { uri -> uri to deriveUploadDisplayName(uri, defaultPrefix = "图片") }
                    .distinctBy { it.first }
            launchBatchUpload(
                category = NasCategory.Photos,
                items = items,
            )
        }
        lastPickedImagesSize = size
    }

    LaunchedEffect(mediaController.pickedFiles.size, lastPickerCategory) {
        val size = mediaController.pickedFiles.size
        val pickerCategory = lastPickerCategory
        if (size > lastPickedFilesSize && pickerCategory != null && pickerCategory != NasCategory.Photos) {
            val newFiles = mediaController.pickedFiles.take(size - lastPickedFilesSize)
            val items =
                newFiles
                    .filter { it.uri.isNotBlank() }
                    .map { file -> file.uri to file.displayName.ifBlank { deriveUploadDisplayName(file.uri, defaultPrefix = pickerCategory.title) } }
                    .distinctBy { it.first }
            launchBatchUpload(
                category = pickerCategory,
                items = items,
            )
        }
        lastPickedFilesSize = size
    }

    fun setCategoryItems(category: NasCategory, items: List<NasFileListItem>, append: Boolean) {
        when (category) {
            NasCategory.Photos -> {
                val mapped = items.map { it.toNasImageItem() }
                if (!append) imageItems.clear()
                imageItems.addAll(mapped)
            }
            NasCategory.Recordings -> {
                val mapped = items.map { it.toNasAudioItem() }
                if (!append) audioItems.clear()
                audioItems.addAll(mapped)
            }
            NasCategory.Documents -> {
                val mapped = items.map { it.toNasDocumentItem() }
                if (!append) documentItems.clear()
                documentItems.addAll(mapped)
            }
        }
    }

    fun clearAllNasListState() {
        imageItems.clear()
        audioItems.clear()
        documentItems.clear()
        nextCursorMap.clear()
        errorMap.clear()
        hasLoadedMap.clear()
        loadingMap.clear()
        loadingMoreMap.clear()
    }

    fun requestNasList(category: NasCategory, loadMore: Boolean = false) {
        if (loadingMap[category] == true || loadingMoreMap[category] == true) return
        val cursor = if (loadMore) nextCursorMap[category] else null
        if (loadMore && cursor.isNullOrBlank()) return

        if (loadMore) {
            loadingMoreMap[category] = true
        } else {
            loadingMap[category] = true
            errorMap.remove(category)
        }

        coroutineScope.launch {
            sdkSessionManager
                .listFilesFromNas(
                    targetCdi = targetCdi,
                    kind = category.toNasListKind(),
                    pageSize = NAS_PAGE_SIZE,
                    cursor = cursor,
                )
                .onSuccess { response ->
                    val responseError = response.error?.takeIf { it.isNotBlank() }
                    if (responseError != null) {
                        errorMap[category] = responseError
                        return@onSuccess
                    }
                    errorMap.remove(category)
                    setCategoryItems(category = category, items = response.items, append = loadMore)
                    nextCursorMap[category] = response.nextCursor
                    hasLoadedMap[category] = true
                }
                .onFailure { error ->
                    errorMap[category] = error.message ?: "加载失败"
                }

            loadingMap[category] = false
            loadingMoreMap[category] = false
        }
    }

    LaunchedEffect(targetCdi) {
        clearAllNasListState()
    }

    LaunchedEffect(selectedCategory, targetCdi) {
        if (hasLoadedMap[selectedCategory] == true) return@LaunchedEffect
        requestNasList(selectedCategory, loadMore = false)
    }

    val imageMonths = imageItems.toImageMonthGroups()
    val audios = audioItems.toAudioMonthGroups()
    val documents = documentItems.toDocumentMonthGroups()
    val allImages = imageItems.toList()
    val allPhotoIds = imageItems.map { it.id }
    val allAudioIds = audioItems.map { it.id }
    val allDocumentIds = documentItems.map { it.id }

    fun exitPhotoSelectionMode() {
        isPhotoSelectionMode = false
        selectedPhotoIds.clear()
    }

    fun exitAudioSelectionMode() {
        isAudioSelectionMode = false
        selectedAudioIds.clear()
    }

    fun exitDocumentSelectionMode() {
        isDocumentSelectionMode = false
        selectedDocumentIds.clear()
    }

    fun exitAllSelectionModes() {
        exitPhotoSelectionMode()
        exitAudioSelectionMode()
        exitDocumentSelectionMode()
    }

    fun togglePhotoSelection(image: NasImageItem) {
        if (selectedPhotoIds.contains(image.id)) {
            selectedPhotoIds.remove(image.id)
        } else {
            selectedPhotoIds.add(image.id)
        }
    }

    fun toggleAudioSelection(audio: NasAudioItem) {
        if (selectedAudioIds.contains(audio.id)) {
            selectedAudioIds.remove(audio.id)
        } else {
            selectedAudioIds.add(audio.id)
        }
    }

    fun toggleDocumentSelection(document: NasDocumentItem) {
        if (selectedDocumentIds.contains(document.id)) {
            selectedDocumentIds.remove(document.id)
        } else {
            selectedDocumentIds.add(document.id)
        }
    }

    fun handleNasBack() {
        when {
            previewImage != null -> previewImage = null
            selectedImage != null -> selectedImage = null
            selectedAudio != null -> {
                mediaController.stopAudioPlayback()
                selectedAudio = null
            }
            selectedDocument != null -> selectedDocument = null
            isSearchSelectionMode -> isSearchSelectionMode = false
            isSearchMode -> {
                isSearchMode = false
                isSearchSelectionMode = false
                searchQuery = ""
            }
            isPhotoSelectionMode || isAudioSelectionMode || isDocumentSelectionMode -> exitAllSelectionModes()
            else -> onBack()
        }
    }

    PlatformBackHandler(onBack = ::handleNasBack)

    // 如果选中了图片，显示详情页
    selectedImage?.let { image ->
        NasImageDetailScreen(
            images = allImages,
            initialImageId = image.id,
            onBack = ::handleNasBack,
            onShare = { currentImage ->
                println("分享图片: ${currentImage.name}")
                // TODO: 实现分享功能
            },
            onDownload = { currentImage ->
                println("下载图片: ${currentImage.name}")
                // TODO: 实现下载功能
            },
            onDelete = { currentImage ->
                println("删除图片: ${currentImage.name}")
                // TODO: 实现删除功能
                selectedImage = null
            }
        )
        return
    }

    // 如果选中了音频，显示详情页
    selectedAudio?.let { audio ->
        NasAudioDetailScreen(
            audio = audio,
            mediaController = mediaController,
            onBack = ::handleNasBack,
            onShare = {
                println("分享音频: ${audio.name}")
                // TODO: 实现分享功能
            },
            onDownload = {
                println("下载音频: ${audio.name}")
                // TODO: 实现下载功能
            },
            onDelete = {
                println("删除音频: ${audio.name}")
                // TODO: 实现删除功能
                mediaController.stopAudioPlayback()
                selectedAudio = null
            }
        )
        return
    }

    // 如果选中了文档，显示详情页
    selectedDocument?.let { document ->
        NasDocumentDetailScreen(
            document = document,
            onBack = ::handleNasBack,
            onShare = {
                println("分享文档: ${document.name}")
                // TODO: 实现分享功能
            },
            onDownload = {
                println("下载文档: ${document.name}")
                // TODO: 实现下载功能
            },
            onDelete = {
                println("删除文档: ${document.name}")
                // TODO: 实现删除功能
                selectedDocument = null
            }
        )
        return
    }

    val isCurrentSelectionMode = when (selectedCategory) {
        NasCategory.Photos -> isPhotoSelectionMode
        NasCategory.Recordings -> isAudioSelectionMode
        NasCategory.Documents -> isDocumentSelectionMode
    }
    val currentCategoryError = errorMap[selectedCategory]
    val currentCategoryLoading = loadingMap[selectedCategory] == true
    val currentCategoryLoadingMore = loadingMoreMap[selectedCategory] == true
    val currentCategoryHasMore = !nextCursorMap[selectedCategory].isNullOrBlank()
    val currentCategoryFooter: @Composable (() -> Unit) = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ds.sm(10.dp))
        ) {
            if (currentCategoryLoading) {
                Text(
                    text = "加载中...",
                    style = TextStyle(color = Color.White.copy(alpha = 0.72f), fontSize = ds.sp(13f))
                )
            }
            if (!currentCategoryLoading && currentCategoryError != null) {
                Text(
                    text = currentCategoryError,
                    style = TextStyle(color = Color.White.copy(alpha = 0.72f), fontSize = ds.sp(13f))
                )
                NasGlassTextButton(
                    text = if (hasLoadedMap[selectedCategory] == true) "重新加载" else "重试",
                    onClick = { requestNasList(selectedCategory, loadMore = false) },
                    modifier = Modifier.width(ds.sm(132.dp))
                )
            } else if (!currentCategoryLoading && currentCategoryHasMore) {
                NasGlassTextButton(
                    text = if (currentCategoryLoadingMore) "加载中..." else "加载更多",
                    onClick = {
                        if (!currentCategoryLoadingMore) {
                            requestNasList(selectedCategory, loadMore = true)
                        }
                    },
                    modifier = Modifier.width(ds.sm(132.dp))
                )
            }
        }
    }

    Scaffold(containerColor = Color.Black) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(::handleNasBack, swipeStartEdgePx, swipeBackThresholdPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(pass = PointerEventPass.Initial)
                        if (down.position.x > swipeStartEdgePx) return@awaitEachGesture

                        val pointerId = down.id
                        var totalDx = 0f
                        var totalAbsDy = 0f

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) break

                            val delta = change.position - change.previousPosition
                            totalDx += delta.x
                            totalAbsDy += abs(delta.y)

                            if (
                                totalDx > swipeBackThresholdPx &&
                                totalDx > totalAbsDy * 1.2f
                            ) {
                                handleNasBack()
                                break
                            }
                        }
                    }
                }
        ) {
            // ── 内容区：全屏可滚动，图片可滚到 tab 栏后面 ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isSearchMode) {
                        Spacer(modifier = Modifier.height(ds.sm(if (activeUploadCount > 0) 108.dp else 72.dp)))
                        Text(
                            text = if (isSearchSelectionMode) {
                                "最近搜索"
                            } else {
                                "正在寻找关于“${searchQuery.ifBlank { "" }}”的${selectedCategory.title}"
                            },
                            style = TextStyle(
                                color = Color.White,
                                fontSize = ds.sp(16f),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(ds.sm(18.dp)))
                    } else if (activeUploadCount > 0) {
                        Spacer(modifier = Modifier.height(ds.sm(44.dp)))
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedCategory) {
                            NasCategory.Photos -> NasPhotosContent(
                                imageMonths = imageMonths,
                                selectionMode = isPhotoSelectionMode,
                                selectedImageIds = selectedPhotoIds,
                                onImageClick = { image -> selectedImage = image },
                                onImageLongClick = { image -> previewImage = image },
                                onImageSelectionToggle = { image -> togglePhotoSelection(image) },
                                emptyText = if (currentCategoryLoading) null else "暂无图片",
                                footer = currentCategoryFooter,
                            )
                            NasCategory.Recordings -> NasRecordingsContent(
                                audioMonths = audios,
                                selectionMode = isAudioSelectionMode,
                                selectedAudioIds = selectedAudioIds,
                                onAudioClick = { audio -> selectedAudio = audio },
                                onAudioSelectionToggle = { audio -> toggleAudioSelection(audio) },
                                emptyText = if (currentCategoryLoading) null else "暂无音频",
                                footer = currentCategoryFooter,
                            )
                            NasCategory.Documents -> NasDocumentsContent(
                                documentMonths = documents,
                                selectionMode = isDocumentSelectionMode,
                                selectedDocumentIds = selectedDocumentIds,
                                onDocumentClick = { document -> selectedDocument = document },
                                onDocumentSelectionToggle = { document -> toggleDocumentSelection(document) },
                                emptyText = if (currentCategoryLoading) null else "暂无文档",
                                footer = currentCategoryFooter,
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 16.dp)
            ) {
                if (isSearchMode) {
                    if (isSearchSelectionMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NasGlassTextButton(
                                text = "全选",
                                onClick = {
                                    when (selectedCategory) {
                                        NasCategory.Photos -> {
                                            if (selectedPhotoIds.size == allPhotoIds.size) {
                                                selectedPhotoIds.clear()
                                            } else {
                                                selectedPhotoIds.clear()
                                                selectedPhotoIds.addAll(allPhotoIds)
                                            }
                                        }
                                        NasCategory.Recordings -> {
                                            if (selectedAudioIds.size == allAudioIds.size) {
                                                selectedAudioIds.clear()
                                            } else {
                                                selectedAudioIds.clear()
                                                selectedAudioIds.addAll(allAudioIds)
                                            }
                                        }
                                        NasCategory.Documents -> {
                                            if (selectedDocumentIds.size == allDocumentIds.size) {
                                                selectedDocumentIds.clear()
                                            } else {
                                                selectedDocumentIds.clear()
                                                selectedDocumentIds.addAll(allDocumentIds)
                                            }
                                        }
                                    }
                                }
                            )
                            NasGlassCircleButton(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭选择",
                                onClick = {
                                    isSearchSelectionMode = false
                                    exitAllSelectionModes()
                                },
                                modifier = Modifier.size(ds.sm(44.dp))
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(ds.sm(44.dp)))
                            NasGlassTextButton(
                                text = "选择",
                                onClick = {
                                    isSearchSelectionMode = true
                                    when (selectedCategory) {
                                        NasCategory.Photos -> {
                                            exitAllSelectionModes()
                                            isPhotoSelectionMode = true
                                            selectedPhotoIds.clear()
                                        }
                                        NasCategory.Recordings -> {
                                            exitAllSelectionModes()
                                            isAudioSelectionMode = true
                                            selectedAudioIds.clear()
                                        }
                                        NasCategory.Documents -> {
                                            exitAllSelectionModes()
                                            isDocumentSelectionMode = true
                                            selectedDocumentIds.clear()
                                        }
                                    }
                                }
                            )
                        }
                    }
                } else if (isCurrentSelectionMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NasGlassTextButton(
                            text = "上传脑花",
                            onClick = {
                                val count = when (selectedCategory) {
                                    NasCategory.Photos -> selectedPhotoIds.size
                                    NasCategory.Recordings -> selectedAudioIds.size
                                    NasCategory.Documents -> selectedDocumentIds.size
                                }
                                println("发送朋友: ${count}项")
                            },
                            icon = Res.drawable.ic_share
                        )
                        NasGlassTextButton(
                            text = "下载",
                            onClick = {
                                val count = when (selectedCategory) {
                                    NasCategory.Photos -> selectedPhotoIds.size
                                    NasCategory.Recordings -> selectedAudioIds.size
                                    NasCategory.Documents -> selectedDocumentIds.size
                                }
                                println("下载资源: ${count}项")
                            },
                            icon = Res.drawable.ic_download
                        )
                        NasGlassTextButton(
                            text = "删除",
                            onClick = {
                                val count = when (selectedCategory) {
                                    NasCategory.Photos -> selectedPhotoIds.size
                                    NasCategory.Recordings -> selectedAudioIds.size
                                    NasCategory.Documents -> selectedDocumentIds.size
                                }
                                println("删除资源: ${count}项")
                            },
                            icon = Res.drawable.ic_delete
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NasTopCategoryRow(
                            selected = selectedCategory,
                            onSelect = {
                                selectedCategory = it
                                exitAllSelectionModes()
                                isSearchMode = false
                                isSearchSelectionMode = false
                                searchQuery = ""
                            }
                        )
                        if (activeUploadCount > 0) {
                            NasUploadBanner(
                                activeCount = activeUploadCount,
                                onClick = { showUploadProgressDialog = true }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                if (isSearchMode) {
                    if (isSearchSelectionMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NasGlassTextButton(
                                text = "发送脑花",
                                onClick = { /* TODO: Implement send action */ },
                                modifier = Modifier.width(140.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NasGlassCircleButton(
                                    imageVector = Icons.Outlined.FileDownload,
                                    contentDescription = "下载",
                                    onClick = { /* TODO: Implement download action */ },
                                    modifier = Modifier.size(44.dp)
                                )
                                NasGlassCircleButton(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "删除",
                                    onClick = { /* TODO: Implement delete action */ },
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .background(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = ds.sp(14f)
                                ),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "搜索",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = ds.sp(14f)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            NasGlassCircleButton(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "退出搜索",
                                onClick = {
                                    isSearchMode = false
                                    searchQuery = ""
                                },
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                } else if (isPhotoSelectionMode || isAudioSelectionMode || isDocumentSelectionMode) {
                    // 选择模式：退出选择 + 占位符平衡布局
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NasGlassTextButton(
                            text = "退出选择",
                            onClick = { exitAllSelectionModes() },
                            selected = true
                        )
                        Spacer(modifier = Modifier.size(width = 96.dp, height = 44.dp))
                    }
                } else {
                    NasBottomQuickActions(
                        onSelectionClick = {
                            when (selectedCategory) {
                                NasCategory.Photos -> {
                                    exitAllSelectionModes()
                                    isPhotoSelectionMode = true
                                    selectedPhotoIds.clear()
                                }
                                NasCategory.Recordings -> {
                                    exitAllSelectionModes()
                                    isAudioSelectionMode = true
                                    selectedAudioIds.clear()
                                }
                                NasCategory.Documents -> {
                                    exitAllSelectionModes()
                                    isDocumentSelectionMode = true
                                    selectedDocumentIds.clear()
                                }
                            }
                        },
                        onAddClick = {
                            lastPickerCategory = selectedCategory
                            when (selectedCategory) {
                                NasCategory.Photos -> mediaController.openGallery()
                                NasCategory.Recordings -> mediaController.openAudioPicker()
                                NasCategory.Documents -> mediaController.openFilePicker()
                            }
                        },
                        onSearchClick = {
                            exitAllSelectionModes()
                            isSearchMode = true
                            isSearchSelectionMode = false
                        }
                    )
                }
            }
        }
    }

    if (showUploadProgressDialog) {
        NasUploadProgressDialog(
            tasks = uploadTasks,
            onDismiss = { showUploadProgressDialog = false }
        )
    }

    previewImage?.let { image ->
        NasImageActionPopup(
            image = image,
            onDismiss = { previewImage = null },
            onShare = {
                println("分享图片: ${image.name}")
                // TODO: 实现分享功能
                previewImage = null
            },
            onDownload = {
                println("下载图片: ${image.name}")
                // TODO: 实现下载功能
                previewImage = null
            },
            onDelete = {
                println("删除图片: ${image.name}")
                // TODO: 实现删除功能
                previewImage = null
            }
        )
    }
}

private fun NasCategory.toUploadTaskType(): NasUploadTaskType =
    when (this) {
        NasCategory.Photos -> NasUploadTaskType.Image
        NasCategory.Recordings -> NasUploadTaskType.Audio
        NasCategory.Documents -> NasUploadTaskType.Document
    }

private fun NasCategory.toNasListKind(): String =
    when (this) {
        NasCategory.Photos -> "image"
        NasCategory.Recordings -> "audio"
        NasCategory.Documents -> "file"
    }

private fun deriveUploadDisplayName(uri: String, defaultPrefix: String): String {
    val sanitized = uri.substringAfterLast('/').substringBefore('?').substringBefore('#')
    return sanitized.takeIf { it.isNotBlank() } ?: "$defaultPrefix-${currentTimeMillisSafe()}"
}

private fun currentTimeMillisSafe(): Long = currentTimeMillis()

private fun String.toNasRegisterKind(): String {
    return when (substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp", "svg" -> "image"
        "mp3", "wav", "m4a", "aac", "flac", "ogg" -> "audio"
        else -> "file"
    }
}

private fun String.toMimeType(): String {
    return when (substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "heic", "heif" -> "image/heic"
        "bmp" -> "image/bmp"
        "svg" -> "image/svg+xml"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "m4a" -> "audio/mp4"
        "aac" -> "audio/aac"
        "flac" -> "audio/flac"
        "ogg" -> "audio/ogg"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "json" -> "application/json"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        else -> "application/octet-stream"
    }
}

private fun NasFileListItem.toNasImageItem(): NasImageItem {
    val name = fileName.orEmpty().ifBlank { "图片-${id ?: currentTimeMillisSafe()}" }
    return NasImageItem(
        id = (id?.toString() ?: name).ifBlank { "image-${currentTimeMillisSafe()}" },
        name = name,
        type = "图片",
        format = name.substringAfterLast('.', "png").ifBlank { "png" },
        sizeKB = ((size ?: 0L) / 1024L).toInt().coerceAtLeast(0),
        path = thumbnailImgBlobRef.orEmpty(),
        time = time.orEmpty().ifBlank { "--" },
        location = location,
        resolution = desc.orEmpty().ifBlank { "--" },
    )
}

private fun NasFileListItem.toNasAudioItem(): NasAudioItem {
    val name = fileName.orEmpty().ifBlank { "音频-${id ?: currentTimeMillisSafe()}" }
    return NasAudioItem(
        id = (id?.toString() ?: name).ifBlank { "audio-${currentTimeMillisSafe()}" },
        name = name,
        type = "音频",
        format = name.substringAfterLast('.', "m4a").ifBlank { "m4a" },
        sizeKB = ((size ?: 0L) / 1024L).toInt().coerceAtLeast(0),
        path = thumbnailImgBlobRef.orEmpty(),
        time = time.orEmpty().ifBlank { "--" },
        durationSec = 0,
    )
}

private fun NasFileListItem.toNasDocumentItem(): NasDocumentItem {
    val name = fileName.orEmpty().ifBlank { "文档-${id ?: currentTimeMillisSafe()}" }
    return NasDocumentItem(
        id = (id?.toString() ?: name).ifBlank { "document-${currentTimeMillisSafe()}" },
        name = name,
        type = "文档",
        format = name.substringAfterLast('.', "file").ifBlank { "file" },
        sizeKB = ((size ?: 0L) / 1024L).toInt().coerceAtLeast(0),
        path = thumbnailImgBlobRef.orEmpty(),
        time = time.orEmpty().ifBlank { "--" },
    )
}

private fun List<NasImageItem>.toImageMonthGroups(): List<NasImageMonthGroup> {
    return this
        .groupBy { it.time.toMonthLabel() }
        .entries
        .sortedByDescending { it.key }
        .map { (label, items) ->
            NasImageMonthGroup(label = label, images = items.sortedByDescending { image -> image.time })
        }
}

private fun List<NasAudioItem>.toAudioMonthGroups(): List<NasAudioMonthGroup> {
    return this
        .groupBy { it.time.toMonthLabel() }
        .entries
        .sortedByDescending { it.key }
        .map { (label, items) ->
            NasAudioMonthGroup(label = label, audios = items.sortedByDescending { audio -> audio.time })
        }
}

private fun List<NasDocumentItem>.toDocumentMonthGroups(): List<NasDocumentMonthGroup> {
    return this
        .groupBy { it.time.toMonthLabel() }
        .entries
        .sortedByDescending { it.key }
        .map { (label, items) ->
            NasDocumentMonthGroup(label = label, documents = items.sortedByDescending { document -> document.time })
        }
}

private fun String.toMonthLabel(): String {
    val datePart = substringBefore(' ').trim()
    val segments = datePart.split('-')
    val year = segments.getOrNull(0)?.takeIf { it.length == 4 && it.all(Char::isDigit) }
    val month = segments.getOrNull(1)?.takeIf { it.isNotBlank() }
    return if (year != null && month != null) {
        "${year}年${month.removePrefix("0")}月"
    } else {
        "未分组"
    }
}

private const val NAS_PAGE_SIZE = 20
