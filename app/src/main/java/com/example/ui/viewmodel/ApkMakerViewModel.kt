package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppProject
import com.example.data.model.ConsoleLogItem
import com.example.data.model.LogLevel
import com.example.data.repository.ProjectRepository
import com.example.engine.ApkBuilderEngine
import com.example.engine.CodeShieldEngine
import com.example.engine.TemplateProvider
import com.example.engine.ZipEngine
import com.example.ui.components.DevicePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets

class ApkMakerViewModel(private val repository: ProjectRepository) : ViewModel() {

    val allProjects: StateFlow<List<AppProject>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentProject = MutableStateFlow<AppProject?>(null)
    val currentProject: StateFlow<AppProject?> = _currentProject.asStateFlow()

    private val _currentFiles = MutableStateFlow<Map<String, String>>(emptyMap())
    val currentFiles: StateFlow<Map<String, String>> = _currentFiles.asStateFlow()

    private val _activeFileName = MutableStateFlow("index.html")
    val activeFileName: StateFlow<String> = _activeFileName.asStateFlow()

    private val _activeFileContent = MutableStateFlow("")
    val activeFileContent: StateFlow<String> = _activeFileContent.asStateFlow()

    private val _isBuildingApk = MutableStateFlow(false)
    val isBuildingApk: StateFlow<Boolean> = _isBuildingApk.asStateFlow()

    private val _buildProgress = MutableStateFlow<ApkBuilderEngine.BuildProgress?>(null)
    val buildProgress: StateFlow<ApkBuilderEngine.BuildProgress?> = _buildProgress.asStateFlow()

    private val _lastBuildResult = MutableStateFlow<ApkBuilderEngine.BuildResult?>(null)
    val lastBuildResult: StateFlow<ApkBuilderEngine.BuildResult?> = _lastBuildResult.asStateFlow()

    private val _consoleLogs = MutableStateFlow<List<ConsoleLogItem>>(emptyList())
    val consoleLogs: StateFlow<List<ConsoleLogItem>> = _consoleLogs.asStateFlow()

    private val _devicePreset = MutableStateFlow(DevicePreset.PHONE_PORTRAIT)
    val devicePreset: StateFlow<DevicePreset> = _devicePreset.asStateFlow()

    private val _reloadTrigger = MutableStateFlow(0)
    val reloadTrigger: StateFlow<Int> = _reloadTrigger.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _preparedHtmlForPreview = MutableStateFlow("")
    val preparedHtmlForPreview: StateFlow<String> = _preparedHtmlForPreview.asStateFlow()

