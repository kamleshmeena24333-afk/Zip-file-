package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppProject
import kotlinx.coroutines.flow.Flow

@Dao
interface AppProjectDao {
    @Query("SELECT * FROM app_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<AppProject>>

    @Query("SELECT * FROM app_projects WHERE id = :id LIMIT 1")
    fun getProjectById(id: Long): Flow<AppProject?>

    @Query("SELECT * FROM app_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectByIdDirect(id: Long): AppProject?

    @Query("SELECT COUNT(*) FROM app_projects")
    suspend fun getProjectCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: AppProject): Long

    @Update
    suspend fun updateProject(project: AppProject)

    @Delete
    suspend fun deleteProject(project: AppProject)

    @Query("DELETE FROM app_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)
}
