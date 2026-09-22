package com.minimal.launcher

/** Info minima de una app lanzable. Sin icono para no consumir memoria en el cajon. */
data class AppInfo(
    val label: String,
    val pkg: String,
    val cls: String
)
