package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AegisBorder
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary

@Composable
fun AegisMarkdownView(
    text: String,
    modifier: Modifier = Modifier
) {
    val blocks = parseMarkdownBlocks(text)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (block in blocks) {
            when (block) {
                is MarkdownBlock.Code -> {
                    AegisCodeBlockCard(code = block.code, language = block.language)
                }
                is MarkdownBlock.Header -> {
                    Text(
                        text = block.text,
                        color = AegisCyan,
                        fontSize = when (block.level) {
                            1 -> 18.sp
                            2 -> 16.sp
                            else -> 14.sp
                        },
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    )
                }
                is MarkdownBlock.Bullet -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = AegisCyan,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = parseInlineStyles(block.text),
                            color = AegisTextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseInlineStyles(block.text),
                        color = AegisTextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AegisCodeBlockCard(
    code: String,
    language: String
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF040A12))
            .border(1.dp, AegisBorder, RoundedCornerShape(8.dp))
    ) {
        // Header with language tag and copy button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AegisSurfaceElevated)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = (if (language.isBlank()) "CODE" else language).uppercase(),
                color = AegisCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            IconButton(
                onClick = {
                    clipboard.setText(AnnotatedString(code))
                    Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = AegisTextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Code Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            Text(
                text = code,
                color = Color(0xFF80D8FF),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 17.sp
            )
        }
    }
}

private sealed class MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock()
    data class Header(val text: String, val level: Int) : MarkdownBlock()
    data class Bullet(val text: String) : MarkdownBlock()
    data class Code(val code: String, val language: String) : MarkdownBlock()
}

private fun parseMarkdownBlocks(input: String): List<MarkdownBlock> {
    val result = mutableListOf<MarkdownBlock>()
    val lines = input.lines()
    var inCodeBlock = false
    var codeLang = ""
    val codeBuilder = StringBuilder()

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                result.add(MarkdownBlock.Code(code = codeBuilder.toString().trimEnd(), language = codeLang))
                codeBuilder.clear()
                inCodeBlock = false
            } else {
                inCodeBlock = true
                codeLang = trimmed.removePrefix("```").trim()
            }
            continue
        }

        if (inCodeBlock) {
            codeBuilder.appendLine(line)
            continue
        }

        if (trimmed.startsWith("### ")) {
            result.add(MarkdownBlock.Header(trimmed.removePrefix("### "), 3))
        } else if (trimmed.startsWith("## ")) {
            result.add(MarkdownBlock.Header(trimmed.removePrefix("## "), 2))
        } else if (trimmed.startsWith("# ")) {
            result.add(MarkdownBlock.Header(trimmed.removePrefix("# "), 1))
        } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            result.add(MarkdownBlock.Bullet(trimmed.substring(2)))
        } else if (trimmed.isNotEmpty()) {
            result.add(MarkdownBlock.Paragraph(line))
        }
    }

    if (inCodeBlock) {
        result.add(MarkdownBlock.Code(code = codeBuilder.toString().trimEnd(), language = codeLang))
    }

    return result
}

private fun parseInlineStyles(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val regex = Regex("(\\*\\*(.*?)\\*\\*)|(`(.*?)`)")
        val matches = regex.findAll(text)

        for (match in matches) {
            if (match.range.first > cursor) {
                append(text.substring(cursor, match.range.first))
            }

            val fullMatch = match.value
            if (fullMatch.startsWith("**") && fullMatch.endsWith("**")) {
                val boldText = fullMatch.removePrefix("**").removeSuffix("**")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = AegisTextPrimary)) {
                    append(boldText)
                }
            } else if (fullMatch.startsWith("`") && fullMatch.endsWith("`")) {
                val codeText = fullMatch.removePrefix("`").removeSuffix("`")
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = AegisCyan, background = Color(0x3300E5FF))) {
                    append(" $codeText ")
                }
            }
            cursor = match.range.last + 1
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}
