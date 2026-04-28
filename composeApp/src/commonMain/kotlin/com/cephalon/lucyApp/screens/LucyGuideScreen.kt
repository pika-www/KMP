package com.cephalon.lucyApp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cephalon.lucyApp.clipboard.platformCopyToClipboard
import com.cephalon.lucyApp.components.LocalDesignScale

// ─── 颜色 ───
private val PageBg = Color(0xFFFDFDFD)
private val TitleColor = Color(0xFF1A1A1A)
private val BodyColor = Color(0xFF333333)
private val SubtleColor = Color(0xFF666666)
private val CodeBg = Color(0xFFF5F5F7)
private val CodeBorder = Color(0xFFE5E5EA)
private val InlineCodeBg = Color(0xFFEEEEF0)
private val InlineCodeColor = Color(0xFF0055AA)
private val DividerColor = Color(0xFFE5E5EA)
private val CopyIconColor = Color(0xFF999999)
private val CommentColor = Color(0xFF6A9955)
private val KeyColor = Color(0xFF0451A5)
private val ValueColor = Color(0xFF098658)
private val KeywordColor = Color(0xFFAF00DB)

@Composable
fun LucyGuideScreen(
    onBack: () -> Unit,
) {
    val ds = LocalDesignScale.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── 顶部栏 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F0F2)),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TitleColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(ds.sh(20.dp)))

        // ── 内容 ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            // ── Lucy 标题 ──
            Text(
                text = "Lucy",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TitleColor,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 简介 ──
            Text(
                text = buildAnnotatedString {
                    append("Lucy 是 OpenClaw 的 NATS-based Channel 插件，把 OpenClaw Gateway 接到 Lucy App 的消息链路上。插件包名是 ")
                    appendInlineCode("@hzttt/lucy-ai-npc")
                    append("；在 OpenClaw 内部，插件 ID、配置前缀和 CLI 命令都叫 ")
                    appendInlineCode("lucy")
                    append("。")
                },
                fontSize = 14.sp,
                color = BodyColor,
                lineHeight = 22.sp,
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(24.dp))

            // ── 版本要求 ──
            SectionTitle("版本要求")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = buildAnnotatedString {
                    append("OpenClaw ")
                    appendInlineCode("2026.3.23")
                    append(" 及以上。")
                },
                fontSize = 14.sp,
                color = BodyColor,
                lineHeight = 22.sp,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── 最小接入三步 ──
            SectionTitle("最小接入三步")
            Spacer(modifier = Modifier.height(12.dp))

            val quickStartCode = """
                |# 1. 安装插件
                |openclaw plugins install @hzttt/lucy-ai-npc
                |
                |# 2. 启用 Lucy channel
                |openclaw config set channels.lucy.enabled true
                |
                |# 3. 生成绑定二维码
                |openclaw lucy auth-qrcode
            """.trimMargin()
            CodeBlock(code = quickStartCode, onCopy = {
                platformCopyToClipboard(quickStartCode)
            })

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = buildAnnotatedString {
                    append("在 Lucy App 中扫描二维码，完成设备绑定。绑定完成后 App 可拉取 ")
                    appendInlineCode("GET /v1/channels/lucy/current-user/model-config")
                    append(" 获取模型配置，然后推送给设备。设备自动重启后上线。")
                },
                fontSize = 14.sp,
                color = BodyColor,
                lineHeight = 22.sp,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── 常用验证命令 ──
            SectionTitle("常用验证命令")
            Spacer(modifier = Modifier.height(12.dp))

            val verifyCode = """
                |# 查看 Lucy 运行状态和连接信息
                |openclaw channels status --probe
                |
                |# 重新生成设备身份和绑定 QR 码（清空本地状态）
                |openclaw lucy reset-state
            """.trimMargin()
            CodeBlock(code = verifyCode, onCopy = {
                platformCopyToClipboard(verifyCode)
            })

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(24.dp))

            // ── 常见配置 ──
            SectionTitle("常见配置")
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "最小配置（NATS 服务器地址由 lucy-server 动态返回）：",
                fontSize = 14.sp,
                color = BodyColor,
                lineHeight = 22.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))

            val minConfig = """
                |{
                |  "channels": {
                |    "lucy": {
                |      "enabled": true
                |    }
                |  }
                |}
            """.trimMargin()
            CodeBlock(code = minConfig, language = "json", onCopy = {
                platformCopyToClipboard(minConfig)
            })

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "如需本地 USB 通知事件回报：",
                fontSize = 14.sp,
                color = BodyColor,
                lineHeight = 22.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))

            val usbConfig = """
                |{
                |  "channels": {
                |    "lucy": {
                |      "enabled": true,
                |      "localNotify": {
                |        "enabled": true,
                |        "bind": "127.0.0.1",
                |        "port": 8788,
                |        "path": "/usb-events"
                |      }
                |    }
                |  }
                |}
            """.trimMargin()
            CodeBlock(code = usbConfig, language = "json", onCopy = {
                platformCopyToClipboard(usbConfig)
            })

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// ── 区块标题 ──
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = TitleColor,
    )
}

