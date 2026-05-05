package com.fuck13.mobileide.editor

import com.fuck13.mobileide.data.model.CodeIssue
import com.fuck13.mobileide.data.model.FileType
import com.fuck13.mobileide.data.model.IssueSeverity
import java.util.regex.Pattern

interface ErrorDetector {
    val language: FileType
    fun detectErrors(code: String, filePath: String, projectId: String): List<CodeIssue>
    fun suggestFix(issue: CodeIssue, code: String): FixSuggestion?
}

data class FixSuggestion(
    val title: String,
    val description: String,
    val replacement: String,
    val startOffset: Int,
    val endOffset: Int
)

abstract class BaseErrorDetector : ErrorDetector {
    protected fun createIssue(
        message: String,
        filePath: String,
        projectId: String,
        line: Int,
        column: Int,
        severity: IssueSeverity,
        suggestion: String? = null,
        fixAvailable: Boolean = false
    ): CodeIssue {
        return CodeIssue(
            filePath = filePath,
            projectId = projectId,
            message = message,
            severity = severity,
            line = line,
            column = column,
            endLine = line,
            endColumn = column + 1,
            suggestion = suggestion,
            fixAvailable = fixAvailable
        )
    }
    
    protected fun getLineAndColumn(code: String, offset: Int): Pair<Int, Int> {
        val lines = code.substring(0, offset).split("\n")
        val line = lines.size
        val column = lines.last().length + 1
        return Pair(line, column)
    }
    
    protected fun getOffset(code: String, line: Int, column: Int): Int {
        val lines = code.split("\n")
        var offset = 0
        for (i in 0 until line - 1) {
            offset += lines[i].length + 1 // +1 for newline
        }
        offset += column - 1
        return offset
    }
}

class KotlinErrorDetector : BaseErrorDetector() {
    override val language = FileType.KOTLIN
    
    override fun detectErrors(code: String, filePath: String, projectId: String): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        val lines = code.split("\n")
        
        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            
            // Check for common issues
            
            // Missing semicolon warning (Kotlin doesn't need them)
            if (line.trim().endsWith(";") && !line.contains("for") && !line.contains("import")) {
                issues.add(createIssue(
                    message = "Unnecessary semicolon",
                    filePath = filePath,
                    projectId = projectId,
                    line = lineNumber,
                    column = line.lastIndexOf(";") + 1,
                    severity = IssueSeverity.INFO,
                    suggestion = "Remove unnecessary semicolon",
                    fixAvailable = true
                ))
            }
            
            // Var could be val
            val varPattern = Pattern.compile("\\bvar\\s+(\\w+)\\s*=")
            val varMatcher = varPattern.matcher(line)
            if (varMatcher.find()) {
                val varName = varMatcher.group(1)
                // Check if variable is reassigned (simplified check)
                if (!code.contains("$varName\\s*=[^=]".toRegex())) {
                    issues.add(createIssue(
                        message = "Variable '$varName' could be 'val' instead of 'var'",
                        filePath = filePath,
                        projectId = projectId,
                        line = lineNumber,
                        column = varMatcher.start() + 1,
                        severity = IssueSeverity.HINT,
                        suggestion = "Change to 'val' if not reassigned",
                        fixAvailable = true
                    ))
                }
            }
            
            // Unnecessary !! operator
            if (line.contains("!!") && !line.contains("!=")) {
                issues.add(createIssue(
                    message = "Unnecessary non-null assertion (!!) - consider using safe call (?.)",
                    filePath = filePath,
                    projectId = projectId,
                    line = lineNumber,
                    column = line.indexOf("!!") + 1,
                    severity = IssueSeverity.WARNING,
                    suggestion = "Use ?. or ?: instead of !!",
                    fixAvailable = false
                ))
            }
            
            // Missing visibility modifier
            val classPattern = Pattern.compile("^\\s*class\\s+")
            if (classPattern.matcher(line).find() && !line.contains("public") && !line.contains("private") && !line.contains("internal")) {
                issues.add(createIssue(
                    message = "Consider adding explicit visibility modifier",
                    filePath = filePath,
                    projectId = projectId,
                    line = lineNumber,
                    column = classPattern.matcher(line).start() + 1,
                    severity = IssueSeverity.HINT,
                    suggestion = "Add 'public', 'private', or 'internal'"
                ))
            }
            
