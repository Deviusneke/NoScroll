package com.example.noscroll.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimitEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val timeLimitInMillis: Long,  // Limite diário em milissegundos
    val lastUpdated: Long = System.currentTimeMillis()
)