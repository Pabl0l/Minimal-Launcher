package com.minimal.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Búsqueda universal: combina apps instaladas, ajustes y búsqueda web.
 * Un solo resultado tipado para alimentar el RecyclerView.
 */
object UniversalSearch {

    /** Tipo de resultado para distinguir el origen. */
    enum class Kind { APP, SETTING, WEB }

    /** Resultado de búsqueda unificado. */
    data class Result(
        val kind: Kind,
        val label: String,
        val subtitle: String = "",
        val icon: Int = 0,          // resource id (0 = icono por defecto)
        val pkg: String = "",
        val cls: String = "",
        val query: String = "",     // para resultados web
    )

    /** Atajos de ajustes que aparecen como resultados. */
    private val settingsShortcuts = listOf(
        Result(Kind.SETTING, "Fondo animado", "Ajustes → Fondo",
            R.drawable.ic_cat_brush, "", "", "wallpaper"),
        Result(Kind.SETTING, "Reloj", "Ajustes → Estilo de reloj",
            R.drawable.ic_cat_clock, "", "", "clock"),
        Result(Kind.SETTING, "Fuentes", "Ajustes → Tipografía",
            R.drawable.ic_cat_note, "", "", "font"),
        Result(Kind.SETTING, "Accesos directos", "Ajustes → Atajos",
            R.drawable.ic_cat_grid, "", "", "shortcuts"),
        Result(Kind.SETTING, "Gestos", "Ajustes → Deslizamientos",
            R.drawable.ic_cat_compass, "", "", "gestures"),
        Result(Kind.SETTING, "Finanzas", "Panel de saldo",
            R.drawable.ic_cat_wallet, "", "", "finance"),
    )

    /**
     * Busca apps + ajustes + web y devuelve una lista combinada ordenada.
     * @param query texto libre del usuario
     * @param maxApps número máximo de apps a incluir
     */
    fun search(context: Context, query: String, maxApps: Int = 15): List<Result> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()

        val results = mutableListOf<Result>()

        // 1) Apps instaladas (máx maxApps)
        val apps = AppsRepository.getApps(context)
            .filter { it.label.lowercase().contains(q) }
            .take(maxApps)
            .map { app ->
                Result(Kind.APP, app.label, app.pkg,
                    0, app.pkg, app.cls)
            }
        results.addAll(apps)

        // 2) Ajustes (coincidencia parcial)
        val matchesSettings = settingsShortcuts.filter {
            it.label.lowercase().contains(q) || it.query.contains(q)
        }
        results.addAll(matchesSettings)

        // 3) Búsqueda web (si hay query, siempre añadir como última opción)
        if (q.length >= 2) {
            results.add(
                Result(Kind.WEB, "Buscar \"$query\" en la web",
                    "Abrir en navegador", 0, query = q)
            )
        }

        return results
    }

    /** Lanza la acción correspondiente al resultado. */
    fun launch(context: Context, result: Result) {
        when (result.kind) {
            Kind.APP -> {
                try {
                    context.startActivity(
                        AppsRepository.launchIntent(result.pkg, result.cls)
                    )
                } catch (_: Exception) { }
            }
            Kind.SETTING -> {
                when (result.query) {
                    "finance" -> {
                        context.startActivity(
                            Intent(context, FinanceActivity::class.java)
                                .putExtra(FinanceActivity.EXTRA_ENTRY_DIR, "up")
                        )
                    }
                    else -> {
                        context.startActivity(
                            Intent(context, SettingsActivity::class.java)
                        )
                    }
                }
            }
            Kind.WEB -> {
                val url = "https://www.google.com/search?q=${Uri.encode(result.query)}"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }
}
