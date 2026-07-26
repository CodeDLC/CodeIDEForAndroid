package com.example.data

import kotlinx.coroutines.flow.Flow

class IdeRepository(private val db: AppDatabase) {
    val recentProjects: Flow<List<ProjectEntity>> = db.projectDao().getAllProjects()
    val editorSettings: Flow<EditorSettingsEntity?> = db.settingsDao().getSettings()

    suspend fun getSettings(): EditorSettingsEntity {
        return db.settingsDao().getSettingsDirect() ?: EditorSettingsEntity()
    }

    suspend fun saveSettings(settings: EditorSettingsEntity) {
        db.settingsDao().saveSettings(settings)
    }

    suspend fun openProject(project: ProjectEntity) {
        db.projectDao().insertProject(project.copy(lastOpenedTime = System.currentTimeMillis()))
    }

    suspend fun deleteProject(project: ProjectEntity) {
        db.projectDao().deleteProject(project)
    }

    fun getOpenTabs(projectId: String): Flow<List<OpenTabEntity>> = db.tabDao().getOpenTabs(projectId)

    suspend fun addOpenTab(tab: OpenTabEntity) {
        db.tabDao().insertTab(tab)
    }

    suspend fun closeTab(projectId: String, filePath: String) {
        db.tabDao().closeTab(projectId, filePath)
    }

    fun getBuildLogs(projectId: String): Flow<List<BuildLogEntity>> = db.buildLogDao().getBuildLogs(projectId)

    suspend fun addBuildLog(log: BuildLogEntity) {
        db.buildLogDao().insertBuildLog(log)
    }
}
