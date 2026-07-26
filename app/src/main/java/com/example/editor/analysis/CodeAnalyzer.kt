package com.example.editor.analysis

import com.example.editor.syntax.CodeProblem
import com.example.editor.syntax.CodeProblem.Severity
import com.example.editor.syntax.QuickFix
import com.example.fs.FileType
import java.util.Stack

object CodeAnalyzer {

    fun analyzeCode(
        content: String,
        fileType: FileType,
        filePath: String
    ): List<CodeProblem> {
        val problems = mutableListOf<CodeProblem>()
        if (content.isBlank()) return problems

        val lines = content.lines()

        // 1. Bracket & Quote Matching Check
        checkBracketMatching(lines, problems)

        // 2. Language-Specific Checks
        when (fileType) {
            FileType.JAVA -> analyzeJavaCode(lines, content, problems)
            FileType.KOTLIN -> analyzeKotlinCode(lines, content, problems)
            FileType.XML -> analyzeXmlCode(lines, problems)
            FileType.JSON -> analyzeJsonCode(content, problems)
            else -> {}
        }

        return problems
    }

    private fun checkBracketMatching(lines: List<String>, problems: MutableList<CodeProblem>) {
        val stack = Stack<Triple<Char, Int, Int>>() // char, lineIndex, colIndex

        lines.forEachIndexed { lineIdx, line ->
            var inString = false
            var stringChar = ' '
            var isComment = false

            for (colIdx in line.indices) {
                val c = line[colIdx]

                if (isComment) break

                // Check for comment start
                if (!inString && c == '/' && colIdx + 1 < line.length && line[colIdx + 1] == '/') {
                    isComment = true
                    break
                }

                // Check string literal bounds
                if ((c == '"' || c == '\'') && (colIdx == 0 || line[colIdx - 1] != '\\')) {
                    if (!inString) {
                        inString = true
                        stringChar = c
                    } else if (stringChar == c) {
                        inString = false
                    }
                    continue
                }

                if (inString) continue

                when (c) {
                    '(', '{', '[' -> stack.push(Triple(c, lineIdx + 1, colIdx + 1))
                    ')', '}', ']' -> {
                        val expected = when (c) {
                            ')' -> '('
                            '}' -> '{'
                            ']' -> '['
                            else -> ' '
                        }
                        if (stack.isEmpty() || stack.peek().first != expected) {
                            problems.add(
                                CodeProblem(
                                    line = lineIdx + 1,
                                    column = colIdx + 1,
                                    length = 1,
                                    severity = Severity.ERROR,
                                    message = "Unmatched closing bracket '$c'",
                                    quickFixes = listOf(QuickFix("Remove extra '$c'"))
                                )
                            )
                        } else {
                            stack.pop()
                        }
                    }
                }
            }

            if (inString) {
                problems.add(
                    CodeProblem(
                        line = lineIdx + 1,
                        column = line.length,
                        length = 1,
                        severity = Severity.ERROR,
                        message = "Unclosed string literal",
                        quickFixes = listOf(QuickFix("Close quote with $stringChar"))
                    )
                )
            }
        }

        // Remaining unmatched opening brackets
        while (!stack.isEmpty()) {
            val (unmatchedChar, lineNum, colNum) = stack.pop()
            val closing = when (unmatchedChar) {
                '(' -> ')'
                '{' -> '}'
                '[' -> ']'
                else -> ' '
            }
            problems.add(
                CodeProblem(
                    line = lineNum,
                    column = colNum,
                    length = 1,
                    severity = Severity.ERROR,
                    message = "Unclosed bracket '$unmatchedChar'",
                    quickFixes = listOf(QuickFix("Insert closing '$closing'"))
                )
            )
        }
    }