            // Trailing whitespace
            if (line != line.trimEnd() && line.isNotEmpty()) {
                issues.add(createIssue(
                    message = "Trailing whitespace",
                    filePath = filePath,
                    projectId = projectId,
                    line = lineNumber,
                    column = line.trimEnd().length + 1,
                    severity = IssueSeverity.INFO,
                    suggestion = "Remove trailing whitespace",
                    fixAvailable = true
                ))
            }
            
            // Long lines
            if (line.length > 120) {
                issues.add(createIssue(
                    message = "Line exceeds 120 characters (${line.length} chars)",
                    filePath = filePath,
                    projectId = projectId,
                    line = lineNumber,
                    column = 121,
                    severity = IssueSeverity.WARNING,
                    suggestion = "Consider breaking into multiple lines"
                ))
            }
            
            // TODO comments
            if (line.contains("TODO", ignoreCase = true)) {
                issues.add(createIssue(
                    message = "TODO found - remember to address this",
                    filePath = filePath,
                    projectId = projectId,
                    line = lineNumber,
                    column = line.indexOf("TODO", ignoreCase = true) + 1,
                    severity = IssueSeverity.INFO,
                    suggestion = null
                ))
            }
            
            // FIXME comments
            if (line.contains("FIXME", ignoreCase = true)) {
                issues.add(createIssue(
                    message = "FIXME found - this needs to be fixed",
                    filePath = filePath,
                    projectId = projectId,
                    line = lineNumber,
                    column = line.indexOf("FIXME", ignoreCase = true) + 1,
                    severity = IssueSeverity.WARNING,
                    suggestion = null
                ))
            }
        }
        
        // Check for matching brackets
        issues.addAll(checkBracketMatching(code, filePath, projectId))
        
        // Check for matching quotes
        issues.addAll(checkQuoteMatching(code, filePath, projectId))
        
        return issues
    }
    
    private fun checkBracketMatching(code: String, filePath: String, projectId: String): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        val stack = mutableListOf<Pair<Char, Int>>() // (bracket, line)
        val lines = code.split("\n")
        val bracketPairs = mapOf(')' to '(', ']' to '[', '}' to '{')
        
        lines.forEachIndexed { lineIndex, line ->
            line.forEachIndexed { charIndex, char ->
                when (char) {
                    '(', '[', '{' -> stack.add(Pair(char, lineIndex + 1))
                    ')', ']', '}' -> {
                        val expected = bracketPairs[char]
                        if (stack.isEmpty() || stack.last().first != expected) {
                            issues.add(createIssue(
                                message = "Unmatched closing bracket '$char'",
                                filePath = filePath,
                                projectId = projectId,
                                line = lineIndex + 1,
                                column = charIndex + 1,
                                severity = IssueSeverity.ERROR,
                                suggestion = "Add matching opening bracket"
                            ))
                        } else {
                            stack.removeLast()
                        }
                    }
                }
            }
        }
        
        // Check for unclosed brackets
        stack.forEach { (bracket, line) ->
            issues.add(createIssue(
                message = "Unclosed bracket '$bracket'",
                filePath = filePath,
                projectId = projectId,
                line = line,
                column = 1,
                severity = IssueSeverity.ERROR,
                suggestion = "Add matching closing bracket"
            ))
        }
        
        return issues
    }
    
    private fun checkQuoteMatching(code: String, filePath: String, projectId: String): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        val lines = code.split("\n")
        
        lines.forEachIndexed { lineIndex, line ->
            var inString = false
            var stringStart = -1
            var stringStartLine = -1
            var i = 0
            
            while (i < line.length) {
                val char = line[i]
                
                when {
                    char == '\\' && inString -> i++ // Skip escaped char
                    char == '"' && !inString -> {
                        inString = true
                        stringStart = i
                        stringStartLine = lineIndex + 1
                    }
                    char == '"' && inString -> {
                        inString = false
                    }
                }
                i++
            }
            
            if (inString) {
                issues.add(createIssue(
                    message = "Unclosed string literal",
                    filePath = filePath,
                    projectId = projectId,
                    line = stringStartLine,
                    column = stringStart + 1,
                    severity = IssueSeverity.ERROR,
                    suggestion = "Add closing quote"
                ))
            }
        }
        
        return issues
    }
    
    override fun suggestFix(issue: CodeIssue, code: String): FixSuggestion? {
        val lines = code.split("\n")
        val line = lines.getOrNull(issue.line - 1) ?: return null
        
        return when {
            issue.message == "Trailing whitespace" -> {
                FixSuggestion(
                    title = "Remove trailing whitespace",
                    description = "Remove whitespace at end of line",
                    replacement = line.trimEnd(),
                    startOffset = getOffset(code, issue.line, 1),
                    endOffset = getOffset(code, issue.line, line.length + 1)
                )
            }
            issue.message.contains("could be 'val'") -> {
                val newLine = line.replaceFirst("\\bvar\\b".toRegex(), "val")
                FixSuggestion(
                    title = "Change to val",
                    description = "Replace var with val",
                    replacement = newLine,
                    startOffset = getOffset(code, issue.line, 1),
                    endOffset = getOffset(code, issue.line, line.length + 1)
                )
            }
            issue.message == "Unnecessary semicolon" -> {
                val newLine = line.trimEnd().trimEnd(';')
                FixSuggestion(
                    title = "Remove semicolon",
                    description = "Remove unnecessary semicolon",
                    replacement = newLine,
                    startOffset = getOffset(code, issue.line, 1),
                    endOffset = getOffset(code, issue.line, line.length + 1)
                )
            }
            else -> null
        }
    }
}

