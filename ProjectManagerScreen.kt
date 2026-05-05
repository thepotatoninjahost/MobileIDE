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
import com.fuck13.mobileide.data.model.*
import com.fuck13.mobileide.data.preferences.UserPreferences
import com.fuck13.mobileide.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf(ProjectViewMode.GRID) }
    
    // Load projects
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // Sample projects for now
            projects = listOf(
                Project(
                    id = "1",
                    name = "MobileIDE",
                    path = "/storage/emulated/0/MobileIDE",
                    description = "A full-featured mobile IDE",
                    projectType = ProjectType.KOTLIN_ANDROID,
                    isFavorite = true
                ),
                Project(
                    id = "2",
                    name = "MyApp",
                    path = "/storage/emulated/0/MyApp",
                    description = "Sample Android app",
                    projectType = ProjectType.KOTLIN_ANDROID,
                    isFavorite = false
                )
            )
            isLoading = false
        }
    }
    
    val filteredProjects = if (searchQuery.isNotBlank()) {
        projects.filter { it.name.contains(searchQuery, ignoreCase = true) }
    } else {
        projects
    }
    
    val favorites = filteredProjects.filter { it.isFavorite }
    val recent = filteredProjects.sortedByDescending { it.lastOpenedAt }.take(5)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top App Bar
        ProjectManagerTopAppBar(
            onBack = onBack,
            onSearch = { searchQuery = it },
            searchQuery = searchQuery,
            onViewModeToggle = { 
                viewMode = if (viewMode == ProjectViewMode.GRID) ProjectViewMode.LIST else ProjectViewMode.GRID 
            },
            viewMode = viewMode,
            onNewProject = { showNewProjectDialog = true },
            onImport = { showImportDialog = true }
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ElectricCyan)
            }
        } else if (projects.isEmpty()) {
            EmptyProjectsState(
                onNewProject = { showNewProjectDialog = true },
                onImport = { showImportDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Favorites section
                if (favorites.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Favorites",
                            icon = Icons.Default.Star,
                            color = SolarOrange
                        )
                    }
                    
                    item {
                        ProjectGrid(
                            projects = favorites,
                            viewMode = viewMode,
                            onProjectClick = { /* Open project */ },
                            onProjectLongClick = { selectedProject = it },
                            onFavoriteToggle = { project ->
                                scope.launch {
                                    // Update favorite status
                                }
                            }
                        )
                    }
                }
                
                // Recent section
                if (recent.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recent",
                            icon = Icons.Default.History,
                            color = ElectricCyan
                        )
                    }
                    
                    items(recent) { project ->
                        ProjectListItem(
                            project = project,
                            onClick = { /* Open project */ },
                            onLongClick = { selectedProject = project },
                            onFavoriteToggle = {
                                scope.launch {
                                    // Update favorite status
                                }
                            }
                        )
                    }
                }
                
                // All projects section
                item {
                    SectionHeader(
                        title = "All Projects",
                        icon = Icons.Default.Folder,
                        color = AccentProject
                    )
                }
                
                items(filteredProjects) { project ->
                    ProjectListItem(
                        project = project,
                        onClick = { /* Open project */ },
                        onLongClick = { selectedProject = project },
                        onFavoriteToggle = {
                            scope.launch {
                                // Update favorite status
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Project actions menu
    selectedProject?.let { project ->
        ProjectActionsMenu(
            project = project,
            onDismiss = { selectedProject = null },
            onOpen = { /* Open project */ },
            onOpenInFiles = { /* Navigate to files */ },
            onBuild = { /* Build project */ },
            onRun = { /* Run project */ },
            onRename = { /* Rename project */ },
            onDelete = { /* Delete project */ },
            onToggleFavorite = {
                scope.launch {
                    // Update favorite status
                }
                selectedProject = null
            }
        )
    }
    
    // New project dialog
    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name, type, path ->
                scope.launch(Dispatchers.IO) {
                    // Create new project
                }
                showNewProjectDialog = false
            }
        )
    }
    
    // Import dialog
    if (showImportDialog) {
        ImportProjectDialog(
            onDismiss = { showImportDialog = false },
            onImport = { path ->
                scope.launch(Dispatchers.IO) {
                    // Import project
                }
                showImportDialog = false
            }
        )
    }
}

