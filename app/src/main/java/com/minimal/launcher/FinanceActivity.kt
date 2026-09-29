package com.minimal.launcher

import android.os.Bundle
import android.text.InputType
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.math.abs

/** Panel de saldo: total (suma de billeteras) + ingresos/egresos + edicion de billeteras. */
class FinanceActivity : AppCompatActivity() {

    private lateinit var totalView: TextView
    private lateinit var container: LinearLayout
    private lateinit var txContainer: LinearLayout
    private lateinit var txHeader: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finance)
        totalView = findViewById(R.id.finance_total)
        container = findViewById(R.id.wallets_container)
        txContainer = findViewById(R.id.tx_container)
        txHeader = findViewById(R.id.tx_header)

        findViewById<TextView>(R.id.btn_income).setOnClickListener { flow(income = true) }
        findViewById<TextView>(R.id.btn_expense).setOnClickListener { flow(income = false) }
        findViewById<TextView>(R.id.add_wallet).setOnClickListener { addWallet() }

        Motion.press(findViewById(R.id.btn_income))
        Motion.press(findViewById(R.id.btn_expense))

        setupParallax()
        setupExitGesture()
        render()
    }

    /** Parallax sutil: el total se desplaza al 30% del scroll. */
    private fun setupParallax() {
        val scroll = findViewById<ScrollView>(R.id.finance_scroll)
        scroll.setOnScrollChangeListener { _, _, sy, _, _ ->
            totalView.translationY = sy * 0.3f
            totalView.alpha = (1f - sy / 600f).coerceIn(0.4f, 1f)
        }
    }

    /**
     * Sale con el gesto CONTRARIO al de entrada: si entró deslizando ↑, sale con ↓;
     * si entró con →, sale con ←; etc. La dirección de entrada llega por intent.
     * En salidas verticales respeta el scroll (solo cierra en el borde correspondiente).
     */
    private fun setupExitGesture() {
        val scroll = findViewById<ScrollView>(R.id.finance_scroll)
        val entry = intent.getStringExtra(EXTRA_ENTRY_DIR) ?: "up"
        val exitDir = opposite(entry)
        val trigger = Dimens.dpF(this, 110f)

        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x
                val dy = e2.y - e1.y
                val horizontal = abs(dx) > abs(dy)
                val dir = if (horizontal) {
                    if (dx < 0) "left" else "right"
                } else {
                    if (dy < 0) "up" else "down"
                }
                if (dir != exitDir) return false

                // Guardas para no chocar con el scroll vertical de la lista.
                if (dir == "down" && scroll.scrollY != 0) return false
                if (dir == "up") {
                    val child = scroll.getChildAt(0)
                    if (child != null && scroll.scrollY + scroll.height < child.height) return false
                }

                val dist = if (horizontal) abs(dx) else abs(dy)
                if (dist > trigger) { finish(); return true }
                return false
            }
        })
        scroll.setOnTouchListener { _, ev ->
            detector.onTouchEvent(ev)
            false // no consumir: scroll y taps siguen funcionando
        }
    }

    private fun opposite(dir: String): String = when (dir) {
        "up" -> "down"
        "down" -> "up"
        "left" -> "right"
        "right" -> "left"
        else -> "down"
    }

    companion object {
        const val EXTRA_ENTRY_DIR = "entry_dir"
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.modal_out)
    }

    private fun render() {
        totalView.text = Finance.format(Finance.total(this))
        container.removeAllViews()
        val wallets = Finance.wallets(this)
        val inflater = LayoutInflater.from(this)
        wallets.forEachIndexed { i, w ->
            val row = inflater.inflate(R.layout.finance_wallet, container, false)
            row.findViewById<TextView>(R.id.wallet_name).text = w.name
            row.findViewById<TextView>(R.id.wallet_amount).text = Finance.format(w.amount)
            row.setOnClickListener { editAmount(i, w) }
            row.setOnLongClickListener { walletOptions(i, w); true }
            Motion.press(row)
            container.addView(row)
            container.addView(divider())
        }
        renderTransactions()
    }

    private fun renderTransactions() {
        val txs = Finance.transactions(this)
        txHeader.visibility = if (txs.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        txContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        txs.forEachIndexed { i, t ->
            val row = inflater.inflate(R.layout.finance_tx, txContainer, false)
            row.findViewById<TextView>(R.id.tx_wallet).text =
                (if (t.type == "in") "Ingreso · " else "Egreso · ") + t.wallet
            row.findViewById<TextView>(R.id.tx_date).text = Finance.formatDate(t.time)
            row.findViewById<TextView>(R.id.tx_amount).text = Finance.formatSigned(t.type, t.amount)
            row.setOnLongClickListener { confirmDeleteTx(i); true }
            Motion.press(row)
            txContainer.addView(row)
            txContainer.addView(divider())
        }
    }

    private fun confirmDeleteTx(displayIndex: Int) {
        AlertDialog.Builder(this, R.style.Theme_MinimalDialog)
            .setTitle("Movimiento")
            .setItems(arrayOf("Eliminar del historial")) { _, _ ->
                Finance.removeTx(this, displayIndex)
                renderTransactions()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // --- Ingreso / Egreso ---

    private fun flow(income: Boolean) {
        pickWallet { index ->
            val name = Finance.wallets(this).getOrNull(index)?.name ?: ""
            amountDialog(if (income) "Ingreso" else "Egreso", "") { value ->
                if (value > 0) {
                    Finance.adjust(this, index, if (income) value else -value)
                    Finance.addTx(this, income, value, name)
                }
                render()
            }
        }
    }

    private fun pickWallet(onPicked: (Int) -> Unit) {
        val wallets = Finance.wallets(this)
        if (wallets.size == 1) {
            onPicked(0); return
        }
        val names = wallets.map { it.name }.toTypedArray()
        AlertDialog.Builder(this, R.style.Theme_MinimalDialog)
            .setTitle("¿A qué billetera?")
            .setItems(names) { _, which -> onPicked(which) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // --- Editar billetera ---

    private fun editAmount(index: Int, w: Wallet) {
        amountDialog("Saldo de ${w.name}", w.amount.toString()) { value ->
            Finance.setAmount(this, index, value)
            render()
        }
    }

    private fun walletOptions(index: Int, w: Wallet) {
        AlertDialog.Builder(this, R.style.Theme_MinimalDialog)
            .setTitle(w.name)
            .setItems(arrayOf("Renombrar", "Eliminar")) { _, which ->
                when (which) {
                    0 -> textDialog("Renombrar billetera", w.name) { name ->
                        if (name.isNotBlank()) { Finance.rename(this, index, name.trim()); render() }
                    }
                    1 -> {
                        Finance.remove(this, index); render()
                    }
                }
            }
            .show()
    }

    private fun addWallet() {
        textDialog("Nueva billetera", "") { name ->
            if (name.isNotBlank()) { Finance.add(this, name.trim()); render() }
        }
    }

    // --- Dialogos de entrada ---

    private fun amountDialog(title: String, prefillDigits: String, onValue: (Long) -> Unit) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "0"
            setText(if (prefillDigits == "0") "" else prefillDigits)
            setTextColor(ContextCompat.getColor(context, R.color.offwhite))
            setHintTextColor(ContextCompat.getColor(context, R.color.dim))
            textSize = 22f
            setSelection(text.length)
        }
        AlertDialog.Builder(this, R.style.Theme_MinimalDialog)
            .setTitle(title)
            .setView(wrap(input))
            .setPositiveButton("Aceptar") { _, _ -> onValue(Finance.parse(input.text.toString())) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun textDialog(title: String, prefill: String, onText: (String) -> Unit) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setText(prefill)
            setTextColor(ContextCompat.getColor(context, R.color.offwhite))
            setHintTextColor(ContextCompat.getColor(context, R.color.dim))
            textSize = 18f
            setSelection(text.length)
        }
        AlertDialog.Builder(this, R.style.Theme_MinimalDialog)
            .setTitle(title)
            .setView(wrap(input))
            .setPositiveButton("Aceptar") { _, _ -> onText(input.text.toString()) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun wrap(view: EditText): FrameLayout {
        val pad = Dimens.dp(this, 22f)
        return FrameLayout(this).apply {
            setPadding(pad, pad / 2, pad, 0)
            addView(view)
        }
    }

    private fun divider(): android.view.View {
        val v = android.view.View(this)
        val h = Dimens.dp(this, 0.5f).coerceAtLeast(1)
        v.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h)
        v.setBackgroundColor(ContextCompat.getColor(this, R.color.separator))
        return v
    }
}
