package com.fuck13.mobileide.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.fuck13.mobileide.data.model.*
import com.fuck13.mobileide.data.preferences.UserPreferences
import com.fuck13.mobileide.editor.*
import com.fuck13.mobileide.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

data class EditorState(
    val filePath: String = "",
    val fileName: String = "Untitled",
    val content: String = "",
    val fileType: FileType = FileType.KOTLIN,
    val cursorLine: Int = 1,
    val cursorColumn: Int = 1,
    val isModified: Boolean = false,
    val issues: List<CodeIssue> = emptyList(),
    val isLoading: Boolean = false,
    val showMinimap: Boolean = true,
    val showIssues: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Preferences
    val fontSize by UserPreferences.fontSize.collectAsState(initial = 14)
    val showLineNumbers by UserPreferences.showLineNumbers.collectAsState(initial = true)
    val wordWrap by UserPreferences.wordWrap.collectAsState(initial = true)
    val autoSave by UserPreferences.autoSave.collectAsState(initial = true)
    
    // Editor state
    var state by remember { mutableStateOf(EditorState()) }
    var searchText by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showCompletion by remember { mutableStateOf(false) }
    
    // Debounced auto-save
    LaunchedEffect(state.content, state.isModified) {
        if (autoSave && state.isModified && state.filePath.isNotEmpty()) {
            delay(2000)
            // Save file
            withContext(Dispatchers.IO) {
                try {
                    File(state.filePath).writeText(state.content)
                    state = state.copy(isModified = false)
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }
    
    // Detect errors when content changes
    LaunchedEffect(state.content) {
        if (state.filePath.isNotEmpty()) {
            delay(500) // Debounce
            val issues = ErrorDetectorRegistry.detectAllErrors(
                state.content,
                state.filePath,
                "default",
                state.fileType
            )
            state = state.copy(issues = issues)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top App Bar
        EditorTopAppBar(
            fileName = state.fileName,
            isModified = state.isModified,
            showSearch = showSearch,
            searchText = searchText,
            onSearchTextChange = { searchText = it },
            onBack = onBack,
            onSave = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        File(state.filePath).writeText(state.content)
                        state = state.copy(isModified = false)
                    }
                }
            },
            onSearchToggle = { showSearch = !showSearch },
            onUndo = { /* TODO */ },
            onRedo = { /* TODO */ },
            onMenuClick = { /* TODO */ }
        )
        
        // Error/Issue indicator bar
        AnimatedVisibility(
            visible = state.issues.isNotEmpty(),
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            IssueBar(
                issues = state.issues,
                onClick = { state = state.copy(showIssues = true) }
            )
        }
        
        // Main Editor Area
        Box(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Line numbers column
                if (showLineNumbers) {
                    LineNumbersColumn(
                        lineCount = state.content.lines().size,
                        currentLine = state.cursorLine,
                        errorLines = state.issues.filter { it.severity == IssueSeverity.ERROR }.map { it.line },
                        warningLines = state.issues.filter { it.severity == IssueSeverity.WARNING }.map { it.line }
                    )
                }
                
                // Code editor
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    CodeEditor(
                        content = state.content,
                        fileType = state.fileType,
                        fontSize = fontSize,
                        wordWrap = wordWrap,
                        onContentChange = { newContent ->
                            state = state.copy(
                                content = newContent,
                                isModified = true
                            )
                        },
                        onCursorChange = { line, column ->
                            state = state.copy(
                                cursorLine = line,
                                cursorColumn = column
                            )
                        }
                    )
                }
                
                // Minimap
                if (state.showMinimap) {
                    Minimap(
                        content = state.content,
                        cursorLine = state.cursorLine
                    )
                }
            }
        }
        
        // Bottom status bar
        EditorStatusBar(
            cursorLine = state.cursorLine,
            cursorColumn = state.cursorColumn,
            fileType = state.fileType,
            issueCount = state.issues.count { it.severity == IssueSeverity.ERROR },
            warningCount = state.issues.count { it.severity == IssueSeverity.WARNING }
        )
    }
    
    // Issue panel overlay
    if (state.showIssues) {
        IssuePanel(
            issues = state.issues,
            onDismiss = { state = state.copy(showIssues = false) },
            onIssueClick = { issue ->
                // Scroll to issue line
                state = state.copy(showIssues = false)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopAppBar(
    fileName: String,
    isModified: Boolean,
    showSearch: Boolean,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSearchToggle: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onMenuClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkOnBackground
                )
                if (isModified) {
                    Text(
                        text = " •",
                        color = ElectricCyan,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DarkOnSurface)
            }
        },
        actions = {
            IconButton(onClick = onSave) {
                Icon(Icons.Default.Save, "Save", tint = DarkOnSurface)
            }
            IconButton(onClick = onUndo) {
                Icon(Icons.Default.Undo, "Undo", tint = DarkOnSurface)
            }
            IconButton(onClick = onRedo) {
                Icon(Icons.Default.Redo, "Redo", tint = DarkOnSurface)
            }
            IconButton(onClick = onSearchToggle) {
                Icon(Icons.Default.Search, "Search", tint = if (showSearch) ElectricCyan else DarkOnSurface)
            }
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, "Menu", tint = DarkOnSurface)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkSurface.copy(alpha = 0.95f)
        )
    )
    
    AnimatedVisibility(
        visible = showSearch,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search in file...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                IconButton(onClick = { onSearchTextChange("") }) {
                    Icon(Icons.Default.Clear, "Clear")
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = DarkSurfaceVariant,
                cursorColor = ElectricCyan
            )
        )
    }
}

