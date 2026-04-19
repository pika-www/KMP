package com.cephalon.lucyApp.payment

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidIAPManager : PlatformIAP(), IAPManager {
    private val _products = MutableStateFlow<List<IAPProduct>>(emptyList())
    override val products: StateFlow<List<IAPProduct>> = _products

    override suspend fun createOrder(amount: Long): Pair<String, String>? {
        // TODO: Implement Android IAP
        return null
    }

    override suspend fun initiatePurchase(productId: String): PurchaseOutcome {
        // TODO: Implement Android IAP（Google Play Billing / 其它通道），目前 Android 侧不支持 IAP。
        // 返回 Failure 让 UI 直接 toast 可读原因，而不是静默压成 Cancelled。
        return PurchaseOutcome.Failure(message = "Android IAP 尚未实现")
    }

    override suspend fun verifyTransaction(transactionId: String): Boolean {
        // TODO: Implement Android IAP
        return false
    }

    override suspend fun finishTransaction(transactionId: String) {
        // TODO: Implement Android IAP
        super.finishTransaction(transactionId)
    }

    override suspend fun loadProducts() {
        // TODO: Implement Android IAP
        _products.value = emptyList()
    }

    override fun handleUnfinishedTransactions() {
        // TODO: Implement Android IAP
    }
}
