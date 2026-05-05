package com.fuck13.mobileide.data

import androidx.room.*
import com.fuck13.mobileide.data.model.*

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastOpenedAt DESC")
    suspend fun getAllProjects(): List<Project>
    
    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): Project?
    
    @Query("SELECT * FROM projects WHERE isFavorite = 1 ORDER BY name")
    suspend fun getFavoriteProjects(): List<Project>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)
    
    @Update
    suspend fun updateProject(project: Project)
    
    @Delete
    suspend fun deleteProject(project: Project)
    
    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
    
    @Query("UPDATE projects SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun updateLastOpened(id: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE projects SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)
}

@Dao
interface FileMetadataDao {
    @Query("SELECT * FROM file_metadata WHERE projectId = :projectId ORDER BY name")
    suspend fun getFilesByProject(projectId: String): List<FileMetadata>
    
    @Query("SELECT * FROM file_metadata WHERE path = :path")
    suspend fun getFileByPath(path: String): FileMetadata?
    
    @Query("SELECT * FROM file_metadata WHERE isOpen = 1")
    suspend fun getOpenFiles(): List<FileMetadata>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileMetadata)
    
    @Update
    suspend fun updateFile(file: FileMetadata)
    
    @Delete
    suspend fun deleteFile(file: FileMetadata)
    
    @Query("DELETE FROM file_metadata WHERE path = :path")
    suspend fun deleteFileByPath(path: String)
    
    @Query("DELETE FROM file_metadata WHERE projectId = :projectId")
    suspend fun deleteFilesByProject(projectId: String)
    
    @Query("UPDATE file_metadata SET isOpen = :open, cursorPosition = :cursor, scrollPosition = :scroll WHERE path = :path")
    suspend fun updateFileState(path: String, open: Boolean, cursor: Int, scroll: Int)
}

@Dao
interface CodeIssueDao {
    @Query("SELECT * FROM code_issues WHERE filePath = :filePath ORDER BY line, column")
    suspend fun getIssuesForFile(filePath: String): List<CodeIssue>
    
    @Query("SELECT * FROM code_issues WHERE projectId = :projectId AND severity = :severity")
    suspend fun getIssuesBySeverity(projectId: String, severity: IssueSeverity): List<CodeIssue>
    
    @Query("SELECT COUNT(*) FROM code_issues WHERE projectId = :projectId AND severity = 'ERROR'")
    suspend fun getErrorCount(projectId: String): Int
    
    @Query("SELECT COUNT(*) FROM code_issues WHERE projectId = :projectId AND severity = 'WARNING'")
    suspend fun getWarningCount(projectId: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssue(issue: CodeIssue)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssues(issues: List<CodeIssue>)
    
    @Query("DELETE FROM code_issues WHERE filePath = :filePath")
    suspend fun deleteIssuesForFile(filePath: String)
    
    @Query("DELETE FROM code_issues WHERE projectId = :projectId")
    suspend fun deleteIssuesForProject(projectId: String)
}

@Dao
interface TerminalSessionDao {
    @Query("SELECT * FROM terminal_sessions ORDER BY lastActiveAt DESC")
    suspend fun getAllSessions(): List<TerminalSession>
    
    @Query("SELECT * FROM terminal_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): TerminalSession?
    
    @Query("SELECT * FROM terminal_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): TerminalSession?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TerminalSession)
    
    @Update
    suspend fun updateSession(session: TerminalSession)
    
    @Delete
    suspend fun deleteSession(session: TerminalSession)
    
    @Query("UPDATE terminal_sessions SET isActive = 0")
    suspend fun deactivateAllSessions()
}

@Dao
interface CodeSnippetDao {
    @Query("SELECT * FROM code_snippets ORDER BY usageCount DESC, name")
    suspend fun getAllSnippets(): List<CodeSnippet>
    
    @Query("SELECT * FROM code_snippets WHERE language = :language ORDER BY usageCount DESC")
    suspend fun getSnippetsByLanguage(language: FileType): List<CodeSnippet>
    
    @Query("SELECT * FROM code_snippets WHERE category = :category ORDER BY name")
    suspend fun getSnippetsByCategory(category: String): List<CodeSnippet>
    
    @Query("SELECT * FROM code_snippets WHERE name LIKE :query OR description LIKE :query")
    suspend fun searchSnippets(query: String): List<CodeSnippet>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: CodeSnippet)
    
    @Update
    suspend fun updateSnippet(snippet: CodeSnippet)
    
    @Delete
    suspend fun deleteSnippet(snippet: CodeSnippet)
    
    @Query("UPDATE code_snippets SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsage(id: Long)
}

@Dao
interface BuildConfigDao {
    @Query("SELECT * FROM build_configs WHERE projectId = :projectId")
    suspend fun getBuildConfig(projectId: String): BuildConfig?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildConfig(config: BuildConfig)
    
    @Update
    suspend fun updateBuildConfig(config: BuildConfig)
}

@Database(
    entities = [
        Project::class,
        FileMetadata::class,
        CodeIssue::class,
        TerminalSession::class,
        CodeSnippet::class,
        BuildConfig::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MobileIDEDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun fileMetadataDao(): FileMetadataDao
    abstract fun codeIssueDao(): CodeIssueDao
    abstract fun terminalSessionDao(): TerminalSessionDao
    abstract fun codeSnippetDao(): CodeSnippetDao
    abstract fun buildConfigDao(): BuildConfigDao
}
