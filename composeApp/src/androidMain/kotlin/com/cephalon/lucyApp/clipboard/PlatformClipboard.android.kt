package com.cephalon.lucyApp.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.cephalon.lucyApp.screens.agentmodel.AndroidAppContextHolder

actual fun platformCopyToClipboard(text: String) {
    val context = AndroidAppContextHolder.appContext
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    manager.setPrimaryClip(ClipData.newPlainText("text", text))
}
