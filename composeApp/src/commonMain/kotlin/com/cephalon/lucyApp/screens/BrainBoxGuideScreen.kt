package com.cephalon.lucyApp.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PsychologyAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlinx.coroutines.launch

private data class GuidePage(
    val title: String,
    val subtitle: String,
    val description: String,
    val primaryText: String,
    val useLightButton: Boolean,
    val showBrand: Boolean = false
)

private val guidePages = listOf(
    GuidePage(
        title = "Welcome to Lucy",
        subtitle = "你的个人 AI 操作系统",
        description = "你不只是聊天，而是真正理解你、持续执行任务的个人 AI 系统",
        primaryText = "下一步",
        useLightButton = false
    ),
    GuidePage(
        title = "本地优先",
        subtitle = "数据主权归你所有",
        description = "模型本地运行，数据不出端，文字与任务都由你自己掌控",
        primaryText = "下一步",
        useLightButton = true
    ),
    GuidePage(
        title = "本地优先\n数据主权归你所有",                                                                                                                                                                          
        subtitle = "",
        description = "模型本地运行，数据不出端，交互、记录与资产都留在你身边",
        primaryText = "开始使用",
        useLightButton = false
    ),
)

@Composable
fun BrainBoxGuideScreen(
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { guidePages.size }
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val baseWidth = 393f
        val scale = min(maxWidth.value / baseWidth, 1.18f)
        val currentPage = pagerState.currentPage
        val page = guidePages[currentPage]

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .background(Color.Black)
            ) {
                if (page.showBrand) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = scaledDp(20.dp, scale), top = scaledDp(10.dp, scale)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(scaledDp(24.dp, scale))
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PsychologyAlt,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(scaledDp(14.dp, scale))
                            )
                        }
                        Spacer(modifier = Modifier.width(scaledDp(8.dp, scale)))
                        Text(
                            text = "LUCY",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = scaledDp(26.dp, scale)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { pageIndex ->
                        val targetPage = guidePages[pageIndex]

                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(scaledDp(320.dp, scale)))

                            HeroIcon(scale = scale, pageIndex = pageIndex)

                            Spacer(modifier = Modifier.height(scaledDp(22.dp, scale)))

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = targetPage.title,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )

                                if (targetPage.subtitle.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(scaledDp(8.dp, scale)))
                                    Text(
                                        text = targetPage.subtitle,
                                        color = Color(0xFFBEBEBE),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(scaledDp(28.dp, scale)))

                                Text(
                                    text = targetPage.description,
                                    color = Color(0xFF7C7C7C),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = scaledDp(24.dp, scale).value.sp
                                    ),
                                    modifier = Modifier.fillMaxWidth(0.95f)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .padding(bottom = scaledDp(14.dp, scale)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        guidePages.indices.forEach { index ->
                            val isSelected = index == currentPage
                            Box(
                                modifier = Modifier
                                    .size(
                                        width = scaledDp(if (isSelected) 18.dp else 6.dp, scale),
                                        height = scaledDp(6.dp, scale)
                                    )
                                    .background(
                                        color = if (isSelected) Color.White else Color(0xFF4A4A4A),
                                        shape = RoundedCornerShape(scaledDp(3.dp, scale))
                                    )
                            )

                            if (index != guidePages.lastIndex) {
                                Spacer(modifier = Modifier.width(scaledDp(8.dp, scale)))
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (currentPage == guidePages.lastIndex) {
                                onFinish()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(currentPage + 1)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(scaledDp(52.dp, scale)),
                        shape = RoundedCornerShape(scaledDp(26.dp, scale)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (page.useLightButton) Color.White else Color(0xFF2E2E31),
                            contentColor = if (page.useLightButton) Color(0xFF111111) else Color.White
                        )
                    ) {
                        Text(
                            text = page.primaryText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.padding(top = scaledDp(12.dp, scale))
                    ) {
                        Text(
                            text = "跳过引导",
                            color = Color(0xFF6B6B6B),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(scaledDp(26.dp, scale)))
                }
            }
        }
    }
}

@Composable
private fun HeroIcon(
    scale: Float,
    pageIndex: Int
) {
    Box(
        modifier = Modifier.size(scaledDp(86.dp, scale)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.PsychologyAlt,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(scaledDp(if (pageIndex == 4) 58.dp else 52.dp, scale))
        )

        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(scaledDp(20.dp, scale))
        )
    }
}

private fun scaledDp(value: Dp, scale: Float): Dp = value * scale
