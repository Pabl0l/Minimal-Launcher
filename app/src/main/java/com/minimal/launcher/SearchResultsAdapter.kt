package com.minimal.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter unificado para resultados de búsqueda universal.
 * Muestra apps con icono del sistema, ajustes con iconos monocromo, web con icono de globe.
 */
class SearchResultsAdapter(
    private var items: List<UniversalSearch.Result> = emptyList(),
    private val onClick: (UniversalSearch.Result) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.result_icon)
        val label: TextView = view.findViewById(R.id.result_label)
        val subtitle: TextView = view.findViewById(R.id.result_subtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val result = items[position]
        holder.label.text = result.label
        holder.subtitle.text = result.subtitle
        holder.subtitle.visibility = if (result.subtitle.isNotEmpty()) View.VISIBLE else View.GONE

        // Icono según tipo
        when (result.kind) {
            UniversalSearch.Kind.APP -> {
                // Cargar icono real de la app
                try {
                    val ai = holder.itemView.context.packageManager
                        .getApplicationInfo(result.pkg, 0)
                    holder.icon.setImageDrawable(
                        holder.itemView.context.packageManager.getApplicationIcon(ai)
                    )
                } catch (_: Exception) {
                    holder.icon.setImageResource(R.drawable.ic_cat_apps)
                }
            }
            UniversalSearch.Kind.SETTING -> {
                if (result.icon != 0) holder.icon.setImageResource(result.icon)
                else holder.icon.setImageResource(R.drawable.ic_cat_settings)
            }
            UniversalSearch.Kind.WEB -> {
                holder.icon.setImageResource(R.drawable.ic_cat_globe)
            }
        }

        holder.itemView.setOnClickListener { onClick(result) }
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<UniversalSearch.Result>) {
        items = newItems
        notifyDataSetChanged()
    }
}
