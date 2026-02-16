package com.odtheking.odinaddon.pvgui.utils

import com.odtheking.odinaddon.pvgui.utils.apiutils.HypixelData

/**
 * Extensions for HypixelData.ItemData
 */

// Get rarity color for item background
fun HypixelData.ItemData.getRarityColor(): Int {
    // Parse rarity from lore (last line usually has rarity)
    val rarityLine = lore.lastOrNull() ?: return 0xFF404040.toInt()

    return when {
        rarityLine.contains("COMMON", ignoreCase = true) -> 0xFF5A5A5A.toInt()
        rarityLine.contains("UNCOMMON", ignoreCase = true) -> 0xFF55FF55.toInt()
        rarityLine.contains("RARE", ignoreCase = true) -> 0xFF5555FF.toInt()
        rarityLine.contains("EPIC", ignoreCase = true) -> 0xFFAA00AA.toInt()
        rarityLine.contains("LEGENDARY", ignoreCase = true) -> 0xFFFFAA00.toInt()
        rarityLine.contains("MYTHIC", ignoreCase = true) -> 0xFFFF55FF.toInt()
        rarityLine.contains("DIVINE", ignoreCase = true) -> 0xFF55FFFF.toInt()
        rarityLine.contains("SPECIAL", ignoreCase = true) -> 0xFFFF5555.toInt()
        else -> 0xFF404040.toInt()
    }
}

fun HypixelData.ItemData.getMagicalPower(): Int {
    // Look for "Magical Power: +X" in lore
    lore.forEach { line ->
        // Strip color codes
        val stripped = line.replace(Regex("§[0-9a-fk-or]"), "")
        if (stripped.contains("Magical Power:", ignoreCase = true)) {
            val match = Regex("\\d+").find(stripped)
            return match?.value?.toIntOrNull() ?: 0
        }
    }
    return 0
}

// Constants
object ItemConstants {
    const val MAX_MAGICAL_POWER = 2000
    const val MAX_ROWS_PER_PAGE = 5
}