enum class ProjectViewMode { GRID, LIST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectManagerTopAppBar(
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    searchQuery: String,
    onViewModeToggle: () -> Unit,
    viewMode: ProjectViewMode,
    onNewProject: () -> Unit,
    onImport: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Projects",
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
            IconButton(onClick = onImport) {
                Icon(Icons.Default.FolderOpen, "Import", tint = DarkOnSurface)
            }
            IconButton(onClick = onNewProject) {
                Icon(Icons.Default.Add, "New Project", tint = ElectricCyan)
            }
            IconButton(onClick = onViewModeToggle) {
                Icon(
                    if (viewMode == ProjectViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                    "Toggle view",
                    tint = DarkOnSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkSurface.copy(alpha = 0.95f)
        )
    )
    
    // Search bar
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearch,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search projects...") },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = DarkOnSurfaceVariant) },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearch("") }) {
                    Icon(Icons.Default.Clear, "Clear", tint = DarkOnSurfaceVariant)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ElectricCyan,
            unfocusedBorderColor = DarkSurfaceVariant,
            cursorColor = ElectricCyan
        )
    )
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = DarkOnBackground,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ProjectGrid(
    projects: List<Project>,
    viewMode: ProjectViewMode,
    onProjectClick: (Project) -> Unit,
    onProjectLongClick: (Project) -> Unit,
    onFavoriteToggle: (Project) -> Unit
) {
    if (viewMode == ProjectViewMode.GRID) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(projects) { project ->
                ProjectGridCard(
                    project = project,
                    onClick = { onProjectClick(project) },
                    onLongClick = { onProjectLongClick(project) },
                    onFavoriteClick = { onFavoriteToggle(project) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectGridCard(
    project: Project,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, AccentProject.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // Project type icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            AccentProject.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (project.projectType) {
                            ProjectType.KOTLIN_ANDROID -> Icons.Default.Android
                            ProjectType.JAVA_ANDROID -> Icons.Default.Android
                            ProjectType.FLUTTER -> Icons.Default.FlutterDash
                            ProjectType.PYTHON -> Icons.Default.Code
                            ProjectType.WEB -> Icons.Default.Web
                            else -> Icons.Default.Folder
                        },
                        contentDescription = null,
                        tint = AccentProject,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = DarkOnBackground,
                    maxLines = 1
                )
                
                Text(
                    text = project.projectType.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkOnSurfaceVariant
                )
            }
            
            // Favorite button
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (project.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (project.isFavorite) SolarOrange else DarkOnSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectListItem(
    project: Project,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Project icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        AccentProject.copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (project.projectType) {
                        ProjectType.KOTLIN_ANDROID -> Icons.Default.Android
                        ProjectType.JAVA_ANDROID -> Icons.Default.Android
                        ProjectType.FLUTTER -> Icons.Default.FlutterDash
                        ProjectType.PYTHON -> Icons.Default.Code
                        ProjectType.WEB -> Icons.Default.Web
                        else -> Icons.Default.Folder
                    },
                    contentDescription = null,
                    tint = AccentProject,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Project info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkOnBackground
                    )
                    if (project.hasUncommittedChanges) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(VividPink, CircleShape)
                        )
                    }
                }
                
                Text(
                    text = project.description.ifEmpty { project.projectType.displayName },
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkOnSurfaceVariant,
                    maxLines = 1
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (project.gitBranch != null) {
                        Text(
                            text = project.gitBranch,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                    }
                    Text(
                        text = "Modified ${formatRelativeTime(project.updatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkOnSurfaceVariant
                    )
                }
            }
            
            // Favorite button
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (project.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (project.isFavorite) SolarOrange else DarkOnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyProjectsState(
    onNewProject: () -> Unit,
    onImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.FolderOff,
            contentDescription = null,
            tint = DarkOnSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No projects yet",
            style = MaterialTheme.typography.headlineSmall,
            color = DarkOnBackground
        )
        
        Text(
            text = "Create a new project or import an existing one to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = DarkOnSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onImport,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FolderOpen, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import")
            }
            
            Button(
                onClick = onNewProject,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Project")
            }
        }
    }
}

@Composable
private fun ProjectActionsMenu(
    project: Project,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onOpenInFiles: () -> Unit,
    onBuild: () -> Unit,
    onRun: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(project.name) },
        text = {
            Column {
                TextButton(onClick = onOpen) {
                    Icon(Icons.Default.OpenInNew, null, tint = ElectricCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open", color = DarkOnBackground)
                }
                TextButton(onClick = onOpenInFiles) {
                    Icon(Icons.Default.Folder, null, tint = AccentProject)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open in Files", color = DarkOnBackground)
                }
                TextButton(onClick = onBuild) {
                    Icon(Icons.Default.Build, null, tint = SolarOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Build", color = DarkOnBackground)
                }
                TextButton(onClick = onRun) {
                    Icon(Icons.Default.PlayArrow, null, tint = NeonGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run", color = DarkOnBackground)
                }
                Divider(color = DarkSurfaceVariant)
                TextButton(onClick = onToggleFavorite) {
                    Icon(
                        if (project.isFavorite) Icons.Default.StarOff else Icons.Default.Star,
                        null,
                        tint = SolarOrange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (project.isFavorite) "Remove from Favorites" else "Add to Favorites", color = DarkOnBackground)
                }
                TextButton(onClick = onRename) {
                    Icon(Icons.Default.Edit, null, tint = DarkOnSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rename", color = DarkOnBackground)
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = ErrorRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete", color = ErrorRed)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, type: ProjectType, path: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ProjectType.KOTLIN_ANDROID) }
    var path by remember { mutableStateOf("/storage/emulated/0") }
    var showPathPicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Project") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Project type", color = DarkOnSurface)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ProjectType.entries) { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.displayName) },
                            leadingIcon = {
                                Icon(
                                    when (type) {
                                        ProjectType.KOTLIN_ANDROID -> Icons.Default.Android
                                        ProjectType.JAVA_ANDROID -> Icons.Default.Android
                                        ProjectType.FLUTTER -> Icons.Default.FlutterDash
                                        ProjectType.PYTHON -> Icons.Default.Code
                                        ProjectType.WEB -> Icons.Default.Web
                                        else -> Icons.Default.Folder
                                    },
                                    null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
                
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Location") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showPathPicker = true }) {
                            Icon(Icons.Default.FolderOpen, "Browse")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name, selectedType, path) }
            ) {
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
private fun ImportProjectDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var path by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Project") },
        text = {
            Column {
                Text(
                    "Select a project directory to import",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkOnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Project path") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (path.isNotBlank()) onImport(path) }) {
                Text("Import", color = ElectricCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
