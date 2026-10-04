package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.TabletMac
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppProject
import com.example.data.model.ConsoleLogItem
import com.example.data.model.LogLevel
import com.example.ui.components.ConsoleBottomSheet
import com.example.ui.components.DeviceFrame
import com.example.ui.components.DevicePreset
import com.example.ui.components.LiveWebViewRunner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveReviewScreen(
    project: AppProject?,
    htmlContent: String,
    filesMap: Map<String, String>,
    reloadTrigger: Int,
    consoleLogs: List<ConsoleLogItem>,
    devicePreset: DevicePreset,
    onSelectDevicePreset: (DevicePreset) -> Unit,
    onReload: () -> Unit,
    onConsoleLog: (ConsoleLogItem) -> Unit,
    onClearLogs: () -> Unit,
    onNavigateToStudio: () -> Unit,
    onNavigateToBuilder: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConsoleSheet by remember { mutableStateOf(false) }
    var isPageLoaded by remember { mutableStateOf(false) }
    var runtimeError by remember { mutableStateOf<String?>(null) }

    val errorCount = remember(consoleLogs) { consoleLogs.count { it.level == LogLevel.ERROR } }
    val warnCount = remember(consoleLogs) { consoleLogs.count { it.level == LogLevel.WARNING } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Live Review Sandbox",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (errorCount > 0) Color(0xFFDC2626) else Color(0xFF10B981))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (errorCount > 0) "$errorCount Errors" else "Active",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Text(
                        text = project?.name ?: "App Preview",
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
                // Hot Reload
                IconButton(onClick = onReload) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color(0xFF38BDF8))
                }

                // Console logs drawer
                IconButton(onClick = { showConsoleSheet = true }) {
                    Box {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = "Console Logs",
                            tint = if (errorCount > 0) Color(0xFFEF4444) else Color(0xFF94A3B8)
                        )
                        if (consoleLogs.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(if (errorCount > 0) Color(0xFFEF4444) else Color(0xFF2563EB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${consoleLogs.size.coerceAtMost(99)}",
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Switch to Studio Code
                IconButton(onClick = onNavigateToStudio) {
                    Icon(Icons.Default.Code, contentDescription = "Code Studio", tint = Color(0xFF60A5FA))
                }

                // Build APK
                IconButton(onClick = onNavigateToBuilder) {
                    Icon(Icons.Default.Build, contentDescription = "Build APK", tint = Color(0xFF34D399))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
        )

        // Device Controls & Dimensions Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B132B))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Frame:", color = Color(0xFF94A3B8), fontSize = 11.sp)

            DevicePreset.values().forEach { preset ->
                val isSelected = preset == devicePreset
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFF1E293B),
                    modifier = Modifier.clickable { onSelectDevicePreset(preset) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val icon = when (preset) {
                            DevicePreset.PHONE_PORTRAIT -> Icons.Default.PhoneAndroid
                            DevicePreset.PHONE_LANDSCAPE -> Icons.Default.ScreenRotation
                            DevicePreset.TABLET -> Icons.Default.TabletMac
                            DevicePreset.FULLSCREEN -> Icons.Default.Fullscreen
                        }
                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Text(
                            text = preset.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Live Device Sandbox Viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF030712)),
            contentAlignment = Alignment.Center
        ) {
            DeviceFrame(
                preset = devicePreset,
                modifier = Modifier.testTag("device_emulator_frame")
            ) {
                LiveWebViewRunner(
                    htmlData = htmlContent,
                    filesMap = filesMap,
                    reloadTrigger = reloadTrigger,
                    onConsoleLog = { log ->
                        onConsoleLog(log)
                    },
                    onPageLoaded = {
                        isPageLoaded = true
                        runtimeError = null
                    },
                    onError = { err ->
                        runtimeError = err
                        onConsoleLog(
                            ConsoleLogItem(
                                message = err,
                                level = LogLevel.ERROR
                            )
                        )
                    }
                )
            }
        }

        // Bottom Diagnostics Status Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (errorCount > 0) Color(0xFFEF4444) else Color(0xFF10B981))
                    )
                    Text(
                        text = if (errorCount > 0) "Errors caught in console ($errorCount)" else "App Running Live • 60 FPS Sandbox",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                Button(
                    onClick = onNavigateToBuilder,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Build APK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Console Logs Bottom Sheet
    if (showConsoleSheet) {
        ConsoleBottomSheet(
            logs = consoleLogs,
            onClearLogs = onClearLogs,
            onDismiss = { showConsoleSheet = false }
        )
    }
}
