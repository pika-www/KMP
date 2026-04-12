package com.cephalon.lucyApp.screens.agentmodel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
    BubbleContainer(alignEnd = alignEnd) { bubbleMaxWidth ->
        Card(
            shape = RoundedCornerShape(16.dp),
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
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
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
    val blocks = markdown.split("```")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEachIndexed { index, block ->
            if (index % 2 == 1) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1F1F1F)
                ) {
                    Text(
                        text = block.trim('\n'),
                        color = Color(0xFFEAEAEA),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            } else {
                block.lines().forEach { rawLine ->
                    val line = rawLine.trimEnd()
                    if (line.isBlank()) return@forEach
                    when {
                        line.startsWith("### ") -> MarkdownLine(
                            line.removePrefix("### "),
                            textColor,
                            MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )

                        line.startsWith("## ") -> MarkdownLine(
                            line.removePrefix("## "),
                            textColor,
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )

                        line.startsWith("# ") -> MarkdownLine(
                            line.removePrefix("# "),
                            textColor,
                            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )

                        line.startsWith("- ") || line.startsWith("* ") -> MarkdownLine(
                            "• ${line.drop(2)}",
                            textColor,
                            MaterialTheme.typography.bodyMedium
                        )

                        else -> MarkdownLine(line, textColor, MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownLine(
    line: String,
    textColor: Color,
    style: androidx.compose.ui.text.TextStyle,
) {
    Text(
        text = markdownInlineAnnotatedString(line, textColor),
        style = style,
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
