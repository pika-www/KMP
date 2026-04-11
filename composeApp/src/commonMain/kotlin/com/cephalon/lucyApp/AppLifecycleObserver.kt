package com.cephalon.lucyApp

import androidx.compose.runtime.Composable

@Composable
expect fun BindAppLifecycle(
    onForeground: () -> Unit,
    onBackground: () -> Unit,
)
