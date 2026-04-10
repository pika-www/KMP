package com.cephalon.lucyApp.media

import androidios.composeapp.generated.resources.Res
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFView
import platform.PDFKit.kPDFDisplaySinglePageContinuous
import platform.QuickLook.QLPreviewController
import platform.QuickLook.QLPreviewControllerDataSourceProtocol
import platform.QuickLook.QLPreviewItemProtocol
import platform.UIKit.UIColor
import platform.UIKit.UIApplication
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewController
import platform.darwin.NSObject

@Composable
actual fun PlatformDocumentPreview(
    source: String,
    fileName: String,
    modifier: Modifier,
) {
    val previewState by produceState<IOSDocumentPreviewState>(
        initialValue = IOSDocumentPreviewState.Loading,
        key1 = source,
        key2 = fileName,
    ) {
        value = try {
            val fileUrl = withContext(Dispatchers.IO) {
                materializeDocumentUrl(source = source, fileName = fileName)
            }
            IOSDocumentPreviewState.Ready(fileUrl)
        } catch (error: Throwable) {
            IOSDocumentPreviewState.Error(error.message.orEmpty().ifBlank { "文档预览失败" })
        }
    }

    when (val state = previewState) {
        IOSDocumentPreviewState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF3A3A3A))
            }
        }
        is IOSDocumentPreviewState.Error -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF3A3A3A)
                )
            }
        }
        is IOSDocumentPreviewState.Ready -> {
            if (fileName.substringAfterLast('.', "").lowercase() == "pdf") {
                IOSPdfDocumentPreview(
                    fileUrl = state.fileUrl,
                    modifier = modifier
                )
            } else {
                IOSQuickLookDocumentPreview(
                    fileUrl = state.fileUrl,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalForeignApi::class)
private fun IOSPdfDocumentPreview(
    fileUrl: NSURL,
    modifier: Modifier = Modifier,
) {
    UIKitView(
        modifier = modifier.fillMaxSize(),
        factory = {
            PDFView().apply {
                this.autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
                this.autoScales = true
                this.displayMode = kPDFDisplaySinglePageContinuous
                this.displaysPageBreaks = false
                this.backgroundColor = UIColor.whiteColor
                this.document = PDFDocument(uRL = fileUrl)
            }
        },
        update = { view ->
            view.document = PDFDocument(uRL = fileUrl)
        }
    )
}

@Composable
@OptIn(ExperimentalForeignApi::class)
private fun IOSQuickLookDocumentPreview(
    fileUrl: NSURL,
    modifier: Modifier = Modifier,
) {
    val controller = remember(fileUrl.absoluteString) { createQuickLookViewController(fileUrl) }

    UIKitViewController(
        modifier = modifier.fillMaxSize(),
        factory = {
            controller
        },
        update = {
        }
    )
}

private sealed interface IOSDocumentPreviewState {
    data object Loading : IOSDocumentPreviewState
    data class Ready(val fileUrl: NSURL) : IOSDocumentPreviewState
    data class Error(val message: String) : IOSDocumentPreviewState
}

@OptIn(ExperimentalForeignApi::class)
private fun createQuickLookViewController(fileUrl: NSURL): UIViewController {
    val previewItem = IOSPreviewItem(fileUrl)
    val dataSource = IOSQuickLookDataSource(previewItem)
    return QLPreviewController().apply {
        this.dataSource = dataSource
        this.reloadData()
    }
}

private class IOSQuickLookDataSource(
    private val item: IOSPreviewItem,
) : NSObject(), QLPreviewControllerDataSourceProtocol {
    override fun numberOfPreviewItemsInPreviewController(controller: QLPreviewController): Long = 1

    override fun previewController(
        controller: QLPreviewController,
        previewItemAtIndex: Long,
    ): QLPreviewItemProtocol = item
}

private class IOSPreviewItem(
    private val fileUrl: NSURL,
) : NSObject(), QLPreviewItemProtocol {
    override fun previewItemURL(): NSURL = fileUrl

    override fun previewItemTitle(): String? = fileUrl.lastPathComponent
}

@OptIn(ExperimentalForeignApi::class, ExperimentalResourceApi::class)
private suspend fun materializeDocumentUrl(
    source: String,
    fileName: String,
): NSURL {
    return when {
        source.startsWith("/") -> NSURL.fileURLWithPath(source)
        source.startsWith("file://") -> NSURL.URLWithString(source) ?: NSURL.fileURLWithPath(source.removePrefix("file://"))
        source.contains("://") -> NSURL.URLWithString(source) ?: error("无法解析文档地址")
        else -> {
            val resourceUri = Res.getUri(source)
            when {
                resourceUri.startsWith("file://") -> {
                    NSURL.URLWithString(resourceUri)
                        ?: NSURL.fileURLWithPath(resourceUri.removePrefix("file://"))
                }
                resourceUri.contains("://") -> {
                    NSURL.URLWithString(resourceUri)
                        ?: error("无法解析资源文档地址")
                }
                else -> NSURL.fileURLWithPath(resourceUri)
            }
        }
    }
}

