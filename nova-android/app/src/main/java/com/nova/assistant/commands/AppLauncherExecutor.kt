package com.nova.assistant.commands

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

data class AppInfo(val label: String, val packageName: String)

class AppLauncherExecutor(private val context: Context) {

    // Loaded once on first use; stable for the lifetime of the service.
    private val apps: List<AppInfo> by lazy { loadApps() }

    fun launchApp(query: String): String {
        if (!Settings.canDrawOverlays(context)) {
            return "Permission required: go to Settings → Apps → Nova → " +
                    "Draw over other apps → Allow"
        }

        val match = resolveApp(query.trim())
            ?: return "Could not find app matching: \"$query\""

        val intent = context.packageManager
            .getLaunchIntentForPackage(match.packageName)
            ?: return "Found \"${match.label}\" but it has no launch intent"

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            "Opening ${match.label}"
        } catch (e: Exception) {
            "Failed to open ${match.label}: ${e.message}"
        }
    }

    private fun resolveApp(query: String): AppInfo? {
        val q = query.lowercase()
        return apps
            .mapNotNull { app ->
                val label = app.label.lowercase()
                val score = when {
                    label == q                                     -> 3
                    label.startsWith(q)                           -> 2
                    label.contains(q)                             -> 1
                    app.packageName.lowercase().contains(q)       -> 0
                    else                                          -> return@mapNotNull null
                }
                score to app
            }
            .maxByOrNull { it.first }
            ?.second
    }

    @Suppress("DEPRECATION")
    private fun loadApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            pm.queryIntentActivities(intent, 0)
        }
        return activities
            .map { ri -> AppInfo(label = ri.loadLabel(pm).toString(), packageName = ri.activityInfo.packageName) }
            .distinctBy { it.packageName }
    }
}
