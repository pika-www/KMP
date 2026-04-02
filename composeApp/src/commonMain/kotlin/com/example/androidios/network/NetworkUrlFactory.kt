package com.example.androidios.network

import io.ktor.http.URLBuilder
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom

class NetworkUrlFactory(private val config: NetworkConfig) {

    fun http(path: String): String {
        return URLBuilder().takeFrom(config.baseUrl).apply {
            encodedPath = normalizePath(path)
        }.buildString()
    }

    fun webSocket(path: String): String {
        return URLBuilder().takeFrom(config.baseUrl).apply {
            protocol = when (protocol.name.lowercase()) {
                "https" -> io.ktor.http.URLProtocol.WSS
                else -> io.ktor.http.URLProtocol.WS
            }
            encodedPath = normalizePath(path)
        }.buildString()
    }

    private fun normalizePath(path: String): String {
        return if (path.startsWith("/")) path else "/$path"
    }
}
