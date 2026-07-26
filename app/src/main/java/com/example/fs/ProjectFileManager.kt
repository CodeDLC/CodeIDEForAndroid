package com.example.fs

import android.content.Context
import java.io.File
import java.io.IOException

class ProjectFileManager(private val context: Context) {

    fun getProjectTree(
        rootPath: String,
        expandedPaths: Set<String>,
        showHidden: Boolean = false
    ): FileTreeItem {
        val rootFile = File(rootPath)
        if (!rootFile.exists() || !rootFile.isDirectory) {
            return FileTreeItem(
                name = rootFile.name.ifEmpty { "Project" },
                path = rootPath,
                isDirectory = true
            )
        }
        return buildNode(rootFile, expandedPaths, showHidden)
    }

    private fun buildNode(
        file: File,
        expandedPaths: Set<String>,
        showHidden: Boolean
    ): FileTreeItem {
        val isDir = file.isDirectory
        val path = file.absolutePath
        val isExpanded = expandedPaths.contains(path)

        val children = if (isDir && isExpanded) {
            file.listFiles()
                ?.filter { child ->
                    if (showHidden) true
                    else !child.name.startsWith(".") && child.name != "build" && child.name != ".gradle"
                }
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?.map { buildNode(it, expandedPaths, showHidden) }
                ?: emptyList()
        } else {
            emptyList()
        }

        return FileTreeItem(
            name = file.name,
            path = path,
            isDirectory = isDir,
            extension = file.extension,
            children = children,
            isExpanded = isExpanded,
            size = if (!isDir) file.length() else 0L,
            lastModified = file.lastModified()
        )
    }

    fun readFile(path: String): String {
        val file = File(path)
        if (!file.exists()) return ""
        return try {
            file.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            "// Error reading file: ${e.localizedMessage}"
        }
    }

    fun writeFile(path: String, content: String): Boolean {
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun createNewFile(parentPath: String, fileName: String): File? {
        return try {
            val file = File(parentPath, fileName)
            file.parentFile?.mkdirs()
            if (file.createNewFile()) file else null
        } catch (e: Exception) {
            null
        }
    }

    fun createNewFolder(parentPath: String, folderName: String): File? {
        return try {
            val folder = File(parentPath, folderName)
            if (folder.mkdirs() || folder.exists()) folder else null
        } catch (e: Exception) {
            null
        }
    }

    fun createJavaClass(parentPath: String, className: String, packageName: String): File? {
        val fileName = "$className.java"
        val content = """
            package $packageName;

            public class $className {
                public $className() {
                    // Constructor
                }
            }
        """.trimIndent()
        val file = createNewFile(parentPath, fileName)
        if (file != null) {
            writeFile(file.absolutePath, content)
        }
        return file
    }

    fun createKotlinClass(parentPath: String, className: String, packageName: String): File? {
        val fileName = "$className.kt"
        val content = """
            package $packageName

            class $className {
                init {
                    // Initialization
                }
            }
        """.trimIndent()
        val file = createNewFile(parentPath, fileName)
        if (file != null) {
            writeFile(file.absolutePath, content)
        }
        return file
    }

    fun deletePath(path: String): Boolean {
        val file = File(path)
        return if (file.isDirectory) file.deleteRecursively() else file.delete()
    }

    fun renamePath(oldPath: String, newName: String): Boolean {
        val file = File(oldPath)
        val newFile = File(file.parentFile, newName)
        return file.renameTo(newFile)
    }

    fun searchFiles(rootPath: String, query: String, maxResults: Int = 30): List<FileTreeItem> {
        if (query.isBlank()) return emptyList()
        val rootDir = File(rootPath)
        if (!rootDir.exists()) return emptyList()

        val results = mutableListOf<FileTreeItem>()
        val qLower = query.lowercase().trim()

        fun searchRecursive(dir: File) {
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.name.startsWith(".") || file.name == "build" || file.name == ".gradle") continue
                if (file.isDirectory) {
                    searchRecursive(file)
                } else if (file.name.lowercase().contains(qLower)) {
                    results.add(
                        FileTreeItem(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = false,
                            extension = file.extension,
                            size = file.length(),
                            lastModified = file.lastModified()
                        )
                    )
                    if (results.size >= maxResults) return
                }
            }
        }

        searchRecursive(rootDir)
        return results
    }
}
