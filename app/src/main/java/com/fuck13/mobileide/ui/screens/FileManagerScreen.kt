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
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.fuck13.mobileide.data.model.FileType
import com.fuck13.mobileide.data.preferences.UserPreferences
import com.fuck13.mobileide.ui.theme.*
import kotlinx.coroutines.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

data class FileItem(
    val file: File,
    val name: String = file.name,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isDirectory) 0 else file.length(),
    val lastModified: Long = file.lastModified(),
    val extension: String = if (file.isDirectory) "" else file.extension
)

sealed class FileSort {
    object Name : FileSort()
    object Size : FileSort()
    object Date : FileSort()
    object Type : FileSort()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    var currentPath by remember { mutableStateOf("/storage/emulated/0") }
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var selectedFiles by remember { mutableStateOf<Set<File>>(emptySet()) }
    var isMultiSelect by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<Pair<File, String>?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Set<File>?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf<FileSort>(FileSort.Name) }
    var sortAscending by remember { mutableStateOf(true) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var showHidden by remember { mutableStateOf(false) }
    var clipboardFiles by remember { mutableStateOf<Set<File>>(emptySet()) }
    var clipboardOperation by remember { mutableStateOf<ClipboardOp?>(null) }
    
    // Load files
    LaunchedEffect(currentPath, sortBy, sortAscending, showHidden) {
        withContext(Dispatchers.IO) {
            try {
                val dir = File(currentPath)
                val loadedFiles = dir.listFiles()
                    ?.filter { showHidden || !it.name.startsWith(".") }
                    ?.map { FileItem(it) }
                    ?.sortedWith(when (sortBy) {
                        FileSort.Name -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() }
                        FileSort.Size -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.size }
                        FileSort.Date -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.lastModified }
                        FileSort.Type -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.extension }
                    })
                    ?.let { if (sortAscending) it else it.reversed() }
                    ?: emptyList()
                files = loadedFiles
            } catch (e: Exception) {
                files = emptyList()
            }
        }
    }
    
    // Add recent file when navigating
    LaunchedEffect(currentPath) {
        UserPreferences.addRecentFile(currentPath)
    }
    
    val filteredFiles = if (searchQuery.isNotBlank()) {
        files.filter { it.name.contains(searchQuery, ignoreCase = true) }
    } else {
        files
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top App Bar
        FileManagerTopAppBar(
            currentPath = currentPath,
            onBack = onBack,
            onNavigateUp = {
                val parent = File(currentPath).parentFile
                if (parent != null && parent.canRead()) {
                    currentPath = parent.absolutePath
                }
            },
            onSearch = { searchQuery = it },
            searchQuery = searchQuery,
            onViewModeToggle = {
                viewMode = if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
            },
            viewMode = viewMode,
            onSortClick = { /* Show sort menu */ },
            onNewFileClick = { showNewFileDialog = true },
            onNewFolderClick = { showNewFolderDialog = true }
        )
        
        // Breadcrumb path
        BreadcrumbPath(
            path = currentPath,
            onNavigate = { currentPath = it }
        )
        
        // Quick actions bar
        if (isMultiSelect && selectedFiles.isNotEmpty()) {
            SelectionActionBar(
                count = selectedFiles.size,
                onCopy = {
                    clipboardFiles = selectedFiles
                    clipboardOperation = ClipboardOp.COPY
                },
                onCut = {
                    clipboardFiles = selectedFiles
                    clipboardOperation = ClipboardOp.CUT
                },
                onDelete = {
                    showDeleteConfirm = selectedFiles
                },
                onRename = {
                    if (selectedFiles.size == 1) {
                        showRenameDialog = Pair(selectedFiles.first(), selectedFiles.first().name)
                    }
                },
                onSelectAll = {
                    selectedFiles = files.map { it.file }.toSet()
                },
                onClearSelection = {
                    selectedFiles = emptySet()
                    isMultiSelect = false
                }
            )
        } else if (clipboardFiles.isNotEmpty()) {
            ClipboardBar(
                count = clipboardFiles.size,
                operation = clipboardOperation,
                onPaste = {
                    scope.launch(Dispatchers.IO) {
                        clipboardFiles.forEach { sourceFile ->
                            val targetFile = File(currentPath, sourceFile.name)
                            when (clipboardOperation) {
                                ClipboardOp.COPY -> sourceFile.copyRecursively(targetFile, overwrite = true)
                                ClipboardOp.CUT -> {
                                    sourceFile.copyRecursively(targetFile, overwrite = true)
                                    sourceFile.deleteRecursively()
                                }
                                null -> {}
                            }
                        }
                        clipboardFiles = emptySet()
                        clipboardOperation = null
                    }
                },
                onClear = {
                    clipboardFiles = emptySet()
                    clipboardOperation = null
                }
            )
        }
        
        // File list
        Box(modifier = Modifier.weight(1f)) {
            if (filteredFiles.isEmpty()) {
                EmptyState(
                    message = if (searchQuery.isNotBlank()) "No files match your search" else "This folder is empty",
                    icon = Icons.Outlined.FolderOff
                )
            } else {
                when (viewMode) {
                    ViewMode.LIST -> FileListView(
                        files = filteredFiles,
                        selectedFiles = selectedFiles,
                        isMultiSelect = isMultiSelect,
                        onFileClick = { fileItem ->
                            if (isMultiSelect) {
                                selectedFiles = if (fileItem.file in selectedFiles) {
                                    selectedFiles - fileItem.file
                                } else {
                                    selectedFiles + fileItem.file
                                }
                            } else if (fileItem.isDirectory) {
                                currentPath = fileItem.file.absolutePath
                            } else {
                                // Open in editor
                            }
                        },
                        onFileLongClick = { fileItem ->
                            if (!isMultiSelect) {
                                isMultiSelect = true
                                selectedFiles = setOf(fileItem.file)
                            }
                        }
                    )
                    ViewMode.GRID -> FileGridView(
                        files = filteredFiles,
                        selectedFiles = selectedFiles,
                        isMultiSelect = isMultiSelect,
                        onFileClick = { fileItem ->
                            if (isMultiSelect) {
                                selectedFiles = if (fileItem.file in selectedFiles) {
                                    selectedFiles - fileItem.file
                                } else {
                                    selectedFiles + fileItem.file
                                }
                            } else if (fileItem.isDirectory) {
                                currentPath = fileItem.file.absolutePath
                            }
                        },
                        onFileLongClick = { fileItem ->
                            if (!isMultiSelect) {
                                isMultiSelect = true
                                selectedFiles = setOf(fileItem.file)
                            }
                        }
                    )
                }
            }
        }
        
        // Storage info bar
        StorageInfoBar(path = currentPath)
    }
    
    // Dialogs
    if (showNewFileDialog) {
        NewFileDialog(
            onDismiss = { showNewFileDialog = false },
            onCreate = { name ->
                scope.launch(Dispatchers.IO) {
                    File(currentPath, name).createNewFile()
                }
                showNewFileDialog = false
            }
        )
    }
    
    if (showNewFolderDialog) {
        NewFolderDialog(
            onDismiss = { showNewFolderDialog = false },
            onCreate = { name ->
                scope.launch(Dispatchers.IO) {
                    File(currentPath, name).mkdirs()
                }
                showNewFolderDialog = false
            }
        )
    }
    
    showRenameDialog?.let { (file, currentName) ->
        RenameDialog(
            currentName = currentName,
            onDismiss = { showRenameDialog = null },
            onRename = { newName ->
                scope.launch(Dispatchers.IO) {
                    file.renameTo(File(file.parent, newName))
                }
                showRenameDialog = null
            }
        )
    }
    
    showDeleteConfirm?.let { filesToDelete ->
        DeleteConfirmDialog(
            count = filesToDelete.size,
            onDismiss = { showDeleteConfirm = null },
            onConfirm = {
                scope.launch(Dispatchers.IO) {
                    filesToDelete.forEach { it.deleteRecursively() }
                }
                selectedFiles = emptySet()
                isMultiSelect = false
                showDeleteConfirm = null
            }
        )
    }
}

