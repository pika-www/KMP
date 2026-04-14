package com.cephalon.lucyApp.deviceaccess

import androidx.compose.runtime.Composable
import com.cephalon.lucyApp.brainbox.rememberBrainBoxProvisionController

@Composable
actual fun rememberBleManager(): BleManager {
    val controller = rememberBrainBoxProvisionController()
    return rememberDelegatingBleManager(controller)
}