class JavaErrorDetector : BaseErrorDetector() {
    override val language = FileType.JAVA
    
    override fun detectErrors(code: String, filePath: String, projectId: String): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        val lines = code.split("\n")
        
        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            
            // Check for missing semicolons
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && 
                !trimmed.endsWith(";") && 
                !trimmed.endsWith("{") && 
                !trimmed.endsWith("}") && 
                !trimmed.endsWith(",") &&
                !trimmed.startsWith("//") &&
                !trimmed.startsWith("/*") &&
                !trimmed.startsWith("*") &&
                !trimmed.startsWith("package") &&
                !trimmed.startsWith("import") &&
                !trimmed.contains("class ") &&
                !trimmed.contains("interface ") &&
                !trimmed.contains("enum ")) {
                // Could be a statement needing semicolon
                if (trimmed.contains("=") || 
                    trimmed.contains("return") ||
                    trimmed.matches(".*\\w+\\s*\\(.*\\).*".toRegex())) {
                    issues.add(createIssue(
                        message = "Possibly missing semicolon",
                        filePath = filePath,
                        projectId = projectId,
                        line = lineNumber,
                        column = line.length,
                        severity = IssueSeverity.WARNING,
                        suggestion = "Add semicolon at end of statement"
                    ))
                }
            }
            
            // Unused imports (simplified)
            if (line.trim().startsWith("import ")) {
                val importMatch = "import\\s+[\\w.]+\\.(\\w+);?".toRegex().find(line)
                if (importMatch != null) {
                    val className = importMatch.groupValues[1]
                    if (!code.contains("\\b$className\\b".toRegex())) {
                        issues.add(createIssue(
                            message = "Unused import: $className",
                            filePath = filePath,
                            projectId = projectId,
                            line = lineNumber,
                            column = 1,
                            severity = IssueSeverity.WARNING,
                            suggestion = "Remove unused import",
                            fixAvailable = true
                        ))
                    }
                }
            }
        }
        
        // Bracket and quote matching (reuse from Kotlin)
        issues.addAll(KotlinErrorDetector().let { detector ->
            detector.detectErrors(code, filePath, projectId).filter { 
                it.message.contains("bracket") || it.message.contains("quote") || it.message.contains("string")
            }
        })
        
        return issues
    }
    
    override fun suggestFix(issue: CodeIssue, code: String): FixSuggestion? {
        if (issue.message.startsWith("Unused import")) {
            val lines = code.split("\n")
            val line = lines.getOrNull(issue.line - 1) ?: return null
            return FixSuggestion(
                title = "Remove unused import",
                description = "Remove the unused import statement",
                replacement = "",
                startOffset = getOffset(code, issue.line, 1),
                endOffset = getOffset(code, issue.line, line.length + 1)
            )
        }
        return null
    }
}

class PythonErrorDetector : BaseErrorDetector() {
    override val language = FileType.PYTHON
    
    override fun detectErrors(code: String, filePath: String, projectId: String): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        val lines = code.split("\n")
        
