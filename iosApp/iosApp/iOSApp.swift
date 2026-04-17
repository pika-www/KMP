import SwiftUI
import ComposeApp // 确保导入了编译后的共享库

@main
struct iOSApp: App {
    @StateObject private var storeKitManager = AppleStoreKit2Manager.shared

    init() {
        // 设置 blob 存储目录（必须在任何 BlobTransfer 实例化之前）
        let caches = NSSearchPathForDirectoriesInDomains(.cachesDirectory, .userDomainMask, true).first ?? NSTemporaryDirectory()
        let blobDir = (caches as NSString).appendingPathComponent("lucy_blob")
        setenv("lucy.blob.store_dir", blobDir, 1)

        // 在应用启动时初始化 Koin
        KoinKt.doInitKoin()
        StoreKitBridge.shared.registerKotlinBridgeHandlers()
        AppleStoreKit2Manager.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(storeKitManager)
        }
    }
}
