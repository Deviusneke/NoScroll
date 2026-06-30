package com.example.noscroll.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(limit: AppLimitEntity)

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName")
    suspend fun getLimit(packageName: String): AppLimitEntity?

    @Query("SELECT * FROM app_limits")
    fun getAllLimits(): Flow<List<AppLimitEntity>>

    @Query("DELETE FROM app_limits WHERE packageName = :packageName")
    suspend fun deleteLimit(packageName: String)

    @Query("SELECT * FROM app_limits")
    suspend fun getAllLimitsSync(): List<AppLimitEntity>
}