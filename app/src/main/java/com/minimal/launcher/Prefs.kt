package com.minimal.launcher

import android.content.Context
import org.json.JSONObject

/** Un acceso directo del menu principal. pkg vacio = slot sin configurar. */
data class Slot(
    val pkg: String = "",
    val cls: String = "",
    val label: String = "",
    val icon: String = IconCatalog.DEFAULT_NAME
) {
    val isEmpty: Boolean get() = pkg.isEmpty()
}

/**
 * Persistencia en SharedPreferences (sin BD ni librerias: minimo overhead).
 * Guarda slots + ajustes de personalizacion.
 */
object Prefs {

    const val MIN_SLOTS = 1
    const val MAX_SLOTS = 10
    const val DEFAULT_SLOTS = 6

    private const val FILE = "minimal_launcher"
    private const val KEY_SLOT = "slot_"
    private const val KEY_COUNT = "slot_count"
    private const val KEY_FONT = "font"
    private const val KEY_BG = "bg"
    private const val KEY_CLOCK24 = "clock24"
    private const val KEY_SHOWDATE = "show_date"
    private const val KEY_CLOCKSCALE = "clock_scale"
    private const val KEY_ALIGN = "align"
    private const val KEY_TEMPLATE = "template"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // --- Slots ---

    fun slotCount(ctx: Context): Int =
        prefs(ctx).getInt(KEY_COUNT, DEFAULT_SLOTS).coerceIn(MIN_SLOTS, MAX_SLOTS)

    fun setSlotCount(ctx: Context, count: Int) {
        prefs(ctx).edit().putInt(KEY_COUNT, count.coerceIn(MIN_SLOTS, MAX_SLOTS)).apply()
    }

    fun getSlots(ctx: Context): List<Slot> =
        (0 until slotCount(ctx)).map { getSlot(ctx, it) }

    fun getSlot(ctx: Context, index: Int): Slot {
        val raw = prefs(ctx).getString(KEY_SLOT + index, null) ?: return Slot()
        return try {
            val o = JSONObject(raw)
            Slot(
                pkg = o.optString("pkg"),
                cls = o.optString("cls"),
                label = o.optString("label"),
                icon = o.optString("icon", IconCatalog.DEFAULT_NAME)
            )
        } catch (e: Exception) {
            Slot()
        }
    }

    fun setSlot(ctx: Context, index: Int, slot: Slot) {
        val o = JSONObject()
            .put("pkg", slot.pkg)
            .put("cls", slot.cls)
            .put("label", slot.label)
            .put("icon", slot.icon)
        prefs(ctx).edit().putString(KEY_SLOT + index, o.toString()).apply()
    }

    fun clearSlot(ctx: Context, index: Int) {
        prefs(ctx).edit().remove(KEY_SLOT + index).apply()
    }

    /** Reordena: saca el slot en [from] y lo inserta en [to], desplazando el resto. */
    fun moveSlot(ctx: Context, from: Int, to: Int) {
        val count = slotCount(ctx)
        if (from == to || from !in 0 until count || to !in 0 until count) return
        val list = (0 until count).map { getSlot(ctx, it) }.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        list.forEachIndexed { i, s ->
            if (s.isEmpty) clearSlot(ctx, i) else setSlot(ctx, i, s)
        }
    }

    fun setIcon(ctx: Context, index: Int, iconName: String) {
        val current = getSlot(ctx, index)
        if (current.isEmpty) return
        setSlot(ctx, index, current.copy(icon = iconName))
    }

    // --- Personalizacion ---

    fun font(ctx: Context): String = prefs(ctx).getString(KEY_FONT, "thin") ?: "thin"
    fun setFont(ctx: Context, v: String) = prefs(ctx).edit().putString(KEY_FONT, v).apply()

    fun background(ctx: Context): String = prefs(ctx).getString(KEY_BG, "none") ?: "none"
    fun setBackground(ctx: Context, v: String) = prefs(ctx).edit().putString(KEY_BG, v).apply()

    fun clock24(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_CLOCK24, true)
    fun setClock24(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_CLOCK24, v).apply()

    private const val KEY_CLOCKSTYLE = "clock_style"
    fun clockStyle(ctx: Context): String = prefs(ctx).getString(KEY_CLOCKSTYLE, "minimal") ?: "minimal"
    fun setClockStyle(ctx: Context, v: String) = prefs(ctx).edit().putString(KEY_CLOCKSTYLE, v).apply()

    fun showDate(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_SHOWDATE, true)
    fun setShowDate(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_SHOWDATE, v).apply()

