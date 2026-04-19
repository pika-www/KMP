package com.cephalon.lucyApp.payment

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val IAP_LOG_TAG = "[IAP][AppleIAPManager]"

object AppleStoreKitBridgeRegistry {
    private var loadProductsHandler: ((List<String>, (Map<String, Map<String, String>>) -> Unit) -> Unit)? = null
    private var purchaseHandler: ((String, (String, String) -> Unit) -> Unit)? = null
    private var finishTransactionHandler: ((String, (Boolean) -> Unit) -> Unit)? = null
    private var getUnfinishedTransactionsHandler: (((List<String>) -> Unit) -> Unit)? = null

    fun registerLoadProductsHandler(handler: (List<String>, (Map<String, Map<String, String>>) -> Unit) -> Unit) {
        println("$IAP_LOG_TAG registerLoadProductsHandler called")
        loadProductsHandler = handler
    }

    fun registerPurchaseHandler(handler: (String, (String, String) -> Unit) -> Unit) {
        println("$IAP_LOG_TAG registerPurchaseHandler called")
        purchaseHandler = handler
    }

    fun registerFinishTransactionHandler(handler: (String, (Boolean) -> Unit) -> Unit) {
        println("$IAP_LOG_TAG registerFinishTransactionHandler called")
        finishTransactionHandler = handler
    }

    fun registerGetUnfinishedTransactionsHandler(handler: ((List<String>) -> Unit) -> Unit) {
        println("$IAP_LOG_TAG registerGetUnfinishedTransactionsHandler called")
        getUnfinishedTransactionsHandler = handler
    }

    fun loadProducts(
        productIds: List<String>,
        completion: (Map<String, Map<String, String>>) -> Unit,
    ) {
        println("$IAP_LOG_TAG AppleStoreKitBridgeRegistry.loadProducts called, productIds=$productIds")
        val handler = loadProductsHandler
        if (handler == null) {
            println("$IAP_LOG_TAG AppleStoreKitBridgeRegistry.loadProducts handler is not registered")
            completion(emptyMap())
            return
        }
        handler(productIds, completion)
    }

    fun purchase(
        productId: String,
        completion: (String, String) -> Unit,
    ) {
        println("$IAP_LOG_TAG AppleStoreKitBridgeRegistry.purchase called, productId=$productId")
        val handler = purchaseHandler
        if (handler == null) {
            println("$IAP_LOG_TAG AppleStoreKitBridgeRegistry.purchase handler is not registered")
            completion("", "Swift purchase handler not registered")
            return
        }
        handler(productId, completion)
    }

    fun finishTransaction(
        transactionId: String,
        completion: (Boolean) -> Unit,
    ) {
        println("$IAP_LOG_TAG AppleStoreKitBridgeRegistry.finishTransaction called, transactionId=$transactionId")
        val handler = finishTransactionHandler
        if (handler == null) {
            println("$IAP_LOG_TAG AppleStoreKitBridgeRegistry.finishTransaction handler is not registered")
            completion(false)
            return
        }
        handler(transactionId, completion)
    }

    fun getUnfinishedTransactions(
        completion: (List<String>) -> Unit,
    ) {
        println("$IAP_LOG_TAG AppleStoreKitBridgeRegistry.getUnfinishedTransactions called")
        val handler = getUnfinishedTransactionsHandler
        if (handler == null) {
            println("$IAP_LOG_TAG AppleStoreKitBridgeRegistry.getUnfinishedTransactions handler is not registered")
            completion(emptyList())
            return
        }
        handler(completion)
    }
}

class AppleIAPManager : PlatformIAP(), IAPManager {
    private val _products = MutableStateFlow<List<IAPProduct>>(emptyList())
    override val products: StateFlow<List<IAPProduct>> = _products

    private val bridge = AppleStoreKitBridgeRegistry

    override suspend fun createOrder(amount: Long): Pair<String, String>? {
        println("$IAP_LOG_TAG createOrder called, amount=$amount, handled by AuthRepository in common code")
        return null
    }

    override suspend fun initiatePurchase(productId: String): PurchaseOutcome {
        println("$IAP_LOG_TAG initiatePurchase start, productId=$productId")
        return suspendCancellableCoroutine { continuation ->
            bridge.purchase(productId) { transactionId, errorMessage ->
                println("$IAP_LOG_TAG initiatePurchase callback, productId=$productId, transactionId=$transactionId, errorMessage=$errorMessage")
                // Swift 侧 StoreKitBridge.purchase 的 completion 协议：
                //   .success:   completion(transactionId, "")
                //   .pending:   completion("", "Purchase pending")        ← 必须原样匹配
                //   .cancelled: completion("", "Purchase cancelled")      ← 必须原样匹配
                //   .failure:   completion("", <Apple 原话 / 桥接错误>)
                // （若修改 Swift 端文案务必同步改这里，否则会降级到 Failure 分支——
                //   不会导致功能错误，但 UI 语义会从"审核中"变成"支付失败"。）
                val outcome: PurchaseOutcome = when {
                    errorMessage.isEmpty() && transactionId.isNotEmpty() ->
                        PurchaseOutcome.Success(transactionId)
                    errorMessage == "Purchase cancelled" -> PurchaseOutcome.Cancelled
                    errorMessage == "Purchase pending" -> PurchaseOutcome.Pending
                    else -> PurchaseOutcome.Failure(
                        message = errorMessage.ifBlank { "Apple 支付未返回有效结果" },
                    )
                }
                println("$IAP_LOG_TAG initiatePurchase → $outcome")
                continuation.resume(outcome)
            }
        }
    }

    override suspend fun verifyTransaction(transactionId: String): Boolean {
        println("$IAP_LOG_TAG verifyTransaction called, transactionId=$transactionId, handled by AuthRepository in common code")
        return false
    }

    override suspend fun finishTransaction(transactionId: String) {
        println("$IAP_LOG_TAG finishTransaction start, transactionId=$transactionId")
        super.finishTransaction(transactionId)
        suspendCancellableCoroutine<Unit> { continuation ->
            bridge.finishTransaction(transactionId) { success ->
                println("$IAP_LOG_TAG finishTransaction callback, transactionId=$transactionId, success=$success")
                continuation.resume(Unit)
            }
        }
    }

    override suspend fun loadProducts() {
        // 前端不再维护商品 ID 列表：真正 purchase 时 Swift 侧会一次性向 App Store 拉取，
        // 无需预加载。保留此方法仅为兼容 IAPManager 接口。
        println("$IAP_LOG_TAG loadProducts: no-op (products loaded on-demand at purchase time)")
    }

    override fun handleUnfinishedTransactions() {
        println("$IAP_LOG_TAG handleUnfinishedTransactions start")
        bridge.getUnfinishedTransactions { transactionIds ->
            println("$IAP_LOG_TAG handleUnfinishedTransactions callback, transactionIds=$transactionIds")
        }
    }
}
