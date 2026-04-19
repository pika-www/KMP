package com.cephalon.lucyApp.api

import com.cephalon.lucyApp.auth.AuthTokenStore
import com.cephalon.lucyApp.logging.appLogD
import com.cephalon.lucyApp.time.todayDateString
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * 适配通用 AuthApi 后的 AuthRepository，支持动态 Map 数据访问。
 */
class AuthRepository(
    private val authApi: AuthApi,
    private val tokenStore: AuthTokenStore,
    private val settings: Settings,
) {

    /**
     * 登录请求
     */
    suspend fun login(request: LoginRequest): BaseResponse<LoginData> {
        val response = authApi.post<LoginRequest, LoginData>("/login", request)
        val token = response.data?.token
        if (response.code == 20000 && token != null) {
            tokenStore.saveToken(token)
            request.phone?.let { tokenStore.saveUserPhone(it) }
            request.email?.let { tokenStore.saveUserEmail(it) }
        }
        return response
    }

    /**
     * 获取验证码
     */
    suspend fun getCode(phone: String? = null, email: String? = null, actionType: String, appType: String = "lucy"): BaseResponse<Unit> {
        val request = CodeRequest(
            phone = phone,
            email = email,
            actionType = actionType,
            appType = appType
        )
        println(request)
        return authApi.post<CodeRequest, Unit>("/code", request)
    }


    /**
     * 校验手机号是否存在
     * 使用 Map<String, Any?> 接收后端返回的所有动态数据，无需定义数据类。
     */
    suspend fun isPhoneExist(phone: String): BaseResponse<IsExistData> {
        return authApi.get<IsExistData>("/is-phone-exist", mapOf("phone" to phone))
    }

    suspend fun isEmailExist(email: String): BaseResponse<IsExistData> {
        return authApi.get<IsExistData>("/is-email-exist", mapOf("email" to email))
    }

    /**
     * 忘记密码
     */
    suspend fun forgetPassword(request: ForgetPasswordRequest): BaseResponse<Unit> {
        return authApi.post<ForgetPasswordRequest, Unit>("/pwd/forget", request)
    }

    suspend fun closeAccount(request: CloseAccountRequest): BaseResponse<Unit> {
        return authApi.post<CloseAccountRequest, Unit>("/user/close", request)
    }

    /**
     * 查询余额
     * symbol_ids: 1=充值脑力值, 4=免费脑力值
     */
    suspend fun getBalance(symbolIds: List<Int> = listOf(1, 4)): BaseResponse<BalanceData> {
        return authApi.getWithListParams<BalanceData>(
            "/user/balance",
            mapOf("symbol_ids" to symbolIds.map { it.toString() })
        )
    }

    // ---- 用户信息 ----
    private val _userInfo = MutableStateFlow<UserInfoData?>(null)
    val userInfo: StateFlow<UserInfoData?> = _userInfo.asStateFlow()

    /**
     * 获取用户信息 GET /user/info
     */
    suspend fun getUserInfo(): BaseResponse<UserInfoData> {
        val response = authApi.get<UserInfoData>("/user/info")
        if (response.code == 20000 && response.data != null) {
            _userInfo.value = response.data
        }
        return response
    }

    /**
     * 获取充值套餐列表 GET /campaign/recharge/rule/list?app_source=lucy_app
     */
    suspend fun getRechargeRules(): BaseResponse<List<RechargeRuleItem>> {
        return authApi.get<List<RechargeRuleItem>>(
            "/campaign/recharge/rule/list",
            mapOf("app_source" to "lucy_app")
        )
    }

    /**
     * 创建充值订单 POST /orders/transfers
     */
    suspend fun createRechargeOrder(amount: Long): BaseResponse<RechargeOrderData> {
        val request = CreateRechargeOrderRequest(amount = amount)
        return authApi.post("/orders/transfers", request)
    }

    /**
     * 验证 Apple IAP 交易 POST /orders/apple/verify
     *
     * 注：新充值流程改用 [getTransferOrderStatus] 轮询 /orders/transfers/{order_id} 代替本地验单，
     * 本方法暂时保留以防仍有旧调用方依赖。
     */
    suspend fun verifyAppleIAPTransaction(transactionId: String): BaseResponse<VerifyTransactionData> {
        val request = mapOf(
            "transaction_id" to transactionId
        )
        return authApi.post("/orders/apple/verify", request)
    }

    /**
     * 查询充值订单状态 GET /orders/transfers/{order_id}（需要 Bearer token，由 authApi 自带）。
     * 返回 `status` 字段为 [TransferOrderStatus] 之一：pending / succeed / canceled。
     */
    suspend fun getTransferOrderStatus(orderId: String): BaseResponse<TransferOrderStatusData> {
        return authApi.get<TransferOrderStatusData>("/orders/transfers/$orderId")
    }

    // ---- Lucy 设备 ----

    private val devicesPath = "/channels/lucy-app/current-user/devices"

    suspend fun getDevices(): List<LucyDevice> {
        val resp = authApi.get<LucyDevicesData>(devicesPath)
        return if (resp.code == 20000) resp.data?.devices.orEmpty() else emptyList()
    }

    suspend fun findDeviceByChannelDeviceId(channelDeviceId: String): LucyDevice? {
        val normalized = channelDeviceId.trim()
        if (normalized.isBlank()) return null
        return getDevices().firstOrNull { it.channelDeviceId == normalized }
    }

    suspend fun requireDeviceByChannelDeviceId(channelDeviceId: String): Result<LucyDevice> {
        val normalized = channelDeviceId.trim()
        if (normalized.isBlank()) {
            return Result.failure(IllegalArgumentException("channel_device_id 不能为空"))
        }
        val device = findDeviceByChannelDeviceId(normalized)
        return if (device != null) {
            Result.success(device)
        } else {
            Result.failure(IllegalStateException("未找到 channel_device_id=$normalized 对应的设备，请先确认设备已完成绑定"))
        }
    }

    // ---- 意见反馈 ----

    private val feedbackPath = "/channels/lucy-app/feedback"

    suspend fun submitFeedback(request: FeedbackRequest): BaseResponse<FeedbackData> {
        return authApi.post<FeedbackRequest, FeedbackData>(feedbackPath, request)
    }

    // ---- 每日免费脑力值 ----

    private val dailyRewardPath = "/channels/lucy-app/daily-reward"

    /**
     * 领取每日免费脑力值。
     * 本地用 Settings 存储上次领取的日期字符串（yyyy-MM-dd），
     * 若与今天一致则跳过请求，否则调用接口并更新存储。
     */
    suspend fun claimDailyRewardIfNeeded() {
        val today = todayDateString()
        val lastClaimed = settings.getStringOrNull(KEY_DAILY_REWARD_DATE)
        if (lastClaimed == today) return

        val resp = authApi.post<Map<String, String>, DailyRewardData>(dailyRewardPath, emptyMap())
        if (resp.code == 20000) {
            settings.putString(KEY_DAILY_REWARD_DATE, today)
        }
    }

    // ---- Lucy App 连接 ----

    private val connectPath = "/channels/lucy-app/connect"

    /**
     * 端脑云用户接入：GET /channels/lucy-app/connect
     * 成功后写入本地连接标记
     */
    suspend fun connectLucyApp(): Result<Unit> {
        appLogD("AuthRepository", "connectLucyApp: 开始请求 $connectPath")
        return try {
            val resp = authApi.get<Map<String, String>>(connectPath)
            appLogD("AuthRepository", "connectLucyApp: code=${resp.code}, msg=${resp.msg}, data=${resp.data}")
            if (resp.code == 20000) {
                settings.putBoolean(KEY_CONNECTION_FLAG, true)
                appLogD("AuthRepository", "connectLucyApp: 接入成功")
                Result.success(Unit)
            } else {
                appLogD("AuthRepository", "connectLucyApp: 接入失败 code=${resp.code} msg=${resp.msg}")
                Result.failure(Exception(resp.msg))
            }
        } catch (e: Exception) {
            appLogD("AuthRepository", "connectLucyApp: 异常 ${e.message}")
            Result.failure(e)
        }
    }

    // ---- Lucy App 连接标记 ----

    private val connectionFlagPath = "/channels/lucy-app/current-user/connection-flag"

    /**
     * 检查连接标记：优先读本地缓存，若无缓存则请求接口并存储
     */
    suspend fun checkConnectionFlag(): Boolean {
        // 优先读本地缓存
        if (settings.getBoolean(KEY_CONNECTION_FLAG, false)) {
            return true
        }
        // 本地无缓存，请求接口
        val resp = authApi.get<ConnectionFlagData>(connectionFlagPath)
        val connected = resp.code == 20000 && resp.data?.hasConnectedLucyApp == true
        if (connected) {
            settings.putBoolean(KEY_CONNECTION_FLAG, true)
        }
        return connected
    }

    /**
     * 设置连接标记：调接口 + 写本地缓存
     */
    suspend fun setConnectionFlag(): Boolean {
        val resp = authApi.put<ConnectionFlagData>(connectionFlagPath)
        val success = resp.code == 20000
        if (success) {
            settings.putBoolean(KEY_CONNECTION_FLAG, true)
        }
        return success
    }

    /**
     * 本地缓存是否已连接（同步读取，不走网络）
     */
    fun isConnectionFlagCached(): Boolean = settings.getBoolean(KEY_CONNECTION_FLAG, false)

    /**
     * 仅清掉本地 connection_flag 缓存（保留 token、用户信息等）。
     *
     * 登录入口应在调用 [checkConnectionFlag] 之前先失效一次，避免上一个账号残留的
     * 缓存 flag 让全新账号被错误地直接带进 AgentModel 对话页。
     */
    fun invalidateConnectionFlagCache() {
        settings.remove(KEY_CONNECTION_FLAG)
    }

    // ---- 设备绑定（OTP） ----

    private val deviceBindingPath = "/aiden/lucy-server/v1/channels/lucy/devices/device-bindings"

    /**
     * 用 BLE 获取的 OTP 绑定设备。
     * PUT /aiden/lucy-server/v1/channels/lucy/devices/device-bindings
     * body: {"otp":"323584"}
     * 成功响应: {"code":200,"msg":"绑定成功","data":{"cdi":"...","status":"..."}}
     */
    suspend fun bindDeviceWithOtp(otp: String): Result<DeviceBindingData> {
        val rawToken = tokenStore.getTokenOrNull()
        val validToken = tokenStore.getValidTokenOrNull()
        val remaining = tokenStore.getTokenRemainingMillis()
        println("[BrainBox] bindDeviceWithOtp: otp=$otp, rawToken=${rawToken?.take(20)}, validToken=${validToken?.take(20)}, remainingMs=$remaining")
        return try {
            val resp = authApi.putAbsolute<DeviceBindingRequest, DeviceBindingData>(
                deviceBindingPath,
                DeviceBindingRequest(otp = otp),
            )
            println("[BrainBox] bindDeviceWithOtp: code=${resp.code}, msg=${resp.msg}, cdi=${resp.data?.cdi}, status=${resp.data?.status}")
            if (resp.code == 200 && resp.data != null) {
                resp.data.serverMsg = resp.msg ?: "绑定成功"
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.msg))
            }
        } catch (e: Exception) {
            println("[BrainBox] bindDeviceWithOtp: 异常 ${e.message}")
            Result.failure(e)
        }
    }

    // ---- 模型配置 ----

    private val modelConfigJson = Json { ignoreUnknownKeys = true }

    private val _modelConfig = MutableStateFlow<ModelConfigData?>(null)
    val modelConfig: StateFlow<ModelConfigData?> = _modelConfig.asStateFlow()

    init {
        // 延迟加载本地缓存的模型配置
        val raw = settings.getStringOrNull(KEY_MODEL_CONFIG)
        if (raw != null) {
            _modelConfig.value = runCatching { modelConfigJson.decodeFromString<ModelConfigData>(raw) }.getOrNull()
        }
    }


    private fun persistModelConfig(data: ModelConfigData) {
        val raw = modelConfigJson.encodeToString(ModelConfigData.serializer(), data)
        settings.putString(KEY_MODEL_CONFIG, raw)
    }

    /**
     * 获取当前用户模型配置 GET /channels/lucy/current-user/model-config
     */
    suspend fun getModelConfig(): BaseResponse<ModelConfigData> {
        val response = authApi.get<ModelConfigData>("/channels/lucy/current-user/model-config")
        if (response.code == 20000 && response.data != null) {
            _modelConfig.value = response.data
            persistModelConfig(response.data)
        }
        return response
    }

    fun logout() {
        _userInfo.value = null
        _modelConfig.value = null
        settings.remove(KEY_MODEL_CONFIG)
        settings.remove(KEY_CONNECTION_FLAG)
        settings.remove(KEY_DAILY_REWARD_DATE)
        tokenStore.clear()
    }

    fun hasValidToken(): Boolean = tokenStore.getValidTokenOrNull() != null

    private companion object {
        const val KEY_CONNECTION_FLAG = "lucy_app.has_connected"
        const val KEY_DAILY_REWARD_DATE = "lucy_app.daily_reward_date"
        const val KEY_MODEL_CONFIG = "lucy_app.model_config"
    }
}