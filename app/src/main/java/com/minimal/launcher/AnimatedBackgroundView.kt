package com.minimal.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Fondos animados monocromo, visibles y suaves. Se pausan cuando la vista no es visible
 * (ahorro de bateria). Modo "none" no dibuja nada (negro puro = maximo ahorro AMOLED).
 */
class AnimatedBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var mode: String = "none"
        set(value) {
            if (field == value) return
            field = value
            regen()
            if (isAttachedToWindow) {
                if (value == "none") { stop(); invalidate() } else start()
            }
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var running = false
    private var startNs = 0L

    /** Retardo entre frames. Previews usan un valor mayor (menos fps) para aligerar. */
    var frameDelayMs = 16L

    /**
     * true cuando la vista no vive en una ventana y alguien externo la dibuja a mano
     * (el live wallpaper). En ese modo el bucle interno de frames queda desactivado:
     * el ritmo lo marca quien dibuja.
     */
    var offscreen = false

    // Buffers por modo (se regeneran al cambiar de tamano o de modo).
    private var stars = FloatArray(0)   // x,y,size,phase
    private var parts = FloatArray(0)   // x,y,speed,size
    private var rain = FloatArray(0)    // x,y,len,speed
    private var nodes = FloatArray(0)   // baseX,baseY,phaseA,phaseB (constellation/mesh)
    private var netPos = FloatArray(0)  // scratch: posiciones animadas x,y
    private var flies = FloatArray(0)   // x,y,phase,speed
    private var snow = FloatArray(0)    // x,y,size,drift
    private var mtx = FloatArray(0)     // x,y,len,speed (matrix)
    private var bubbles = FloatArray(0) // x,size,speed,phase
    private var meteors = FloatArray(0) // x,y,len,speed
    private var warp = FloatArray(0)    // ang,dist,speed,size
    private var vortex = FloatArray(0)  // ang,radius,speed,size
    private var embers = FloatArray(0)  // x,y,speed,size
    private var sphere = FloatArray(0)  // x,y,z (unidad)
    private var flow = FloatArray(0)    // x,y
    private var blips = FloatArray(0)   // x,y
    private var conf = FloatArray(0)    // x,y,rot,speed,size (confetti)
    private var fw = FloatArray(0)      // cx,cy,tOffset (fireworks)
    private var smoke = FloatArray(0)   // x,y,speed,size
    private var drops = FloatArray(0)   // x,y,tOffset
    private var bin = FloatArray(0)     // x,y,len,speed (binario)

    private val frame = object : Runnable {
        override fun run() {
            if (running) {
                invalidate()
                postOnAnimationDelayed(this, frameDelayMs)
            }
        }
    }

    fun start() {
        if (offscreen || mode == "none" || running) return
        if (width == 0 || height == 0) return
        running = true
        if (startNs == 0L) startNs = System.nanoTime()
        postOnAnimation(frame)
    }

    /**
     * Prepara la vista para dibujarse en un canvas ajeno (live wallpaper): la mide,
     * la posiciona y arranca el reloj de la animacion. Sin esto `startNs` seguiria en 0
     * y todas las animaciones quedarian congeladas en t = 0.
     */
    fun prepareOffscreen(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        offscreen = true
        measure(
            MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
        )
        layout(0, 0, w, h)   // dispara onSizeChanged -> regen()
        if (startNs == 0L) startNs = System.nanoTime()
    }

    fun stop() {
        running = false
        removeCallbacks(frame)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (mode != "none") start()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        regen()
        if (mode != "none" && !running) start()
    }

    private fun regen() {
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        val rnd = Random(7)
        when (mode) {
            "stars" -> {
                val n = 95; stars = FloatArray(n * 4)
                for (i in 0 until n) {
                    stars[i * 4] = rnd.nextFloat() * w
                    stars[i * 4 + 1] = rnd.nextFloat() * h
                    stars[i * 4 + 2] = dp(0.8f) + rnd.nextFloat() * dp(2.0f)
                    stars[i * 4 + 3] = rnd.nextFloat() * 6.28f
                }
            }
            "particles" -> {
                val n = 48; parts = FloatArray(n * 4)
                for (i in 0 until n) {
                    parts[i * 4] = rnd.nextFloat() * w
                    parts[i * 4 + 1] = rnd.nextFloat() * h
                    parts[i * 4 + 2] = dp(10f) + rnd.nextFloat() * dp(28f)
                    parts[i * 4 + 3] = dp(2f) + rnd.nextFloat() * dp(4.5f)
                }
            }
            "rain" -> {
                val cols = (w / dp(20f)).toInt().coerceAtLeast(8); rain = FloatArray(cols * 4)
                for (i in 0 until cols) {
                    rain[i * 4] = i * dp(20f) + dp(6f)
                    rain[i * 4 + 1] = rnd.nextFloat() * h
                    rain[i * 4 + 2] = dp(60f) + rnd.nextFloat() * dp(140f)
                    rain[i * 4 + 3] = dp(90f) + rnd.nextFloat() * dp(180f)
                }
            }
            "matrix" -> {
                val cols = (w / dp(16f)).toInt().coerceAtLeast(12); mtx = FloatArray(cols * 4)
                for (i in 0 until cols) {
                    mtx[i * 4] = i * dp(16f) + dp(5f)
                    mtx[i * 4 + 1] = rnd.nextFloat() * h
                    mtx[i * 4 + 2] = dp(80f) + rnd.nextFloat() * dp(160f)
                    mtx[i * 4 + 3] = dp(120f) + rnd.nextFloat() * dp(220f)
                }
            }
            "constellation", "mesh" -> {
                val n = if (mode == "mesh") 26 else 34
                nodes = FloatArray(n * 4)
                netPos = FloatArray(n * 2)
                for (i in 0 until n) {
                    nodes[i * 4] = rnd.nextFloat() * w
                    nodes[i * 4 + 1] = rnd.nextFloat() * h
                    nodes[i * 4 + 2] = rnd.nextFloat() * 6.28f + i        // fase X
                    nodes[i * 4 + 3] = rnd.nextFloat() * 6.28f + i * 0.5f // fase Y
                }
            }
            "fireflies" -> {
                val n = 40; flies = FloatArray(n * 4)
                for (i in 0 until n) {
                    flies[i * 4] = rnd.nextFloat() * w
                    flies[i * 4 + 1] = rnd.nextFloat() * h
                    flies[i * 4 + 2] = rnd.nextFloat() * 6.28f
                    flies[i * 4 + 3] = 0.4f + rnd.nextFloat() * 0.9f
                }
            }
            "snow" -> {
                val n = 90; snow = FloatArray(n * 4)
                for (i in 0 until n) {
                    snow[i * 4] = rnd.nextFloat() * w
                    snow[i * 4 + 1] = rnd.nextFloat() * h
                    snow[i * 4 + 2] = dp(1.2f) + rnd.nextFloat() * dp(3.2f)
                    snow[i * 4 + 3] = 0.4f + rnd.nextFloat() * 1.4f
                }
            }
            "bubbles" -> {
                val n = 26; bubbles = FloatArray(n * 4)
                for (i in 0 until n) {
                    bubbles[i * 4] = rnd.nextFloat() * w
                    bubbles[i * 4 + 1] = dp(6f) + rnd.nextFloat() * dp(30f)
                    bubbles[i * 4 + 2] = dp(20f) + rnd.nextFloat() * dp(55f)
                    bubbles[i * 4 + 3] = rnd.nextFloat() * 6.28f
                }
            }
            "meteors" -> {
                val n = 14; meteors = FloatArray(n * 4)
                for (i in 0 until n) {
                    meteors[i * 4] = rnd.nextFloat() * w
                    meteors[i * 4 + 1] = rnd.nextFloat() * h
                    meteors[i * 4 + 2] = dp(80f) + rnd.nextFloat() * dp(160f)
                    meteors[i * 4 + 3] = dp(220f) + rnd.nextFloat() * dp(300f)
                }
            }
            "warp" -> {
                val n = 90; warp = FloatArray(n * 4)
                for (i in 0 until n) {
                    warp[i * 4] = rnd.nextFloat() * 6.28f
                    warp[i * 4 + 1] = rnd.nextFloat()             // 0..1 dist normalizada
                    warp[i * 4 + 2] = 0.25f + rnd.nextFloat() * 0.9f
                    warp[i * 4 + 3] = dp(1f) + rnd.nextFloat() * dp(2f)
                }
            }
            "vortex" -> {
                val n = 120; vortex = FloatArray(n * 4)
                for (i in 0 until n) {
                    vortex[i * 4] = rnd.nextFloat() * 6.28f
                    vortex[i * 4 + 1] = rnd.nextFloat()
                    vortex[i * 4 + 2] = 0.5f + rnd.nextFloat()
                    vortex[i * 4 + 3] = dp(1f) + rnd.nextFloat() * dp(2.2f)
                }
            }
            "embers" -> {
                val n = 55; embers = FloatArray(n * 4)
                for (i in 0 until n) {
                    embers[i * 4] = rnd.nextFloat() * w
                    embers[i * 4 + 1] = rnd.nextFloat() * h
                    embers[i * 4 + 2] = dp(30f) + rnd.nextFloat() * dp(70f)
                    embers[i * 4 + 3] = dp(1.4f) + rnd.nextFloat() * dp(2.6f)
                }
            }
            "sphere" -> {
                val n = 160; sphere = FloatArray(n * 3)
                for (i in 0 until n) {
                    // distribucion uniforme sobre esfera (metodo de la espiral de Fibonacci)
                    val y = 1f - (i + 0.5f) / n * 2f
                    val r = sqrt((1f - y * y).coerceAtLeast(0f))
                    val phi = i * 2.399963f
                    sphere[i * 3] = cos(phi) * r
                    sphere[i * 3 + 1] = y
                    sphere[i * 3 + 2] = sin(phi) * r
                }
            }
            "flow" -> {
                val n = 140; flow = FloatArray(n * 2)
                for (i in 0 until n) {
                    flow[i * 2] = rnd.nextFloat() * w
                    flow[i * 2 + 1] = rnd.nextFloat() * h
                }
            }
            "radar" -> {
                val n = 16; blips = FloatArray(n * 2)
                for (i in 0 until n) {
                    blips[i * 2] = rnd.nextFloat()          // radio 0..1
                    blips[i * 2 + 1] = rnd.nextFloat() * 6.28f // angulo
                }
            }
            "confetti" -> {
                val n = 60; conf = FloatArray(n * 5)
                for (i in 0 until n) {
                    conf[i * 5] = rnd.nextFloat() * w
                    conf[i * 5 + 1] = rnd.nextFloat() * h
                    conf[i * 5 + 2] = rnd.nextFloat() * 6.28f
                    conf[i * 5 + 3] = dp(40f) + rnd.nextFloat() * dp(90f)
                    conf[i * 5 + 4] = dp(3f) + rnd.nextFloat() * dp(4f)
                }
            }
            "fireworks" -> {
                val n = 6; fw = FloatArray(n * 3)
                for (i in 0 until n) {
                    fw[i * 3] = (0.2f + rnd.nextFloat() * 0.6f) * w
                    fw[i * 3 + 1] = (0.2f + rnd.nextFloat() * 0.5f) * h
                    fw[i * 3 + 2] = rnd.nextFloat() * 3f
                }
            }
            "smoke" -> {
                val n = 22; smoke = FloatArray(n * 4)
                for (i in 0 until n) {
                    smoke[i * 4] = rnd.nextFloat() * w
                    smoke[i * 4 + 1] = rnd.nextFloat() * h
                    smoke[i * 4 + 2] = dp(12f) + rnd.nextFloat() * dp(28f)
                    smoke[i * 4 + 3] = dp(50f) + rnd.nextFloat() * dp(90f)
                }
            }
            "droplets" -> {
                val n = 10; drops = FloatArray(n * 3)
                for (i in 0 until n) {
                    drops[i * 3] = rnd.nextFloat() * w
                    drops[i * 3 + 1] = rnd.nextFloat() * h
                    drops[i * 3 + 2] = rnd.nextFloat() * 4f
                }
            }
            "binary" -> {
                val cols = (w / dp(18f)).toInt().coerceAtLeast(10); bin = FloatArray(cols * 4)
                for (i in 0 until cols) {
                    bin[i * 4] = i * dp(18f) + dp(6f)
                    bin[i * 4 + 1] = rnd.nextFloat() * h
                    bin[i * 4 + 2] = dp(90f) + rnd.nextFloat() * dp(160f)
                    bin[i * 4 + 3] = dp(70f) + rnd.nextFloat() * dp(150f)
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        val t = if (startNs == 0L) 0f else (System.nanoTime() - startNs) / 1_000_000_000f
        val w = width.toFloat()
        val h = height.toFloat()
        when (mode) {
            "stars" -> drawStars(canvas, t, w, h)
            "particles" -> drawParticles(canvas, t, h)
            "breathe" -> drawBreathe(canvas, t, w, h)
            "waves" -> drawWaves(canvas, t, w, h)
            "rain" -> drawRain(canvas, t, h)
            "gradient" -> drawAurora(canvas, t, w, h)
            "grid" -> drawGrid(canvas, t, w, h)
            "constellation" -> drawNetwork(canvas, t, w, h, dp(120f), 150)
            "mesh" -> drawNetwork(canvas, t, w, h, dp(170f), 90)
            "fireflies" -> drawFireflies(canvas, t, w, h)
            "snow" -> drawSnow(canvas, t, w, h)
            "matrix" -> drawMatrix(canvas, t, h)
            "orbit" -> drawOrbit(canvas, t, w, h)
            "ripple" -> drawRipple(canvas, t, w, h)
            "spiral" -> drawSpiral(canvas, t, w, h)
            "dna" -> drawDna(canvas, t, w, h)
            "bubbles" -> drawBubbles(canvas, t, h)
            "meteors" -> drawMeteors(canvas, t, w, h)
            "nebula" -> drawNebula(canvas, t, w, h)
            "pulse" -> drawSonar(canvas, t, w, h)
            "equalizer" -> drawEqualizer(canvas, t, w, h)
            "warp" -> drawWarp(canvas, t, w, h)
            "noise" -> drawNoise(canvas, t, w, h)
            "vortex" -> drawVortex(canvas, t, w, h)
            "hexagons" -> drawHexagons(canvas, t, w, h)
            "contour" -> drawContour(canvas, t, w, h)
            "lissajous" -> drawLissajous(canvas, t, w, h)
            "pendulum" -> drawPendulum(canvas, t, w, h)
            "lightning" -> drawLightning(canvas, t, w, h)
            "embers" -> drawEmbers(canvas, t, h)
            "comet" -> drawComet(canvas, t, w, h)
            "radar" -> drawRadar(canvas, t, w, h)
            "heartbeat" -> drawHeartbeat(canvas, t, w, h)
            "scanline" -> drawScanline(canvas, t, w, h)
            "sphere" -> drawSphere(canvas, t, w, h)
            "flow" -> drawFlow(canvas, t, w, h)
            "tunnel" -> drawTunnel(canvas, t, w, h)
            "starburst" -> drawStarburst(canvas, t, w, h)
            "rings" -> drawRings(canvas, t, w, h)
            "binary" -> drawBinary(canvas, t, h)
            "confetti" -> drawConfetti(canvas, t, w, h)
            "fireworks" -> drawFireworks(canvas, t, w, h)
            "smoke" -> drawSmoke(canvas, t, h)
            "kaleidoscope" -> drawKaleidoscope(canvas, t, w, h)
            "waveform" -> drawWaveform(canvas, t, w, h)
            "plasma" -> drawPlasma(canvas, t, w, h)
            "droplets" -> drawDroplets(canvas, t, w, h)
            else -> {}
        }
    }

    // ---------------------------------------------------------------- originales

    private fun drawStars(c: Canvas, t: Float, w: Float, h: Float) {
        paint.style = Paint.Style.FILL; paint.shader = null
        val n = stars.size / 4
        for (i in 0 until n) {
            var x = stars[i * 4] + sin(t * 0.25f + i) * dp(4f)
            var y = stars[i * 4 + 1] + t * dp(5f)
            val size = stars[i * 4 + 2]; val phase = stars[i * 4 + 3]
            x = ((x % w) + w) % w; y = ((y % h) + h) % h
            val tw = 0.45f + 0.55f * (0.5f + 0.5f * sin(t * 2.2f + phase))
            paint.color = white((tw * 235).toInt())
            c.drawCircle(x, y, size, paint)
        }
    }

    private fun drawParticles(c: Canvas, t: Float, h: Float) {
        paint.style = Paint.Style.FILL; paint.shader = null
        val n = parts.size / 4
        for (i in 0 until n) {
            val x = parts[i * 4]; val speed = parts[i * 4 + 2]; val size = parts[i * 4 + 3]
            var y = parts[i * 4 + 1] - t * speed
            y = ((y % h) + h) % h
            val edge = (1f - abs(y / h - 0.5f) * 2f).coerceIn(0f, 1f)
            paint.color = white((40 + 110 * edge).toInt())
            c.drawCircle(x, y, size, paint)
        }
    }

    private fun drawBreathe(c: Canvas, t: Float, w: Float, h: Float) {
        val pulse = 0.5f + 0.5f * sin(t * 0.85f)
        val cx = w / 2f; val cy = h * 0.40f
        val r = (w * 0.30f) + (w * 0.22f) * pulse
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(cx, cy, r, white((70 + 60 * pulse).toInt()), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, paint); paint.shader = null
    }

    private fun drawWaves(c: Canvas, t: Float, w: Float, h: Float) {
        paint.style = Paint.Style.STROKE; paint.shader = null
        val step = dp(8f)
        for (l in 0 until 5) {
            paint.strokeWidth = dp(1.6f)
            paint.color = white((150 - l * 22).coerceAtLeast(40))
            val baseY = h * (0.30f + l * 0.11f); val amp = dp(16f) + l * dp(7f)
            var x = 0f; var prevY = baseY
            while (x <= w) {
                val y = baseY + amp * sin((x / w) * (2f * PI.toFloat()) * 1.4f + t * 1.2f + l * 0.7f)
                if (x > 0) c.drawLine(x - step, prevY, x, y, paint)
                prevY = y; x += step
            }
        }
    }

    private fun drawRain(c: Canvas, t: Float, h: Float) {
        paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(1.8f)
        val n = rain.size / 4
        for (i in 0 until n) {
            val x = rain[i * 4]; val len = rain[i * 4 + 2]; val speed = rain[i * 4 + 3]
            val y = (rain[i * 4 + 1] + t * speed) % (h + len); val top = y - len
            paint.shader = LinearGradient(x, top, x, y, Color.TRANSPARENT, white(210), Shader.TileMode.CLAMP)
            c.drawLine(x, top, x, y, paint)
            paint.shader = null; paint.style = Paint.Style.FILL; paint.color = white(235)
            c.drawCircle(x, y, dp(1.6f), paint); paint.style = Paint.Style.STROKE
        }
        paint.shader = null
    }

    private fun drawAurora(c: Canvas, t: Float, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        val cx1 = w * (0.35f + 0.2f * sin(t * 0.35f)); val cy1 = h * (0.35f + 0.12f * sin(t * 0.27f + 1f))
        paint.shader = RadialGradient(cx1, cy1, w * 0.7f, Color.argb(95, 210, 210, 215), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, paint)
        val cx2 = w * (0.7f + 0.2f * sin(t * 0.3f + 2f)); val cy2 = h * (0.6f + 0.15f * sin(t * 0.23f))
        paint.shader = RadialGradient(cx2, cy2, w * 0.65f, Color.argb(70, 180, 180, 190), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, paint); paint.shader = null
    }

    private fun drawGrid(c: Canvas, t: Float, w: Float, h: Float) {
        paint.style = Paint.Style.FILL; paint.shader = null
        val gap = dp(38f); val off = (t * dp(10f)) % gap
        var y = -gap + off
        while (y < h + gap) {
            var x = -gap + off
            while (x < w + gap) {
                val wave = 0.5f + 0.5f * sin(t * 1.4f + (x + y) * 0.012f)
                paint.color = white((30 + 70 * wave).toInt())
                c.drawCircle(x, y, dp(1.4f), paint); x += gap
            }
            y += gap
        }
    }

    // ---------------------------------------------------------------- nuevos

    /** Red de nodos con deriva suave; une con lineas los que estan cerca. */
    private fun drawNetwork(c: Canvas, t: Float, w: Float, h: Float, linkDist: Float, dotAlpha: Int) {
        val n = nodes.size / 4
        if (netPos.size != n * 2) netPos = FloatArray(n * 2)
        for (i in 0 until n) {
            netPos[i * 2] = nodes[i * 4] + sin(t * 0.3f + nodes[i * 4 + 2]) * dp(30f)
            netPos[i * 2 + 1] = nodes[i * 4 + 1] + cos(t * 0.25f + nodes[i * 4 + 3]) * dp(30f)
        }
        paint.style = Paint.Style.STROKE; paint.shader = null; paint.strokeWidth = dp(1f)
        for (i in 0 until n) {
            val xi = netPos[i * 2]; val yi = netPos[i * 2 + 1]
            for (j in i + 1 until n) {
                val d = hypot((xi - netPos[j * 2]).toDouble(), (yi - netPos[j * 2 + 1]).toDouble()).toFloat()
                if (d < linkDist) {
                    paint.color = white((110 * (1f - d / linkDist)).toInt())
                    c.drawLine(xi, yi, netPos[j * 2], netPos[j * 2 + 1], paint)
                }
            }
        }
        paint.style = Paint.Style.FILL; paint.color = white(dotAlpha)
        for (i in 0 until n) c.drawCircle(netPos[i * 2], netPos[i * 2 + 1], dp(2f), paint)
    }

    private fun drawFireflies(c: Canvas, t: Float, w: Float, h: Float) {
        paint.style = Paint.Style.FILL; paint.shader = null
        val n = flies.size / 4
        for (i in 0 until n) {
            val ph = flies[i * 4 + 2]; val sp = flies[i * 4 + 3]
            val x = flies[i * 4] + sin(t * sp + ph) * dp(30f)
            val y = flies[i * 4 + 1] + cos(t * sp * 0.8f + ph) * dp(30f)
            val glow = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * 2.5f + ph * 3f))
            paint.color = white((glow * 220).toInt())
            c.drawCircle(clamp(x, w), clamp(y, h), dp(2f), paint)
        }
    }

    private fun drawSnow(c: Canvas, t: Float, w: Float, h: Float) {
        paint.style = Paint.Style.FILL; paint.shader = null
        val n = snow.size / 4
        for (i in 0 until n) {
            val drift = snow[i * 4 + 3]
            var x = snow[i * 4] + sin(t * drift + i) * dp(16f)
            var y = snow[i * 4 + 1] + t * (dp(18f) + snow[i * 4 + 2] * 10f)
            x = ((x % w) + w) % w; y = ((y % h) + h) % h
            paint.color = white(200)
            c.drawCircle(x, y, snow[i * 4 + 2], paint)
        }
    }

    private fun drawMatrix(c: Canvas, t: Float, h: Float) {
        paint.shader = null
        val n = mtx.size / 4; val seg = dp(11f)
        for (i in 0 until n) {
            val x = mtx[i * 4]; val len = mtx[i * 4 + 2]; val speed = mtx[i * 4 + 3]
            val head = (mtx[i * 4 + 1] + t * speed) % (h + len)
            var y = head; var a = 255f
            paint.style = Paint.Style.FILL
            while (y > head - len && a > 12f) {
                paint.color = white(a.toInt())
                c.drawRect(x - dp(2f), y - seg * 0.6f, x + dp(2f), y, paint)
                y -= seg; a -= 255f / (len / seg)
            }
        }
    }

    private fun drawOrbit(c: Canvas, t: Float, w: Float, h: Float) {
        val cx = w / 2f; val cy = h * 0.42f
        paint.shader = null
        for (ring in 0 until 4) {
            val rad = dp(40f) + ring * dp(46f)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(0.8f)
            paint.color = white(28)
            c.drawCircle(cx, cy, rad, paint)
            val count = 2 + ring
            val dir = if (ring % 2 == 0) 1f else -1f
            val sp = 0.6f - ring * 0.1f
            paint.style = Paint.Style.FILL; paint.color = white(220)
            for (k in 0 until count) {
                val ang = dir * t * sp + k * (2f * PI.toFloat() / count)
                c.drawCircle(cx + cos(ang) * rad, cy + sin(ang) * rad, dp(2.6f), paint)
            }
        }
    }

    private fun drawRipple(c: Canvas, t: Float, w: Float, h: Float) {
        val cx = w / 2f; val cy = h * 0.42f
        paint.style = Paint.Style.STROKE; paint.shader = null; paint.strokeWidth = dp(1.6f)
        val maxR = hypot(w.toDouble(), h.toDouble()).toFloat() * 0.6f
        val waves = 6
        for (k in 0 until waves) {
            val prog = ((t * 0.35f + k.toFloat() / waves) % 1f)
            val r = prog * maxR
            paint.color = white(((1f - prog) * 150).toInt())
            c.drawCircle(cx, cy, r, paint)
        }
    }

    private fun drawSpiral(c: Canvas, t: Float, w: Float, h: Float) {
        val cx = w / 2f; val cy = h * 0.42f
        paint.style = Paint.Style.FILL; paint.shader = null
        val n = 150
        for (i in 0 until n) {
            val f = i.toFloat() / n
            val ang = f * 22f + t * 0.8f
            val rad = f * hypot(w.toDouble(), h.toDouble()).toFloat() * 0.42f
            paint.color = white((60 + 180 * f).toInt())
            c.drawCircle(cx + cos(ang) * rad, cy + sin(ang) * rad, dp(1f) + f * dp(2.4f), paint)
        }
    }

    private fun drawDna(c: Canvas, t: Float, w: Float, h: Float) {
        val cx = w / 2f; val amp = w * 0.20f
        paint.shader = null
        val step = dp(18f); var y = 0f; var i = 0
        while (y < h) {
            val ph = y * 0.03f + t * 1.6f
            val x1 = cx + sin(ph) * amp; val x2 = cx - sin(ph) * amp
            val depth = 0.5f + 0.5f * cos(ph)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(1f); paint.color = white(60)
            if (i % 2 == 0) c.drawLine(x1, y, x2, y, paint)
            paint.style = Paint.Style.FILL
            paint.color = white((90 + 150 * depth).toInt()); c.drawCircle(x1, y, dp(3f), paint)
            paint.color = white((90 + 150 * (1f - depth)).toInt()); c.drawCircle(x2, y, dp(3f), paint)
            y += step; i++
        }
    }

    private fun drawBubbles(c: Canvas, t: Float, h: Float) {
        paint.style = Paint.Style.STROKE; paint.shader = null; paint.strokeWidth = dp(1.4f)
        val n = bubbles.size / 4
        for (i in 0 until n) {
            val size = bubbles[i * 4 + 1]; val speed = bubbles[i * 4 + 2]; val ph = bubbles[i * 4 + 3]
            val x = bubbles[i * 4] + sin(t * 0.8f + ph) * dp(14f)
            var y = h - ((t * speed + ph * 40f) % (h + size * 2))
            paint.color = white(140)
            c.drawCircle(x, y, size, paint)
        }
    }

    private fun drawMeteors(c: Canvas, t: Float, w: Float, h: Float) {
        paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(2f)
        val n = meteors.size / 4; val diag = 0.6f
        for (i in 0 until n) {
            val len = meteors[i * 4 + 2]; val speed = meteors[i * 4 + 3]
            val travel = (t * speed + i * dp(120f)) % (w + h)
            val x = meteors[i * 4] + travel; val y = meteors[i * 4 + 1] + travel * diag
            val xr = ((x % (w + len)) )
            val tx = xr - len; val ty = y % (h + len) - len * diag
            paint.shader = LinearGradient(tx, ty, xr, ty + len * diag, Color.TRANSPARENT, white(230), Shader.TileMode.CLAMP)
            c.drawLine(tx, ty, xr, ty + len * diag, paint)
        }
        paint.shader = null
    }

    private fun drawNebula(c: Canvas, t: Float, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        val blobs = 4
        for (k in 0 until blobs) {
            val cx = w * (0.5f + 0.42f * sin(t * (0.18f + k * 0.05f) + k))
            val cy = h * (0.45f + 0.30f * cos(t * (0.15f + k * 0.04f) + k * 2))
            paint.shader = RadialGradient(cx, cy, w * (0.45f + k * 0.06f), white(60), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w, h, paint)
        }
        paint.shader = null
    }

    private fun drawSonar(c: Canvas, t: Float, w: Float, h: Float) {
        val cx = w / 2f; val cy = h * 0.85f
        paint.style = Paint.Style.STROKE; paint.shader = null; paint.strokeWidth = dp(2f)
        val maxR = h * 0.9f; val waves = 5
        for (k in 0 until waves) {
            val prog = ((t * 0.4f + k.toFloat() / waves) % 1f)
            paint.color = white(((1f - prog) * 170).toInt())
            c.drawCircle(cx, cy, prog * maxR, paint)
        }
        paint.style = Paint.Style.FILL; paint.color = white(230)
        c.drawCircle(cx, cy, dp(3f), paint)
    }

    private fun drawEqualizer(c: Canvas, t: Float, w: Float, h: Float) {
        paint.style = Paint.Style.FILL; paint.shader = null
        val bars = 22; val gap = dp(4f)
        val bw = (w - gap * (bars + 1)) / bars; val baseY = h * 0.72f; val maxH = h * 0.24f
        for (i in 0 until bars) {
            val amp = (0.2f + 0.8f * abs(sin(t * (1.4f + i * 0.09f) + i))) * maxH
            val x = gap + i * (bw + gap)
            paint.color = white((110 + 120 * (amp / maxH)).toInt())
            c.drawRoundRect(x, baseY - amp, x + bw, baseY, dp(2f), dp(2f), paint)
        }
    }

    private fun drawWarp(c: Canvas, t: Float, w: Float, h: Float) {
        val cx = w / 2f; val cy = h * 0.45f
        paint.style = Paint.Style.STROKE; paint.shader = null
        val maxR = hypot(w.toDouble(), h.toDouble()).toFloat() * 0.6f
        val n = warp.size / 4
        for (i in 0 until n) {
            val ang = warp[i * 4]; val sp = warp[i * 4 + 2]
            val d = ((warp[i * 4 + 1] + t * sp * 0.35f) % 1f)
            val r = d * maxR; val r2 = (r - dp(30f)).coerceAtLeast(0f)
            paint.strokeWidth = warp[i * 4 + 3]
            paint.color = white((d * 220).toInt())
            c.drawLine(cx + cos(ang) * r2, cy + sin(ang) * r2, cx + cos(ang) * r, cy + sin(ang) * r, paint)
        }
    }

    private fun drawNoise(c: Canvas, t: Float, w: Float, h: Float) {
        paint.style = Paint.Style.FILL; paint.shader = null
        val rnd = Random((t * 60f).toInt().toLong())
        val n = 220
        for (i in 0 until n) {
            paint.color = white(60 + rnd.nextInt(160))
            c.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, dp(1.2f), paint)
        }
    }

    private fun drawVortex(c: Canvas, t: Float, w: Float, h: Float) {
        val cx = w / 2f; val cy = h * 0.42f
        paint.style = Paint.Style.FILL; paint.shader = null
        val maxR = hypot(w.toDouble(), h.toDouble()).toFloat() * 0.5f
        val n = vortex.size / 4
        for (i in 0 until n) {
            val sp = vortex[i * 4 + 2]
            val d = 1f - ((vortex[i * 4 + 1] + t * 0.12f) % 1f)   // hacia el centro
            val ang = vortex[i * 4] + t * sp + (1f - d) * 6f
            val r = d * maxR
            paint.color = white((200 * (1f - d)).toInt().coerceIn(20, 230))
            c.drawCircle(cx + cos(ang) * r, cy + sin(ang) * r, vortex[i * 4 + 3], paint)
        }
    }

    private fun drawHexagons(c: Canvas, t: Float, w: Float, h: Float) {
        paint.style = Paint.Style.STROKE; paint.shader = null; paint.strokeWidth = dp(1f)
        val s = dp(30f); val hw = s * 1.5f; val hh = (sqrt(3.0).toFloat() * s)
        var row = 0; var y = -hh
        while (y < h + hh) {
            val xoff = if (row % 2 == 0) 0f else hw * 0.5f
            var x = -hw + xoff
            while (x < w + hw) {
                val pulse = 0.5f + 0.5f * sin(t * 1.6f + (x + y) * 0.02f)
                paint.color = white((25 + 90 * pulse).toInt())
                hexPath(x, y, s * 0.9f); c.drawPath(path, paint)
                x += hw
            }
            y += hh * 0.5f; row++
        }
    }

    private fun drawContour(c: Canvas, t: Float, w: Float, h: Float) {
        paint.style = Paint.Style.STROKE; paint.shader = null; paint.strokeWidth = dp(1.2f)
        val lines = 12; val step = dp(10f)
        for (l in 0 until lines) {
            paint.color = white(70)
            val baseY = h * (0.1f + l * 0.07f)
            var x = 0f; var prevY = baseY
            while (x <= w) {
                val y = baseY +
                    sin((x / w) * 6f + t * 0.6f + l * 0.5f) * dp(10f) +
                    sin((x / w) * 13f - t * 0.4f + l) * dp(5f)
                if (x > 0) c.drawLine(x - step, prevY, x, y, paint)
                prevY = y; x += step
            }
        }
    }

    private fun drawLissajous(c: Canvas, t: Float, w: Float, h: Float) {
        val cx = w / 2f; val cy = h * 0.42f
        val ax = w * 0.38f; val ay = h * 0.24f
        paint.style = Paint.Style.FILL; paint.shader = null
        val n = 220; val a = 3f; val b = 2f; val delta = t * 0.3f
        for (i in 0 until n) {
            val p = i.toFloat() / n * 2f * PI.toFloat()
            val x = cx + ax * sin(a * p + delta); val y = cy + ay * sin(b * p)
            paint.color = white((60 + 180 * (i.toFloat() / n)).toInt())
            c.drawCircle(x, y, dp(1.8f), paint)
        }
    }

    private fun drawPendulum(c: Canvas, t: Float, w: Float, h: Float) {
        paint.shader = null
        val count = 5; val topY = h * 0.12f
        for (k in 0 until count) {
            val pivotX = w * (0.2f + k * 0.15f)
            val len = h * (0.35f + k * 0.06f)
            val swing = sin(t * (1.2f + k * 0.12f)) * (PI.toFloat() / 5f)
            val bx = pivotX + sin(swing) * len; val by = topY + cos(swing) * len
            paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(1f); paint.color = white(70)
            c.drawLine(pivotX, topY, bx, by, paint)
            paint.style = Paint.Style.FILL; paint.color = white(230)
            c.drawCircle(bx, by, dp(6f), paint)
        }
    }

    private fun drawLightning(c: Canvas, t: Float, w: Float, h: Float) {
        val period = 1.7f; val idx = (t / period).toInt(); val local = t - idx * period
        if (local > 0.35f) return
        val a = (1f - local / 0.35f)
        val rnd = Random(idx.toLong() * 131 + 7)
        // destello de fondo
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(w * 0.5f, h * 0.3f, w, white((30 * a).toInt()), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, paint); paint.shader = null
        // rayo
        paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(2.4f); paint.color = white((240 * a).toInt())
        path.reset()
        var x = w * (0.2f + rnd.nextFloat() * 0.6f); var y = 0f
        path.moveTo(x, y)
        while (y < h) {
            y += h / 9f; x += (rnd.nextFloat() - 0.5f) * dp(60f)
            path.lineTo(x.coerceIn(0f, w), y)
        }
        c.drawPath(path, paint)
    }

    private fun drawEmbers(c: Canvas, t: Float, h: Float) {
        paint.style = Paint.Style.FILL; paint.shader = null
        val n = embers.size / 4
        for (i in 0 until n) {
            val speed = embers[i * 4 + 2]; val size = embers[i * 4 + 3]
            var y = embers[i * 4 + 1] - t * speed
            y = ((y % h) + h) % h
            val x = embers[i * 4] + sin(t * 1.5f + i) * dp(10f)
            val flick = 0.4f + 0.6f * (0.5f + 0.5f * sin(t * 6f + i * 2f))
            val rise = 1f - y / h
            paint.color = white((flick * 200 * (0.4f + rise)).toInt().coerceIn(0, 255))
            c.drawCircle(x, y, size, paint)
        }
    }

    private fun drawComet(c: Canvas, t: Float, w: Float, h: Float) {
        val period = 5f; val local = (t % period) / period
        val x = local * (w + dp(200f)) - dp(100f); val y = h * (0.2f + 0.25f * sin(local * PI.toFloat()))
        // cola
        paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(3f)
        val tailX = x - dp(160f); val tailY = y - dp(70f)
        paint.shader = LinearGradient(tailX, tailY, x, y, Color.TRANSPARENT, white(230), Shader.TileMode.CLAMP)
        c.drawLine(tailX, tailY, x, y, paint); paint.shader = null
        paint.style = Paint.Style.FILL; paint.color = white(255)
        c.drawCircle(x, y, dp(4f), paint)
        paint.shader = RadialGradient(x, y, dp(16f), white(120), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawCircle(x, y, dp(16f), paint); paint.shader = null
    }

    private fun drawRadar(c: Canvas, t: Float, w: Float, h: Float) {
        val cx = w / 2f; val cy = h * 0.42f
        val maxR = hypot(w.toDouble(), h.toDouble()).toFloat() * 0.45f
        paint.style = Paint.Style.STROKE; paint.shader = null; paint.strokeWidth = dp(0.8f); paint.color = white(28)
        for (k in 1..4) c.drawCircle(cx, cy, maxR * k / 4f, paint)
        val beam = (t * 1.1f) % (2f * PI.toFloat())
        // haz con estela (varios segmentos que se desvanecen)
        for (s in 0 until 26) {
            val ang = beam - s * 0.05f
            paint.color = white((120 * (1f - s / 26f)).toInt())
            paint.strokeWidth = dp(2f)
            c.drawLine(cx, cy, cx + cos(ang) * maxR, cy + sin(ang) * maxR, paint)
        }
        // blips que brillan al pasar el haz
        paint.style = Paint.Style.FILL
        val n = blips.size / 2
        for (i in 0 until n) {
            val r = blips[i * 2] * maxR; val ang = blips[i * 2 + 1]
            var diff = (beam - ang) % (2f * PI.toFloat()); if (diff < 0) diff += 2f * PI.toFloat()
            val glow = (1f - diff / (2f * PI.toFloat()))
            paint.color = white((glow * glow * 240).toInt())
            c.drawCircle(cx + cos(ang) * r, cy + sin(ang) * r, dp(3f), paint)
        }
    }

    private fun drawHeartbeat(c: Canvas, t: Float, w: Float, h: Float) {
        paint.style = Paint.Style.STROKE; paint.shader = null; paint.strokeWidth = dp(2f); paint.color = white(220)
        val baseY = h * 0.42f; val amp = h * 0.16f; val period = w * 0.5f; val speed = w * 0.35f
        path.reset(); var first = true; var x = 0f
        while (x <= w) {
            val u = (((x + t * speed) % period) / period)
            path.let {
                val y = baseY - ecg(u) * amp
                if (first) { it.moveTo(x, y); first = false } else it.lineTo(x, y)
            }
            x += dp(3f)
        }
        c.drawPath(path, paint)
    }

    private fun drawScanline(c: Canvas, t: Float, w: Float, h: Float) {
        paint.shader = null
        // rejilla tenue
        paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(0.6f); paint.color = white(16)
        var gy = 0f; val gap = dp(26f)
        while (gy < h) { c.drawLine(0f, gy, w, gy, paint); gy += gap }
        // barra brillante que recorre
        val y = (t * dp(140f)) % (h + dp(80f)) - dp(40f)
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(0f, y - dp(40f), 0f, y + dp(40f),
            intArrayOf(Color.TRANSPARENT, white(120), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, y - dp(40f), w, y + dp(40f), paint); paint.shader = null
        paint.color = white(200); c.drawRect(0f, y - dp(1f), w, y + dp(1f), paint)
    }

    private fun drawSphere(c: Canvas, t: Float, w: Float, h: Float) {
        val cx = w / 2f; val cy = h * 0.42f; val rad = w * 0.32f
        val ca = cos(t * 0.5f); val sa = sin(t * 0.5f)
        val cb = cos(t * 0.22f); val sb = sin(t * 0.22f)
        paint.style = Paint.Style.FILL; paint.shader = null
        val n = sphere.size / 3
        for (i in 0 until n) {
            var x = sphere[i * 3]; var y = sphere[i * 3 + 1]; var z = sphere[i * 3 + 2]
            // rotacion Y
            val x1 = x * ca - z * sa; val z1 = x * sa + z * ca; x = x1; z = z1
            // rotacion X
            val y1 = y * cb - z * sb; val z2 = y * sb + z * cb; y = y1; z = z2
            val depth = (z + 1f) / 2f
            paint.color = white((30 + 200 * depth).toInt())
            c.drawCircle(cx + x * rad, cy + y * rad, dp(1f) + depth * dp(2f), paint)
        }
    }

    private fun drawFlow(c: Canvas, t: Float, w: Float, h: Float) {
        paint.style = Paint.Style.FILL; paint.shader = null
        val n = flow.size / 2; val step = dp(1.6f)
        for (i in 0 until n) {
            var x = flow[i * 2]; var y = flow[i * 2 + 1]
            val ang = (sin(x * 0.01f + t * 0.4f) + cos(y * 0.011f - t * 0.3f)) * PI.toFloat()
            x += cos(ang) * step; y += sin(ang) * step
            x = ((x % w) + w) % w; y = ((y % h) + h) % h
            flow[i * 2] = x; flow[i * 2 + 1] = y
            paint.color = white(150)
            c.drawCircle(x, y, dp(1.3f), paint)
        }
    }

    private fun drawTunnel(c: Canvas, t: Float, w: Float, h: Float) {
        val cx = w / 2f; val cy = h * 0.42f
        paint.style = Paint.Style.STROKE; paint.shader = null; paint.strokeWidth = dp(1.4f)
        val rings = 14; val maxR = hypot(w.toDouble(), h.toDouble()).toFloat() * 0.6f
        for (k in 0 until rings) {
            val prog = ((t * 0.3f + k.toFloat() / rings) % 1f)
            val r = prog * prog * maxR   // aceleracion = sensacion de profundidad
            val rot = t * 0.4f + k * 0.2f
            paint.color = white((220 * (1f - prog)).toInt())
            squarePath(cx, cy, r, rot); c.drawPath(path, paint)
        }
    }

    private fun drawStarburst(c: Canvas, t: Float, w: Float, h: Float) {
        val cx = w / 2f; val cy = h * 0.42f
        paint.style = Paint.Style.STROKE; paint.shader = null
        val rays = 28; val maxR = hypot(w.toDouble(), h.toDouble()).toFloat() * 0.55f
        for (i in 0 until rays) {
            val ang = i * (2f * PI.toFloat() / rays) + t * 0.15f
            val len = maxR * (0.4f + 0.6f * (0.5f + 0.5f * sin(t * 2f + i)))
            paint.strokeWidth = dp(1.4f)
            paint.shader = LinearGradient(cx, cy, cx + cos(ang) * len, cy + sin(ang) * len,
                white(200), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            c.drawLine(cx, cy, cx + cos(ang) * len, cy + sin(ang) * len, paint)
        }
        paint.shader = null; paint.style = Paint.Style.FILL; paint.color = white(255)
        c.drawCircle(cx, cy, dp(4f), paint)
    }

    private fun drawRings(c: Canvas, t: Float, w: Float, h: Float) {
        val cx = w / 2f; val cy = h * 0.42f
        paint.style = Paint.Style.STROKE; paint.shader = null
        for (k in 0 until 5) {
            val rad = dp(40f) + k * dp(34f)
            val dashes = 10 + k * 4; val dir = if (k % 2 == 0) 1f else -1f
            val rot = dir * t * (0.5f - k * 0.06f)
            paint.strokeWidth = dp(2.2f)
            for (d in 0 until dashes) {
                val a0 = rot + d * (2f * PI.toFloat() / dashes)
                val a1 = a0 + (PI.toFloat() / dashes) * 0.9f
                paint.color = white(120 - k * 12)
                path.reset()
                path.moveTo(cx + cos(a0) * rad, cy + sin(a0) * rad)
                path.arcTo(cx - rad, cy - rad, cx + rad, cy + rad,
                    Math.toDegrees(a0.toDouble()).toFloat(),
                    Math.toDegrees((a1 - a0).toDouble()).toFloat(), false)
                c.drawPath(path, paint)
            }
        }
    }

    private fun drawBinary(c: Canvas, t: Float, h: Float) {
        paint.style = Paint.Style.FILL; paint.shader = null
        paint.textSize = dp(13f); paint.textAlign = Paint.Align.CENTER
        val n = bin.size / 4; val seg = dp(15f)
        for (i in 0 until n) {
            val x = bin[i * 4]; val len = bin[i * 4 + 2]; val speed = bin[i * 4 + 3]
            val head = (bin[i * 4 + 1] + t * speed) % (h + len)
            var y = head; var a = 255f
            while (y > head - len && a > 12f) {
                val ch = if (((x.toInt() + (y / seg).toInt() * 7 + (t * 3).toInt()) % 2) == 0) "0" else "1"
                paint.color = white(a.toInt()); c.drawText(ch, x, y, paint)
                y -= seg; a -= 255f / (len / seg)
            }
        }
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawConfetti(c: Canvas, t: Float, w: Float, h: Float) {
        paint.style = Paint.Style.FILL; paint.shader = null
        val n = conf.size / 5
        for (i in 0 until n) {
            val speed = conf[i * 5 + 3]; val size = conf[i * 5 + 4]
            val x = conf[i * 5] + sin(t * 0.8f + i) * dp(20f)
            var y = (conf[i * 5 + 1] + t * speed) % (h + size * 2)
            val rot = Math.toDegrees((conf[i * 5 + 2] + t * 2.5f).toDouble()).toFloat()
            paint.color = white(190)
            c.save(); c.translate(x, y); c.rotate(rot)
            c.drawRect(-size, -size * 0.55f, size, size * 0.55f, paint)
            c.restore()
        }
    }

    private fun drawFireworks(c: Canvas, t: Float, w: Float, h: Float) {
        paint.style = Paint.Style.FILL; paint.shader = null
        val n = fw.size / 3; val period = 2.6f; val sparks = 26
        val maxR = w * 0.28f
        for (b in 0 until n) {
            val cx = fw[b * 3]; val cy = fw[b * 3 + 1]
            val phase = ((t + fw[b * 3 + 2]) % period) / period
            val r = phase * maxR; val a = ((1f - phase) * 230).toInt()
            if (a <= 4) continue
            for (k in 0 until sparks) {
                val ang = k * (2f * PI.toFloat() / sparks)
                val px = cx + cos(ang) * r
                val py = cy + sin(ang) * r + phase * phase * dp(40f)
                paint.color = white(a)
                c.drawCircle(px, py, dp(2f), paint)
            }
        }
    }

    private fun drawSmoke(c: Canvas, t: Float, h: Float) {
        paint.style = Paint.Style.FILL
        val n = smoke.size / 4
        for (i in 0 until n) {
            val size = smoke[i * 4 + 2]; val speed = smoke[i * 4 + 3]
            var y = smoke[i * 4 + 1] - t * speed
            y = ((y % h) + h) % h
            val x = smoke[i * 4] + sin(t * 0.4f + i) * dp(34f)
            val rise = 1f - y / h
            val r = size * (1.4f + rise * 1.6f)
            paint.shader = RadialGradient(x, y, r, white((45 * (0.3f + rise)).toInt()), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            c.drawCircle(x, y, r, paint)
        }
        paint.shader = null
    }

    private fun drawKaleidoscope(c: Canvas, t: Float, w: Float, h: Float) {
        val cx = w / 2f; val cy = h * 0.42f
        paint.style = Paint.Style.FILL; paint.shader = null
        val seg = 6
        for (i in 0 until 44) {
            val baseAng = i * 0.32f + t * 0.5f
            val r = (i % 11) * dp(13f) + dp(8f)
            val alpha = 60 + (i * 5) % 160
            for (s in 0 until seg) {
                val rotk = s * (2f * PI.toFloat() / seg)
                val a1 = baseAng + rotk
                paint.color = white(alpha)
                c.drawCircle(cx + cos(a1) * r, cy + sin(a1) * r, dp(2f), paint)
                val a2 = rotk - baseAng   // espejo
                c.drawCircle(cx + cos(a2) * r, cy + sin(a2) * r, dp(2f), paint)
            }
        }
    }

    private fun drawWaveform(c: Canvas, t: Float, w: Float, h: Float) {
        paint.style = Paint.Style.STROKE; paint.shader = null; paint.strokeWidth = dp(2.4f)
        val cy = h * 0.42f; val bars = 46; val gap = w / bars
        for (i in 0 until bars) {
            val f = i.toFloat() / bars
            val amp = (abs(sin(t * 1.6f + f * 10f)) * 0.6f + abs(sin(t * 2.3f + f * 22f)) * 0.4f) * h * 0.16f
            val x = gap * i + gap / 2
            paint.color = white((120 + 120 * (amp / (h * 0.16f))).toInt())
            c.drawLine(x, cy - amp, x, cy + amp, paint)
        }
    }

    private fun drawPlasma(c: Canvas, t: Float, w: Float, h: Float) {
        paint.style = Paint.Style.FILL; paint.shader = null
        val cell = dp(26f)
        var y = 0f
        while (y < h) {
            var x = 0f
            while (x < w) {
                val v = sin(x * 0.03f + t) + sin(y * 0.04f - t * 0.8f) + sin((x + y) * 0.02f + t * 1.3f)
                val bright = (v + 3f) / 6f
                paint.color = white((20 + 150 * bright).toInt())
                c.drawRect(x, y, x + cell, y + cell, paint)
                x += cell
            }
            y += cell
        }
    }

    private fun drawDroplets(c: Canvas, t: Float, w: Float, h: Float) {
        paint.shader = null
        val n = drops.size / 3; val period = 3f; val maxR = dp(75f)
        for (i in 0 until n) {
            val x = drops[i * 3]; val y = drops[i * 3 + 1]
            val phase = ((t + drops[i * 3 + 2]) % period) / period
            val r = phase * maxR; val a = ((1f - phase) * 170).toInt()
            paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(1.6f); paint.color = white(a)
            c.drawCircle(x, y, r, paint)
            if (phase < 0.15f) {
                paint.style = Paint.Style.FILL; paint.color = white(200)
                c.drawCircle(x, y, dp(2.5f), paint)
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun ecg(u: Float): Float = when {
        u < 0.10f -> sin(u / 0.10f * PI.toFloat()) * 0.12f      // onda P
        u < 0.16f -> -0.10f                                     // Q
        u < 0.20f -> (u - 0.16f) / 0.04f * 1.0f                 // subida R
        u < 0.24f -> 1.0f - (u - 0.20f) / 0.04f * 1.35f         // bajada a S
        u < 0.28f -> -0.35f + (u - 0.24f) / 0.04f * 0.35f       // recupera de S
        u < 0.50f -> sin((u - 0.34f) / 0.16f * PI.toFloat()).coerceAtLeast(0f) * 0.28f // onda T
        else -> 0f
    }

    private fun hexPath(cx: Float, cy: Float, r: Float) {
        path.reset()
        for (i in 0 until 6) {
            val a = i * (PI.toFloat() / 3f) + PI.toFloat() / 6f
            val x = cx + cos(a) * r; val y = cy + sin(a) * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }

    private fun squarePath(cx: Float, cy: Float, r: Float, rot: Float) {
        path.reset()
        for (i in 0 until 4) {
            val a = rot + i * (PI.toFloat() / 2f) + PI.toFloat() / 4f
            val x = cx + cos(a) * r; val y = cy + sin(a) * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }

    private fun bounce(v: Float, max: Float): Float {
        val m = max.coerceAtLeast(1f)
        var x = v % (2 * m); if (x < 0) x += 2 * m
        return if (x > m) 2 * m - x else x
    }

    private fun clamp(v: Float, max: Float): Float = v.coerceIn(0f, max)

    private fun white(a: Int) = Color.argb(a.coerceIn(0, 255), 255, 255, 255)

    private fun dp(v: Float) = v * resources.displayMetrics.density
}
