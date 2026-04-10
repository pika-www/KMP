package com.cephalon.lucyApp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.guide_bg
import androidios.composeapp.generated.resources.logo
import androidios.composeapp.generated.resources.tanslogo
import com.cephalon.lucyApp.components.DesignScaleProvider
import com.cephalon.lucyApp.components.LocalDesignScale
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

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
        description = "不只是聊天，而是真正理解你、\n持续执行任务的个人 AI 系统",
        primaryText = "下一步",
        useLightButton = false
    ),
    GuidePage(
        title = "本地优先",
        subtitle = "数据主权归你所有",
        description = "模型本地运行，数据不出端",
        primaryText = "下一步",
        useLightButton = false
    ),
    GuidePage(
        title = "SKILL 生态",
        subtitle = "可持续运行的工作流",
        description = "从晨报、回忆助手到内容流水线\nSkill 持续帮你完成任务",
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

    DesignScaleProvider(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val ds = LocalDesignScale.current
        val currentPage = pagerState.currentPage
        val page = guidePages[currentPage]

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            // 背景图
            Image(
                painter = painterResource(Res.drawable.guide_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                if (page.showBrand) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = ds.sw(20.dp), top = ds.sh(10.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(ds.sm(24.dp))
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.logo),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(ds.sm(18.dp))
                            )
                        }
                        Spacer(modifier = Modifier.width(ds.sw(8.dp)))
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
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pager 不受水平 padding 限制，滑动到屏幕边缘再消失
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        beyondViewportPageCount = 1
                    ) { pageIndex ->
                        val targetPage = guidePages[pageIndex]

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = ds.sw(26.dp)),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 弹性留白，自动适应屏幕高度
                            Spacer(modifier = Modifier.weight(1f))

                            HeroIcon(pageIndex = pageIndex)

                            Spacer(modifier = Modifier.height(ds.sh(24.dp)))

                            Text(
                                text = targetPage.title,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = ds.sp(24f),
                                fontWeight = FontWeight.Medium,
                            )

                            if (targetPage.subtitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(ds.sh(4.dp)))
                                Text(
                                    text = targetPage.subtitle,
                                    color = Color.White.copy(alpha = 0.90f),
                                    textAlign = TextAlign.Center,
                                    fontSize = ds.sp(14f),
                                    fontWeight = FontWeight.Light,
                                    lineHeight = ds.sp(22f),
                                )
                            }

                            Spacer(modifier = Modifier.height(ds.sh(32.dp)))

                            Text(
                                text = targetPage.description,
                                color = Color.White.copy(alpha = 0.60f),
                                textAlign = TextAlign.Center,
                                fontSize = ds.sp(12f),
                                fontWeight = FontWeight.Light,
                                lineHeight = ds.sp(20f),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(ds.sh(40.dp)))

                    // 进度点，点击可跳转
                    Row(
                        modifier = Modifier.padding(horizontal = ds.sw(26.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        guidePages.indices.forEach { index ->
                            val isSelected = index == currentPage
                            Box(
                                modifier = Modifier
                                    .size(
                                        width = ds.sw(if (isSelected) 18.dp else 6.dp),
                                        height = ds.sh(6.dp)
                                    )
                                    .background(
                                        color = if (isSelected) Color.White else Color(0xFF4A4A4A),
                                        shape = RoundedCornerShape(ds.sm(3.dp))
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        scope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                            )

                            if (index != guidePages.lastIndex) {
                                Spacer(modifier = Modifier.width(ds.sw(8.dp)))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(ds.sh(40.dp)))

                    FrostedGlassButton(
                        text = page.primaryText,
                        modifier = Modifier.padding(horizontal = ds.sw(26.dp)),
                        onClick = {
                            if (currentPage == guidePages.lastIndex) {
                                onFinish()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(currentPage + 1)
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(ds.sh(21.dp)))

                    // 最后一步隐藏"跳过引导"，但保留占位
                    val isLastPage = currentPage == guidePages.lastIndex
                    TextButton(
                        onClick = { if (!isLastPage) onBack() },
                        enabled = !isLastPage,
                    ) {
                        Text(
                            text = "跳过引导",
                            color = if (isLastPage) Color.Transparent else Color.White.copy(alpha = 0.60f),
                            fontSize = ds.sp(16f),
                            fontWeight = FontWeight.Normal,
                        )
                    }

                    Spacer(modifier = Modifier.height(ds.sh(26.dp)))
                }
            }
        }
    }
}

@Composable
private fun HeroIcon(
    pageIndex: Int
) {
    val ds = LocalDesignScale.current
    Box(
        modifier = Modifier.size(
            width = ds.sm(99.dp),
            height = ds.sm(82.dp)
        ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.tanslogo),
            contentDescription = null,
            modifier = Modifier.size(
                width = ds.sm(99.dp),
                height = ds.sm(82.dp)
            )
        )
    }
}

@Composable
private fun FrostedGlassButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val ds = LocalDesignScale.current
    val shape = RoundedCornerShape(100.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ds.sh(52.dp))
            .clip(shape)
            .background(Color(0xFF171717), shape)
            .background(Color(0x408C8C8C), shape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = shape
            )
            .drawBehind {
                val cr = CornerRadius(size.height / 2f)
                // 内发光 — 模拟 box-shadow inset 0 0 8px #F2F2F2
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.05f),
                    topLeft = Offset(3f, 3f),
                    size = Size(size.width - 6f, size.height - 6f),
                    cornerRadius = cr,
                    style = Stroke(width = 6f)
                )
                // 顶部高光 — 模拟 box-shadow 3px 3px 0.5px -3.5px #FFF inset
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.10f),
                    topLeft = Offset(1.5f, 1.5f),
                    size = Size(size.width - 3f, size.height - 3f),
                    cornerRadius = cr,
                    style = Stroke(width = 1f)
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = ds.sp(16f),
            fontWeight = FontWeight.Medium,
        )
    }
}
