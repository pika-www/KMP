package com.cephalon.lucyApp.screens.nas

import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.ic_delete
import androidios.composeapp.generated.resources.ic_download
import androidios.composeapp.generated.resources.ic_share
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.cephalon.lucyApp.components.LocalDesignScale
import com.cephalon.lucyApp.time.currentTimeMillis
import com.cephalon.lucyApp.sdk.NasCategoryCache
import com.cephalon.lucyApp.sdk.NasFileListItem
import com.cephalon.lucyApp.sdk.NasRegisterBlobItem
import com.cephalon.lucyApp.sdk.FileTransferDeviceKind
import com.cephalon.lucyApp.sdk.SdkSessionManager
import com.cephalon.lucyApp.sdk.TransferUploadItem
import com.cephalon.lucyApp.media.platformSaveFile
import com.cephalon.lucyApp.media.rememberPlatformMediaAccessController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.abs

/** 上传任务持久化：不随 NasScreen 组合生命周期销毁 */
internal object NasUploadTaskStore {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val tasks = mutableStateListOf<NasUploadTaskItem>()
    val progressBatches = mutableStateListOf<NasUploadBatchSummary>()
    var taskProgressSummary by mutableStateOf<NasUploadProgressSummary?>(null)
    var showDialog by mutableStateOf(false)

    fun append(newTasks: List<NasUploadTaskItem>) {
        if (newTasks.isEmpty()) return
        tasks.addAll(0, newTasks.asReversed())
        showDialog = true
    }

    fun beginProgressBatch(batchId: String, totalCount: Int) {
        if (totalCount <= 0) return
        cleanupFinishedUploadBatches()
        progressBatches.add(
            0,
            NasUploadBatchSummary(
                id = batchId,
                totalCount = totalCount,
                completedCount = 0
            )
        )
        refreshUploadProgressSummary()
        showDialog = true
    }

    fun replace(taskId: String, transform: (NasUploadTaskItem) -> NasUploadTaskItem) {
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            tasks[index] = transform(tasks[index])
        }
    }

    fun setBatchCompletedCount(batchId: String, completedCount: Int) {
        val batchIndex = progressBatches.indexOfFirst { it.id == batchId }
        if (batchIndex < 0) return
        val current = progressBatches[batchIndex]
        progressBatches[batchIndex] = current.copy(
            completedCount = completedCount.coerceIn(current.completedCount, current.totalCount)
        )
        refreshUploadProgressSummary()
    }

    fun completeTask(taskId: String, countTowardBatch: Boolean = true) {
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index < 0) return
        val task = tasks[index]
        val batchId = task.batchId
        if (countTowardBatch && batchId != null) {
            val batchIndex = progressBatches.indexOfFirst { it.id == batchId }
            if (batchIndex >= 0) {
                val current = progressBatches[batchIndex]
                progressBatches[batchIndex] = current.copy(
                    completedCount = (current.completedCount + 1).coerceAtMost(current.totalCount)
                )
                refreshUploadProgressSummary()
            }
        }
        tasks.removeAt(index)
        cleanupFinishedUploadBatches()
        if (tasks.isEmpty() && progressBatches.isEmpty()) {
            showDialog = false
        }
    }

    fun cleanupFinishedUploadBatches() {
        for (index in progressBatches.lastIndex downTo 0) {
            val batch = progressBatches[index]
            if (batch.completedCount >= batch.totalCount) {
                progressBatches.removeAt(index)
            }
        }
        refreshUploadProgressSummary()
        if (progressBatches.isEmpty()) {
            showDialog = false
        }
    }

    fun refreshUploadProgressSummary() {
        taskProgressSummary = progressBatches.toProgressSummary()
    }

    fun clear() {
        tasks.clear()
        progressBatches.clear()
        taskProgressSummary = null
        showDialog = false
    }
}

internal data class NasSingleDeleteTarget(
    val fileId: Long,
    val category: NasCategory,
    val categoryName: String,
    val onDeleted: () -> Unit
)

