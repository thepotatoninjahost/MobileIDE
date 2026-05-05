package com.fuck13.mobileide.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.fuck13.mobileide.MobileIDEApp
import com.fuck13.mobileide.R
import com.fuck13.mobileide.data.model.BuildStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class BuildProgress(
    val projectId: String,
    val status: BuildStatus,
    val currentTask: String = "",
    val progress: Float = 0f,
    val errors: Int = 0,
    val warnings: Int = 0,
    val output: List<String> = emptyList(),
    val startTime: Long = System.currentTimeMillis()
)

class CompilerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _buildProgress = MutableStateFlow<BuildProgress?>(null)
    val buildProgress: StateFlow<BuildProgress?> = _buildProgress
    
    private var currentBuild: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        startForeground()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_BUILD -> {
                val projectId = intent.getStringExtra(EXTRA_PROJECT_ID) ?: return START_NOT_STICKY
                val projectPath = intent.getStringExtra(EXTRA_PROJECT_PATH) ?: return START_NOT_STICKY
                val buildType = intent.getStringExtra(EXTRA_BUILD_TYPE) ?: "debug"
                
                startBuild(projectId, projectPath, buildType)
            }
            ACTION_CANCEL -> {
                cancelBuild()
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cancelBuild()
        serviceScope.cancel()
    }
    
    private fun startForeground() {
        val notification = createNotification(
            title = "Compiler Service",
            message = "Ready to build"
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }
    
    private fun startBuild(projectId: String, projectPath: String, buildType: String) {
        currentBuild?.cancel()
        
        currentBuild = serviceScope.launch {
            _buildProgress.value = BuildProgress(
                projectId = projectId,
                status = BuildStatus.RUNNING,
                currentTask = "Initializing build...",
                startTime = System.currentTimeMillis()
            )
            
            try {
                val projectDir = File(projectPath)
                val output = mutableListOf<String>()
                
                // Simulate build process for Kotlin/Android project
                val buildSteps = listOf(
                    "Cleaning previous build artifacts...",
                    "Resolving dependencies...",
                    "Compiling Kotlin sources...",
                    "Processing resources...",
                    "Generating R.java...",
                    "Compiling Java sources...",
                    "Dexing classes...",
                    "Merging DEX files...",
                    "Packaging APK...",
                    "Aligning APK...",
                    if (buildType == "release") "Signing APK..." else "Build complete!"
                )
                
                buildSteps.forEachIndexed { index, step ->
                    ensureActive()
                    
                    _buildProgress.value = _buildProgress.value?.copy(
                        currentTask = step,
                        progress = (index + 1) / buildSteps.size.toFloat()
                    )
                    
                    updateNotification(step)
                    
                    output.add("[$index] $step")
                    
                    // Simulate work
                    delay(1000)
                    
                    // Check for simulated errors
                    if (step.contains("Compiling") && Math.random() < 0.1) {
                        // Simulate a compilation error (rare)
                        val errorMsg = "Error: Unresolved reference: foo in MainActivity.kt:42"
                        output.add("ERROR: $errorMsg")
                        _buildProgress.value = _buildProgress.value?.copy(
                            status = BuildStatus.FAILED,
                            errors = _buildProgress.value!!.errors + 1,
                            output = output
                        )
                        updateNotification("Build failed")
                        return@launch
                    }
                }
                
                // Build successful
                val buildTime = System.currentTimeMillis() - (_buildProgress.value?.startTime ?: System.currentTimeMillis())
                output.add("BUILD SUCCESSFUL in ${buildTime / 1000}s")
                
                _buildProgress.value = _buildProgress.value?.copy(
                    status = BuildStatus.SUCCESS,
                    currentTask = "Build complete!",
                    progress = 1f,
                    output = output
                )
                
                updateNotification("Build complete!")
                
            } catch (e: CancellationException) {
                _buildProgress.value = _buildProgress.value?.copy(
                    status = BuildStatus.CANCELLED,
                    currentTask = "Build cancelled"
                )
                updateNotification("Build cancelled")
            } catch (e: Exception) {
                _buildProgress.value = _buildProgress.value?.copy(
                    status = BuildStatus.FAILED,
                    currentTask = "Build failed: ${e.message}"
                )
                updateNotification("Build failed")
            }
        }
    }
    
    private fun cancelBuild() {
        currentBuild?.cancel()
        currentBuild = null
        _buildProgress.value = null
    }
    
    private fun createNotification(title: String, message: String): Notification {
        val stopIntent = Intent(this, CompilerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, MobileIDEApp.CHANNEL_ID_COMPILER)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .build()
    }
    
    private fun updateNotification(message: String) {
        val notification = createNotification(
            title = "Building Project",
            message = message
        )
        
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
    
    companion object {
        const val ACTION_BUILD = "com.fuck13.mobileide.BUILD"
        const val ACTION_CANCEL = "com.fuck13.mobileide.CANCEL"
        const val ACTION_STOP = "com.fuck13.mobileide.STOP"
        
        const val EXTRA_PROJECT_ID = "project_id"
        const val EXTRA_PROJECT_PATH = "project_path"
        const val EXTRA_BUILD_TYPE = "build_type"
        
        private const val NOTIFICATION_ID = 1001
    }
}
