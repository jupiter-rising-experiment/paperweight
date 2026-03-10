package com.paperweight.launcher.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "core_app_settings")
data class CoreAppSettings(
    @PrimaryKey val packageName: String,
    val position: Int
)