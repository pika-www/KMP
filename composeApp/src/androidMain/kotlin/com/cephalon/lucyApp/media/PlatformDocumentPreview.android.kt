package com.cephalon.lucyApp.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidios.composeapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.io.File

@Composable
actual fun PlatformDocumentPreview(
    source: String,
    fileName: String,
    modifier: Modifier,
) {
    val extension = remember(fileName, source) {
        fileName.substringAfterLast('.', source.substringAfterLast('.', "")).lowercase()
    }

    if (extension == "pdf") {
        AndroidPdfDocumentPreview(
            source = source,
            fileName = fileName,
            modifier = modifier
        )
    } else {
        AndroidUnsupportedDocumentPreview(
            source = source,
            fileName = fileName,
            modifier = modifier
        )
    }
}

@Composable
private fun AndroidPdfDocumentPreview(
    source: String,
    fileName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val previewState by produceState<AndroidDocumentPreviewState>(
        initialValue = AndroidDocumentPreviewState.Loading,
        key1 = source,
        key2 = fileName,
    ) {
        value = try {
            val pages = withContext(Dispatchers.IO) {
                loadPdfPages(context = context, source = source, fileName = fileName)
            }
            AndroidDocumentPreviewState.Ready(pages)
        } catch (error: Throwable) {
            AndroidDocumentPreviewState.Error(error.message.orEmpty().ifBlank { "文档预览失败" })
        }
    }

    when (val state = previewState) {
        AndroidDocumentPreviewState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF3A3A3A))
            }
        }
        is AndroidDocumentPreviewState.Error -> {
            AndroidPreviewError(
                message = state.message,
                source = source,
                fileName = fileName,
                modifier = modifier
            )
        }
        is AndroidDocumentPreviewState.Ready -> {
            if (state.pages.isEmpty()) {
                AndroidPreviewError(
                    message = "文档没有可显示的页面",
                    source = source,
                    fileName = fileName,
                    modifier = modifier
                )
            } else {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .background(Color(0xFFF7F7F8)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(state.pages) { _, page ->
                        Image(
                            bitmap = page,
                            contentDescription = fileName,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AndroidUnsupportedDocumentPreview(
    source: String,
    fileName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "当前 Android 端暂不支持应用内预览 ${fileName.substringAfterLast('.', "").uppercase()}，可使用系统文档预览。",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF3A3A3A),
            textAlign = TextAlign.Center
        )

        errorMessage?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFF3B30),
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = {
                scope.launch {
                    errorMessage = runCatching {
                        val file = materializeDocumentFile(context = context, source = source, fileName = fileName)
                        openDocumentExternally(context = context, file = file, fileName = fileName)
                        null
                    }.getOrElse {
                        it.message.orEmpty().ifBlank { "打开系统预览失败" }
                    }
                }
            },
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text("打开系统预览")
        }
    }
}

@Composable
private fun AndroidPreviewError(
    message: String,
    source: String,
    fileName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var actionError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF3A3A3A),
            textAlign = TextAlign.Center
        )

        actionError?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFF3B30),
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = {
                scope.launch {
                    actionError = runCatching {
                        val file = materializeDocumentFile(context = context, source = source, fileName = fileName)
                        openDocumentExternally(context = context, file = file, fileName = fileName)
                        null
                    }.getOrElse {
                        it.message.orEmpty().ifBlank { "打开系统预览失败" }
                    }
                }
            },
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text("使用系统预览")
        }
    }
}

private sealed interface AndroidDocumentPreviewState {
    data object Loading : AndroidDocumentPreviewState
    data class Ready(val pages: List<ImageBitmap>) : AndroidDocumentPreviewState
    data class Error(val message: String) : AndroidDocumentPreviewState
}

private suspend fun loadPdfPages(
    context: Context,
    source: String,
    fileName: String,
): List<ImageBitmap> {
    val file = materializeDocumentFile(context = context, source = source, fileName = fileName)
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(descriptor)
    return try {
        buildList(renderer.pageCount) {
            for (pageIndex in 0 until renderer.pageCount) {
                val page = renderer.openPage(pageIndex)
                try {
                    val width = (page.width * 2).coerceAtLeast(1)
                    val height = (page.height * 2).coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    add(bitmap.asImageBitmap())
                } finally {
                    page.close()
                }
            }
        }
    } finally {
        renderer.close()
        descriptor.close()
    }
}

@OptIn(ExperimentalResourceApi::class)
private suspend fun materializeDocumentFile(
    context: Context,
    source: String,
    fileName: String,
): File {
    val targetFile = File(context.cacheDir, "nas_preview_${source.hashCode()}_${sanitizeFileName(fileName)}")
    return when {
        source.startsWith("/") -> File(source)
        source.startsWith("file://") -> {
            val path = Uri.parse(source).path.orEmpty()
            File(path)
        }
        source.startsWith("content://") -> {
            context.contentResolver.openInputStream(Uri.parse(source))?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: error("无法读取文档流")
            targetFile
        }
        else -> {
            val bytes = Res.readBytes(source)
            targetFile.outputStream().use { output ->
                output.write(bytes)
            }
            targetFile
        }
    }
}

private fun openDocumentExternally(
    context: Context,
    file: File,
    fileName: String,
) {
    val fileUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val extension = fileName.substringAfterLast('.', "").lowercase()
    val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(fileUri, mimeType)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, fileName)
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(chooser)
    } else {
        error("当前设备没有可用于预览该文件的应用")
    }
}

private fun sanitizeFileName(fileName: String): String {
    return fileName.map { char ->
        when {
            char.isLetterOrDigit() || char == '.' || char == '_' || char == '-' -> char
            else -> '_'
        }
    }.joinToString(separator = "")
}
