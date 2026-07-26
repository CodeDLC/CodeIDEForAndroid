package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EditorSettingsEntity
import com.example.editor.syntax.SyntaxColorScheme
import com.example.ui.IdeViewModel

@Composable
fun QuickSearchDialog(
    viewModel: IdeViewModel,
    colorScheme: SyntaxColorScheme,
    onDismiss: () -> Unit
) {
    val query by viewModel.quickSearchQuery.collectAsState()
    val results by viewModel.quickSearchResults.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = colorScheme.keyword)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Search File (Double Shift)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.updateQuickSearchQuery(it) },
                    placeholder = { Text("Type file name...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(results) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.openFile(item.path)
                                    onDismiss()
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = item.path, color = colorScheme.comment, fontSize = 10.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun SettingsDialog(
    settings: EditorSettingsEntity,
    colorScheme: SyntaxColorScheme,
    onSave: (EditorSettingsEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var themeName by remember { mutableStateOf(settings.themeName) }
    var fontSize by remember { mutableStateOf(settings.fontSizeSp.toString()) }
    var showLineNumbers by remember { mutableStateOf(settings.showLineNumbers) }
    var showMinimap by remember { mutableStateOf(settings.showMinimap) }
    var apiKey by remember { mutableStateOf(settings.geminiApiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("IDE Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Theme", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = themeName == "Darcula",
                        onClick = { themeName = "Darcula" },
                        label = { Text("Darcula (Dark)") }
                    )
                    FilterChip(
                        selected = themeName == "IntelliJLight",
                        onClick = { themeName = "IntelliJLight" },
                        label = { Text("IntelliJ Light") }
                    )
                }

                OutlinedTextField(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    label = { Text("Editor Font Size (sp)") },
                    singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showLineNumbers, onCheckedChange = { showLineNumbers = it })
                    Text("Show Line Numbers")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showMinimap, onCheckedChange = { showMinimap = it })
                    Text("Show Right Error Stripe / Minimap")
                }

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Gemini API Key (OpenCode AI)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    settings.copy(
                        themeName = themeName,
                        fontSizeSp = fontSize.toFloatOrNull() ?: 13f,
                        showLineNumbers = showLineNumbers,
                        showMinimap = showMinimap,
                        geminiApiKey = apiKey
                    )
                )
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NewProjectDialog(
    onCreateProject: (name: String, version: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("MyFabricMod") }
    var version by remember { mutableStateOf("1.21.4") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Fabric Mod Project", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Mod Project Name") },
                    singleLine = true
                )

                Text("Minecraft Version", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("1.21.4", "1.21.8", "1.21.11").forEach { v ->
                        FilterChip(
                            selected = version == v,
                            onClick = { version = v },
                            label = { Text(v) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onCreateProject(name, version)
                    onDismiss()
                }
            }) {
                Text("Create")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun NewFileDialog(
    parentPath: String,
    onCreateFile: (fileName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var fileName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New File", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Directory: $parentPath", fontSize = 10.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name (e.g. ModItem.kt, config.json)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (fileName.isNotBlank()) {
                    onCreateFile(fileName)
                    onDismiss()
                }
            }) { Text("Create") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun NewClassDialog(
    parentPath: String,
    isKotlin: Boolean,
    onCreateClass: (className: String, pkg: String) -> Unit,
    onDismiss: () -> Unit
) {
    var className by remember { mutableStateOf("CustomItem") }
    var pkg by remember { mutableStateOf("com.example.fabricmod") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isKotlin) "New Kotlin Class" else "New Java Class", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Class Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = pkg,
                    onValueChange = { pkg = it },
                    label = { Text("Package Name") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (className.isNotBlank()) {
                    onCreateClass(className, pkg)
                    onDismiss()
                }
            }) { Text("Create") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
