package com.cephalon.lucyApp.payment

open class PlatformIAP {
    open suspend fun initialize() {
    }

    open suspend fun getProducts(productIds: List<String>): List<IAPProduct> {
        return emptyList()
    }

    open suspend fun purchaseProduct(productId: String, orderId: String): IAPResult {
        return IAPResult.Failure("Not implemented")
    }

    open suspend fun finishTransaction(transactionId: String) {
    }

    open suspend fun checkUnfinishedTransactions(): List<String> {
        return emptyList()
    }
}
