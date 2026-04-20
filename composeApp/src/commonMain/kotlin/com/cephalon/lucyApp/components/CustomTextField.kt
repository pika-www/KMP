package com.cephalon.lucyApp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val InputTextColor = Color(0xFF1F2535)
private val PlaceholderColor = Color(0xFFAAADB6)
private val CodeLinkColor = Color(0xFF0A59F7)
private val InputShape = RoundedCornerShape(12.dp)

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
    isVerificationCode: Boolean = false,
    onSendCode: ((startTimer: () -> Unit) -> Unit)? = null,
    canSend: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    val ds = LocalDesignScale.current
    var timeLeft by remember { mutableIntStateOf(0) }
    val isCountingDown = timeLeft > 0
    var hasSentOnce by remember { mutableStateOf(false) }

    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000L)
            timeLeft -= 1
        }
    }

    val textStyle = TextStyle(
        color = InputTextColor,
        fontSize = ds.sp(14f),
        fontWeight = FontWeight.Normal,
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(ds.sh(52.dp))
            .shadow(
                elevation = 10.dp,
                shape = InputShape,
                ambientColor = Color.Black.copy(alpha = 0.02f),
                spotColor = Color.Black.copy(alpha = 0.02f)
            )
            .background(Color.White, InputShape)
            .padding(horizontal = ds.sw(16.dp)),
        textStyle = textStyle,
        cursorBrush = SolidColor(InputTextColor),
        singleLine = singleLine,
        enabled = enabled,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = label,
                            color = PlaceholderColor,
                            fontSize = ds.sp(14f),
                            fontWeight = FontWeight.Normal,
                        )
                    }
                    innerTextField()
                }
                if (isVerificationCode) {
                    val codeClickable = !isCountingDown && enabled && canSend
                    Text(
                        text = when {
                            isCountingDown -> "${timeLeft}s"
                            hasSentOnce -> "重新发送"
                            else -> "获取验证码"
                        },
                        color = when {
                            isCountingDown -> PlaceholderColor
                            !canSend -> PlaceholderColor
                            else -> CodeLinkColor
                        },
                        fontSize = ds.sp(12f),
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.noRippleClickable(enabled = codeClickable) {
                            onSendCode?.invoke {
                                timeLeft = 60
                                hasSentOnce = true
                            }
                        }
                    )
                } else {
                    trailingIcon?.invoke()
                }
            }
        }
    )
}

private fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = this.clickable(
    interactionSource = MutableInteractionSource(),
    indication = null,
    enabled = enabled,
    onClick = onClick
)

private fun normalizeChinaPhoneOrNull(input: String): String? {
    val raw = input.trim().replace(" ", "")
    val withoutPrefix = when {
        raw.startsWith("+86") -> raw.removePrefix("+86")
        raw.startsWith("86") && raw.length > 11 -> raw.removePrefix("86")
        else -> raw
    }
    val normalized = withoutPrefix.trim()
    return if (Regex("^1\\d{10}$").matches(normalized)) normalized else null
}

private fun normalizeComEmailOrNull(input: String): String? {
    val raw = input.trim()
    return if (Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.com$").matches(raw)) raw else null
}

@Composable
fun AccountInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onNext: (() -> Unit)? = null,
    onValidationError: ((String) -> Unit)? = null,
    onValidated: ((normalized: String, isEmail: Boolean) -> Unit)? = null,
) {
    CustomTextField(
        value = value,
        onValueChange = onValueChange,
        label = "请输入邮箱/手机",
        leadingIcon = Icons.Default.Person,
        enabled = enabled,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                val input = value.trim()
                if (input.isBlank()) {
                    onValidationError?.invoke("请输入账号")
                    return@KeyboardActions
                }

                val normalizedEmail = normalizeComEmailOrNull(input)
                if (normalizedEmail != null) {
                    onValidated?.invoke(normalizedEmail, true)
                    onNext?.invoke()
                    return@KeyboardActions
                }

                val normalizedPhone = normalizeChinaPhoneOrNull(input)
                if (normalizedPhone != null) {
                    onValidated?.invoke(normalizedPhone, false)
                    onNext?.invoke()
                    return@KeyboardActions
                }

                onValidationError?.invoke("请输入正确的手机号(+86 11位)或邮箱(.com)")
            }
        )
    )
}

