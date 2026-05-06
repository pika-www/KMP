package com.cephalon.lucyApp.navigation

interface ScanBindChannelComponent {
    fun onBack()
    fun onScanSuccess(cdi: String, onLoading: (Boolean) -> Unit)
    fun onOpenGuide()
}
