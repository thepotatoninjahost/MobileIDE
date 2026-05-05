package com.fuck13.mobileide.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.fuck13.mobileide.data.preferences.UserPreferences
import com.fuck13.mobileide.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*

data class TerminalLine(
    val text: String,
    val type: LineType = LineType.OUTPUT,
    val timestamp: Long = System.currentTimeMillis()
)

enum class LineType {
    INPUT, OUTPUT, ERROR, SUCCESS, SYSTEM
}

data class TerminalSession(
    val id: String,
    val name: String,
    val workingDirectory: String,
    val lines: List<TerminalLine> = emptyList(),
    val isRunning: Boolean = false,
    val currentProcess: Process? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Preferences
    val fontSize by UserPreferences.terminalFontSize.collectAsState(initial = 12)
    val colorScheme by UserPreferences.terminalColorScheme.collectAsState(initial = "dracula")
    
    // Terminal state
    var sessions by remember { mutableStateOf(listOf(TerminalSession("1", "Main Terminal", "/storage/emulated/0"))) }
    var activeSessionId by remember { mutableStateOf("1") }
    var inputText by remember { mutableStateOf("") }
    var commandHistory by remember { mutableStateOf(listOf<String>()) }
    var historyIndex by remember { mutableStateOf(-1) }
    var showSessionList by remember { mutableStateOf(false) }
    
    val activeSession = sessions.find { it.id == activeSessionId } ?: sessions.first()
    
    // Color scheme for terminal
    val terminalColors = when (colorScheme) {
        "dracula" -> TerminalColors(
            background = Color(0xFF282A36),
            foreground = Color(0xFFF8F8F2),
            cyan = Color(0xFF8BE9FD),
            green = Color(0xFF50FA7B),
            yellow = Color(0xFFF1FA8C),
            red = Color(0xFFFF5555),
            blue = Color(0xFF6272A4),
            magenta = Color(0xFFFF79C6)
        )
        else -> TerminalColors(
            background = DarkBackground,
            foreground = Color.White,
            cyan = ElectricCyan,
            green = NeonGreen,
            yellow = WarningYellow,
            red = ErrorRed,
            blue = InfoBlue,
            magenta = VividPink
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(terminalColors.background)
    ) {
        // Top App Bar
        TerminalTopAppBar(
            sessionName = activeSession.name,
            isRunning = activeSession.isRunning,
            onBack = onBack,
            onNewSession = {
                val newId = (sessions.maxOfOrNull { it.id.toInt() } ?: 0 + 1).toString()
                sessions = sessions + TerminalSession(newId, "Terminal $newId", activeSession.workingDirectory)
                activeSessionId = newId
            },
            onSessionsClick = { showSessionList = true },
            onClear = {
                sessions = sessions.map { 
                    if (it.id == activeSessionId) it.copy(lines = emptyList()) 
                    else it 
                }
            },
            onStop = {
                // Stop running process
                activeSession.currentProcess?.destroy()
                sessions = sessions.map { 
                    if (it.id == activeSessionId) it.copy(isRunning = false) 
                    else it 
                }
            }
        )
        
        // Terminal output
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            TerminalOutput(
                lines = activeSession.lines,
                fontSize = fontSize,
                colors = terminalColors,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Input field
        TerminalInput(
            inputText = inputText,
            onInputTextChange = { inputText = it },
            fontSize = fontSize,
            colors = terminalColors,
            workingDirectory = activeSession.workingDirectory,
            onCommandSubmit = { command ->
                if (command.isNotBlank()) {
                    // Add to history
                    commandHistory = (commandHistory + command).takeLast(100)
                    historyIndex = -1
                    
                    // Execute command
                    scope.launch(Dispatchers.IO) {
                        executeCommand(command, activeSession) { newSession ->
                            sessions = sessions.map { 
                                if (it.id == activeSessionId) newSession 
                                else it 
                            }
                        }
                    }
                    
                    inputText = ""
                }
            },
            onNavigateHistory = { direction ->
                if (direction == HistoryDirection.UP && historyIndex < commandHistory.size - 1) {
                    historyIndex++
                    inputText = commandHistory[commandHistory.size - 1 - historyIndex]
                } else if (direction == HistoryDirection.DOWN && historyIndex > 0) {
                    historyIndex--
                    inputText = commandHistory[commandHistory.size - 1 - historyIndex]
                } else if (direction == HistoryDirection.DOWN && historyIndex == 0) {
                    historyIndex = -1
                    inputText = ""
                }
            }
        )
    }
    
    // Session list overlay
    if (showSessionList) {
        SessionListOverlay(
            sessions = sessions,
            activeSessionId = activeSessionId,
            onSelectSession = { id ->
                activeSessionId = id
                showSessionList = false
            },
            onCloseSession = { id ->
                if (sessions.size > 1) {
                    sessions = sessions.filter { it.id != id }
                    if (activeSessionId == id) {
                        activeSessionId = sessions.first().id
                    }
                }
            },
            onDismiss = { showSessionList = false }
        )
    }
}

data class TerminalColors(
    val background: Color,
    val foreground: Color,
    val cyan: Color,
    val green: Color,
    val yellow: Color,
    val red: Color,
    val blue: Color,
    val magenta: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TerminalTopAppBar(
    sessionName: String,
    isRunning: Boolean,
    onBack: () -> Unit,
    onNewSession: () -> Unit,
    onSessionsClick: () -> Unit,
    onClear: () -> Unit,
    onStop: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sessionName,
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkOnBackground
                )
                if (isRunning) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(NeonGreen, CircleShape)
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
            if (isRunning) {
                IconButton(onClick = onStop) {
                    Icon(Icons.Default.Stop, "Stop", tint = ErrorRed)
                }
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Default.DeleteSweep, "Clear", tint = DarkOnSurface)
            }
            IconButton(onClick = onNewSession) {
                Icon(Icons.Default.Add, "New Session", tint = ElectricCyan)
            }
            IconButton(onClick = onSessionsClick) {
                Icon(Icons.Default.ViewCarousel, "Sessions", tint = DarkOnSurface)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkSurface.copy(alpha = 0.95f)
        )
    )
}

