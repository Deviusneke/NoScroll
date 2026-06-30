package com.example.noscroll.helper

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import com.example.noscroll.data.AppUsageInfo
import java.text.SimpleDateFormat
import java.util.*

class UsageStatsManagerHelper(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager

    fun hasUsageStatsPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    appOps.checkOpNoThrow(
                        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(),
                        context.packageName
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(),
                        context.packageName
                    )
                }
                return mode == android.app.AppOpsManager.MODE_ALLOWED
            } catch (e: Exception) {
                return false
            }
        }
        return true
    }

    fun requestUsageStatsPermission() {
        if (!hasUsageStatsPermission()) {
            val intent = android.content.Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun getAppUsageStats(startTime: Long, endTime: Long): List<AppUsageInfo> {
        val appUsageList = mutableListOf<AppUsageInfo>()

        try {
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            if (usageStats != null) {
                for (stats in usageStats) {
                    // Pular o próprio app se necessário
                    if (stats.packageName == context.packageName) continue

                    val appName = getAppName(stats.packageName)
                    val icon = getAppIcon(stats.packageName)

                    val appInfo = AppUsageInfo(
                        packageName = stats.packageName,
                        appName = appName,
                        totalTimeInForeground = stats.totalTimeInForeground,
                        lastTimeUsed = stats.lastTimeUsed,
                        icon = icon
                    )
                    appUsageList.add(appInfo)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return appUsageList.sortedByDescending { it.totalTimeInForeground }
    }

    /**
     * Obtém o aplicativo atual em primeiro plano de forma mais precisa
     * Usando UsageEvents para melhor precisão
     */
    fun getCurrentForegroundApp(): String? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val endTime = System.currentTimeMillis()
                val startTime = endTime - 60 * 1000 // Último minuto

                val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
                var lastForegroundApp: String? = null
                var lastEventTime = 0L

                if (usageEvents != null) {
                    val event = UsageEvents.Event()
                    while (usageEvents.hasNextEvent()) {
                        usageEvents.getNextEvent(event)

                        // Verificar eventos de movimento para foreground
                        if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED)) {
                            if (event.timeStamp > lastEventTime) {
                                lastEventTime = event.timeStamp
                                lastForegroundApp = event.packageName
                            }
                        }
                    }
                }

                // Se não encontrou com eventos, tenta com queryUsageStats
                if (lastForegroundApp == null) {
                    val usageStats = usageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY,
                        startTime,
                        endTime
                    )

                    usageStats?.let { stats ->
                        val validStats = stats.filter { it.packageName != context.packageName }
                        if (validStats.isNotEmpty()) {
                            val recentTask = validStats.maxByOrNull { it.lastTimeUsed }
                            lastForegroundApp = recentTask?.packageName
                        }
                    }
                }

                return lastForegroundApp?.takeIf { it != context.packageName }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Obtém o tempo total de uso de um app específico hoje
     */
    fun getAppTodayUsage(packageName: String): Long {
        val startOfDay = getStartOfDay()
        val endTime = System.currentTimeMillis()

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startOfDay,
            endTime
        )

        return usageStats?.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
    }

    /**
     * Verifica se um app já excedeu o limite hoje
     */
    fun hasExceededLimit(packageName: String, limitInMillis: Long): Boolean {
        if (limitInMillis <= 0) return false
        val todayUsage = getAppTodayUsage(packageName)
        return todayUsage >= limitInMillis
    }

    /**
     * Obtém todos os apps com seus respectivos tempos de uso hoje
     */
    fun getAllAppsTodayUsage(): Map<String, Long> {
        val usageMap = mutableMapOf<String, Long>()
        val startOfDay = getStartOfDay()
        val endTime = System.currentTimeMillis()

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startOfDay,
            endTime
        )

        usageStats?.forEach { stats ->
            if (stats.packageName != context.packageName) {
                usageMap[stats.packageName] = stats.totalTimeInForeground
            }
        }

        return usageMap
    }

    /**
     * Força o fechamento de um aplicativo
     */
    fun forceCloseApp(packageName: String): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activityManager.killBackgroundProcesses(packageName)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Verifica se um aplicativo está em primeiro plano no momento
     */
    fun isAppInForeground(packageName: String): Boolean {
        val currentApp = getCurrentForegroundApp()
        return currentApp == packageName
    }

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun getAppIcon(packageName: String): Drawable? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationIcon(appInfo)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formata o tempo em milissegundos para uma string legível
     */
    fun formatTime(millis: Long): String {
        val hours = (millis / (1000 * 60 * 60)).toInt()
        val minutes = ((millis % (1000 * 60 * 60)) / (1000 * 60)).toInt()
        val seconds = ((millis % (1000 * 60)) / 1000).toInt()

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    /**
     * Obtém a data atual formatada
     */
    fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}