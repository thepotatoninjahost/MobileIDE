package com.fuck13.mobileide.data.repository

import android.content.Context
import androidx.room.Room
import com.fuck13.mobileide.data.*
import com.fuck13.mobileide.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ProjectRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context,
        MobileIDEDatabase::class.java,
        "mobile_ide_db"
    ).build()
    
    private val projectDao = db.projectDao()
    private val fileMetadataDao = db.fileMetadataDao()
    private val codeIssueDao = db.codeIssueDao()
    private val snippetDao = db.codeSnippetDao()
    
    // Projects
    suspend fun getAllProjects(): List<Project> = projectDao.getAllProjects()
    
    suspend fun getProjectById(id: String): Project? = projectDao.getProjectById(id)
    
    suspend fun createProject(project: Project) = projectDao.insertProject(project)
    
    suspend fun updateProject(project: Project) = projectDao.updateProject(project)
    
    suspend fun deleteProject(id: String) = projectDao.deleteProjectById(id)
    
    suspend fun setProjectFavorite(id: String, favorite: Boolean) = 
        projectDao.setFavorite(id, favorite)
    
    // File metadata
    suspend fun getProjectFiles(projectId: String): List<FileMetadata> = 
        fileMetadataDao.getFilesByProject(projectId)
    
    suspend fun getFileMetadata(path: String): FileMetadata? = 
        fileMetadataDao.getFileByPath(path)
    
    suspend fun saveFileMetadata(file: FileMetadata) = 
        fileMetadataDao.insertFile(file)
    
    suspend fun deleteFileMetadata(path: String) = 
        fileMetadataDao.deleteFileByPath(path)
    
    suspend fun updateFileState(path: String, isOpen: Boolean, cursor: Int, scroll: Int) =
        fileMetadataDao.updateFileState(path, isOpen, cursor, scroll)
    
    // Code issues
    suspend fun getFileIssues(filePath: String): List<CodeIssue> = 
        codeIssueDao.getIssuesForFile(filePath)
    
    suspend fun getProjectErrorCount(projectId: String): Int = 
        codeIssueDao.getErrorCount(projectId)
    
    suspend fun getProjectWarningCount(projectId: String): Int = 
        codeIssueDao.getWarningCount(projectId)
    
    suspend fun saveIssues(issues: List<CodeIssue>) = 
        codeIssueDao.insertIssues(issues)
    
    suspend fun clearFileIssues(filePath: String) = 
        codeIssueDao.deleteIssuesForFile(filePath)
    
    // Snippets
    suspend fun getAllSnippets(): List<CodeSnippet> = 
        snippetDao.getAllSnippets()
    
    suspend fun getSnippetsByLanguage(language: FileType): List<CodeSnippet> = 
        snippetDao.getSnippetsByLanguage(language)
    
    suspend fun searchSnippets(query: String): List<CodeSnippet> = 
        snippetDao.searchSnippets(query)
    
    suspend fun saveSnippet(snippet: CodeSnippet) = 
        snippetDao.insertSnippet(snippet)
    
    suspend fun deleteSnippet(snippet: CodeSnippet) = 
        snippetDao.deleteSnippet(snippet)
    
    suspend fun incrementSnippetUsage(id: Long) = 
        snippetDao.incrementUsage(id)
}

class TerminalSessionRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context,
        MobileIDEDatabase::class.java,
        "mobile_ide_db"
    ).fallbackToDestructiveMigration().build()
    
    private val sessionDao = db.terminalSessionDao()
    
    suspend fun getAllSessions(): List<TerminalSession> = 
        sessionDao.getAllSessions()
    
    suspend fun getSessionById(id: String): TerminalSession? = 
        sessionDao.getSessionById(id)
    
    suspend fun getActiveSession(): TerminalSession? = 
        sessionDao.getActiveSession()
    
    suspend fun createSession(session: TerminalSession) = 
        sessionDao.insertSession(session)
    
    suspend fun updateSession(session: TerminalSession) = 
        sessionDao.updateSession(session)
    
    suspend fun deleteSession(session: TerminalSession) = 
        sessionDao.deleteSession(session)
    
    suspend fun deactivateAllSessions() = 
        sessionDao.deactivateAllSessions()
}
