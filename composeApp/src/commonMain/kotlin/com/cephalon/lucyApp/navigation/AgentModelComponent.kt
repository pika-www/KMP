package com.cephalon.lucyApp.navigation

interface AgentModelComponent {
    val targetCdi: String?
    fun onBack()
    fun onNavigateToNas()
    fun onNavigateToHome()
    fun onLogout()
}
