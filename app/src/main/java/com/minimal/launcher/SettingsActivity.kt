package com.minimal.launcher

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/** Panel de ajustes "liquid glass": tarjetas translucidas con sliders, segments y switches. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var bgView: AnimatedBackgroundView
    private var pendingGestureKey: String? = null
    private var firstBuild = true

    private val pickApp = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@registerForActivityResult
        val key = pendingGestureKey ?: return@registerForActivityResult
        val pkg = data.getStringExtra(AppDrawerActivity.EXTRA_PKG) ?: return@registerForActivityResult
        val cls = data.getStringExtra(AppDrawerActivity.EXTRA_CLS) ?: return@registerForActivityResult
        val label = data.getStringExtra(AppDrawerActivity.EXTRA_LABEL) ?: pkg
        Prefs.setGesture(this, key, Gestures.appAction(pkg, cls, label))
        pendingGestureKey = null
        build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        container = findViewById(R.id.settings_container)
        bgView = findViewById(R.id.settings_bg)
        setupBlurredBackground()
    }

    override fun onResume() {
        super.onResume()
        bgView.start()
        build()
    }

    override fun onPause() {
        bgView.stop()
        super.onPause()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.modal_out)
    }

    /** Fondo animado del usuario, difuminado como cristal esmerilado (API 31+). */
    private fun setupBlurredBackground() {
        val userBg = Prefs.background(this)
        bgView.mode = if (userBg == "none") "nebula" else userBg
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val r = 55f * resources.displayMetrics.density
            bgView.setRenderEffect(
                RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP)
            )
        }
    }

    private fun build() {
        // conserva el titulo (hijo 0), reconstruye el resto
        while (container.childCount > 1) container.removeViewAt(1)
        firstBuild = firstBuild && container.childCount <= 1

        // --- RELOJ ---
        section("Reloj") { card ->
            card.addView(SettingsUi.navRow(this, "Tipo de reloj", ClockFaces.labelFor(Prefs.clockStyle(this))) {
                Sheets.clockStylePicker(this) { build() }
            })
            card.addView(div())
            card.addView(SettingsUi.sliderRow(
                this, "Tamaño", 60, 170, (Prefs.clockScale(this) * 100).toInt(),
                { "$it%" }) { Prefs.setClockScale(this, it / 100f) })
            card.addView(div())
            card.addView(SettingsUi.segment(
                this, "Alineación", listOf("Izquierda", "Centro"),
                if (Prefs.align(this) == "center") 1 else 0) { i ->
                Prefs.setAlign(this, if (i == 1) "center" else "start"); build()
            })
            card.addView(div())
            card.addView(SettingsUi.switchRow(this, "Formato 24 horas", null, Prefs.clock24(this)) {
                Prefs.setClock24(this, it)
            })
            card.addView(div())
            card.addView(SettingsUi.switchRow(this, "Mostrar fecha", null, Prefs.showDate(this)) {
                Prefs.setShowDate(this, it)
            })
        }

        // --- PANTALLA ---
        section("Pantalla") { card ->
            card.addView(SettingsUi.navRow(this, "Fondo animado", Backgrounds.labelFor(Prefs.background(this))) {
                Sheets.backgroundPicker(this) { build() }
            })
            card.addView(div())
            card.addView(SettingsUi.navRow(this, "Tipo de letra", Fonts.labelFor(Prefs.font(this))) {
                Sheets.fontPicker(this) { build() }
            })
        }

        // --- BLOQUEO ---
        section("Pantalla de bloqueo") { card ->
            card.addView(SettingsUi.navRow(
                this, "Fondo de bloqueo", Backgrounds.lockLabelFor(Prefs.lockBackground(this))
            ) {
                Sheets.backgroundPicker(
                    ctx = this,
                    title = "Fondo de bloqueo",
                    current = Prefs.lockBackground(this),
                    options = Backgrounds.lockOptions,
                    onPick = { Prefs.setLockBackground(this, it) }
                ) { build() }
            })
            card.addView(div())
            card.addView(SettingsUi.switchRow(this, "Reloj sobre el fondo", null, Prefs.lockClock(this)) {
                Prefs.setLockClock(this, it)
            })
            card.addView(div())
            card.addView(SettingsUi.switchRow(this, "Fecha sobre el fondo", null, Prefs.lockDate(this)) {
                Prefs.setLockDate(this, it)
            })
            card.addView(div())
            card.addView(SettingsUi.switchRow(this, "Batería sobre el fondo", null, Prefs.lockBattery(this)) {
                Prefs.setLockBattery(this, it)
            })
            card.addView(div())
            card.addView(SettingsUi.sliderRow(
                this, "Fluidez", Prefs.MIN_FPS, Prefs.MAX_FPS, Prefs.wallpaperFps(this),
                { "$it fps" }) { Prefs.setWallpaperFps(this, it) })
            card.addView(div())
            card.addView(SettingsUi.primaryButton(this, "Activar fondo animado") {
                openLiveWallpaperPicker()
            })
        }

        // --- ACCESOS ---
        section("Accesos directos") { card ->
            card.addView(SettingsUi.sliderRow(
                this, "Cantidad", Prefs.MIN_SLOTS, Prefs.MAX_SLOTS, Prefs.slotCount(this),
                { "$it" }) { Prefs.setSlotCount(this, it) })
            card.addView(div())
            card.addView(SettingsUi.segment(
                this, "Diseño", listOf("Lista", "Cuadrícula"),
                if (Prefs.layoutMode(this) == "grid") 1 else 0) { i ->
                Prefs.setLayoutMode(this, if (i == 1) "grid" else "list"); build()
            })
        }

        // --- GESTOS ---
        section("Gestos") { card ->
            card.addView(SettingsUi.navRow(this, "Configurar gestos", gestureSummary()) {
                Sheets.gestureSheet(this, { key -> pickGestureApp(key) }) { build() }
            })
        }

        // --- ESTILO ---
        section("Estilo") { card ->
            card.addView(SettingsUi.navRow(this, "Plantillas", Prefs.template(this).ifEmpty { "Personalizado" }) {
                Sheets.templatePicker(this) { build() }
            })
        }

        // --- Acciones ---
        val footer = SettingsUi.card(this)
        footer.addView(SettingsUi.primaryButton(this, "Launcher predeterminado") { openHomeSettings() })
        container.addView(footer)

        if (firstBuild) animateIn()
        firstBuild = false
    }

    private inline fun section(label: String, block: (LinearLayout) -> Unit) {
        container.addView(SettingsUi.sectionLabel(this, label))
        val card = SettingsUi.card(this)
        block(card)
        container.addView(card)
    }

    private fun div(): View = SettingsUi.innerDivider(this)

    private fun gestureSummary(): String =
        Gestures.directions.joinToString("   ") { (key, _) ->
            "${Gestures.arrow(key)} ${Gestures.label(Prefs.gesture(this, key))}"
        }

    private fun pickGestureApp(key: String) {
        pendingGestureKey = key
        val intent = Intent(this, AppDrawerActivity::class.java)
            .putExtra(AppDrawerActivity.EXTRA_PICK, true)
        pickApp.launch(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /**
     * Abre el selector del sistema ya posicionado en nuestro live wallpaper. Algunos
     * fabricantes no resuelven el intent directo: se cae al selector generico y, si
     * tampoco existe, se avisa al usuario en vez de fallar en silencio.
     */
    private fun openLiveWallpaperPicker() {
        val component = ComponentName(this, MinimalWallpaperService::class.java)
        val direct = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
        if (startIfResolvable(direct)) return
        if (startIfResolvable(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))) return
        android.widget.Toast.makeText(
            this,
            "Actívalo desde Ajustes › Fondo de pantalla › Fondos animados",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    private fun startIfResolvable(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }

    private fun openHomeSettings() {
        try {
            startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "No disponible", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun animateIn() {
        var i = 0L
        for (c in 1 until container.childCount) {
            Motion.enter(container.getChildAt(c), 40 + i * 30)
            i++
        }
    }
}
