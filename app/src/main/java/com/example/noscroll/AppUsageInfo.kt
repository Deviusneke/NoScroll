package com.example.noscroll.data

import android.graphics.drawable.Drawable

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeInForeground: Long,
    val lastTimeUsed: Long,
    val icon: Drawable?
)