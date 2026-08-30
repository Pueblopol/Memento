package com.pol.memento.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class MarkdownVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(parseMarkdown(text.text), OffsetMapping.Identity)
    }
}

fun parseMarkdown(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    
    // Bold
    val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
    boldRegex.findAll(text).forEach { matchResult ->
        builder.addStyle(
            SpanStyle(fontWeight = FontWeight.Bold),
            matchResult.range.first,
            matchResult.range.last + 1
        )
    }
    
    // Markdown links [text](url)
    val linkRegex = Regex("\\[(.*?)\\]\\((.*?)\\)")
    linkRegex.findAll(text).forEach { matchResult ->
        val url = matchResult.groupValues[2]
        builder.addStyle(
            SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF2196F3), textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
            matchResult.range.first,
            matchResult.range.last + 1
        )
        builder.addStringAnnotation(
            tag = "URL",
            annotation = url,
            start = matchResult.range.first,
            end = matchResult.range.last + 1
        )
    }
    
    // Plain URLs http...
    val urlRegex = Regex("(?<!\\()(https?://[^\\s]+)(?!\\))")
    urlRegex.findAll(text).forEach { matchResult ->
        val url = matchResult.groupValues[1]
        builder.addStyle(
            SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF2196F3), textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
            matchResult.range.first,
            matchResult.range.last + 1
        )
        builder.addStringAnnotation(
            tag = "URL",
            annotation = url,
            start = matchResult.range.first,
            end = matchResult.range.last + 1
        )
    }
    
    return builder.toAnnotatedString()
}