@Composable
fun EmailInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onNext: (() -> Unit)? = null,
    onValidationError: ((String) -> Unit)? = null,
    onValidated: ((normalized: String) -> Unit)? = null,
) {
    CustomTextField(
        value = value,
        onValueChange = onValueChange,
        label = "请输入邮箱",
        leadingIcon = Icons.Default.Person,
        enabled = enabled,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                val normalized = normalizeComEmailOrNull(value)
                if (normalized == null) {
                    onValidationError?.invoke("请输入正确的邮箱(.com)")
                    return@KeyboardActions
                }
                onValidated?.invoke(normalized)
                onNext?.invoke()
            }
        )
    )
}

@Composable
fun CodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onSendCode: ((startTimer: () -> Unit) -> Unit)? = null,
    canSend: Boolean = true,
) {
    CustomTextField(
        value = value,
        onValueChange = onValueChange,
        label = "请输入验证码",
        leadingIcon = Icons.Default.LockOpen,
        enabled = enabled,
        modifier = modifier,
        isVerificationCode = true,
        onSendCode = onSendCode,
        canSend = canSend,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction
        )
    )
}

@Composable
fun PasswordInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "密码",
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    errorText: String? = null,
) {
    var visible by remember { mutableStateOf(false) }
    val ds = LocalDesignScale.current

    Column(modifier = modifier) {
        CustomTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            leadingIcon = Icons.Default.Lock,
            enabled = enabled,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Icon(
                    imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = PlaceholderColor,
                    modifier = Modifier
                        .size(20.dp)
                        .noRippleClickable { visible = !visible }
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    onDone?.invoke()
                }
            )
        )
        if (!errorText.isNullOrBlank()) {
            Text(
                text = errorText,
                color = Color(0xFFE53935),
                fontSize = ds.sp(12f),
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

/**
 * 密码合法性校验。
 * 规则：
 * - 长度 >= 6
 * - 只能包含 ASCII 数字 / 英文字母 / 英文符号（码位 0x21-0x7E，不含空格、中文等）
 * - 上述三类字符中至少包含两种
 * @return null 表示合法；非 null 为错误提示文案。空字符串返回 null（由调用方决定是否提示"请输入密码"）。
 */
fun validatePasswordRule(pwd: String): String? {
    if (pwd.isEmpty()) return null
    if (pwd.length < 6) return "密码长度至少 6 位"
    val allowed = 0x21..0x7E
    if (pwd.any { it.code !in allowed }) return "密码只能包含数字、英文字母或英文符号"
    val hasDigit = pwd.any { it in '0'..'9' }
    val hasLetter = pwd.any { it in 'a'..'z' || it in 'A'..'Z' }
    val hasSymbol = pwd.any { c ->
        c.code in allowed && c !in '0'..'9' && c !in 'a'..'z' && c !in 'A'..'Z'
    }
    val types = listOf(hasDigit, hasLetter, hasSymbol).count { it }
    if (types < 2) return "至少包含 数字 / 字母 / 符号 中的两种"
    return null
}

@Composable
fun PhoneOnlyInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "请输入手机号",
    imeAction: ImeAction = ImeAction.Next,
) {
    CustomTextField(
        value = value,
        onValueChange = { newValue ->
            val digitsOnly = newValue.filter { it.isDigit() }
            if (digitsOnly.length <= 11) onValueChange(digitsOnly)
        },
        label = label,
        leadingIcon = Icons.Default.Person,
        enabled = enabled,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = imeAction
        )
    )
}