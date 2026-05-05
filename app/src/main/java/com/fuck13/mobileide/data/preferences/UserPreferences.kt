package com.fuck13.mobileide.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mobile_ide_prefs")

object UserPreferences {
    private lateinit var context: Context
    
    fun init(context: Context) {
        this.context = context
    }
    
    object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val TAB_SIZE = stringPreferencesKey("tab_size")
        val WORD_WRAP = booleanPreferencesKey("word_wrap")
        val SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val AUTO_SAVE_DELAY = stringPreferencesKey("auto_save_delay")
        val LAST_OPENED_FILE = stringPreferencesKey("last_opened_file")
        val LAST_OPENED_PROJECT = stringPreferencesKey("last_opened_project")
        val RECENT_FILES = stringPreferencesKey("recent_files")
        val TERMINAL_FONT_SIZE = stringPreferencesKey("terminal_font_size")
        val TERMINAL_COLOR_SCHEME = stringPreferencesKey("terminal_color_scheme")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
    }
    
    // Dark mode
    val darkMode: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.DARK_MODE] ?: true }
    
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }
    
    // Dynamic colors
    val dynamicColors: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.DYNAMIC_COLORS] ?: false }
    
    suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLORS] = enabled }
    }
    
    // Font size
    val fontSize: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[Keys.FONT_SIZE]?.toIntOrNull() ?: 14 }
    
    suspend fun setFontSize(size: Int) {
        context.dataStore.edit { it[Keys.FONT_SIZE] = size.toString() }
    }
    
    // Tab size
    val tabSize: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[Keys.TAB_SIZE]?.toIntOrNull() ?: 4 }
    
    suspend fun setTabSize(size: Int) {
        context.dataStore.edit { it[Keys.TAB_SIZE] = size.toString() }
    }
    
    // Word wrap
    val wordWrap: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.WORD_WRAP] ?: true }
    
    suspend fun setWordWrap(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WORD_WRAP] = enabled }
    }
    
    // Show line numbers
    val showLineNumbers: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.SHOW_LINE_NUMBERS] ?: true }
    
    suspend fun setShowLineNumbers(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_LINE_NUMBERS] = enabled }
    }
    
    // Auto save
    val autoSave: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.AUTO_SAVE] ?: true }
    
    suspend fun setAutoSave(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SAVE] = enabled }
    }
    
    // Auto save delay (milliseconds)
    val autoSaveDelay: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[Keys.AUTO_SAVE_DELAY]?.toLongOrNull() ?: 2000L }
    
    suspend fun setAutoSaveDelay(delay: Long) {
        context.dataStore.edit { it[Keys.AUTO_SAVE_DELAY] = delay.toString() }
    }
    
    // Last opened file
    val lastOpenedFile: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[Keys.LAST_OPENED_FILE] }
    
    suspend fun setLastOpenedFile(path: String?) {
        context.dataStore.edit { 
            if (path != null) it[Keys.LAST_OPENED_FILE] = path 
            else it.remove(Keys.LAST_OPENED_FILE)
        }
    }
    
    // Last opened project
    val lastOpenedProject: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[Keys.LAST_OPENED_PROJECT] }
    
    suspend fun setLastOpenedProject(path: String?) {
        context.dataStore.edit { 
            if (path != null) it[Keys.LAST_OPENED_PROJECT] = path 
            else it.remove(Keys.LAST_OPENED_PROJECT)
        }
    }
    
    // Recent files (comma-separated paths)
    val recentFiles: Flow<List<String>> = context.dataStore.data
        .map { preferences -> 
            preferences[Keys.RECENT_FILES]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        }
    
    suspend fun addRecentFile(path: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_FILES]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
            val updated = (listOf(path) + current.filter { it != path }).take(10)
            prefs[Keys.RECENT_FILES] = updated.joinToString(",")
        }
    }
    
    // Terminal font size
    val terminalFontSize: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[Keys.TERMINAL_FONT_SIZE]?.toIntOrNull() ?: 12 }
    
    suspend fun setTerminalFontSize(size: Int) {
        context.dataStore.edit { it[Keys.TERMINAL_FONT_SIZE] = size.toString() }
    }
    
    // Terminal color scheme
    val terminalColorScheme: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[Keys.TERMINAL_COLOR_SCHEME] ?: "dracula" }
    
    suspend fun setTerminalColorScheme(scheme: String) {
        context.dataStore.edit { it[Keys.TERMINAL_COLOR_SCHEME] = scheme }
    }
    
    // Haptic feedback
    val hapticFeedback: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.HAPTIC_FEEDBACK] ?: true }
    
    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }
    
    // Sound effects
    val soundEffects: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.SOUND_EFFECTS] ?: false }
    
    suspend fun setSoundEffects(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_EFFECTS] = enabled }
    }
}
