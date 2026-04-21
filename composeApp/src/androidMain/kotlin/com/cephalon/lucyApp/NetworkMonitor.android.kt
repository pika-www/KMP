package com.cephalon.lucyApp

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.cephalon.lucyApp.logging.appLogD

private const val TAG = "NetworkMonitor"

@Composable
actual fun BindNetworkMonitor(
    onNetworkChanged: () -> Unit,
) {
    val context = LocalContext.current
    val currentOnNetworkChanged = rememberUpdatedState(onNetworkChanged)

    DisposableEffect(context) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        // 标记是否已经收到过初始回调；首次 onAvailable 不视为"网络变化"
        var hasReceivedInitialCallback = false

        var registeredCallback: ConnectivityManager.NetworkCallback? = null

        if (connectivityManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (!hasReceivedInitialCallback) {
                        hasReceivedInitialCallback = true
                        appLogD(TAG, "初始网络就绪，不触发重连")
                        return
                    }
                    appLogD(TAG, "网络可用 (onAvailable)，触发重连")
                    currentOnNetworkChanged.value()
                }

                override fun onLost(network: Network) {
                    hasReceivedInitialCallback = true
                    appLogD(TAG, "网络丢失 (onLost)，触发重连")
                    currentOnNetworkChanged.value()
                }
            }
            runCatching {
                connectivityManager.registerDefaultNetworkCallback(callback)
                registeredCallback = callback
            }.onFailure {
                appLogD(TAG, "注册 DefaultNetworkCallback 失败: ${it.message}")
            }
        } else {
            appLogD(TAG, "ConnectivityManager 不可用或 API < 24，跳过网络监听")
        }

        onDispose {
            registeredCallback?.let { cb ->
                runCatching { connectivityManager?.unregisterNetworkCallback(cb) }
            }
        }
    }
}
