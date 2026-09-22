package com.minimal.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Consulta apps lanzables. Cachea el resultado para evitar reconsultar al PackageManager
 * cada vez que se abre el cajon (eficiencia de CPU/bateria).
 */
object AppsRepository {

    @Volatile
    private var cache: List<AppInfo>? = null

    /** Lista alfabetica de apps lanzables. Usa cache salvo forceReload. */
    fun getApps(context: Context, forceReload: Boolean = false): List<AppInfo> {
        val cached = cache
        if (cached != null && !forceReload) return cached
        val loaded = loadApps(context.applicationContext)
        cache = loaded
        return loaded
    }

    fun invalidate() {
        cache = null
    }

    private fun loadApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val self = context.packageName
        return resolveInfos.asSequence()
            .map { ri ->
                AppInfo(
                    label = ri.loadLabel(pm).toString(),
                    pkg = ri.activityInfo.packageName,
                    cls = ri.activityInfo.name
                )
            }
            .filter { it.pkg != self } // no listarnos a nosotros mismos
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** Intent para lanzar una app concreta por paquete/clase. */
    fun launchIntent(pkg: String, cls: String): Intent =
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(pkg, cls)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }

    /** True si el paquete sigue instalado (para limpiar slots huerfanos). */
    fun isInstalled(context: Context, pkg: String): Boolean =
        try {
            context.packageManager.getApplicationInfo(pkg, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
}
