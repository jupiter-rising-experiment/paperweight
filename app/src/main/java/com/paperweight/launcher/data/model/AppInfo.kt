package com.paperweight.launcher.data.model

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: android.graphics.drawable.Drawable,
    val isCoreApp: Boolean = false
)