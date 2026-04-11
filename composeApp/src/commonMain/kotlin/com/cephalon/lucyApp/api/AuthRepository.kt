package com.cephalon.lucyApp.api

import com.cephalon.lucyApp.auth.AuthTokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 适配通用 AuthApi 后的 AuthRepository，支持动态 Map 数据访问。
 */
class AuthRepository(
    private val authApi: AuthApi,
    private val tokenStore: AuthTokenStore
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

    fun logout() {
        _userInfo.value = null
        tokenStore.clear()
    }

    fun hasValidToken(): Boolean = tokenStore.getValidTokenOrNull() != null
}