package com.minimal.launcher

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Hojas inferiores "liquid glass" con preview real:
 * fondos animados en vivo, fuentes en su tipografia, estilos de reloj y gestos.
 */
object Sheets {

    private fun dp(ctx: Context, v: Float) = Dimens.dp(ctx, v)

    /** Crea la hoja anclada abajo y devuelve (dialog, cuerpo donde agregar filas). */
    private fun sheet(ctx: Context, titleText: String): Pair<Dialog, LinearLayout> {
        val dialog = Dialog(ctx, R.style.Theme_GlassSheet)

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(ctx, R.drawable.sheet_bg)
            setPadding(dp(ctx, 18f), dp(ctx, 12f), dp(ctx, 18f), dp(ctx, 30f))
        }

        // grabber
        root.addView(View(ctx).apply {
            background = ContextCompat.getDrawable(ctx, R.drawable.grabber)
            val lp = LinearLayout.LayoutParams(dp(ctx, 40f), dp(ctx, 5f))
            lp.gravity = Gravity.CENTER_HORIZONTAL
            lp.bottomMargin = dp(ctx, 14f)
            layoutParams = lp
        })

        root.addView(TextView(ctx).apply {
            text = titleText
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(dp(ctx, 6f), 0, 0, dp(ctx, 12f))
            layoutParams = lp
        })

