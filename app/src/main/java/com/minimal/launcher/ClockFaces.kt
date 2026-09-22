package com.minimal.launcher

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class ClockKind { TEXT, ANALOG }

/**
 * Un "tipo de reloj". La mayoria son TEXT (render devuelve una cadena);
 * ANALOG se dibuja con AnalogClockView.
 *
 * baseSp = tamano base en sp (se multiplica por la escala del usuario).
 */
data class ClockFace(
    val key: String,
    val label: String,
    val kind: ClockKind = ClockKind.TEXT,
    val twoLine: Boolean = false,
    val baseSp: Float = 68f,
    val render: (Calendar, Boolean) -> String = { _, _ -> "" }
)

object ClockFaces {

    // --- helpers de texto en espanol ---

    private val hourWords = arrayOf(
        "doce", "una", "dos", "tres", "cuatro", "cinco", "seis",
        "siete", "ocho", "nueve", "diez", "once", "doce"
    )
    private val units = arrayOf(
        "cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
        "diez", "once", "doce", "trece", "catorce", "quince", "dieciséis", "diecisiete",
        "dieciocho", "diecinueve", "veinte", "veintiuno", "veintidós", "veintitrés",
        "veinticuatro", "veinticinco", "veintiséis", "veintisiete", "veintiocho", "veintinueve"
    )
    private val tens = arrayOf("", "", "", "treinta", "cuarenta", "cincuenta")

    private fun spell(n: Int): String = when {
        n < 30 -> units[n]
        else -> {
            val t = tens[n / 10]
            if (n % 10 == 0) t else "$t y ${units[n % 10]}"
        }
    }

    private fun art(h12: Int) = if (h12 == 1) "la" else "las"

    /** "las diez y veintisiete" / "las diez en punto". */
    private fun wordFull(c: Calendar): String {
        val h = c.get(Calendar.HOUR); val h12 = if (h == 0) 12 else h
        val m = c.get(Calendar.MINUTE)
        val mw = if (m == 0) "en punto" else "y ${spell(m)}"
        return "${art(h12)} ${hourWords[h12]} $mw"
    }

    /** Coloquial redondeado a 5 min: "las diez y cuarto", "las once menos veinte". */
    private fun wordFuzzy(c: Calendar): String {
        val h = c.get(Calendar.HOUR)
        var h12 = if (h == 0) 12 else h
        var m5 = Math.round(c.get(Calendar.MINUTE) / 5f) * 5
        if (m5 == 60) { m5 = 0; h12 = if (h12 == 12) 1 else h12 + 1 }
        // "menos" usa la hora siguiente
        val nextH = if (h12 == 12) 1 else h12 + 1
        return when (m5) {
            0 -> "${art(h12)} ${hourWords[h12]} en punto"
            5 -> "${art(h12)} ${hourWords[h12]} y cinco"
            10 -> "${art(h12)} ${hourWords[h12]} y diez"
            15 -> "${art(h12)} ${hourWords[h12]} y cuarto"
            20 -> "${art(h12)} ${hourWords[h12]} y veinte"
            25 -> "${art(h12)} ${hourWords[h12]} y veinticinco"
            30 -> "${art(h12)} ${hourWords[h12]} y media"
            35 -> "${art(nextH)} ${hourWords[nextH]} menos veinticinco"
            40 -> "${art(nextH)} ${hourWords[nextH]} menos veinte"
            45 -> "${art(nextH)} ${hourWords[nextH]} menos cuarto"
            50 -> "${art(nextH)} ${hourWords[nextH]} menos diez"
            else -> "${art(nextH)} ${hourWords[nextH]} menos cinco"
        }
    }

    private val romanUnits = arrayOf("", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX")
    private val romanTens = arrayOf("", "X", "XX", "XXX", "XL", "L")

    private fun roman(n: Int): String =
        if (n == 0) "·" else romanTens[n / 10] + romanUnits[n % 10]

    private fun romanClock(c: Calendar, is24: Boolean): String {
        val h = if (is24) c.get(Calendar.HOUR_OF_DAY) else (if (c.get(Calendar.HOUR) == 0) 12 else c.get(Calendar.HOUR))
        return "${roman(h)} : ${roman(c.get(Calendar.MINUTE))}"
    }

    private fun bits(value: Int, count: Int): String {
        val sb = StringBuilder()
        for (i in count - 1 downTo 0) {
            sb.append(if ((value shr i) and 1 == 1) "●" else "○")
            if (i > 0) sb.append(' ')
        }
        return sb.toString()
    }

    /** Reloj binario: fila de horas y fila de minutos con puntos. */
    private fun binaryClock(c: Calendar, is24: Boolean): String {
        val h = if (is24) c.get(Calendar.HOUR_OF_DAY) else (if (c.get(Calendar.HOUR) == 0) 12 else c.get(Calendar.HOUR))
        return "${bits(h, 5)}\n${bits(c.get(Calendar.MINUTE), 6)}"
    }

    private fun fmt(c: Calendar, pattern: String): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(c.time)

    private fun digital(key: String, label: String, p24: String, p12: String, twoLine: Boolean = false, sp: Float = 68f) =
        ClockFace(key, label, ClockKind.TEXT, twoLine, sp) { c, is24 -> fmt(c, if (is24) p24 else p12) }

    // --- catalogo ---

    val options: List<ClockFace> = listOf(
        // Numericos
        digital("minimal", "Minimalista", "HH:mm", "h:mm"),
        digital("seconds", "Con segundos", "HH:mm:ss", "h:mm:ss", sp = 58f),
        digital("ampm", "Con a.m./p.m.", "HH:mm", "h:mm a", sp = 54f),
        digital("stacked", "Apilado", "HH\nmm", "h\nmm", twoLine = true),
        digital("dotless", "Sin dos puntos", "HH mm", "h mm"),
        digital("dotted", "Con punto", "HH·mm", "h·mm"),
        digital("compact", "Compacto", "H:mm", "h:mm"),
        digital("wide", "Espaciado", "HH : mm", "h : mm", sp = 58f),
        digital("full", "Día y hora", "EEE HH:mm", "EEE h:mm", sp = 46f),
        digital("hour", "Solo hora", "HH", "h a", sp = 92f),
        digital("stackedbig", "Apilado con seg.", "HH\nmm ss", "h\nmm ss", twoLine = true, sp = 60f),
        // Creativos no numericos
        ClockFace("word", "En palabras", ClockKind.TEXT, false, 34f) { c, _ -> wordFull(c) },
        ClockFace("fuzzy", "Coloquial", ClockKind.TEXT, false, 34f) { c, _ -> wordFuzzy(c) },
        ClockFace("roman", "Números romanos", ClockKind.TEXT, false, 56f) { c, is24 -> romanClock(c, is24) },
        ClockFace("binary", "Binario (puntos)", ClockKind.TEXT, true, 30f) { c, is24 -> binaryClock(c, is24) },
        ClockFace("terminal", "Terminal", ClockKind.TEXT, false, 40f) { c, _ -> "> ${fmt(c, "HH:mm:ss")}▮" },
        ClockFace("analog", "Analógico", ClockKind.ANALOG),
    )

    private val byKey = options.associateBy { it.key }
    val DEFAULT = options.first()

    fun byKeyOrDefault(key: String): ClockFace = byKey[key] ?: DEFAULT
    fun labelFor(key: String): String = byKey[key]?.label ?: DEFAULT.label

    fun now(): Calendar = Calendar.getInstance()
}
