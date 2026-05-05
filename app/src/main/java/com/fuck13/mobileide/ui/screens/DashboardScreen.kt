package com.fuck13.mobileide.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.fuck13.mobileide.ui.theme.*
import kotlinx.coroutines.*

data class QuickAction(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToEditor: () -> Unit,
    onNavigateToFiles: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToTerminal: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientAnimation"
    )
    
    val gradientColors = listOf(
        DarkBackground,
        Color(0xFF0A1628),
        Color(0xFF0F1F35),
        DarkBackground
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(0f, animationProgress * 2000f),
                    end = Offset(2000f, animationProgress * 2000f + 2000f)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -it }
            ) {
                DashboardHeader()
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Quick Actions Grid
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(700, delayMillis = 200)) + scaleIn(tween(700, delayMillis = 200))
            ) {
                QuickActionsGrid(
                    actions = listOf(
                        QuickAction("Code Editor", "Write & edit code", Icons.Outlined.Code, AccentEditor, onNavigateToEditor),
                        QuickAction("File Manager", "Browse & manage files", Icons.Outlined.Folder, AccentProject, onNavigateToFiles),
                        QuickAction("Projects", "Your workspace", Icons.Outlined.Work, AccentTerminal, onNavigateToProjects),
                        QuickAction("Terminal", "Run commands", Icons.Outlined.Terminal, NeonGreen, onNavigateToTerminal)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Recent Files Section
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(900, delayMillis = 400)) + slideInVertically(tween(900, delayMillis = 400)) { it }
            ) {
                RecentFilesSection(
                    onFileClick = onNavigateToEditor
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom Navigation
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(500, delayMillis = 600))
            ) {
                BottomQuickBar(
                    onSettingsClick = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "MobileIDE",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    brush = Brush.linearGradient(
                        colors = listOf(ElectricCyan, NeonGreen)
                    )
                )
            )
            Text(
                text = "Your pocket development studio",
                style = MaterialTheme.typography.bodyMedium,
                color = DarkOnSurfaceVariant
            )
        }
        
        // Animated logo
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ElectricCyan.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Terminal,
                contentDescription = "Logo",
                tint = ElectricCyan,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun QuickActionsGrid(actions: List<QuickAction>) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            color = DarkOnSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(280.dp)
        ) {
            items(actions) { action ->
                QuickActionCard(action = action)
            }
        }
    }
}

@Composable
private fun QuickActionCard(action: QuickAction) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "scale"
    )
    
    Card(
        onClick = action.onClick,
        modifier = Modifier
            .aspectRatio(1.3f)
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.7f)
        ),
        border = BorderStroke(1.dp, action.color.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 8.dp
        ),
        interactionSource = remember { MutableInteractionSource() }.also { source ->
            LaunchedEffect(source) {
                source.collectInteractions { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> isPressed = true
                        is PressInteraction.Release -> isPressed = false
                        is PressInteraction.Cancel -> isPressed = false
                    }
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            action.color.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    tint = action.color,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkOnBackground,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = action.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkOnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecentFilesSection(onFileClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Files",
                style = MaterialTheme.typography.titleMedium,
                color = DarkOnSurface
            )
            TextButton(onClick = onFileClick) {
                Text("See all", color = ElectricCyan)
            }
        }
        
        // Sample recent files
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            items(5) { index ->
                RecentFileCard(
                    fileName = listOf("MainActivity.kt", "build.gradle", "strings.xml", "styles.xml", "App.kt")[index],
                    project = "MobileIDE",
                    onClick = onFileClick
                )
            }
        }
    }
}

@Composable
private fun RecentFileCard(fileName: String, project: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = when (fileName.substringAfterLast(".")) {
                    "kt" -> Icons.Outlined.Code
                    "xml" -> Icons.Outlined.DataObject
                    "gradle" -> Icons.Outlined.Settings
                    else -> Icons.Outlined.Description
                },
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = DarkOnBackground,
                maxLines = 1
            )
            Text(
                text = project,
                style = MaterialTheme.typography.bodySmall,
                color = DarkOnSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BottomQuickBar(onSettingsClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface.copy(alpha = 0.8f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                IconButton(onClick = { /* Search */ }) {
                    Icon(Icons.Outlined.Search, "Search", tint = DarkOnSurfaceVariant)
                }
                IconButton(onClick = { /* Git */ }) {
                    Icon(Icons.Outlined.Source, "Git", tint = DarkOnSurfaceVariant)
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                IconButton(onClick = { /* Help */ }) {
                    Icon(Icons.Outlined.HelpOutline, "Help", tint = DarkOnSurfaceVariant)
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Outlined.Settings, "Settings", tint = DarkOnSurfaceVariant)
                }
            }
        }
    }
}
