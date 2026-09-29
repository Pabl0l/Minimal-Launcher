package com.minimal.launcher

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/** Menu principal: reloj, fecha, accesos editables, tuerca, gestos. */
class HomeActivity : AppCompatActivity() {

    private lateinit var shortcutsContainer: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var clockTime: TextView
    private lateinit var clockAnalog: AnalogClockView
    private lateinit var clockDate: TextView
    private lateinit var gear: ImageView
    private lateinit var bgView: AnimatedBackgroundView

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() {
            updateClockContent()
            clockHandler.postDelayed(this, 1000L - System.currentTimeMillis() % 1000L)
        }
    }

    private var pendingSlotIndex = -1
    private var pendingGestureKey: String? = null

    private val slotViews = mutableListOf<View>()
    private var builtCount = -1
    private var builtMode = ""

    private val pickApp = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@registerForActivityResult
        val pkg = data.getStringExtra(AppDrawerActivity.EXTRA_PKG) ?: return@registerForActivityResult
        val cls = data.getStringExtra(AppDrawerActivity.EXTRA_CLS) ?: return@registerForActivityResult
        val label = data.getStringExtra(AppDrawerActivity.EXTRA_LABEL) ?: pkg

        val gk = pendingGestureKey
        if (gk != null) {
            Prefs.setGesture(this, gk, Gestures.appAction(pkg, cls, label))
            pendingGestureKey = null
            return@registerForActivityResult
        }
        val idx = pendingSlotIndex
        if (idx < 0) return@registerForActivityResult
        val existing = Prefs.getSlot(this, idx)
        val icon = if (existing.isEmpty) IconCatalog.DEFAULT_NAME else existing.icon
        Prefs.setSlot(this, idx, Slot(pkg, cls, label, icon))
        pendingSlotIndex = -1
        renderSlots()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        content = findViewById(R.id.home_content)
        shortcutsContainer = findViewById(R.id.shortcuts)
        clockTime = findViewById(R.id.clock_time)
        clockAnalog = findViewById(R.id.clock_analog)
        clockDate = findViewById(R.id.clock_date)
        gear = findViewById(R.id.gear)
        bgView = findViewById(R.id.bg_view)

        gear.setOnClickListener { openSettings() }
        Motion.press(gear)

        Prefs.migrateUpToFinance(this)
        setupGestures(findViewById(R.id.home_root))
    }

    override fun onResume() {
        super.onResume()
        applyAppearance()
        cleanOrphanSlots()
        renderSlots()
        bgView.start()
        clockHandler.post(clockTick)
        animateEntrance()
    }

    override fun onPause() {
        bgView.stop()
        clockHandler.removeCallbacks(clockTick)
        super.onPause()
    }

    @Deprecated("Back en launcher no hace nada")
    override fun onBackPressed() {
    }

    // --- Apariencia ---

    private fun applyAppearance() {
        val tf = Fonts.typeface(Prefs.font(this))
        clockTime.typeface = tf
        clockDate.typeface = tf
        clockDate.visibility = if (Prefs.showDate(this)) View.VISIBLE else View.GONE
        applyClockFace()
        bgView.mode = Prefs.background(this)

        val center = Prefs.align(this) == "center"
        content.gravity = if (center) Gravity.CENTER_HORIZONTAL else Gravity.START

        if (builtCount != Prefs.slotCount(this) || builtMode != Prefs.layoutMode(this)) {
            buildSlotViews()
        }
    }

    /** Configura la cara elegida (analogico vs texto) y su tamano. */
    private fun applyClockFace() {
        val face = ClockFaces.byKeyOrDefault(Prefs.clockStyle(this))
        val scale = Prefs.clockScale(this)
        if (face.kind == ClockKind.ANALOG) {
            clockTime.visibility = View.GONE
            clockAnalog.visibility = View.VISIBLE
            clockAnalog.scaleX = scale
            clockAnalog.scaleY = scale
            clockAnalog.pivotX = 0f
            clockAnalog.faceColor = 0xFFFFFFFF.toInt()
            clockAnalog.invalidate()
        } else {
            clockAnalog.visibility = View.GONE
            clockTime.visibility = View.VISIBLE
            clockTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, face.baseSp * scale)
            clockTime.gravity = if (face.twoLine) Gravity.CENTER_HORIZONTAL else Gravity.NO_GRAVITY
            clockTime.setLineSpacing(0f, if (face.twoLine) 0.9f else 1.0f)
        }
        updateClockContent()
    }

    /** Actualiza el contenido cada segundo (texto o refresco del analogico). */
    private fun updateClockContent() {
        val face = ClockFaces.byKeyOrDefault(Prefs.clockStyle(this))
        if (face.kind == ClockKind.ANALOG) {
            clockAnalog.invalidate()
        } else {
            clockTime.text = face.render(Calendar.getInstance(), Prefs.clock24(this))
        }
    }

    private fun animateEntrance() {
        Motion.enter(clockTime, 0)
        Motion.enter(clockAnalog, 0)
        Motion.enter(clockDate, 40)
        slotViews.forEachIndexed { i, v -> Motion.enter(v, 90 + i * 40L) }
        Motion.enter(gear, 60, 8f)
    }

    // --- Slots ---

    private fun buildSlotViews() {
        shortcutsContainer.removeAllViews()
        slotViews.clear()
        val count = Prefs.slotCount(this)
        val mode = Prefs.layoutMode(this)
        val inflater = LayoutInflater.from(this)

        if (mode == "grid") {
            val cols = if (count <= 3) count.coerceAtLeast(1) else 4
            val grid = GridLayout(this).apply { columnCount = cols }
            grid.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            for (i in 0 until count) {
                val cell = inflater.inflate(R.layout.home_shortcut_grid, grid, false)
                val lp = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(i % cols, 1f)
                    rowSpec = GridLayout.spec(i / cols)
                }
                cell.layoutParams = lp
                wireSlot(cell, i)
                grid.addView(cell)
                slotViews.add(cell)
            }
            shortcutsContainer.addView(grid)
        } else {
            for (i in 0 until count) {
                val row = inflater.inflate(R.layout.home_shortcut, shortcutsContainer, false)
                wireSlot(row, i)
                shortcutsContainer.addView(row)
                slotViews.add(row)
            }
        }
        builtCount = count
        builtMode = mode
    }

    private var dragFrom = -1
    private var dropHandled = false

    private fun wireSlot(v: View, index: Int) {
        v.setOnClickListener { onSlotClick(index) }
        v.setOnLongClickListener { startSlotDrag(v); true }
        v.setOnDragListener { target, event -> onSlotDrag(target, event) }
        Motion.press(v)
    }

    /** Long-press: levanta el acceso para arrastrarlo. */
    private fun startSlotDrag(v: View) {
        dragFrom = slotViews.indexOf(v)
        if (dragFrom < 0) return
        dropHandled = false
        v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        val shadow = View.DragShadowBuilder(v)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            v.startDragAndDrop(null, shadow, null, 0)
        } else {
            @Suppress("DEPRECATION")
            v.startDrag(null, shadow, null, 0)
        }
        v.alpha = 0.3f
    }

    private fun onSlotDrag(target: View, event: android.view.DragEvent): Boolean {
        when (event.action) {
            android.view.DragEvent.ACTION_DRAG_STARTED -> return true
            android.view.DragEvent.ACTION_DRAG_ENTERED -> {
                target.animate().scaleX(1.08f).scaleY(1.08f).setDuration(120).start()
                return true
            }
            android.view.DragEvent.ACTION_DRAG_EXITED -> {
                target.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                return true
            }
            android.view.DragEvent.ACTION_DROP -> {
                target.scaleX = 1f; target.scaleY = 1f
                val to = slotViews.indexOf(target)
                if (dragFrom >= 0 && to >= 0 && dragFrom != to) {
                    Prefs.moveSlot(this, dragFrom, to)
                    dropHandled = true
                    renderSlots()
                }
                return true
            }
            android.view.DragEvent.ACTION_DRAG_ENDED -> {
                slotViews.forEach { it.alpha = 1f; it.scaleX = 1f; it.scaleY = 1f }
                // soltado en el mismo sitio (sin mover) -> abrir configuracion
                if (!dropHandled && dragFrom >= 0 && slotViews.indexOf(target) == dragFrom) {
                    showSlotConfig(dragFrom)
                }
                dragFrom = -1
                return true
            }
        }
        return false
    }

    private fun renderSlots() {
        if (builtCount != Prefs.slotCount(this) || builtMode != Prefs.layoutMode(this)) {
            buildSlotViews()
        }
        val slots = Prefs.getSlots(this)
        val tf = Fonts.typeface(Prefs.font(this))
        val center = Prefs.align(this) == "center"
        val isList = builtMode != "grid"
        for (i in slots.indices) {
            val v = slotViews.getOrNull(i) ?: continue
            if (isList) {
                (v as? LinearLayout)?.gravity =
                    if (center) Gravity.CENTER else (Gravity.CENTER_VERTICAL or Gravity.START)
            }
            val icon = v.findViewById<ImageView>(R.id.slot_icon)
            val label = v.findViewById<TextView>(R.id.slot_label)
            label.typeface = tf
            val slot = slots[i]
            if (slot.isEmpty) {
                icon.setImageResource(R.drawable.ic_cat_grid)
                icon.alpha = 0.3f
                label.text = if (isList) getString(R.string.empty_slot) + "  añadir"
                             else getString(R.string.empty_slot)
                label.alpha = 0.3f
            } else {
                icon.setImageResource(IconCatalog.resFor(slot.icon))
                icon.alpha = 1f
                label.text = slot.label
                label.alpha = 1f
            }
        }
    }

    private fun onSlotClick(index: Int) {
        val slot = Prefs.getSlot(this, index)
        if (slot.isEmpty) pickAppForSlot(index) else launchApp(slot.pkg, slot.cls)
    }

    // --- Config de slot ---

    private fun showSlotConfig(index: Int) {
        val slot = Prefs.getSlot(this, index)
        val options = if (slot.isEmpty) {
            arrayOf(getString(R.string.cfg_choose_app))
        } else {
            arrayOf(
                getString(R.string.cfg_choose_app),
                getString(R.string.cfg_choose_icon),
                getString(R.string.cfg_clear)
            )
        }
        AlertDialog.Builder(this, R.style.Theme_MinimalDialog)
            .setTitle(R.string.cfg_title)
            .setItems(options) { _, which ->
                when (options[which]) {
                    getString(R.string.cfg_choose_app) -> pickAppForSlot(index)
                    getString(R.string.cfg_choose_icon) -> showIconPicker(index)
                    getString(R.string.cfg_clear) -> {
                        Prefs.clearSlot(this, index)
                        renderSlots()
                    }
                }
            }
            .show()
    }

    private fun pickAppForSlot(index: Int) {
        pendingGestureKey = null
        pendingSlotIndex = index
        launchPicker()
    }

    private fun launchPicker() {
        val intent = Intent(this, AppDrawerActivity::class.java)
            .putExtra(AppDrawerActivity.EXTRA_PICK, true)
        pickApp.launch(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun showIconPicker(index: Int) {
        val recycler = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 5)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val dialog = AlertDialog.Builder(this, R.style.Theme_MinimalDialog)
            .setTitle(R.string.pick_icon_title)
            .setView(recycler)
            .setNegativeButton(R.string.cancel, null)
            .create()
        recycler.adapter = IconGridAdapter { chosen ->
            Prefs.setIcon(this, index, chosen.name)
            renderSlots()
            dialog.dismiss()
        }
        dialog.show()
    }

    // --- Ajustes / finanzas ---

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
        overridePendingTransition(R.anim.modal_in, R.anim.hold)
    }

    /** Abre finanzas guardando la direccion de entrada (para salir con la contraria). */
    private fun openFinance(entryDir: String) {
        startActivity(
            Intent(this, FinanceActivity::class.java)
                .putExtra(FinanceActivity.EXTRA_ENTRY_DIR, entryDir)
        )
        overridePendingTransition(R.anim.modal_in, R.anim.hold)
    }

    // --- Gestos ---

    private fun setupGestures(root: GestureFrameLayout) {
        root.onSwipe = { direction ->
            val action = Prefs.gesture(this, direction)
            Gestures.run(this, action, { openDrawer() }, { openFinance(direction) })
        }
    }

    // --- Utilidades ---

    private fun openDrawer() {
        startActivity(Intent(this, AppDrawerActivity::class.java))
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun launchApp(pkg: String, cls: String) {
        try {
            startActivity(AppsRepository.launchIntent(pkg, cls))
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir la app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cleanOrphanSlots() {
        val count = Prefs.slotCount(this)
        for (i in 0 until count) {
            val slot = Prefs.getSlot(this, i)
            if (!slot.isEmpty && !AppsRepository.isInstalled(this, slot.pkg)) {
                Prefs.clearSlot(this, i)
            }
        }
    }

    private fun dp(v: Int) = Dimens.dp(this, v.toFloat())
}
