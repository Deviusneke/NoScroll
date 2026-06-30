package com.example.noscroll

import android.graphics.drawable.Drawable
import java.io.Serializable

data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable?,
    var isSelected: Boolean = false
) : Serializable {
    // Drawable não é serializável, então marcamos como transient
    @Transient
    val appIcon: Drawable? = icon
}