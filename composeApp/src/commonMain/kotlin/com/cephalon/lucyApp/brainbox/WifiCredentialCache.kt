package com.cephalon.lucyApp.brainbox

import com.russhwolf.settings.Settings

/**
 * 缓存用户为特定 Wi‑Fi SSID 输入过的密码，让脑花盒子后续配网可以
 * "读本机 SSID + 回填本地缓存密码 → 全自动下发"。
 *
 * 存储位置：App 私有 sandbox（Android SharedPreferences / iOS NSUserDefaults）。
 * 粒度：按 SSID 单条 key，值是最近一次 `configureWifi` 成功时用户输入的明文密码。
 *
 * 说明：此处不做额外加密。该 Wi‑Fi 密码仅为"方便下次免输"的本地缓存，读/写都
 * 仅在同一台设备、同一个 App 沙盒内发生；真正的安全边界由系统 App 沙箱保证。
 */
class WifiCredentialCache(private val settings: Settings) {

    /** 返回 null 表示该 SSID 从未成功配过或被清除过。 */
    fun get(ssid: String): String? {
        val key = keyOf(ssid) ?: return null
        return settings.getStringOrNull(key)?.takeIf { it.isNotEmpty() }
    }

    /** 仅在 configureWifi 成功后调用。空 ssid / 空密码都不缓存（空密码通常是开放 Wi‑Fi）。 */
    fun save(ssid: String, password: String) {
        val key = keyOf(ssid) ?: return
        if (password.isEmpty()) return
        settings.putString(key, password)
    }

    /** 缓存密码跑不通（configureWifi 失败）时清掉这条，避免下次反复失败。 */
    fun remove(ssid: String) {
        val key = keyOf(ssid) ?: return
        settings.remove(key)
    }

    private fun keyOf(ssid: String): String? {
        val trimmed = ssid.trim()
        if (trimmed.isEmpty()) return null
        return KEY_PREFIX + trimmed
    }

    companion object {
        private const val KEY_PREFIX = "brainbox.wifi.pwd."
    }
}
