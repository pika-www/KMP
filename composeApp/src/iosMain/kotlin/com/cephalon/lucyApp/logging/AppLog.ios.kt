package com.cephalon.lucyApp.logging

import platform.Foundation.NSLog

actual fun appLogD(tag: String, message: String) {
    NSLog("[$tag] $message")
}