        val body = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(ctx).apply {
            isVerticalScrollBarEnabled = false
            addView(body)
        })

        dialog.setContentView(root)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
        }
        return dialog to body
    }

    private fun checkRow(
        ctx: Context, label: String, tf: Typeface?, textSize: Float,
        selected: Boolean, onClick: () -> Unit
    ): View {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(ctx, 54f)
            setPadding(dp(ctx, 8f), dp(ctx, 8f), dp(ctx, 8f), dp(ctx, 8f))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val out = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, out, true)
            setBackgroundResource(out.resourceId)
        }
        row.addView(TextView(ctx).apply {
            text = label
            if (tf != null) typeface = tf
            setTextColor(ContextCompat.getColor(ctx, if (selected) R.color.white else R.color.offwhite))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_check)
            setColorFilter(Color.WHITE)
            visibility = if (selected) View.VISIBLE else View.INVISIBLE
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 20f), dp(ctx, 20f))
        })
        row.setOnClickListener { onClick() }
        Motion.press(row)
        return row
    }

    // ---------------------------------------------------------------- Fuentes

    fun fontPicker(ctx: Context, onChange: () -> Unit) {
        val (dialog, body) = sheet(ctx, "Tipo de letra")
        val current = Prefs.font(ctx)
        Fonts.options.forEach { opt ->
            body.addView(
                checkRow(ctx, opt.label, Fonts.typeface(opt.key), 21f, opt.key == current) {
                    Prefs.setFont(ctx, opt.key); onChange(); dialog.dismiss()
                }
            )
        }
        dialog.show()
    }

    // ---------------------------------------------------------------- Fondos

    /**
     * Selector de fondo animado con miniaturas en vivo.
     * Sirve tanto para el fondo del inicio (por defecto) como para el del bloqueo,
     * pasando [title], [current], [options] y [onPick].
     */
    fun backgroundPicker(
        ctx: Context,
        title: String = "Fondo animado",
        current: String = Prefs.background(ctx),
        options: List<BackgroundOption> = Backgrounds.options,
        onPick: (String) -> Unit = { Prefs.setBackground(ctx, it) },
        onChange: () -> Unit
    ) {
        val (dialog, body) = sheet(ctx, title)
        val cols = 2
        var rowLayout: LinearLayout? = null
        options.forEachIndexed { i, opt ->
            if (i % cols == 0) {
                rowLayout = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                body.addView(rowLayout)
            }
            rowLayout!!.addView(bgTile(ctx, opt, opt.key == current) {
                onPick(opt.key); onChange(); dialog.dismiss()
            })
        }
        dialog.show()
    }

    private fun bgTile(ctx: Context, opt: BackgroundOption, selected: Boolean, onClick: () -> Unit): View {
        val cell = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(dp(ctx, 5f), dp(ctx, 5f), dp(ctx, 5f), dp(ctx, 5f))
            layoutParams = lp
        }
        val frame = FrameLayout(ctx).apply {
            background = ContextCompat.getDrawable(
                ctx, if (selected) R.drawable.glass_tile_selected else R.drawable.glass_tile
            )
            outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 96f)
            )
        }
        // La opcion "igual que el inicio" se previsualiza con el fondo del inicio.
        val previewMode =
            if (opt.key == Prefs.LOCK_BG_SAME) Prefs.background(ctx) else opt.key
        if (previewMode != "none") {
            frame.addView(AnimatedBackgroundView(ctx).apply {
                frameDelayMs = 40L   // ~25 fps: previews multiples sin recalentar
                mode = previewMode
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
            })
        }
        cell.addView(frame)
        cell.addView(TextView(ctx).apply {
            text = opt.label
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(ctx, if (selected) R.color.white else R.color.gray))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            maxLines = 1
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(ctx, 6f)
            layoutParams = lp
        })
        cell.setOnClickListener { onClick() }
        Motion.press(cell)
        return cell
    }

    // ---------------------------------------------------------------- Reloj estilo

    fun clockStylePicker(ctx: Context, onChange: () -> Unit) {
        val (dialog, body) = sheet(ctx, "Tipo de reloj")
        val current = Prefs.clockStyle(ctx)
        val is24 = Prefs.clock24(ctx)
        val tf = Fonts.typeface(Prefs.font(ctx))
        ClockFaces.options.forEach { face ->
            body.addView(clockFaceRow(ctx, face, tf, is24, face.key == current) {
                Prefs.setClockStyle(ctx, face.key); onChange(); dialog.dismiss()
            })
        }
        dialog.show()
    }

    private fun clockFaceRow(
        ctx: Context, face: ClockFace, tf: Typeface, is24: Boolean,
        selected: Boolean, onClick: () -> Unit
    ): View {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(ctx, 62f)
            setPadding(dp(ctx, 8f), dp(ctx, 8f), dp(ctx, 8f), dp(ctx, 8f))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val out = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, out, true)
            setBackgroundResource(out.resourceId)
        }
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        // muestra: analogico en vivo o texto renderizado
        if (face.kind == ClockKind.ANALOG) {
            col.addView(AnalogClockView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(dp(ctx, 44f), dp(ctx, 44f))
            })
        } else {
            val sample = try {
                face.render(ClockFaces.now(), is24)
            } catch (e: Exception) { face.label }
            col.addView(TextView(ctx).apply {
                text = sample
                typeface = tf
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, if (face.twoLine) 20f else 26f)
                maxLines = 2
            })
        }
        col.addView(TextView(ctx).apply {
            text = face.label
            setTextColor(ContextCompat.getColor(ctx, R.color.gray))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(ctx, 3f)
            layoutParams = lp
        })
        row.addView(col)
        row.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_check)
            setColorFilter(Color.WHITE)
            visibility = if (selected) View.VISIBLE else View.INVISIBLE
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 20f), dp(ctx, 20f))
        })
        row.setOnClickListener { onClick() }
        Motion.press(row)
        return row
    }

    // ---------------------------------------------------------------- Plantillas

    fun templatePicker(ctx: Context, onChange: () -> Unit) {
        val (dialog, body) = sheet(ctx, "Plantillas")
        val current = Prefs.template(ctx)
        Templates.all.forEach { t ->
            body.addView(checkRow(ctx, t.name, null, 17f, t.name == current) {
                Templates.apply(ctx, t); onChange(); dialog.dismiss()
            })
        }
        dialog.show()
    }

    // ---------------------------------------------------------------- Gestos

    fun gestureSheet(ctx: Context, onPickApp: (String) -> Unit, onChange: () -> Unit) {
        val (dialog, body) = sheet(ctx, "Gestos")
        Gestures.directions.forEach { (key, name) ->
            body.addView(gestureTile(ctx, key, name) {
                dialog.dismiss()
                gestureActionSheet(ctx, key, onPickApp, onChange)
            })
        }
        dialog.show()
    }

    private fun gestureTile(ctx: Context, key: String, name: String, onClick: () -> Unit): View {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(ctx, 66f)
            setPadding(dp(ctx, 6f), dp(ctx, 8f), dp(ctx, 8f), dp(ctx, 8f))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        // circulo glass con la flecha (SVG)
        row.addView(ImageView(ctx).apply {
            setImageResource(Gestures.arrowRes(key))
            setColorFilter(Color.WHITE)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            background = ContextCompat.getDrawable(ctx, R.drawable.glass_pill)
            val pad = dp(ctx, 11f)
            setPadding(pad, pad, pad, pad)
            val lp = LinearLayout.LayoutParams(dp(ctx, 46f), dp(ctx, 46f))
            lp.marginEnd = dp(ctx, 14f)
            layoutParams = lp
        })
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        col.addView(TextView(ctx).apply {
            text = name
            setTextColor(ContextCompat.getColor(ctx, R.color.offwhite))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        })
        col.addView(TextView(ctx).apply {
            text = Gestures.label(Prefs.gesture(ctx, key))
            setTextColor(ContextCompat.getColor(ctx, R.color.gray))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        })
        row.addView(col)
        row.addView(TextView(ctx).apply {
            text = "›"
            setTextColor(ContextCompat.getColor(ctx, R.color.dim))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        })
        row.setOnClickListener { onClick() }
        Motion.press(row)
        return row
    }

    private fun gestureActionSheet(
        ctx: Context, key: String, onPickApp: (String) -> Unit, onChange: () -> Unit
    ) {
        val title = Gestures.directions.first { it.first == key }.second
        val (dialog, body) = sheet(ctx, title)
        val current = Prefs.gesture(ctx, key)
        Gestures.presets.forEach { (action, label) ->
            body.addView(checkRow(ctx, label, null, 17f, action == current) {
                Prefs.setGesture(ctx, key, action); onChange(); dialog.dismiss()
            })
        }
        body.addView(checkRow(ctx, "Elegir app…", null, 17f, current.startsWith("app|")) {
            dialog.dismiss(); onPickApp(key)
        })
        dialog.show()
    }
}
