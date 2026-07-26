package com.example.editor.analysis

import com.example.fs.FileType
import java.io.File

data class StructureItem(
    val name: String,
    val kind: Kind, // CLASS, INTERFACE, METHOD, FIELD, PROPERTY
    val line: Int,
    val children: List<StructureItem> = emptyList()
) {
    enum class Kind { CLASS, INTERFACE, METHOD, FIELD, PROPERTY }
}

data class AutocompleteSuggestion(
    val label: String,
    val detail: String,
    val insertText: String,
    val kind: StructureItem.Kind
)

object CodeIndexer {

    fun parseStructure(content: String, fileType: FileType): List<StructureItem> {
        val items = mutableListOf<StructureItem>()
        if (content.isBlank()) return items

        val lines = content.lines()

        lines.forEachIndexed { lineIdx, line ->
            val trimmed = line.trim()

            // Class / Interface declaration
            if (trimmed.contains("class ") || trimmed.contains("interface ") || trimmed.contains("object ")) {
                val name = trimmed.substringAfter("class ")
                    .substringAfter("interface ")
                    .substringAfter("object ")
                    .substringBefore(" ")
                    .substringBefore("(")
                    .substringBefore(":")
                    .substringBefore("{")
                if (name.isNotEmpty()) {
                    val kind = if (trimmed.contains("interface ")) StructureItem.Kind.INTERFACE else StructureItem.Kind.CLASS
                    items.add(StructureItem(name = name, kind = kind, line = lineIdx + 1))
                }
            }
            // Method declaration (fun / void / public / private / protected)
            else if (trimmed.contains("fun ") || (trimmed.contains("void ") && trimmed.contains("(")) || (trimmed.contains("public ") && trimmed.contains("("))) {
                val methodName = trimmed.substringAfter("fun ")
                    .substringAfter("void ")
                    .substringBefore("(")
                    .substringAfterLast(" ")
                if (methodName.isNotEmpty() && !methodName.contains("class")) {
                    items.add(StructureItem(name = "$methodName()", kind = StructureItem.Kind.METHOD, line = lineIdx + 1))
                }
            }
            // Field / Property
            else if (trimmed.startsWith("val ") || trimmed.startsWith("var ") || (trimmed.contains("private ") && !trimmed.contains("("))) {
                val fieldName = trimmed.substringAfter("val ")
                    .substringAfter("var ")
                    .substringAfter("private ")
                    .substringBefore(":")
                    .substringBefore("=")
                    .substringBefore(";")
                    .trim()
                if (fieldName.isNotEmpty()) {
                    items.add(StructureItem(name = fieldName, kind = StructureItem.Kind.FIELD, line = lineIdx + 1))
                }
            }
        }

        return items
    }

    fun getAutocompleteSuggestions(
        content: String,
        fileType: FileType,
        prefix: String
    ): List<AutocompleteSuggestion> {
        if (prefix.isBlank()) return emptyList()

        val pLower = prefix.lowercase()
        val suggestions = mutableSetOf<AutocompleteSuggestion>()

        // 1. Common Keywords
        val keywords = when (fileType) {
            FileType.JAVA -> listOf("public", "private", "protected", "class", "interface", "void", "return", "new", "import", "package", "extends", "implements", "if", "for", "while", "try", "catch")
            FileType.KOTLIN -> listOf("fun", "val", "var", "class", "object", "interface", "data class", "sealed class", "when", "return", "import", "package", "override", "private", "public")
            else -> emptyList()
        }

        keywords.filter { it.startsWith(pLower) }.forEach {
            suggestions.add(
                AutocompleteSuggestion(
                    label = it,
                    detail = "Keyword",
                    insertText = it,
                    kind = StructureItem.Kind.PROPERTY
                )
            )
        }

        // 2. Tokens from current file
        val tokens = content.split(Regex("[^a-zA-Z0-9_]"))
            .filter { it.length > 2 && it.lowercase().startsWith(pLower) && it != prefix }
            .distinct()

        tokens.take(15).forEach { token ->
            val kind = if (token[0].isUpperCase()) StructureItem.Kind.CLASS else StructureItem.Kind.FIELD
            suggestions.add(
                AutocompleteSuggestion(
                    label = token,
                    detail = if (token[0].isUpperCase()) "Class/Type" else "Symbol",
                    insertText = token,
                    kind = kind
                )
            )
        }

        return suggestions.toList()
    }
}
