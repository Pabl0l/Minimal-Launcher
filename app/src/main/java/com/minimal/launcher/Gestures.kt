package com.minimal.launcher

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast

/**
 * Acciones de gesto. Formato de accion:
 *  - "drawer"  -> cajon de apps
 *  - "camera"  -> camara del sistema
 *  - "browser" -> navegador por defecto
 *  - "none"    -> nada
 *  - "app|pkg|cls|label" -> app concreta
 */
object Gestures {

    val directions = listOf(
        "left" to "Deslizar a la izquierda",
        "right" to "Deslizar a la derecha",
        "up" to "Deslizar hacia arriba",
        "down" to "Deslizar hacia abajo"
    )

    private val arrows = mapOf(
        "left" to "←", "right" to "→", "up" to "↑", "down" to "↓"
    )

    fun arrow(key: String): String = arrows[key] ?: "•"

    /** Recurso vectorial de flecha por dirección. */
    fun arrowRes(key: String): Int = when (key) {
        "up" -> R.drawable.ic_arrow_up
        "down" -> R.drawable.ic_arrow_down
        "left" -> R.drawable.ic_arrow_left
        else -> R.drawable.ic_arrow_right
    }

    /** Acciones fijas ofrecidas en el selector (ademas de "Elegir app…"). */
    val presets = listOf(
        "drawer" to "Cajón de apps",
        "finance" to "Ver saldo",
        "camera" to "Cámara",
        "browser" to "Navegador",
        "phone" to "Teléfono",
        "settings" to "Ajustes del sistema",
        "none" to "Nada"
    )

    fun label(action: String): String {
        if (action.startsWith("app|")) {
            val parts = action.split("|")
            return parts.getOrNull(3)?.ifEmpty { "App" } ?: "App"
        }
        return presets.firstOrNull { it.first == action }?.second ?: "Nada"
    }

    fun appAction(pkg: String, cls: String, appLabel: String) = "app|$pkg|$cls|$appLabel"

    /** Ejecuta la accion. openDrawer/openFinance se delegan para aplicar transicion. */
    fun run(
        activity: Activity,
        action: String,
        openDrawer: () -> Unit,
        openFinance: () -> Unit
    ) {
        when {
            action == "drawer" -> openDrawer()
            action == "finance" -> openFinance()
            action == "camera" -> launch(activity, cameraIntent())
            action == "browser" -> launch(activity, browserIntent())
            action == "phone" -> launch(activity, phoneIntent())
            action == "settings" -> launch(activity, settingsIntent())
            action.startsWith("app|") -> {
                val p = action.split("|")
                if (p.size >= 3) launch(activity, AppsRepository.launchIntent(p[1], p[2]))
            }
            else -> {} // none
        }
    }

    private fun cameraIntent() =
        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun browserIntent() =
        Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun phoneIntent() =
        Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun settingsIntent() =
        Intent(android.provider.Settings.ACTION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun launch(activity: Activity, intent: Intent) {
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            // fallback navegador: ACTION_VIEW
            try {
                activity.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e2: Exception) {
                Toast.makeText(activity, "No hay app para esta acción", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
