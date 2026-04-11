package com.cephalon.lucyApp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification

@Composable
actual fun BindAppLifecycle(
    onForeground: () -> Unit,
    onBackground: () -> Unit,
) {
    val currentOnForeground = rememberUpdatedState(onForeground)
    val currentOnBackground = rememberUpdatedState(onBackground)

    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val foregroundObserver =
            center.addObserverForName(
                name = UIApplicationDidBecomeActiveNotification,
                `object` = null,
                queue = null,
            ) { _ ->
                currentOnForeground.value()
            }
        val backgroundObserver =
            center.addObserverForName(
                name = UIApplicationDidEnterBackgroundNotification,
                `object` = null,
                queue = null,
            ) { _ ->
                currentOnBackground.value()
            }

        onDispose {
            center.removeObserver(foregroundObserver)
            center.removeObserver(backgroundObserver)
        }
    }
}
