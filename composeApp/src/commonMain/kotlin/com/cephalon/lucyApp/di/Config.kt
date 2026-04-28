package com.cephalon.lucyApp.di

import com.cephalon.lucyApp.AppEnvironment
import com.cephalon.lucyApp.appEnvironment
import com.cephalon.lucyApp.network.NetworkConfig
import kotlin.concurrent.Volatile

object AppConfig {
    val env: AppEnvironment get() = appEnvironment

    val baseDomain: String get() = when (env) {
        AppEnvironment.DEBUG   -> "https://test.unicorn.org.cn"
        AppEnvironment.TEST    -> "https://test.unicorn.org.cn"
        AppEnvironment.RELEASE -> "https://prod.unicorn.org.cn"
    }

    const val TIMEOUT_MILLIS = 20000L

    val networkConfig get() = NetworkConfig(
        baseUrl = baseDomain,
        timeoutMillis = TIMEOUT_MILLIS
    )

    /**
     * Lucy IM SDK：为 true 时，JetStream 发布失败会记录一段简短的 UTF-8 负载预览。
     * 默认开启便于排查；设为 `false`（例如在 release）可减少日志中的敏感数据。
     * 失败时通过 [lucy.im.sdk.LucyImAppConfig.jetStreamVerboseLoggingProvider] 读取。
     */
    @Volatile
    var lucyImJetStreamVerboseLogging: Boolean = true

    /**
     * [lucyImJetStreamVerboseLogging] 为 true 时，负载预览最多占用的 UTF-8 字符数。
     * 在 [com.cephalon.lucyApp.sdk.SdkSessionManager] 构建 [lucy.im.sdk.LucyImAppConfig] 时生效；若需不同上限请在首次连接前修改。
     */
    var lucyImJetStreamPayloadPreviewMaxChars: Int = 120
        set(value) {
            field = value.coerceIn(16, 512)
        }
}
