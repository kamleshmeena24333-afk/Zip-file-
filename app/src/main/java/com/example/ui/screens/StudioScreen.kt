package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppProject
import com.example.engine.CodeShieldEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    project: AppProject?,
    filesMap: Map<String, String>,
    activeFileName: String,
    activeFileContent: String,
    onFileSelected: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSaveProject: () -> Unit,
    onAddNewFile: (String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateToBuilder: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val quickSnippets = remember {
        listOf(
            "<div>" to "<div>\n    \n</div>",
            "<button>" to "<button id=\"myBtn\">Click</button>",
            "<h1>" to "<h1>Title</h1>",
            "<script>" to "<script>\n    \n</script>",
            "<style>" to "<style>\n    \n</style>",
            "console.log" to "console.log(\"App running!\");",
            "alert" to "alert(\"Hello!\");",
            "function" to "function doAction() {\n    \n}"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        // App Top Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = project?.name ?: "Code Studio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = "${filesMap.size} files • $activeFileName",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            actions = {
                // Auto-Shield Fixer
                IconButton(
                    onClick = {
                        val current = activeFileContent
                        val shielded = if (activeFileName.endsWith(".html", ignoreCase = true)) {
                            CodeShieldEngine.shieldHtmlContent(current, project?.name ?: "App", filesMap)
                        } else {
                            current
                        }
                        onContentChange(shielded)
                    }
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = "Auto Error Shield",
                        tint = Color(0xFF34D399)
                    )
                }

                // Save
                IconButton(onClick = onSaveProject) {
                    Icon(Icons.Default.Save, contentDescription = "Save", tint = Color(0xFF38BDF8))
                }

                // Live Review
                IconButton(onClick = onNavigateToReview) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Live Review", tint = Color(0xFF10B981))
                }

                // Build APK
                IconButton(onClick = onNavigateToBuilder) {
                    Icon(Icons.Default.Build, contentDescription = "Build APK", tint = Color(0xFF60A5FA))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF0F172A)
            )
        )

        // File Tabs Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B132B))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filesMap.keys.sorted().forEach { fileName ->
                val isSelected = fileName == activeFileName
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                    ),
                    modifier = Modifier.clickable { onFileSelected(fileName) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconChar = when {
                            fileName.endsWith(".html") -> "🌐"
                            fileName.endsWith(".css") -> "🎨"
                            fileName.endsWith(".js") -> "⚡"
                            fileName.endsWith(".json") -> "📋"
                            else -> "📄"
                        }
                        Text(text = iconChar, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = fileName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Add File Button
            IconButton(
                onClick = { showAddFileDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add File", tint = Color(0xFF38BDF8))
            }

            if (activeFileName != "index.html" && filesMap.containsKey(activeFileName)) {
                IconButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete File", tint = Color(0xFFEF4444))
                }
            }
        }

        // Quick Snippets Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF070B14))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickSnippets.forEach { (label, snippet) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                        .clickable {
                            onContentChange(activeFileContent + "\n" + snippet)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF93C5FD)
                    )
                }
            }
        }

        // Code Editor
        TextField(
            value = activeFileContent,
            onValueChange = onContentChange,
            modifier = Modifier
                .fillMaxSize()
                .testTag("code_editor_textarea"),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color(0xFFE2E8F0)
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF090D16),
                unfocusedContainerColor = Color(0xFF090D16),
                cursorColor = Color(0xFF38BDF8),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }

    // Add File Dialog
    if (showAddFileDialog) {
        AlertDialog(
            onDismissRequest = { showAddFileDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Add New File", color = Color.White) },
            text = {
                Column {
                    Text("Enter file name (e.g. style.css, utils.js, data.json):", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        singleLine = true,
                        placeholder = { Text("new_file.js") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            onAddNewFile(newFileName)
                            newFileName = ""
                            showAddFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFileDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Delete File Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Delete File?", color = Color.White) },
            text = { Text("Are you sure you want to delete '$activeFileName'?", color = Color(0xFF94A3B8)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFile(activeFileName)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}
