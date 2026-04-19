import Foundation
import CoreLocation
import NetworkExtension

/// 当前 Wi‑Fi SSID 的单例读取器。职责两件：
///   1. 应用启动时提前触发一次定位授权弹窗（`ensureLocationAuthorization`）。
///      没有定位权限，即使 Access WiFi Information entitlement 都开着，
///      `NEHotspotNetwork.fetchCurrent` 也会回调 nil。
///   2. 按需读取 SSID（`fetchCurrentSSID`）。已有授权 → 直接读；授权未决定 →
///      排队等弹窗回调，一次性带着授权结果走。
///
/// iOS 13+ 读 SSID 的五项硬性前置（任一不满足都是空）：
///   (a) Info.plist 有 `NSLocationWhenInUseUsageDescription`
///   (b) Signing & Capabilities 有 "Access WiFi Information"
///   (c) 用户在运行时同意了定位（WhenInUse 或 Always）
///   (d) 设备此刻确实连在某个 Wi‑Fi 上（不是蜂窝 / 飞行模式）
///   (e) App 处于前台
///
/// 本类 **不** 会自己跳去系统设置；调用方（Compose UI / Kotlin）在拿到空 SSID
/// 后自行决定是否 toast 提示 "请手动输入 Wi‑Fi 名称"。
@objc public final class WiFiInfoProvider: NSObject {

    // MARK: - Public singleton

    @objc public static let shared = WiFiInfoProvider()

    // MARK: - Private state

    private let locationManager = CLLocationManager()

    /// 仍在等待"定位授权弹窗"回调的 SSID 查询请求。授权回调到达时一次性全部冲掉。
    private var pendingSsidHandlers: [(String) -> Void] = []

    // MARK: - Init

    private override init() {
        super.init()
        locationManager.delegate = self
    }

    // MARK: - Public API

    /// App 启动时调用一次（放在 `iOSApp.init()` 里最合适）。
    /// 状态还是 `.notDetermined` 时会弹权限框；其它状态什么都不做，避免重复骚扰用户。
    @objc public func ensureLocationAuthorization() {
        let status = currentAuthorizationStatus()
        guard status == .notDetermined else {
            NSLog("[WiFiInfoProvider] location status 已确定 = %d，不再弹窗", status.rawValue)
            return
        }
        NSLog("[WiFiInfoProvider] location status notDetermined，请求 WhenInUse")
        locationManager.requestWhenInUseAuthorization()
    }

    /// 读当前 Wi‑Fi SSID。读不到时回调空串，由调用方降级到"让用户手动输入"。
    ///
    /// - Parameter completion: 统一在主线程回调，便于 UI 直接使用。
    @objc public func fetchCurrentSSID(completion: @escaping (String) -> Void) {
        let status = currentAuthorizationStatus()

        switch status {
        case .notDetermined:
            // 权限还没决定：先排队，等用户做完选择再统一走 NEHotspotNetwork。
            NSLog("[WiFiInfoProvider] location notDetermined，入队等待授权回调")
            pendingSsidHandlers.append(completion)
            locationManager.requestWhenInUseAuthorization()

        case .authorizedWhenInUse, .authorizedAlways:
            readCurrentSSID(completion: completion)

        case .denied, .restricted:
            NSLog("[WiFiInfoProvider] location=%d（denied/restricted），无法读取 SSID，返回空串", status.rawValue)
            mainAsync { completion("") }

        @unknown default:
            NSLog("[WiFiInfoProvider] 未知 location 状态 = %d，返回空串", status.rawValue)
            mainAsync { completion("") }
        }
    }

    // MARK: - Private helpers

    private func currentAuthorizationStatus() -> CLAuthorizationStatus {
        if #available(iOS 14.0, *) {
            return locationManager.authorizationStatus
        } else {
            return CLLocationManager.authorizationStatus()
        }
    }

    private func readCurrentSSID(completion: @escaping (String) -> Void) {
        NEHotspotNetwork.fetchCurrent { network in
            let ssid = network?.ssid ?? ""
            if ssid.isEmpty {
                NSLog("[WiFiInfoProvider] NEHotspotNetwork.fetchCurrent 返回 nil（可能缺 entitlement / 没连 Wi‑Fi / 不在前台）")
            } else {
                NSLog("[WiFiInfoProvider] fetchCurrent ssid=%@", ssid)
            }
            self.mainAsync { completion(ssid) }
        }
    }

    private func mainAsync(_ block: @escaping () -> Void) {
        if Thread.isMainThread {
            block()
        } else {
            DispatchQueue.main.async(execute: block)
        }
    }

    private func drainPendingHandlers(with status: CLAuthorizationStatus) {
        let handlers = pendingSsidHandlers
        pendingSsidHandlers.removeAll()
        guard !handlers.isEmpty else { return }

        if status == .authorizedWhenInUse || status == .authorizedAlways {
            NSLog("[WiFiInfoProvider] 授权已通过，冲掉 %d 个 pending SSID 请求", handlers.count)
            handlers.forEach { handler in
                readCurrentSSID(completion: handler)
            }
        } else {
            NSLog("[WiFiInfoProvider] 授权未通过 (status=%d)，%d 个 pending 请求返回空串", status.rawValue, handlers.count)
            handlers.forEach { handler in
                mainAsync { handler("") }
            }
        }
    }
}

// MARK: - CLLocationManagerDelegate

extension WiFiInfoProvider: CLLocationManagerDelegate {

    /// iOS 14+ 推荐的回调。
    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        NSLog("[WiFiInfoProvider] locationManagerDidChangeAuthorization status=%d", status.rawValue)
        drainPendingHandlers(with: status)
    }

    /// iOS 13 及更早的回调（iOS 14 起已 deprecated，但仍会被调用；保留双轨兼容）。
    public func locationManager(_ manager: CLLocationManager,
                                didChangeAuthorization status: CLAuthorizationStatus) {
        NSLog("[WiFiInfoProvider] didChangeAuthorization status=%d", status.rawValue)
        drainPendingHandlers(with: status)
    }
}