@Composable
private fun LineNumbersColumn(
    lineCount: Int,
    currentLine: Int,
    errorLines: List<Int>,
    warningLines: List<Int>
) {
    val scrollState = rememberLazyListState()
    
    Column(
        modifier = Modifier
            .width(48.dp)
            .fillMaxHeight()
            .background(DarkSurface.copy(alpha = 0.3f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.End
    ) {
        repeat(lineCount) { index ->
            val lineNumber = index + 1
            val isError = lineNumber in errorLines
            val isWarning = lineNumber in warningLines
            val isCurrent = lineNumber == currentLine
            
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .fillMaxWidth()
                    .background(
                        when {
                            isCurrent -> ElectricCyan.copy(alpha = 0.1f)
                            isError -> ErrorRed.copy(alpha = 0.1f)
                            isWarning -> WarningYellow.copy(alpha = 0.1f)
                            else -> Color.Transparent
                        }
                    )
                    .padding(end = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = lineNumber.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = when {
                        isCurrent -> ElectricCyan
                        isError -> ErrorRed
                        isWarning -> WarningYellow
                        else -> DarkOnSurfaceVariant.copy(alpha = 0.6f)
                    }
                )
            }
        }
    }
}

@Composable
private fun CodeEditor(
    content: String,
    fileType: FileType,
    fontSize: Int,
    wordWrap: Boolean,
    onContentChange: (String) -> Unit,
    onCursorChange: (Int, Int) -> Unit
) {
    val textFieldValue = remember(content) {
        mutableStateOf(TextFieldValue(AnnotatedString(content)))
    }
    
    val highlightedText = remember(content, fileType) {
        SyntaxHighlighter.highlight(content, fileType)
    }
    
    // Update text field with highlighted content
    LaunchedEffect(highlightedText) {
        textFieldValue.value = TextFieldValue(
            annotatedString = highlightedText,
            selection = textFieldValue.value.selection
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 8.dp)
    ) {
        BasicTextField(
            value = textFieldValue.value,
            onValueChange = { newValue ->
                textFieldValue.value = newValue
                onContentChange(newValue.text)
                
                // Calculate cursor position
                val textBeforeCursor = newValue.text.substring(0, newValue.selection.min)
                val lines = textBeforeCursor.split("\n")
                val line = lines.size
                val column = lines.last().length + 1
                onCursorChange(line, column)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp),
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                color = Color.White,
                lineHeight = (fontSize + 6).sp
            ),
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false
            ),
            codeStyling = true
        )
    }
}

