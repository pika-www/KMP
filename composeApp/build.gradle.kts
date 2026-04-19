import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

    alias(libs.plugins.kotlin.serialization)
}

kotlin {

    // ✅ 继续使用（虽然 deprecated，但当前最稳）
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
                }
            }
        }
    }

    // ✅ iOS
    val blobRoot = rootProject.projectDir.resolve("sdk/lucy-im-sdk-kotlin/lucy-blob").absolutePath
    val blobIosArm64StaticLib = "$blobRoot/target/aarch64-apple-ios/release/liblucy_blob_core.a"
    val blobIosSimulatorArm64StaticLib = "$blobRoot/target/aarch64-apple-ios-sim/release/liblucy_blob_core.a"

    val iosTargets = listOf(
        iosArm64(),
        iosSimulatorArm64()
    )

    iosArm64 {
        binaries.all {
            linkerOpts(blobIosArm64StaticLib)
            linkerOpts("-framework", "Network")
            linkerOpts("-framework", "CoreBluetooth")
            linkerOpts("-framework", "Security")
            linkerOpts("-framework", "CoreFoundation")
            linkerOpts("-framework", "SystemConfiguration")
        }
        binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    iosSimulatorArm64 {
        binaries.all {
            linkerOpts(blobIosSimulatorArm64StaticLib)
            linkerOpts("-framework", "Network")
            linkerOpts("-framework", "CoreBluetooth")
            linkerOpts("-framework", "Security")
            linkerOpts("-framework", "CoreFoundation")
            linkerOpts("-framework", "SystemConfiguration")
        }
        binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {

        val commonMain by getting {
            dependencies {
                implementation(project(":lucy-im-sdk-kotlin"))
                // --- 2. Koin (保持不变) ---
                implementation("io.insert-koin:koin-core:3.5.6")
                implementation("io.insert-koin:koin-compose:1.1.5")

                implementation("com.arkivanov.decompose:decompose:3.5.0")
                implementation("com.arkivanov.decompose:extensions-compose:3.5.0")

                // --- 3. Serialization (保持 1.7.3) ---
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

                // --- 4. Ktor (建议同步升级到 2.3.12 以获得更好的 KMP 支持) ---
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.websockets)


                // icon 组件
                implementation(compose.materialIconsExtended)

                // --- 5. 原有：其他依赖 ---
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime") {
                    version {
                        strictly("0.6.1")
                    }
                }
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
                implementation(libs.ktor.client.okhttp)
                implementation(libs.androidx.activity.compose)
                implementation(libs.compose.uiToolingPreview)



                implementation("androidx.camera:camera-core:1.3.3")
                implementation("androidx.camera:camera-camera2:1.3.3")
                implementation("androidx.camera:camera-lifecycle:1.3.3")
                implementation("androidx.camera:camera-view:1.3.3")
                implementation("com.google.mlkit:barcode-scanning:17.2.0")
                implementation("com.alphacephei:vosk-android:0.3.47")
            }
        }

        // ✅ iOS 共享层
        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
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
    namespace = "com.cephalon.lucyApp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.cephalon.lucyApp"
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
        getByName("debug") {
            buildConfigField("String", "APP_ENV", "\"test\"")
        }
        create("staging") {
            initWith(getByName("debug"))
            buildConfigField("String", "APP_ENV", "\"test\"")
        }
        getByName("release") {
            isMinifyEnabled = false
            buildConfigField("String", "APP_ENV", "\"release\"")
        }
    }

    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "脑花-${buildType.name}.apk"
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
