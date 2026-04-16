package com.cephalon.lucyApp.di

import com.cephalon.lucyApp.payment.AndroidIAPManager
import com.cephalon.lucyApp.payment.IAPManager
import com.cephalon.lucyApp.payment.PlatformIAP
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { AndroidIAPManager() }
    single<PlatformIAP> { get<AndroidIAPManager>() }
    single<IAPManager> { get<AndroidIAPManager>() }
}