@Composable
private fun Minimap(content: String, cursorLine: Int) {
    val lines = remember(content) { content.lines() }
    
    Canvas(
        modifier = Modifier
            .width(60.dp)
            .fillMaxHeight()
            .background(DarkSurface.copy(alpha = 0.3f))
    ) {
        val lineHeight = 2f
        val charWidth = 1f
        
        lines.forEachIndexed { index, line ->
            val y = index * lineHeight + 4f
            
            // Draw line content as colored blocks
            line.forEachIndexed { charIndex, char ->
                val color = when {
                    char.isWhitespace() -> Color.Transparent
                    else -> SyntaxVariable.copy(alpha = 0.3f)
                }
                
                drawRect(
                    color = color,
                    topLeft = Offset(charIndex * charWidth + 4f, y),
                    size = Size(charWidth, lineHeight)
                )
            }
        }
        
        // Draw cursor line indicator
        val cursorY = cursorLine * lineHeight + 4f
        drawRect(
            color = ElectricCyan.copy(alpha = 0.2f),
            topLeft = Offset(0f, cursorY - lineHeight),
            size = Size(60.dp.toPx(), lineHeight)
        )
    }
}

@Composable
private fun IssueBar(issues: List<CodeIssue>, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = DarkSurfaceVariant.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = if (issues.any { it.severity == IssueSeverity.ERROR }) ErrorRed else WarningYellow,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${issues.count { it.severity == IssueSeverity.ERROR }} errors, ${issues.count { it.severity == IssueSeverity.WARNING }} warnings",
                style = MaterialTheme.typography.bodySmall,
                color = DarkOnSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "View issues",
                tint = DarkOnSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditorStatusBar(
    cursorLine: Int,
    cursorColumn: Int,
    fileType: FileType,
    issueCount: Int,
    warningCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = fileType.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan
                )
                if (issueCount > 0) {
                    Text(
                        text = "$issueCount errors",
                        style = MaterialTheme.typography.labelSmall,
                        color = ErrorRed
                    )
                }
                if (warningCount > 0) {
                    Text(
                        text = "$warningCount warnings",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningYellow
                    )
                }
            }
            Text(
                text = "Ln $cursorLine, Col $cursorColumn",
                style = MaterialTheme.typography.labelSmall,
                color = DarkOnSurfaceVariant
            )
        }
    }
}

@Composable
private fun IssuePanel(
    issues: List<CodeIssue>,
    onDismiss: () -> Unit,
    onIssueClick: (CodeIssue) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = DarkSurface
        ) {
            Column {
                // Handle bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(DarkOnSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                    )
                }
                
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Problems",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkOnBackground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = DarkOnSurface)
                    }
                }
                
                // Issues list
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(issues) { issue ->
                        IssueItem(
                            issue = issue,
                            onClick = { onIssueClick(issue) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueItem(issue: CodeIssue, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = when (issue.severity) {
                    IssueSeverity.ERROR -> Icons.Default.Error
                    IssueSeverity.WARNING -> Icons.Default.Warning
                    IssueSeverity.INFO -> Icons.Default.Info
                    IssueSeverity.HINT -> Icons.Default.Lightbulb
                },
                contentDescription = null,
                tint = when (issue.severity) {
                    IssueSeverity.ERROR -> ErrorRed
                    IssueSeverity.WARNING -> WarningYellow
                    IssueSeverity.INFO -> InfoBlue
                    IssueSeverity.HINT -> NeonGreen
                },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = issue.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkOnBackground
                )
                Text(
                    text = "Line ${issue.line}, Column ${issue.column}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkOnSurfaceVariant
                )
            }
            if (issue.fixAvailable) {
                FilledTonalButton(
                    onClick = { /* Apply fix */ },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ElectricCyan.copy(alpha = 0.2f)
                    )
                ) {
                    Text("Fix", color = ElectricCyan)
                }
            }
        }
    }
}
