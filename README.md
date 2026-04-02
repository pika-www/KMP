# KMP 跨端登录示例（Android + iOS）

这是一个基于 Kotlin Multiplatform（KMP）的跨端项目，目标是：

- 复用 Android / iOS 的业务逻辑与网络层；
- 使用 Compose Multiplatform 复用主要 UI；
- 在 iOS 侧通过 SwiftUI 容器承载 Kotlin Compose 页面；
- 使用 Koin 管理依赖、Ktor 进行网络请求、kotlinx.serialization 处理 JSON。

当前业务示例是「登录 -> 主页 -> 退出登录」的完整闭环。

---

## 1. 系统架构总览

项目采用「共享业务层 + 平台入口层」的 KMP 结构：

- **共享层（`composeApp/src/commonMain`）**
  - 共享 UI：`App`、`LoginScreen`、`HomeScreen`
  - 共享业务：`AuthRepository`
  - 共享接口：`AuthApi`
  - 共享网络：`createHttpClient()`（Ktor + 拦截/超时/日志）
  - 共享模型：`LoginRequest`、`BaseResponse`、`LoginData`
  - 共享 DI：`initKoin()`、`appModule`

- **Android 平台层（`composeApp/src/androidMain`）**
  - `MainActivity` 作为 Android 入口，直接渲染共享 `App()`
  - `KoinApplication` 在应用启动时初始化 Koin
  - `AndroidManifest.xml` 配置 `Application`、`Activity` 与网络权限

- **iOS 平台层（`composeApp/src/iosMain` + `iosApp`）**
  - Kotlin 侧提供 `MainViewController()` 返回 `ComposeUIViewController`
  - SwiftUI 侧 `ContentView` 使用 `UIViewControllerRepresentable` 嵌入 Compose
  - `iOSApp.swift` 在启动时调用 `KoinKt.doInitKoin()` 初始化 DI

---

## 2. 分层与调用链

登录流程（简化）：

1. `LoginScreen` 收集用户名/密码并发起登录
2. 通过 Koin 注入 `AuthRepository`
3. `AuthRepository.login()` 调用 `AuthApi.login()`
4. `AuthApi` 负责拼接登录模块路径 `/cephalon/user-center/v1/login`
5. `HttpClient` 自动附加基础域名、Header、超时、日志与响应校验
6. 返回 `BaseResponse<LoginData>` 并在 UI 层处理成功/失败状态
7. 登录成功后切换到 `HomeScreen`，可执行退出登录

该结构的优点：

- UI 状态、业务逻辑、网络层都在 `commonMain`，跨端行为一致；
- 平台代码仅保留「启动、容器、生命周期接入」职责；
- 后续新增接口时，只需在共享层增加 repository/model 即可双端复用。

---

## 3. 项目目录说明

```text
KMP/
├── composeApp/                      # KMP 主模块（共享 + Android + iOS Kotlin 代码）
│   ├── build.gradle.kts             # 多平台 target、依赖、Android 配置
│   └── src/
│       ├── commonMain/kotlin/...    # 共享 UI、DI、网络、数据模型、业务逻辑
│       ├── androidMain/kotlin/...   # Android 入口（MainActivity / Application）
│       └── iosMain/kotlin/...       # iOS 入口（MainViewController）
├── iosApp/                          # iOS 原生工程（SwiftUI 容器）
│   ├── iosApp.xcodeproj
│   └── iosApp/*.swift
├── gradle/libs.versions.toml        # 版本与插件管理
├── settings.gradle.kts              # 模块注册（当前包含 :composeApp）
└── README.md
```

---

## 4. 技术栈

- Kotlin Multiplatform
- Compose Multiplatform（共享 UI）
- Ktor Client（`core` / `content-negotiation` / `logging` / `okhttp` / `darwin`）
- kotlinx.serialization（JSON）
- Koin（依赖注入）
- Kotlin Coroutines
- Android Gradle Plugin + Gradle Wrapper

---

## 5. 开发环境要求

建议版本（与当前工程配置兼容）：

