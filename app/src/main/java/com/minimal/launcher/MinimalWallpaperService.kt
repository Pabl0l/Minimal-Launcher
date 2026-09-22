package com.minimal.launcher

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

/**
 * Fondo de pantalla animado. Es la unica via publica de Android para que las animaciones
 * del launcher se vean en la pantalla de bloqueo real: el wallpaper se dibuja detras del
 * keyguard, sin permisos y sin reemplazar nada del sistema.
 *
 * Reusa [AnimatedBackgroundView] tal cual (los 47 fondos): la vista se mide fuera de la
 * jerarquia de ventanas y este servicio la dibuja a mano sobre el canvas del surface.
 *
 * Bateria: cuando la pantalla se apaga el sistema llama a onVisibilityChanged(false) y el
 * bucle de frames se detiene por completo.
 */
class MinimalWallpaperService : WallpaperService() {

    private companion object {
        /** fps de la miniatura del selector de fondos del sistema. */
        const val PREVIEW_FPS = 25
        /** Cada cuanto se consulta si el telefono sigue bloqueado (llamada al sistema). */
        const val LOCK_POLL_MS = 1000L
        /** Ritmo cuando no hay nada que animar: solo sondear bloqueo/desbloqueo. */
        const val IDLE_DELAY_MS = 1000L
    }

    override fun onCreateEngine(): Engine = MinimalEngine()

    private inner class MinimalEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private val view = AnimatedBackgroundView(this@MinimalWallpaperService)
        private val overlay = LockOverlay(this@MinimalWallpaperService)
        private val keyguard by lazy {
            getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        }

        private val frame = Runnable { drawFrame() }

        private var visible = false
        private var frameDelayMs = 33L
        private var locked = false
        private var lockCheckedAt = 0L
        private var surfaceW = 0
        private var surfaceH = 0
        /** Se apaga si el dispositivo no soporta canvas por hardware en el wallpaper. */
        private var useHardwareCanvas = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            view.offscreen = true
            setTouchEventsEnabled(false)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h)
            surfaceW = w
            surfaceH = h
            view.prepareOffscreen(w, h)
            applyPrefs()
            if (visible) schedule(0L)
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (isVisible) {
                applyPrefs()
                schedule(0L)
            } else {
                handler.removeCallbacks(frame)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(frame)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            handler.removeCallbacks(frame)
            super.onDestroy()
        }

        /** Relee ajustes del usuario. Solo al cambiar visibilidad o tamano, nunca por frame. */
        private fun applyPrefs() {
            val ctx = this@MinimalWallpaperService
            val fps = if (isPreview) PREVIEW_FPS else Prefs.wallpaperFps(ctx)
            frameDelayMs = (1000L / fps).coerceAtLeast(1L)
            overlay.refresh()
            refreshLockState(force = true)
        }

        /**
         * El fondo del bloqueo puede ser distinto al del inicio. La miniatura del selector
         * del sistema muestra siempre la configuracion de bloqueo: es para lo que sirve
         * este wallpaper.
         */
        private fun refreshLockState(force: Boolean) {
            val now = SystemClock.elapsedRealtime()
            if (!force && now - lockCheckedAt < LOCK_POLL_MS) return
            lockCheckedAt = now
            locked = isPreview || keyguard.isKeyguardLocked
            val ctx = this@MinimalWallpaperService
            view.mode = if (locked) Prefs.lockBackgroundResolved(ctx) else Prefs.background(ctx)
        }

        private fun schedule(delayMs: Long) {
            handler.removeCallbacks(frame)
            if (!visible) return
            handler.postDelayed(frame, delayMs)
        }

        private fun drawFrame() {
            if (!visible || surfaceW == 0 || surfaceH == 0) return
            refreshLockState(force = false)
            val drawOverlay = locked && !overlay.isEmpty

            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = lockCanvas(holder)
                if (canvas != null) {
                    canvas.drawColor(Color.BLACK)
                    view.draw(canvas)
                    if (drawOverlay) overlay.draw(canvas, surfaceW.toFloat())
                }
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: IllegalStateException) {
                        // El surface murio entre el lock y el post: nada que publicar.
                    }
                }
            }

            // Sin fondo animado y sin textos no hay nada que refrescar a 30 fps:
            // basta con seguir sondeando el bloqueo una vez por segundo.
            val idle = view.mode == "none" && !drawOverlay
            schedule(if (idle) IDLE_DELAY_MS else frameDelayMs)
        }

        /**
         * Canvas por hardware cuando se puede. Si el dispositivo no lo soporta sobre el
         * surface del wallpaper, baja a software para siempre en vez de quedarse en negro.
         */
        private fun lockCanvas(holder: SurfaceHolder): Canvas? {
            if (useHardwareCanvas && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    return holder.lockHardwareCanvas()
                } catch (e: IllegalStateException) {
                    // Surface aun no valido: se reintenta en el siguiente frame.
                    return null
                } catch (e: RuntimeException) {
                    useHardwareCanvas = false
                }
            }
            return try {
                holder.lockCanvas()
            } catch (e: IllegalStateException) {
                null
            }
        }
    }
}
