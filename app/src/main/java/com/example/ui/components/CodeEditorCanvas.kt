package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OpenTabEntity
import com.example.editor.syntax.CodeProblem
import com.example.editor.syntax.SyntaxColorScheme
import com.example.editor.syntax.SyntaxHighlighter
import com.example.fs.FileType
import com.example.ui.IdeViewModel
import java.io.File

@Composable
fun CodeEditorCanvas(
    viewModel: IdeViewModel,
    colorScheme: SyntaxColorScheme
) {
    val openTabs by viewModel.openTabs.collectAsState()
    val activeTabPath by viewModel.activeTabPath.collectAsState()
    val content by viewModel.activeFileContent.collectAsState()
    val isModified by viewModel.isContentModified.collectAsState()
    val problems by viewModel.codeProblems.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val lines = remember(content) { content.lines() }
    val activeFile = activeTabPath?.let { File(it) }
    val fileType = remember(activeTabPath) {
        if (activeFile != null) FileType.fromExtension(activeFile.extension, activeFile.name) else FileType.TEXT
    }

    val lazyListState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // 1. Open Tabs Header
        if (openTabs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(colorScheme.activeLineBg)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                openTabs.forEach { tab ->
                    val isSelected = tab.filePath == activeTabPath
                    val file = File(tab.filePath)

                    Surface(
                        color = if (isSelected) colorScheme.background else Color.Transparent,
                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .clickable { viewModel.openFile(tab.filePath) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            FileIcon(
                                fileType = FileType.fromExtension(file.extension, file.name),
                                colorScheme = colorScheme
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = file.name + if (isSelected && isModified) " *" else "",
                                color = if (isSelected) colorScheme.foreground else colorScheme.comment,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close tab",
                                tint = colorScheme.comment,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { viewModel.closeTab(tab.filePath) }
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = colorScheme.activeLineBg)
        }

        // 2. Breadcrumbs Bar
        if (activeFile != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.background)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val breadcrumbParts = activeFile.path.split("/").takeLast(5)
                breadcrumbParts.forEachIndexed { index, part ->
                    Text(
                        text = part,
                        color = if (index == breadcrumbParts.lastIndex) colorScheme.foreground else colorScheme.comment,
                        fontSize = 11.sp,
                        fontWeight = if (index == breadcrumbParts.lastIndex) FontWeight.Bold else FontWeight.Normal
                    )
                    if (index < breadcrumbParts.lastIndex) {
                        Text(
                            text = " > ",
                            color = colorScheme.comment,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            HorizontalDivider(color = colorScheme.activeLineBg)
        }

        // 3. Main Code Canvas Area + Right Error Stripe / Minimap
        if (activeTabPath != null) {
            Row(modifier = Modifier.weight(1f)) {
                // Line Numbers + Code Editor Lines
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    itemsIndexed(lines) { index, lineText ->
                        val lineNumber = index + 1
                        val hasError = problems.any { it.line == lineNumber && it.severity == CodeProblem.Severity.ERROR }
                        val hasWarning = problems.any { it.line == lineNumber && it.severity == CodeProblem.Severity.WARNING }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .background(if (index % 2 == 0) Color.Transparent else colorScheme.activeLineBg.copy(alpha = 0.15f)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Line Number Column
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .fillMaxHeight()
                                    .background(colorScheme.activeLineBg.copy(alpha = 0.3f))
                                    .padding(end = 6.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = "$lineNumber",
                                    color = if (hasError) colorScheme.errorUnderline else if (hasWarning) colorScheme.warningUnderline else colorScheme.lineNumber,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Highlighted Line Text
                            val annotatedString = remember(lineText, fileType, colorScheme) {
                                SyntaxHighlighter.highlightLine(lineText, fileType, colorScheme)
                            }

                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                Text(
                                    text = annotatedString,
                                    fontSize = settings.fontSizeSp.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )

                                // Squiggly error underline style
                                if (hasError || hasWarning) {
                                    val problem = problems.firstOrNull { it.line == lineNumber }
                                    if (problem != null) {
                                        Text(
                                            text = lineText,
                                            fontSize = settings.fontSizeSp.sp,
                                            fontFamily = FontFamily.Monospace,
                                            style = TextStyle(
                                                textDecoration = TextDecoration.Underline,
                                                color = if (hasError) colorScheme.errorUnderline else colorScheme.warningUnderline
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Editable text input overlay at bottom of file or inline
                        BasicTextField(
                            value = content,
                            onValueChange = { viewModel.updateActiveContent(it) },
                            textStyle = TextStyle(
                                color = colorScheme.foreground,
                                fontSize = settings.fontSizeSp.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            cursorBrush = SolidColor(colorScheme.keyword),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                // 4. IntelliJ Right Error Stripe & Minimap
                if (settings.showMinimap) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .fillMaxHeight()
                            .background(colorScheme.activeLineBg)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val totalLines = lines.size.coerceAtLeast(1)
                            val canvasHeight = size.height

                            problems.forEach { problem ->
                                val lineRatio = problem.line.toFloat() / totalLines
                                val yPos = lineRatio * canvasHeight
                                val tickColor = if (problem.severity == CodeProblem.Severity.ERROR) colorScheme.errorUnderline else colorScheme.warningUnderline

                                drawRect(
                                    color = tickColor,
                                    topLeft = Offset(0f, yPos),
                                    size = Size(size.width, 3.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Empty State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = colorScheme.comment,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select or create a file from the Project panel", color = colorScheme.comment, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Press Search icon or Double Shift to find files", color = colorScheme.comment.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
        }
    }
}
