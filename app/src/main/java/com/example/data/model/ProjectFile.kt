package com.example.data.model

data class ProjectFile(
    val path: String,
    val content: String,
    val isBinary: Boolean = false,
    val sizeBytes: Long = content.toByteArray().size.toLong()
)

data class ConsoleLogItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val message: String,
    val level: LogLevel,
    val sourceId: String? = null,
    val lineNumber: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR
}
