package com.cephalon.lucyApp.auth

import io.ktor.util.decodeBase64Bytes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object JwtUtils {
    private val json = Json { ignoreUnknownKeys = true }

    fun getExpMillisOrNull(jwt: String): Long? {
        val raw = jwt.removePrefix("Bearer ").trim()
        val parts = raw.split('.')
        if (parts.size < 2) return null

        val payload = parts[1]
        val payloadBytes = decodeBase64Url(payload) ?: return null
        val payloadString = payloadBytes.decodeToString()

        val obj = try {
            json.parseToJsonElement(payloadString).jsonObject
        } catch (_: Throwable) {
            return null
        }

        val expSeconds = obj["exp"]?.jsonPrimitive?.content?.toLongOrNull() ?: return null
        return expSeconds * 1000L
    }

    private fun decodeBase64Url(value: String): ByteArray? {
        val normalized = value
            .replace('-', '+')
            .replace('_', '/')
            .let { v ->
                val pad = (4 - (v.length % 4)) % 4
                v + "=".repeat(pad)
            }

        return try {
            normalized.decodeBase64Bytes()
        } catch (_: Throwable) {
            null
        }
    }
}
