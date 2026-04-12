package com.cephalon.lucyApp.screens.nas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
        fun img(id: String, name: String, time: String, location: String) = NasImageItem(
            id = id, name = name, type = "图片", format = "png",
            sizeKB = (2100..2300).random(), path = "drawable/img-demo.png",
            time = time, location = location, resolution = "1920x1080"
        )
        listOf(
            NasImageMonthGroup(
                label = "2026年4月",
                images = listOf(
                    img("img_001", "morning_walk.png", "2026-04-01 08:30", "上海·徐汇滨江"),
                    img("img_002", "coffee_corner.png", "2026-04-03 10:15", "上海·静安"),
                    img("img_003", "team_lunch.png", "2026-04-06 12:40", "上海·长宁"),
                    img("img_004", "night_view.png", "2026-04-08 20:05", "上海·陆家嘴"),
                    img("img_005", "rooftop_sunset.png", "2026-04-11 18:30", "上海·外滩"),
                    img("img_006", "garden_bloom.png", "2026-04-14 09:00", "上海·世纪公园"),
                    img("img_007", "street_art.png", "2026-04-17 15:20", "上海·莫干山路"),
                    img("img_008", "bridge_night.png", "2026-04-20 21:10", "上海·南浦大桥"),
                    img("img_009", "brunch_table.png", "2026-04-23 11:00", "上海·武康路"),
                    img("img_010", "cat_cafe.png", "2026-04-26 14:45", "上海·愚园路"),
                    img("img_011", "rain_drops.png", "2026-04-28 16:30", "上海·新天地"),
                    img("img_012", "bookshelf.png", "2026-04-30 10:00", "上海·思南路")
                )
            ),
            NasImageMonthGroup(
                label = "2026年3月",
                images = listOf(
                    img("img_013", "spring_park.png", "2026-03-02 09:20", "杭州·西湖"),
                    img("img_014", "office_board.png", "2026-03-05 14:10", "杭州·滨江"),
                    img("img_015", "book_store.png", "2026-03-08 16:45", "杭州·天目里"),
                    img("img_016", "weekend_market.png", "2026-03-11 11:30", "杭州·武林路"),
                    img("img_017", "tea_house.png", "2026-03-14 15:00", "杭州·龙井"),
                    img("img_018", "lake_view.png", "2026-03-17 07:40", "杭州·断桥"),
                    img("img_019", "night_canal.png", "2026-03-20 20:15", "杭州·拱宸桥"),
                    img("img_020", "cherry_blossom.png", "2026-03-23 10:30", "杭州·太子湾"),
                    img("img_021", "temple_steps.png", "2026-03-26 13:00", "杭州·灵隐"),
                    img("img_022", "hill_trail.png", "2026-03-29 08:00", "杭州·北高峰")
                )
            ),
            NasImageMonthGroup(
                label = "2026年2月",
                images = listOf(
                    img("img_023", "train_window.png", "2026-02-01 07:55", "高铁上"),
                    img("img_024", "family_dinner.png", "2026-02-04 18:25", "苏州·园区"),
                    img("img_025", "museum_day.png", "2026-02-07 15:10", "苏州博物馆"),
                    img("img_026", "river_evening.png", "2026-02-10 17:45", "苏州·金鸡湖"),
                    img("img_027", "lantern_fest.png", "2026-02-13 19:30", "苏州·平江路"),
                    img("img_028", "snow_garden.png", "2026-02-16 09:00", "苏州·拙政园"),
                    img("img_029", "noodle_shop.png", "2026-02-19 12:15", "苏州·观前街"),
                    img("img_030", "canal_bridge.png", "2026-02-22 16:00", "苏州·山塘街"),
                    img("img_031", "sunset_tower.png", "2026-02-25 17:20", "苏州·虎丘"),
                    img("img_032", "market_stall.png", "2026-02-28 10:45", "苏州·双塔")
                )
            ),
            NasImageMonthGroup(
                label = "2026年1月",
                images = listOf(
                    img("img_033", "new_year.png", "2026-01-01 00:05", "上海·外滩"),
                    img("img_034", "hot_pot.png", "2026-01-04 19:00", "上海·打浦路"),
                    img("img_035", "gym_selfie.png", "2026-01-07 07:30", "上海·静安"),
                    img("img_036", "coding_desk.png", "2026-01-10 22:00", "上海·漕河泾"),
                    img("img_037", "foggy_morning.png", "2026-01-13 08:15", "上海·世博园"),
                    img("img_038", "pet_dog.png", "2026-01-16 14:00", "上海·共青森林"),
                    img("img_039", "mall_lights.png", "2026-01-19 20:30", "上海·环球港"),
                    img("img_040", "vinyl_record.png", "2026-01-22 16:45", "上海·衡山路")
                )
            ),
            NasImageMonthGroup(
                label = "2025年12月",
                images = listOf(
                    img("img_041", "christmas_tree.png", "2025-12-01 18:00", "上海·恒隆"),
                    img("img_042", "winter_run.png", "2025-12-04 06:50", "上海·世纪公园"),
                    img("img_043", "year_end_party.png", "2025-12-08 21:30", "上海·THE BUND"),
                    img("img_044", "gift_wrap.png", "2025-12-12 13:00", "上海·淮海路"),
                    img("img_045", "frozen_lake.png", "2025-12-16 10:00", "上海·顾村公园"),
                    img("img_046", "coffee_art.png", "2025-12-20 15:20", "上海·巨鹿路"),
                    img("img_047", "city_skyline.png", "2025-12-24 19:00", "上海·陆家嘴"),
                    img("img_048", "countdown.png", "2025-12-31 23:55", "上海·人民广场")
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

    val isCurrentSelectionMode = when (selectedCategory) {
        NasCategory.Photos -> isPhotoSelectionMode
        NasCategory.Recordings -> isAudioSelectionMode
        NasCategory.Documents -> isDocumentSelectionMode
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

            // ── 顶部：悬浮 toolbar ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NasTopBackButton(onClick = {
                        if (isCurrentSelectionMode) exitAllSelectionModes() else handleNasBack()
                    })

                    if (isCurrentSelectionMode) {
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
                            modifier = Modifier.weight(1f)
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
                            modifier = Modifier.weight(1f)
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
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        NasGlassCircleButton(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "搜索",
                            onClick = {}
                        )

                        NasTopCategoryRow(
                            selected = selectedCategory,
                            onSelect = {
                                selectedCategory = it
                                exitAllSelectionModes()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── 底部：悬浮操作栏 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
            ) {
                if (isCurrentSelectionMode) {
                    NasGlassTextButton(
                        text = "取消选择",
                        onClick = { exitAllSelectionModes() }
                    )
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
                            when (selectedCategory) {
                                NasCategory.Photos -> mediaController.openGallery()
                                NasCategory.Recordings -> mediaController.openAudioPicker()
                                NasCategory.Documents -> mediaController.openFilePicker()
                            }
                        }
                    )
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
