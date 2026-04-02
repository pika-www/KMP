import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

    kotlin("plugin.serialization") version "1.9.22"
}

kotlin {

    // ✅ 继续使用（虽然 deprecated，但当前最稳）
    androidTarget()

    // ✅ iOS
    val iosTargets = listOf(
        iosArm64(),
        iosSimulatorArm64()
    )

    iosTargets.forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {

        val commonMain by getting {
            dependencies {
                // --- 2. Koin (保持不变) ---
                implementation("io.insert-koin:koin-core:3.5.6")
                implementation("io.insert-koin:koin-compose:1.1.5")

                implementation("com.arkivanov.decompose:decompose:3.5.0")
                implementation("com.arkivanov.decompose:extensions-compose:3.5.0")

                // --- 3. Serialization (保持 1.7.3) ---
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

                // --- 4. Ktor (建议同步升级到 2.3.12 以获得更好的 KMP 支持) ---
                implementation("io.ktor:ktor-client-core:2.3.12")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
                implementation("io.ktor:ktor-client-logging:2.3.12")
                implementation("io.ktor:ktor-client-websockets:2.3.12")


                // icon 组件
                implementation(compose.materialIconsExtended)

                // --- 5. 原有：其他依赖 ---
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("com.russhwolf:multiplatform-settings:1.1.1")
                implementation("com.russhwolf:multiplatform-settings-no-arg:1.1.1")
                implementation("com.russhwolf:multiplatform-settings-serialization:1.1.1")

                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)

                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:2.3.12")
                implementation(libs.androidx.activity.compose)
                implementation(libs.compose.uiToolingPreview)
            }
        }

        // ✅ iOS 共享层
        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.12")
            }
        }

        iosTargets.forEach {
            it.compilations.getByName("main").defaultSourceSet.dependsOn(iosMain)
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "com.example.androidios"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.androidios"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    // ✅ 正确 JVM 设置（替代你报错那段）
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}
