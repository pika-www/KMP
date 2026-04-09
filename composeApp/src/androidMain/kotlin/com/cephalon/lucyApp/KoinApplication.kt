package com.cephalon.lucyApp

import android.app.Application
import com.cephalon.lucyApp.di.initKoin
import com.cephalon.lucyApp.screens.agentmodel.AndroidAppContextHolder

class KoinApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppContextHolder.appContext = applicationContext
        initKoin()
    }
}