    private val _deviceZips = MutableStateFlow<List<File>>(emptyList())
    val deviceZips: StateFlow<List<File>> = _deviceZips.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialProjectsIfNeeded()
        }
    }

    fun loadDeviceZips(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            ZipEngine.prepareDeviceSampleZips(context)
            val found = ZipEngine.getDeviceZipFiles(context)
            withContext(Dispatchers.Main) {
                _deviceZips.value = found
            }
        }
    }

    fun importDeviceZipFile(context: Context, file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            importZip(context, uri)
        }
    }

    fun selectProject(project: AppProject) {
        _currentProject.value = project
        val files = ProjectRepository.jsonToMap(project.filesJson)
        _currentFiles.value = files
        val initialFile = if (files.containsKey(project.mainHtmlFile)) project.mainHtmlFile else (files.keys.firstOrNull() ?: "index.html")
        _activeFileName.value = initialFile
        _activeFileContent.value = files[initialFile] ?: ""
        _lastBuildResult.value = null
        _consoleLogs.value = emptyList()
        compilePreviewHtml()
    }

    fun setActiveFile(fileName: String) {
        // Save current active file first
        val currentMap = _currentFiles.value.toMutableMap()
        currentMap[_activeFileName.value] = _activeFileContent.value
        _currentFiles.value = currentMap

        _activeFileName.value = fileName
        _activeFileContent.value = currentMap[fileName] ?: ""
        compilePreviewHtml()
    }

    fun updateActiveFileContent(newContent: String) {
        _activeFileContent.value = newContent
        val currentMap = _currentFiles.value.toMutableMap()
        currentMap[_activeFileName.value] = newContent
        _currentFiles.value = currentMap
    }

    fun saveCurrentProject() {
        viewModelScope.launch {
            val project = _currentProject.value ?: return@launch
            val currentMap = _currentFiles.value.toMutableMap()
            currentMap[_activeFileName.value] = _activeFileContent.value
            _currentFiles.value = currentMap

            val updatedProject = project.copy(
                filesJson = ProjectRepository.mapToJson(currentMap),
                updatedAt = System.currentTimeMillis()
            )
            repository.updateProject(updatedProject)
            _currentProject.value = updatedProject
            compilePreviewHtml()
            _userMessage.value = "Project saved successfully! ✨"
        }
    }

    fun compilePreviewHtml() {
        val project = _currentProject.value ?: return
        val currentMap = _currentFiles.value.toMutableMap()
        currentMap[_activeFileName.value] = _activeFileContent.value

        val mainHtml = currentMap[project.mainHtmlFile] ?: currentMap["index.html"] ?: "<h1>${project.name}</h1>"
        // In-line styles and scripts from project files so local links work in WebView
        val bundledHtml = CodeShieldEngine.bundleToSingleHtml(mainHtml, currentMap)
        val shielded = CodeShieldEngine.shieldHtmlContent(bundledHtml, project.name, currentMap, injectConsoleBridge = true)
        _preparedHtmlForPreview.value = shielded
        _reloadTrigger.value += 1
    }

    fun reloadPreview() {
        compilePreviewHtml()
        _reloadTrigger.value += 1
    }

    fun setDevicePreset(preset: DevicePreset) {
        _devicePreset.value = preset
    }

    fun addConsoleLog(log: ConsoleLogItem) {
        _consoleLogs.value = listOf(log) + _consoleLogs.value.take(199)
    }

    fun clearLogs() {
        _consoleLogs.value = emptyList()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun importZip(context: Context, zipUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val extractResult = ZipEngine.extractZip(context, zipUri)
                val sanitizedPkg = "com.apkmaker." + extractResult.detectedAppName.lowercase().replace("[^a-z0-9]".toRegex(), "")
                val project = AppProject(
                    name = extractResult.detectedAppName,
                    packageName = if (sanitizedPkg.length > 14) sanitizedPkg else "com.apkmaker.importedapp",
                    versionName = "1.0.0",
                    versionCode = 1,
                    themeColorHex = "#2563EB",
                    iconEmoji = "📦",
                    mainHtmlFile = extractResult.mainHtmlFile,
                    filesJson = ProjectRepository.mapToJson(extractResult.files),
                    category = "Imported ZIP"
                )
                val newId = repository.insertProject(project)
                val created = project.copy(id = newId)
                withContext(Dispatchers.Main) {
                    selectProject(created)
                    _userMessage.value = "ZIP imported! Extracted ${extractResult.totalFiles} files 🎉"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Error extracting ZIP: ${e.message}"
                }
            }
        }
    }

    fun createAndImportDemoZip(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val demoHtml = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                        <title>Space Blaster Test</title>
                        <link rel="stylesheet" href="style.css">
                    </head>
                    <body>
                        <div class="hud">
                            <h1>🚀 Space Blaster</h1>
                            <p>Imported directly from ZIP file</p>
                            <div class="score">Score: <span id="pts">0</span></div>
                        </div>
                        <canvas id="stage"></canvas>
                        <script src="game.js"></script>
                    </body>
                    </html>
                """.trimIndent()

                val demoCss = """
                    * { margin:0; padding:0; box-sizing:border-box; user-select:none; }
                    body { background:#030712; color:#fff; font-family:system-ui; overflow:hidden; display:flex; flex-direction:column; align-items:center; height:100vh; }
                    .hud { padding:16px; text-align:center; z-index:10; }
                    .hud h1 { color:#38bdf8; font-size:24px; }
                    .hud p { color:#94a3b8; font-size:12px; margin-top:4px; }
                    .score { margin-top:8px; font-size:20px; font-weight:bold; color:#facc15; }
                    #stage { width:100%; max-width:400px; height:70vh; background:#0f172a; border-radius:16px; border:2px solid #1e293b; touch-action:none; }
                """.trimIndent()

                val demoJs = """
                    const canvas = document.getElementById('stage');
                    const ctx = canvas.getContext('2d');
                    const pts = document.getElementById('pts');
                    canvas.width = canvas.clientWidth || 360;
                    canvas.height = canvas.clientHeight || 500;

                    let score = 0;
                    let shipX = canvas.width / 2;
                    const stars = Array.from({length: 40}, () => ({
                        x: Math.random() * canvas.width,
                        y: Math.random() * canvas.height,
                        speed: Math.random() * 3 + 1
                    }));

                    const targets = [];
                    function spawnTarget() {
                        targets.push({
                            x: Math.random() * (canvas.width - 40) + 20,
                            y: -20,
                            size: 16,
                            speed: Math.random() * 2 + 1.5
                        });
                    }
                    setInterval(spawnTarget, 900);

                    function update() {
                        ctx.fillStyle = '#0f172a';
                        ctx.fillRect(0, 0, canvas.width, canvas.height);

                        ctx.fillStyle = '#64748b';
                        stars.forEach(s => {
                            s.y += s.speed;
                            if (s.y > canvas.height) s.y = 0;
                            ctx.fillRect(s.x, s.y, 2, 2);
                        });

                        for (let i = targets.length - 1; i >= 0; i--) {
                            const t = targets[i];
                            t.y += t.speed;
                            ctx.fillStyle = '#ef4444';
                            ctx.beginPath();
                            ctx.arc(t.x, t.y, t.size, 0, Math.PI * 2);
                            ctx.fill();

                            if (t.y > canvas.height + 20) {
                                targets.splice(i, 1);
                            }
                        }

                        ctx.fillStyle = '#38bdf8';
                        ctx.beginPath();
                        ctx.moveTo(shipX, canvas.height - 40);
                        ctx.lineTo(shipX - 16, canvas.height - 10);
                        ctx.lineTo(shipX + 16, canvas.height - 10);
                        ctx.closePath();
                        ctx.fill();

                        requestAnimationFrame(update);
                    }

                    function handleTouch(e) {
                        const rect = canvas.getBoundingClientRect();
                        const clientX = e.touches ? e.touches[0].clientX : e.clientX;
                        const clientY = e.touches ? e.touches[0].clientY : e.clientY;
                        shipX = Math.max(20, Math.min(canvas.width - 20, clientX - rect.left));

                        for (let i = targets.length - 1; i >= 0; i--) {
                            const t = targets[i];
                            const dist = Math.hypot(t.x - (clientX - rect.left), t.y - (clientY - rect.top));
                            if (dist < 40) {
                                targets.splice(i, 1);
                                score += 10;
                                pts.innerText = score;
                            }
                        }
                    }

                    canvas.addEventListener('touchmove', handleTouch, {passive: true});
                    canvas.addEventListener('touchstart', handleTouch, {passive: true});
                    canvas.addEventListener('mousedown', handleTouch);
                    update();
                    console.log('Space Blaster ZIP payload active!');
                """.trimIndent()

                val demoFiles = mapOf(
                    "index.html" to demoHtml,
                    "style.css" to demoCss,
                    "game.js" to demoJs
                )

                val zipFile = ZipEngine.createZip(context, "SpaceBlaster_Test.zip", demoFiles)
                val zipUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    zipFile
                )
                importZip(context, zipUri)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Demo zip error: ${e.message}"
                }
            }
        }
    }

    fun importHtml(context: Context, htmlUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bytes = context.contentResolver.openInputStream(htmlUri)?.use { it.readBytes() } ?: ByteArray(0)
                val content = String(bytes, StandardCharsets.UTF_8)
                val titleMatch = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(content)
                val title = titleMatch?.groupValues?.get(1)?.trim() ?: "My Web App"
                val sanitizedPkg = "com.apkmaker." + title.lowercase().replace("[^a-z0-9]".toRegex(), "")

                val files = mapOf("index.html" to content)
                val project = AppProject(
                    name = title,
                    packageName = if (sanitizedPkg.length > 14) sanitizedPkg else "com.apkmaker.webapp",
                    versionName = "1.0.0",
                    versionCode = 1,
                    themeColorHex = "#38BDF8",
                    iconEmoji = "🌐",
                    mainHtmlFile = "index.html",
                    filesJson = ProjectRepository.mapToJson(files),
                    category = "HTML App"
                )
                val newId = repository.insertProject(project)
                val created = project.copy(id = newId)
                withContext(Dispatchers.Main) {
                    selectProject(created)
                    _userMessage.value = "HTML imported successfully! 🎉"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Error importing HTML: ${e.message}"
                }
            }
        }
    }

    fun importHtmlText(name: String, code: String) {
        viewModelScope.launch {
            val title = name.ifBlank { "My Web App" }
            val sanitizedPkg = "com.apkmaker." + title.lowercase().replace("[^a-z0-9]".toRegex(), "")
            val files = mapOf("index.html" to code)
            val project = AppProject(
                name = title,
                packageName = if (sanitizedPkg.length > 14) sanitizedPkg else "com.apkmaker.customapp",
                versionName = "1.0.0",
                versionCode = 1,
                themeColorHex = "#38BDF8",
                iconEmoji = "🌐",
                mainHtmlFile = "index.html",
                filesJson = ProjectRepository.mapToJson(files),
                category = "HTML App"
            )
            val newId = repository.insertProject(project)
            val created = project.copy(id = newId)
            selectProject(created)
            _userMessage.value = "App '$title' created from HTML! 🚀"
        }
    }

    fun createFromTemplate(templateId: String) {
        viewModelScope.launch {
            val template = TemplateProvider.getAllTemplates().find { it.id == templateId } ?: return@launch
            val project = AppProject(
                name = template.title,
                packageName = template.defaultPackageName,
                versionName = "1.0.0",
                versionCode = 1,
                themeColorHex = template.themeColorHex,
                iconEmoji = template.iconEmoji,
                orientation = template.orientation,
                isFullscreen = template.category == "Game",
                mainHtmlFile = "index.html",
                filesJson = ProjectRepository.mapToJson(template.files),
                category = template.category
            )
            val newId = repository.insertProject(project)
            val created = project.copy(id = newId)
            selectProject(created)
            _userMessage.value = "Created app from ${template.title}! 🚀"
        }
    }

    fun createBlankProject(name: String, category: String) {
        viewModelScope.launch {
            val cleanPkg = "com.apkmaker." + name.lowercase().replace("[^a-z0-9]".toRegex(), "")
            val defaultHtml = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <title>$name</title>
                    <link rel="stylesheet" href="style.css">
                </head>
                <body>
                    <div class="card">
                        <h1>$name</h1>
                        <p>Welcome to your new Android app built with APK Maker!</p>
                        <button id="actionBtn">Click Me!</button>
                    </div>
                    <script src="script.js"></script>
                </body>
                </html>
            """.trimIndent()

            val defaultCss = """
                * { box-sizing: border-box; margin: 0; padding: 0; font-family: system-ui, sans-serif; }
                body { background: #0f172a; color: white; display: flex; justify-content: center; align-items: center; min-height: 100vh; padding: 20px; }
                .card { background: #1e293b; padding: 32px 24px; border-radius: 20px; text-align: center; border: 1px solid #334155; width: 100%; max-width: 400px; }
                h1 { color: #38bdf8; margin-bottom: 12px; }
                p { color: #94a3b8; margin-bottom: 24px; font-size: 15px; }
                button { background: #2563eb; color: white; border: none; padding: 12px 28px; border-radius: 12px; font-size: 16px; font-weight: bold; cursor: pointer; }
                button:active { transform: scale(0.95); }
            """.trimIndent()

            val defaultJs = """
                document.getElementById('actionBtn').addEventListener('click', () => {
                    alert('Hello from $name!');
                    console.log('Button tapped successfully!');
                });
                console.log('$name initialized smoothly.');
            """.trimIndent()

            val files = mapOf(
                "index.html" to defaultHtml,
                "style.css" to defaultCss,
                "script.js" to defaultJs
            )

            val project = AppProject(
                name = name,
                packageName = if (cleanPkg.length > 14) cleanPkg else "com.apkmaker.customapp",
                versionName = "1.0.0",
                versionCode = 1,
                themeColorHex = "#2563EB",
                iconEmoji = "⚡",
                mainHtmlFile = "index.html",
                filesJson = ProjectRepository.mapToJson(files),
                category = category
            )
            val newId = repository.insertProject(project)
            val created = project.copy(id = newId)
            selectProject(created)
            _userMessage.value = "New app '$name' created! Ready to build. 🎉"
        }
    }

    fun addNewFile(fileName: String, initialContent: String = "") {
        val clean = fileName.trim().removePrefix("/")
        if (clean.isBlank()) return
        val currentMap = _currentFiles.value.toMutableMap()
        currentMap[clean] = initialContent
        _currentFiles.value = currentMap
        setActiveFile(clean)
    }

    fun deleteFile(fileName: String) {
        if (fileName == "index.html") {
            _userMessage.value = "Cannot delete main index.html file."
            return
        }
        val currentMap = _currentFiles.value.toMutableMap()
        currentMap.remove(fileName)
        _currentFiles.value = currentMap
        if (_activeFileName.value == fileName) {
            setActiveFile("index.html")
        }
    }

    fun updateProjectSettings(
        name: String,
        packageName: String,
        versionName: String,
        versionCode: Int,
        emoji: String,
        colorHex: String,
        orientation: String,
        isFullscreen: Boolean
    ) {
        viewModelScope.launch {
            val project = _currentProject.value ?: return@launch
            val updated = project.copy(
                name = name,
                packageName = packageName,
                versionName = versionName,
                versionCode = versionCode,
                iconEmoji = emoji,
                themeColorHex = colorHex,
                orientation = orientation,
                isFullscreen = isFullscreen,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateProject(updated)
            _currentProject.value = updated
            compilePreviewHtml()
            _userMessage.value = "App configuration saved! 📱"
        }
    }

    fun buildApk(context: Context) {
        val project = _currentProject.value ?: return
        val currentMap = _currentFiles.value.toMutableMap()
        currentMap[_activeFileName.value] = _activeFileContent.value

        _isBuildingApk.value = true
        _buildProgress.value = ApkBuilderEngine.BuildProgress("Starting APK build...", 5)
        _lastBuildResult.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val result = ApkBuilderEngine.buildApk(
                context = context,
                project = project,
                files = currentMap,
                onProgress = { progress ->
                    _buildProgress.value = progress
                }
            )

            if (result.success && result.apkFile != null) {
                val updatedProject = project.copy(
                    lastApkPath = result.apkFile.absolutePath,
                    lastApkSize = result.apkFile.length(),
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateProject(updatedProject)
                _currentProject.value = updatedProject
            }

            withContext(Dispatchers.Main) {
                _isBuildingApk.value = false
                _lastBuildResult.value = result
                if (result.success) {
                    _userMessage.value = "APK Built successfully! Saved to phone storage. Ready to install! 📦"
                } else {
                    _userMessage.value = "Build error: ${result.errorMessage}"
                }
            }
        }
    }

    fun exportToZip(context: Context) {
        val project = _currentProject.value ?: return
        val currentMap = _currentFiles.value.toMutableMap()
        currentMap[_activeFileName.value] = _activeFileContent.value

        viewModelScope.launch(Dispatchers.IO) {
            val zipFile = ZipEngine.createZip(context, project.name, currentMap)
            val uri = ZipEngine.saveToDownloads(
                context,
                "${project.name}_source.zip",
                "application/zip",
                zipFile.readBytes()
            )
            withContext(Dispatchers.Main) {
                _userMessage.value = if (uri != null) {
                    "ZIP saved to phone Downloads! 📂"
                } else {
                    "ZIP generated in app cache: ${zipFile.name}"
                }
            }
        }
    }

    fun exportToSingleHtml(context: Context) {
        val project = _currentProject.value ?: return
        val currentMap = _currentFiles.value.toMutableMap()
        currentMap[_activeFileName.value] = _activeFileContent.value

        viewModelScope.launch(Dispatchers.IO) {
            val mainHtml = currentMap[project.mainHtmlFile] ?: currentMap["index.html"] ?: "<h1>${project.name}</h1>"
            val bundled = CodeShieldEngine.bundleToSingleHtml(mainHtml, currentMap)
            val uri = ZipEngine.saveToDownloads(
                context,
                "${project.name}.html",
                "text/html",
                bundled.toByteArray(StandardCharsets.UTF_8)
            )
            withContext(Dispatchers.Main) {
                _userMessage.value = if (uri != null) {
                    "HTML file saved to phone Downloads! 🌐"
                } else {
                    "Single HTML file generated successfully!"
                }
            }
        }
    }

    fun deleteProject(project: AppProject) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (_currentProject.value?.id == project.id) {
                _currentProject.value = null
                _currentFiles.value = emptyMap()
            }
            _userMessage.value = "Project '${project.name}' deleted."
        }
    }

    fun downloadThisApp(context: Context, installAfter: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = ApkBuilderEngine.exportCurrentAppApk(context)
            withContext(Dispatchers.Main) {
                if (result != null) {
                    val (apkFile, uri) = result
                    _userMessage.value = "✅ APK Successfully Downloaded to Downloads! (${apkFile.name})"
                    android.widget.Toast.makeText(
                        context,
                        "✅ APK Downloaded! (${apkFile.name}) Opening installer...",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    if (installAfter) {
                        ApkBuilderEngine.triggerInstall(context, apkFile)
                    }
                } else {
                    _userMessage.value = "Could not locate app APK file on this device."
                    android.widget.Toast.makeText(context, "Could not locate app APK", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun shareThisApp(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = ApkBuilderEngine.exportCurrentAppApk(context)
            withContext(Dispatchers.Main) {
                if (result != null) {
                    ApkBuilderEngine.shareApk(context, result.first, "APK Maker Studio")
                } else {
                    _userMessage.value = "Could not share APK."
                }
            }
        }
    }

    fun emailThisApp(context: Context, emailAddress: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = ApkBuilderEngine.exportCurrentAppApk(context)
            withContext(Dispatchers.Main) {
                if (result != null) {
                    ApkBuilderEngine.emailApk(context, result.first, emailAddress, "APK Maker Studio")
                } else {
                    _userMessage.value = "Could not locate APK to email."
                }
            }
        }
    }

    fun emailBuildResultApk(context: Context, emailAddress: String) {
        val result = _lastBuildResult.value ?: return
        val file = result.apkFile ?: return
        val project = _currentProject.value
        val name = project?.name ?: "Android App"
        ApkBuilderEngine.emailApk(context, file, emailAddress, name)
    }
}

class ApkMakerViewModelFactory(private val repository: ProjectRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ApkMakerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ApkMakerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
