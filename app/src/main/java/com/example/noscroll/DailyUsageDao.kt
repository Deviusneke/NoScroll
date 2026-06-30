package com.example.noscroll.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(usage: DailyUsageEntity)

    @Query("SELECT * FROM daily_usage WHERE packageName = :packageName AND date = :date")
    suspend fun getUsage(packageName: String, date: String): DailyUsageEntity?

    @Query("SELECT * FROM daily_usage WHERE date = :date")
    fun getDailyUsages(date: String): Flow<List<DailyUsageEntity>>

    // Método síncrono para usar em coroutines
    @Query("SELECT * FROM daily_usage WHERE date = :date")
    suspend fun getUsagesSync(date: String): List<DailyUsageEntity>

    @Query("UPDATE daily_usage SET timeUsedInMillis = :timeUsed WHERE packageName = :packageName AND date = :date")
    suspend fun updateUsage(packageName: String, date: String, timeUsed: Long)

}

