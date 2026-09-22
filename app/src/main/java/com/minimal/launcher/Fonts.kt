package com.minimal.launcher

import android.graphics.Typeface

/** Opcion de fuente: key persistida + familia del sistema (sin TTF empaquetado). */
data class FontOption(val key: String, val label: String, val family: String, val style: Int)

/**
 * Fuentes del sistema Android (no aumentan el tamano del APK).
 * Algunas familias dependen del fabricante; si no existe, cae a la por defecto.
 */
object Fonts {

    val options: List<FontOption> = listOf(
        // Sans-serif y sus pesos
        FontOption("thin", "Fina", "sans-serif-thin", Typeface.NORMAL),
        FontOption("light", "Ligera", "sans-serif-light", Typeface.NORMAL),
        FontOption("sans", "Normal", "sans-serif", Typeface.NORMAL),
        FontOption("medium", "Media", "sans-serif-medium", Typeface.NORMAL),
        FontOption("bold", "Seminegrita", "sans-serif", Typeface.BOLD),
        FontOption("black", "Negrita", "sans-serif-black", Typeface.NORMAL),
        FontOption("italic", "Cursiva fina", "sans-serif-light", Typeface.ITALIC),
        FontOption("bolditalic", "Cursiva negrita", "sans-serif", Typeface.BOLD_ITALIC),
        // Condensadas
        FontOption("condensed", "Condensada", "sans-serif-condensed", Typeface.NORMAL),
        FontOption("condlight", "Condensada fina", "sans-serif-condensed-light", Typeface.NORMAL),
        FontOption("condmedium", "Condensada media", "sans-serif-condensed-medium", Typeface.NORMAL),
        FontOption("condbold", "Condensada negrita", "sans-serif-condensed", Typeface.BOLD),
        // Estilos especiales
        FontOption("smallcaps", "Versalitas", "sans-serif-smallcaps", Typeface.NORMAL),
        // Serif
        FontOption("serif", "Serif", "serif", Typeface.NORMAL),
        FontOption("serifbold", "Serif negrita", "serif", Typeface.BOLD),
        FontOption("serifitalic", "Serif cursiva", "serif", Typeface.ITALIC),
        FontOption("serifbolditalic", "Serif elegante", "serif", Typeface.BOLD_ITALIC),
        // Monoespaciada
        FontOption("mono", "Monoespaciada", "monospace", Typeface.NORMAL),
        FontOption("monobold", "Mono negrita", "monospace", Typeface.BOLD),
        FontOption("monoitalic", "Mono cursiva", "monospace", Typeface.ITALIC),
        FontOption("serifmono", "Serif mono", "serif-monospace", Typeface.NORMAL),
        // Manuscritas
        FontOption("casual", "Casual", "casual", Typeface.NORMAL),
        FontOption("casualbold", "Casual gruesa", "casual", Typeface.BOLD),
        FontOption("cursive", "Manuscrita", "cursive", Typeface.NORMAL),
        FontOption("cursivebold", "Manuscrita gruesa", "cursive", Typeface.BOLD),
        // Cursivas extra
        FontOption("thinitalic", "Fina cursiva", "sans-serif-thin", Typeface.ITALIC),
        FontOption("mediumitalic", "Media cursiva", "sans-serif-medium", Typeface.ITALIC),
        FontOption("blackitalic", "Negra cursiva", "sans-serif-black", Typeface.ITALIC),
        FontOption("conditalic", "Condensada cursiva", "sans-serif-condensed", Typeface.ITALIC),
        FontOption("smallcapsbold", "Versalitas negrita", "sans-serif-smallcaps", Typeface.BOLD),
        FontOption("serifmonobold", "Serif mono negrita", "serif-monospace", Typeface.BOLD),
    )

    private val byKey = options.associateBy { it.key }

    fun labelFor(key: String): String = byKey[key]?.label ?: "Fina"

    fun typeface(key: String): Typeface {
        val opt = byKey[key] ?: byKey["thin"]!!
        return Typeface.create(opt.family, opt.style)
    }
}