@Composable
private fun TerminalOutput(
    lines: List<TerminalLine>,
    fontSize: Int,
    colors: TerminalColors,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = modifier
            .background(colors.background)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        items(lines) { line ->
            TerminalLineText(
                line = line,
                fontSize = fontSize,
                colors = colors
            )
        }
    }
}

@Composable
private fun TerminalLineText(
    line: TerminalLine,
    fontSize: Int,
    colors: TerminalColors
) {
    val color = when (line.type) {
        LineType.INPUT -> colors.cyan
        LineType.OUTPUT -> colors.foreground
        LineType.ERROR -> colors.red
        LineType.SUCCESS -> colors.green
        LineType.SYSTEM -> colors.magenta
    }
    
    Text(
        text = line.text,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp,
            color = color
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TerminalInput(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    fontSize: Int,
    colors: TerminalColors,
    workingDirectory: String,
    onCommandSubmit: (String) -> Unit,
    onNavigateHistory: (HistoryDirection) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface.copy(alpha = 0.9f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prompt indicator
            Text(
                text = "$ ",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize.sp,
                    color = colors.green
                )
            )
            
            // Input field
            BasicTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize.sp,
                    color = colors.foreground
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        onCommandSubmit(inputText)
                        keyboardController?.hide()
                    }
                ),
                onKeyEvent = { keyEvent ->
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                            onNavigateHistory(HistoryDirection.UP)
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                            onNavigateHistory(HistoryDirection.DOWN)
                            true
                        }
                        else -> false
                    }
                }
            )
            
            // Submit button
            IconButton(
                onClick = { onCommandSubmit(inputText) }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Execute",
                    tint = ElectricCyan
                )
            }
        }
    }
}

enum class HistoryDirection { UP, DOWN }

