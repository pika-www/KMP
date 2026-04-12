package com.cephalon.lucyApp.screens.agentmodel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cephalon.lucyApp.components.LocalDesignScale

@Composable
internal fun Bubble(
    text: String,
    background: Color,
    textColor: Color,
    alignEnd: Boolean,
    border: BorderStroke? = null,
    isMarkdown: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val ds = LocalDesignScale.current
    BubbleContainer(alignEnd = alignEnd) { bubbleMaxWidth ->
        if (alignEnd) {
            Card(
                shape = RoundedCornerShape(ds.sm(16.dp)),
                colors = CardDefaults.cardColors(containerColor = background),
                border = border,
                modifier = Modifier
                    .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                    .wrapContentWidth()
                    .widthIn(max = bubbleMaxWidth)
            ) {
                if (isMarkdown) {
                    MarkdownBubbleText(
                        markdown = text,
                        textColor = textColor,
                        modifier = Modifier.padding(horizontal = ds.sw(14.dp), vertical = ds.sh(12.dp))
                    )
                } else {
                    Text(
                        text = text,
                        fontSize = ds.sp(14f),
                        color = textColor,
                        modifier = Modifier.padding(horizontal = ds.sw(14.dp), vertical = ds.sh(12.dp))
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                    .wrapContentWidth()
                    .widthIn(max = bubbleMaxWidth)
            ) {
                if (isMarkdown) {
                    MarkdownBubbleText(
                        markdown = text,
                        textColor = textColor,
                        modifier = Modifier
                    )
                } else {
                    Text(
                        text = text,
                        fontSize = ds.sp(14f),
                        color = textColor,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownBubbleText(
    markdown: String,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val ds = LocalDesignScale.current
    val blocks = markdown.split("```")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ds.sh(6.dp))) {
        blocks.forEachIndexed { index, block ->
            if (index % 2 == 1) {
                Surface(
                    shape = RoundedCornerShape(ds.sm(10.dp)),
                    color = Color(0xFF1F1F1F)
                ) {
                    Text(
                        text = block.trim('\n'),
                        color = Color(0xFFEAEAEA),
                        fontSize = ds.sp(13f),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = ds.sw(10.dp), vertical = ds.sh(8.dp))
                    )
                }
            } else {
                val lines = block.lines().map { it.trimEnd() }
                var i = 0
                while (i < lines.size) {
                    val line = lines[i]
                    if (line.isBlank()) { i++; continue }

                    if (line.trimStart().startsWith("|")) {
                        val tableLines = mutableListOf<String>()
                        while (i < lines.size && lines[i].trimStart().startsWith("|")) {
                            tableLines.add(lines[i])
                            i++
                        }
                        MarkdownTable(tableLines, textColor)
                    } else {
                        when {
                            line.startsWith("### ") -> MarkdownLine(
                                line.removePrefix("### "),
                                textColor,
                                ds.sp(16f),
                                FontWeight.SemiBold
                            )

                            line.startsWith("## ") -> MarkdownLine(
                                line.removePrefix("## "),
                                textColor,
                                ds.sp(18f),
                                FontWeight.SemiBold
                            )

                            line.startsWith("# ") -> MarkdownLine(
                                line.removePrefix("# "),
                                textColor,
                                ds.sp(20f),
                                FontWeight.Bold
                            )

                            line.startsWith("- ") || line.startsWith("* ") -> MarkdownLine(
                                "• ${line.drop(2)}",
                                textColor,
                                ds.sp(14f),
                                FontWeight.Normal
                            )

                            else -> MarkdownLine(line, textColor, ds.sp(14f), FontWeight.Normal)
                        }
                        i++
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownTable(
    tableLines: List<String>,
    textColor: Color,
) {
    val ds = LocalDesignScale.current
    val borderColor = Color(0xFFE0E0E0)

    val rows = tableLines.mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.matches(Regex("^\\|[\\s\\-:|]+\\|$"))) return@mapNotNull null
        trimmed.trim('|').split("|").map { it.trim() }
    }
    if (rows.isEmpty()) return

    val columnCount = rows.maxOf { it.size }

    Surface(
        shape = RoundedCornerShape(ds.sm(8.dp)),
        border = BorderStroke(0.5.dp, borderColor),
        color = Color.Transparent
    ) {
        Column {
            rows.forEachIndexed { rowIndex, cells ->
                val isHeader = rowIndex == 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .then(
                            if (isHeader) Modifier.background(Color(0xFFF5F5F7))
                            else Modifier
                        )
                ) {
                    for (colIndex in 0 until columnCount) {
                        if (colIndex > 0) {
                            Box(
                                modifier = Modifier
                                    .width(0.5.dp)
                                    .fillMaxHeight()
                                    .background(borderColor)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(
                                    horizontal = ds.sw(8.dp),
                                    vertical = ds.sh(6.dp)
                                )
                        ) {
                            Text(
                                text = markdownInlineAnnotatedString(
                                    cells.getOrElse(colIndex) { "" },
                                    textColor
                                ),
                                fontSize = ds.sp(13f),
                                fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
                                color = textColor
                            )
                        }
                    }
                }
                if (rowIndex < rows.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(borderColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownLine(
    line: String,
    textColor: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
) {
    Text(
        text = markdownInlineAnnotatedString(line, textColor),
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = textColor
    )
}

private fun markdownInlineAnnotatedString(text: String, textColor: Color): AnnotatedString {
    val regex = Regex("(\\*\\*[^*]+\\*\\*)|(`[^`]+`)|(\\[[^\\]]+\\]\\([^)]*\\))")
    return buildAnnotatedString {
        var lastIndex = 0
        regex.findAll(text).forEach { match ->
            if (match.range.first > lastIndex) {
                append(text.substring(lastIndex, match.range.first))
            }
            val token = match.value
            when {
                token.startsWith("**") && token.endsWith("**") -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor))
                    append(token.removePrefix("**").removeSuffix("**"))
                    pop()
                }

                token.startsWith("`") && token.endsWith("`") -> {
                    pushStyle(
                        SpanStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            background = Color(0x14000000),
                            color = textColor
                        )
                    )
                    append(token.removePrefix("`").removeSuffix("`"))
                    pop()
                }

                token.startsWith("[") -> {
                    val label = token.substringAfter("[").substringBefore("]")
                    pushStyle(
                        SpanStyle(
                            color = Color(0xFF2F6FED),
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    append(label)
                    pop()
                }
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

@Composable
internal fun BubbleContainer(
    alignEnd: Boolean,
    content: @Composable (Dp) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val bubbleMaxWidth = maxWidth * 0.82f
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
        ) {
            content(bubbleMaxWidth)
        }
    }
}
