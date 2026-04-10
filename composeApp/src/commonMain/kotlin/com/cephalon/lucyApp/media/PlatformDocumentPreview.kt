package com.cephalon.lucyApp.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformDocumentPreview(
    source: String,
    fileName: String,
    modifier: Modifier = Modifier,
)
