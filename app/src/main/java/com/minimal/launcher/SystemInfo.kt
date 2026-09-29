package com.minimal.launcher

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs

/**
 * Información del sistema: batería, WiFi, almacenamiento.
 * Todo local, sin permisos de red.
 */
object SystemInfo {

    data class BatteryInfo(val pct: Int, val isCharging: Boolean)
    data class StorageInfo(val usedMb: Long, val totalMb: Long, val pct: Int)
    data class WifiInfo(val ssid: String, val strength: Int) // strength: 0-4

    fun battery(context: Context): BatteryInfo {
        val status = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val plugged = status?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val isCharging = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
        return BatteryInfo(pct, isCharging)
    }

    fun storage(): StorageInfo {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.totalBytes
        val freeBytes = stat.availableBytes
        val usedBytes = totalBytes - freeBytes
        val totalMb = totalBytes / (1024 * 1024)
        val usedMb = usedBytes / (1024 * 1024)
        val pct = if (totalBytes > 0) (usedBytes * 100 / totalBytes).toInt() else 0
        return StorageInfo(usedMb, totalMb, pct)
    }

    @Suppress("DEPRECATION")
    fun wifi(context: Context): WifiInfo {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val info = wm?.connectionInfo
            val ssid = info?.ssid?.removeSurrounding("\"") ?: "—"
            val rssi = info?.rssi ?: 0
            val strength = when {
                rssi > -50 -> 4
                rssi > -65 -> 3
                rssi > -75 -> 2
                rssi > -85 -> 1
                else -> 0
            }
            WifiInfo(ssid, strength)
        } catch (_: SecurityException) {
            // Sin permiso ACCESS_WIFI_STATE: devuelve valor por defecto
            WifiInfo("—", 0)
        }
    }
}
