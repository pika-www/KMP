package com.cephalon.lucyApp

import androidx.compose.runtime.Composable

/**
 * 平台侧网络变化监听：当设备默认网络发生切换（Wi‑Fi ↔ 蜂窝、Wi‑Fi A→B 等）时
 * 回调 [onNetworkChanged]，供上层主动断开旧连接并重连，避免 natskt 内部 socket 断开
 * 导致未捕获异常闪退。
 *
 * - Android：通过 ConnectivityManager.registerDefaultNetworkCallback 实现。
 * - iOS：目前不需要（系统和 natskt 对网络变化的容错足够），提供空实现。
 */
@Composable
expect fun BindNetworkMonitor(
    onNetworkChanged: () -> Unit,
)
