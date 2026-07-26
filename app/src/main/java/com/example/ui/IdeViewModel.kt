package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiDiffResult
import com.example.ai.OpenCodeAiAssistant
import com.example.build.BuildOutputLine
import com.example.build.GradleRunner
import com.example.data.*
import com.example.editor.analysis.CodeAnalyzer
import com.example.editor.analysis.CodeIndexer
import com.example.editor.analysis.StructureItem
import com.example.editor.syntax.CodeProblem
import com.example.editor.syntax.SyntaxColorScheme
import com.example.fs.FileTreeItem
import com.example.fs.FileType
import com.example.fs.ProjectFileManager
import com.example.templates.FabricModTemplateGenerator
import com.example.terminal.TerminalSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

enum class LeftToolWindow { PROJECT, STRUCTURE, GIT, PROBLEMS }
enum class BottomToolWindow { NONE, BUILD, TERMINAL, PROBLEMS, AI_ASSISTANT }

class IdeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = IdeRepository(db)
    val fileManager = ProjectFileManager(application)
    val gradleRunner = GradleRunner(application)
    val aiAssistant = OpenCodeAiAssistant()

    // Recent Projects
    val recentProjects: StateFlow<List<ProjectEntity>> = repository.recentProjects
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Editor Settings
    val settings: StateFlow<EditorSettingsEntity> = repository.editorSettings
        .map { it ?: EditorSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EditorSettingsEntity())

    // Current Project
    private val _currentProject = MutableStateFlow<ProjectEntity?>(null)
    val currentProject: StateFlow<ProjectEntity?> = _currentProject.asStateFlow()

    // File Tree State
    private val _expandedPaths = MutableStateFlow<Set<String>>(emptySet())
    val expandedPaths: StateFlow<Set<String>> = _expandedPaths.asStateFlow()

    private val _showHiddenFiles = MutableStateFlow(false)
    val showHiddenFiles: StateFlow<Boolean> = _showHiddenFiles.asStateFlow()

    val fileTree: StateFlow<FileTreeItem?> = combine(
        _currentProject,
        _expandedPaths,
        _showHiddenFiles
    ) { project, expanded, showHidden ->
        if (project == null) null
        else fileManager.getProjectTree(project.path, expanded, showHidden)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Open Tabs & Active Tab
    private val _openTabs = MutableStateFlow<List<OpenTabEntity>>(emptyList())
    val openTabs: StateFlow<List<OpenTabEntity>> = _openTabs.asStateFlow()

    private val _activeTabPath = MutableStateFlow<String?>(null)
    val activeTabPath: StateFlow<String?> = _activeTabPath.asStateFlow()

    // Editor Content
    private val _activeFileContent = MutableStateFlow("")
    val activeFileContent: StateFlow<String> = _activeFileContent.asStateFlow()

    private val _isContentModified = MutableStateFlow(false)
    val isContentModified: StateFlow<Boolean> = _isContentModified.asStateFlow()

    // Code Problems (Errors / Warnings)
    private val _codeProblems = MutableStateFlow<List<CodeProblem>>(emptyList())
    val codeProblems: StateFlow<List<CodeProblem>> = _codeProblems.asStateFlow()

    // File Structure Tree
    private val _fileStructure = MutableStateFlow<List<StructureItem>>(emptyList())
    val fileStructure: StateFlow<List<StructureItem>> = _fileStructure.asStateFlow()

    // Tool Windows
    private val _leftToolWindow = MutableStateFlow(LeftToolWindow.PROJECT)
    val leftToolWindow: StateFlow<LeftToolWindow> = _leftToolWindow.asStateFlow()

    private val _bottomToolWindow = MutableStateFlow(BottomToolWindow.NONE)
    val bottomToolWindow: StateFlow<BottomToolWindow> = _bottomToolWindow.asStateFlow()

    private val _isLeftPanelOpen = MutableStateFlow(true)
    val isLeftPanelOpen: StateFlow<Boolean> = _isLeftPanelOpen.asStateFlow()

    // Build Logs
    private val _buildLogs = MutableStateFlow<List<BuildOutputLine>>(emptyList())
    val buildLogs: StateFlow<List<BuildOutputLine>> = _buildLogs.asStateFlow()

    private val _isBuilding = MutableStateFlow(false)
    val isBuilding: StateFlow<Boolean> = _isBuilding.asStateFlow()

    // Terminal Sessions
    private val _terminalSessions = MutableStateFlow(listOf(TerminalSession(1, "Local Terminal")))
    val terminalSessions: StateFlow<List<TerminalSession>> = _terminalSessions.asStateFlow()

    private val _activeTerminalIdx = MutableStateFlow(0)
    val activeTerminalIdx: StateFlow<Int> = _activeTerminalIdx.asStateFlow()

    // Quick Search Dialog (Double Shift)
    private val _isQuickSearchOpen = MutableStateFlow(false)
    val isQuickSearchOpen: StateFlow<Boolean> = _isQuickSearchOpen.asStateFlow()

    private val _quickSearchQuery = MutableStateFlow("")
    val quickSearchQuery: StateFlow<String> = _quickSearchQuery.asStateFlow()

    private val _quickSearchResults = MutableStateFlow<List<FileTreeItem>>(emptyList())
    val quickSearchResults: StateFlow<List<FileTreeItem>> = _quickSearchResults.asStateFlow()

    // AI Assistant Diff
    private val _aiDiffResult = MutableStateFlow<AiDiffResult?>(null)
    val aiDiffResult: StateFlow<AiDiffResult?> = _aiDiffResult.asStateFlow()

    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating.asStateFlow()

    // Auto-save Job
    private var autoSaveJob: Job? = null

    init {
        // Load default sample project on launch if no project open
        viewModelScope.launch {
            val sampleDir = FabricModTemplateGenerator.ensureSampleProject(application)
            val project = ProjectEntity(
                id = sampleDir.absolutePath,
                name = sampleDir.name,
                path = sampleDir.absolutePath,
                isFabricMod = true
            )
            openProject(project)
        }
    }

    fun openProject(project: ProjectEntity) {
        viewModelScope.launch {
            _currentProject.value = project
            repository.openProject(project)

            // Auto expand root folder
            _expandedPaths.value = setOf(project.path)

            // Open main entry file automatically if available
            val mainJava = File(project.path, "src/main/java/com/example/fabricmod/ExampleMod.java")
            if (mainJava.exists()) {
                openFile(mainJava.absolutePath)
            } else {
                val files = File(project.path).walkTopDown().filter { it.isFile }.toList()
                if (files.isNotEmpty()) {
                    openFile(files.first().absolutePath)
                }
            }
        }
    }

    fun togglePathExpanded(path: String) {
        val current = _expandedPaths.value.toMutableSet()
        if (current.contains(path)) current.remove(path) else current.add(path)
        _expandedPaths.value = current
    }

    fun toggleShowHiddenFiles() {
        _showHiddenFiles.value = !_showHiddenFiles.value
    }

    fun openFile(filePath: String) {
        val project = _currentProject.value ?: return
        val content = fileManager.readFile(filePath)

        _activeTabPath.value = filePath
        _activeFileContent.value = content
        _isContentModified.value = false

        val currentTabs = _openTabs.value.toMutableList()
        if (currentTabs.none { it.filePath == filePath }) {
            val newTab = OpenTabEntity(projectId = project.id, filePath = filePath)
            currentTabs.add(newTab)
            _openTabs.value = currentTabs
            viewModelScope.launch { repository.addOpenTab(newTab) }
        }

        analyzeActiveFile(content, filePath)
    }

    fun closeTab(filePath: String) {
        val project = _currentProject.value ?: return
        val currentTabs = _openTabs.value.toMutableList()
        currentTabs.removeAll { it.filePath == filePath }
        _openTabs.value = currentTabs

        viewModelScope.launch { repository.closeTab(project.id, filePath) }

        if (_activeTabPath.value == filePath) {
            val nextTab = currentTabs.lastOrNull()
            if (nextTab != null) {
                openFile(nextTab.filePath)
            } else {
                _activeTabPath.value = null
                _activeFileContent.value = ""
                _codeProblems.value = emptyList()
                _fileStructure.value = emptyList()
            }
        }
    }

    fun updateActiveContent(newContent: String) {
        _activeFileContent.value = newContent
        _isContentModified.value = true

        val currentPath = _activeTabPath.value ?: return
        analyzeActiveFile(newContent, currentPath)

        // Debounced Auto-Save (500ms)
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(settings.value.autoSaveDelayMs)
            fileManager.writeFile(currentPath, newContent)
            _isContentModified.value = false
        }
    }

    private fun analyzeActiveFile(content: String, filePath: String) {
        val ext = File(filePath).extension
        val fileType = FileType.fromExtension(ext, File(filePath).name)

        _codeProblems.value = CodeAnalyzer.analyzeCode(content, fileType, filePath)
        _fileStructure.value = CodeIndexer.parseStructure(content, fileType)
    }

    fun applyQuickFix(problem: CodeProblem, fixIndex: Int) {
        val currentContent = _activeFileContent.value
        val lines = currentContent.lines().toMutableList()
        if (problem.line in 1..lines.size) {
            val idx = problem.line - 1
            when {
                problem.message.contains("Java statement must end with ';'") -> {
                    lines[idx] = lines[idx] + ";"
                }
                problem.message.contains("Unused import") -> {
                    lines.removeAt(idx)
                }
                problem.message.contains("Insert closing") -> {
                    lines[idx] = lines[idx] + problem.message.takeLast(3).replace("'", "")
                }
            }
            updateActiveContent(lines.joinToString("\n"))
        }
    }

    fun runBuildTask(taskName: String = "build") {
        val project = _currentProject.value ?: return
        _isBuilding.value = true
        _bottomToolWindow.value = BottomToolWindow.BUILD
        _buildLogs.value = emptyList()

        viewModelScope.launch {
            val success = gradleRunner.runGradleTask(
                projectDir = project.path,
                taskName = taskName,
                onOutput = { line ->
                    val logs = _buildLogs.value.toMutableList()
                    logs.add(line)
                    _buildLogs.value = logs
                }
            )
            _isBuilding.value = false
            repository.addBuildLog(
                BuildLogEntity(
                    projectId = project.id,
                    command = "gradle $taskName",
                    logText = _buildLogs.value.joinToString("\n") { it.text },
                    isSuccess = success
                )
            )
        }
    }

    fun executeTerminalCommand(cmd: String) {
        val project = _currentProject.value ?: return
        val session = _terminalSessions.value.getOrNull(_activeTerminalIdx.value) ?: return
        session.executeCommand(cmd, project.path)
    }

    fun requestAiFixOrFeature(userPrompt: String) {
        val currentPath = _activeTabPath.value ?: return
        val currentContent = _activeFileContent.value
        _isAiGenerating.value = true
        _bottomToolWindow.value = BottomToolWindow.AI_ASSISTANT

        viewModelScope.launch {
            val result = aiAssistant.generateCodeFixOrFeature(
                userPrompt = userPrompt,
                selectedCode = "",
                fullFileCode = currentContent,
                filePath = currentPath,
                apiKeyOverride = settings.value.geminiApiKey
            )
            _aiDiffResult.value = result
            _isAiGenerating.value = false
        }
    }

    fun applyAiDiff() {
        val diff = _aiDiffResult.value ?: return
        updateActiveContent(diff.proposedCode)
        _aiDiffResult.value = null
    }

    fun rejectAiDiff() {
        _aiDiffResult.value = null
    }

    fun setLeftToolWindow(window: LeftToolWindow) {
        if (_leftToolWindow.value == window && _isLeftPanelOpen.value) {
            _isLeftPanelOpen.value = false
        } else {
            _leftToolWindow.value = window
            _isLeftPanelOpen.value = true
        }
    }

    fun setBottomToolWindow(window: BottomToolWindow) {
        _bottomToolWindow.value = if (_bottomToolWindow.value == window) BottomToolWindow.NONE else window
    }

    fun openQuickSearch() {
        _isQuickSearchOpen.value = true
        _quickSearchQuery.value = ""
        _quickSearchResults.value = emptyList()
    }

    fun closeQuickSearch() {
        _isQuickSearchOpen.value = false
    }

    fun updateQuickSearchQuery(query: String) {
        _quickSearchQuery.value = query
        val project = _currentProject.value ?: return
        _quickSearchResults.value = fileManager.searchFiles(project.path, query)
    }

    fun saveEditorSettings(newSettings: EditorSettingsEntity) {
        viewModelScope.launch { repository.saveSettings(newSettings) }
    }

    fun createNewProject(name: String, minecraftVersion: String) {
        viewModelScope.launch {
            val parentDir = File(getApplication<Application>().filesDir, "projects")
            val newProjectDir = FabricModTemplateGenerator.generateFabricModProject(
                parentDir = parentDir,
                modId = name.lowercase().replace(" ", "_"),
                modName = name,
                packageName = "com.example.${name.lowercase().replace(" ", "")}",
                minecraftVersion = minecraftVersion
            )
            val projectEntity = ProjectEntity(
                id = newProjectDir.absolutePath,
                name = newProjectDir.name,
                path = newProjectDir.absolutePath,
                isFabricMod = true,
                minecraftVersion = minecraftVersion
            )
            openProject(projectEntity)
        }
    }
}
