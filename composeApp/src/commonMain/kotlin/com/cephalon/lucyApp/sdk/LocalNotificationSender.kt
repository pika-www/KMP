package com.cephalon.lucyApp.sdk

interface LocalNotificationSender {
    fun sendNotification(title: String, body: String)
}
