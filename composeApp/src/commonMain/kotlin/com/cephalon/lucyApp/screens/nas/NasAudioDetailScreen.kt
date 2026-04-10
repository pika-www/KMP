package com.cephalon.lucyApp.screens.nas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.media.PlatformMediaAccessController
import kotlin.math.roundToLong

@Composable
internal fun NasAudioDetailScreen(
    audio: NasAudioItem,
    mediaController: PlatformMediaAccessController,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSeeking by remember(audio.id) { mutableStateOf(false) }
    var sliderPositionMillis by remember(audio.id) { mutableFloatStateOf(0f) }
    val playbackState = mediaController.audioPlaybackState
    val isCurrentAudio = playbackState.sourceId == audio.id
    val fallbackDurationMillis = audio.durationSec * 1000L
    val durationMillis = when {
        isCurrentAudio && playbackState.durationMillis > 0L -> playbackState.durationMillis
        else -> fallbackDurationMillis
    }.coerceAtLeast(1L)
    val currentPositionMillis = when {
        isCurrentAudio -> playbackState.currentPositionMillis.coerceIn(0L, durationMillis)
        else -> 0L
    }
    val isPlaying = isCurrentAudio && playbackState.isPlaying

    LaunchedEffect(audio.id, isCurrentAudio, currentPositionMillis, durationMillis, isSeeking) {
        if (!isSeeking) {
            sliderPositionMillis = currentPositionMillis.coerceIn(0L, durationMillis).toFloat()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 顶部栏：返回按钮、Tab切换、菜单按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回按钮
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color(0xFF3A3A3A)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Tab切换
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.width(180.dp),
                        containerColor = Color.White,
                        indicator = {},
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (selectedTab == 0) Color.Black else Color.Transparent
                                )
                        ) {
                            Text(
                                text = "音频",
                                modifier = Modifier.padding(vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (selectedTab == 0) Color.White else Color(0xFF3A3A3A)
                            )
                        }
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (selectedTab == 1) Color.Black else Color.Transparent
                                )
                        ) {
                            Text(
                                text = "文稿",
                                modifier = Modifier.padding(vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (selectedTab == 1) Color.White else Color(0xFF3A3A3A)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 菜单按钮
                IconButton(
                    onClick = { /* TODO: 显示菜单 */ },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多",
                        tint = Color(0xFF3A3A3A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 内容区域
            when (selectedTab) {
                0 -> AudioPlayerContent(
                    audio = audio,
                    isPlaying = isPlaying,
                    currentPositionMillis = if (isSeeking) sliderPositionMillis.roundToLong() else currentPositionMillis,
                    durationMillis = durationMillis,
                    onPlayPauseClick = {
                        mediaController.toggleAudioPlayback(
                            sourceId = audio.id,
                            name = audio.name,
                            source = audio.path
                        )
                    },
                    onProgressChange = { value ->
                        isSeeking = true
                        sliderPositionMillis = value.coerceIn(0f, durationMillis.toFloat())
                    },
                    onProgressChangeFinished = {
                        mediaController.seekAudioPlaybackTo(sliderPositionMillis.roundToLong())
                        isSeeking = false
                    },
                    onSkipPreviousClick = {
                        mediaController.skipAudioPlaybackBy(-10_000L)
                    },
                    onSkipNextClick = {
                        mediaController.skipAudioPlaybackBy(10_000L)
                    },
                    modifier = Modifier.weight(1f)
                )
                1 -> TranscriptContent(
                    modifier = Modifier.weight(1f)
                )
            }

            // 底部操作按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 发送朋友按钮
                Button(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF222222)
                    )
                ) {
                    Text(
                        text = "发送朋友",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                // 删除按钮
                Button(
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFFFF3B30)
                    )
                ) {
                    Text(
                        text = "删除",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioPlayerContent(
    audio: NasAudioItem,
    isPlaying: Boolean,
    currentPositionMillis: Long,
    durationMillis: Long,
    onPlayPauseClick: () -> Unit,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: () -> Unit,
    onSkipPreviousClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 音频可视化圆形
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            // TODO: 添加音频波形可视化
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 音频标题和格式
        Text(
            text = "${audio.name.replace(".${audio.format}", "")} (${audio.format.uppercase()})",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF222222)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 时间
        Text(
            text = audio.time,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8E8E93)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 播放进度条
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            val sliderMax = durationMillis.coerceAtLeast(1L).toFloat()
            Slider(
                value = currentPositionMillis.coerceIn(0L, durationMillis).toFloat(),
                onValueChange = onProgressChange,
                onValueChangeFinished = onProgressChangeFinished,
                modifier = Modifier.fillMaxWidth(),
                valueRange = 0f..sliderMax,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF3A3A3A),
                    inactiveTrackColor = Color(0xFFE5E5EA)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTimeFromMillis(currentPositionMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93)
                )
                Text(
                    text = formatTimeFromMillis(durationMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 播放控制按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 快退按钮
            IconButton(
                onClick = onSkipPreviousClick,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "快退",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF3A3A3A)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // 播放/暂停按钮
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF3A3A3A)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // 快进按钮
            IconButton(
                onClick = onSkipNextClick,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "快进",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF3A3A3A)
                )
            }
        }
    }
}

@Composable
private fun TranscriptContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "文稿内容为空",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF8E8E93)
        )
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}

private fun formatTimeFromMillis(durationMillis: Long): String {
    return formatTime((durationMillis / 1000L).toInt())
}
