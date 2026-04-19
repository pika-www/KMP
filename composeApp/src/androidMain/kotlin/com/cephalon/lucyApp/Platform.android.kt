package com.cephalon.lucyApp

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual val appEnvironment: AppEnvironment = when (BuildConfig.APP_ENV) {
    "test" -> AppEnvironment.TEST
    "release" -> AppEnvironment.RELEASE
    else -> AppEnvironment.DEBUG
}