package com.cephalon.lucyApp.payment

import kotlinx.coroutines.flow.StateFlow

interface IAPManager {
    val products: StateFlow<List<IAPProduct>>
    suspend fun createOrder(amount: Long): Pair<String, String>?

    /**
     * 拉起平台支付并等待结果。
     *
     * 返回 [PurchaseOutcome] 以便 UI 区分 "用户主动取消" / "pending（家庭共享审批等）" /
     * "商品/配置错误"——这些在 Apple 原语义里是完全不同的事，之前统一压成 String? 会让用户
     * 看到的 toast 全是 "支付已取消或未完成"，无法从 UI 区分根因。
     */
    suspend fun initiatePurchase(productId: String): PurchaseOutcome
    suspend fun verifyTransaction(transactionId: String): Boolean
    suspend fun finishTransaction(transactionId: String)
    suspend fun loadProducts()
    fun handleUnfinishedTransactions()
}

/** IAP 拉起结果。各分支一一对应 Apple StoreKit 2 的 Product.PurchaseResult + 前置商品查询阶段的失败。 */
sealed class PurchaseOutcome {
    /** Apple 已验证的成功交易：必有 transactionId，之后调 /orders/transfers/{order_id} 轮询确认服务端入账。 */
    data class Success(val transactionId: String) : PurchaseOutcome()

    /** 用户在 Apple 支付 sheet 点"取消" —— 不是错，不用上报任何错误信息。 */
    data object Cancelled : PurchaseOutcome()

    /**
     * Apple 判定为 pending（典型：未成年人 "Ask to Buy" 等家长审批中）。这时 Apple 侧还**没**完成支付，
     * transactionId 也暂不可得；前端不能开启轮询，应该提示用户稍后再查订单。
     */
    data object Pending : PurchaseOutcome()

    /**
     * 其它所有失败：商品不可购买 / Bundle ID 不匹配 / 沙盒 storefront / Kotlin↔Swift 桥接未注册 / StoreKit verify 失败 …
     * [message] 是 Apple 原话或桥接层的结构化描述，UI 应当直接透传给 toast 便于排障。
     */
    data class Failure(val message: String) : PurchaseOutcome()
}

data class IAPProduct(
    val productId: String,
    val title: String,
    val description: String,
    val price: String,
    val currencyCode: String
)

sealed class IAPResult {
    data class Success(val transactionId: String) : IAPResult()
    data class Failure(val errorMessage: String) : IAPResult()
}
