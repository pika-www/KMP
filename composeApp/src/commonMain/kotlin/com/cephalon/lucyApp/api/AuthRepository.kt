package com.cephalon.lucyApp.api

import com.cephalon.lucyApp.auth.AuthTokenStore
import com.cephalon.lucyApp.time.todayDateString
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    // ---- Lucy 设备 ----

    private val devicesPath = "/channels/lucy-app/current-user/devices"

    suspend fun getDevices(): List<LucyDevice> {
        val resp = authApi.get<LucyDevicesData>(devicesPath)
        return if (resp.code == 20000) resp.data?.devices.orEmpty() else emptyList()
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

    fun logout() {
        _userInfo.value = null
        settings.remove(KEY_CONNECTION_FLAG)
        tokenStore.clear()
    }

    fun hasValidToken(): Boolean = tokenStore.getValidTokenOrNull() != null

    private companion object {
        const val KEY_CONNECTION_FLAG = "lucy_app.has_connected"
        const val KEY_DAILY_REWARD_DATE = "lucy_app.daily_reward_date"
    }
}