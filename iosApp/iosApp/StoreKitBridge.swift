import Foundation
import StoreKit
import SwiftUI
import ComposeApp

private let iapLogTag = "[IAP][StoreKitBridge]"

// 暴露给 Kotlin 的桥接协议
@objc protocol StoreKitBridgeProtocol {
    func loadProducts(_ productIds: [String], completion: @escaping ([String: [String: String]]) -> Void)
    func purchase(_ productId: String, completion: @escaping (String, String) -> Void)
    func finishTransaction(_ transactionId: String, completion: @escaping (Bool) -> Void)
    func getUnfinishedTransactions(_ completion: @escaping ([String]) -> Void)
}

// 桥接实现类
@objc class StoreKitBridge: NSObject, StoreKitBridgeProtocol {
    static let shared = StoreKitBridge()
    
    private override init() {}

    func registerKotlinBridgeHandlers() {
        print("\(iapLogTag) registerKotlinBridgeHandlers start")

        AppleStoreKitBridgeRegistry.shared.registerLoadProductsHandler { productIds, completion in
            let ids = productIds as? [String] ?? []
            print("\(iapLogTag) Kotlin -> Swift loadProducts, productIds=\(ids)")
            StoreKitBridge.shared.loadProducts(ids) { result in
                print("\(iapLogTag) Swift -> Kotlin loadProducts completion, result=\(result)")
                completion(result)
            }
        }

        AppleStoreKitBridgeRegistry.shared.registerPurchaseHandler { productId, completion in
            print("\(iapLogTag) Kotlin -> Swift purchase, productId=\(productId)")
            StoreKitBridge.shared.purchase(productId) { transactionId, errorMessage in
                print("\(iapLogTag) Swift -> Kotlin purchase completion, productId=\(productId), transactionId=\(transactionId), errorMessage=\(errorMessage)")
                completion(transactionId, errorMessage)
            }
        }

        AppleStoreKitBridgeRegistry.shared.registerFinishTransactionHandler { transactionId, completion in
            print("\(iapLogTag) Kotlin -> Swift finishTransaction, transactionId=\(transactionId)")
            StoreKitBridge.shared.finishTransaction(transactionId) { success in
                print("\(iapLogTag) Swift -> Kotlin finishTransaction completion, transactionId=\(transactionId), success=\(success)")
                completion(KotlinBoolean(value: success))
            }
        }

        AppleStoreKitBridgeRegistry.shared.registerGetUnfinishedTransactionsHandler { completion in
            print("\(iapLogTag) Kotlin -> Swift getUnfinishedTransactions")
            StoreKitBridge.shared.getUnfinishedTransactions { transactionIds in
                print("\(iapLogTag) Swift -> Kotlin getUnfinishedTransactions completion, transactionIds=\(transactionIds)")
                completion(transactionIds)
            }
        }

        print("\(iapLogTag) registerKotlinBridgeHandlers completed")
    }
    
    func loadProducts(_ productIds: [String], completion: @escaping ([String: [String: String]]) -> Void) {
        print("\(iapLogTag) loadProducts start, productIds=\(productIds)")
        Task {
            do {
                let products = try await AppleStoreKit2Manager.shared.loadProducts(productIds: productIds)
                print("\(iapLogTag) loadProducts fetched products count=\(products.count), products=\(products)")
                var result: [String: [String: String]] = [:]
                for product in products {
                    result[product.id] = [
                        "displayName": product.displayName,
                        "description": product.description,
                        "displayPrice": product.displayPrice,
                        "currencyCode": product.currencyCode
                    ]
                }
                print("\(iapLogTag) loadProducts completion result=\(result)")
                completion(result)
            } catch {
                print("\(iapLogTag) loadProducts failed, error=\(error.localizedDescription)")
                completion([:])
            }
        }
    }
    
    func purchase(_ productId: String, completion: @escaping (String, String) -> Void) {
        print("\(iapLogTag) purchase start, productId=\(productId)")
        Task {
            let result = await AppleStoreKit2Manager.shared.purchase(productId: productId)
            print("\(iapLogTag) purchase result, productId=\(productId), result=\(result)")
            switch result {
            case .success(let transactionId, let productId):
                print("\(iapLogTag) purchase success, transactionId=\(transactionId), productId=\(productId)")
                completion(transactionId, "")
            case .pending:
                print("\(iapLogTag) purchase pending, productId=\(productId)")
                completion("", "Purchase pending")
            case .cancelled:
                print("\(iapLogTag) purchase cancelled, productId=\(productId)")
                completion("", "Purchase cancelled")
            case .failure(let message):
                print("\(iapLogTag) purchase failed, productId=\(productId), message=\(message)")
                completion("", message)
            }
        }
    }
    
    func finishTransaction(_ transactionId: String, completion: @escaping (Bool) -> Void) {
        print("\(iapLogTag) finishTransaction start, transactionId=\(transactionId)")
        Task {
            do {
                try await AppleStoreKit2Manager.shared.finish(transactionId: transactionId)
                print("\(iapLogTag) finishTransaction success, transactionId=\(transactionId)")
                completion(true)
            } catch {
                print("\(iapLogTag) finishTransaction failed, transactionId=\(transactionId), error=\(error.localizedDescription)")
                completion(false)
            }
        }
    }
    
    func getUnfinishedTransactions(_ completion: @escaping ([String]) -> Void) {
        print("\(iapLogTag) getUnfinishedTransactions start")
        Task {
            await AppleStoreKit2Manager.shared.refreshUnfinishedTransactions()
            let ids = await AppleStoreKit2Manager.shared.unfinishedTransactionIds
            print("\(iapLogTag) getUnfinishedTransactions completion, transactionIds=\(ids)")
            completion(ids)
        }
    }
}
