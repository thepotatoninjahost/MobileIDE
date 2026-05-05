# MobileIDE

A full-featured, native Android IDE built with Kotlin and Jetpack Compose.

## Features

- **Code Editor** - Syntax highlighting for Kotlin, Java, Python, JavaScript, and more
- **Error Detection** - Real-time error and warning detection with fix suggestions
- **Terminal Emulator** - Built-in terminal with command execution
- **File Manager** - Full-featured file browser with copy, cut, paste, rename, delete
- **Project Manager** - Create, import, and manage multiple projects
- **Build System** - Background compilation service (framework in place)

## Building

### Option 1: GitHub Actions (Recommended)

1. Create a new repository on GitHub
2. Upload this entire project
3. Go to Actions tab
4. The workflow will automatically build the APK
5. Download the APK from the Artifacts section

### Option 2: Local Build

```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`

## Requirements

- Android 8.0 (API 26) or higher
- Storage permissions for file access

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM + Clean Architecture
- **Database**: Room
- **Preferences**: DataStore
- **Navigation**: Compose Navigation

## Project Structure

```
app/src/main/java/com/fuck13/mobileide/
├── data/
│   ├── model/          # Data models & entities
│   ├── preferences/    # User preferences (DataStore)
│   └── repository/     # Data repositories
├── editor/
│   ├── SyntaxHighlighter.kt
│   └── ErrorDetector.kt
├── service/
│   └── CompilerService.kt
├── ui/
│   ├── screens/        # Compose screens
│   └── theme/          # Theme & styling
├── MainActivity.kt
└── MobileIDEApp.kt
```

## License

MIT License
