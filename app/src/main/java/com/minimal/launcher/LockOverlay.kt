package com.minimal.launcher

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.os.BatteryManager
import android.os.SystemClock
import android.util.TypedValue
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Textos opcionales que el live wallpaper pinta sobre la animacion mientras el telefono
 * esta bloqueado: reloj, fecha y bateria. Los tres vienen apagados porque el keyguard ya
 * trae su propio reloj; se activan desde Ajustes.
 *
 * Sin vistas: dibujo directo sobre el canvas del wallpaper, reusando la cara de reloj
 * ([ClockFaces]) y la tipografia ([Fonts]) que el usuario ya eligio para el inicio.
 */
class LockOverlay(private val ctx: Context) {

    private companion object {
        /** La bateria cambia despacio: no tiene sentido leerla mas seguido que esto. */
        const val BATTERY_TTL_MS = 60_000L
        const val PADDING_START_DP = 36f
        const val PADDING_TOP_DP = 72f
        const val GAP_DP = 4f
        const val SUB_SP = 15f
    }

    private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dateFormat = SimpleDateFormat("EEEE d MMMM", Locale.getDefault())

    private var showClock = false
    private var showDate = false
    private var showBattery = false
    private var center = false
    private var is24 = true
    private var face: ClockFace = ClockFaces.DEFAULT

    private var batteryPct = -1
    private var batteryReadAt = 0L

    /** true si no hay nada que pintar: el wallpaper se ahorra todo el bloque. */
    val isEmpty: Boolean get() = !showClock && !showDate && !showBattery

    /** Relee los ajustes. Se llama al cambiar la visibilidad, nunca por frame. */
    fun refresh() {
        showClock = Prefs.lockClock(ctx)
        showDate = Prefs.lockDate(ctx)
        showBattery = Prefs.lockBattery(ctx)
        center = Prefs.align(ctx) == "center"
        is24 = Prefs.clock24(ctx)

        // El reloj analogico se dibuja con una vista propia (AnalogClockView) que no se
        // porta al wallpaper: cae al minimalista.
        val chosen = ClockFaces.byKeyOrDefault(Prefs.clockStyle(ctx))
        face = if (chosen.kind == ClockKind.ANALOG) ClockFaces.DEFAULT else chosen

        val tf = Fonts.typeface(Prefs.font(ctx))
        clockPaint.typeface = tf
        clockPaint.color = ContextCompat.getColor(ctx, R.color.white)
        clockPaint.textSize = sp(face.baseSp * Prefs.clockScale(ctx))
        subPaint.typeface = tf
        subPaint.color = ContextCompat.getColor(ctx, R.color.gray)
        subPaint.textSize = sp(SUB_SP)

        batteryReadAt = 0L
    }

    fun draw(canvas: Canvas, w: Float) {
        if (isEmpty) return

        val align = if (center) Paint.Align.CENTER else Paint.Align.LEFT
        clockPaint.textAlign = align
        subPaint.textAlign = align
        val x = if (center) w / 2f else dp(PADDING_START_DP)

        var y = dp(PADDING_TOP_DP)
        fun line(text: String, paint: Paint) {
            val fm = paint.fontMetrics
            y -= fm.top
            canvas.drawText(text, x, y, paint)
            y += fm.bottom
        }

        if (showClock) {
            face.render(Calendar.getInstance(), is24).split('\n').forEach { line(it, clockPaint) }
        }
        if (showDate) {
            y += dp(GAP_DP)
            line(dateFormat.format(Calendar.getInstance().time), subPaint)
        }
        if (showBattery) {
            val pct = battery()
            if (pct >= 0) {
                y += dp(GAP_DP)
                line("$pct %", subPaint)
            }
        }
    }

    /** Nivel de bateria 0..100, o -1 si el sistema no lo reporta. Cacheado. */
    private fun battery(): Int {
        val now = SystemClock.elapsedRealtime()
        if (batteryPct >= 0 && now - batteryReadAt < BATTERY_TTL_MS) return batteryPct
        // receiver null = lectura directa del broadcast sticky, no registra nada.
        val status = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val max = status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        batteryPct = if (level >= 0 && max > 0) level * 100 / max else -1
        batteryReadAt = now
        return batteryPct
    }

    private fun dp(v: Float): Float = v * ctx.resources.displayMetrics.density

    private fun sp(v: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, v, ctx.resources.displayMetrics
    )
}
