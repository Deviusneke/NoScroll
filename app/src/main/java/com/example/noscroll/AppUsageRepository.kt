package com.example.noscroll.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AppUsageRepository(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val appLimitDao = database.appLimitDao()
    private val dailyUsageDao = database.dailyUsageDao()

    // Salvar ou atualizar limite de um app
    suspend fun saveAppLimit(packageName: String, appName: String, limitInMillis: Long) {
        withContext(Dispatchers.IO) {
            val entity = AppLimitEntity(
                packageName = packageName,
                appName = appName,
                timeLimitInMillis = limitInMillis
            )
            appLimitDao.insertOrUpdate(entity)
        }
    }

    // Deletar limite de um app completamente (remove do monitoramento)
    suspend fun deleteAppLimit(packageName: String) {
        withContext(Dispatchers.IO) {
            appLimitDao.deleteLimit(packageName)
        }
    }

    // Obter limite de um app (0 = sem limite)
    suspend fun getAppLimit(packageName: String): Long {
        return withContext(Dispatchers.IO) {
            val limit = appLimitDao.getLimit(packageName)
            limit?.timeLimitInMillis ?: 0L
        }
    }

    // Obter todos os limites
    suspend fun getAllAppLimits(): List<AppLimitEntity> {
        return withContext(Dispatchers.IO) {
            appLimitDao.getAllLimitsSync()
        }
    }

    // Salvar uso diário (acumulativo)
    suspend fun saveDailyUsage(packageName: String, timeUsedInMillis: Long) {
        withContext(Dispatchers.IO) {
            val today = getTodayDate()
            val existing = dailyUsageDao.getUsage(packageName, today)

            if (existing == null) {
                val entity = DailyUsageEntity(
                    packageName = packageName,
                    date = today,
                    timeUsedInMillis = timeUsedInMillis
                )
                dailyUsageDao.insertOrUpdate(entity)
            } else {
                // Atualizar apenas se o novo valor for maior
                if (timeUsedInMillis > existing.timeUsedInMillis) {
                    dailyUsageDao.updateUsage(packageName, today, timeUsedInMillis)
                }
            }
        }
    }

    // Obter uso de hoje de um app
    suspend fun getTodayUsage(packageName: String): Long {
        return withContext(Dispatchers.IO) {
            val today = getTodayDate()
            val usage = dailyUsageDao.getUsage(packageName, today)
            usage?.timeUsedInMillis ?: 0L
        }
    }

    // Verificar se app está bloqueado hoje
    suspend fun isAppBlockedToday(packageName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val limit = getAppLimit(packageName)
            if (limit == 0L) return@withContext false
            val todayUsage = getTodayUsage(packageName)
            todayUsage >= limit
        }
    }

    // Obter todos os usos de hoje
    suspend fun getAllTodayUsages(): Map<String, Long> {
        return withContext(Dispatchers.IO) {
            val today = getTodayDate()
            val usages = dailyUsageDao.getDailyUsages(today)
            // Como Flow não pode ser usado assim, vamos fazer de outra forma
            val list = dailyUsageDao.getUsagesSync(today)
            list.associate { it.packageName to it.timeUsedInMillis }
        }
    }

    // Obter total somado por dia dos ultimos 7 dias
    suspend fun getWeeklyUsage(): List<Pair<String, Long>> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<Pair<String, Long>>()
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val calendar = java.util.Calendar.getInstance()
            
            // Loop de 6 dias atrás até hoje (7 dias)
            for (i in 6 downTo 0) {
                val loopCal = calendar.clone() as java.util.Calendar
                loopCal.add(java.util.Calendar.DAY_OF_YEAR, -i)
                val dateStr = sdf.format(loopCal.time)
                
                // Pega todos os usos daquele dia
                val list = dailyUsageDao.getUsagesSync(dateStr)
                // Soma tudo (uso global no aplicativo)
                val totalMs = list.sumOf { it.timeUsedInMillis }
                
                // Formato curto pra data (ex: "15/04") para o rotulo do grafico
                val formatCurto = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())
                val label = formatCurto.format(loopCal.time)
                
                result.add(Pair(label, totalMs))
            }
            result
        }
    }

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}