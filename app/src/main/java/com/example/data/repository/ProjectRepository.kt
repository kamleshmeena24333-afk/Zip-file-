package com.example.data.repository

import com.example.data.dao.AppProjectDao
import com.example.data.model.AppProject
import com.example.engine.TemplateProvider
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

class ProjectRepository(private val dao: AppProjectDao) {

    val allProjects: Flow<List<AppProject>> = dao.getAllProjects()

    fun getProject(id: Long): Flow<AppProject?> = dao.getProjectById(id)

    suspend fun getProjectDirect(id: Long): AppProject? = dao.getProjectByIdDirect(id)

    suspend fun insertProject(project: AppProject): Long = dao.insertProject(project)

    suspend fun updateProject(project: AppProject) = dao.updateProject(project)

    suspend fun deleteProject(project: AppProject) = dao.deleteProject(project)

    suspend fun deleteProjectById(id: Long) = dao.deleteProjectById(id)

    suspend fun seedInitialProjectsIfNeeded() {
        val count = dao.getProjectCount()
        if (count == 0) {
            val templates = TemplateProvider.getAllTemplates()
            templates.take(3).forEach { t ->
                val project = AppProject(
                    name = t.title,
                    packageName = t.defaultPackageName,
                    versionName = "1.0.0",
                    versionCode = 1,
                    themeColorHex = t.themeColorHex,
                    iconEmoji = t.iconEmoji,
                    orientation = t.orientation,
                    isFullscreen = t.category == "Game",
                    mainHtmlFile = "index.html",
                    filesJson = mapToJson(t.files),
                    category = t.category
                )
                dao.insertProject(project)
            }
        }
    }

    companion object {
        fun mapToJson(map: Map<String, String>): String {
            val json = JSONObject()
            map.forEach { (k, v) ->
                json.put(k, v)
            }
            return json.toString()
        }

        fun jsonToMap(jsonStr: String): Map<String, String> {
            val map = mutableMapOf<String, String>()
            if (jsonStr.isBlank()) return map
            try {
                val json = JSONObject(jsonStr)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = json.optString(key, "")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return map
        }
    }
}
