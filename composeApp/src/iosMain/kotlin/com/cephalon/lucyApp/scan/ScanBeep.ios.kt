package com.cephalon.lucyApp.scan

import platform.AudioToolbox.AudioServicesPlaySystemSound

actual fun playScanBeep() {
    AudioServicesPlaySystemSound(1108u)
}
