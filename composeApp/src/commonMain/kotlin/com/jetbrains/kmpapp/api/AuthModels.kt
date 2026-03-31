package com.jetbrains.kmpapp.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * 登录请求模型
 */
@Serializable
data class LoginRequest(
    val phone: String,
    val pwd: String,

    @SerialName("app_type")
    val appType: String = "platform",

    val way: String = "phone_pwd"
)

/**
 * 通用响应包装类
 */
@Serializable
data class BaseResponse<T>(
    val code: Int,
    val msg: String,
    val data: T? = null
)

/**
 * 登录成功后的用户信息模型
 */
@Serializable
data class LoginData(
    val token: String? = null,
    val userId: String? = null,
    val nickname: String? = null
)