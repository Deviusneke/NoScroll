package com.example.noscroll.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.noscroll.BlockActivity
import com.example.noscroll.R
import com.example.noscroll.helper.UsageStatsManagerHelper

class UsageMonitorService : Service() {

    private lateinit var usageHelper: UsageStatsManagerHelper
    private lateinit var sharedPreferences: SharedPreferences
    private var isMonitoring = false
    private var monitoringThread: Thread? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "usage_monitor_channel"
        private const val CHECK_INTERVAL = 60000L // Verificar a cada 1 minuto
    }

    override fun onCreate() {
        super.onCreate()
        usageHelper = UsageStatsManagerHelper(this)
        sharedPreferences = getSharedPreferences("app_limits", Context.MODE_PRIVATE)
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
                "Monitor de Uso de Apps",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitora o tempo de uso dos aplicativos"
                setSound(null, null)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NoScroll - Controle Parental Ativo")
            .setContentText("Monitorando o tempo de uso dos aplicativos")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startMonitoring() {
        if (isMonitoring) return

        isMonitoring = true
        monitoringThread = Thread {
            while (isMonitoring) {
                checkAppLimits()
                try {
                    Thread.sleep(CHECK_INTERVAL)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
        monitoringThread?.start()
    }

    private fun checkAppLimits() {
        try {
            if (!usageHelper.hasUsageStatsPermission()) {
                return
            }

            val currentTime = System.currentTimeMillis()
            val startOfDay = getStartOfDay()
            val appUsageList = usageHelper.getAppUsageStats(startOfDay, currentTime)

            val allLimits = sharedPreferences.all
            val blockedApps = mutableListOf<String>()

            for ((packageName, limitObj) in allLimits) {
                val timeLimit = limitObj as Long
                if (timeLimit > 0) {
                    val appUsage = appUsageList.find { it.packageName == packageName }
                    val timeUsed = appUsage?.totalTimeInForeground ?: 0

                    if (timeUsed >= timeLimit) {
                        blockedApps.add(packageName)
                        if (!wasBlockedToday(packageName)) {
                            showBlockPopup(packageName, appUsage?.appName ?: packageName)
                            markBlockedToday(packageName)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showBlockPopup(packageName: String, appName: String) {
        try {
            val intent = Intent(this, BlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(BlockActivity.EXTRA_APP_NAME, appName)
                putExtra(BlockActivity.EXTRA_PACKAGE_NAME, packageName)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun wasBlockedToday(packageName: String): Boolean {
        val prefs = getSharedPreferences("blocked_history", Context.MODE_PRIVATE)
        val today = getTodayDate()
        return prefs.getBoolean("$packageName-$today", false)
    }

    private fun markBlockedToday(packageName: String) {
        val prefs = getSharedPreferences("blocked_history", Context.MODE_PRIVATE)
        val today = getTodayDate()
        prefs.edit().putBoolean("$packageName-$today", true).apply()
    }

    private fun getTodayDate(): String {
        val calendar = java.util.Calendar.getInstance()
        return "${calendar.get(java.util.Calendar.YEAR)}-${calendar.get(java.util.Calendar.MONTH)}-${calendar.get(java.util.Calendar.DAY_OF_MONTH)}"
    }

    private fun getStartOfDay(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        monitoringThread?.interrupt()
    }
}