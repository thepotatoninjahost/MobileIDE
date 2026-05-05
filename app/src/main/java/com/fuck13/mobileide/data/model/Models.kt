package com.fuck13.mobileide.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

// File types supported by the IDE
enum class FileType(val extension: String, val displayName: String, val icon: String) {
    KOTLIN("kt", "Kotlin", "code"),
    JAVA("java", "Java", "code"),
    XML("xml", "XML", "markup"),
    JSON("json", "JSON", "data"),
    GRADLE("gradle", "Gradle", "config"),
    MARKDOWN("md", "Markdown", "doc"),
    TEXT("txt", "Text", "text"),
    PYTHON("py", "Python", "code"),
    JAVASCRIPT("js", "JavaScript", "code"),
    TYPESCRIPT("ts", "TypeScript", "code"),
    HTML("html", "HTML", "markup"),
    CSS("css", "CSS", "style"),
    UNKNOWN("", "Unknown", "file");

    companion object {
        fun fromExtension(ext: String): FileType {
            return entries.find { it.extension.equals(ext, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

// Project entity - stored in Room database
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey
    val id: String,
    val name: String,
    val path: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val projectType: ProjectType = ProjectType.KOTLIN_ANDROID,
    val configuration: String = "{}", // JSON config
    val gitBranch: String? = null,
    val hasUncommittedChanges: Boolean = false
)

enum class ProjectType(val displayName: String, val icon: String, val defaultStructure: List<String>) {
    KOTLIN_ANDROID("Kotlin Android", "android", listOf(
        "app/src/main/java",
        "app/src/main/res",
        "app/src/main/AndroidManifest.xml",
        "build.gradle.kts",
        "settings.gradle.kts"
    )),
    JAVA_ANDROID("Java Android", "android", listOf(
        "app/src/main/java",
        "app/src/main/res",
        "app/src/main/AndroidManifest.xml",
        "build.gradle"
    )),
    KOTLIN_JVM("Kotlin JVM", "java", listOf(
        "src/main/kotlin",
        "src/main/resources",
        "build.gradle.kts"
    )),
    PYTHON("Python", "python", listOf(
        "src",
        "requirements.txt",
        "README.md"
    )),
    WEB("Web (HTML/CSS/JS)", "web", listOf(
        "index.html",
        "styles.css",
        "script.js"
    )),
    FLUTTER("Flutter", "flutter", listOf(
        "lib",
        "pubspec.yaml"
    )),
    REACT_NATIVE("React Native", "react", listOf(
        "src",
        "package.json"
    )),
    EMPTY("Empty Project", "folder", emptyList())
}

// File metadata - stored in Room for quick access
@Entity(tableName = "file_metadata")
data class FileMetadata(
    @PrimaryKey
    val path: String,
    val name: String,
    val extension: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val projectId: String? = null,
    val isOpen: Boolean = false,
    val cursorPosition: Int = 0,
    val scrollPosition: Int = 0,
    val encoding: String = "UTF-8"
)

// Code error/warning - for error detection
@Entity(tableName = "code_issues")
data class CodeIssue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val projectId: String,
    val message: String,
    val severity: IssueSeverity,
    val line: Int,
    val column: Int,
    val endLine: Int,
    val endColumn: Int,
    val ruleId: String? = null,
    val suggestion: String? = null,
    val fixAvailable: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class IssueSeverity {
    ERROR, WARNING, INFO, HINT
}

// Terminal session
@Entity(tableName = "terminal_sessions")
data class TerminalSession(
    @PrimaryKey
    val id: String,
    val name: String,
    val workingDirectory: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = false,
    val shellPath: String = "/system/bin/sh",
    val environmentJson: String = "{}"
)

// Build configuration
@Entity(tableName = "build_configs")
data class BuildConfig(
    @PrimaryKey
    val projectId: String,
    val buildType: String = "debug",
    val minifyEnabled: Boolean = false,
    val proguardEnabled: Boolean = false,
    val lastBuildTime: Long? = null,
    val lastBuildStatus: BuildStatus? = null,
    val buildOutputPath: String? = null,
    val buildErrors: Int = 0,
    val buildWarnings: Int = 0
)

enum class BuildStatus {
    SUCCESS, FAILED, RUNNING, CANCELLED
}

// Code snippet - for quick code insertion
@Entity(tableName = "code_snippets")
data class CodeSnippet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val language: FileType,
    val code: String,
    val description: String = "",
    val category: String = "General",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val usageCount: Int = 0
)

// Git status for a file
data class GitFileStatus(
    val path: String,
    val status: GitStatus
)

enum class GitStatus {
    UNTRACKED, MODIFIED, STAGED, COMMITTED, IGNORED
}
