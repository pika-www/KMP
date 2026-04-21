package com.cephalon.lucyApp

import androidx.compose.runtime.Composable

@Composable
actual fun BindNetworkMonitor(
    onNetworkChanged: () -> Unit,
) {
    // iOS 不需要额外的网络监听：系统和 natskt 对网络变化的容错足够。
}
