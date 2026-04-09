package com.cephalon.lucyApp.screens.nas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.media.rememberPlatformMediaAccessController

@Composable
fun NasScreen(onBack: () -> Unit) {
    var selectedCategory by remember { mutableStateOf(NasCategory.Photos) }
    var selectedImage by remember { mutableStateOf<NasImageItem?>(null) }
    var selectedAudio by remember { mutableStateOf<NasAudioItem?>(null) }
    var selectedDocument by remember { mutableStateOf<NasDocumentItem?>(null) }
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
                type = "录音",
                format = "m4a",
                sizeKB = 102,
                path = "drawable/demo.m4a",
                time = "2026-04-08 09:00",
                durationSec = 60
            ),
            NasAudioItem(
                id = "aud_002",
                name = "voice_note.m4a",
                type = "录音",
                format = "m4a",
                sizeKB = 102,
                path = "drawable/demo.m4a",
                time = "2026-04-01 15:30",
                durationSec = 185
            )
        )
    }

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
            )
        )
    }

    // 如果选中了图片，显示详情页
    selectedImage?.let { image ->
        NasImageDetailScreen(
            image = image,
            onBack = { selectedImage = null },
            onShare = {
                println("分享图片: ${image.name}")
                // TODO: 实现分享功能
            },
            onDelete = {
                println("删除图片: ${image.name}")
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
            onBack = { selectedAudio = null },
            onShare = {
                println("分享音频: ${audio.name}")
                // TODO: 实现分享功能
            },
            onDelete = {
                println("删除音频: ${audio.name}")
                // TODO: 实现删除功能
                selectedAudio = null
            }
        )
        return
    }

    // 如果选中了文档，显示详情页
    selectedDocument?.let { document ->
        NasDocumentDetailScreen(
            document = document,
            onBack = { selectedDocument = null },
            onShare = {
                println("分享文档: ${document.name}")
                // TODO: 实现分享功能
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
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NAS",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF121212)
                )
                TextButton(onClick = onBack) {
                    Text("返回")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                ) {
                    NasCategoryAndAddRow(
                        selected = selectedCategory,
                        onSelect = { selectedCategory = it },
                        onAddClick = {
                            when (selectedCategory) {
                                NasCategory.Photos -> mediaController.openGallery()
                                NasCategory.Recordings -> mediaController.openAudioPicker()
                                NasCategory.Documents -> mediaController.openFilePicker()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        when (selectedCategory) {
                            NasCategory.Photos -> NasPhotosContent(
                                imageMonths = imageMonths,
                                onImageClick = { image -> selectedImage = image }
                            )
                            NasCategory.Recordings -> NasRecordingsContent(
                                audios = audios,
                                onAudioClick = { audio -> selectedAudio = audio }
                            )
                            NasCategory.Documents -> NasDocumentsContent(
                                documents = documents,
                                onDocumentClick = { document -> selectedDocument = document }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    NasSearchBar(onClick = {})
                }
            }
        }
    }
}
