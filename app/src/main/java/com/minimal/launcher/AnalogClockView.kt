package com.minimal.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Reloj analogico monocromo. Se auto-actualiza cada segundo mientras esta visible. */
class AnalogClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var faceColor: Int = Color.WHITE

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val ticker = object : Runnable {
        override fun run() {
            invalidate()
            postDelayed(this, 1000L - System.currentTimeMillis() % 1000L)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(ticker)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - dp(4f)
        if (r <= 0) return

        // aro exterior
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(2f)
        paint.color = withAlpha(0.9f)
        canvas.drawCircle(cx, cy, r, paint)

        // marcas de horas
        for (i in 0 until 12) {
            val a = i * (PI.toFloat() / 6f)
            val long = i % 3 == 0
            val inner = r - if (long) dp(11f) else dp(6f)
            paint.strokeWidth = if (long) dp(2.4f) else dp(1.2f)
            paint.color = withAlpha(if (long) 0.9f else 0.45f)
            canvas.drawLine(
                cx + cos(a) * inner, cy + sin(a) * inner,
                cx + cos(a) * r, cy + sin(a) * r, paint
            )
        }

        val c = Calendar.getInstance()
        val h = c.get(Calendar.HOUR)
        val m = c.get(Calendar.MINUTE)
        val s = c.get(Calendar.SECOND)

        val hourAng = (h + m / 60f) * (PI.toFloat() / 6f) - PI.toFloat() / 2f
        val minAng = (m + s / 60f) * (PI.toFloat() / 30f) - PI.toFloat() / 2f
        val secAng = s * (PI.toFloat() / 30f) - PI.toFloat() / 2f

        // manecillas
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = withAlpha(1f)
        paint.strokeWidth = dp(4f)
        hand(canvas, cx, cy, hourAng, r * 0.52f)
        paint.strokeWidth = dp(2.6f)
        hand(canvas, cx, cy, minAng, r * 0.78f)
        paint.strokeWidth = dp(1.4f)
        paint.color = withAlpha(0.7f)
        hand(canvas, cx, cy, secAng, r * 0.86f)

        // centro
        paint.style = Paint.Style.FILL
        paint.color = withAlpha(1f)
        canvas.drawCircle(cx, cy, dp(3.5f), paint)
    }

    private fun hand(c: Canvas, cx: Float, cy: Float, ang: Float, len: Float) {
        c.drawLine(cx, cy, cx + cos(ang) * len, cy + sin(ang) * len, paint)
    }

    private fun withAlpha(f: Float): Int {
        val a = (Color.alpha(faceColor) * f).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(faceColor), Color.green(faceColor), Color.blue(faceColor))
    }
}
