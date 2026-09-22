package com.minimal.launcher

import android.view.MotionEvent
import android.view.View
import android.view.animation.PathInterpolator
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

/** Animaciones tipo iOS: entrada escalonada + feedback de pulsacion con muelle. */
object Motion {

    val EASE = PathInterpolator(0.32f, 0.72f, 0f, 1f)

    /** Aparicion suave: fade + leve desplazamiento hacia arriba, con retardo escalonado. */
    fun enter(view: View, delayMs: Long, distanceDp: Float = 18f) {
        val d = view.resources.displayMetrics.density
        view.alpha = 0f
        view.translationY = distanceDp * d
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delayMs)
            .setDuration(420)
            .setInterpolator(EASE)
            .start()
    }

    /** Escala al pulsar (0.96) y muelle de vuelta a 1.0, como en iOS. */
    fun press(view: View) {
        val sx = SpringAnimation(view, SpringAnimation.SCALE_X)
        val sy = SpringAnimation(view, SpringAnimation.SCALE_Y)
        for (s in listOf(sx, sy)) {
            s.spring = SpringForce(1f).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness = 1400f
            }
        }
        view.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    sx.animateToFinalPosition(0.96f)
                    sy.animateToFinalPosition(0.96f)
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    sx.animateToFinalPosition(1f)
                    sy.animateToFinalPosition(1f)
                }
            }
            false // no consumir: deja pasar click/long-click
        }
    }
}
