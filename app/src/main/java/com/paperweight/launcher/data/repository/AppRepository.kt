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
            val icon = CUSTOM_ICONS[packageName]
                ?.let { ContextCompat.getDrawable(context, it) }
                ?: pm.getApplicationIcon(packageName)
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
        val prefs = context.getSharedPreferences("paperweight_prefs", Context.MODE_PRIVATE)
        val seededVersion = prefs.getInt("core_apps_defaults_version", 0)
        val currentVersion = 2

        if (seededVersion >= currentVersion && coreAppDao.getAll().isNotEmpty()) return@withContext

        // Role-based defaults: first installed package per role wins
        val roleDefaults = listOf(
            listOf("com.google.android.apps.messaging", "com.android.mms"),   // Messages
            listOf("com.google.android.dialer", "com.android.dialer"),         // Phone
            listOf("com.google.android.gm", "com.android.email"),              // Email
            listOf("com.android.chrome", "org.chromium.chrome")                // Chrome
        )

        val pm = context.packageManager
        coreAppDao.deleteAll()
        var position = 0
        for (candidates in roleDefaults) {
            for (pkg in candidates) {
                try {
                    pm.getPackageInfo(pkg, 0)
                    coreAppDao.upsert(CoreAppSettings(pkg, position++))
                    break
                } catch (e: PackageManager.NameNotFoundException) {
                    // try next candidate
                }
            }
        }

        prefs.edit().putInt("core_apps_defaults_version", currentVersion).apply()
    }

    fun launchApp(packageName: String) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    private val CUSTOM_ICONS = mapOf(
        "com.google.android.dialer"          to R.drawable.ic_custom_phone,
        "com.android.dialer"                 to R.drawable.ic_custom_phone,
        "com.google.android.gm"              to R.drawable.ic_custom_gmail,
        "com.android.chrome"                 to R.drawable.ic_custom_chrome,
        "org.chromium.chrome"                to R.drawable.ic_custom_chrome,
        "com.google.android.apps.messaging"  to R.drawable.ic_custom_messages,
        "com.android.mms"                    to R.drawable.ic_custom_messages,
    )

}