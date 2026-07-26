package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.editor.syntax.SyntaxColorScheme
import com.example.ui.IdeViewModel
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: IdeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val settings by viewModel.settings.collectAsState()
                val syntaxColorScheme = remember(settings.themeName) {
                    if (settings.themeName == "IntelliJLight") SyntaxColorScheme.IntelliJLight else SyntaxColorScheme.Darcula
                }

                val isLeftPanelOpen by viewModel.isLeftPanelOpen.collectAsState()
                val isQuickSearchOpen by viewModel.isQuickSearchOpen.collectAsState()

                var showSettingsDialog by remember { mutableStateOf(false) }
                var showNewProjectDialog by remember { mutableStateOf(false) }
                var showNewFileDialog by remember { mutableStateOf<String?>(null) }
                var showNewClassDialog by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    bottomBar = {
                        // IntelliJ Status Bar
                        Surface(
                            color = syntaxColorScheme.activeLineBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Ready  |  UTF-8  |  4 spaces  |  Git: main",
                                    color = syntaxColorScheme.comment,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Idea Mobile IDE v1.0",
                                    color = syntaxColorScheme.keyword,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(syntaxColorScheme.background)
                    ) {
                        // Top Bar
                        IdeTopBar(
                            viewModel = viewModel,
                            colorScheme = syntaxColorScheme,
                            onOpenSettings = { showSettingsDialog = true },
                            onOpenNewProject = { showNewProjectDialog = true }
                        )

                        // Main Workspace Area (Left Panel + Code Editor Area)
                        Row(modifier = Modifier.weight(1f)) {
                            if (isLeftPanelOpen) {
                                ProjectToolWindow(
                                    viewModel = viewModel,
                                    colorScheme = syntaxColorScheme,
                                    onOpenNewFileDialog = { parentPath -> showNewFileDialog = parentPath },
                                    onOpenNewClassDialog = { parentPath, isKt -> showNewClassDialog = parentPath to isKt }
                                )
                                VerticalDivider(color = syntaxColorScheme.activeLineBg)
                            }

                            CodeEditorCanvas(
                                viewModel = viewModel,
                                colorScheme = syntaxColorScheme
                            )
                        }

                        // Bottom Tool Window (Build Log, Terminal, AI Assistant OpenCode)
                        BottomToolWindows(
                            viewModel = viewModel,
                            colorScheme = syntaxColorScheme
                        )
                    }
                }

                // Dialog Modals
                if (isQuickSearchOpen) {
                    QuickSearchDialog(
                        viewModel = viewModel,
                        colorScheme = syntaxColorScheme,
                        onDismiss = { viewModel.closeQuickSearch() }
                    )
                }

                if (showSettingsDialog) {
                    SettingsDialog(
                        settings = settings,
                        colorScheme = syntaxColorScheme,
                        onSave = { viewModel.saveEditorSettings(it) },
                        onDismiss = { showSettingsDialog = false }
                    )
                }

                if (showNewProjectDialog) {
                    NewProjectDialog(
                        onCreateProject = { name, version ->
                            viewModel.createNewProject(name, version)
                        },
                        onDismiss = { showNewProjectDialog = false }
                    )
                }

                showNewFileDialog?.let { parentPath ->
                    NewFileDialog(
                        parentPath = parentPath,
                        onCreateFile = { fileName ->
                            viewModel.fileManager.createNewFile(parentPath, fileName)
                            viewModel.openFile("$parentPath/$fileName")
                        },
                        onDismiss = { showNewFileDialog = null }
                    )
                }

                showNewClassDialog?.let { (parentPath, isKt) ->
                    NewClassDialog(
                        parentPath = parentPath,
                        isKotlin = isKt,
                        onCreateClass = { className, pkg ->
                            if (isKt) {
                                viewModel.fileManager.createKotlinClass(parentPath, className, pkg)
                                viewModel.openFile("$parentPath/$className.kt")
                            } else {
                                viewModel.fileManager.createJavaClass(parentPath, className, pkg)
                                viewModel.openFile("$parentPath/$className.java")
                            }
                        },
                        onDismiss = { showNewClassDialog = null }
                    )
                }
            }
        }
    }
}
