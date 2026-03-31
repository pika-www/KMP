import SwiftUI
import composeApp // 确保导入了编译后的共享库

@main
struct iOSApp: App {
    init() {
        // 在应用启动时初始化 Koin
        KoinKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