@Composable
fun NasScreen(
    onBack: () -> Unit,
    isVisible: Boolean = true
) {
    val ds = LocalDesignScale.current
    val density = LocalDensity.current
    val swipeStartEdgePx = with(density) { 28.dp.toPx() }
    val swipeBackThresholdPx = with(density) { 72.dp.toPx() }
    val sdkSessionManager = koinInject<SdkSessionManager>()
    val coroutineScope = rememberCoroutineScope()
    val onlineDeviceCdis by sdkSessionManager.onlineDeviceCdis.collectAsState()
    val selectedDeviceCdi by sdkSessionManager.selectedDeviceCdi.collectAsState()

    var selectedCategory by remember { mutableStateOf(NasCategory.Photos) }
    var isSearchMode by remember { mutableStateOf(false) }
    var isSearchSelectionMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var debouncedSearchQuery by remember { mutableStateOf("") }
    var isPhotoSelectionMode by remember { mutableStateOf(false) }
    var isAudioSelectionMode by remember { mutableStateOf(false) }
    var isDocumentSelectionMode by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<NasImageItem?>(null) }
    var previewImage by remember { mutableStateOf<NasImageItem?>(null) }
    var selectedAudio by remember { mutableStateOf<NasAudioItem?>(null) }
    var selectedDocument by remember { mutableStateOf<NasDocumentItem?>(null) }
    var lastPickerCategory by remember { mutableStateOf<NasCategory?>(null) }
    var lastPickedImagesSize by remember { mutableIntStateOf(0) }
    var lastPickedFilesSize by remember { mutableIntStateOf(0) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var singleDeleteTarget by remember { mutableStateOf<NasSingleDeleteTarget?>(null) }
    val selectedPhotoIds = remember { mutableStateListOf<String>() }
    val selectedAudioIds = remember { mutableStateListOf<String>() }
    val selectedDocumentIds = remember { mutableStateListOf<String>() }
    val uploadTasks = NasUploadTaskStore.tasks
    val taskProgressSummary = NasUploadTaskStore.taskProgressSummary
    val nasCacheMap by sdkSessionManager.nasCache.collectAsState()
    val searchCacheMap = remember { mutableStateMapOf<String, NasCategoryCache>() }
    val errorMap = remember { mutableStateMapOf<NasCategory, String?>() }
    val loadingMap = remember { mutableStateMapOf<NasCategory, Boolean>() }
    val loadingMoreMap = remember { mutableStateMapOf<NasCategory, Boolean>() }
    val searchErrorMap = remember { mutableStateMapOf<NasCategory, String?>() }
    val searchLoadingMap = remember { mutableStateMapOf<NasCategory, Boolean>() }
    val refreshIndicatorMap = remember { mutableStateMapOf<NasCategory, Boolean>() }
    val searchRefreshIndicatorMap = remember { mutableStateMapOf<NasCategory, Boolean>() }
    val targetCdi = selectedDeviceCdi ?: onlineDeviceCdis.firstOrNull() ?: ""
    val mediaController = rememberPlatformMediaAccessController(
        onEvent = { message -> println("NAS Media Event: $message") }
    )
    val imeInsets = WindowInsets.ime
    val detailEnter = slideInHorizontally(
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        initialOffsetX = { it / 3 }
    ) + fadeIn(animationSpec = tween(durationMillis = 240))
    val detailExit = slideOutHorizontally(
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        targetOffsetX = { it / 3 }
    ) + fadeOut(animationSpec = tween(durationMillis = 180))
    val rememberedSelectedImage = remember { mutableStateOf<NasImageItem?>(null) }
    val rememberedSelectedAudio = remember { mutableStateOf<NasAudioItem?>(null) }
    val rememberedSelectedDocument = remember { mutableStateOf<NasDocumentItem?>(null) }
    val photoScrollState = rememberScrollState()
    val audioScrollState = rememberScrollState()
    val documentScrollState = rememberScrollState()
    val searchInputScrollState = rememberScrollState()
    val activeScrollState = when (selectedCategory) {
        NasCategory.Photos -> photoScrollState
        NasCategory.Recordings -> audioScrollState
        NasCategory.Documents -> documentScrollState
    }
    val imeBottomPx = imeInsets.getBottom(density)
    var searchFieldFocused by remember { mutableStateOf(false) }
    var pendingImeScroll by remember { mutableStateOf(false) }
    var hasBeenVisible by remember { mutableStateOf(false) }
    var visibilityRefreshTick by remember { mutableIntStateOf(0) }
    val topSelectionActionTextStyle = remember(ds) {
        TextStyle(
            fontSize = ds.sp(16f),
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.W600,
            lineHeight = TextUnit(0f, TextUnitType.Unspecified)
        )
    }
    val topSelectionActionPadding = remember(ds) {
        PaddingValues(horizontal = ds.sm(16.dp), vertical = ds.sm(9.dp))
    }
    val searchInputShape = remember(ds) { RoundedCornerShape(ds.sm(100.dp)) }
    val searchActive = isSearchMode && debouncedSearchQuery.isNotBlank()
    val imageItems = remember(nasCacheMap["image"], searchCacheMap["image"], searchActive) {
        val source = if (searchActive) searchCacheMap["image"] else nasCacheMap["image"]
        source?.items?.map { it.toNasImageItem() } ?: emptyList()
    }
    val audioItems = remember(nasCacheMap["audio"], searchCacheMap["audio"], searchActive) {
        val source = if (searchActive) searchCacheMap["audio"] else nasCacheMap["audio"]
        source?.items?.map { it.toNasAudioItem() } ?: emptyList()
    }
    val documentItems = remember(nasCacheMap["doc"], searchCacheMap["doc"], searchActive) {
        val source = if (searchActive) searchCacheMap["doc"] else nasCacheMap["doc"]
        source?.items?.map { it.toNasDocumentItem() } ?: emptyList()
    }

    if (selectedImage != null) rememberedSelectedImage.value = selectedImage
    if (selectedAudio != null) rememberedSelectedAudio.value = selectedAudio
    if (selectedDocument != null) rememberedSelectedDocument.value = selectedDocument

    LaunchedEffect(searchFieldFocused, pendingImeScroll, imeBottomPx, activeScrollState.maxValue) {
        if (!searchFieldFocused || !pendingImeScroll) return@LaunchedEffect
        if (imeBottomPx <= 0 || activeScrollState.maxValue <= 0) return@LaunchedEffect

        delay(180)
        if (!searchFieldFocused || !pendingImeScroll) return@LaunchedEffect
        if (imeBottomPx <= 0 || activeScrollState.maxValue <= 0) return@LaunchedEffect

        if (activeScrollState.maxValue > activeScrollState.value) {
            activeScrollState.animateScrollTo(activeScrollState.maxValue)
        }
        pendingImeScroll = false
    }

    fun appendUploadTasks(tasks: List<NasUploadTaskItem>) {
        NasUploadTaskStore.append(tasks)
    }

    fun replaceUploadTask(taskId: String, transform: (NasUploadTaskItem) -> NasUploadTaskItem) {
        NasUploadTaskStore.replace(taskId, transform)
    }

    /** 下载单个 NAS 文件，写入任务进度列表 */
    fun downloadNasFile(fileId: Long?, fileName: String, mimeType: String) {
        if (fileId == null) return
        val taskId = "nas_dl_${currentTimeMillisSafe()}_${fileName.hashCode().toString().replace('-', '0')}"
        val taskType = mimeType.toDownloadTaskType()
        val task = NasUploadTaskItem(
            id = taskId,
            title = fileName,
            type = taskType,
            progress = 0f,
            status = NasUploadTaskStatus.Downloading,
            direction = NasTaskDirection.Download,
        )
        NasUploadTaskStore.append(listOf(task))

        NasUploadTaskStore.scope.launch {
            runCatching {
                // 阶段1: 获取文件信息
                replaceUploadTask(taskId) { it.copy(status = NasUploadTaskStatus.Downloading, progress = 0.10f) }
                val getResponse = sdkSessionManager.getFileFromNas(
                    targetCdi = targetCdi,
                    fileId = fileId,
                ).getOrThrow()
                val blobRef = getResponse.item?.blobRef
                    ?: throw IllegalStateException("文件详情缺少 blobRef")

                // 阶段2: 下载 blob 数据
                replaceUploadTask(taskId) { it.copy(status = NasUploadTaskStatus.Downloading, progress = 0.30f) }
                val bytes = sdkSessionManager.fetchBlobBytes(blobRef).getOrThrow()

                // 阶段3: 保存到本地
                replaceUploadTask(taskId) { it.copy(status = NasUploadTaskStatus.Saving, progress = 0.85f) }
                platformSaveFile(bytes, fileName, mimeType)
            }.onSuccess {
                NasUploadTaskStore.completeTask(taskId)
            }.onFailure {
                replaceUploadTask(taskId) { it.copy(status = NasUploadTaskStatus.Failed) }
            }
        }
    }

    /** 批量下载 NAS 文件 */
    fun downloadNasFiles(items: List<Triple<Long?, String, String>>) {
        val validItems = items.filter { it.first != null }
        if (validItems.isEmpty()) return

        val preparedTasks = validItems.mapIndexed { index, (_, fileName, mimeType) ->
            NasUploadTaskItem(
                id = "nas_dl_${currentTimeMillisSafe()}_${index}_${fileName.hashCode().toString().replace('-', '0')}",
                title = fileName,
                type = mimeType.toDownloadTaskType(),
                progress = 0f,
                status = if (index == 0) NasUploadTaskStatus.Downloading else NasUploadTaskStatus.Waiting,
                direction = NasTaskDirection.Download,
            )
        }
        NasUploadTaskStore.append(preparedTasks)

        preparedTasks.forEachIndexed { index, task ->
            val (fileId, fileName, mimeType) = validItems[index]
            NasUploadTaskStore.scope.launch {
                runCatching {
                    replaceUploadTask(task.id) { it.copy(status = NasUploadTaskStatus.Downloading, progress = 0.10f) }
                    val getResponse = sdkSessionManager.getFileFromNas(
                        targetCdi = targetCdi,
                        fileId = fileId!!,
                    ).getOrThrow()
                    val blobRef = getResponse.item?.blobRef
                        ?: throw IllegalStateException("文件详情缺少 blobRef")

                    replaceUploadTask(task.id) { it.copy(status = NasUploadTaskStatus.Downloading, progress = 0.30f) }
                    val bytes = sdkSessionManager.fetchBlobBytes(blobRef).getOrThrow()

                    replaceUploadTask(task.id) { it.copy(status = NasUploadTaskStatus.Saving, progress = 0.85f) }
                    platformSaveFile(bytes, fileName, mimeType)
                }.onSuccess {
                    NasUploadTaskStore.completeTask(task.id)
                }.onFailure {
                    replaceUploadTask(task.id) { it.copy(status = NasUploadTaskStatus.Failed) }
                }
            }
        }
    }

    fun clearAllNasListState() {
        sdkSessionManager.clearNasCache()
        errorMap.clear()
        loadingMap.clear()
        loadingMoreMap.clear()
    }

    fun clearNasSearchState(category: NasCategory? = null) {
        if (category == null) {
            searchCacheMap.clear()
            searchErrorMap.clear()
            searchLoadingMap.clear()
            return
        }
        val kind = category.toNasListKind()
        searchCacheMap.remove(kind)
        searchErrorMap.remove(category)
        searchLoadingMap.remove(category)
    }

    fun requestNasList(
        category: NasCategory,
        loadMore: Boolean = false,
        showRefreshIndicator: Boolean = false,
    ) {
        if (loadingMap[category] == true || loadingMoreMap[category] == true) return
        val kind = category.toNasListKind()
        val pageSize = category.nasPageSize()
        val cachedCursor = nasCacheMap[kind]?.nextCursor
        val cursor = if (loadMore) cachedCursor else null
        if (loadMore && cursor.isNullOrBlank()) return

        if (loadMore) {
            loadingMoreMap[category] = true
        } else {
            loadingMap[category] = true
            refreshIndicatorMap[category] = showRefreshIndicator
            errorMap.remove(category)
        }

        coroutineScope.launch {
            sdkSessionManager
                .listFilesFromNas(
                    targetCdi = targetCdi,
                    kind = kind,
                    pageSize = pageSize,
                    cursor = cursor,
                )
                .onSuccess { response ->
                    val responseError = response.error?.takeIf { it.isNotBlank() }
                    if (responseError != null) {
                        errorMap[category] = responseError
                        return@onSuccess
                    }
                    errorMap.remove(category)
                    sdkSessionManager.updateNasCache(kind) { cache ->
                        cache.copy(
                            items = if (loadMore) cache.items + response.items else response.items,
                            nextCursor = response.nextCursor,
                            hasLoaded = true,
                            refreshVersion = currentTimeMillisSafe(),
                        )
                    }
                }
                .onFailure { error ->
                    errorMap[category] = error.message ?: "加载失败"
                }

            loadingMap[category] = false
            loadingMoreMap[category] = false
            refreshIndicatorMap[category] = false
        }
    }

    fun requestNasSearch(
        category: NasCategory,
        keyword: String,
        showRefreshIndicator: Boolean = false,
    ) {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isBlank()) {
            clearNasSearchState(category)
            return
        }
        if (searchLoadingMap[category] == true) return

        val kind = category.toNasListKind()
        searchLoadingMap[category] = true
        searchRefreshIndicatorMap[category] = showRefreshIndicator
        searchErrorMap.remove(category)

        coroutineScope.launch {
            sdkSessionManager
                .searchFilesFromNas(
                    targetCdi = targetCdi,
                    kind = kind,
                    keyword = normalizedKeyword,
                )
                .onSuccess { response ->
                    if (
                        !isSearchMode ||
                        selectedCategory != category ||
                        debouncedSearchQuery != normalizedKeyword
                    ) {
                        return@onSuccess
                    }
                    val responseError = response.error?.takeIf { it.isNotBlank() }
                    if (responseError != null) {
                        searchErrorMap[category] = responseError
                        searchCacheMap.remove(kind)
                        return@onSuccess
                    }
                    searchErrorMap.remove(category)
                    searchCacheMap[kind] =
                        NasCategoryCache(
                            items = response.items,
                            nextCursor = response.nextCursor,
                            hasLoaded = true,
                            refreshVersion = currentTimeMillisSafe(),
                        )
                }
                .onFailure { error ->
                    if (
                        isSearchMode &&
                        selectedCategory == category &&
                        debouncedSearchQuery == normalizedKeyword
                    ) {
                        searchErrorMap[category] = error.message ?: "搜索失败"
                        searchCacheMap.remove(kind)
                    }
                }

            if (
                isSearchMode &&
                selectedCategory == category &&
                debouncedSearchQuery == normalizedKeyword
            ) {
                searchLoadingMap[category] = false
                searchRefreshIndicatorMap[category] = false
            } else {
                searchLoadingMap.remove(category)
                searchRefreshIndicatorMap.remove(category)
            }
        }
    }

    /** 上传成功后刷新：先拉取新数据，成功后再替换缓存，避免清空造成闪白 */
    fun refreshNasListAfterUpload(category: NasCategory) {
        val kind = category.toNasListKind()
        val pageSize = category.nasPageSize()
        NasUploadTaskStore.scope.launch {
            sdkSessionManager
                .listFilesFromNas(
                    targetCdi = targetCdi,
                    kind = kind,
                    pageSize = pageSize,
                    cursor = null,
                )
                .onSuccess { response ->
                    val responseError = response.error?.takeIf { it.isNotBlank() }
                    if (responseError != null) return@onSuccess
                    sdkSessionManager.updateNasCache(kind) { _ ->
                        NasCategoryCache(
                            items = response.items,
                            nextCursor = response.nextCursor,
                            hasLoaded = true,
                            refreshVersion = currentTimeMillisSafe(),
                        )
                    }
                }
        }
    }

    /** 批量删除 NAS 文件，全部完成后刷新列表 */
    fun deleteNasFiles(fileIds: List<Long?>, category: NasCategory) {
        val validIds = fileIds.filterNotNull()
        if (validIds.isEmpty()) return
        coroutineScope.launch {
            val batchId = "nas_delete_batch_${currentTimeMillisSafe()}_${category.name.lowercase()}"
            val preparedTasks = validIds.mapIndexed { index, fileId ->
                NasUploadTaskItem(
                    id = "nas_delete_${currentTimeMillisSafe()}_${index}_$fileId",
                    title = when (category) {
                        NasCategory.Photos -> "删除图片"
                        NasCategory.Recordings -> "删除音频"
                        NasCategory.Documents -> "删除文档"
                    },
                    type = category.toUploadTaskType(),
                    progress = if (index == 0) 0.2f else 0f,
                    status = if (index == 0) NasUploadTaskStatus.Deleting else NasUploadTaskStatus.Waiting,
                    direction = NasTaskDirection.Delete,
                    batchId = batchId
                )
            }
            NasUploadTaskStore.beginProgressBatch(batchId = batchId, totalCount = preparedTasks.size)
            NasUploadTaskStore.append(preparedTasks)
            var anySuccess = false
            preparedTasks.forEachIndexed { index, task ->
                val fid = validIds[index]
                replaceUploadTask(task.id) {
                    it.copy(status = NasUploadTaskStatus.Deleting, progress = 0.35f)
                }
                sdkSessionManager.deleteFileFromNas(
                    targetCdi = targetCdi,
                    fileId = fid,
                ).onSuccess { response ->
                    if (response.ok) {
                        anySuccess = true
                        NasUploadTaskStore.completeTask(task.id)
                    } else {
                        println("NAS 删除失败 fileId=$fid: ${response.error ?: "unknown"}")
                        replaceUploadTask(task.id) {
                            it.copy(status = NasUploadTaskStatus.Failed, progress = 0.35f)
                        }
                    }
                }.onFailure { error ->
                    println("NAS 删除异常 fileId=$fid: ${error.message ?: "unknown"}")
                    replaceUploadTask(task.id) {
                        it.copy(status = NasUploadTaskStatus.Failed, progress = 0.35f)
                    }
                }
            }
            if (anySuccess) {
                refreshNasListAfterUpload(category)
            }
        }
    }

    /** 删除单个 NAS 文件，成功后刷新列表 */
    fun deleteNasFile(fileId: Long?, category: NasCategory) {
        if (fileId == null) return
        deleteNasFiles(listOf(fileId), category)
    }

    fun queueDeleteConfirmation(
        fileId: Long?,
        category: NasCategory,
        categoryName: String,
        onDeleted: () -> Unit
    ) {
        val validFileId = fileId ?: return
        singleDeleteTarget = NasSingleDeleteTarget(
            fileId = validFileId,
            category = category,
            categoryName = categoryName,
            onDeleted = onDeleted
        )
    }

    fun submitSingleNasSendItem(
        item: NasSendItem?,
        afterSubmit: () -> Unit = {}
    ) {
        if (item == null) return
        NasSendToChatStore.submit(listOf(item))
        afterSubmit()
        onBack()
    }

    fun launchBatchUpload(
        category: NasCategory,
        items: List<Pair<String, String>>,
    ) {
        if (items.isEmpty()) return

        val batchId = "nas_upload_batch_${currentTimeMillisSafe()}_${category.name.lowercase()}"
        val preparedTasks =
            items.mapIndexed { index, (uri, displayName) ->
                NasUploadTaskItem(
                    id = "nas_upload_${currentTimeMillisSafe()}_${index}_${displayName.hashCode().toString().replace('-', '0')}",
                    title = displayName,
                    type = category.toUploadTaskType(),
                    progress = if (index == 0) 0.08f else 0f,
                    status = if (index == 0) NasUploadTaskStatus.Uploading else NasUploadTaskStatus.Waiting,
                    batchId = batchId
                )
            }
        NasUploadTaskStore.beginProgressBatch(batchId = batchId, totalCount = preparedTasks.size)
        appendUploadTasks(preparedTasks)

        NasUploadTaskStore.scope.launch {
            val uploadPayloads = mutableListOf<TransferUploadItem>()

            preparedTasks.forEachIndexed { index, task ->
                val uri = items[index].first
                println("[NasUpload] 开始读取文件 uri=$uri taskId=${task.id}")
                val bytes = mediaController.readUriToBytes(uri)
                println("[NasUpload] 读取结果 uri=$uri bytes=${bytes?.size ?: "null"}")
                if (bytes == null || bytes.isEmpty()) {
                    println("[NasUpload] 文件读取失败(null或空), 标记为 Failed, uri=$uri")
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
                        println(
                            "[NasUpload] onProgress " +
                                "v=${frame.v} phase=${frame.phase} transferId=${frame.transferId} " +
                                "completedEntries=${frame.completedEntries}/${frame.totalEntries} " +
                                "currentEntryId=${frame.currentEntryId} " +
                                "currentBytesFetched=${frame.currentBytesFetched} overallBytesFetched=${frame.overallBytesFetched} " +
                                "currentPctApprox=${frame.currentPctApprox} overallPctApprox=${frame.overallPctApprox}"
                        )
                        val orderedIds = uploadPayloads.map { it.entryId }
                        val byteSizeMap = uploadPayloads.associate { it.entryId to it.bytes.size.toLong() }
                        val completedCount = frame.completedEntries.coerceIn(0, orderedIds.size)
                        NasUploadTaskStore.setBatchCompletedCount(batchId, completedCount)
                        orderedIds.forEachIndexed { index, entryId ->
                            when {
                                index < completedCount -> {
                                    replaceUploadTask(entryId) {
                                        it.copy(status = NasUploadTaskStatus.Uploading, progress = 0.90f)
                                    }
                                }
                                index == completedCount -> {
                                    val totalBytes = byteSizeMap[entryId] ?: 1L
                                    val fetched = if (frame.currentEntryId == entryId) {
                                        frame.currentBytesFetched ?: 0L
                                    } else {
                                        0L
                                    }
                                    val ratio = (fetched.toFloat() / totalBytes.coerceAtLeast(1L)).coerceIn(0f, 1f)
                                    replaceUploadTask(entryId) {
                                        it.copy(status = NasUploadTaskStatus.Uploading, progress = ratio * 0.90f)
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
                            var hasAnySuccess = false
                            registerItems.forEach { registerItem ->
                                val registerResult = resultMap[registerItem.blobRef]
                                val ok = registerResult?.ok == true
                                if (ok) hasAnySuccess = true
                                if (ok) {
                                    NasUploadTaskStore.completeTask(
                                        taskId = registerItem.entryId.orEmpty(),
                                        countTowardBatch = false
                                    )
                                } else {
                                    replaceUploadTask(registerItem.entryId.orEmpty()) { current ->
                                        current.copy(
                                            status = NasUploadTaskStatus.Failed,
                                            progress = current.progress.coerceAtLeast(0.96f),
                                        )
                                    }
                                }
                            }
                            if (hasAnySuccess) {
                                refreshNasListAfterUpload(category)
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
                .onFailure { error ->
                    println("[NasUpload] sendFilesToDevice 失败: ${error.message}")
                    error.printStackTrace()
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
            delay(300)
            val latestSize = mediaController.pickedImages.size
            val newUris = mediaController.pickedImages.take(latestSize - lastPickedImagesSize)
            val items =
                newUris
                    .filter { it.isNotBlank() }
                    .map { uri -> uri to deriveUploadDisplayName(uri, defaultPrefix = "图片") }
                    .distinctBy { it.first }
            launchBatchUpload(
                category = NasCategory.Photos,
                items = items,
            )
            lastPickedImagesSize = latestSize
        } else {
            lastPickedImagesSize = size
        }
    }

    LaunchedEffect(mediaController.pickedFiles.size, lastPickerCategory) {
        val size = mediaController.pickedFiles.size
        val pickerCategory = lastPickerCategory
        if (size > lastPickedFilesSize && pickerCategory != null && pickerCategory != NasCategory.Photos) {
            delay(300)
            val latestSize = mediaController.pickedFiles.size
            val newFiles = mediaController.pickedFiles.take(latestSize - lastPickedFilesSize)
            val items =
                newFiles
                    .filter { it.uri.isNotBlank() }
                    .map { file -> file.uri to file.displayName.ifBlank { deriveUploadDisplayName(file.uri, defaultPrefix = pickerCategory.title) } }
                    .distinctBy { it.first }
            launchBatchUpload(
                category = pickerCategory,
                items = items,
            )
            lastPickedFilesSize = latestSize
        } else {
            lastPickedFilesSize = size
        }
    }

    LaunchedEffect(targetCdi) {
        if (sdkSessionManager.clearNasCacheIfCdiChanged(targetCdi)) {
            errorMap.clear()
            loadingMap.clear()
            loadingMoreMap.clear()
        }
        clearNasSearchState()
        debouncedSearchQuery = ""
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) return@LaunchedEffect
        if (hasBeenVisible) {
            visibilityRefreshTick += 1
        } else {
            hasBeenVisible = true
        }
    }

    LaunchedEffect(selectedCategory, targetCdi, isVisible, visibilityRefreshTick) {
        if (!isVisible || targetCdi.isBlank()) return@LaunchedEffect
        val kind = selectedCategory.toNasListKind()
        val shouldForceRefresh = visibilityRefreshTick > 0
        if (!shouldForceRefresh && nasCacheMap[kind]?.hasLoaded == true) return@LaunchedEffect
        requestNasList(selectedCategory, loadMore = false)
    }

    LaunchedEffect(isSearchMode, selectedCategory, targetCdi, searchQuery) {
        if (!isSearchMode) {
            debouncedSearchQuery = ""
            return@LaunchedEffect
        }
        val normalizedQuery = searchQuery.trim()
        if (normalizedQuery.isEmpty()) {
            debouncedSearchQuery = ""
            clearNasSearchState(selectedCategory)
            return@LaunchedEffect
        }

        delay(350)
        val latestQuery = searchQuery.trim()
        if (!isSearchMode || latestQuery != normalizedQuery) return@LaunchedEffect

        debouncedSearchQuery = latestQuery
        requestNasSearch(selectedCategory, latestQuery)
    }

    val imageMonths = remember(imageItems) { imageItems.toImageMonthGroups() }
    val audios = remember(audioItems) { audioItems.toAudioMonthGroups() }
    val documents = remember(documentItems) { documentItems.toDocumentMonthGroups() }
    val allImages = imageItems
    val allPhotoIds = remember(imageItems) { imageItems.map { it.id } }
    val allAudioIds = remember(audioItems) { audioItems.map { it.id } }
    val allDocumentIds = remember(documentItems) { documentItems.map { it.id } }

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
                debouncedSearchQuery = ""
                clearNasSearchState()
            }
            isPhotoSelectionMode || isAudioSelectionMode || isDocumentSelectionMode -> exitAllSelectionModes()
            else -> onBack()
        }
    }

    PlatformBackHandler(onBack = ::handleNasBack)

    val isCurrentSelectionMode = when (selectedCategory) {
        NasCategory.Photos -> isPhotoSelectionMode
        NasCategory.Recordings -> isAudioSelectionMode
        NasCategory.Documents -> isDocumentSelectionMode
    }
    val currentCategoryError = if (searchActive) searchErrorMap[selectedCategory] else errorMap[selectedCategory]
    val currentCategoryLoading = if (searchActive) searchLoadingMap[selectedCategory] == true else loadingMap[selectedCategory] == true
    val currentCategoryLoadingMore = if (searchActive) false else loadingMoreMap[selectedCategory] == true
    val currentRefreshIndicator = if (searchActive) {
        searchRefreshIndicatorMap[selectedCategory] == true
    } else {
        refreshIndicatorMap[selectedCategory] == true
    }
    val currentCategoryHasMore = if (searchActive) false else !nasCacheMap[selectedCategory.toNasListKind()]?.nextCursor.isNullOrBlank()
    val currentSearchEmptyText = when {
        currentCategoryLoading -> null
        currentCategoryError != null -> currentCategoryError
        searchActive -> "暂无搜索结果"
        else -> null
    }
    val currentSearchEmptyTextColor = when {
        searchActive && currentCategoryError != null -> Color.Black.copy(alpha = 0.90f)
        else -> Color.White.copy(alpha = 0.72f)
    }

    LaunchedEffect(
        selectedCategory,
        isVisible,
        searchActive,
        currentCategoryLoading,
        currentCategoryLoadingMore,
        currentCategoryHasMore,
        activeScrollState.value,
        activeScrollState.maxValue,
    ) {
        if (!isVisible || searchActive) return@LaunchedEffect
        if (currentCategoryLoading || currentCategoryLoadingMore || !currentCategoryHasMore) return@LaunchedEffect

        val remainingPx = activeScrollState.maxValue - activeScrollState.value
        val loadMoreThresholdPx = with(density) { 180.dp.roundToPx() }
        val shouldLoadMore = remainingPx <= loadMoreThresholdPx
        if (shouldLoadMore) {
            requestNasList(selectedCategory, loadMore = true)
        }
    }

    val contentBottomPadding = ds.sm(
        when {
            isSearchMode -> 120.dp
            isCurrentSelectionMode -> 112.dp
            else -> 96.dp
        }
    )
    val currentCategoryFooter: @Composable (() -> Unit) = {
        val footerTextColor = Color(0xB3000000)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ds.sm(10.dp))
        ) {
            if (currentCategoryLoading) {
                Text(
                    text = "加载中...",
                    style = TextStyle(color = footerTextColor, fontSize = ds.sp(13f))
                )
            }
            if (!currentCategoryLoading && currentCategoryError != null) {
                Text(
                    text = currentCategoryError,
                    style = TextStyle(color = footerTextColor, fontSize = ds.sp(13f))
                )
                NasGlassTextButton(
                    text = if (searchActive) "重新搜索" else if (nasCacheMap[selectedCategory.toNasListKind()]?.hasLoaded == true) "重新加载" else "重试",
                    onClick = {
                        if (searchActive) {
                            requestNasSearch(selectedCategory, debouncedSearchQuery)
                        } else {
                            requestNasList(selectedCategory, loadMore = false)
                        }
                    },
                    modifier = Modifier.width(ds.sm(132.dp))
                )
            } else if (!currentCategoryLoading && currentCategoryLoadingMore) {
                Text(
                    text = "加载中...",
                    style = TextStyle(color = footerTextColor, fontSize = ds.sp(13f))
                )
            }
        }
    }

    Scaffold(containerColor = Color(0xFFFAFAFC)) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // .background(Color(0xFFFAFAFC))
                .imePadding()
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
                        Spacer(modifier = Modifier.height(ds.sm(72.dp)))
                        Text(
                            text = if (isSearchSelectionMode) {
                                "最近搜索"
                            } else {
                                "正在寻找关于“${searchQuery.ifBlank { "" }}”的${selectedCategory.title}"
                            },
                            style = TextStyle(
                                color = Color(0xE6000000),
                                fontSize = ds.sp(16f),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(ds.sm(8.dp)))
                    }

                    PullToRefreshBox(
                        isRefreshing = currentRefreshIndicator,
                        onRefresh = {
                            if (searchActive) {
                                requestNasSearch(
                                    selectedCategory,
                                    debouncedSearchQuery,
                                    showRefreshIndicator = true,
                                )
                            } else {
                                requestNasList(
                                    selectedCategory,
                                    loadMore = false,
                                    showRefreshIndicator = true,
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        when (selectedCategory) {
                            NasCategory.Photos -> NasPhotosContent(
                                imageMonths = imageMonths,
                                bottomPadding = contentBottomPadding,
                                scrollState = photoScrollState,
                                showMonthHeaders = !isSearchMode,
                                selectionMode = isPhotoSelectionMode,
                                selectedImageIds = selectedPhotoIds,
                                onImageClick = { image -> selectedImage = image },
                                onImageLongClick = { image -> previewImage = image },
                                onImageSelectionToggle = { image -> togglePhotoSelection(image) },
                                emptyText = if (searchActive) currentSearchEmptyText else if (currentCategoryLoading) null else "暂无图片",
                                emptyTextColor = currentSearchEmptyTextColor,
                                footer = if (isSearchMode) null else currentCategoryFooter,
                            )
                            NasCategory.Recordings -> NasRecordingsContent(
                                audioMonths = audios,
                                bottomPadding = contentBottomPadding,
                                scrollState = audioScrollState,
                                selectionMode = isAudioSelectionMode,
                                selectedAudioIds = selectedAudioIds,
                                onAudioClick = { audio -> selectedAudio = audio },
                                onAudioSelectionToggle = { audio -> toggleAudioSelection(audio) },
                                emptyText = if (searchActive) currentSearchEmptyText else if (currentCategoryLoading) null else "暂无音频",
                                emptyTextColor = currentSearchEmptyTextColor,
                                footer = if (isSearchMode) null else currentCategoryFooter,
                            )
                            NasCategory.Documents -> NasDocumentsContent(
                                documentMonths = documents,
                                bottomPadding = contentBottomPadding,
                                scrollState = documentScrollState,
                                selectionMode = isDocumentSelectionMode,
                                selectedDocumentIds = selectedDocumentIds,
                                onDocumentClick = { document -> selectedDocument = document },
                                onDocumentSelectionToggle = { document -> toggleDocumentSelection(document) },
                                emptyText = if (searchActive) currentSearchEmptyText else if (currentCategoryLoading) null else "暂无文档",
                                emptyTextColor = currentSearchEmptyTextColor,
                                footer = if (isSearchMode) null else currentCategoryFooter,
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
                    .padding(top = 9.dp, bottom = 9.dp)
            ) {
                if (isSearchMode) {
                    if (isSearchSelectionMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedCategory == NasCategory.Photos) {
                                NasLightPillButton(
                                    text = "全选",
                                    onClick = {
                                        if (selectedPhotoIds.size == allPhotoIds.size) {
                                            selectedPhotoIds.clear()
                                        } else {
                                            selectedPhotoIds.clear()
                                            selectedPhotoIds.addAll(allPhotoIds)
                                        }
                                    },
                                    textStyle = TextStyle(
                                        fontSize = ds.sp(18f),
                                        fontStyle = FontStyle.Normal,
                                        fontWeight = FontWeight.W600,
                                        lineHeight = TextUnit(0f, TextUnitType.Unspecified)
                                    )
                                )
                                NasBottomQuickActionIconButton(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "关闭选择",
                                    onClick = {
                                        isSearchSelectionMode = false
                                        exitAllSelectionModes()
                                    },
                                    modifier = Modifier.size(ds.sm(40.dp))
                                )
                            } else {
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
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(ds.sm(44.dp)))
                            if (selectedCategory == NasCategory.Photos) {
                                NasLightPillButton(
                                    text = "选择",
                                    onClick = {
                                        isSearchSelectionMode = true
                                        exitAllSelectionModes()
                                        isPhotoSelectionMode = true
                                        selectedPhotoIds.clear()
                                    },
                                    modifier = Modifier.width(ds.sw(140.dp)),
                                    textStyle = TextStyle(
                                        fontSize = ds.sp(18f),
                                        fontStyle = FontStyle.Normal,
                                        fontWeight = FontWeight.W600,
                                        lineHeight = TextUnit(0f, TextUnitType.Unspecified)
                                    )
                                )
                            } else {
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
                    }
                } else if (isCurrentSelectionMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NasLightPillButton(
                            text = "发送脑花",
                            onClick = {
                                val items = when (selectedCategory) {
                                    NasCategory.Photos -> imageItems
                                        .filter { it.id in selectedPhotoIds && it.fileId != null }
                                        .map {
                                            NasSendItem(
                                                fileId = it.fileId!!,
                                                fileName = it.name,
                                                fileType = NasSendFileType.Image,
                                                previewBlobRef = it.path,
                                                sizeKB = it.sizeKB,
                                                format = it.format,
                                            )
                                        }
                                    NasCategory.Recordings -> audioItems
                                        .filter { it.id in selectedAudioIds && it.fileId != null }
                                        .map {
                                            NasSendItem(
                                                fileId = it.fileId!!,
                                                fileName = it.name,
                                                fileType = NasSendFileType.Audio,
                                                previewBlobRef = it.path,
                                                sizeKB = it.sizeKB,
                                                format = it.format,
                                            )
                                        }
                                    NasCategory.Documents -> documentItems
                                        .filter { it.id in selectedDocumentIds && it.fileId != null }
                                        .map {
                                            NasSendItem(
                                                fileId = it.fileId!!,
                                                fileName = it.name,
                                                fileType = NasSendFileType.Document,
                                                previewBlobRef = it.path,
                                                sizeKB = it.sizeKB,
                                                format = it.format,
                                            )
                                        }
                                }
                                if (items.isNotEmpty()) {
                                    NasSendToChatStore.submit(items)
                                    exitAllSelectionModes()
                                    onBack()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(ds.sh(40.dp)),
                            textStyle = topSelectionActionTextStyle,
                            contentPadding = topSelectionActionPadding
                        )
                        NasLightPillButton(
                            text = "下载",
                            onClick = {
                                val downloadItems: List<Triple<Long?, String, String>> = when (selectedCategory) {
                                    NasCategory.Photos -> {
                                        imageItems.filter { it.id in selectedPhotoIds }
                                            .map { Triple(it.fileId, it.name, it.name.toMimeType()) }
                                    }
                                    NasCategory.Recordings -> {
                                        audioItems.filter { it.id in selectedAudioIds }
                                            .map { Triple(it.fileId, it.name, it.name.toMimeType()) }
                                    }
                                    NasCategory.Documents -> {
                                        documentItems.filter { it.id in selectedDocumentIds }
                                            .map { Triple(it.fileId, it.name, it.name.toMimeType()) }
                                    }
                                }
                                if (downloadItems.isNotEmpty()) {
                                    downloadNasFiles(downloadItems)
                                    exitAllSelectionModes()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(ds.sh(40.dp)),
                            textStyle = topSelectionActionTextStyle,
                            contentPadding = topSelectionActionPadding
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            NasLightPillButton(
                                text = "删除",
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(ds.sh(40.dp)),
                                textStyle = topSelectionActionTextStyle,
                                contentPadding = topSelectionActionPadding
                            )
                            if (showDeleteConfirm) {
                                val count = when (selectedCategory) {
                                    NasCategory.Photos -> selectedPhotoIds.size
                                    NasCategory.Recordings -> selectedAudioIds.size
                                    NasCategory.Documents -> selectedDocumentIds.size
                                }
                                val categoryName = when (selectedCategory) {
                                    NasCategory.Photos -> "张照片"
                                    NasCategory.Recordings -> "个音频"
                                    NasCategory.Documents -> "个文档"
                                }
                                NasDeleteConfirmPopup(
                                    count = count,
                                    categoryName = categoryName,
                                    onConfirm = {
                                        showDeleteConfirm = false
                                        val fileIds = when (selectedCategory) {
                                            NasCategory.Photos -> imageItems.filter { it.id in selectedPhotoIds }.map { it.fileId }
                                            NasCategory.Recordings -> audioItems.filter { it.id in selectedAudioIds }.map { it.fileId }
                                            NasCategory.Documents -> documentItems.filter { it.id in selectedDocumentIds }.map { it.fileId }
                                        }
                                        deleteNasFiles(fileIds, selectedCategory)
                                        exitAllSelectionModes()
                                    },
                                    onDismiss = { showDeleteConfirm = false }
                                )
                            }
                        }
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
                                debouncedSearchQuery = ""
                                clearNasSearchState()
                                if (isVisible && targetCdi.isNotBlank()) {
                                    requestNasList(it, loadMore = false)
                                }
                            }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .then(
                        if (isSearchMode || isCurrentSelectionMode) {
                            Modifier
                                .padding(bottom = 16.dp)
                                .navigationBarsPadding()
                                .imePadding()
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (isSearchMode) {
                    if (isSearchSelectionMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedCategory == NasCategory.Photos) {
                                NasLightPillButton(
                                    text = "发送脑花",
                                    onClick = {
                                        val items = imageItems
                                            .filter { it.id in selectedPhotoIds && it.fileId != null }
                                            .map {
                                                NasSendItem(
                                                    fileId = it.fileId!!,
                                                    fileName = it.name,
                                                    fileType = NasSendFileType.Image,
                                                    previewBlobRef = it.path,
                                                    sizeKB = it.sizeKB,
                                                    format = it.format,
                                                )
                                            }
                                        if (items.isNotEmpty()) {
                                            NasSendToChatStore.submit(items)
                                            exitAllSelectionModes()
                                            isSearchSelectionMode = false
                                            isSearchMode = false
                                            searchQuery = ""
                                            debouncedSearchQuery = ""
                                            clearNasSearchState()
                                            onBack()
                                        }
                                    },
                                    modifier = Modifier.width(140.dp),
                                    textStyle = TextStyle(
                                        fontSize = ds.sp(18f),
                                        fontStyle = FontStyle.Normal,
                                        fontWeight = FontWeight.W600,
                                        lineHeight = TextUnit(0f, TextUnitType.Unspecified)
                                    )
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    NasBottomQuickActionIconButton(
                                        imageVector = Icons.Outlined.FileDownload,
                                        contentDescription = "下载",
                                        onClick = {
                                            val downloadItems = imageItems
                                                .filter { it.id in selectedPhotoIds }
                                                .map { Triple(it.fileId, it.name, it.name.toMimeType()) }
                                            if (downloadItems.isNotEmpty()) {
                                                downloadNasFiles(downloadItems)
                                                exitAllSelectionModes()
                                                isSearchSelectionMode = false
                                            }
                                        },
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Box {
                                        NasBottomQuickActionIconButton(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "删除",
                                            onClick = { showDeleteConfirm = true },
                                            modifier = Modifier.size(40.dp)
                                        )
                                        if (showDeleteConfirm) {
                                            val count = selectedPhotoIds.size
                                            val categoryName = "张照片"
                                            NasDeleteConfirmPopup(
                                                count = count,
                                                categoryName = categoryName,
                                                onConfirm = {
                                                    showDeleteConfirm = false
                                                    val fileIds = imageItems.filter { it.id in selectedPhotoIds }.map { it.fileId }
                                                    deleteNasFiles(fileIds, selectedCategory)
                                                    exitAllSelectionModes()
                                                    isSearchSelectionMode = false
                                                },
                                                onDismiss = { showDeleteConfirm = false }
                                            )
                                        }
                                    }
                                }
                            } else {
                                NasGlassTextButton(
                                    text = "发送脑花",
                                    onClick = {
                                        val items = when (selectedCategory) {
                                            NasCategory.Photos -> imageItems
                                                .filter { it.id in selectedPhotoIds && it.fileId != null }
                                                .map {
                                                    NasSendItem(
                                                        fileId = it.fileId!!,
                                                        fileName = it.name,
                                                        fileType = NasSendFileType.Image,
                                                        previewBlobRef = it.path,
                                                        sizeKB = it.sizeKB,
                                                        format = it.format,
                                                    )
                                                }
                                            NasCategory.Recordings -> audioItems
                                                .filter { it.id in selectedAudioIds && it.fileId != null }
                                                .map {
                                                    NasSendItem(
                                                        fileId = it.fileId!!,
                                                        fileName = it.name,
                                                        fileType = NasSendFileType.Audio,
                                                        previewBlobRef = it.path,
                                                        sizeKB = it.sizeKB,
                                                        format = it.format,
                                                    )
                                                }
                                            NasCategory.Documents -> documentItems
                                                .filter { it.id in selectedDocumentIds && it.fileId != null }
                                                .map {
                                                    NasSendItem(
                                                        fileId = it.fileId!!,
                                                        fileName = it.name,
                                                        fileType = NasSendFileType.Document,
                                                        previewBlobRef = it.path,
                                                        sizeKB = it.sizeKB,
                                                        format = it.format,
                                                    )
                                                }
                                        }
                                        if (items.isNotEmpty()) {
                                            NasSendToChatStore.submit(items)
                                            exitAllSelectionModes()
                                            isSearchSelectionMode = false
                                            isSearchMode = false
                                            searchQuery = ""
                                            debouncedSearchQuery = ""
                                            clearNasSearchState()
                                            onBack()
                                        }
                                    },
                                    modifier = Modifier.width(140.dp)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    NasGlassCircleButton(
                                        imageVector = Icons.Outlined.FileDownload,
                                        contentDescription = "下载",
                                        onClick = {
                                            val downloadItems: List<Triple<Long?, String, String>> = when (selectedCategory) {
                                                NasCategory.Photos -> {
                                                    imageItems.filter { it.id in selectedPhotoIds }
                                                        .map { Triple(it.fileId, it.name, it.name.toMimeType()) }
                                                }
                                                NasCategory.Recordings -> {
                                                    audioItems.filter { it.id in selectedAudioIds }
                                                        .map { Triple(it.fileId, it.name, it.name.toMimeType()) }
                                                }
                                                NasCategory.Documents -> {
                                            documentItems.filter { it.id in selectedDocumentIds }
                                                        .map { Triple(it.fileId, it.name, it.name.toMimeType()) }
                                                }
                                            }
                                            if (downloadItems.isNotEmpty()) {
                                                downloadNasFiles(downloadItems)
                                                exitAllSelectionModes()
                                                isSearchSelectionMode = false
                                            }
                                        },
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Box {
                                        NasGlassCircleButton(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "删除",
                                            onClick = { showDeleteConfirm = true },
                                            modifier = Modifier.size(44.dp)
                                        )
                                        if (showDeleteConfirm) {
                                            val count = when (selectedCategory) {
                                                NasCategory.Photos -> selectedPhotoIds.size
                                                NasCategory.Recordings -> selectedAudioIds.size
                                                NasCategory.Documents -> selectedDocumentIds.size
                                            }
                                            val categoryName = when (selectedCategory) {
                                                NasCategory.Photos -> "张照片"
                                                NasCategory.Recordings -> "个音频"
                                                NasCategory.Documents -> "个文档"
                                            }
                                            NasDeleteConfirmPopup(
                                                count = count,
                                                categoryName = categoryName,
                                                onConfirm = {
                                                    showDeleteConfirm = false
                                                    val fileIds = when (selectedCategory) {
                                                        NasCategory.Photos -> imageItems.filter { it.id in selectedPhotoIds }.map { it.fileId }
                                                        NasCategory.Recordings -> audioItems.filter { it.id in selectedAudioIds }.map { it.fileId }
                                                        NasCategory.Documents -> documentItems.filter { it.id in selectedDocumentIds }.map { it.fileId }
                                                    }
                                                    deleteNasFiles(fileIds, selectedCategory)
                                                    exitAllSelectionModes()
                                                    isSearchSelectionMode = false
                                                },
                                                onDismiss = { showDeleteConfirm = false }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                maxLines = Int.MAX_VALUE,
                                cursorBrush = SolidColor(Color.Black.copy(alpha = 0.90f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .shadow(
                                        elevation = 30.dp,
                                        shape = searchInputShape,
                                        ambientColor = Color.Black.copy(alpha = 0.10f),
                                        spotColor = Color.Black.copy(alpha = 0.10f)
                                    )
                                    .clip(searchInputShape)
                                    .verticalScroll(searchInputScrollState)
                                    .onFocusChanged { focusState ->
                                        val isFocused = focusState.isFocused
                                        if (isFocused && !searchFieldFocused) {
                                            pendingImeScroll = true
                                        } else if (!isFocused) {
                                            pendingImeScroll = false
                                        }
                                        searchFieldFocused = isFocused
                                    }
                                    .background(
                                        color = Color.White,
                                        shape = searchInputShape
                                    )
                                    .padding(horizontal = 12.dp),
                                textStyle = TextStyle(
                                    color = Color.Black.copy(alpha = 0.90f),
                                    fontSize = ds.sp(12f)
                                ),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "搜索",
                                                color = Color.Black.copy(alpha = 0.40f),
                                                fontSize = ds.sp(12f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                modifier = Modifier.size(28.dp),
                                shape = CircleShape,
                                color = Color.White
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            isSearchMode = false
                                            searchQuery = ""
                                            debouncedSearchQuery = ""
                                            clearNasSearchState()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "退出搜索",
                                        tint = Color(0xFF1F2535),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                } else if (isCurrentSelectionMode) {
                    // 选择模式：退出选择 + 占位符平衡布局
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedCategory == NasCategory.Photos) {
                            NasBottomQuickActionTextButton(
                                text = "取消选择",
                                onClick = { exitAllSelectionModes() },
                                modifier = Modifier
                                    .width(ds.sw(140.dp))
                                    .height(ds.sh(40.dp))
                            )
                        } else {
                            NasBottomQuickActionTextButton(
                                text = "取消选择",
                                onClick = { exitAllSelectionModes() },
                                modifier = Modifier
                                    .width(ds.sw(140.dp))
                                    .height(ds.sh(40.dp))
                            )
                        }
                        Spacer(modifier = Modifier.size(width = ds.sw(96.dp), height = ds.sh(44.dp)))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 44.dp)
                    ) {
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

        val hasActiveTaskProgress = taskProgressSummary?.completedCount?.let { completedCount ->
            completedCount < (taskProgressSummary.totalCount)
        } == true
        if (hasActiveTaskProgress && taskProgressSummary != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (NasUploadTaskStore.showDialog) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 116.dp, end = 20.dp)
                            .fillMaxHeight()
                            .offset(y = 64.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        NasUploadProgressDialog(
                            tasks = uploadTasks,
                            uploadSummary = taskProgressSummary,
                            onDismiss = {
                                NasUploadTaskStore.showDialog = false
                                NasUploadTaskStore.cleanupFinishedUploadBatches()
                            }
                        )
                    }
                }

                NasUploadProgressEntry(
                    summary = taskProgressSummary,
                    onClick = { NasUploadTaskStore.showDialog = !NasUploadTaskStore.showDialog },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 116.dp, end = 20.dp)
                )
            }
        }
    }

    AnimatedVisibility(
        visible = selectedImage != null,
        enter = detailEnter,
        exit = detailExit,
    ) {
        val image = rememberedSelectedImage.value
        if (image != null) {
            NasImageDetailScreen(
                images = allImages,
                initialImageId = image.id,
                targetCdi = targetCdi,
                onBack = ::handleNasBack,
                onShare = { currentImage ->
                    submitSingleNasSendItem(
                        item = currentImage.fileId?.let {
                            NasSendItem(
                                fileId = it,
                                fileName = currentImage.name,
                                fileType = NasSendFileType.Image,
                                previewBlobRef = currentImage.path,
                                sizeKB = currentImage.sizeKB,
                                format = currentImage.format,
                            )
                        }
                    )
                },
                onDownload = { currentImage ->
                    downloadNasFile(
                        fileId = currentImage.fileId,
                        fileName = currentImage.name,
                        mimeType = currentImage.name.toMimeType(),
                    )
                },
                onDelete = { currentImage ->
                    queueDeleteConfirmation(
                        fileId = currentImage.fileId,
                        category = NasCategory.Photos,
                        categoryName = "张照片",
                    ) {
                        selectedImage = null
                    }
                }
            )
        }
    }

    AnimatedVisibility(
        visible = selectedAudio != null,
        enter = detailEnter,
        exit = detailExit,
    ) {
        val audio = rememberedSelectedAudio.value
        if (audio != null) {
            NasAudioDetailScreen(
                audio = audio,
                targetCdi = targetCdi,
                mediaController = mediaController,
                onBack = ::handleNasBack,
                onShare = {
                    submitSingleNasSendItem(
                        item = audio.fileId?.let {
                            NasSendItem(
                                fileId = it,
                                fileName = audio.name,
                                fileType = NasSendFileType.Audio,
                                previewBlobRef = audio.path,
                                sizeKB = audio.sizeKB,
                                format = audio.format,
                            )
                        },
                        afterSubmit = { mediaController.stopAudioPlayback() }
                    )
                },
                onDownload = {
                    downloadNasFile(
                        fileId = audio.fileId,
                        fileName = audio.name,
                        mimeType = audio.name.toMimeType(),
                    )
                },
                onDelete = {
                    queueDeleteConfirmation(
                        fileId = audio.fileId,
                        category = NasCategory.Recordings,
                        categoryName = "个音频",
                    ) {
                        mediaController.stopAudioPlayback()
                        selectedAudio = null
                    }
                }
            )
        }
    }

    AnimatedVisibility(
        visible = selectedDocument != null,
        enter = detailEnter,
        exit = detailExit,
    ) {
        val document = rememberedSelectedDocument.value
        if (document != null) {
            NasDocumentDetailScreen(
                document = document,
                targetCdi = targetCdi,
                onBack = ::handleNasBack,
                onShare = {
                    submitSingleNasSendItem(
                        item = document.fileId?.let {
                            NasSendItem(
                                fileId = it,
                                fileName = document.name,
                                fileType = NasSendFileType.Document,
                                previewBlobRef = document.path,
                                sizeKB = document.sizeKB,
                                format = document.format,
                            )
                        }
                    )
                },
                onDownload = {
                    downloadNasFile(
                        fileId = document.fileId,
                        fileName = document.name,
                        mimeType = document.name.toMimeType(),
                    )
                },
                onDelete = {
                    queueDeleteConfirmation(
                        fileId = document.fileId,
                        category = NasCategory.Documents,
                        categoryName = "个文档",
                    ) {
                        selectedDocument = null
                    }
                }
            )
        }
    }

    singleDeleteTarget?.let { target ->
        NasDeleteConfirmPopup(
            count = 1,
            categoryName = target.categoryName,
            onConfirm = {
                singleDeleteTarget = null
                deleteNasFile(target.fileId, target.category)
                target.onDeleted()
            },
            onDismiss = { singleDeleteTarget = null }
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
                downloadNasFile(
                    fileId = image.fileId,
                    fileName = image.name,
                    mimeType = image.name.toMimeType(),
                )
                previewImage = null
            },
            onDelete = {
                deleteNasFile(image.fileId, NasCategory.Photos)
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

private fun String.toDownloadTaskType(): NasUploadTaskType =
    when {
        startsWith("image/") -> NasUploadTaskType.Image
        startsWith("audio/") -> NasUploadTaskType.Audio
        else -> NasUploadTaskType.Document
    }

private fun NasCategory.toNasListKind(): String =
    when (this) {
        NasCategory.Photos -> "image"
        NasCategory.Recordings -> "audio"
        NasCategory.Documents -> "doc"
    }

private fun NasCategory.nasPageSize(): Int =
    when (this) {
        NasCategory.Photos -> NAS_PHOTO_PAGE_SIZE
        NasCategory.Recordings, NasCategory.Documents -> NAS_DEFAULT_PAGE_SIZE
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
        else -> "doc"
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
        fileId = id,
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
        fileId = id,
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
        fileId = id,
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

private const val NAS_PHOTO_PAGE_SIZE = 50
private const val NAS_DEFAULT_PAGE_SIZE = 20
