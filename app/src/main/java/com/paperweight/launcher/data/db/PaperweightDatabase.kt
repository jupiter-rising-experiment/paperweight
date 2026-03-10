package com.paperweight.launcher.data.db

import android.content.Context
import androidx.room.*

@Dao
interface AppTierDao {
    @Query("SELECT * FROM app_tier_settings")
    suspend fun getAll(): List<AppTierSettings>

    @Query("SELECT * FROM app_tier_settings WHERE packageName = :pkg")
    suspend fun getForPackage(pkg: String): AppTierSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppTierSettings)
}

@Dao
interface CoreAppDao {
    @Query("SELECT * FROM core_app_settings ORDER BY position ASC")
    suspend fun getAll(): List<CoreAppSettings>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: CoreAppSettings)

    @Delete
    suspend fun delete(app: CoreAppSettings)

    @Query("DELETE FROM core_app_settings WHERE packageName = :pkg")
    suspend fun deleteByPackage(pkg: String)
}

@Database(
    entities = [AppTierSettings::class, CoreAppSettings::class],
    version = 1,
    exportSchema = false
)
abstract class PaperweightDatabase : RoomDatabase() {
    abstract fun appTierDao(): AppTierDao
    abstract fun coreAppDao(): CoreAppDao

    companion object {
        @Volatile private var INSTANCE: PaperweightDatabase? = null

        fun getInstance(context: Context): PaperweightDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    PaperweightDatabase::class.java,
                    "paperweight.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}