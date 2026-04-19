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
    get() {
        val fromPairing = pairingInfo?.channelDeviceId?.trim().orEmpty()
        return fromPairing.ifEmpty { id.trim() }
    }

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
) {
    /** 服务端响应 msg，由 Repository 层填入，不参与序列化 */
    @kotlinx.serialization.Transient
    var serverMsg: String = ""
}

/**
 * /v1/orders/transfers 创建充值订单请求
 */
@Serializable
data class CreateRechargeOrderRequest(
    val type: String = "recharge_apple_iap",
    val amount: Long
)

/**
 * /v1/orders/transfers 创建充值订单响应数据
 */
@Serializable
data class RechargeOrderData(
    @SerialName("order_id")
    val orderId: String,
    @SerialName("product_id")
    val productId: String
)

/**
 * /v1/orders/apple/verify 验证交易响应数据。
 *
 * **所有字段都给默认值**：后端在业务错误（如 code=40003 `Apple transaction already bound to
 * another order`）时返回 `{"data":{}}`，如果 `verified` 是 required `Boolean`，kotlinx 反序列化
 * 会抛 `JsonConvertException`，被 AuthApi.post 的 catch 压成 `code=-1` + 反序列化 trace，
 * **真实业务 code/msg 就彻底丢了**。把字段都加默认值后，`{"data":{}}` 能反序列化为
 * `VerifyTransactionData(verified=false, …)`，外层的 code/msg（业务失败原因）得以正确透传。
 */
@Serializable
data class VerifyTransactionData(
    val verified: Boolean = false,
    @SerialName("order_id")
    val orderId: String? = null,
    val error: String? = null
)

/**
 * GET /cephalon/user-center/v1/model/record 响应 data 字段。
 *
 * 分页协议：请求携带 `page_index`（从 1 起）与 `page_size`；响应回显它们以及 `total`。
 * 前端用 "本页长度 < page_size" 作为"已加载到末页"的启发式——total 字段仅供展示参考，
 * 不作为分页终止条件（total 计算口径在后端可能不实时，信 list.size 更稳）。
 */
@Serializable
data class ModelRecordListData(
    val list: List<ModelRecordItem> = emptyList(),
    @SerialName("page_index")
    val pageIndex: Int = 0,
    @SerialName("page_size")
    val pageSize: Int = 0,
    val total: Int = 0,
)

/**
 * 单条脑力值用量记录。**只解析 UI 需要的字段**，其余由全局 `ignoreUnknownKeys=true`
 * 自动忽略（api_token / invoke_times / token_cost / record_time 等都不需要）。
 *
 * 展示仅用 3 个字段：
 *  - [createdAt] 记录创建时间，ISO 8601，如 `"2026-04-19T15:52:46.508505+08:00"`，
 *    UI 侧 `formatRecordTime()` 会裁到 `"2026.04.19 15:52:46"`。
 *  - `edges.model.name` 模型名（如 `"kimi-k2.5"`）。
 *  - [inputCepCost] + [outputCepCost] 合计为用户在这条记录里消耗的脑力值。
 */
@Serializable
data class ModelRecordItem(
    val id: String = "",
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("input_cep_cost")
    val inputCepCost: Long = 0,
    @SerialName("output_cep_cost")
    val outputCepCost: Long = 0,
    val edges: ModelRecordEdges? = null,
)

@Serializable
data class ModelRecordEdges(
    val model: ModelRecordModelInfo? = null,
)

@Serializable
data class ModelRecordModelInfo(
    val name: String = "",
)

/**
 * GET /v1/channels/lucy/current-user/model-config
 */
@Serializable
data class ModelConfigData(
    val channel: String = "",
    @SerialName("provider_id")
    val providerId: String = "",
    @SerialName("api_key")
    val apiKey: String = "",
    @SerialName("base_url")
    val baseUrl: String = "",
    @SerialName("default_model_id")
    val defaultModelId: String = "",
    val created: Boolean = false,
    val models: List<ModelItem> = emptyList(),
)

@Serializable
data class ModelItem(
    val id: String = "",
    val label: String = "",
    val enabled: Boolean = true,
    @SerialName("is_default")
    val isDefault: Boolean = false,
)

object AuthInput {
    fun isEmail(input: String): Boolean = input.contains('@')

    fun normalizeAccount(input: String): String = input.trim()
}