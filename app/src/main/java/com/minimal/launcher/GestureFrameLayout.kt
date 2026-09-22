package com.minimal.launcher

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Raiz del Home que detecta swipes aunque el dedo empiece sobre un acceso clickable.
 * Intercepta el gesto solo cuando el movimiento supera el touch slop, de modo que los
 * taps siguen llegando a los hijos (se abren las apps) y los swipes disparan la accion.
 */
class GestureFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    /** Direccion: "left","right","up","down". */
    var onSwipe: ((String) -> Unit)? = null

    /** Si true, solo intercepta swipes horizontales (deja pasar el scroll vertical). */
    var horizontalOnly: Boolean = false

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val trigger = 70f * context.resources.displayMetrics.density

    private var downX = 0f
    private var downY = 0f

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - downX
                val dy = ev.y - downY
                if (horizontalOnly) {
                    if (abs(dx) > abs(dy) && abs(dx) > slop * 2) return true
                } else {
                    if (hypot(dx.toDouble(), dy.toDouble()) > slop * 2) return true
                }
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = ev.x - downX
                val dy = ev.y - downY
                if (abs(dx) > abs(dy)) {
                    if (abs(dx) > trigger) onSwipe?.invoke(if (dx < 0) "left" else "right")
                } else {
                    if (abs(dy) > trigger) onSwipe?.invoke(if (dy < 0) "up" else "down")
                }
                return true
            }
        }
        return true
    }
}