private suspend fun executeCommand(
    command: String,
    session: TerminalSession,
    updateSession: (TerminalSession) -> Unit
) {
    // Add input line
    updateSession(session.copy(
        lines = session.lines + TerminalLine("$ ${command}", LineType.INPUT),
        isRunning = true
    ))
    
    try {
        // Parse command and arguments
        val parts = command.split(" ")
        val cmd = parts.first()
        val args = parts.drop(1).toTypedArray()
        
        // Handle built-in commands
        when (cmd) {
            "cd" -> {
                val newDir = args.firstOrNull() ?: "/storage/emulated/0"
                val targetDir = File(session.workingDirectory, newDir).canonicalPath
                if (File(targetDir).exists() && File(targetDir).isDirectory) {
                    updateSession(session.copy(
                        lines = session.lines + TerminalLine("Changed directory to $targetDir", LineType.SUCCESS),
                        workingDirectory = targetDir,
                        isRunning = false
                    ))
                } else {
                    updateSession(session.copy(
                        lines = session.lines + TerminalLine("cd: no such file or directory: $newDir", LineType.ERROR),
                        isRunning = false
                    ))
                }
            }
            "clear" -> {
                updateSession(session.copy(
                    lines = emptyList(),
                    isRunning = false
                ))
            }
            "pwd" -> {
                updateSession(session.copy(
                    lines = session.lines + TerminalLine(session.workingDirectory, LineType.OUTPUT),
                    isRunning = false
                ))
            }
            "ls" -> {
                val dir = File(session.workingDirectory)
                val files = dir.listFiles()?.sortedBy { it.name } ?: emptyArray()
                val output = files.joinToString("\n") { file ->
                    if (file.isDirectory) "${file.name}/" else file.name
                }
                updateSession(session.copy(
                    lines = session.lines + TerminalLine(output.ifEmpty { "(empty directory)" }, LineType.OUTPUT),
                    isRunning = false
                ))
            }
            "echo" -> {
                updateSession(session.copy(
                    lines = session.lines + TerminalLine(args.joinToString(" "), LineType.OUTPUT),
                    isRunning = false
                ))
            }
            "help" -> {
                val helpText = """
                    MobileIDE Terminal v1.0
                    Built-in commands:
                      cd <dir>    - Change directory
                      ls         - List directory contents
                      pwd        - Print working directory
                      echo       - Print text
                      clear      - Clear terminal
                      help       - Show this help
                      exit       - Close terminal
                """.trimIndent()
                updateSession(session.copy(
                    lines = session.lines + TerminalLine(helpText, LineType.SYSTEM),
                    isRunning = false
                ))
            }
            "exit" -> {
                updateSession(session.copy(
                    lines = session.lines + TerminalLine("Goodbye!", LineType.SYSTEM),
                    isRunning = false
                ))
            }
            else -> {
                // Try to execute as shell command
                try {
                    val process = Runtime.getRuntime().exec(
                        command,
                        arrayOf("PATH=/system/bin:/system/xbin"),
                        File(session.workingDirectory)
                    )
                    
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                    
                    val output = mutableListOf<String>()
                    val errors = mutableListOf<String>()
                    
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        output.add(line!!)
                    }
                    while (errorReader.readLine().also { line = it } != null) {
                        errors.add(line!!)
                    }
                    
                    process.waitFor()
                    
                    val newLines = session.lines.toMutableList()
                    if (output.isNotEmpty()) {
                        newLines.add(TerminalLine(output.joinToString("\n"), LineType.OUTPUT))
                    }
                    if (errors.isNotEmpty()) {
                        newLines.add(TerminalLine(errors.joinToString("\n"), LineType.ERROR))
                    }
                    
                    updateSession(session.copy(
                        lines = newLines,
                        isRunning = false
                    ))
                } catch (e: Exception) {
                    updateSession(session.copy(
                        lines = session.lines + TerminalLine("${cmd}: command not found or error: ${e.message}", LineType.ERROR),
                        isRunning = false
                    ))
                }
            }
        }
    } catch (e: Exception) {
        updateSession(session.copy(
            lines = session.lines + TerminalLine("Error: ${e.message}", LineType.ERROR),
            isRunning = false
        ))
    }
}

@Composable
private fun SessionListOverlay(
    sessions: List<TerminalSession>,
    activeSessionId: String,
    onSelectSession: (String) -> Unit,
    onCloseSession: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.9f)
                .padding(top = 80.dp)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Terminal Sessions",
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkOnBackground
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                sessions.forEach { session ->
                    SessionItem(
                        session = session,
                        isActive = session.id == activeSessionId,
                        onSelect = { onSelectSession(session.id) },
                        onClose = { onCloseSession(session.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SessionItem(
    session: TerminalSession,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) ElectricCyan.copy(alpha = 0.1f) else DarkSurfaceVariant
        ),
        border = if (isActive) BorderStroke(1.dp, ElectricCyan) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DarkOnBackground
                )
                Text(
                    text = session.workingDirectory,
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkOnSurfaceVariant,
                    maxLines = 1
                )
                if (session.isRunning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(NeonGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Running",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen
                        )
                    }
                }
            }
            
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Close", tint = DarkOnSurfaceVariant)
            }
        }
    }
}
