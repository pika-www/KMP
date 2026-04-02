package com.example.androidios.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * 登录请求模型
 */
@Serializable
data class LoginRequest(
    val phone: String? = null,

    val email: String? = null,

    val pwd: String? = null,

    @SerialName("confirm_pwd")
    val confirmPwd: String? = null,

    @SerialName("track_id")
    val trackId: String,

    @SerialName("app_type")
    val appType: String = "platform",

    val way: String = "phone_pwd"
)

@Serializable
data class CodeRequest(
    @SerialName("action_type")
    val actionType: String,

    @SerialName("app_type")
    val appType: String? = null,

    val email: String? = null,
    val phone: String? = null,

    @SerialName("point_dots")
    val pointDots: String = "",

    @SerialName("secret_key")
    val secretKey: String = ""
)

@Serializable
data class ForgetPasswordRequest(
    val account: String,
    val code: String,

    @SerialName("confirm_pwd")
    val confirmPwd: String,

    val pwd: String,

    val type: String
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

object AuthInput {
    fun isEmail(input: String): Boolean = input.contains('@')

    fun normalizeAccount(input: String): String = input.trim()
}