package com.paperweight.launcher.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_tier_settings")
data class AppTierSettings(
    @PrimaryKey val packageName: String,
    val tier: Int
)