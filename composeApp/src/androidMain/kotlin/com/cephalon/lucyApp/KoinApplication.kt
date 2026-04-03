package com.cephalon.lucyApp

import android.app.Application
import com.cephalon.lucyApp.di.initKoin

class KoinApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}