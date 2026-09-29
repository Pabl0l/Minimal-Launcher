package com.minimal.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Widget de información del sistema: batería, WiFi, almacenamiento.
 * Dibujado directamente sobre Canvas (sin vistas hijas) para máxima eficiencia.
 * Estilo minimalista monocromo, consistente con el resto del launcher.
 */
class SystemInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8E8E93.toInt() // gray
        textSize = Dimens.sp(context, 12f)
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
    }

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8E8E93.toInt()
        textSize = Dimens.sp(context, 14f)
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF5F5F7.toInt() // offwhite
        textSize = Dimens.sp(context, 13f)
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33FFFFFF.toInt() // 20% blanco
        strokeWidth = Dimens.dpF(context, 0.5f)
    }

    private var batteryPct = -1
    private var batteryCharging = false
    private var storageUsed = ""
    private var storageTotal = ""
    private var wifiSsid = ""
    private var wifiStrength = 0

    /** Actualiza la información del sistema y redibuja. */
    fun refresh() {
        val bat = SystemInfo.battery(context)
        batteryPct = bat.pct
        batteryCharging = bat.isCharging

        val stor = SystemInfo.storage()
        storageUsed = "${stor.usedMb}MB"
        storageTotal = "${stor.totalMb}MB"

        val w = SystemInfo.wifi(context)
        wifiSsid = w.ssid
        wifiStrength = w.strength

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val padX = Dimens.dpF(context, 2f)
        val lineH = Dimens.dpF(context, 22f)

        var y = lineH

        // --- Batería ---
        val batIcon = when {
            batteryPct < 0 -> "?"
            batteryCharging -> "⚡"
            batteryPct > 75 -> "●"
            batteryPct > 25 -> "◕"
            else -> "○"
        }
        val batText = if (batteryPct >= 0) "$batteryPct%" else "—"
        canvas.drawText("Batería", padX, y, textPaint)
        canvas.drawText("$batIcon $batText", w - padX - valuePaint.measureText("$batIcon $batText"), y, valuePaint)
        y += lineH

        // Divider
        canvas.drawLine(padX, y - lineH * 0.3f, w - padX, y - lineH * 0.3f, dividerPaint)

        // --- WiFi ---
        val wifiIcon = when (wifiStrength) {
            4 -> "▂▄▆█"
            3 -> "▂▄▆ "
            2 -> "▂▄  "
            1 -> "▂   "
            else -> "    "
        }
        canvas.drawText("WiFi", padX, y, textPaint)
        val wifiLabel = if (wifiSsid == "—") "Sin red" else wifiSsid
        canvas.drawText("$wifiIcon $wifiLabel", w - padX - valuePaint.measureText("$wifiIcon $wifiLabel"), y, valuePaint)
        y += lineH

        // Divider
        canvas.drawLine(padX, y - lineH * 0.3f, w - padX, y - lineH * 0.3f, dividerPaint)

        // --- Almacenamiento ---
        canvas.drawText("Almacenamiento", padX, y, textPaint)
        val storText = "$storageUsed / $storageTotal"
        canvas.drawText(storText, w - padX - valuePaint.measureText(storText), y, valuePaint)
    }
}
