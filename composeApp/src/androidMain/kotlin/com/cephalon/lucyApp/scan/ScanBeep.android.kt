package com.cephalon.lucyApp.scan

import android.media.AudioManager
import android.media.ToneGenerator

actual fun playScanBeep() {
    ToneGenerator(AudioManager.STREAM_MUSIC, 80).startTone(ToneGenerator.TONE_PROP_BEEP, 150)
}
