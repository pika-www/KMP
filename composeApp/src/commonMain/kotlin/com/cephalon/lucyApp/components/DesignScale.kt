package com.cephalon.lucyApp.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import kotlin.math.min

/**
 * 设计稿适配系统（375 × 812）
 */
@Immutable
data class DesignScale(
    val scaleW: Float,
    val scaleH: Float,
    val scaleMin: Float,
) {

    // =========================
    // 📐 布局：dp 体系（px 思维）
    // =========================

    /** 设计稿 px -> dp */
    fun px(value: Float): Dp = (value * scaleW).dp

    fun sw(dp: Dp): Dp = dp * scaleW
    fun sh(dp: Dp): Dp = dp * scaleH
    fun sm(dp: Dp): Dp = dp * scaleMin

    /** 设计稿 px (Float) -> 适配后 dp，省去手动写 .dp */
    fun sw(px: Float): Dp = (px * scaleW).dp
    fun sh(px: Float): Dp = (px * scaleH).dp
    fun sm(px: Float): Dp = (px * scaleMin).dp

    // =========================
    // 🔤 字体：sp 体系（核心）
    // =========================

    /**
     * 标准字体（推荐默认用这个）
     * 设计稿 px -> sp
     */
    fun sp(value: Float): TextUnit {
        return (value * scaleW).sp
    }

    /**
     * 安全字体（防止大屏过大 / 小屏过小）
     */
    fun spSafe(value: Float): TextUnit {
        val fontScale = scaleW.coerceIn(0.95f, 1.15f)
        return (value * fontScale).sp
    }

    companion object {
        const val DESIGN_WIDTH = 360f
        const val DESIGN_HEIGHT = 812f
    }
}

/**
 * 全局 Scale
 */
val LocalDesignScale = staticCompositionLocalOf {
    DesignScale(1f, 1f, 1f)
}

/**
 * Provider：自动适配屏幕
 */
@Composable
fun DesignScaleProvider(
    modifier: Modifier = Modifier.fillMaxSize(),
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier) {

        var lastWidth by remember { mutableStateOf(maxWidth) }
        var stableHeight by remember { mutableStateOf(maxHeight) }

        // 屏幕变化（旋转 / 分屏）
        if (maxWidth != lastWidth) {
            lastWidth = maxWidth
            stableHeight = maxHeight
        }
        // 键盘收起恢复
        else if (maxHeight > stableHeight) {
            stableHeight = maxHeight
        }

        val scaleW = maxWidth.value / DesignScale.DESIGN_WIDTH
        val scaleH = stableHeight.value / DesignScale.DESIGN_HEIGHT
        val scaleMin = scaleW

        val scale = DesignScale(
            scaleW = scaleW,
            scaleH = scaleH,
            scaleMin = scaleMin
        )

        CompositionLocalProvider(LocalDesignScale provides scale) {
            content()
        }
    }
}