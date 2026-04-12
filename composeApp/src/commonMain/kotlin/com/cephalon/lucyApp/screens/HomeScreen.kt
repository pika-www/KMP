package com.cephalon.lucyApp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.material3.Text
import androidios.composeapp.generated.resources.Res
import androidios.composeapp.generated.resources.logo
import androidios.composeapp.generated.resources.reboto
import androidios.composeapp.generated.resources.roboto_bg
import com.cephalon.lucyApp.components.DesignScaleProvider
import com.cephalon.lucyApp.components.LocalDesignScale
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.absoluteValue

// ─── 颜色常量 ───
private val TitleColor = Color(0xFF1F2535)
private val SubtitleColor = Color(0xFF717580)
private val DotActiveColor = Color(0xFF1F2535)
private val DotInactiveColor = Color(0x331F2535) // rgba(31,37,53,0.20)
private val CardButtonColor = Color(0xFF1F2535)

// ─── 卡片数据 ───
private data class AccessCard(
    val title: String,
    val subtitle: String,
    val buttonText: String,
)

private val accessCards = listOf(
    AccessCard(
        title = "脑花盒子用户",
        subtitle = "我拥有 AI NPC/龙虾pai",
        buttonText = "点击登录",
    ),
    AccessCard(
        title = "端脑云用户",
        subtitle = "我没有龙虾，我要领养一只",
        buttonText = "点击登录",
    ),
    AccessCard(
        title = "本地部署用户",
        subtitle = "我自己有本地龙虾，我要接入",
        buttonText = "点击登录",
    ),
)

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onOpenSdkTest: () -> Unit,
    onOpenWsTest: () -> Unit,
    onOpenBrainBoxGuide: () -> Unit,
    onOpenAgentModel: () -> Unit,
    onOpenScanBindChannel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { accessCards.size }
    )

    DesignScaleProvider(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7))
    ) {
        val ds = LocalDesignScale.current
        val currentPage = pagerState.currentPage

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // ── 主标题 ──
            Text(
                text = "选择 Lucy 接入方式",
                color = TitleColor,
                textAlign = TextAlign.Center,
                fontSize = ds.sp(28f),
                fontWeight = FontWeight.Medium,
            )

            // 间隔 8px
            Spacer(modifier = Modifier.height(ds.sh(8.dp)))

            // ── 副标题 ──
            Text(
                text = "不同的接入方式决定了您的数据存储位置和算\n力来源",
                color = SubtitleColor,
                textAlign = TextAlign.Center,
                fontSize = ds.sp(16f),
                fontWeight = FontWeight.Light,
            )

            // 副标题距卡片 81px
            Spacer(modifier = Modifier.height(ds.sh(81.dp)))

            // ── 卡片轮播 ──
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ds.sh(240.dp)),
                contentPadding = PaddingValues(horizontal = ds.sw(98.dp)),
                pageSpacing = ds.sw(16.dp),
                beyondViewportPageCount = 1,
            ) { pageIndex ->
                val pageOffset = ((pagerState.currentPage - pageIndex) +
                        pagerState.currentPageOffsetFraction).absoluteValue
                val scale = lerp(start = 0.85f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))
                val alpha = lerp(start = 0.5f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))

                val card = accessCards[pageIndex]

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        .width(ds.sw(180.dp))
                        .height(ds.sh(240.dp))
                        .shadow(
                            elevation = 15.dp,
                            shape = RoundedCornerShape(ds.sm(16.dp)),
                            ambientColor = Color.Black.copy(alpha = 0.05f),
                            spotColor = Color.Black.copy(alpha = 0.05f),
                        )
                        .clip(RoundedCornerShape(ds.sm(16.dp)))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (pagerState.currentPage == pageIndex) {
                                when (pageIndex) {
                                    0 -> onOpenWsTest()
                                    1 -> onOpenAgentModel()
                                    2 -> onOpenScanBindChannel()
                                }
                            } else {
                                scope.launch { pagerState.animateScrollToPage(pageIndex) }
                            }
                        }
                ) {
                    // 卡片背景图
                    Image(
                        painter = painterResource(Res.drawable.roboto_bg),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = ds.sw(16.dp),
                                end = ds.sw(16.dp),
                                top = ds.sh(16.dp),
                                bottom = ds.sh(20.dp)
                            ),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        // 卡片大标题 14sp/#000/400
                        Text(
                            text = card.title,
                            color = Color.Black,
                            fontSize = ds.sp(14f),
                            fontWeight = FontWeight.Normal,
                        )

                        // 间距 2px
                        Spacer(modifier = Modifier.height(ds.sh(2.dp)))

                        // 卡片小字 10sp/rgba(0,0,0,0.60)/400
                        Text(
                            text = card.subtitle,
                            color = Color.Black.copy(alpha = 0.60f),
                            fontSize = ds.sp(10f),
                            fontWeight = FontWeight.Normal,
                        )

                        // 图片距小字 12px
                        Spacer(modifier = Modifier.height(ds.sh(12.dp)))

                        // 中间图片 124×121 居中
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.reboto),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(
                                        width = ds.sw(124.dp),
                                        height = ds.sh(121.dp)
                                    ),
                                contentScale = ContentScale.Fit,
                            )
                        }

                        // 按钮距图片 7px
                        Spacer(modifier = Modifier.height(ds.sh(7.dp)))

                        // 按钮 border-radius:16px, bg:#1F2535
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ds.sh(32.dp))
                                .clip(RoundedCornerShape(ds.sm(16.dp)))
                                .background(CardButtonColor)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    if (pagerState.currentPage == pageIndex) {
                                        when (pageIndex) {
                                            0 -> onOpenWsTest()
                                            1 -> onOpenAgentModel()
                                            2 -> onOpenScanBindChannel()
                                        }
                                    } else {
                                        scope.launch { pagerState.animateScrollToPage(pageIndex) }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = card.buttonText,
                                color = Color.White,
                                fontSize = ds.sp(12f),
                                fontWeight = FontWeight.Normal,
                            )
                        }
                    }
                }
            }

            // 卡片下方距进度点 82px
            Spacer(modifier = Modifier.height(ds.sh(82.dp)))

            // ── 进度点 ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(ds.sw(8.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                accessCards.indices.forEach { index ->
                    val isSelected = index == currentPage
                    Box(
                        modifier = Modifier
                            .size(
                                width = ds.sw(if (isSelected) 18.dp else 8.dp),
                                height = ds.sh(8.dp)
                            )
                            .background(
                                color = if (isSelected) DotActiveColor else DotInactiveColor,
                                shape = RoundedCornerShape(ds.sm(4.dp))
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
                }
            }

            // 进度点距 logo 62px
            Spacer(modifier = Modifier.height(ds.sh(62.dp)))

            // ── 底部 Logo 64px ──
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(ds.sm(64.dp)),
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
