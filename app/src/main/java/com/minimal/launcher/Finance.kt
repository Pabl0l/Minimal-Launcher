package com.minimal.launcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Una billetera: nombre + saldo en pesos (enteros). */
data class Wallet(val name: String, val amount: Long)

/** Un movimiento: tipo (in/out), monto, billetera y fecha (millis). */
data class Transaction(val type: String, val amount: Long, val wallet: String, val time: Long)

/**
 * Control de saldo simple. El monto actual = suma de todas las billeteras.
 * Persistido en SharedPreferences como JSON. Sin decimales (pesos COP).
 */
object Finance {

    private const val FILE = "minimal_launcher"
    private const val KEY = "wallets"
    private const val KEY_TX = "transactions"
    private const val TX_CAP = 300

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun wallets(ctx: Context): MutableList<Wallet> {
        val raw = prefs(ctx).getString(KEY, null)
            ?: return mutableListOf(Wallet("Efectivo", 0), Wallet("Nequi", 0))
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Wallet(o.optString("name"), o.optLong("amount"))
            }.ifEmpty { mutableListOf(Wallet("Efectivo", 0), Wallet("Nequi", 0)) }
        } catch (e: Exception) {
            mutableListOf(Wallet("Efectivo", 0), Wallet("Nequi", 0))
        }
    }

    fun save(ctx: Context, list: List<Wallet>) {
        val arr = JSONArray()
        for (w in list) {
            arr.put(JSONObject().put("name", w.name).put("amount", w.amount))
        }
        prefs(ctx).edit().putString(KEY, arr.toString()).apply()
    }

    fun total(ctx: Context): Long = wallets(ctx).sumOf { it.amount }

    fun setAmount(ctx: Context, index: Int, amount: Long) {
        val list = wallets(ctx)
        if (index in list.indices) {
            list[index] = list[index].copy(amount = amount)
            save(ctx, list)
        }
    }

    /** delta > 0 ingreso, < 0 egreso. */
    fun adjust(ctx: Context, index: Int, delta: Long) {
        val list = wallets(ctx)
        if (index in list.indices) {
            list[index] = list[index].copy(amount = list[index].amount + delta)
            save(ctx, list)
        }
    }

    fun rename(ctx: Context, index: Int, name: String) {
        val list = wallets(ctx)
        if (index in list.indices) {
            list[index] = list[index].copy(name = name)
            save(ctx, list)
        }
    }

    fun add(ctx: Context, name: String) {
        val list = wallets(ctx)
        list.add(Wallet(name, 0))
        save(ctx, list)
    }

    fun remove(ctx: Context, index: Int) {
        val list = wallets(ctx)
        if (index in list.indices) {
            list.removeAt(index)
            save(ctx, list)
        }
    }

    // --- Historial de movimientos ---

    /** Lista de movimientos, mas reciente primero. */
    fun transactions(ctx: Context): List<Transaction> {
        val raw = prefs(ctx).getString(KEY_TX, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = ArrayList<Transaction>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    Transaction(
                        o.optString("type", "in"),
                        o.optLong("amount"),
                        o.optString("wallet"),
                        o.optLong("time")
                    )
                )
            }
            list.asReversed()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addTx(ctx: Context, income: Boolean, amount: Long, wallet: String) {
        val raw = prefs(ctx).getString(KEY_TX, null)
        val arr = try {
            if (raw != null) JSONArray(raw) else JSONArray()
        } catch (e: Exception) {
            JSONArray()
        }
        arr.put(
            JSONObject()
                .put("type", if (income) "in" else "out")
                .put("amount", amount)
                .put("wallet", wallet)
                .put("time", System.currentTimeMillis())
        )
        // recorta a los ultimos TX_CAP
        val trimmed = if (arr.length() > TX_CAP) {
            val out = JSONArray()
            for (i in arr.length() - TX_CAP until arr.length()) out.put(arr.get(i))
            out
        } else arr
        prefs(ctx).edit().putString(KEY_TX, trimmed.toString()).apply()
    }

    /** Elimina un movimiento por posicion en la lista mostrada (reciente primero). */
    fun removeTx(ctx: Context, displayIndex: Int) {
        val raw = prefs(ctx).getString(KEY_TX, null) ?: return
        try {
            val arr = JSONArray(raw)
            val realIndex = arr.length() - 1 - displayIndex
            if (realIndex < 0 || realIndex >= arr.length()) return
            val out = JSONArray()
            for (i in 0 until arr.length()) if (i != realIndex) out.put(arr.get(i))
            prefs(ctx).edit().putString(KEY_TX, out.toString()).apply()
        } catch (e: Exception) {
        }
    }

    fun formatDate(time: Long): String {
        if (time <= 0) return ""
        return SimpleDateFormat("d MMM · HH:mm", Locale("es")).format(Date(time))
    }

    fun formatSigned(type: String, amount: Long): String =
        (if (type == "in") "+" else "−") + format(amount)

    /** "$1.234.567" o "-$1.234" (separador de miles con punto, estilo COP). */
    fun format(amount: Long): String {
        val sign = if (amount < 0) "-" else ""
        val digits = abs(amount).toString()
        val sb = StringBuilder()
        val n = digits.length
        for (i in 0 until n) {
            if (i > 0 && (n - i) % 3 == 0) sb.append('.')
            sb.append(digits[i])
        }
        return "$sign\$$sb"
    }

    /** Extrae un Long de un texto (solo digitos). */
    fun parse(text: String): Long =
        text.filter { it.isDigit() }.ifEmpty { "0" }.toLong()
}
