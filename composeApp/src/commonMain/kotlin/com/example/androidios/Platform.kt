package com.example.androidios

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform