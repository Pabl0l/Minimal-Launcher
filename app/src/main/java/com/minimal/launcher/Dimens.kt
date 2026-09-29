package com.minimal.launcher

import android.content.Context

/**
 * Utilidades de dimensiones centralizadas.
 * Reemplaza las copias de dp() duplicadas en 7 archivos.
 */
object Dimens {
    /** Convierte dp a pixels (Int). */
    fun dp(ctx: Context, v: Float): Int =
        (v * ctx.resources.displayMetrics.density).toInt()

    /** Convierte dp a pixels (Float). */
    fun dpF(ctx: Context, v: Float): Float =
        v * ctx.resources.displayMetrics.density

    /** Convierte sp a pixels (Float) — para tipografía. */
    fun sp(ctx: Context, v: Float): Float =
        v * ctx.resources.displayMetrics.scaledDensity
}