// ── 代码块 ──
@Composable
private fun CodeBlock(
    code: String,
    language: String = "shell",
    onCopy: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CodeBg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = if (language == "json") colorizeJson(code) else colorizeShell(code),
                fontSize = 12.5.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 20.sp,
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
            )

            // 复制按钮
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, end = 8.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onCopy),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "📋",
                    fontSize = 15.sp,
                )
            }
        }
    }
}

// ── 内联代码 helper ──
private fun AnnotatedString.Builder.appendInlineCode(text: String) {
    withStyle(
        SpanStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = InlineCodeColor,
            background = InlineCodeBg,
        )
    ) {
        append(" $text ")
    }
}

// ── Shell 语法着色 ──
private fun colorizeShell(code: String): AnnotatedString = buildAnnotatedString {
    code.lines().forEach { line ->
        val trimmed = line.trimStart()
        if (trimmed.startsWith("#")) {
            withStyle(SpanStyle(color = CommentColor)) { append(line) }
        } else {
            val parts = line.split(" ", limit = 2)
            if (parts.isNotEmpty()) {
                withStyle(SpanStyle(color = BodyColor, fontWeight = FontWeight.Medium)) {
                    append(parts[0])
                }
                if (parts.size > 1) {
                    append(" ")
                    val rest = parts[1]
                    // 高亮 true / false
                    if (rest.endsWith("true") || rest.endsWith("false")) {
                        val idx = rest.lastIndexOf(' ')
                        if (idx >= 0) {
                            withStyle(SpanStyle(color = BodyColor)) { append(rest.substring(0, idx + 1)) }
                            withStyle(SpanStyle(color = KeywordColor, fontWeight = FontWeight.SemiBold)) {
                                append(rest.substring(idx + 1))
                            }
                        } else {
                            withStyle(SpanStyle(color = KeywordColor, fontWeight = FontWeight.SemiBold)) {
                                append(rest)
                            }
                        }
                    } else {
                        withStyle(SpanStyle(color = BodyColor)) { append(rest) }
                    }
                }
            }
        }
        append("\n")
    }
}

// ── JSON 语法着色 ──
private fun colorizeJson(code: String): AnnotatedString = buildAnnotatedString {
    code.lines().forEach { line ->
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    val end = line.indexOf('"', i + 1)
                    if (end < 0) {
                        withStyle(SpanStyle(color = BodyColor)) { append(line.substring(i)) }
                        i = line.length
                    } else {
                        val content = line.substring(i, end + 1)
                        // key vs value: key 后面紧跟 ':'
                        val afterQuote = line.substring(end + 1).trimStart()
                        val isKey = afterQuote.startsWith(":")
                        withStyle(SpanStyle(color = if (isKey) KeyColor else ValueColor)) {
                            append(content)
                        }
                        i = end + 1
                    }
                }
                c.isDigit() || (c == '-' && i + 1 < line.length && line[i + 1].isDigit()) -> {
                    val start = i
                    while (i < line.length && (line[i].isDigit() || line[i] == '.')) i++
                    withStyle(SpanStyle(color = ValueColor)) { append(line.substring(start, i)) }
                }
                line.substring(i).startsWith("true") || line.substring(i).startsWith("false") -> {
                    val kw = if (line.substring(i).startsWith("true")) "true" else "false"
                    withStyle(SpanStyle(color = KeywordColor, fontWeight = FontWeight.SemiBold)) {
                        append(kw)
                    }
                    i += kw.length
                }
                else -> {
                    append(c)
                    i++
                }
            }
        }
        append("\n")
    }
}
