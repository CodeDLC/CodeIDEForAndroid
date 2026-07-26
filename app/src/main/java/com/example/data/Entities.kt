package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String, // project path as unique ID
    val name: String,
    val path: String,
    val lastOpenedTime: Long = System.currentTimeMillis(),
    val isFabricMod: Boolean = false,
    val minecraftVersion: String = "1.21.4"
)

@Entity(tableName = "open_tabs")
data class OpenTabEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: String,
    val filePath: String,
    val cursorPosition: Int = 0,
    val isPinned: Boolean = false,
    val scrollLine: Int = 0
)

@Entity(tableName = "build_logs")
data class BuildLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val command: String,
    val logText: String,
    val isSuccess: Boolean
)

@Entity(tableName = "editor_settings")
data class EditorSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val themeName: String = "Darcula",
    val fontSizeSp: Float = 13f,
    val fontFamily: String = "JetBrains Mono",
    val showLineNumbers: Boolean = true,
    val showMinimap: Boolean = true,
    val showWhitespace: Boolean = false,
    val tabSize: Int = 4,
    val autoSaveDelayMs: Long = 500L,
    val geminiApiKey: String = ""
)
