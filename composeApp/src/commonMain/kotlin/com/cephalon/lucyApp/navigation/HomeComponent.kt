package com.cephalon.lucyApp.navigation

interface HomeComponent {
    val showBack: Boolean
    fun onBack()
    fun onLogout()
    fun onOpenSdkTest()
    fun onOpenWsTest()
    fun onOpenBrainBoxGuide()
    fun onOpenBrainBoxLoginSuccess()
    fun onOpenAgentModel()
    fun onOpenScanBindChannel()
    fun onOpenNas()
}
