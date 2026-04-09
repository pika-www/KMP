package com.cephalon.lucyApp.screens.agentmodel

import android.annotation.SuppressLint
import android.content.Context
import java.io.File

@SuppressLint("StaticFieldLeak")
internal object AndroidAppContextHolder {
    lateinit var appContext: Context
}

actual fun clearAppCache() {
    try {
        val ctx = AndroidAppContextHolder.appContext
        ctx.cacheDir.deleteRecursively()
        ctx.externalCacheDir?.deleteRecursively()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

actual fun getAppCacheSize(): Long {
    return try {
        val ctx = AndroidAppContextHolder.appContext
        val internal = dirSize(ctx.cacheDir)
        val external = ctx.externalCacheDir?.let { dirSize(it) } ?: 0L
        internal + external
    } catch (_: Exception) {
        0L
    }
}

private fun dirSize(dir: File): Long {
    if (!dir.exists()) return 0L
    if (dir.isFile) return dir.length()
    return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
