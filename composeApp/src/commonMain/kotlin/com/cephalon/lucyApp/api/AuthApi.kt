package com.cephalon.lucyApp.api

import com.cephalon.lucyApp.network.NetworkPaths
import com.cephalon.lucyApp.network.NetworkUrlFactory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * 修正后的认证接口定义，支持返回通用的 Map 类型，以便接收后端返回的所有动态数据。
 */
class AuthApi(
    @PublishedApi internal val client: HttpClient,
    @PublishedApi internal val urlFactory: NetworkUrlFactory
) {
    @PublishedApi internal val prefix = NetworkPaths.API_PREFIX

    /**
     * 通用的 GET 请求方法
     * 如果你想接收所有值，R 可以传入 Map<String, Any?>
     */
    suspend inline fun <reified R> get(path: String, params: Map<String, String> = emptyMap()): BaseResponse<R> {
        return try {
            client.get(urlFactory.http("$prefix$path")) {
                params.forEach { (key, value) -> parameter(key, value) }
            }.body()
        } catch (e: Exception) {
            BaseResponse(code = -1, msg = e.message ?: "网络连接失败")
        }
    }

    /**
     * 通用的 POST 请求方法
     */
    suspend inline fun <reified T, reified R> post(path: String, body: T): BaseResponse<R> {
        return try {
            client.post(urlFactory.http("$prefix$path")) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
        } catch (e: Exception) {
            BaseResponse(code = -1, msg = e.message ?: "网络连接失败")
        }
    }

    /**
     * 通用的 PUT 请求方法
     */
    suspend inline fun <reified T, reified R> put(path: String, body: T): BaseResponse<R> {
        return try {
            client.put(urlFactory.http("$prefix$path")) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
        } catch (e: Exception) {
            BaseResponse(code = -1, msg = e.message ?: "网络连接失败")
        }
    }

    /**
     * 通用的 DELETE 请求方法
     */
    suspend inline fun <reified R> delete(path: String, params: Map<String, String> = emptyMap()): BaseResponse<R> {
        return try {
            client.delete(urlFactory.http("$prefix$path")) {
                params.forEach { (key, value) -> parameter(key, value) }
            }.body()
        } catch (e: Exception) {
            BaseResponse(code = -1, msg = e.message ?: "网络连接失败")
        }
    }
}