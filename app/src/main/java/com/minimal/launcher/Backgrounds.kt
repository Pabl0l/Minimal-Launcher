package com.minimal.launcher

/** Fondo animado: key persistida + etiqueta. */
data class BackgroundOption(val key: String, val label: String)

object Backgrounds {
    val options: List<BackgroundOption> = listOf(
        BackgroundOption("none", "Ninguno (negro)"),
        // Originales
        BackgroundOption("stars", "Estrellas"),
        BackgroundOption("particles", "Partículas"),
        BackgroundOption("breathe", "Respiración"),
        BackgroundOption("waves", "Ondas"),
        BackgroundOption("rain", "Lluvia de código"),
        BackgroundOption("gradient", "Degradado"),
        BackgroundOption("grid", "Rejilla"),
        // Nuevos
        BackgroundOption("constellation", "Constelación"),
        BackgroundOption("fireflies", "Luciérnagas"),
        BackgroundOption("snow", "Nieve"),
        BackgroundOption("matrix", "Matrix"),
        BackgroundOption("orbit", "Órbitas"),
        BackgroundOption("ripple", "Ondas concéntricas"),
        BackgroundOption("spiral", "Espiral"),
        BackgroundOption("dna", "ADN"),
        BackgroundOption("bubbles", "Burbujas"),
        BackgroundOption("meteors", "Meteoros"),
        BackgroundOption("nebula", "Nebulosa"),
        BackgroundOption("pulse", "Sónar"),
        BackgroundOption("equalizer", "Ecualizador"),
        BackgroundOption("warp", "Hipervelocidad"),
        BackgroundOption("noise", "Estática"),
        BackgroundOption("vortex", "Vórtice"),
        BackgroundOption("hexagons", "Panal"),
        BackgroundOption("mesh", "Malla"),
        BackgroundOption("contour", "Curvas de nivel"),
        BackgroundOption("lissajous", "Lissajous"),
        BackgroundOption("pendulum", "Péndulos"),
        BackgroundOption("lightning", "Relámpago"),
        BackgroundOption("embers", "Brasas"),
        BackgroundOption("comet", "Cometa"),
        BackgroundOption("radar", "Radar"),
        BackgroundOption("heartbeat", "Electro"),
        BackgroundOption("scanline", "Escáner"),
        BackgroundOption("sphere", "Esfera"),
        BackgroundOption("flow", "Flujo"),
        BackgroundOption("tunnel", "Túnel"),
        BackgroundOption("starburst", "Destello"),
        BackgroundOption("rings", "Anillos"),
        // Extra
        BackgroundOption("binary", "Binario"),
        BackgroundOption("confetti", "Confeti"),
        BackgroundOption("fireworks", "Fuegos"),
        BackgroundOption("smoke", "Humo"),
        BackgroundOption("kaleidoscope", "Caleidoscopio"),
        BackgroundOption("waveform", "Onda de audio"),
        BackgroundOption("plasma", "Plasma"),
        BackgroundOption("droplets", "Gotas"),
    )

    private const val SAME_LABEL = "Igual que el inicio"

    /** Catalogo para la pantalla de bloqueo: agrega "heredar el fondo del inicio". */
    val lockOptions: List<BackgroundOption> =
        listOf(BackgroundOption(Prefs.LOCK_BG_SAME, SAME_LABEL)) + options

    private val byKey = options.associateBy { it.key }
    fun labelFor(key: String): String = byKey[key]?.label ?: "Ninguno (negro)"

    fun lockLabelFor(key: String): String =
        if (key == Prefs.LOCK_BG_SAME) SAME_LABEL else labelFor(key)
}
