package com.cephalon.lucyApp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun HalfModalBottomSheet(
    onDismissRequest: () -> Unit,
    onDismissed: () -> Unit,
    isVisible: Boolean, // 控制显示/隐藏
    onBack: (() -> Unit)? = null,
    showBackButton: Boolean = onBack != null,
    showCloseButton: Boolean = true,
    showTopBar: Boolean = true,
    containerShape: Shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    containerColor: Color = Color.White,
    topPadding: Dp = 60.dp,
    contentPadding: PaddingValues = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val latestOnDismissed = rememberUpdatedState(onDismissed)
    val latestOnDismissRequest = rememberUpdatedState(onDismissRequest)

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissKeyboard = {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    val density = LocalDensity.current
    val dragDismissThresholdPx = with(density) { 120.dp.toPx() }
    val scope = rememberCoroutineScope()
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val settleAnim = remember { Animatable(0f) }
    var settleJob: Job? = remember { null }

    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = isVisible

    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
        if (visibleState.isIdle && !visibleState.currentState) {
            settleJob?.cancel()
            settleJob = null
            dragOffsetPx = 0f
            settleAnim.snapTo(0f)
            latestOnDismissed.value()
        }
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            settleJob?.cancel()
            settleJob = null
            dragOffsetPx = 0f
            settleAnim.snapTo(0f)
        }
    }

    if (!visibleState.currentState && !visibleState.targetState) return

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(animationSpec = tween(delayMillis = 90, durationMillis = 220)),
            exit = fadeOut(animationSpec = tween(durationMillis = 140))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        dismissKeyboard()
                        latestOnDismissRequest.value()
                    }
            )
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                visibleState = visibleState,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(delayMillis = 90, durationMillis = 360)
                ) + fadeIn(animationSpec = tween(delayMillis = 90, durationMillis = 160)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeOut(animationSpec = tween(durationMillis = 120))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(top = topPadding)
                        .graphicsLayer { translationY = dragOffsetPx }
                        .clip(containerShape)
                        .background(containerColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { dismissKeyboard() }
                        .imePadding()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    settleJob?.cancel()
                                    settleJob = null
                                    dragOffsetPx = (dragOffsetPx + delta).coerceAtLeast(0f)
                                },
                                onDragStopped = {
                                    if (dragOffsetPx >= dragDismissThresholdPx) {
                                        latestOnDismissRequest.value()
                                    } else {
                                        settleJob?.cancel()
                                        settleJob = scope.launch {
                                            settleAnim.stop()
                                            settleAnim.snapTo(dragOffsetPx)
                                            settleAnim.animateTo(
                                                0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            ) {
                                                dragOffsetPx = value
                                            }
                                            dragOffsetPx = 0f
                                        }
                                    }
                                }
                            )
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (showTopBar) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (showBackButton) {
                                        IconButton(
                                            onClick = { onBack?.invoke() ?: latestOnDismissRequest.value() },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE6E6E6))
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back",
                                                tint = Color(0xFF2D2D2D),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(40.dp))
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    if (showCloseButton) {
                                        IconButton(
                                            onClick = { latestOnDismissRequest.value() },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE6E6E6))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close",
                                                tint = Color(0xFF2D2D2D),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(40.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(contentPadding)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}