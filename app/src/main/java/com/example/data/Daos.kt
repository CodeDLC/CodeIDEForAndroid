package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastOpenedTime DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
}

@Dao
interface TabDao {
    @Query("SELECT * FROM open_tabs WHERE projectId = :projectId")
    fun getOpenTabs(projectId: String): Flow<List<OpenTabEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: OpenTabEntity)

    @Query("DELETE FROM open_tabs WHERE projectId = :projectId AND filePath = :filePath")
    suspend fun closeTab(projectId: String, filePath: String)

    @Query("DELETE FROM open_tabs WHERE projectId = :projectId")
    suspend fun clearTabsForProject(projectId: String)
}

@Dao
interface BuildLogDao {
    @Query("SELECT * FROM build_logs WHERE projectId = :projectId ORDER BY timestamp DESC LIMIT 20")
    fun getBuildLogs(projectId: String): Flow<List<BuildLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildLog(log: BuildLogEntity)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM editor_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<EditorSettingsEntity?>

    @Query("SELECT * FROM editor_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): EditorSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: EditorSettingsEntity)
}
