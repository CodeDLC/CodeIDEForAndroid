package com.example.terminal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class TerminalLine(
    val text: String,
    val isCommand: Boolean = false,
    val isError: Boolean = false
)

class TerminalSession(val id: Int, val name: String) {
    private val _outputLines = MutableStateFlow<List<TerminalLine>>(
        listOf(
            TerminalLine("Idea Mobile Embedded Terminal Session #$id initialized."),
            TerminalLine("Type 'gradle build', 'gradle tasks', 'git status', or 'opencode' to run commands.")
        )
    )
    val outputLines: StateFlow<List<TerminalLine>> = _outputLines

    fun executeCommand(command: String, currentProjectDir: String) {
        val currentList = _outputLines.value.toMutableList()
        currentList.add(TerminalLine("$ $command", isCommand = true))

        val lowerCmd = command.trim().lowercase()

        when {
            lowerCmd == "clear" -> {
                _outputLines.value = emptyList()
                return
            }
            lowerCmd == "pwd" -> {
                currentList.add(TerminalLine(currentProjectDir))
            }
            lowerCmd == "ls" -> {
                currentList.add(TerminalLine("build.gradle.kts  settings.gradle.kts  gradle.properties  src/"))
            }
            lowerCmd.startsWith("git ") -> {
                currentList.add(TerminalLine("On branch main\nYour branch is up to date with 'origin/main'.\n\nNothing to commit, working tree clean"))
            }
            lowerCmd == "opencode" || lowerCmd.startsWith("opencode ") -> {
                currentList.add(TerminalLine("[OpenCode AI Agent] Active. Send prompts from the AI Assistant panel."))
            }
            else -> {
                currentList.add(TerminalLine("Executed: $command (exit code 0)"))
            }
        }

        _outputLines.value = currentList
    }

    fun clear() {
        _outputLines.value = emptyList()
    }
}
