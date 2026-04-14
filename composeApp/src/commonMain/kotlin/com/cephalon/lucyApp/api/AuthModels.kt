package com.cephalon.lucyApp.api

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

    val code: String? = null,

    @SerialName("track_id")
    val trackId: String,

    @SerialName("app_type")
    val appType: String = "platform",

    val way: String = "phone_pwd"
)

@Serializable
data class CodeRequest(
    val phone: String? = null,
    val email: String? = null,

    @SerialName("action_type")
    val actionType: String,

    @SerialName("app_type")
    val appType: String? = null,
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

@Serializable
data class CloseAccountRequest(
    val phone: String? = null,
    val email: String? = null,
    val code: String,
    val way: String,
)

/**
 * /user/balance 余额响应
 * data: { balances: { "1": 0, "4": 0 } }
 */
@Serializable
data class BalanceData(
    val balances: Map<String, Long> = emptyMap()
)

@Serializable
data class IsExistData(
    @SerialName("is_exist")
    val isExist: Boolean = false
)

/**
 * /user/info 用户信息响应
 */
@Serializable
data class UserInfoData(
    @SerialName("id")
    val userId: String? = null,
    val nickname: String? = null,
    val avatar: String? = null,
    val phone: String? = null,
    val email: String? = null,
)

/**
 * /campaign/recharge/rule/list 充值规则
 * little_value ~ large_value 为充值金额区间（元），gift_percent 为赠送比例(%)
 */
@Serializable
data class RechargeRuleItem(
    val id: String? = null,
    @SerialName("little_value")
    val littleValue: Long = 0,
    @SerialName("large_value")
    val largeValue: Long = 0,
    @SerialName("gift_percent")
    val giftPercent: Int = 0,
    @SerialName("app_source")
    val appSource: String? = null,
)

/**
 * /v1/channels/lucy-app/current-user/connection-flag
 */
@Serializable
data class ConnectionFlagData(
    @SerialName("has_connected_lucy_app")
    val hasConnectedLucyApp: Boolean = false
)

/**
 * /v1/channels/lucy-app/current-user/devices
 */
@Serializable
data class LucyDevicesData(
    val devices: List<LucyDevice> = emptyList()
)

@Serializable
data class LucyDevicePairingInfo(
    @SerialName("channel_device_id")
    val channelDeviceId: String = "",
)

@Serializable
data class LucyDevice(
    val id: String = "",
    val name: String = "",
    @SerialName("serial_number")
    val serialNumber: String = "",
    val status: String = "offline",
    @SerialName("pairing_info")
    val pairingInfo: LucyDevicePairingInfo? = null,
)

val LucyDevice.channelDeviceId: String
    get() = pairingInfo?.channelDeviceId?.trim().orEmpty()

/**
 * POST /v1/channels/lucy-app/feedback
 */
@Serializable
data class FeedbackRequest(
    val title: String = "",
    val category: String = "other",
    val content: String,
    val contact: String = "",
    val images: List<String> = emptyList()
)

@Serializable
data class FeedbackData(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val content: String = "",
    val contact: String = "",
    val images: List<String> = emptyList()
)

/**
 * POST /v1/channels/lucy-app/daily-reward
 */
@Serializable
data class DailyRewardData(
    val granted: Boolean = false,
    @SerialName("reward_amount")
    val rewardAmount: Long = 0
)

/**
 * POST /aiden/lucy-server/v1/channels/lucy/devices/device-bindings
 */
@Serializable
data class DeviceBindingRequest(
    val otp: String,
)

@Serializable
data class DeviceBindingData(
    val cdi: String = "",
    val status: String = "",
)

object AuthInput {
    fun isEmail(input: String): Boolean = input.contains('@')

    fun normalizeAccount(input: String): String = input.trim()
}