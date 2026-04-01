package com.example.androidios

import android.app.Application
import com.example.androidios.di.initKoin

class KoinApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}