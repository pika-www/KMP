package com.cephalon.lucyApp.sdk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class NasJsonParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun nasJsonErrorMessage_objectWithCodeAndMessage() {
        val el = json.parseToJsonElement(
            """{"code":"index_failed","message":"ignored extension or index error"}""",
        )
        assertEquals(
            "[index_failed] ignored extension or index error",
            nasJsonErrorMessage(el),
        )
    }

    @Test
    fun nasJsonErrorMessage_primitiveString() {
        assertEquals("oops", nasJsonErrorMessage(JsonPrimitive("oops")))
    }

    @Test
    fun nasJsonErrorMessage_nullAndJsonNull() {
        assertNull(nasJsonErrorMessage(null))
        assertNull(nasJsonErrorMessage(JsonNull))
    }

    @Test
    fun nasJsonOptionalString_primitiveAndNull() {
        assertEquals("42", nasJsonOptionalString(JsonPrimitive("42")))
        assertNull(nasJsonOptionalString(JsonNull))
        assertNull(nasJsonOptionalString(null))
    }

    @Test
    fun fileRegisterBlobsRsp_payloadFromLogs_doesNotThrow() {
        val payload =
            """{"cmd":"file_register_blobs_rsp","file_register_blobs_rsp":{"error":null,"ok":false,"results":[{"blobRef":"blobacrd22eaerzjwdtr6nri42mxaawtdmecgpcncyxlodp56ytp6xtciajpnb2hi4dthixs6zlvmmys2mjoojswyylzfzxdaltjojxwqlldmfxgc4tzfzuxe33ifzwgs3tlfyxqmaakemsjti64aiahrw5idcocwagavap5li64aiasicefeoired7lht7nz776gpwej67faiasicmbjkg5akuufdkpn77633boh67faiasicmjjkgrsidu4rqxr776ce6kf67faia4pxe2pv4b2rfcmug2jbuclqxjrbsjaz3u5eqvmid4ueuq3233l5a","error":{"code":"index_failed","message":"ignored extension or index error"},"id":null,"ok":false}]}}"""
        val root = json.parseToJsonElement(payload).jsonObject
        val body = root["file_register_blobs_rsp"]!!.jsonObject
        val item = body["results"]!!.jsonArray.first().jsonObject
        assertNull(nasJsonOptionalString(item["id"]))
        assertEquals(
            "[index_failed] ignored extension or index error",
            nasJsonErrorMessage(item["error"]),
        )
    }
}
