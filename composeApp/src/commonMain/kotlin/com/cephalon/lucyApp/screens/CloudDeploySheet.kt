package com.cephalon.lucyApp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.components.HalfModalBottomSheet
import com.cephalon.lucyApp.components.LocalDesignScale

// ─── 颜色常量 ───
private val StepCompletedColor = Color(0xFF1F2535)
private val StepPendingColor = Color(0xFFD1D1D6)
private val ButtonBgColor = Color(0xFFE5E5EA)
private val ButtonTextColor = Color(0xFF8E8E93)
private val DividerColor = Color(0xFFF0F0F0)

/**
 * 云端部署进度弹窗。
 *
 * @param isVisible       是否展示
 * @param completedStep   已完成的步骤数（0=都未完成, 1=创建完成, 2=启动完成, 3=链接完成）
 * @param errorMessage    错误信息（非空时展示在底部按钮上）
 * @param onDismiss       关闭弹窗
 */
@Composable
fun CloudDeploySheet(
    isVisible: Boolean,
    completedStep: Int,
    errorMessage: String?,
    onDismiss: () -> Unit,
) {
    val ds = LocalDesignScale.current

    data class StepItem(
        val label: String,
        val isCompleted: Boolean,
        val isActive: Boolean,
    )

    val steps = listOf(
        StepItem(
            label = "创建云端应用",
            isCompleted = completedStep >= 1,
            isActive = completedStep < 1,
        ),
        StepItem(
            label = "启动云端应用",
            isCompleted = completedStep >= 2,
            isActive = completedStep == 1,
        ),
        StepItem(
            label = "链接脑花",
            isCompleted = completedStep >= 3,
            isActive = completedStep == 2,
        ),
    )

    val allDone = completedStep >= 3
    val buttonText = when {
        errorMessage != null -> errorMessage
        allDone -> "创建完成"
        else -> "创建中..."
    }

    HalfModalBottomSheet(
        isVisible = isVisible,
        onDismissRequest = onDismiss,
        onDismissed = {},
        showTopBar = false,
        topPadding = ds.sh(340.dp),
        contentPadding = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ds.sw(24.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(ds.sh(32.dp)))

            // ── 标题 ──
            Text(
                text = "正在创建云端智能应用",
                color = Color.Black,
                fontSize = ds.sp(22f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(ds.sh(8.dp)))

            // ── 副标题 ──
            Text(
                text = "一键云端部署，无缝接入脑花。创建预计耗时2-3分钟。",
                color = Color(0xFF999999),
                fontSize = ds.sp(14f),
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(ds.sh(28.dp)))

            // ── 步骤列表 ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ds.sm(12.dp)))
                    .background(Color.White)
                    .padding(horizontal = ds.sw(16.dp)),
            ) {
                steps.forEachIndexed { index, step ->
                    DeployStepRow(
                        label = step.label,
                        isCompleted = step.isCompleted,
                        isActive = step.isActive,
                    )
                    if (index < steps.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(DividerColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── 底部按钮 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ds.sh(48.dp))
                    .clip(RoundedCornerShape(ds.sm(12.dp)))
                    .background(ButtonBgColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = buttonText,
                    color = ButtonTextColor,
                    fontSize = ds.sp(16f),
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(ds.sh(24.dp)))
        }
    }
}

@Composable
private fun DeployStepRow(
    label: String,
    isCompleted: Boolean,
    isActive: Boolean,
) {
    val ds = LocalDesignScale.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ds.sh(16.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── 图标 ──
        if (isCompleted) {
            Box(
                modifier = Modifier
                    .size(ds.sm(22.dp))
                    .clip(CircleShape)
                    .background(StepCompletedColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(ds.sm(14.dp)),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(ds.sm(22.dp))
                    .clip(CircleShape)
                    .background(StepPendingColor),
            )
        }

        Spacer(modifier = Modifier.width(ds.sw(12.dp)))

        // ── 标签 ──
        Text(
            text = label,
            color = if (isCompleted || isActive) Color.Black else Color(0xFFBBBBBB),
            fontSize = ds.sp(16f),
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )

        // ── loading ──
        if (isActive) {
            CircularProgressIndicator(
                modifier = Modifier.size(ds.sm(18.dp)),
                color = StepCompletedColor,
                strokeWidth = ds.sm(2.dp),
            )
        }
    }
}
