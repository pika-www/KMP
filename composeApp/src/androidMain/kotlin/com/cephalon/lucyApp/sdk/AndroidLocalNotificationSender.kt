package com.cephalon.lucyApp.sdk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.cephalon.lucyApp.screens.agentmodel.AndroidAppContextHolder

class AndroidLocalNotificationSender : LocalNotificationSender {
    companion object {
        private const val CHANNEL_ID = "chat_completion"
        private const val CHANNEL_NAME = "对话完成通知"
        private const val NOTIFICATION_ID = 9001
    }

    override fun sendNotification(title: String, body: String) {
        val context = try {
            AndroidAppContextHolder.appContext
        } catch (_: Exception) {
            return
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }
}
