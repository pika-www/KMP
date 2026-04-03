package com.cephalon.lucyApp

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

object IOSViewControllerHolder {
    var rootViewController: UIViewController? = null
}

fun MainViewController(): UIViewController {
    val controller = ComposeUIViewController { App() }
    IOSViewControllerHolder.rootViewController = controller
    return controller
}