enum class ViewMode { LIST, GRID }
enum class ClipboardOp { COPY, CUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileManagerTopAppBar(
    currentPath: String,
    onBack: () -> Unit,
    onNavigateUp: () -> Unit,
    onSearch: (String) -> Unit,
    searchQuery: String,
    onViewModeToggle: () -> Unit,
    viewMode: ViewMode,
    onSortClick: () -> Unit,
    onNewFileClick: () -> Unit,
    onNewFolderClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Files",
                style = MaterialTheme.typography.titleLarge,
                color = DarkOnBackground
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DarkOnSurface)
            }
        },
        actions = {
            IconButton(onClick = onNavigateUp) {
                Icon(Icons.Default.ArrowUpward, "Navigate up", tint = DarkOnSurface)
            }
            IconButton(onClick = onViewModeToggle) {
                Icon(
                    if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                    "Toggle view",
                    tint = DarkOnSurface
                )
            }
            IconButton(onClick = onNewFolderClick) {
                Icon(Icons.Default.CreateNewFolder, "New folder", tint = AccentProject)
            }
            IconButton(onClick = onNewFileClick) {
                Icon(Icons.Default.NoteAdd, "New file", tint = AccentEditor)
            }
            IconButton(onClick = onSortClick) {
                Icon(Icons.Default.Sort, "Sort", tint = DarkOnSurface)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkSurface.copy(alpha = 0.95f)
        )
    )
}

