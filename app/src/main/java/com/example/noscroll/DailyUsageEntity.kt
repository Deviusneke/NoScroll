package com.example.noscroll.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_usage")
data class DailyUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val date: String,  // Formato: YYYY-MM-DD
    val timeUsedInMillis: Long,
    val lastUpdated: Long = System.currentTimeMillis()
)