package com.example.noscroll.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.noscroll.BlockActivity
import com.example.noscroll.R
import com.example.noscroll.data.AppUsageRepository
import com.example.noscroll.helper.UsageStatsManagerHelper
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class AppUsageMonitor : Service() {

    private lateinit var usageHelper: UsageStatsManagerHelper
    private lateinit var repository: AppUsageRepository
    private var isMonitoring = false
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Cache de limites para evitar consultas repetidas ao banco
    private val limitsCache = mutableMapOf<String, Long>()
    private var lastCacheUpdate: Long = 0



    private val checkRunnable = object : Runnable {
        override fun run() {
            if (forceUpdate) {
                forceUpdate = false
                updateLimitsCache()
            }
            checkAndBlockApps()
            handler.postDelayed(this, 2000) // Verificar a cada 2 segundos
        }
    }

    private val syncRunnable = object : Runnable {
        override fun run() {
            syncUsageData()
            handler.postDelayed(this, 60000) // Sincronizar a cada 1 minuto
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "app_monitor_channel"
        var forceUpdate = false
    }

    override fun onCreate() {
        super.onCreate()
        usageHelper = UsageStatsManagerHelper(this)
        repository = AppUsageRepository(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startMonitoring()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NoScroll - Proteção Ativa",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitora e bloqueia apps que excederam o limite de tempo"
                setSound(null, null)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ NoScroll - Proteção Ativa")
            .setContentText("Monitorando apps com limite de tempo...")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        handler.post(checkRunnable)
        handler.post(syncRunnable)
        println("✅ [Service] Monitoramento iniciado")
    }



    private fun updateLimitsCache() {
        serviceScope.launch {
            val limits = repository.getAllAppLimits()
            limitsCache.clear()
            limits.forEach { limit ->
                if (limit.timeLimitInMillis > 0) {
                    limitsCache[limit.packageName] = limit.timeLimitInMillis
                }
            }
            lastCacheUpdate = System.currentTimeMillis()
            println("📦 [Cache] Limites atualizados: ${limitsCache.size} apps")
        }
    }

    private fun syncUsageData() {
        serviceScope.launch {
            try {
                if (!usageHelper.hasUsageStatsPermission()) return@launch

                // Atualizar cache de limites periodicamente
                if (System.currentTimeMillis() - lastCacheUpdate > 30000) {
                    updateLimitsCache()
                }

                val startOfDay = getStartOfDay()
                val endTime = System.currentTimeMillis()
                val appUsageList = usageHelper.getAppUsageStats(startOfDay, endTime)

                for (app in appUsageList) {
                    repository.saveDailyUsage(app.packageName, app.totalTimeInForeground)
                }
                println("✅ [Sync] Dados sincronizados - ${appUsageList.size} apps")
            } catch (e: Exception) {
                println("❌ [Sync] Erro: ${e.message}")
            }
        }
    }

    private fun checkAndBlockApps() {
        serviceScope.launch {
            try {
                if (!usageHelper.hasUsageStatsPermission()) {
                    return@launch
                }

                // Obter app atual em primeiro plano
                val currentApp = usageHelper.getCurrentForegroundApp()
                if (currentApp == null || currentApp == packageName) {
                    return@launch
                }

                // Verificar limite
                val timeLimit = limitsCache[currentApp] ?: repository.getAppLimit(currentApp)
                if (timeLimit == 0L) {
                    return@launch
                }

                // Usar dados ao vivo do sistema operacional para evitar delay de sincronização do banco
                val timeUsed = usageHelper.getAppTodayUsage(currentApp)
                
                // Excedeu ou atingiu o limite?
                if (timeUsed >= timeLimit) {
                    val appName = getAppName(currentApp)
                    println("🚫 [BLOQUEIO] $appName excedeu o tempo!")
                    showBlockAndCloseApp(currentApp, appName)
                }
            } catch (e: Exception) {
                println("❌ [Erro] checkAndBlockApps: ${e.message}")
            }
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun showBlockAndCloseApp(packageName: String, appName: String) {
        try {
            // Fechar o app
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activityManager.killBackgroundProcesses(packageName)
            }

            // Mostrar popup de bloqueio
            val intent = Intent(this, BlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                putExtra(BlockActivity.EXTRA_APP_NAME, appName)
                putExtra(BlockActivity.EXTRA_PACKAGE_NAME, packageName)
            }
            startActivity(intent)
        } catch (e: Exception) {
            println("❌ [Erro] showBlockAndCloseApp: ${e.message}")
        }
    }

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        println("🛑 [Service] AppUsageMonitor destruído")
        isMonitoring = false
        handler.removeCallbacks(checkRunnable)
        handler.removeCallbacks(syncRunnable)
        serviceScope.cancel()
    }
}