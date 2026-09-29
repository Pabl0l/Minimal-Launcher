package com.minimal.launcher

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat

/**
 * Fabrica de controles "liquid glass" para el panel de ajustes.
 * Todo programatico (sin Material): tarjetas translucidas, segmentos, sliders,
 * switches y filas de navegacion con estetica premium tipo iOS.
 */
object SettingsUi {

    fun dp(ctx: Context, v: Float): Int = Dimens.dp(ctx, v)

    private fun mw() = ViewGroup.LayoutParams.MATCH_PARENT
    private fun wc() = ViewGroup.LayoutParams.WRAP_CONTENT

    /** Etiqueta de seccion: mayusculas tenues, con aire arriba. */
    fun sectionLabel(ctx: Context, text: String): TextView = TextView(ctx).apply {
        this.text = text.uppercase()
        setTextColor(ContextCompat.getColor(ctx, R.color.gray))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        letterSpacing = 0.12f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        val lp = LinearLayout.LayoutParams(wc(), wc())
        lp.setMargins(dp(ctx, 22f), dp(ctx, 22f), 0, dp(ctx, 9f))
        layoutParams = lp
    }

    /** Tarjeta glass con sombra y esquinas grandes. */
    fun card(ctx: Context): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        background = ContextCompat.getDrawable(ctx, R.drawable.glass_card)
        setPadding(dp(ctx, 18f), dp(ctx, 6f), dp(ctx, 18f), dp(ctx, 6f))
        elevation = dp(ctx, 10f).toFloat()
        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true
        val lp = LinearLayout.LayoutParams(mw(), wc())
        lp.setMargins(0, 0, 0, dp(ctx, 4f))
        layoutParams = lp
    }

    private fun rippleBg(ctx: Context): Int {
        val out = TypedValue()
        ctx.theme.resolveAttribute(android.R.attr.selectableItemBackground, out, true)
        return out.resourceId
    }

    private fun rowBase(ctx: Context): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(ctx, 56f)
        setPadding(dp(ctx, 4f), dp(ctx, 12f), dp(ctx, 4f), dp(ctx, 12f))
        layoutParams = LinearLayout.LayoutParams(mw(), wc())
    }

    private fun title(ctx: Context, text: String): TextView = TextView(ctx).apply {
        this.text = text
        setTextColor(ContextCompat.getColor(ctx, R.color.offwhite))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f)
    }

    /** Fila que abre otra pantalla/hoja: titulo + valor + chevron. */
    fun navRow(ctx: Context, titleText: String, value: String, onClick: () -> Unit): View {
        val row = rowBase(ctx)
        row.setBackgroundResource(rippleBg(ctx))
        row.isClickable = true

        val t = title(ctx, titleText)
        t.layoutParams = LinearLayout.LayoutParams(0, wc(), 1f)
        row.addView(t)

        val v = TextView(ctx).apply {
            text = value
            setTextColor(ContextCompat.getColor(ctx, R.color.gray))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            maxWidth = dp(ctx, 150f)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        row.addView(v)

        val chevron = TextView(ctx).apply {
            text = "  ›"
            setTextColor(ContextCompat.getColor(ctx, R.color.dim))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        }
        row.addView(chevron)

        row.setOnClickListener { onClick() }
        Motion.press(row)
        return row
    }

    /** Fila con interruptor. */
    fun switchRow(
        ctx: Context, titleText: String, subtitle: String?,
        checked: Boolean, onChange: (Boolean) -> Unit
    ): View {
        val row = rowBase(ctx)
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, wc(), 1f)
        }
        col.addView(title(ctx, titleText))
        if (!subtitle.isNullOrEmpty()) {
            col.addView(TextView(ctx).apply {
                text = subtitle
                setTextColor(ContextCompat.getColor(ctx, R.color.gray))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                val lp = LinearLayout.LayoutParams(wc(), wc())
                lp.topMargin = dp(ctx, 2f)
                layoutParams = lp
            })
        }
        row.addView(col)

        val sw = SwitchCompat(ctx).apply {
            isChecked = checked
            thumbTintList = ColorStateList.valueOf(Color.WHITE)
            trackTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(ContextCompat.getColor(ctx, R.color.accent_dim),
                    ContextCompat.getColor(ctx, R.color.glass_track))
            )
            setOnCheckedChangeListener { _, v -> onChange(v) }
        }
        row.addView(sw)
        return row
    }

    /** Fila con slider (SeekBar) y valor en vivo. min..max en pasos enteros. */
    fun sliderRow(
        ctx: Context, titleText: String, min: Int, max: Int, value: Int,
        format: (Int) -> String, onChange: (Int) -> Unit
    ): View {
        val wrap = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 4f), dp(ctx, 12f), dp(ctx, 4f), dp(ctx, 12f))
            layoutParams = LinearLayout.LayoutParams(mw(), wc())
        }
        val head = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val t = title(ctx, titleText).apply {
            layoutParams = LinearLayout.LayoutParams(0, wc(), 1f)
        }
        val valLabel = TextView(ctx).apply {
            text = format(value)
            setTextColor(ContextCompat.getColor(ctx, R.color.offwhite))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        head.addView(t); head.addView(valLabel)
        wrap.addView(head)

        val seek = SeekBar(ctx).apply {
            this.max = max - min
            progress = value - min
            progressDrawable = ContextCompat.getDrawable(ctx, R.drawable.seek_track)
            thumb = ContextCompat.getDrawable(ctx, R.drawable.seek_thumb)
            splitTrack = false
            val lp = LinearLayout.LayoutParams(mw(), wc())
            lp.topMargin = dp(ctx, 8f)
            layoutParams = lp
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                    val nv = min + p
                    valLabel.text = format(nv)
                    onChange(nv)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        wrap.addView(seek)
        return wrap
    }

    /** Control segmentado tipo iOS. Devuelve la vista; reconstruye al elegir. */
    fun segment(
        ctx: Context, titleText: String?, labels: List<String>,
        selected: Int, onSelect: (Int) -> Unit
    ): View {
        val wrap = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 4f), dp(ctx, 12f), dp(ctx, 4f), dp(ctx, 12f))
            layoutParams = LinearLayout.LayoutParams(mw(), wc())
        }
        if (!titleText.isNullOrEmpty()) {
            wrap.addView(title(ctx, titleText).apply {
                val lp = LinearLayout.LayoutParams(wc(), wc())
                lp.bottomMargin = dp(ctx, 10f)
                layoutParams = lp
            })
        }
        val track = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            background = ContextCompat.getDrawable(ctx, R.drawable.segment_track)
            setPadding(dp(ctx, 3f), dp(ctx, 3f), dp(ctx, 3f), dp(ctx, 3f))
            layoutParams = LinearLayout.LayoutParams(mw(), wc())
        }
        labels.forEachIndexed { i, label ->
            val seg = TextView(ctx).apply {
                text = label
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(0, dp(ctx, 9f), 0, dp(ctx, 9f))
                if (i == selected) {
                    background = ContextCompat.getDrawable(ctx, R.drawable.segment_thumb)
                    setTextColor(Color.WHITE)
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                } else {
                    setTextColor(ContextCompat.getColor(ctx, R.color.gray))
                }
                layoutParams = LinearLayout.LayoutParams(0, wc(), 1f)
                setOnClickListener { onSelect(i) }
            }
            track.addView(seg)
        }
        wrap.addView(track)
        return wrap
    }

    /** Boton principal glass (pastilla). */
    fun primaryButton(ctx: Context, text: String, onClick: () -> Unit): View =
        TextView(ctx).apply {
            this.text = text
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            background = ContextCompat.getDrawable(ctx, R.drawable.glass_pill)
            setPadding(0, dp(ctx, 15f), 0, dp(ctx, 15f))
            val lp = LinearLayout.LayoutParams(mw(), wc())
            lp.setMargins(0, dp(ctx, 6f), 0, dp(ctx, 6f))
            layoutParams = lp
            setOnClickListener { onClick() }
            Motion.press(this)
        }

    /** Separador fino dentro de una tarjeta. */
    fun innerDivider(ctx: Context): View = View(ctx).apply {
        setBackgroundColor(ContextCompat.getColor(ctx, R.color.glass_stroke_soft))
        layoutParams = LinearLayout.LayoutParams(mw(), dp(ctx, 0.5f).coerceAtLeast(1))
    }
}
