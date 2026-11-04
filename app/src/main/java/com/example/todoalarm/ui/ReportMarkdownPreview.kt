package com.example.todoalarm.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import org.commonmark.node.*
import org.commonmark.parser.Parser

private val previewParser = Parser.builder().build()

/** Native, non-interactive preview. HTML is not executed and images are never fetched. */
internal fun reportPreviewText(markdown: String): AnnotatedString {
    val tree = previewParser.parse(markdown.take(12_000))
    val rendered = buildAnnotatedString {
        fun children(parent: Node) {
            var child = parent.firstChild
            while (child != null) {
                val node = child
                when (node) {
                    is org.commonmark.node.Text -> append(node.literal)
                    is Code -> append(node.literal)
                    is StrongEmphasis -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { children(node) }
                    is Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { children(node) }
                    is Heading -> { if (length > 0) append('\n'); withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { children(node) }; append('\n') }
                    is Paragraph -> { children(node); append('\n') }
                    is ListItem -> { append("• "); children(node) }
                    is SoftLineBreak, is HardLineBreak -> append(' ')
                    is FencedCodeBlock -> { append(node.literal.trimEnd()); append('\n') }
                    is IndentedCodeBlock -> { append(node.literal.trimEnd()); append('\n') }
                    is HtmlBlock, is HtmlInline, is ThematicBreak -> Unit
                    else -> children(node)
                }
                child = node.next
            }
        }
        children(tree)
    }
    val end = rendered.text.trimEnd().length.coerceAtMost(900)
    return if (end == 0) AnnotatedString("（空报告）") else rendered.subSequence(0, end)
}

@Composable
internal fun ReportMarkdownPreview(markdown: String) {
    val preview = remember(markdown) { reportPreviewText(markdown) }
    Text(preview, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
        maxLines = 5, overflow = TextOverflow.Ellipsis)
}
