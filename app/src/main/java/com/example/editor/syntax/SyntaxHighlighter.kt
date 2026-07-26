package com.example.editor.syntax

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.fs.FileType
import java.util.regex.Pattern

object SyntaxHighlighter {

    private val JAVA_KOTLIN_KEYWORDS = setOf(
        "abstract", "actual", "annotation", "as", "break", "by", "catch", "class", "companion",
        "const", "constructor", "continue", "crossinline", "data", "delegate", "do", "else",
        "enum", "expect", "external", "false", "field", "final", "finally", "for", "fun",
        "get", "if", "import", "in", "infix", "init", "inline", "inner", "interface",
        "internal", "is", "it", "lateinit", "noinline", "null", "object", "open", "operator",
        "out", "override", "package", "private", "protected", "public", "reified", "return",
        "sealed", "set", "super", "suspend", "tailrec", "this", "throw", "true", "try",
        "typealias", "val", "var", "vararg", "when", "where", "while",
        "extends", "implements", "instanceof", "native", "new", "static", "strictfp",
        "synchronized", "transient", "volatile", "void"
    )

    private val PRIMITIVE_TYPES = setOf(
        "boolean", "byte", "char", "double", "float", "int", "long", "short",
        "Boolean", "Byte", "Char", "Double", "Float", "Int", "Long", "Short",
        "String", "Any", "Unit", "Nothing", "List", "Set", "Map", "Class", "Object",
        "Logger", "LoggerFactory", "ModInitializer", "ClientModInitializer"
    )

    fun highlightLine(
        line: String,
        fileType: FileType,
        colorScheme: SyntaxColorScheme
    ): AnnotatedString {
        if (line.isEmpty()) return AnnotatedString("")

        return buildAnnotatedString {
            append(line)

            when (fileType) {
                FileType.JAVA, FileType.KOTLIN, FileType.GRADLE -> {
                    highlightCodeLine(line, colorScheme)
                }
                FileType.XML -> {
                    highlightXmlLine(line, colorScheme)
                }
                FileType.JSON -> {
                    highlightJsonLine(line, colorScheme)
                }
                else -> {
                    addStyle(SpanStyle(color = colorScheme.foreground), 0, line.length)
                }
            }
        }
    }

    private fun AnnotatedString.Builder.highlightCodeLine(
        line: String,
        colorScheme: SyntaxColorScheme
    ) {
        var i = 0
        val len = line.length

        while (i < len) {
            val ch = line[i]

            // Single line comment
            if (ch == '/' && i + 1 < len && line[i + 1] == '/') {
                addStyle(SpanStyle(color = colorScheme.comment), i, len)
                break
            }

            // Strings
            if (ch == '"' || ch == '\'') {
                val quote = ch
                val start = i
                i++
                while (i < len) {
                    if (line[i] == '\\' && i + 1 < len) {
                        i += 2
                        continue
                    }
                    if (line[i] == quote) {
                        i++
                        break
                    }
                    i++
                }
                addStyle(SpanStyle(color = colorScheme.string), start, i.coerceAtMost(len))
                continue
            }

            // Annotations (@Name)
            if (ch == '@' && (i == 0 || !line[i - 1].isLetterOrDigit())) {
                val start = i
                i++
                while (i < len && (line[i].isLetterOrDigit() || line[i] == '.')) {
                    i++
                }
                addStyle(SpanStyle(color = colorScheme.annotation, fontWeight = FontWeight.Bold), start, i)
                continue
            }

            // Identifiers / Keywords / Numbers
            if (ch.isLetter() || ch == '_') {
                val start = i
                while (i < len && (line[i].isLetterOrDigit() || line[i] == '_')) {
                    i++
                }
                val token = line.substring(start, i)
                val color = when {
                    JAVA_KOTLIN_KEYWORDS.contains(token) -> colorScheme.keyword
                    PRIMITIVE_TYPES.contains(token) || (token[0].isUpperCase() && !token.all { it.isUpperCase() }) -> colorScheme.type
                    else -> colorScheme.foreground
                }
                val weight = if (JAVA_KOTLIN_KEYWORDS.contains(token)) FontWeight.Bold else FontWeight.Normal
                addStyle(SpanStyle(color = color, fontWeight = weight), start, i)
                continue
            }

            // Numbers
            if (ch.isDigit()) {
                val start = i
                while (i < len && (line[i].isLetterOrDigit() || line[i] == '.' || line[i] == 'x' || line[i] == 'L' || line[i] == 'f')) {
                    i++
                }
                addStyle(SpanStyle(color = colorScheme.number), start, i)
                continue
            }

            // Punctuation
            if ("{}()[]:;,.<>+-*/%=&|^!?".contains(ch)) {
                addStyle(SpanStyle(color = colorScheme.punctuation), i, i + 1)
            } else {
                addStyle(SpanStyle(color = colorScheme.foreground), i, i + 1)
            }
            i++
        }
    }

    private fun AnnotatedString.Builder.highlightXmlLine(
        line: String,
        colorScheme: SyntaxColorScheme
    ) {
        addStyle(SpanStyle(color = colorScheme.foreground), 0, line.length)
        val tagPattern = Pattern.compile("</?([a-zA-Z0-9_:-]+)")
        val attrPattern = Pattern.compile("([a-zA-Z0-9_:-]+)=\"([^\"]*)\"")

        val tagMatcher = tagPattern.matcher(line)
        while (tagMatcher.find()) {
            addStyle(SpanStyle(color = colorScheme.keyword, fontWeight = FontWeight.Bold), tagMatcher.start(), tagMatcher.end())
        }

        val attrMatcher = attrPattern.matcher(line)
        while (attrMatcher.find()) {
            if (attrMatcher.groupCount() >= 2) {
                addStyle(SpanStyle(color = colorScheme.type), attrMatcher.start(1), attrMatcher.end(1))
                addStyle(SpanStyle(color = colorScheme.string), attrMatcher.start(2) - 1, attrMatcher.end(2) + 1)
            }
        }
    }

    private fun AnnotatedString.Builder.highlightJsonLine(
        line: String,
        colorScheme: SyntaxColorScheme
    ) {
        addStyle(SpanStyle(color = colorScheme.foreground), 0, line.length)
        val keyPattern = Pattern.compile("\"([^\"]+)\"\\s*:")
        val stringValPattern = Pattern.compile(":\\s*\"([^\"]*)\"")

        val keyMatcher = keyPattern.matcher(line)
        while (keyMatcher.find()) {
            addStyle(SpanStyle(color = colorScheme.type, fontWeight = FontWeight.Bold), keyMatcher.start(1) - 1, keyMatcher.end(1) + 1)
        }

        val stringValMatcher = stringValPattern.matcher(line)
        while (stringValMatcher.find()) {
            addStyle(SpanStyle(color = colorScheme.string), stringValMatcher.start(1) - 1, stringValMatcher.end(1) + 1)
        }
    }
}
