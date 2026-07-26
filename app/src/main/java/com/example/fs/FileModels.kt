package com.example.fs

import java.io.File

data class FileTreeItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val extension: String = "",
    val children: List<FileTreeItem> = emptyList(),
    val isExpanded: Boolean = false,
    val isModified: Boolean = false,
    val size: Long = 0L,
    val lastModified: Long = 0L
)

enum class FileType {
    JAVA, KOTLIN, GRADLE, XML, JSON, YAML, TEXT, DIRECTORY, UNKNOWN;

    companion object {
        fun fromExtension(ext: String, name: String): FileType {
            if (name == "build.gradle" || name == "build.gradle.kts" || name == "settings.gradle" || name == "settings.gradle.kts") return GRADLE
            return when (ext.lowercase()) {
                "java" -> JAVA
                "kt", "kts" -> KOTLIN
                "gradle" -> GRADLE
                "xml" -> XML
                "json" -> JSON
                "yaml", "yml" -> YAML
                "txt", "md", "properties" -> TEXT
                else -> UNKNOWN
            }
        }
    }
}
