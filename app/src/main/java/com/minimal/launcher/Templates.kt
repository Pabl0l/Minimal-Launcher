package com.minimal.launcher

import android.content.Context

/**
 * Plantilla = combinacion preconfigurada de fuente, fondo, nº de accesos,
 * escala de reloj y alineacion. Al aplicarla se escriben todos los ajustes.
 */
data class Template(
    val name: String,
    val font: String,
    val background: String,
    val slots: Int,
    val clockScale: Float,
    val align: String,
    val clock24: Boolean = true,
    val showDate: Boolean = true
)

object Templates {

    val all: List<Template> = listOf(
        Template("Puro", "thin", "none", 6, 1.0f, "start"),
        Template("Zen", "light", "breathe", 5, 1.1f, "center"),
        Template("Terminal", "mono", "rain", 6, 0.9f, "start"),
        Template("Editorial", "serif", "none", 6, 1.15f, "start"),
        Template("Estelar", "light", "stars", 6, 1.0f, "center"),
        Template("Neón", "medium", "particles", 6, 1.0f, "start"),
        Template("Onda", "sans", "waves", 6, 1.0f, "start"),
        Template("Condensado", "condensed", "none", 8, 0.9f, "start"),
        Template("Degradé", "thin", "gradient", 6, 1.05f, "center"),
        Template("Rejilla", "mono", "grid", 6, 1.0f, "start"),
        Template("Grande", "black", "none", 4, 1.3f, "start"),
        Template("Compacto", "condensed", "none", 10, 0.75f, "start"),
    )

    fun apply(ctx: Context, t: Template) {
        Prefs.setFont(ctx, t.font)
        Prefs.setBackground(ctx, t.background)
        Prefs.setSlotCount(ctx, t.slots)
        Prefs.setClockScale(ctx, t.clockScale)
        Prefs.setAlign(ctx, t.align)
        Prefs.setClock24(ctx, t.clock24)
        Prefs.setShowDate(ctx, t.showDate)
        Prefs.setTemplate(ctx, t.name)
    }
}
