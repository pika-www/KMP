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
            response.data.userId?.takeIf { it.isNotBlank() }?.let { tokenStore.saveUserId(it) }
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
        return authApi.put<CloseAccountRequest, Unit>("/user/close", request)
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
            response.data.userId?.takeIf { it.isNotBlank() }?.let { tokenStore.saveUserId(it) }
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
     * 把 Apple StoreKit 2 已验证交易的 transactionId 交给服务端：
     * POST /v1/orders/apple/verify  body: {"transaction_id":"...", "order_id":"..."}
     *
     * 调用时机：`handleRechargePackageClick` 的 Step 3，在 Apple 支付成功（拿到
     * transactionId）之后。Step 1 `createRechargeOrder` 只是在后端预留订单，服务端此时并不
     * 知道对应 Apple 哪一笔 transaction；这一步把两边挂钩，服务端同步完成签名校验与入账，
     * `verified=true` 即视为最终 succeed —— 前端**不再轮询** /orders/transfers/{order_id}。
     *
     * 参数：
     *  - [transactionId] 是 Apple StoreKit 2 `Product.purchase()` 返回的 `transaction.id`。
     *  - [orderId] 是 Step 1 `createRechargeOrder` 响应的 `data.order_id`，一定要同步传。
     *    后端需要 orderId 才能精确定位到要 verify 哪一条 order（单靠 transactionId 反查不
     *    唯一：同一 Apple 账号可能有多笔正在处理的未完成交易，同一 order 也可能在异常场景
     *    下对应多笔 Apple transaction）。
     *
     * 响应 [VerifyTransactionData]：
     *  - `verified=true`  → UI 直接 finishTransaction + 提示成功；
     *  - `verified=false` → `error` 字段是后端拒绝原因（签名错 / 金额错 / orderId 不匹配 /
     *     已被消费等），UI 直接透传给用户，**不** finishTransaction（保留 Apple 未完成交易
     *     给下次启动的 handleUnfinishedTransactions 兜底）。
     */
    suspend fun verifyAppleIAPTransaction(
        transactionId: String,
        orderId: String,
    ): BaseResponse<VerifyTransactionData> {
        val request = mapOf(
            "transaction_id" to transactionId,
            "order_id" to orderId,
        )
        return authApi.post("/orders/apple/verify", request)
    }

    /**
     * 分页拉取脑力值用量记录：
     * GET {baseDomain}/cephalon/user-center/v1/model/record?invoke_type=api&page_index=&page_size=
     *
     * 该接口不走 /v1/aiden/cephalon-app 前缀，故用 [AuthApi.getAbsolute]。
     * [pageIndex] 从 1 开始；[pageSize] 由 UI 侧传入（充值页用 10）。
     * [invokeType] 后端筛选"用户实际调用方式"，当前 UI 固定传 `"api"`。
     */
    suspend fun getModelRecords(
        pageIndex: Int,
        pageSize: Int,
        invokeType: String = "api",
    ): BaseResponse<ModelRecordListData> {
        return authApi.getAbsolute(
            "/cephalon/user-center/v1/model/record",
            mapOf(
                "invoke_type" to invokeType,
                "page_index" to pageIndex.toString(),
                "page_size" to pageSize.toString(),
            ),
        )
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
        val key = userKeyOf(KEY_DAILY_REWARD_DATE) ?: return
        val lastClaimed = settings.getStringOrNull(key)
        if (lastClaimed == today) return

        val resp = authApi.post<Map<String, String>, DailyRewardData>(dailyRewardPath, emptyMap())
        if (resp.code == 20000) {
            settings.putString(key, today)
        }
    }

    // ---- Lucy App 连接 ----

    private val connectPath = "/channels/lucy-app/connect"

    /**
     * 端脑云用户接入：GET /channels/lucy-app/connect
     * 成功后写入本地连接标记，返回 ConnectLucyAppData（含 bootstrap_mission_id 和 bootstrap_status）。
     * code=20000（首次连接）和 code=40088（已连接）都视为成功。
     */
    suspend fun connectLucyApp(): Result<ConnectLucyAppData> {
        appLogD("AuthRepository", "connectLucyApp: 开始请求 $connectPath")
        return try {
            val resp = authApi.get<ConnectLucyAppData>(connectPath)
            appLogD("AuthRepository", "connectLucyApp: code=${resp.code}, msg=${resp.msg}, data=${resp.data}")
            if ((resp.code == 20000 || resp.code == 40088) && resp.data != null) {
                userKeyOf(KEY_CONNECTION_FLAG)?.let { settings.putBoolean(it, true) }
                val missionId = resp.data.bootstrapMissionId
                // 按用户存储 bootstrapMissionId，防止轮询中途 App 被关闭后丢失
                userKeyOf(KEY_BOOTSTRAP_MISSION_ID)?.let { settings.putString(it, missionId) }
                appLogD("AuthRepository", "connectLucyApp: 接入成功, id=${resp.data.id}, bootstrapMissionId=$missionId, status=${resp.data.bootstrapStatus}（已持久化）")
                Result.success(resp.data.copy(responseMsg = resp.msg, responseCode = resp.code))
            } else {
                appLogD("AuthRepository", "connectLucyApp: 接入失败 code=${resp.code} msg=${resp.msg}")
                Result.failure(Exception(resp.msg))
            }
        } catch (e: Exception) {
            appLogD("AuthRepository", "connectLucyApp: 异常 ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 查询用户任务列表：GET /user/missions?page_index=&page_size=&front_state=
     */
    suspend fun getUserMissions(
        pageIndex: Int = 1,
        pageSize: Int = 1,
        frontState: String = "running",
    ): BaseResponse<UserMissionsData> {
        appLogD("AuthRepository", "getUserMissions: pageIndex=$pageIndex, pageSize=$pageSize, frontState=$frontState")
        return authApi.get<UserMissionsData>("/user/missions?page_index=$pageIndex&page_size=$pageSize&front_state=$frontState")
    }

    /**
     * 查询云设备绑定状态：GET /user/missions/{id}/device-binding-status
     */
    suspend fun getDeviceBindingStatus(missionId: String): BaseResponse<DeviceBindingStatusData> {
        appLogD("AuthRepository", "getDeviceBindingStatus: missionId=$missionId")
        return authApi.get<DeviceBindingStatusData>("/user/missions/$missionId/device-binding-status")
    }

    // ---- Lucy App 连接标记 ----

    private val connectionFlagPath = "/channels/lucy-app/current-user/connection-flag"

    /**
     * 检查连接标记：优先读本地缓存，若无缓存则请求接口并存储
     */
    suspend fun checkConnectionFlag(): Boolean {
        val key = userKeyOf(KEY_CONNECTION_FLAG)
        // 优先读本地缓存（userId 未知时跳过缓存，直接走网络）
        if (key != null && settings.getBoolean(key, false)) {
            return true
        }
        // 本地无缓存或 userId 未知，请求接口
        val resp = authApi.get<ConnectionFlagData>(connectionFlagPath)
        val connected = resp.code == 20000 && resp.data?.hasConnectedLucyApp == true
        if (connected) {
            userKeyOf(KEY_CONNECTION_FLAG)?.let { settings.putBoolean(it, true) }
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
            userKeyOf(KEY_CONNECTION_FLAG)?.let { settings.putBoolean(it, true) }
        }
        return success
    }

    /**
     * 本地缓存是否已连接（同步读取，不走网络）
     */
    fun isConnectionFlagCached(): Boolean {
        val key = userKeyOf(KEY_CONNECTION_FLAG) ?: return false
        return settings.getBoolean(key, false)
    }

    /**
     * 仅清掉本地 connection_flag 缓存（保留 token、用户信息等）。
     *
     * 登录入口应在调用 [checkConnectionFlag] 之前先失效一次，避免上一个账号残留的
     * 缓存 flag 让全新账号被错误地直接带进 AgentModel 对话页。
     */
    fun invalidateConnectionFlagCache() {
        userKeyOf(KEY_CONNECTION_FLAG)?.let { settings.remove(it) }
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
                resp.data.serverMsg = resp.msg
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.msg))
            }
        } catch (e: Exception) {
            println("[BrainBox] bindDeviceWithOtp: 异常 ${e.message}")
            Result.failure(e)
        }
    }

    // ---- 淘宝链接 ----

    suspend fun getTaobaoLinks(): TaobaoLinkData? {
        val resp = authApi.get<TaobaoLinkData>("/channels/lucy-app/taobao-link")
        return if (resp.code == 20000) resp.data else null
    }

    // ---- 模型配置 ----

    private val modelConfigJson = Json { ignoreUnknownKeys = true }

    private val _modelConfig = MutableStateFlow<ModelConfigData?>(null)
    val modelConfig: StateFlow<ModelConfigData?> = _modelConfig.asStateFlow()

    init {
        // 延迟加载本地缓存的模型配置（仅当 userId 已知时才使用缓存，防止跨用户污染）
        val configKey = userKeyOf(KEY_MODEL_CONFIG)
        if (configKey != null) {
            val raw = settings.getStringOrNull(configKey)
            if (raw != null) {
                _modelConfig.value = runCatching { modelConfigJson.decodeFromString<ModelConfigData>(raw) }.getOrNull()
            }
        }
    }


    private fun persistModelConfig(data: ModelConfigData) {
        val key = userKeyOf(KEY_MODEL_CONFIG) ?: return
        val raw = modelConfigJson.encodeToString(ModelConfigData.serializer(), data)
        settings.putString(key, raw)
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

    /**
     * 读取当前用户缓存的 bootstrap_mission_id（轮询中途 App 被杀后恢复用）。
     * 返回 null 表示没有未完成的轮询。
     */
    fun getStoredBootstrapMissionId(): String? {
        val key = userKeyOf(KEY_BOOTSTRAP_MISSION_ID) ?: return null
        return settings.getStringOrNull(key)?.takeIf { it.isNotBlank() }
    }

    /**
     * 清除缓存的 bootstrap_mission_id（绑定成功、终态失败时调用）。
     */
    fun clearBootstrapMissionId() {
        userKeyOf(KEY_BOOTSTRAP_MISSION_ID)?.let { settings.remove(it) }
    }

    fun logout() {
        _userInfo.value = null
        _modelConfig.value = null
        // 清除当前用户的缓存 key（在 tokenStore.clear() 之前，因为 clear 会移除 userId）
        userKeyOf(KEY_MODEL_CONFIG)?.let { settings.remove(it) }
        userKeyOf(KEY_CONNECTION_FLAG)?.let { settings.remove(it) }
        userKeyOf(KEY_DAILY_REWARD_DATE)?.let { settings.remove(it) }
        userKeyOf(KEY_BOOTSTRAP_MISSION_ID)?.let { settings.remove(it) }
        tokenStore.clear()
    }

    fun hasValidToken(): Boolean = tokenStore.getValidTokenOrNull() != null

    /**
     * 拼接当前用户的隔离 key：`{base}.{userId}`。
     * userId 未知（未登录/token 过期已清除）时返回 null，调用方应跳过缓存读写。
     */
    private fun userKeyOf(base: String): String? {
        val uid = tokenStore.getCurrentUserId() ?: return null
        return "$base.$uid"
    }

    private companion object {
        const val KEY_CONNECTION_FLAG = "lucy_app.has_connected"
        const val KEY_DAILY_REWARD_DATE = "lucy_app.daily_reward_date"
        const val KEY_MODEL_CONFIG = "lucy_app.model_config"
        const val KEY_BOOTSTRAP_MISSION_ID = "lucy_app.bootstrap_mission_id"
    }
}
