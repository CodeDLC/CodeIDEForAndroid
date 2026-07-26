package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.DiffLine
import com.example.editor.syntax.SyntaxColorScheme
import com.example.ui.BottomToolWindow
import com.example.ui.IdeViewModel

@Composable
fun BottomToolWindows(
    viewModel: IdeViewModel,
    colorScheme: SyntaxColorScheme
) {
    val bottomWindow by viewModel.bottomToolWindow.collectAsState()
    val buildLogs by viewModel.buildLogs.collectAsState()
    val isBuilding by viewModel.isBuilding.collectAsState()
    val terminalSessions by viewModel.terminalSessions.collectAsState()
    val activeTerminalIdx by viewModel.activeTerminalIdx.collectAsState()
    val aiDiffResult by viewModel.aiDiffResult.collectAsState()
    val isAiGenerating by viewModel.isAiGenerating.collectAsState()

    var terminalInput by remember { mutableStateOf("") }
    var aiPromptInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.background)
    ) {
        // Bottom Tool Bar Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(colorScheme.activeLineBg)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BottomDockButton(
                    text = "Build Output",
                    icon = Icons.Default.Build,
                    isSelected = bottomWindow == BottomToolWindow.BUILD,
                    badgeCount = if (isBuilding) -1 else null,
                    colorScheme = colorScheme,
                    onClick = { viewModel.setBottomToolWindow(BottomToolWindow.BUILD) }
                )
                BottomDockButton(
                    text = "Terminal",
                    icon = Icons.Default.Terminal,
                    isSelected = bottomWindow == BottomToolWindow.TERMINAL,
                    colorScheme = colorScheme,
                    onClick = { viewModel.setBottomToolWindow(BottomToolWindow.TERMINAL) }
                )
                BottomDockButton(
                    text = "AI Assistant (OpenCode)",
                    icon = Icons.Default.AutoAwesome,
                    isSelected = bottomWindow == BottomToolWindow.AI_ASSISTANT,
                    colorScheme = colorScheme,
                    onClick = { viewModel.setBottomToolWindow(BottomToolWindow.AI_ASSISTANT) }
                )
            }

            if (bottomWindow != BottomToolWindow.NONE) {
                IconButton(
                    onClick = { viewModel.setBottomToolWindow(BottomToolWindow.NONE) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Bottom Window",
                        tint = colorScheme.comment,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Active Bottom Panel Content
        if (bottomWindow != BottomToolWindow.NONE) {
            HorizontalDivider(color = colorScheme.activeLineBg)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(colorScheme.background)
            ) {
                when (bottomWindow) {
                    BottomToolWindow.BUILD -> {
                        BuildOutputPanel(
                            buildLogs = buildLogs,
                            isBuilding = isBuilding,
                            colorScheme = colorScheme,
                            onRunTask = { viewModel.runBuildTask(it) },
                            onOpenErrorLink = { link ->
                                viewModel.openFile(link.filePath)
                            }
                        )
                    }
                    BottomToolWindow.TERMINAL -> {
                        val session = terminalSessions.getOrNull(activeTerminalIdx)
                        TerminalPanel(
                            session = session,
                            terminalInput = terminalInput,
                            onInputChange = { terminalInput = it },
                            onExecute = {
                                viewModel.executeTerminalCommand(terminalInput)
                                terminalInput = ""
                            },
                            colorScheme = colorScheme
                        )
                    }
                    BottomToolWindow.AI_ASSISTANT -> {
                        AiAssistantPanel(
                            aiPromptInput = aiPromptInput,
                            onPromptChange = { aiPromptInput = it },
                            isAiGenerating = isAiGenerating,
                            aiDiffResult = aiDiffResult,
                            onSendPrompt = {
                                viewModel.requestAiFixOrFeature(aiPromptInput)
                            },
                            onApplyDiff = { viewModel.applyAiDiff() },
                            onRejectDiff = { viewModel.rejectAiDiff() },
                            colorScheme = colorScheme
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun BottomDockButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    badgeCount: Int? = null,
    colorScheme: SyntaxColorScheme,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) colorScheme.background else Color.Transparent,
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) colorScheme.keyword else colorScheme.comment,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = if (isSelected) colorScheme.foreground else colorScheme.comment,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (badgeCount == -1) {
                Spacer(modifier = Modifier.width(4.dp))
                CircularProgressIndicator(
                    color = colorScheme.keyword,
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 1.5.dp
                )
            }
        }
    }
}

@Composable
private fun BuildOutputPanel(
    buildLogs: List<com.example.build.BuildOutputLine>,
    isBuilding: Boolean,
    colorScheme: SyntaxColorScheme,
    onRunTask: (String) -> Unit,
    onOpenErrorLink: (com.example.build.BuildErrorLink) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        // Quick Action Task Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Text("Run Task:", color = colorScheme.comment, fontSize = 11.sp)
            listOf("build", "clean", "tasks", "runClient").forEach { task ->
                Surface(
                    color = colorScheme.activeLineBg,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.clickable { onRunTask(task) }
                ) {
                    Text(
                        text = task,
                        color = colorScheme.foreground,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = colorScheme.activeLineBg)

        LazyColumn(modifier = Modifier.weight(1f).padding(top = 4.dp)) {
            items(buildLogs) { log ->
                val link = log.errorLink
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                        .then(if (link != null) Modifier.clickable { onOpenErrorLink(link) } else Modifier)
                ) {
                    Text(
                        text = log.text,
                        color = if (log.isError) colorScheme.errorUnderline
                        else if (log.isWarning) colorScheme.warningUnderline
                        else colorScheme.foreground,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalPanel(
    session: com.example.terminal.TerminalSession?,
    terminalInput: String,
    onInputChange: (String) -> Unit,
    onExecute: () -> Unit,
    colorScheme: SyntaxColorScheme
) {
    val outputLines by (session?.outputLines?.collectAsState() ?: remember { mutableStateOf(emptyList()) })

    Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(outputLines) { line ->
                Text(
                    text = line.text,
                    color = if (line.isCommand) colorScheme.keyword else colorScheme.foreground,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
            Text("$ ", color = colorScheme.keyword, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            OutlinedTextField(
                value = terminalInput,
                onValueChange = onInputChange,
                placeholder = { Text("Enter terminal command...", fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onExecute() }),
                modifier = Modifier.weight(1f).height(40.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onExecute) {
                Icon(Icons.Default.Send, contentDescription = "Run", tint = colorScheme.keyword)
            }
        }
    }
}

@Composable
private fun AiAssistantPanel(
    aiPromptInput: String,
    onPromptChange: (String) -> Unit,
    isAiGenerating: Boolean,
    aiDiffResult: com.example.ai.AiDiffResult?,
    onSendPrompt: () -> Unit,
    onApplyDiff: () -> Unit,
    onRejectDiff: () -> Unit,
    colorScheme: SyntaxColorScheme
) {
    Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        if (aiDiffResult != null) {
            // Diff Preview Card
            Surface(
                color = colorScheme.activeLineBg,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "OpenCode AI Proposed Diff",
                            color = colorScheme.keyword,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = onApplyDiff,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71)),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Apply Diff", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = onRejectDiff,
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Reject", fontSize = 10.sp)
                            }
                        }
                    }

                    Text(text = aiDiffResult.explanation, color = colorScheme.foreground, fontSize = 11.sp)

                    LazyColumn(modifier = Modifier.weight(1f).padding(top = 4.dp)) {
                        items(aiDiffResult.diffLines) { diff ->
                            val (bgColor, textColor) = when (diff.type) {
                                DiffLine.Type.ADDED -> Color(0xFF2ECC71).copy(alpha = 0.2f) to Color(0xFF2ECC71)
                                DiffLine.Type.DELETED -> Color(0xFFE74C3C).copy(alpha = 0.2f) to Color(0xFFE74C3C)
                                DiffLine.Type.MODIFIED -> Color(0xFFF39C12).copy(alpha = 0.2f) to Color(0xFFF39C12)
                                DiffLine.Type.UNCHANGED -> Color.Transparent to colorScheme.foreground
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bgColor)
                                    .padding(vertical = 1.dp)
                            ) {
                                Text(
                                    text = diff.proposedLine ?: diff.originalLine ?: "",
                                    color = textColor,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Prompt Input Area
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = aiPromptInput,
                    onValueChange = onPromptChange,
                    placeholder = { Text("Ask OpenCode AI (e.g. 'Fix syntax error', 'Add Fabric item')") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onSendPrompt,
                    enabled = !isAiGenerating && aiPromptInput.isNotBlank()
                ) {
                    if (isAiGenerating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Ask AI")
                    }
                }
            }
        }
    }
}
