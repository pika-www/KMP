package com.cephalon.lucyApp.time

import platform.Foundation.NSDate

private const val UNIX_EPOCH_OFFSET_SECONDS: Double = 978307200.0

actual fun currentTimeMillis(): Long {
    val seconds = NSDate().timeIntervalSinceReferenceDate + UNIX_EPOCH_OFFSET_SECONDS
    return (seconds * 1000.0).toLong()
}