    fun clockScale(ctx: Context): Float = prefs(ctx).getFloat(KEY_CLOCKSCALE, 1.0f)
    fun setClockScale(ctx: Context, v: Float) = prefs(ctx).edit().putFloat(KEY_CLOCKSCALE, v).apply()

    /** "start" o "center". */
    fun align(ctx: Context): String = prefs(ctx).getString(KEY_ALIGN, "start") ?: "start"
    fun setAlign(ctx: Context, v: String) = prefs(ctx).edit().putString(KEY_ALIGN, v).apply()

    fun template(ctx: Context): String = prefs(ctx).getString(KEY_TEMPLATE, "") ?: ""
    fun setTemplate(ctx: Context, v: String) = prefs(ctx).edit().putString(KEY_TEMPLATE, v).apply()

    private const val KEY_LAYOUT = "layout_mode"
    /** "list" (lista vertical) o "grid" (cuadricula icono+nombre). */
    fun layoutMode(ctx: Context): String = prefs(ctx).getString(KEY_LAYOUT, "list") ?: "list"
    fun setLayoutMode(ctx: Context, v: String) = prefs(ctx).edit().putString(KEY_LAYOUT, v).apply()

    // --- Pantalla de bloqueo (fondo animado en vivo) ---

    /** Valor de KEY_BG_LOCK que significa "usar el mismo fondo que el inicio". */
    const val LOCK_BG_SAME = "same"

    const val MIN_FPS = 15
    const val MAX_FPS = 60
    const val DEFAULT_FPS = 30

    private const val KEY_BG_LOCK = "bg_lock"
    private const val KEY_WP_FPS = "wp_fps"
    private const val KEY_LOCK_CLOCK = "lock_clock"
    private const val KEY_LOCK_DATE = "lock_date"
    private const val KEY_LOCK_BATTERY = "lock_battery"

    /** Fondo elegido para el bloqueo; puede ser [LOCK_BG_SAME]. */
    fun lockBackground(ctx: Context): String =
        prefs(ctx).getString(KEY_BG_LOCK, LOCK_BG_SAME) ?: LOCK_BG_SAME

    fun setLockBackground(ctx: Context, v: String) =
        prefs(ctx).edit().putString(KEY_BG_LOCK, v).apply()

    /** Fondo real a dibujar en el bloqueo, con [LOCK_BG_SAME] ya resuelto. */
    fun lockBackgroundResolved(ctx: Context): String {
        val v = lockBackground(ctx)
        return if (v == LOCK_BG_SAME) background(ctx) else v
    }

    /** Techo de fps del live wallpaper. Menos fps = menos bateria. */
    fun wallpaperFps(ctx: Context): Int =
        prefs(ctx).getInt(KEY_WP_FPS, DEFAULT_FPS).coerceIn(MIN_FPS, MAX_FPS)

    fun setWallpaperFps(ctx: Context, v: Int) =
        prefs(ctx).edit().putInt(KEY_WP_FPS, v.coerceIn(MIN_FPS, MAX_FPS)).apply()

    fun lockClock(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_LOCK_CLOCK, false)
    fun setLockClock(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_LOCK_CLOCK, v).apply()

    fun lockDate(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_LOCK_DATE, false)
    fun setLockDate(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_LOCK_DATE, v).apply()

    fun lockBattery(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_LOCK_BATTERY, false)
    fun setLockBattery(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_LOCK_BATTERY, v).apply()

    // --- Gestos ---
    // key: "left","right","up","down". Valor = accion (ver Gestures).

    private const val KEY_GESTURE = "gesture_"

    private val gestureDefaults = mapOf(
        "left" to "drawer",    // deslizar de derecha a izquierda
        "right" to "camera",   // deslizar de izquierda a derecha
        "up" to "finance",     // deslizar de abajo hacia arriba -> ver saldo
        "down" to "none"
    )

    fun gesture(ctx: Context, key: String): String =
        prefs(ctx).getString(KEY_GESTURE + key, gestureDefaults[key] ?: "none")
            ?: (gestureDefaults[key] ?: "none")

    fun setGesture(ctx: Context, key: String, action: String) {
        prefs(ctx).edit().putString(KEY_GESTURE + key, action).apply()
    }

    /** Migracion unica: pasar el gesto ↑ a "finance" (antes navegador). */
    fun migrateUpToFinance(ctx: Context) {
        val p = prefs(ctx)
        if (p.getBoolean("mig_up_finance", false)) return
        if (gesture(ctx, "up") == "browser") setGesture(ctx, "up", "finance")
        p.edit().putBoolean("mig_up_finance", true).apply()
    }
}
