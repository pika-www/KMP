package com.cephalon.lucyApp.media

/**
 * 将字节数据保存到设备本地。
 * - 图片类型保存到系统相册
 * - 音频/文档类型保存到下载目录或 Files 目录
 *
 * @param bytes 文件字节数据
 * @param fileName 文件名（含扩展名）
 * @param mimeType MIME 类型，如 "image/jpeg"
 * @return 保存后的文件路径或 URI 字符串（成功时），失败时抛异常
 */
expect suspend fun platformSaveFile(bytes: ByteArray, fileName: String, mimeType: String): String

/**
 * 将字节数据保存到应用缓存目录（临时文件）。
 * 用于预览文档/音频等场景，文件可被系统清理。
 *
 * @param bytes 文件字节数据
 * @param fileName 文件名（含扩展名）
 * @return 保存后的文件绝对路径
 */
expect suspend fun platformSaveCacheFile(bytes: ByteArray, fileName: String): String
