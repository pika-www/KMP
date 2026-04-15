package com.cephalon.lucyApp.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/**
 * 基于设计稿 375×812 的等比缩放工具。
 * 通过 [DesignScaleProvider] 注入后，任意子组件均可通过 [LocalDesignScale] 获取。
 */
@Immutable
data class DesignScale(
    val scaleW: Float,
    val scaleH: Float,
    val scaleMin: Float,
) {
    /** 水平方向缩放 (基于设计稿宽度 375) */
    fun sw(value: Dp): Dp = value * scaleW

    /** 垂直方向缩放 (基于设计稿高度 812) */
    fun sh(value: Dp): Dp = value * scaleH

    /** 等比缩放 min(scaleW, scaleH)，用于图标、圆角等 */
    fun sm(value: Dp): Dp = value * scaleMin

    /** 文字等比缩放 */
    fun sp(value: Float): TextUnit = (value * scaleMin).sp

    companion object {
        const val DESIGN_WIDTH = 375f
        const val DESIGN_HEIGHT = 812f
    }
}

val LocalDesignScale = staticCompositionLocalOf {
    DesignScale(scaleW = 1f, scaleH = 1f, scaleMin = 1f)
}

/**
 * 包裹子组件，自动测量屏幕尺寸并注入 [DesignScale]。
 * 子组件内通过 `LocalDesignScale.current` 获取缩放值。
 */
@Composable
fun DesignScaleProvider(
    modifier: Modifier = Modifier.fillMaxSize(),
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        var lastWidth by remember { mutableStateOf(maxWidth) }
        var stableHeight by remember { mutableStateOf(maxHeight) }

        if (maxWidth != lastWidth) {
            // 宽度变化 → 真正的配置变更（旋转/折叠/分屏）→ 更新高度基准
            lastWidth = maxWidth
            stableHeight = maxHeight
        } else if (maxHeight > stableHeight) {
            // 高度增大（键盘收起等）→ 恢复高度基准
            stableHeight = maxHeight
        }
        // 高度单独减小（键盘弹出）→ 不更新 stableHeight，缩放不变

        val scale = DesignScale(
            scaleW = maxWidth.value / DesignScale.DESIGN_WIDTH,
            scaleH = stableHeight.value / DesignScale.DESIGN_HEIGHT,
            scaleMin = min(
                maxWidth.value / DesignScale.DESIGN_WIDTH,
                stableHeight.value / DesignScale.DESIGN_HEIGHT
            )
        )
        CompositionLocalProvider(LocalDesignScale provides scale) {
            content()
        }
    }
}
