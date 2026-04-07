package com.cephalon.lucyApp.screens.localdeploy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.media.PlatformImagePreview

@Composable
internal fun LocalDeployTestImagePreview(
    uri: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val overlayInteractionSource = remember { MutableInteractionSource() }
    val closeInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(
                interactionSource = overlayInteractionSource,
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF111111),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.8f)
                .pointerInput(uri) {
                    detectTapGestures(onTap = {})
                }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PlatformImagePreview(
                    uri = uri,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0x66000000),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clickable(
                            interactionSource = closeInteractionSource,
                            indication = null,
                            onClick = onDismiss
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close preview",
                        tint = Color.White,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}
