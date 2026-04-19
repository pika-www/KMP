package com.cephalon.lucyApp

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual val appEnvironment: AppEnvironment = when (
    NSBundle.mainBundle.infoDictionary?.get("AppEnvironment") as? String
) {
    "test" -> AppEnvironment.TEST
    "release" -> AppEnvironment.RELEASE
    else -> AppEnvironment.DEBUG
}