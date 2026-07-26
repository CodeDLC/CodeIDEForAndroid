package com.example.editor.syntax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.fs.FileType

enum class TokenType {
    KEYWORD, TYPE, STRING, NUMBER, COMMENT, ANNOTATION, PUNCTUATION, IDENTIFIER, ERROR, DEFAULT
}

data class SyntaxToken(
    val start: Int,
    val end: Int,
    val type: TokenType
)

data class CodeProblem(
    val line: Int, // 1-based line index
    val column: Int, // 1-based column
    val length: Int,
    val severity: Severity,
    val message: String,
    val quickFixes: List<QuickFix> = emptyList()
) {
    enum class Severity { ERROR, WARNING, INFO }
}

data class QuickFix(
    val title: String,
    val action: () -> Unit = {}
)

data class SyntaxColorScheme(
    val background: Color,
    val foreground: Color,
    val lineNumber: Color,
    val activeLineBg: Color,
    val selectionBg: Color,
    val keyword: Color,
    val type: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val annotation: Color,
    val punctuation: Color,
    val errorUnderline: Color,
    val warningUnderline: Color
) {
    companion object {
        val Darcula = SyntaxColorScheme(
            background = Color(0xFF1E1F22),
            foreground = Color(0xFFA9B7C6),
            lineNumber = Color(0xFF5A5D63),
            activeLineBg = Color(0xFF26282E),
            selectionBg = Color(0xFF214283),
            keyword = Color(0xFFCC7832), // IntelliJ orange
            type = Color(0xFF4EC9B0),    // IntelliJ cyan
            string = Color(0xFF6A8759),  // IntelliJ green
            number = Color(0xFF6897BB),  // IntelliJ light blue
            comment = Color(0xFF808080), // IntelliJ gray
            annotation = Color(0xFFBBB529), // IntelliJ yellow-olive
            punctuation = Color(0xFFA9B7C6),
            errorUnderline = Color(0xFFF14C4C),
            warningUnderline = Color(0xFFE5C07B)
        )

        val IntelliJLight = SyntaxColorScheme(
            background = Color(0xFFFFFFFF),
            foreground = Color(0xFF000000),
            lineNumber = Color(0xFFA0A0A0),
            activeLineBg = Color(0xFFF2F4F7),
            selectionBg = Color(0xFFA6D2FF),
            keyword = Color(0xFF000080), // Bold navy blue
            type = Color(0xFF007C92),
            string = Color(0xFF008000),  // Green
            number = Color(0xFF0000FF),  // Blue
            comment = Color(0xFF808080), // Gray
            annotation = Color(0xFF808000),
            punctuation = Color(0xFF000000),
            errorUnderline = Color(0xFFDC3545),
            warningUnderline = Color(0xFFFFC107)
        )
    }
}
