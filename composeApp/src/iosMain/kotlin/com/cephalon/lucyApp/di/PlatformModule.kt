package com.cephalon.lucyApp.di

import com.cephalon.lucyApp.payment.AppleIAPManager
import com.cephalon.lucyApp.payment.IAPManager
import com.cephalon.lucyApp.payment.PlatformIAP
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { AppleIAPManager() }
    single<PlatformIAP> { get<AppleIAPManager>() }
    single<IAPManager> { get<AppleIAPManager>() }
}
