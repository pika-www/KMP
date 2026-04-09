package com.cephalon.lucyApp.screens.agentmodel

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun clearAppCache() {
    val fileManager = NSFileManager.defaultManager
    val cachePaths = fileManager.URLsForDirectory(NSCachesDirectory, NSUserDomainMask)
    cachePaths.forEach { url ->
        @Suppress("UNCHECKED_CAST")
        val nsUrl = url as? platform.Foundation.NSURL ?: return@forEach
        val path = nsUrl.path ?: return@forEach
        val contents = fileManager.contentsOfDirectoryAtPath(path, null) ?: return@forEach
        @Suppress("UNCHECKED_CAST")
        (contents as List<String>).forEach { item ->
            fileManager.removeItemAtPath("$path/$item", null)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun getAppCacheSize(): Long {
    val fileManager = NSFileManager.defaultManager
    val cachePaths = fileManager.URLsForDirectory(NSCachesDirectory, NSUserDomainMask)
    var totalSize = 0L
    cachePaths.forEach { url ->
        @Suppress("UNCHECKED_CAST")
        val nsUrl = url as? platform.Foundation.NSURL ?: return@forEach
        val path = nsUrl.path ?: return@forEach
        totalSize += directorySize(fileManager, path)
    }
    return totalSize
}

@OptIn(ExperimentalForeignApi::class)
private fun directorySize(fileManager: NSFileManager, path: String): Long {
    val enumerator = fileManager.enumeratorAtPath(path) ?: return 0L
    var size = 0L
    while (true) {
        val file = enumerator.nextObject() as? String ?: break
        val fullPath = "$path/$file"
        val attrs = fileManager.attributesOfItemAtPath(fullPath, null) ?: continue
        val fileSize = attrs[NSFileSize] as? Long ?: (attrs[NSFileSize] as? Number)?.toLong() ?: 0L
        size += fileSize
    }
    return size
}