    private fun analyzeJavaCode(lines: List<String>, fullContent: String, problems: MutableList<CodeProblem>) {
        lines.forEachIndexed { lineIdx, line ->
            val trimmed = line.trim()

            // Missing semicolon check in Java
            if (trimmed.isNotEmpty() &&
                !trimmed.startsWith("//") &&
                !trimmed.startsWith("/*") &&
                !trimmed.startsWith("*") &&
                !trimmed.startsWith("@") &&
                !trimmed.endsWith("{") &&
                !trimmed.endsWith("}") &&
                !trimmed.endsWith(";") &&
                !trimmed.endsWith(":") &&
                !trimmed.startsWith("package") &&
                !trimmed.startsWith("import") &&
                (trimmed.contains("=") || trimmed.contains("return ") || trimmed.contains("System.") || trimmed.contains("LOGGER."))) {

                problems.add(
                    CodeProblem(
                        line = lineIdx + 1,
                        column = line.length,
                        length = 1,
                        severity = Severity.ERROR,
                        message = "Java statement must end with ';'",
                        quickFixes = listOf(QuickFix("Add ';' to end of statement"))
                    )
                )
            }

            // Unused import detection
            if (trimmed.startsWith("import ")) {
                val importClass = trimmed.removePrefix("import ").removeSuffix(";").trim()
                val className = importClass.substringAfterLast('.')
                if (className.isNotEmpty() && className != "*") {
                    val count = fullContent.split(Regex("\\b$className\\b")).size - 1
                    if (count <= 1) { // Only appears in the import line itself
                        problems.add(
                            CodeProblem(
                                line = lineIdx + 1,
                                column = 1,
                                length = trimmed.length,
                                severity = Severity.WARNING,
                                message = "Unused import '$importClass'",
                                quickFixes = listOf(QuickFix("Remove unused import"))
                            )
                        )
                    }
                }
            }
        }
    }

    private fun analyzeKotlinCode(lines: List<String>, fullContent: String, problems: MutableList<CodeProblem>) {
        lines.forEachIndexed { lineIdx, line ->
            val trimmed = line.trim()

            // Deprecated API warning
            if (trimmed.contains("@Deprecated") || trimmed.contains("@java.lang.Deprecated")) {
                problems.add(
                    CodeProblem(
                        line = lineIdx + 1,
                        column = 1,
                        length = trimmed.length,
                        severity = Severity.WARNING,
                        message = "Usage of deprecated element",
                        quickFixes = listOf(QuickFix("Replace with recommended non-deprecated alternative"))
                    )
                )
            }

            // Unused import check
            if (trimmed.startsWith("import ")) {
                val importClass = trimmed.removePrefix("import ").trim()
                val className = importClass.substringAfterLast('.')
                if (className.isNotEmpty() && className != "*") {
                    val count = fullContent.split(Regex("\\b$className\\b")).size - 1
                    if (count <= 1) {
                        problems.add(
                            CodeProblem(
                                line = lineIdx + 1,
                                column = 1,
                                length = trimmed.length,
                                severity = Severity.WARNING,
                                message = "Unused import '$importClass'",
                                quickFixes = listOf(QuickFix("Remove unused import"))
                            )
                        )
                    }
                }
            }
        }
    }

    private fun analyzeXmlCode(lines: List<String>, problems: MutableList<CodeProblem>) {
        val tagStack = Stack<Pair<String, Int>>() // tagName, line

        lines.forEachIndexed { lineIdx, line ->
            val tagRegex = Regex("</?([a-zA-Z0-9_:-]+)[^>]*>")
            for (match in tagRegex.findAll(line)) {
                val tagText = match.value
                val tagName = match.groupValues[1]

                if (tagText.endsWith("/>") || tagText.startsWith("<?")) {
                    continue // Self-closing or XML declaration
                }

                if (tagText.startsWith("</")) { // Closing tag
                    if (tagStack.isEmpty() || tagStack.peek().first != tagName) {
                        problems.add(
                            CodeProblem(
                                line = lineIdx + 1,
                                column = match.range.first + 1,
                                length = tagText.length,
                                severity = Severity.ERROR,
                                message = "Mismatched XML closing tag '</$tagName>'",
                                quickFixes = listOf(QuickFix("Fix XML tag name"))
                            )
                        )
                    } else {
                        tagStack.pop()
                    }
                } else { // Opening tag
                    tagStack.push(Pair(tagName, lineIdx + 1))
                }
            }
        }

        while (!tagStack.isEmpty()) {
            val (tagName, lineNum) = tagStack.pop()
            problems.add(
                CodeProblem(
                    line = lineNum,
                    column = 1,
                    length = tagName.length,
                    severity = Severity.ERROR,
                    message = "Unclosed XML tag '<$tagName>'",
                    quickFixes = listOf(QuickFix("Add '</$tagName>'"))
                )
            )
        }
    }

    private fun analyzeJsonCode(content: String, problems: MutableList<CodeProblem>) {
        val trimmed = content.trim()
        if (trimmed.isNotEmpty() && !trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            problems.add(
                CodeProblem(
                    line = 1,
                    column = 1,
                    length = 1,
                    severity = Severity.ERROR,
                    message = "JSON document must start with '{' or '['",
                    quickFixes = listOf(QuickFix("Wrap with '{ ... }'"))
                )
            )
        }
    }
}
