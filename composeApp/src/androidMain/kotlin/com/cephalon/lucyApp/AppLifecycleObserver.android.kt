package com.cephalon.lucyApp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
actual fun BindAppLifecycle(
    onForeground: () -> Unit,
    onBackground: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnForeground = rememberUpdatedState(onForeground)
    val currentOnBackground = rememberUpdatedState(onBackground)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> currentOnForeground.value()
                Lifecycle.Event.ON_STOP -> currentOnBackground.value()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
