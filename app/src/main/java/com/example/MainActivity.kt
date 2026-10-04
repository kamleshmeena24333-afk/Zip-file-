package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.data.repository.ProjectRepository
import com.example.ui.screens.ApkBuilderScreen
import com.example.ui.screens.CreateImportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveReviewScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ApkMakerViewModel
import com.example.ui.viewmodel.ApkMakerViewModelFactory

enum class AppScreen {
    HOME,
    CREATE,
    STUDIO,
    REVIEW,
    BUILDER
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = ProjectRepository(database.appProjectDao())
        val viewModelFactory = ApkMakerViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                val viewModel: ApkMakerViewModel = viewModel(factory = viewModelFactory)
                ApkMakerApp(viewModel)
            }
        }
    }
}

@Composable
fun ApkMakerApp(viewModel: ApkMakerViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }

    val projects by viewModel.allProjects.collectAsStateWithLifecycle()
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val currentFiles by viewModel.currentFiles.collectAsStateWithLifecycle()
    val activeFileName by viewModel.activeFileName.collectAsStateWithLifecycle()
    val activeFileContent by viewModel.activeFileContent.collectAsStateWithLifecycle()
    val isBuilding by viewModel.isBuildingApk.collectAsStateWithLifecycle()
    val buildProgress by viewModel.buildProgress.collectAsStateWithLifecycle()
    val buildResult by viewModel.lastBuildResult.collectAsStateWithLifecycle()
    val consoleLogs by viewModel.consoleLogs.collectAsStateWithLifecycle()
    val devicePreset by viewModel.devicePreset.collectAsStateWithLifecycle()
    val reloadTrigger by viewModel.reloadTrigger.collectAsStateWithLifecycle()
    val preparedHtml by viewModel.preparedHtmlForPreview.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val deviceZips by viewModel.deviceZips.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadDeviceZips(context)
    }

    // Handle user snackbar/toast notifications
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Back navigation handling
    if (currentScreen != AppScreen.HOME) {
        BackHandler {
            currentScreen = AppScreen.HOME
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF090D16),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = Color(0xFF0A0F1D),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📦", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "APK Maker",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    // 1-Click Instant Download Button
                    Button(
                        onClick = {
                            viewModel.downloadThisApp(context, installAfter = true)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("topbar_instant_download_button")
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "Download APK",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DOWNLOAD APK",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                tonalElevation = 8.dp
            ) {
                val navItems = listOf(
                    Triple(AppScreen.HOME, "Projects", Icons.Default.Dashboard),
                    Triple(AppScreen.CREATE, "New/Import", Icons.Default.AddCircle),
                    Triple(AppScreen.STUDIO, "Studio", Icons.Default.Code),
                    Triple(AppScreen.REVIEW, "Live Review", Icons.Default.PlayCircle),
                    Triple(AppScreen.BUILDER, "Build APK", Icons.Default.Build)
                )

                navItems.forEach { (screen, label, icon) ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (screen in listOf(AppScreen.STUDIO, AppScreen.REVIEW, AppScreen.BUILDER) && currentProject == null && projects.isNotEmpty()) {
                                viewModel.selectProject(projects.first())
                            }
                            currentScreen = screen
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF38BDF8),
                            selectedTextColor = Color(0xFF38BDF8),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B),
                            indicatorColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.HOME -> {
                    HomeScreen(
                        projects = projects,
                        onSelectProject = { project ->
                            viewModel.selectProject(project)
                        },
                        onNavigateToCreate = { currentScreen = AppScreen.CREATE },
                        onNavigateToStudio = { currentScreen = AppScreen.STUDIO },
                        onNavigateToReview = { currentScreen = AppScreen.REVIEW },
                        onNavigateToBuilder = { currentScreen = AppScreen.BUILDER },
                        onExportZip = { project ->
                            viewModel.selectProject(project)
                            viewModel.exportToZip(context)
                        },
                        onExportHtml = { project ->
                            viewModel.selectProject(project)
                            viewModel.exportToSingleHtml(context)
                        },
                        onDeleteProject = { project ->
                            viewModel.deleteProject(project)
                        },
                        onDownloadThisApp = {
                            viewModel.downloadThisApp(context, installAfter = false)
                        },
                        onInstallThisApp = {
                            viewModel.downloadThisApp(context, installAfter = true)
                        },
                        onShareThisApp = {
                            viewModel.shareThisApp(context)
                        },
                        onEmailThisApp = { email ->
                            viewModel.emailThisApp(context, email)
                        }
                    )
                }

                AppScreen.CREATE -> {
                    CreateImportScreen(
                        deviceZips = deviceZips,
                        onImportZip = { uri ->
                            viewModel.importZip(context, uri)
                            currentScreen = AppScreen.STUDIO
                        },
                        onImportDeviceZipFile = { file ->
                            viewModel.importDeviceZipFile(context, file)
                            currentScreen = AppScreen.STUDIO
                        },
                        onImportHtml = { uri ->
                            viewModel.importHtml(context, uri)
                            currentScreen = AppScreen.STUDIO
                        },
                        onImportHtmlCode = { name, code ->
                            viewModel.importHtmlText(name, code)
                            currentScreen = AppScreen.STUDIO
                        },
                        onSelectTemplate = { templateId ->
                            viewModel.createFromTemplate(templateId)
                            currentScreen = AppScreen.STUDIO
                        },
                        onCreateBlank = { name, category ->
                            viewModel.createBlankProject(name, category)
                            currentScreen = AppScreen.STUDIO
                        },
                        onTestDemoZip = {
                            viewModel.createAndImportDemoZip(context)
                            currentScreen = AppScreen.STUDIO
                        },
                        onNavigateBack = { currentScreen = AppScreen.HOME }
                    )
                }

                AppScreen.STUDIO -> {
                    if (currentProject == null && projects.isNotEmpty()) {
                        viewModel.selectProject(projects.first())
                    }

                    StudioScreen(
                        project = currentProject,
                        filesMap = currentFiles,
                        activeFileName = activeFileName,
                        activeFileContent = activeFileContent,
                        onFileSelected = { fileName -> viewModel.setActiveFile(fileName) },
                        onContentChange = { newContent -> viewModel.updateActiveFileContent(newContent) },
                        onSaveProject = { viewModel.saveCurrentProject() },
                        onAddNewFile = { newName -> viewModel.addNewFile(newName) },
                        onDeleteFile = { fileName -> viewModel.deleteFile(fileName) },
                        onNavigateToReview = { currentScreen = AppScreen.REVIEW },
                        onNavigateToBuilder = { currentScreen = AppScreen.BUILDER },
                        onNavigateBack = { currentScreen = AppScreen.HOME }
                    )
                }

                AppScreen.REVIEW -> {
                    if (currentProject == null && projects.isNotEmpty()) {
                        viewModel.selectProject(projects.first())
                    }

                    LiveReviewScreen(
                        project = currentProject,
                        htmlContent = preparedHtml,
                        filesMap = currentFiles,
                        reloadTrigger = reloadTrigger,
                        consoleLogs = consoleLogs,
                        devicePreset = devicePreset,
                        onSelectDevicePreset = { preset -> viewModel.setDevicePreset(preset) },
                        onReload = { viewModel.reloadPreview() },
                        onConsoleLog = { log -> viewModel.addConsoleLog(log) },
                        onClearLogs = { viewModel.clearLogs() },
                        onNavigateToStudio = { currentScreen = AppScreen.STUDIO },
                        onNavigateToBuilder = { currentScreen = AppScreen.BUILDER },
                        onNavigateBack = { currentScreen = AppScreen.HOME }
                    )
                }

                AppScreen.BUILDER -> {
                    if (currentProject == null && projects.isNotEmpty()) {
                        viewModel.selectProject(projects.first())
                    }

                    ApkBuilderScreen(
                        project = currentProject,
                        isBuilding = isBuilding,
                        buildProgress = buildProgress,
                        buildResult = buildResult,
                        onBuildApk = { viewModel.buildApk(context) },
                        onExportZip = { viewModel.exportToZip(context) },
                        onExportHtml = { viewModel.exportToSingleHtml(context) },
                        onUpdateSettings = { name, pkg, vName, vCode, emoji, color, ori, full ->
                            viewModel.updateProjectSettings(name, pkg, vName, vCode, emoji, color, ori, full)
                        },
                        onNavigateToReview = { currentScreen = AppScreen.REVIEW },
                        onNavigateBack = { currentScreen = AppScreen.HOME }
                    )
                }
            }
        }
    }
}
