import SwiftUI
import ComposeApp // 确保导入了编译后的共享库

@main
struct iOSApp: App {
    @StateObject private var storeKitManager = AppleStoreKit2Manager.shared

    init() {
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
