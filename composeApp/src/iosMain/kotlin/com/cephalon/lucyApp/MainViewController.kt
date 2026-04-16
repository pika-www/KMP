package com.cephalon.lucyApp

import androidx.compose.ui.window.ComposeUIViewController
import com.cephalon.lucyApp.payment.IAPManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import platform.UIKit.UIViewController
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook

object IOSViewControllerHolder {
    var rootViewController: UIViewController? = null
}

private object IOSKoinComponent : KoinComponent

@OptIn(ExperimentalNativeApi::class)
fun MainViewController(): UIViewController {
    // 全局兜底：防止第三方库（如 natskt）内部协程的未捕获异常导致闪退
    setUnhandledExceptionHook { throwable ->
        println("iOS 未捕获异常(已拦截): ${throwable.message}")
        throwable.printStackTrace()
    }

    val iapManager = runCatching { IOSKoinComponent.get<IAPManager>() }
        .onFailure { println("MainViewController: failed to resolve IAPManager from Koin: ${it.message}") }
        .getOrNull()

    val controller = ComposeUIViewController { App(iapManager = iapManager) }
    IOSViewControllerHolder.rootViewController = controller
    return controller
}
