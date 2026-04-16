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

    override suspend fun initiatePurchase(productId: String): String? {
        println("$IAP_LOG_TAG initiatePurchase start, productId=$productId")
        return suspendCancellableCoroutine<String?> { continuation ->
            bridge.purchase(productId) { transactionId, errorMessage ->
                println("$IAP_LOG_TAG initiatePurchase callback, productId=$productId, transactionId=$transactionId, errorMessage=$errorMessage")
                if (errorMessage.isEmpty()) {
                    println("$IAP_LOG_TAG initiatePurchase success, transactionId=$transactionId")
                    continuation.resume(transactionId)
                } else {
                    println("$IAP_LOG_TAG initiatePurchase failed, productId=$productId, errorMessage=$errorMessage")
                    continuation.resume(null)
                }
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
        val productIds = listOf(
            "com.cephalon.lucyApp.9.9",
            "com.cephalon.lucyApp.39.9",
            "com.cephalon.lucyApp.69.9",
            "com.cephalon.lucyApp.99",
            "com.cephalon.lucyApp.299",
            "com.cephalon.lucyApp.499",
            "com.cephalon.lucyApp.699",
            "com.cephalon.lucyApp.899",
            "com.cephalon.lucyApp.999"
        )
        println("$IAP_LOG_TAG loadProducts start, productIds=$productIds")
        val loadedProducts = suspendCancellableCoroutine<List<IAPProduct>> { continuation ->
            bridge.loadProducts(productIds) { productsMap ->
                println("$IAP_LOG_TAG loadProducts callback, rawProductsMap=$productsMap")
                val mappedProducts = productsMap.map { entry ->
                    val productInfo = entry.value
                    IAPProduct(
                        productId = entry.key,
                        title = productInfo["displayName"] ?: "",
                        description = productInfo["description"] ?: "",
                        price = productInfo["displayPrice"] ?: "",
                        currencyCode = productInfo["currencyCode"] ?: "USD"
                    )
                }
                println("$IAP_LOG_TAG loadProducts mappedProducts=$mappedProducts")
                continuation.resume(mappedProducts)
            }
        }
        _products.value = loadedProducts
        println("$IAP_LOG_TAG loadProducts completed, productsCount=${loadedProducts.size}")
    }

    override fun handleUnfinishedTransactions() {
        println("$IAP_LOG_TAG handleUnfinishedTransactions start")
        bridge.getUnfinishedTransactions { transactionIds ->
            println("$IAP_LOG_TAG handleUnfinishedTransactions callback, transactionIds=$transactionIds")
        }
    }
}
