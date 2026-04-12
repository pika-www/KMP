package com.cephalon.lucyApp.sdk

actual fun createLocalNotificationSender(): LocalNotificationSender = IosLocalNotificationSender()
