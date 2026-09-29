package com.minimal.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Indice lateral A-Z. Dibuja las letras verticalmente y notifica la letra bajo el dedo.
 * Minimalista: texto gris, sin fondo.
 */
class SideIndexView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val letters = ('A'..'Z').toList() + '#'
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888")
        textAlign = Paint.Align.CENTER
        isFakeBoldText = false
    }

    var onLetter: ((Char) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (letters.isEmpty()) return
        val cellH = height.toFloat() / letters.size
        paint.textSize = (cellH * 0.7f).coerceAtMost(dp(13f))
        val cx = width / 2f
        for (i in letters.indices) {
            val cy = cellH * i + cellH / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(letters[i].toString(), cx, cy, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val idx = (event.y / height * letters.size).toInt()
                    .coerceIn(0, letters.size - 1)
                onLetter?.invoke(letters[idx])
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun dp(v: Float) = Dimens.dpF(context, v)
}
