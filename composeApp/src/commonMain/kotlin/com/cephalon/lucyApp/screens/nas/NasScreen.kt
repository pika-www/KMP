package com.cephalon.lucyApp.screens.nas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.media.rememberPlatformMediaAccessController
import kotlin.math.abs

@Composable
fun NasScreen(onBack: () -> Unit) {
    val density = LocalDensity.current
    val swipeStartEdgePx = with(density) { 28.dp.toPx() }
    val swipeBackThresholdPx = with(density) { 72.dp.toPx() }

    var selectedCategory by remember { mutableStateOf(NasCategory.Photos) }
    var isPhotoSelectionMode by remember { mutableStateOf(false) }
    var isAudioSelectionMode by remember { mutableStateOf(false) }
    var isDocumentSelectionMode by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<NasImageItem?>(null) }
    var previewImage by remember { mutableStateOf<NasImageItem?>(null) }
    var selectedAudio by remember { mutableStateOf<NasAudioItem?>(null) }
    var selectedDocument by remember { mutableStateOf<NasDocumentItem?>(null) }
    val selectedPhotoIds = remember { mutableStateListOf<String>() }
    val selectedAudioIds = remember { mutableStateListOf<String>() }
    val selectedDocumentIds = remember { mutableStateListOf<String>() }
    val mediaController = rememberPlatformMediaAccessController(
        onEvent = { message -> println("NAS Media Event: $message") }
    )
    
    val imageMonths = remember {
        listOf(
            NasImageMonthGroup(
                label = "2026年4月",
                images = listOf(
                    NasImageItem(
                        id = "img_001",
                        name = "morning_walk.png",
                        type = "图片",
                        format = "png",
                        sizeKB = 2191,
                        path = "drawable/img-demo.png",
                        time = "2026-04-09 08:30",
                        location = "上海·徐汇滨江",
                        resolution = "1920x1080"
                    ),
                    NasImageItem(
                        id = "img_002",
                        name = "coffee_corner.png",
                        type = "图片",
                        format = "png",
                        sizeKB = 2187,
                        path = "drawable/img-demo.png",
                        time = "2026-04-12 10:15",
                        location = "上海·静安",
                        resolution = "1920x1080"
                    ),
                    NasImageItem(
                        id = "img_003",
                        name = "team_lunch.png",
                        type = "图片",
                        format = "png",
                        sizeKB = 2204,
                        path = "drawable/img-demo.png",
                        time = "2026-04-18 12:40",
                        location = "上海·长宁",
                        resolution = "1920x1080"
                    ),
                    NasImageItem(
                        id = "img_004",
                        name = "night_view.png",
                        type = "图片",
                        format = "png",
                        sizeKB = 2210,
                        path = "drawable/img-demo.png",
                        time = "2026-04-26 20:05",
                        location = "上海·陆家嘴",
                        resolution = "1920x1080"
                    )
                )
            ),
            NasImageMonthGroup(
                label = "2026年3月",
                images = listOf(
                    NasImageItem(
                        id = "img_005",
                        name = "spring_park.png",
                        type = "图片",
                        format = "png",
                        sizeKB = 2175,
                        path = "drawable/img-demo.png",
                        time = "2026-03-03 09:20",
                        location = "杭州·西湖",
                        resolution = "1920x1080"
                    ),
                    NasImageItem(
                        id = "img_006",
                        name = "office_board.png",
                        type = "图片",
                        format = "png",
                        sizeKB = 2158,
                        path = "drawable/img-demo.png",
                        time = "2026-03-08 14:10",
                        location = "杭州·滨江",
                        resolution = "1920x1080"
                    ),
                    NasImageItem(
                        id = "img_007",
                        name = "book_store.png",
                        type = "图片",
                        format = "png",
                        sizeKB = 2226,
                        path = "drawable/img-demo.png",
                        time = "2026-03-16 16:45",
                        location = "杭州·天目里",
                        resolution = "1920x1080"
                    ),
                    NasImageItem(
                        id = "img_008",
                        name = "weekend_market.png",
                        type = "图片",
                        format = "png",
                        sizeKB = 2234,
                        path = "drawable/img-demo.png",
                        time = "2026-03-27 11:30",
                        location = "杭州·武林路",
                        resolution = "1920x1080"
                    )
                )
            ),
            NasImageMonthGroup(
                label = "2026年2月",
                images = listOf(
                    NasImageItem(
                        id = "img_009",
                        name = "train_window.png",
                        type = "图片",
                        format = "png",
                        sizeKB = 2149,
                        path = "drawable/img-demo.png",
                        time = "2026-02-02 07:55",
                        location = "高铁上",
                        resolution = "1920x1080"
                    ),
                    NasImageItem(
                        id = "img_010",
                        name = "family_dinner.png",
                        type = "图片",
                        format = "png",
                        sizeKB = 2218,
                        path = "drawable/img-demo.png",
                        time = "2026-02-10 18:25",
                        location = "苏州·园区",
                        resolution = "1920x1080"
                    ),
                    NasImageItem(
                        id = "img_011",
                        name = "museum_day.png",
                        type = "图片",
                        format = "png",
                        sizeKB = 2199,
                        path = "drawable/img-demo.png",
                        time = "2026-02-19 15:10",
                        location = "苏州博物馆",
                        resolution = "1920x1080"
                    ),
                    NasImageItem(
                        id = "img_012",
                        name = "river_evening.png",
                        type = "图片",
                        format = "png",
                        sizeKB = 2208,
                        path = "drawable/img-demo.png",
                        time = "2026-02-25 17:45",
                        location = "苏州·金鸡湖",
                        resolution = "1920x1080"
                    )
                )
            )
        )
    }

    val audios = remember {
        listOf(
            NasAudioItem(
                id = "aud_001",
                name = "demo.m4a",
                type = "音频",
                format = "m4a",
                sizeKB = 102,
                path = "drawable/demo.m4a",
                time = "2026-04-08 09:00",
                durationSec = 60
            ),
            NasAudioItem(
                id = "aud_002",
                name = "voice_note.m4a",
                type = "音频",
                format = "m4a",
                sizeKB = 102,
                path = "drawable/demo.m4a",
                time = "2026-04-01 15:30",
                durationSec = 185
            )
        )
    }

    val allPhotoIds = remember(imageMonths) {
        imageMonths.flatMap { monthGroup -> monthGroup.images }.map { image -> image.id }
    }

    val allImages = remember(imageMonths) {
        imageMonths.flatMap { monthGroup -> monthGroup.images }
    }

    val allAudioIds = remember(audios) {
        audios.map { audio -> audio.id }
    }

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
            isPhotoSelectionMode || isAudioSelectionMode || isDocumentSelectionMode -> exitAllSelectionModes()
            else -> onBack()
        }
    }

    PlatformBackHandler(onBack = ::handleNasBack)

    val documents = remember {
        listOf(
            NasDocumentItem(
                id = "doc_001",
                name = "NAS.pdf",
                type = "文档",
                format = "pdf",
                sizeKB = 256,
                path = "drawable/NAS.pdf",
                time = "2026-04-09 18:50"
            ),
            NasDocumentItem(
                id = "doc_002",
                name = "demo-doc.pages",
                type = "文档",
                format = "pages",
                sizeKB = 88,
                path = "drawable/demo-doc.pages",
                time = "2026-04-01 12:00"
            ),
            NasDocumentItem(
                id = "doc_003",
                name = "project_plan.pages",
                type = "文档",
                format = "pages",
                sizeKB = 88,
                path = "drawable/demo-doc.pages",
                time = "2026-03-28 09:00"
            ),
            NasDocumentItem(
                id = "doc_004",
                name = "doc.doc",
                type = "文档",
                format = "doc",
                sizeKB = 28,
                path = "drawable/doc.doc",
                time = "2026-04-10 11:10"
            ),
            NasDocumentItem(
                id = "doc_005",
                name = "pptx.pptx",
                type = "文档",
                format = "pptx",
                sizeKB = 31,
                path = "drawable/pptx.pptx",
                time = "2026-04-10 11:11"
            ),
            NasDocumentItem(
                id = "doc_006",
                name = "xls.xls",
                type = "文档",
                format = "xls",
                sizeKB = 30,
                path = "drawable/xls.xls",
                time = "2026-04-10 11:12"
            )
        )
    }

    val allDocumentIds = remember(documents) {
        documents.map { document -> document.id }
    }

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

    Scaffold(containerColor = Color(0xF0FFFFFF)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NasCircularIconButton(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    onClick = ::handleNasBack,
                    modifier = Modifier.size(40.dp)
                )

                Text(
                    text = "NAS",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF111111),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                ) {
                    val isCurrentSelectionMode = when (selectedCategory) {
                        NasCategory.Photos -> isPhotoSelectionMode
                        NasCategory.Recordings -> isAudioSelectionMode
                        NasCategory.Documents -> isDocumentSelectionMode
                    }

                    if (isCurrentSelectionMode) {
                        NasPhotoSelectionRow(
                            selectedCount = when (selectedCategory) {
                                NasCategory.Photos -> selectedPhotoIds.size
                                NasCategory.Recordings -> selectedAudioIds.size
                                NasCategory.Documents -> selectedDocumentIds.size
                            },
                            onSelectAllClick = {
                                when (selectedCategory) {
                                    NasCategory.Photos -> {
                                        selectedPhotoIds.clear()
                                        selectedPhotoIds.addAll(allPhotoIds)
                                    }

                                    NasCategory.Recordings -> {
                                        selectedAudioIds.clear()
                                        selectedAudioIds.addAll(allAudioIds)
                                    }

                                    NasCategory.Documents -> {
                                        selectedDocumentIds.clear()
                                        selectedDocumentIds.addAll(allDocumentIds)
                                    }
                                }
                            },
                            onCancelClick = { exitAllSelectionModes() }
                        )
                    } else {
                        NasCategoryAndAddRow(
                            selected = selectedCategory,
                            onSelect = {
                                selectedCategory = it
                                exitAllSelectionModes()
                            },
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
                                when (selectedCategory) {
                                    NasCategory.Photos -> mediaController.openGallery()
                                    NasCategory.Recordings -> mediaController.openAudioPicker()
                                    NasCategory.Documents -> mediaController.openFilePicker()
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        when (selectedCategory) {
                            NasCategory.Photos -> NasPhotosContent(
                                imageMonths = imageMonths,
                                selectionMode = isPhotoSelectionMode,
                                selectedImageIds = selectedPhotoIds,
                                onImageClick = { image -> selectedImage = image },
                                onImageLongClick = { image -> previewImage = image },
                                onImageSelectionToggle = { image -> togglePhotoSelection(image) }
                            )
                            NasCategory.Recordings -> NasRecordingsContent(
                                audios = audios,
                                selectionMode = isAudioSelectionMode,
                                selectedAudioIds = selectedAudioIds,
                                onAudioClick = { audio -> selectedAudio = audio },
                                onAudioSelectionToggle = { audio -> toggleAudioSelection(audio) }
                            )
                            NasCategory.Documents -> NasDocumentsContent(
                                documents = documents,
                                selectionMode = isDocumentSelectionMode,
                                selectedDocumentIds = selectedDocumentIds,
                                onDocumentClick = { document -> selectedDocument = document },
                                onDocumentSelectionToggle = { document -> toggleDocumentSelection(document) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isCurrentSelectionMode) {
                        NasPhotoSelectionBottomBar(
                            onShareClick = {
                                val count = when (selectedCategory) {
                                    NasCategory.Photos -> selectedPhotoIds.size
                                    NasCategory.Recordings -> selectedAudioIds.size
                                    NasCategory.Documents -> selectedDocumentIds.size
                                }
                                println("发送朋友: ${count}项")
                            },
                            onDownloadClick = {
                                val count = when (selectedCategory) {
                                    NasCategory.Photos -> selectedPhotoIds.size
                                    NasCategory.Recordings -> selectedAudioIds.size
                                    NasCategory.Documents -> selectedDocumentIds.size
                                }
                                println("下载资源: ${count}项")
                            },
                            onDeleteClick = {
                                val count = when (selectedCategory) {
                                    NasCategory.Photos -> selectedPhotoIds.size
                                    NasCategory.Recordings -> selectedAudioIds.size
                                    NasCategory.Documents -> selectedDocumentIds.size
                                }
                                println("删除资源: ${count}项")
                            },
                            onSearchClick = {}
                        )
                    } else {
                        NasSearchBar(onClick = {})
                    }
                }
            }
        }
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
