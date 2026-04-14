package com.cephalon.lucyApp.deviceaccess.gatt

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

class JsonCommandDispatcher(
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    },
) {
    fun encodeJsonObject(payload: JsonObject): ByteArray {
        return json.encodeToString(payload).encodeToByteArray()
    }

    fun decodeJsonObject(bytes: ByteArray): JsonObject {
        return json.decodeFromString(bytes.decodeToString())
    }

    inline fun <reified T> encode(value: T): ByteArray {
        return json.encodeToString(value).encodeToByteArray()
    }

    inline fun <reified T> decode(bytes: ByteArray): T {
        return json.decodeFromString(bytes.decodeToString())
    }

    inline fun <reified T> decode(payload: JsonObject): T {
        return json.decodeFromJsonElement(payload)
    }

    inline fun <reified T> toJsonObject(value: T): JsonObject {
        return json.encodeToJsonElement(value) as JsonObject
    }
}
