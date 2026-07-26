package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeTopBar(
    viewModel: IdeViewModel,
    colorScheme: SyntaxColorScheme,
    onOpenSettings: () -> Unit,
    onOpenNewProject: () -> Unit
) {
    val currentProject by viewModel.currentProject.collectAsState()
    val isBuilding by viewModel.isBuilding.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        color = colorScheme.background,
        shadowElevation = 2.dp
    ) {
        Column {
            // Main Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Project Name & Menu Trigger
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = colorScheme.foreground
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Fabric Mod Project") },
                                onClick = { showMenu = false; onOpenNewProject() },
                                leadingIcon = { Icon(Icons.Default.Add, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Run 'gradle build'") },
                                onClick = { showMenu = false; viewModel.runBuildTask("build") },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Run 'gradle clean'") },
                                onClick = { showMenu = false; viewModel.runBuildTask("clean") },
                                leadingIcon = { Icon(Icons.Default.Refresh, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("IDE Settings") },
                                onClick = { showMenu = false; onOpenSettings() },
                                leadingIcon = { Icon(Icons.Default.Settings, null) }
                            )
                        }
                    }

                    // Project Tag & Fabric Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(colorScheme.activeLineBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = colorScheme.keyword,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentProject?.name ?: "Idea Mobile",
                            color = colorScheme.foreground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (currentProject?.isFabricMod == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF3498DB),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = "Fabric ${currentProject?.minecraftVersion ?: "1.21.4"}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Right: Action Buttons (Build, Double Shift Search, Settings)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Double Shift Search Button
                    IconButton(onClick = { viewModel.openQuickSearch() }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Files (Double Shift)",
                            tint = colorScheme.foreground
                        )
                    }

                    // Gradle Run/Build Button
                    Surface(
                        color = if (isBuilding) Color(0xFFE67E22) else Color(0xFF27AE60),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.clickable { viewModel.runBuildTask("build") }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            if (isBuilding) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Build",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBuilding) "Building..." else "Build",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Settings Button
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = colorScheme.foreground
                        )
                    }
                }
            }

            HorizontalDivider(color = colorScheme.activeLineBg)
        }
    }
}
