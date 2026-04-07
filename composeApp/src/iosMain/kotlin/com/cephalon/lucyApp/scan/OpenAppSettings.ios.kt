package com.cephalon.lucyApp.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

@Composable
actual fun rememberOpenAppSettings(): () -> Unit {
    return remember {
        {
            val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return@remember
            val app = UIApplication.sharedApplication
            if (app.canOpenURL(url)) {
                app.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
            }
        }
    }
}
