package com.odtheking.odinaddon.pvgui.utils

import com.odtheking.odin.utils.toFixed
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import kotlin.math.floor

fun Double.colorCode(max: Double): String = when {
    this >= max -> "§b"
    this >= max * 0.90 -> "§c"
    this >= max * 0.75 -> "§d"
    this >= max * 0.65 -> "§6"
    this >= max * 0.50 -> "§5"
    this >= max * 0.25 -> "§9"
    this >= max * 0.10 -> "§a"
    else -> "§f"
}

fun Number.colorize(max: Number, decimals: Int = 2): String =
    toDouble().let { "${it.colorCode(max.toDouble())}${it.toFixed(decimals)}" }

fun Long.colorizeNumber(max: Long): String = toDouble().colorCode(max.toDouble())

val Double.truncate: String get() = when {
    this >= 1_000_000_000 -> "${(this / 1_000_000_000.0).toFixed(2)}B"
    this >= 1_000_000 -> "${(this / 1_000_000.0).toFixed(2)}M"
    this >= 1_000 -> "${(this / 1_000.0).toFixed(2)}K"
    else -> if (this == floor(this)) toLong().toString() else toFixed(2)
}
val Long.truncate: String get() = toDouble().truncate
val Number.commas: String get() = "%,d".format(toLong())

fun <T> List<T>.without(vararg items: T): List<T> = filter { it !in items }
fun <K, V> Map<K, V>.without(vararg keys: K): Map<K, V> = filter { it.key !in keys }

val String.colorClass: String get() = when (lowercase()) {
    "berserk" -> "§c$this"
    "archer" -> "§6$this"
    "mage" -> "§b$this"
    "tank" -> "§2$this"
    "healer" -> "§d$this"
    else -> "§7$this"
}

val HypixelData.PetsData.activeDisplay: String get() = activePet?.coloredName ?: "§7None"