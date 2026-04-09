package com.cephalon.lucyApp.logging

actual fun appLogD(tag: String, message: String) {
    println("D/$tag: $message")
}
