package com.cephalon.lucyApp.clipboard

/**
 * 把纯文本写入平台原生剪贴板。
 *
 * 为什么不直接用 Compose 的 LocalClipboardManager：
 * - 在 Compose Multiplatform 某些版本的 iOS 实现里，`LocalClipboardManager.setText`
 *   对 `UIPasteboard` 的写入存在时机/内容格式问题，导致原生 `BasicTextField`
 *   长按菜单"粘贴"读不到内容。
 * - 通过 expect/actual 直接操作 Android ClipboardManager / iOS UIPasteboard，
 *   可以确保写入的是 plain-text 并立即对系统可见。
 */
expect fun platformCopyToClipboard(text: String)
