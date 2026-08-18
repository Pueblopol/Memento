package com.pol.memento.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class MarkdownVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        boldRegex.findAll(text.text).forEach { matchResult ->
            builder.addStyle(
                SpanStyle(fontWeight = FontWeight.Bold),
                matchResult.range.first,
                matchResult.range.last + 1
            )
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

fun parseMarkdown(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
    boldRegex.findAll(text).forEach { matchResult ->
        builder.addStyle(
            SpanStyle(fontWeight = FontWeight.Bold),
            matchResult.range.first,
            matchResult.range.last + 1
        )
    }
    return builder.toAnnotatedString()
}
