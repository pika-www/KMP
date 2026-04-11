package com.cephalon.lucyApp

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook

object IOSViewControllerHolder {
    var rootViewController: UIViewController? = null
}

@OptIn(ExperimentalNativeApi::class)
fun MainViewController(): UIViewController {
    // 全局兜底：防止第三方库（如 natskt）内部协程的未捕获异常导致闪退
    setUnhandledExceptionHook { throwable ->
        println("iOS 未捕获异常(已拦截): ${throwable.message}")
        throwable.printStackTrace()
    }

    val controller = ComposeUIViewController { App() }
    IOSViewControllerHolder.rootViewController = controller
    return controller
}
