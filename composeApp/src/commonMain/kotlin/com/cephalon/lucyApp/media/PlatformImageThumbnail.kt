package com.cephalon.lucyApp.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformImageThumbnail(
    uri: String,
    modifier: Modifier = Modifier,
)
