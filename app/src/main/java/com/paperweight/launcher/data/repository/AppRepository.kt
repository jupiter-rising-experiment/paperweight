package com.paperweight.launcher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.paperweight.launcher.data.db.CoreAppSettings
import com.paperweight.launcher.data.db.PaperweightDatabase
import com.paperweight.launcher.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    private val db = PaperweightDatabase.getInstance(context)
    private val coreAppDao = db.coreAppDao()

    suspend fun getAllApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val corePackages = coreAppDao.getAll().map { it.packageName }.toSet()

        pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            .map { resolveInfo ->
                AppInfo(
                    packageName = resolveInfo.activityInfo.packageName,
                    label = resolveInfo.loadLabel(pm).toString(),
                    icon = resolveInfo.loadIcon(pm),
                    isCoreApp = resolveInfo.activityInfo.packageName in corePackages
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    suspend fun getCoreApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val coreSettings = coreAppDao.getAll()

        coreSettings.mapNotNull { setting ->
            try {
                val appInfo = pm.getApplicationInfo(setting.packageName, 0)
                AppInfo(
                    packageName = setting.packageName,
                    label = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(setting.packageName),
                    isCoreApp = true
                )
            } catch (e: PackageManager.NameNotFoundException) {
                coreAppDao.deleteByPackage(setting.packageName)
                null
            }
        }
    }

    suspend fun addCoreApp(packageName: String) = withContext(Dispatchers.IO) {
        val current = coreAppDao.getAll()
        if (current.size >= 6) return@withContext
        val position = current.size
        coreAppDao.upsert(CoreAppSettings(packageName, position))
    }

    suspend fun removeCoreApp(packageName: String) = withContext(Dispatchers.IO) {
        coreAppDao.deleteByPackage(packageName)
        val remaining = coreAppDao.getAll()
        remaining.forEachIndexed { index, app ->
            coreAppDao.upsert(app.copy(position = index))
        }
    }

    suspend fun seedDefaultsIfEmpty() = withContext(Dispatchers.IO) {
        val existing = coreAppDao.getAll()
        if (existing.isNotEmpty()) return@withContext

        val defaults = listOf(
            "com.android.dialer",
            "com.android.mms",
            "com.google.android.apps.messaging"
        )

        val pm = context.packageManager
        var position = 0
        for (pkg in defaults) {
            try {
                pm.getPackageInfo(pkg, 0)
                coreAppDao.upsert(CoreAppSettings(pkg, position++))
            } catch (e: PackageManager.NameNotFoundException) {
                // Not installed, skip
            }
        }
    }

    fun launchApp(packageName: String) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}