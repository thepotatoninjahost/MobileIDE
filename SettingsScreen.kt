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
import com.fuck13.mobileide.data.preferences.UserPreferences
import com.fuck13.mobileide.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Preferences
    val darkMode by UserPreferences.darkMode.collectAsState(initial = true)
    val dynamicColors by UserPreferences.dynamicColors.collectAsState(initial = false)
    val fontSize by UserPreferences.fontSize.collectAsState(initial = 14)
    val tabSize by UserPreferences.tabSize.collectAsState(initial = 4)
    val wordWrap by UserPreferences.wordWrap.collectAsState(initial = true)
    val showLineNumbers by UserPreferences.showLineNumbers.collectAsState(initial = true)
    val autoSave by UserPreferences.autoSave.collectAsState(initial = true)
    val autoSaveDelay by UserPreferences.autoSaveDelay.collectAsState(initial = 2000L)
    val terminalFontSize by UserPreferences.terminalFontSize.collectAsState(initial = 12)
    val terminalColorScheme by UserPreferences.terminalColorScheme.collectAsState(initial = "dracula")
    val hapticFeedback by UserPreferences.hapticFeedback.collectAsState(initial = true)
    val soundEffects by UserPreferences.soundEffects.collectAsState(initial = false)
    
    // Local state for sliders
    var editorFontSizeSlider by remember { mutableStateOf(fontSize.toFloat()) }
    var terminalFontSizeSlider by remember { mutableStateOf(terminalFontSize.toFloat()) }
    var tabSizeSlider by remember { mutableStateOf(tabSize.toFloat()) }
    var autoSaveDelaySlider by remember { mutableStateOf(autoSaveDelay.toFloat() / 1000f) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = DarkOnBackground
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DarkOnSurface)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkSurface.copy(alpha = 0.95f)
            )
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Appearance section
            item {
                SettingsSectionHeader(
                    title = "Appearance",
                    icon = Icons.Default.Palette,
                    color = AccentSettings
                )
            }
            
            item {
                SettingsCard {
                    SettingsToggle(
                        title = "Dark Mode",
                        subtitle = "Use dark theme throughout the app",
                        checked = darkMode,
                        onCheckedChange = { 
                            scope.launch { UserPreferences.setDarkMode(it) }
                        }
                    )
                    
                    Divider(color = DarkSurfaceVariant.copy(alpha = 0.3f))
                    
                    SettingsToggle(
                        title = "Dynamic Colors",
                        subtitle = "Use Material You colors from wallpaper",
                        checked = dynamicColors,
                        onCheckedChange = {
                            scope.launch { UserPreferences.setDynamicColors(it) }
                        }
                    )
                }
            }
            
            // Editor section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionHeader(
                    title = "Editor",
                    icon = Icons.Default.Edit,
                    color = AccentEditor
                )
            }
            
            item {
                SettingsCard {
                    Column {
                        SettingsSlider(
                            title = "Font Size",
                            value = editorFontSizeSlider,
                            valueRange = 8f..24f,
                            steps = 15,
                            valueText = "${editorFontSizeSlider.toInt()} sp",
                            onValueChange = { editorFontSizeSlider = it },
                            onValueChangeFinished = {
                                scope.launch { UserPreferences.setFontSize(editorFontSizeSlider.toInt()) }
                            }
                        )
                        
                        Divider(color = DarkSurfaceVariant.copy(alpha = 0.3f))
                        
                        SettingsSlider(
                            title = "Tab Size",
                            value = tabSizeSlider,
                            valueRange = 2f..8f,
                            steps = 5,
                            valueText = "${tabSizeSlider.toInt()} spaces",
                            onValueChange = { tabSizeSlider = it },
                            onValueChangeFinished = {
                                scope.launch { UserPreferences.setTabSize(tabSizeSlider.toInt()) }
                            }
                        )
                        
                        Divider(color = DarkSurfaceVariant.copy(alpha = 0.3f))
                        
                        SettingsToggle(
                            title = "Word Wrap",
                            subtitle = "Wrap long lines to fit screen",
                            checked = wordWrap,
                            onCheckedChange = {
                                scope.launch { UserPreferences.setWordWrap(it) }
                            }
                        )
                        
                        Divider(color = DarkSurfaceVariant.copy(alpha = 0.3f))
                        
                        SettingsToggle(
                            title = "Line Numbers",
                            subtitle = "Show line numbers in editor",
                            checked = showLineNumbers,
                            onCheckedChange = {
                                scope.launch { UserPreferences.setShowLineNumbers(it) }
                            }
                        )
                    }
                }
            }
            
            // Auto-save section
            item {
                SettingsCard {
                    SettingsToggle(
                        title = "Auto Save",
                        subtitle = "Automatically save files while editing",
                        checked = autoSave,
                        onCheckedChange = {
                            scope.launch { UserPreferences.setAutoSave(it) }
                        }
                    )
                    
                    if (autoSave) {
                        Divider(color = DarkSurfaceVariant.copy(alpha = 0.3f))
                        
                        SettingsSlider(
                            title = "Auto Save Delay",
                            value = autoSaveDelaySlider,
                            valueRange = 0.5f..10f,
                            steps = 18,
                            valueText = "${autoSaveDelaySlider.toInt()} seconds",
                            onValueChange = { autoSaveDelaySlider = it },
                            onValueChangeFinished = {
                                scope.launch { UserPreferences.setAutoSaveDelay((autoSaveDelaySlider * 1000).toLong()) }
                            }
                        )
                    }
                }
            }
            
            // Terminal section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionHeader(
                    title = "Terminal",
                    icon = Icons.Default.Terminal,
                    color = AccentTerminal
                )
            }
            
            item {
                SettingsCard {
                    SettingsSlider(
                        title = "Font Size",
                        value = terminalFontSizeSlider,
                        valueRange = 8f..20f,
                        steps = 11,
                        valueText = "${terminalFontSizeSlider.toInt()} sp",
                        onValueChange = { terminalFontSizeSlider = it },
                        onValueChangeFinished = {
                            scope.launch { UserPreferences.setTerminalFontSize(terminalFontSizeSlider.toInt()) }
                        }
                    )
                    
                    Divider(color = DarkSurfaceVariant.copy(alpha = 0.3f))
                    
                    SettingsDropdown(
                        title = "Color Scheme",
                        subtitle = terminalColorScheme.capitalize(),
                        options = listOf("dracula", "monokai", "github", "one dark", "nord"),
                        selectedOption = terminalColorScheme,
                        onOptionSelected = {
                            scope.launch { UserPreferences.setTerminalColorScheme(it) }
                        }
                    )
                }
            }
            
            // Behavior section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionHeader(
                    title = "Behavior",
                    icon = Icons.Default.Settings,
                    color = ElectricCyan
                )
            }
            
            item {
                SettingsCard {
                    SettingsToggle(
                        title = "Haptic Feedback",
                        subtitle = "Vibrate on key actions",
                        checked = hapticFeedback,
                        onCheckedChange = {
                            scope.launch { UserPreferences.setHapticFeedback(it) }
                        }
                    )
                    
                    Divider(color = DarkSurfaceVariant.copy(alpha = 0.3f))
                    
                    SettingsToggle(
                        title = "Sound Effects",
                        subtitle = "Play sounds on actions",
                        checked = soundEffects,
                        onCheckedChange = {
                            scope.launch { UserPreferences.setSoundEffects(it) }
                        }
                    )
                }
            }
            
            // About section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(
                    title = "About",
                    icon = Icons.Default.Info,
                    color = InfoBlue
                )
            }
            
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(ElectricCyan, NeonGreen)
                                    ),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Terminal,
                                null,
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = "MobileIDE",
                                style = MaterialTheme.typography.titleLarge,
                                color = DarkOnBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Version 1.0.0",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DarkOnSurfaceVariant
                            )
                        }
                    }
                    
                    Divider(color = DarkSurfaceVariant.copy(alpha = 0.3f))
                    
                    SettingsInfoItem(
                        title = "Built with Kotlin & Jetpack Compose",
                        subtitle = "A full-featured mobile development environment"
                    )
                }
            }
            
            // Footer
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Made with love for developers",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "© 2024 MobileIDE Team",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkOnSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = DarkOnBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkOnSurfaceVariant
                )
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ElectricCyan,
                checkedTrackColor = ElectricCyan.copy(alpha = 0.5f),
                uncheckedThumbColor = DarkOnSurfaceVariant,
                uncheckedTrackColor = DarkSurfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = DarkOnBackground
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = ElectricCyan
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = ElectricCyan,
                activeTrackColor = ElectricCyan,
                inactiveTrackColor = DarkSurfaceVariant
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDropdown(
    title: String,
    subtitle: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = DarkOnBackground
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedOption.capitalize(),
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = DarkSurfaceVariant
                ),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.capitalize()) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsInfoItem(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = DarkOnBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = DarkOnSurfaceVariant
        )
    }
}
