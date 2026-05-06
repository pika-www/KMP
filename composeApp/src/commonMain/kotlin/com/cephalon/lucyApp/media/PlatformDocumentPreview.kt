package com.cephalon.lucyApp.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

@Composable
expect fun PlatformDocumentPreview(
    source: String,
    fileName: String,
    modifier: Modifier = Modifier,
)

suspend expect fun platformReadTextDocument(
    source: String,
    fileName: String,
): String

@Composable
internal fun TextDocumentPreview(
    source: String,
    fileName: String,
    modifier: Modifier = Modifier,
) {
    val previewText by produceState<String?>(
        initialValue = null,
        key1 = source,
        key2 = fileName,
    ) {
        value = withContext(Dispatchers.IO) {
            platformReadTextDocument(source = source, fileName = fileName)
        }
    }

    val verticalScrollState = rememberScrollState()

    SelectionContainer {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF7F7F8))
                .verticalScroll(verticalScrollState)
                .padding(16.dp)
        ) {
            Text(
                text = previewText ?: "",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                color = Color(0xFF1C1C1E)
            )
        }
    }
}