@Composable
private fun BreadcrumbPath(path: String, onNavigate: (String) -> Unit) {
    val segments = path.split("/").filter { it.isNotEmpty() }
    
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        item {
            TextButton(
                onClick = { onNavigate("/") },
                colors = TextButtonDefaults.textButtonColors(
                    contentColor = ElectricCyan
                )
            ) {
                Icon(Icons.Default.Home, null, modifier = Modifier.size(16.dp))
            }
        }
        
        itemsIndexed(segments) { index, segment ->
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = DarkOnSurfaceVariant,
                modifier = Modifier.size(16.dp).align(Alignment.CenterVertically)
            )
            TextButton(
                onClick = {
                    val newPath = "/" + segments.take(index + 1).joinToString("/")
                    onNavigate(newPath)
                },
                colors = TextButtonDefaults.textButtonColors(
                    contentColor = if (index == segments.lastIndex) DarkOnBackground else DarkOnSurfaceVariant
                )
            ) {
                Text(
                    segment,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SelectionActionBar(
    count: Int,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, "Copy", tint = DarkOnSurface)
                }
                IconButton(onClick = onCut) {
                    Icon(Icons.Default.ContentCut, "Cut", tint = DarkOnSurface)
                }
                IconButton(onClick = onRename) {
                    Icon(Icons.Default.Edit, "Rename", tint = DarkOnSurface)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = ErrorRed)
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = onSelectAll) {
                    Text("Select all", color = ElectricCyan)
                }
                TextButton(onClick = onClearSelection) {
                    Text("Cancel", color = DarkOnSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ClipboardBar(
    count: Int,
    operation: ClipboardOp?,
    onPaste: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (operation == ClipboardOp.CUT) ErrorRed.copy(alpha = 0.1f) else NeonGreen.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${operation?.name?.lowercase()?.capitalize()} $count item${if (count > 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (operation == ClipboardOp.CUT) ErrorRed else NeonGreen
            )
            Row {
                TextButton(onClick = onPaste) {
                    Text("Paste", color = ElectricCyan)
                }
                TextButton(onClick = onClear) {
                    Text("Cancel", color = DarkOnSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun FileListView(
    files: List<FileItem>,
    selectedFiles: Set<File>,
    isMultiSelect: Boolean,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(files) { fileItem ->
            FileListItem(
                fileItem = fileItem,
                isSelected = fileItem.file in selectedFiles,
                isMultiSelect = isMultiSelect,
                onClick = { onFileClick(fileItem) },
                onLongClick = { onFileLongClick(fileItem) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListItem(
    fileItem: FileItem,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val fileType = FileType.fromExtension(fileItem.extension)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) ElectricCyan.copy(alpha = 0.15f) else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox for multi-select
        if (isMultiSelect) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = ElectricCyan,
                    uncheckedColor = DarkOnSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    when {
                        fileItem.isDirectory -> AccentProject.copy(alpha = 0.15f)
                        fileType == FileType.KOTLIN -> SyntaxType.copy(alpha = 0.15f)
                        fileType == FileType.JAVA -> SyntaxClass.copy(alpha = 0.15f)
                        fileType == FileType.XML -> SyntaxVariable.copy(alpha = 0.15f)
                        else -> DarkSurfaceVariant.copy(alpha = 0.3f)
                    },
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    fileItem.isDirectory -> Icons.Outlined.Folder
                    fileType == FileType.KOTLIN -> Icons.Outlined.Code
                    fileType == FileType.JAVA -> Icons.Outlined.Code
                    fileType == FileType.XML -> Icons.Outlined.DataObject
                    fileType == FileType.JSON -> Icons.Outlined.DataObject
                    fileType == FileType.MARKDOWN -> Icons.Outlined.Description
                    else -> Icons.Outlined.InsertDriveFile
                },
                contentDescription = null,
                tint = when {
                    fileItem.isDirectory -> AccentProject
                    fileType == FileType.KOTLIN -> SyntaxType
                    fileType == FileType.JAVA -> SyntaxClass
                    fileType == FileType.XML -> SyntaxVariable
                    else -> DarkOnSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Name and info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileItem.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (fileItem.isDirectory) AccentProject else DarkOnBackground,
                maxLines = 1
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!fileItem.isDirectory) {
                    Text(
                        text = formatSize(fileItem.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant
                    )
                }
                Text(
                    text = formatDate(fileItem.lastModified),
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkOnSurfaceVariant
                )
            }
        }
        
        // Arrow for directories
        if (fileItem.isDirectory) {
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = DarkOnSurfaceVariant
            )
        }
    }
}

@Composable
private fun FileGridView(
    files: List<FileItem>,
    selectedFiles: Set<File>,
    isMultiSelect: Boolean,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(files) { fileItem ->
            FileGridItem(
                fileItem = fileItem,
                isSelected = fileItem.file in selectedFiles,
                isMultiSelect = isMultiSelect,
                onClick = { onFileClick(fileItem) },
                onLongClick = { onFileLongClick(fileItem) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileGridItem(
    fileItem: FileItem,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val fileType = FileType.fromExtension(fileItem.extension)
    
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .background(if (isSelected) ElectricCyan.copy(alpha = 0.15f) else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when {
                    fileItem.isDirectory -> Icons.Outlined.Folder
                    fileType == FileType.KOTLIN -> Icons.Outlined.Code
                    fileType == FileType.JAVA -> Icons.Outlined.Code
                    else -> Icons.Outlined.InsertDriveFile
                },
                contentDescription = null,
                tint = if (fileItem.isDirectory) AccentProject else ElectricCyan,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = fileItem.name,
                style = MaterialTheme.typography.bodySmall,
                color = DarkOnBackground,
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DarkOnSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = DarkOnSurfaceVariant
        )
    }
}

@Composable
private fun StorageInfoBar(path: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = File(path).let { 
                    if (it.exists()) formatSize(it.freeSpace) + " free" else "Unknown"
                },
                style = MaterialTheme.typography.labelSmall,
                color = DarkOnSurfaceVariant
            )
            Text(
                text = "${File(path).listFiles()?.size ?: 0} items",
                style = MaterialTheme.typography.labelSmall,
                color = DarkOnSurfaceVariant
            )
        }
    }
}

// Dialogs
@Composable
private fun NewFileDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New File") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("File name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name) }) {
                Text("Create", color = ElectricCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NewFolderDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Folder name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name) }) {
                Text("Create", color = ElectricCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RenameDialog(currentName: String, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("New name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank() && name != currentName) onRename(name) }) {
                Text("Rename", color = ElectricCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteConfirmDialog(count: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${if (count > 1) "$count items" else "item"}?") },
        text = { Text("This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = ErrorRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Utility functions
private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
