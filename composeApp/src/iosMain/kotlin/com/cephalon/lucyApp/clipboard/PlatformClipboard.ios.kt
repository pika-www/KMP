package com.cephalon.lucyApp.clipboard

import platform.UIKit.UIPasteboard

actual fun platformCopyToClipboard(text: String) {
    // 直接写 plain-text，iOS 的 BasicTextField / 其它系统原生文本控件
    // 在长按"粘贴"时都会读取 UIPasteboard.general.string，这样就能稳定取到。
    UIPasteboard.generalPasteboard.string = text
}
