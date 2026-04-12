package com.cephalon.lucyApp.sdk

import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

class IosLocalNotificationSender : LocalNotificationSender {
    override fun sendNotification(title: String, body: String) {
        val center = UNUserNotificationCenter.currentNotificationCenter()

        // 请求权限（首次会弹窗，后续静默）
        center.requestAuthorizationWithOptions(
            options = platform.UserNotifications.UNAuthorizationOptionAlert or
                platform.UserNotifications.UNAuthorizationOptionSound,
        ) { granted, _ ->
            if (!granted) return@requestAuthorizationWithOptions

            val content = UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(body)
                setSound(platform.UserNotifications.UNNotificationSound.defaultSound)
            }
            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                timeInterval = 0.1,
                repeats = false,
            )
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = "chat_completion",
                content = content,
                trigger = trigger,
            )
            center.addNotificationRequest(request, withCompletionHandler = null)
        }
    }
}
