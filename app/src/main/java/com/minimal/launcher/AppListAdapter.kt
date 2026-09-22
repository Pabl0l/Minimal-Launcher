package com.minimal.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** Lista de texto A-Z para el cajon de apps. Solo etiqueta, sin iconos. */
class AppListAdapter(
    private var items: List<AppInfo>,
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: ((AppInfo) -> Unit)? = null
) : RecyclerView.Adapter<AppListAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val label: TextView = view.findViewById(R.id.app_label)
    }

    private var lastAnimated = -1

    fun submit(newItems: List<AppInfo>) {
        items = newItems
        lastAnimated = -1
        notifyDataSetChanged()
    }

    /** Primer indice cuya etiqueta empieza por [letter] (o '#' = no alfabetico). */
    fun indexForLetter(letter: Char): Int {
        return items.indexOfFirst { info ->
            val c = info.label.firstOrNull()?.uppercaseChar() ?: '#'
            if (letter == '#') !c.isLetter() else c == letter
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.label.text = item.label
        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener {
            onLongClick?.invoke(item)
            onLongClick != null
        }
        // Entrada suave escalonada la primera vez que aparece cada fila
        if (position > lastAnimated) {
            lastAnimated = position
            val v = holder.itemView
            v.alpha = 0f
            v.translationY = 20f * v.resources.displayMetrics.density
            v.animate().alpha(1f).translationY(0f)
                .setStartDelay((position % 12) * 22L)
                .setDuration(300)
                .setInterpolator(Motion.EASE)
                .start()
        }
    }

    override fun getItemCount() = items.size
}
