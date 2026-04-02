package com.example.androidios.network

/**
 * 网络基础配置，只负责环境与域名，不承载业务路径。
 */
data class NetworkConfig(
    val baseUrl: String,
    val timeoutMillis: Long
)

object NetworkPaths {
    const val API_PREFIX = "/cephalon/user-center/v1"
}
