package com.cephalon.lucyApp.logging

import android.util.Log

actual fun appLogD(tag: String, message: String) {
    Log.d(tag, message)
}
