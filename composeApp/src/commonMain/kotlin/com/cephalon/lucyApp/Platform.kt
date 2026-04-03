package com.cephalon.lucyApp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform