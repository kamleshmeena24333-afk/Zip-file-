package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_projects")
data class AppProject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val packageName: String,
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val themeColorHex: String = "#2563EB",
    val iconEmoji: String = "⚡",
    val orientation: String = "portrait", // "portrait", "landscape", "sensor"
    val isFullscreen: Boolean = false,
    val mainHtmlFile: String = "index.html",
    val filesJson: String = "{}", // JSON Map of relative-path to content
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastApkPath: String? = null,
    val lastApkSize: Long = 0L,
    val category: String = "Web App" // Game, Utility, Shop, Notes, Custom
)
