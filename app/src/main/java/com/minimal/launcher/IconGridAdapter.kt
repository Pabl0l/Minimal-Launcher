package com.minimal.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

/** Grid del catalogo de iconos para el selector. */
class IconGridAdapter(
    private val onPick: (CatalogIcon) -> Unit
) : RecyclerView.Adapter<IconGridAdapter.VH>() {

    private val items = IconCatalog.icons

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_icon, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.res)
        holder.itemView.setOnClickListener { onPick(item) }
    }

    override fun getItemCount() = items.size
}
