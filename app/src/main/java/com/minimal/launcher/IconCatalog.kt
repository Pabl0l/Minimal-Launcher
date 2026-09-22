package com.minimal.launcher

import androidx.annotation.DrawableRes

/** Un icono del catalogo: nombre estable (guardado en prefs) + recurso drawable. */
data class CatalogIcon(val name: String, @DrawableRes val res: Int)

/**
 * Catalogo de iconos monocromo (glifos blancos). El usuario elige uno por slot.
 * El nombre se persiste; el res se resuelve aqui para evitar guardar ids inestables.
 */
object IconCatalog {

    val icons: List<CatalogIcon> = listOf(
        CatalogIcon("phone", R.drawable.ic_cat_phone),
        CatalogIcon("chat", R.drawable.ic_cat_chat),
        CatalogIcon("camera", R.drawable.ic_cat_camera),
        CatalogIcon("globe", R.drawable.ic_cat_globe),
        CatalogIcon("settings", R.drawable.ic_cat_settings),
        CatalogIcon("clock", R.drawable.ic_cat_clock),
        CatalogIcon("music", R.drawable.ic_cat_music),
        CatalogIcon("image", R.drawable.ic_cat_image),
        CatalogIcon("mail", R.drawable.ic_cat_mail),
        CatalogIcon("map", R.drawable.ic_cat_map),
        CatalogIcon("calc", R.drawable.ic_cat_calc),
        CatalogIcon("calendar", R.drawable.ic_cat_calendar),
        CatalogIcon("contacts", R.drawable.ic_cat_contacts),
        CatalogIcon("search", R.drawable.ic_cat_search),
        CatalogIcon("note", R.drawable.ic_cat_note),
        CatalogIcon("video", R.drawable.ic_cat_video),
        CatalogIcon("folder", R.drawable.ic_cat_folder),
        CatalogIcon("bag", R.drawable.ic_cat_bag),
        CatalogIcon("star", R.drawable.ic_cat_star),
        CatalogIcon("home", R.drawable.ic_cat_home),
        CatalogIcon("grid", R.drawable.ic_cat_grid),
        CatalogIcon("heart", R.drawable.ic_cat_heart),
        CatalogIcon("book", R.drawable.ic_cat_book),
        CatalogIcon("wallet", R.drawable.ic_cat_wallet),
        CatalogIcon("wifi", R.drawable.ic_cat_wifi),
        CatalogIcon("bluetooth", R.drawable.ic_cat_bluetooth),
        CatalogIcon("battery", R.drawable.ic_cat_battery),
        CatalogIcon("download", R.drawable.ic_cat_download),
        CatalogIcon("cloud", R.drawable.ic_cat_cloud),
        CatalogIcon("lock", R.drawable.ic_cat_lock),
        CatalogIcon("bell", R.drawable.ic_cat_bell),
        CatalogIcon("mic", R.drawable.ic_cat_mic),
        CatalogIcon("headphones", R.drawable.ic_cat_headphones),
        CatalogIcon("game", R.drawable.ic_cat_game),
        CatalogIcon("car", R.drawable.ic_cat_car),
        CatalogIcon("plane", R.drawable.ic_cat_plane),
        CatalogIcon("cart", R.drawable.ic_cat_cart),
        CatalogIcon("coffee", R.drawable.ic_cat_coffee),
        CatalogIcon("sun", R.drawable.ic_cat_sun),
        CatalogIcon("moon", R.drawable.ic_cat_moon),
        // Nuevos
        CatalogIcon("bookmark", R.drawable.ic_cat_bookmark),
        CatalogIcon("shield", R.drawable.ic_cat_shield),
        CatalogIcon("tag", R.drawable.ic_cat_tag),
        CatalogIcon("key", R.drawable.ic_cat_key),
        CatalogIcon("trash", R.drawable.ic_cat_trash),
        CatalogIcon("edit", R.drawable.ic_cat_edit),
        CatalogIcon("share", R.drawable.ic_cat_share),
        CatalogIcon("link", R.drawable.ic_cat_link),
        CatalogIcon("eye", R.drawable.ic_cat_eye),
        CatalogIcon("bulb", R.drawable.ic_cat_bulb),
        CatalogIcon("brush", R.drawable.ic_cat_brush),
        CatalogIcon("rocket", R.drawable.ic_cat_rocket),
        CatalogIcon("trophy", R.drawable.ic_cat_trophy),
        CatalogIcon("target", R.drawable.ic_cat_target),
        CatalogIcon("dollar", R.drawable.ic_cat_dollar),
        CatalogIcon("chart", R.drawable.ic_cat_chart),
        CatalogIcon("code", R.drawable.ic_cat_code),
        CatalogIcon("printer", R.drawable.ic_cat_printer),
        CatalogIcon("tv", R.drawable.ic_cat_tv),
        CatalogIcon("bus", R.drawable.ic_cat_bus),
        CatalogIcon("flag", R.drawable.ic_cat_flag),
        CatalogIcon("gift", R.drawable.ic_cat_gift),
        CatalogIcon("compass", R.drawable.ic_cat_compass),
        CatalogIcon("flame", R.drawable.ic_cat_flame),
    )

    private val byName = icons.associateBy { it.name }

    const val DEFAULT_NAME = "grid"

    /** Resuelve el drawable de un nombre; fallback a grid si no existe. */
    @DrawableRes
    fun resFor(name: String?): Int =
        byName[name]?.res ?: R.drawable.ic_cat_grid
}
