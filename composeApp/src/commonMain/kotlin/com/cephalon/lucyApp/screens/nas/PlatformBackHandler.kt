package com.cephalon.lucyApp.screens.nas

import androidx.compose.runtime.Composable

@Composable
internal expect fun PlatformBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
)
