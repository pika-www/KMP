package com.cephalon.lucyApp.auth

import com.cephalon.lucyApp.time.currentTimeMillis
import com.russhwolf.settings.Settings

class AuthTokenStore(
    private val settings: Settings
) {
    fun getTokenOrNull(): String? {
        val token = settings.getStringOrNull(KEY_TOKEN) ?: return null
        if (token.isBlank()) return null
        return token
    }

    fun saveToken(token: String) {
        settings.putString(KEY_TOKEN, token)
    }

    fun saveUserPhone(phone: String) {
        settings.putString(KEY_PHONE, phone)
    }

    fun getUserPhone(): String? = settings.getStringOrNull(KEY_PHONE)

    fun saveUserEmail(email: String) {
        settings.putString(KEY_EMAIL, email)
    }

    fun getUserEmail(): String? = settings.getStringOrNull(KEY_EMAIL)

    fun clear() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_PHONE)
        settings.remove(KEY_EMAIL)
    }

    fun isTokenExpired(): Boolean {
        val token = getTokenOrNull() ?: return true
        val expMillis = JwtUtils.getExpMillisOrNull(token) ?: return false
        return currentTimeMillis() >= expMillis
    }

    /** 返回 token 剩余有效时间（毫秒），若无 token 或已过期返回 0 */
    fun getTokenRemainingMillis(): Long {
        val token = getTokenOrNull() ?: return 0L
        val expMillis = JwtUtils.getExpMillisOrNull(token) ?: return Long.MAX_VALUE
        return (expMillis - currentTimeMillis()).coerceAtLeast(0L)
    }

    fun getValidTokenOrNull(): String? {
        val token = getTokenOrNull() ?: return null
        if (isTokenExpired()) {
            clear()
            return null
        }
        return token
    }

    private companion object {
        private const val KEY_TOKEN = "auth.token"
        private const val KEY_PHONE = "auth.phone"
        private const val KEY_EMAIL = "auth.email"
    }
}
