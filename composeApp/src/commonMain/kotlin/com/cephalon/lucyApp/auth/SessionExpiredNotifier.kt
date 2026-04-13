package com.cephalon.lucyApp.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 全局会话过期通知器。
 * 当接口返回 code=40000（未登录）时触发，上层（RootComponent）监听后执行退出逻辑。
 */
object SessionExpiredNotifier {
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expired: SharedFlow<Unit> = _expired.asSharedFlow()

    fun fire() {
        _expired.tryEmit(Unit)
    }
}
