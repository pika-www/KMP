package com.cephalon.lucyApp.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun QrScannerView(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onQrCodeScanned: (String) -> Unit,
)