        var previousIndent = 0
        
        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            
            // Check indentation consistency
            val currentIndent = line.takeWhile { it == ' ' }.length
            if (currentIndent % 4 != 0 && line.trim().isNotEmpty()) {
                issues.add(createIssue(
                    message = "Inconsistent indentation - use multiples of 4 spaces",
                    filePath = filePath,
                    projectId = projectId,
                    line = lineNumber,
                    column = 1,
                    severity = IssueSeverity.WARNING,
                    suggestion = "Adjust indentation to be consistent"
                ))
            }
            
            // Check for tabs vs spaces
            if (line.contains("\t") && line.contains(" ")) {
                issues.add(createIssue(
                    message = "Mixed tabs and spaces in indentation",
                    filePath = filePath,
                    projectId = projectId,
                    line = lineNumber,
                    column = 1,
                    severity = IssueSeverity.ERROR,
                    suggestion = "Use only spaces or only tabs"
                ))
            }
            
            // Check for missing colons after statements that need them
            val colonRequiredPatterns = listOf("if ", "elif ", "else", "for ", "while ", "def ", "class ", "try", "except", "finally", "with ")
            val trimmed = line.trim()
            for (pattern in colonRequiredPatterns) {
                if (trimmed.startsWith(pattern) && !trimmed.endsWith(":")) {
                    issues.add(createIssue(
                        message = "Missing colon after '${pattern.trim()}' statement",
                        filePath = filePath,
                        projectId = projectId,
                        line = lineNumber,
                        column = line.length,
                        severity = IssueSeverity.ERROR,
                        suggestion = "Add colon at end of statement"
                    ))
                }
            }
            
            previousIndent = currentIndent
        }
        
        // Check for unmatched brackets
        val bracketStack = mutableListOf<Pair<Char, Int>>()
        val bracketPairs = mapOf(')' to '(', ']' to '[', '}' to '{')
        
        lines.forEachIndexed { lineIndex, line ->
            var inString = false
            var stringChar = ' '
            var i = 0
            
            while (i < line.length) {
                val char = line[i]
                
                // Track string state
                if ((char == '"' || char == '\'') && (i == 0 || line[i-1] != '\\')) {
                    if (!inString) {
                        inString = true
                        stringChar = char
                    } else if (char == stringChar) {
                        inString = false
                    }
                }
                
                if (!inString) {
                    when (char) {
                        '(', '[', '{' -> bracketStack.add(Pair(char, lineIndex + 1))
                        ')', ']', '}' -> {
                            val expected = bracketPairs[char]
                            if (bracketStack.isEmpty() || bracketStack.last().first != expected) {
                                issues.add(createIssue(
                                    message = "Unmatched closing bracket '$char'",
                                    filePath = filePath,
                                    projectId = projectId,
                                    line = lineIndex + 1,
                                    column = i + 1,
                                    severity = IssueSeverity.ERROR
                                ))
                            } else {
                                bracketStack.removeLast()
                            }
                        }
                    }
                }
                i++
            }
        }
        
        bracketStack.forEach { (bracket, line) ->
            issues.add(createIssue(
                message = "Unclosed bracket '$bracket'",
                filePath = filePath,
                projectId = projectId,
                line = line,
                column = 1,
                severity = IssueSeverity.ERROR
            ))
        }
        
        return issues
    }
    
    override fun suggestFix(issue: CodeIssue, code: String): FixSuggestion? {
        return null // Python fixes are more complex, would need full AST
    }
}

object ErrorDetectorRegistry {
    private val detectors = mapOf<FileType, ErrorDetector>(
        FileType.KOTLIN to KotlinErrorDetector(),
        FileType.JAVA to JavaErrorDetector(),
        FileType.PYTHON to PythonErrorDetector()
    )
    
    fun getDetector(fileType: FileType): ErrorDetector? = detectors[fileType]
    
    fun detectAllErrors(code: String, filePath: String, projectId: String, fileType: FileType): List<CodeIssue> {
        val detector = detectors[fileType] ?: return emptyList()
        return detector.detectErrors(code, filePath, projectId)
    }
    
    fun getFixSuggestion(issue: CodeIssue, code: String, fileType: FileType): FixSuggestion? {
        val detector = detectors[fileType] ?: return null
        return detector.suggestFix(issue, code)
    }
}
