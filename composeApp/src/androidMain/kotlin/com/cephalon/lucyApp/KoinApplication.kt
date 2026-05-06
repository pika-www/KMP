package com.cephalon.lucyApp

import android.app.Application
import com.cephalon.lucyApp.di.initKoin
import com.cephalon.lucyApp.screens.agentmodel.AndroidAppContextHolder

class KoinApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppContextHolder.appContext = applicationContext
        initKoin()
        installGlobalExceptionGuard()
    }

    /**
     * 兜底：natskt 等第三方库的内部协程可能在自己的 scope 上抛出未捕获异常
     * （如切 Wi-Fi 时 ClosedByteChannelException），若不拦截会直接杀进程。
     * 这里只吞噬已知的可恢复网络错误，其余仍交回系统默认 handler。
     */
    private fun installGlobalExceptionGuard() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val isRecoverableNetworkError = isRecoverableTransportException(throwable)
            if (isRecoverableNetworkError) {
                println("[App] 全局兜底: 吞噬可恢复网络异常 ${throwable::class.simpleName}: ${throwable.message}")
            } else {
                // 非网络类异常 → 交回系统默认 handler（弹崩溃对话框）
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun isRecoverableTransportException(throwable: Throwable): Boolean {
        var current: Throwable? = throwable
        while (current != null) {
            val name = current::class.simpleName.orEmpty()
            val msg = (current.message ?: "").lowercase()
            if (name == "ClosedByteChannelException" ||
                name == "ClosedChannelException" ||
                msg.contains("software caused connection abort") ||
                msg.contains("connection reset") ||
                msg.contains("broken pipe") ||
                msg.contains("end of stream") ||
                msg.contains("network unreachable") ||
                msg.contains("host unreachable")
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }
}