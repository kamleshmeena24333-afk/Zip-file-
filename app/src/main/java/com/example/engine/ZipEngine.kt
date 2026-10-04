package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipEngine {

    data class ZipExtractResult(
        val files: Map<String, String>,
        val mainHtmlFile: String,
        val totalFiles: Int,
        val detectedAppName: String
    )

    /**
     * Reads a ZIP file from Uri and extracts all text/assets into a map of relative path -> content
     */
    fun extractZip(context: Context, zipUri: Uri): ZipExtractResult {
        val files = mutableMapOf<String, String>()
        var mainHtml = "index.html"
        var candidateHtml: String? = null
        var total = 0
        var detectedTitle = ""

        context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val name = entry.name.replace("\\", "/")
                    // Protect against Zip Slip vulnerability
                    if (!name.contains("..") && !entry.isDirectory && !name.startsWith("__MACOSX")) {
                        total++
                        val bytes = readEntryBytes(zis)
                        val relativePath = normalizePath(name)

                        // Check if text file or asset
                        val isText = isTextExtension(relativePath)
                        if (isText) {
                            val content = String(bytes, StandardCharsets.UTF_8)
                            files[relativePath] = content

                            if (relativePath.equals("index.html", ignoreCase = true)) {
                                mainHtml = relativePath
                                val titleMatch = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(content)
                                if (titleMatch != null) {
                                    detectedTitle = titleMatch.groupValues[1].trim()
                                }
                            } else if (candidateHtml == null && relativePath.endsWith(".html", ignoreCase = true)) {
                                candidateHtml = relativePath
                            }
                        } else {
                            // Store binary file as base64 string
                            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                            files[relativePath] = "data:${getMimeType(relativePath)};base64,$base64"
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        // If no index.html found at root, use candidate or first html
        if (!files.containsKey(mainHtml)) {
            mainHtml = candidateHtml ?: files.keys.firstOrNull { it.endsWith(".html", ignoreCase = true) } ?: "index.html"
            if (!files.containsKey(mainHtml)) {
                // Generate a launcher page showing all files
                val generatedLauncher = generateDirectoryListingHtml(files)
                files["index.html"] = generatedLauncher
                mainHtml = "index.html"
            }
        }

        if (detectedTitle.isBlank()) {
            val fileName = getFileName(context, zipUri)
            detectedTitle = fileName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
                .split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
            if (detectedTitle.isBlank()) detectedTitle = "My Imported App"
        }

        return ZipExtractResult(
            files = files,
            mainHtmlFile = mainHtml,
            totalFiles = total,
            detectedAppName = detectedTitle
        )
    }

    /**
     * Packages files into a ZIP archive File.
     */
    fun createZip(context: Context, zipName: String, files: Map<String, String>): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val cleanName = zipName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val outFile = File(exportDir, if (cleanName.endsWith(".zip")) cleanName else "$cleanName.zip")

        ZipOutputStream(FileOutputStream(outFile)).use { zos ->
            files.forEach { (path, content) ->
                val entry = ZipEntry(path)
                zos.putNextEntry(entry)
                if (content.startsWith("data:") && content.contains(";base64,")) {
                    val base64Data = content.substringAfter(";base64,")
                    val rawBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    zos.write(rawBytes)
                } else {
                    zos.write(content.toByteArray(StandardCharsets.UTF_8))
                }
                zos.closeEntry()
            }
        }
        return outFile
    }

    /**
     * Creates and ensures real sample ZIP files exist on device storage
     * (in both app internal files and public Downloads folder).
     */
    fun prepareDeviceSampleZips(context: Context): List<File> {
        val sampleDir = File(context.filesDir, "device_zips").apply { mkdirs() }
        val results = mutableListOf<File>()

        val templateFiles = mapOf(
            "TestProject_SpaceArcade.zip" to TemplateProvider.getAllTemplates().first { it.id == "flappy_droid" }.files,
            "TestProject_TaskApp.zip" to TemplateProvider.getAllTemplates().first { it.id == "taskflow_notes" }.files,
            "TestProject_Calculator.zip" to TemplateProvider.getAllTemplates().first { it.id == "neocalc_pro" }.files
        )

        templateFiles.forEach { (zipName, files) ->
            val targetFile = File(sampleDir, zipName)
            if (!targetFile.exists() || targetFile.length() == 0L) {
                ZipOutputStream(FileOutputStream(targetFile)).use { zos ->
                    files.forEach { (path, content) ->
                        zos.putNextEntry(ZipEntry(path))
                        zos.write(content.toByteArray(StandardCharsets.UTF_8))
                        zos.closeEntry()
                    }
                }
                // Also mirror to public Downloads so system file picker can see it
                saveToDownloads(context, zipName, "application/zip", targetFile.readBytes())
            }
            results.add(targetFile)
        }
        return results
    }

    fun getDeviceZipFiles(context: Context): List<File> {
        val list = mutableListOf<File>()
        val sampleDir = File(context.filesDir, "device_zips")
        if (sampleDir.exists()) {
            sampleDir.listFiles { f -> f.extension.equals("zip", ignoreCase = true) }?.let {
                list.addAll(it)
            }
        }
        val exportDir = File(context.cacheDir, "exports")
        if (exportDir.exists()) {
            exportDir.listFiles { f -> f.extension.equals("zip", ignoreCase = true) }?.let {
                list.addAll(it)
            }
        }
        return list.distinctBy { it.name }
    }

    /**
     * Saves any file (APK, ZIP, HTML) to the device's public Downloads directory.
     * Works on Android 10+ (via MediaStore) and earlier (via Environment).
     */
    fun saveToDownloads(context: Context, fileName: String, mimeType: String, fileBytes: ByteArray): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(fileBytes)
                        os.flush()
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                uri
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, fileName)
                FileOutputStream(targetFile).use { it.write(fileBytes) }
                Uri.fromFile(targetFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun readEntryBytes(zis: ZipInputStream): ByteArray {
        val buffer = ByteArray(4096)
        val baos = ByteArrayOutputStream()
        var len: Int
        while (zis.read(buffer).also { len = it } > 0) {
            baos.write(buffer, 0, len)
        }
        return baos.toByteArray()
    }

    private fun normalizePath(rawPath: String): String {
        // Strip common leading root folder e.g. "my-project/index.html" -> "index.html" if top-level container
        var p = rawPath.removePrefix("/")
        val firstSlash = p.indexOf('/')
        // If there's an index.html at a subfolder, keep it clean
        return p
    }

    private fun isTextExtension(path: String): Boolean {
        val ext = path.substringAfterLast(".", "").lowercase()
        return ext in listOf("html", "htm", "css", "js", "json", "txt", "svg", "xml", "md", "csv")
    }

    fun getMimeType(path: String): String {
        val ext = path.substringAfterLast(".", "").lowercase()
        return when (ext) {
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "json" -> "application/json"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "webp" -> "image/webp"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            else -> "application/octet-stream"
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "Project"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex) ?: name
                }
            }
        }
        return name
    }

    private fun generateDirectoryListingHtml(files: Map<String, String>): String {
        val items = files.keys.joinToString("") {
            "<li><a href=\"$it\">$it</a></li>"
        }
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Project Explorer</title>
                <style>
                    body { font-family:sans-serif; background:#0f172a; color:#fff; padding:20px; }
                    h2 { color:#38bdf8; }
                    ul { list-style:none; padding:0; }
                    li { margin:8px 0; }
                    a { color:#60a5fa; text-decoration:none; font-size:16px; }
                </style>
            </head>
            <body>
                <h2>Project Files</h2>
                <ul>$items</ul>
            </body>
            </html>
        """.trimIndent()
    }
}
