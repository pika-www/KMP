package com.cephalon.lucyApp.payment

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface IAPManager {
    val products: StateFlow<List<IAPProduct>>
    suspend fun createOrder(amount: Long): Pair<String, String>?
    suspend fun initiatePurchase(productId: String): String?
    suspend fun verifyTransaction(transactionId: String): Boolean
    suspend fun finishTransaction(transactionId: String)
    suspend fun loadProducts()
    fun handleUnfinishedTransactions()
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
