package com.odtheking.odinaddon.pvgui.utils

import com.odtheking.odin.utils.toFixed
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData

object Utils {
    fun commas(number: Long): String = "%,d".format(number)
    fun commas(number: Int): String = "%,d".format(number)

    fun colorize(value: Double, threshold: Double): String = when {
        value >= threshold        -> "§b"
        value >= threshold * 0.9  -> "§c"
        value >= threshold * 0.75 -> "§d"
        value >= threshold * 0.65 -> "§6"
        value >= threshold * 0.50 -> "§5"
        value >= threshold * 0.25 -> "§9"
        value >= threshold * 0.10 -> "§a"
        else -> "§f"
    }

    fun colorizeNumber(value: Long, threshold: Long): String = colorize(value.toDouble(), threshold.toDouble())

    fun formatHeldItem(heldItem: String): String {
        val cleaned = heldItem
            .removePrefix("PET_ITEM_")
            .removePrefix("PET_")
            .removePrefix("ITEM_")
            .removePrefix("RARE_")
            .removePrefix("EPIC_")
            .removePrefix("LEGENDARY_")
            .removePrefix("MYTHIC_")
            .removePrefix("COMMON_")
            .removePrefix("UNCOMMON_")
        return cleaned.replace("_", " ")
            .lowercase()
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
    }

    fun truncate(number: Double): String = when {
        number >= 1_000_000_000 -> "${(number / 1_000_000_000.0).toFixed(2)}B"
        number >= 1_000_000 -> "${(number / 1_000_000.0).toFixed(2)}M"
        number >= 1_000 -> "${(number / 1_000.0).toFixed(2)}K"
        else -> if (number == kotlin.math.floor(number)) number.toLong().toString() else number.toFixed(2)
    }

    fun truncate(number: Long): String = truncate(number.toDouble())

    fun getActivePetDisplay(pets: HypixelData.PetsData): String {
        val active = pets.pets.find { it.active } ?: return "§7None"
        val tierColor = Theme.petTierColor(active.tier)
        val name = active.type.lowercase()
            .replaceFirstChar { it.uppercase() }
            .replace("_", " ")
        return "$tierColor$name"
    }

    fun <T> getSubset(list: List<T>, page: Int, pageSize: Int): List<T> {
        val start = page * pageSize
        return list.subList(start.coerceAtMost(list.size), (start + pageSize).coerceAtMost(list.size))
    }

    fun <T> List<T>.without(vararg items: T): List<T> = filter { it !in items }
    fun <K, V> Map<K, V>.without(vararg keys: K): Map<K, V> = filter { it.key !in keys }
}