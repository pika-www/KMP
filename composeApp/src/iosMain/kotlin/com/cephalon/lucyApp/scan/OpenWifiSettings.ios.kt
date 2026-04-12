package com.cephalon.lucyApp.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberOpenWifiSettings(): () -> Unit {
    return remember {
        {
            val url = NSURL.URLWithString("App-Prefs:root=WIFI")
                ?: NSURL.URLWithString("prefs:root=WIFI")
            if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
                UIApplication.sharedApplication.openURL(url)
            } else {
                // Fallback: 打开系统设置主页
                val settingsUrl = NSURL.URLWithString("App-Prefs:")
                if (settingsUrl != null) {
                    UIApplication.sharedApplication.openURL(settingsUrl)
                }
            }
        }
    }
}
