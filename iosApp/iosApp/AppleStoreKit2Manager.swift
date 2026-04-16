import Foundation
import StoreKit
import SwiftUI

struct AppleStoreProduct: Identifiable, Equatable {
    let id: String
    let displayName: String
    let description: String
    let displayPrice: String
    let currencyCode: String
}

enum ApplePurchaseResult: Equatable {
    case success(transactionId: String, productId: String)
    case pending
    case cancelled
    case failure(message: String)
}

@MainActor
final class AppleStoreKit2Manager: ObservableObject {
    static let shared = AppleStoreKit2Manager()

    @Published private(set) var products: [AppleStoreProduct] = []
    @Published private(set) var unfinishedTransactionIds: [String] = []

    private var storeProducts: [String: Product] = [:]
    private var updatesTask: Task<Void, Never>?

    private init() {}

    func start() {
        guard updatesTask == nil else { return }
        updatesTask = Task {
            await observeTransactionUpdates()
        }
        Task {
            await refreshUnfinishedTransactions()
        }
    }

    deinit {
        updatesTask?.cancel()
    }

    func loadProducts(productIds: [String]) async throws -> [AppleStoreProduct] {
        let fetchedProducts = try await Product.products(for: productIds)
        storeProducts = Dictionary(uniqueKeysWithValues: fetchedProducts.map { ($0.id, $0) })
        let mapped = fetchedProducts.map {
            AppleStoreProduct(
                id: $0.id,
                displayName: $0.displayName,
                description: $0.description,
                displayPrice: $0.displayPrice,
                currencyCode: $0.priceFormatStyle.currencyCode
            )
        }
        products = mapped.sorted { $0.id < $1.id }
        return products
    }

    func purchase(productId: String, appAccountToken: UUID? = nil) async -> ApplePurchaseResult {
        guard let product = storeProducts[productId] else {
            return .failure(message: "Product not loaded: \(productId)")
        }

        do {
            let result: Product.PurchaseResult
            if let appAccountToken {
                result = try await product.purchase(options: [.appAccountToken(appAccountToken)])
            } else {
                result = try await product.purchase()
            }

            switch result {
            case .success(let verificationResult):
                switch verificationResult {
                case .verified(let transaction):
                    return .success(
                        transactionId: String(transaction.id),
                        productId: transaction.productID
                    )
                case .unverified(_, let error):
                    return .failure(message: error.localizedDescription)
                }
            case .pending:
                return .pending
            case .userCancelled:
                return .cancelled
            @unknown default:
                return .failure(message: "Unknown purchase result")
            }
        } catch {
            return .failure(message: error.localizedDescription)
        }
    }

    func finish(transactionId: String) async throws {
        let targetId = UInt64(transactionId)
        for await result in Transaction.unfinished {
            switch result {
            case .verified(let transaction) where transaction.id == targetId:
                await transaction.finish()
                await refreshUnfinishedTransactions()
                return
            case .unverified(let transaction, _) where transaction.id == targetId:
                await transaction.finish()
                await refreshUnfinishedTransactions()
                return
            default:
                continue
            }
        }
    }

    func refreshUnfinishedTransactions() async {
        var ids: [String] = []
        for await result in Transaction.unfinished {
            switch result {
            case .verified(let transaction):
                ids.append(String(transaction.id))
            case .unverified(let transaction, _):
                ids.append(String(transaction.id))
            }
        }
        unfinishedTransactionIds = ids
    }

    private func observeTransactionUpdates() async {
        for await result in Transaction.updates {
            switch result {
            case .verified:
                await refreshUnfinishedTransactions()
            case .unverified:
                await refreshUnfinishedTransactions()
            }
        }
    }
}