- **JDK**：17 或更高（推荐 17）
- **Android Studio**：较新稳定版（支持 Kotlin Multiplatform / Compose）
- **Xcode**：支持当前 iOS SDK 的稳定版
- **CocoaPods**：可选（本项目当前通过 framework 集成，默认可不依赖 Pods）

首次拉取后建议执行：

```bash
./gradlew --version
./gradlew tasks --all
```

---

## 6. 如何启动项目

### 6.1 启动 Android

#### 方式 A：Android Studio（推荐）

1. 用 Android Studio 打开项目根目录 `KMP/`
2. 等待 Gradle 同步完成
3. 选择 Android 设备/模拟器
4. 运行 `composeApp` 的 Android App 配置

#### 方式 B：命令行

```bash
# 构建 Debug APK
./gradlew :composeApp:assembleDebug

# 安装到已连接设备（需要先启动模拟器或连接真机）
./gradlew :composeApp:installDebug
```

> 如果需要卸载重装，可先在设备上卸载包名 `com.example.androidios`。

---

### 6.2 启动 iOS

#### 方式 A：Xcode（推荐）

1. 打开 `iosApp/iosApp.xcodeproj`
2. 选择模拟器（如 iPhone 15）
3. 直接点击 Run
4. Xcode 会通过 Gradle 任务 `embedAndSignAppleFrameworkForXcode` 嵌入 Kotlin framework

#### 方式 B：先命令行编译 Kotlin iOS Framework，再用 Xcode 跑

```bash
# 编译 iOS 模拟器 Debug Framework
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# 编译 iOS 真机 Debug Framework
./gradlew :composeApp:linkDebugFrameworkIosArm64
```

---

## 7. 常用编译与检查命令

```bash
# 全量构建（含测试）
./gradlew :composeApp:build

# 清理
./gradlew :composeApp:clean

# 只编译 Android Debug
./gradlew :composeApp:compileDebugSources

# 只编译 iOS（模拟器）
./gradlew :composeApp:compileKotlinIosSimulatorArm64

# 只编译 iOS（真机）
./gradlew :composeApp:compileKotlinIosArm64

# 运行单元测试（commonTest）
./gradlew :composeApp:allTests
```

如果想查看所有可用任务：

```bash
./gradlew tasks --all
```

---

## 8. 接口环境与配置

接口基础配置在：

- `composeApp/src/commonMain/kotlin/com/example/androidios/di/Config.kt`

可调整项：

- `IS_PROD`：切换测试/生产环境
- `baseDomain`：接口基础域名
- `TIMEOUT_MILLIS`：请求超时时间

说明：

- `AppConfig` 只维护环境和基础域名
- `/cephalon/user-center/v1/` 已收敛到 `AuthApi`，作为登录模块自己的路径前缀

网络客户端配置在：

- `composeApp/src/commonMain/kotlin/com/example/androidios/di/HttpClient.kt`

已包含：

- 默认请求头（`Content-Type`、`Lang`）
- 超时策略
- JSON 序列化配置
- 请求/响应日志
- 响应状态校验与异常处理

---

## 9. 常见问题

- **iOS 运行时报 framework 相关错误**
  - 先执行 `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`
  - 再在 Xcode 中 Clean Build Folder 后重新运行

- **Android 无法访问网络**
  - 确认 `AndroidManifest.xml` 包含 `INTERNET` 权限
  - 检查 `BASE_URL` 是否可达、证书是否有效

- **Gradle 版本/插件警告较多**
  - 当前工程可正常构建；后续可逐步做 KMP + AGP 结构升级（拆分 Android app 模块）以消除兼容性警告

---

## 10. 后续可扩展建议

- 引入 ViewModel（`commonMain`）统一状态管理（MVI/MVVM）
- 登录态持久化（如 `multiplatform-settings` 保存 token）
- 请求鉴权与自动刷新 token
- 更完整的导航栈（替代本地 `isLoggedIn` 状态切页）
- 增加 UI 测试与接口模拟测试
