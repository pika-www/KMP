package com.cephalon.lucyApp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

enum class AppEnvironment {
    DEBUG, TEST, RELEASE;

    val isDebug get() = this == DEBUG
    val isTest get() = this == TEST
    val isRelease get() = this == RELEASE
    val isProd get() = this == RELEASE
}

expect val appEnvironment: AppEnvironment