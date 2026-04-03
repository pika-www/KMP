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

    fun clear() {
        settings.remove(KEY_TOKEN)
    }

    fun isTokenExpired(): Boolean {
        val token = getTokenOrNull() ?: return true
        val expMillis = JwtUtils.getExpMillisOrNull(token) ?: return false
        return currentTimeMillis() >= expMillis
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
    }
}
