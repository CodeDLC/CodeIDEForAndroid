package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.editor.analysis.StructureItem
import com.example.editor.syntax.CodeProblem
import com.example.editor.syntax.SyntaxColorScheme
import com.example.fs.FileTreeItem
import com.example.fs.FileType
import com.example.ui.LeftToolWindow
import com.example.ui.IdeViewModel

@Composable
fun ProjectToolWindow(
    viewModel: IdeViewModel,
    colorScheme: SyntaxColorScheme,
    onOpenNewFileDialog: (parentPath: String) -> Unit,
    onOpenNewClassDialog: (parentPath: String, isKotlin: Boolean) -> Unit
) {
    val leftWindow by viewModel.leftToolWindow.collectAsState()
    val fileTree by viewModel.fileTree.collectAsState()
    val activePath by viewModel.activeTabPath.collectAsState()
    val showHidden by viewModel.showHiddenFiles.collectAsState()
    val fileStructure by viewModel.fileStructure.collectAsState()
    val problems by viewModel.codeProblems.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(260.dp),
        color = colorScheme.background
    ) {
        Column {
            // Header Switcher Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.activeLineBg)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    ToolTabButton(
                        text = "Project",
                        icon = Icons.Default.Folder,
                        isSelected = leftWindow == LeftToolWindow.PROJECT,
                        colorScheme = colorScheme,
                        onClick = { viewModel.setLeftToolWindow(LeftToolWindow.PROJECT) }
                    )
                    ToolTabButton(
                        text = "Structure",
                        icon = Icons.Default.List,
                        isSelected = leftWindow == LeftToolWindow.STRUCTURE,
                        colorScheme = colorScheme,
                        onClick = { viewModel.setLeftToolWindow(LeftToolWindow.STRUCTURE) }
                    )
                    ToolTabButton(
                        text = "Problems",
                        icon = Icons.Default.Warning,
                        isSelected = leftWindow == LeftToolWindow.PROBLEMS,
                        colorScheme = colorScheme,
                        onClick = { viewModel.setLeftToolWindow(LeftToolWindow.PROBLEMS) }
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleShowHiddenFiles() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (showHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Show Hidden Files",
                        tint = colorScheme.foreground,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = colorScheme.activeLineBg)

            // Content Panel
            Box(modifier = Modifier.weight(1f)) {
                when (leftWindow) {
                    LeftToolWindow.PROJECT -> {
                        if (fileTree != null) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    FileTreeNodeItem(
                                        item = fileTree!!,
                                        level = 0,
                                        activePath = activePath,
                                        colorScheme = colorScheme,
                                        onSelect = { item ->
                                            if (item.isDirectory) {
                                                viewModel.togglePathExpanded(item.path)
                                            } else {
                                                viewModel.openFile(item.path)
                                            }
                                        },
                                        onNewFile = { path -> onOpenNewFileDialog(path) },
                                        onNewClass = { path, isKt -> onOpenNewClassDialog(path, isKt) }
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No project open", color = colorScheme.comment, fontSize = 12.sp)
                            }
                        }
                    }
                    LeftToolWindow.STRUCTURE -> {
                        StructureView(
                            items = fileStructure,
                            colorScheme = colorScheme
                        )
                    }
                    LeftToolWindow.PROBLEMS -> {
                        ProblemsView(
                            problems = problems,
                            colorScheme = colorScheme,
                            onSelectProblem = { problem ->
                                viewModel.applyQuickFix(problem, 0)
                            }
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun ToolTabButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    colorScheme: SyntaxColorScheme,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) colorScheme.background else Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
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
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileTreeNodeItem(
    item: FileTreeItem,
    level: Int,
    activePath: String?,
    colorScheme: SyntaxColorScheme,
    onSelect: (FileTreeItem) -> Unit,
    onNewFile: (parentPath: String) -> Unit,
    onNewClass: (parentPath: String, isKotlin: Boolean) -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val isSelected = item.path == activePath

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelected) colorScheme.selectionBg.copy(alpha = 0.3f) else Color.Transparent)
                .combinedClickable(
                    onClick = { onSelect(item) },
                    onLongClick = { showContextMenu = true }
                )
                .padding(start = (level * 12 + 6).dp, top = 4.dp, bottom = 4.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.isDirectory) {
                Icon(
                    imageVector = if (item.isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.ArrowRight,
                    contentDescription = null,
                    tint = colorScheme.comment,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color(0xFFF39C12),
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(18.dp))
                FileIcon(
                    fileType = FileType.fromExtension(item.extension, item.name),
                    colorScheme = colorScheme
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = item.name,
                color = if (isSelected) colorScheme.foreground else colorScheme.foreground.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Context Menu Dropdown
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                val parentDir = if (item.isDirectory) item.path else item.path.substringBeforeLast("/")
                DropdownMenuItem(
                    text = { Text("New Java Class") },
                    onClick = { showContextMenu = false; onNewClass(parentDir, false) },
                    leadingIcon = { Text("J", color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold) }
                )
                DropdownMenuItem(
                    text = { Text("New Kotlin Class") },
                    onClick = { showContextMenu = false; onNewClass(parentDir, true) },
                    leadingIcon = { Text("K", color = Color(0xFF9B59B6), fontWeight = FontWeight.Bold) }
                )
                DropdownMenuItem(
                    text = { Text("New File") },
                    onClick = { showContextMenu = false; onNewFile(parentDir) },
                    leadingIcon = { Icon(Icons.Default.Add, null) }
                )
            }
        }

        if (item.isDirectory && item.isExpanded) {
            item.children.forEach { child ->
                FileTreeNodeItem(
                    item = child,
                    level = level + 1,
                    activePath = activePath,
                    colorScheme = colorScheme,
                    onSelect = onSelect,
                    onNewFile = onNewFile,
                    onNewClass = onNewClass
                )
            }
        }
    }
}

@Composable
fun FileIcon(fileType: FileType, colorScheme: SyntaxColorScheme) {
    when (fileType) {
        FileType.JAVA -> {
            Surface(
                color = Color(0xFFE74C3C),
                shape = RoundedCornerShape(2.dp)
            ) {
                Text(
                    text = "J",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                )
            }
        }
        FileType.KOTLIN -> {
            Surface(
                color = Color(0xFF9B59B6),
                shape = RoundedCornerShape(2.dp)
            ) {
                Text(
                    text = "K",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                )
            }
        }
        FileType.GRADLE -> {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = Color(0xFF2ECC71),
                modifier = Modifier.size(14.dp)
            )
        }
        FileType.XML -> {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = Color(0xFFE67E22),
                modifier = Modifier.size(14.dp)
            )
        }
        FileType.JSON -> {
            Icon(
                imageVector = Icons.Default.DataObject,
                contentDescription = null,
                tint = Color(0xFFF1C40F),
                modifier = Modifier.size(14.dp)
            )
        }
        else -> {
            Icon(
                imageVector = Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = colorScheme.comment,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun StructureView(items: List<StructureItem>, colorScheme: SyntaxColorScheme) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No structure available", color = colorScheme.comment, fontSize = 12.sp)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            items(items) { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    val (icon, color) = when (item.kind) {
                        StructureItem.Kind.CLASS -> Icons.Default.Class to colorScheme.type
                        StructureItem.Kind.INTERFACE -> Icons.Default.Category to colorScheme.type
                        StructureItem.Kind.METHOD -> Icons.Default.Functions to colorScheme.keyword
                        StructureItem.Kind.FIELD -> Icons.Default.Tag to colorScheme.string
                        StructureItem.Kind.PROPERTY -> Icons.Default.Tag to colorScheme.number
                    }
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = item.name, color = colorScheme.foreground, fontSize = 12.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = ":${item.line}", color = colorScheme.comment, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun ProblemsView(
    problems: List<CodeProblem>,
    colorScheme: SyntaxColorScheme,
    onSelectProblem: (CodeProblem) -> Unit
) {
    if (problems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2ECC71), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("No problems found in file", color = colorScheme.comment, fontSize = 12.sp)
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            items(problems) { problem ->
                val color = if (problem.severity == CodeProblem.Severity.ERROR) colorScheme.errorUnderline else colorScheme.warningUnderline
                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { onSelectProblem(problem) }
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (problem.severity == CodeProblem.Severity.ERROR) Icons.Default.Error else Icons.Default.Warning,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = problem.message, color = colorScheme.foreground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Line ${problem.line}, Column ${problem.column}", color = colorScheme.comment, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
