package com.odtheking.odinaddon.pvgui.utils

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Color.Companion.brighter
import com.odtheking.odin.utils.Color.Companion.withAlpha
import com.odtheking.odin.utils.getSkyblockRarity
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule

object Theme {
    private val ct get() = ProfileViewerModule.currentTheme

    val bg get() = ct.main.rgba
    val slotBg get() = ct.items.rgba
    val separator get() = ct.line.rgba
    val btnNormal get() = ct.button.rgba
    val btnHover get() = ct.button.brighter(1.2f).rgba
    val btnSelected get() = ct.selected.rgba
    val textPrimary get() = ct.font.rgba
    val textSecondary get() = ct.font.withAlpha(0.6f).rgba
    val radius get() = ct.roundness
    val slotRadius get() = ct.inventoryRound

    val fontCode get() = ct.name.let {
        when (it) {
            "Odin" -> "f"
            "Midnight" -> "f"
            "Light" -> "0"
            "Sunrise" -> "b"
            else -> "f"
        }
    }

    fun rarityColor(tier: String, alpha: Float = 0.25f): Int = when (tier.uppercase()) {
        "MYTHIC" -> Color(255, 85, 255, alpha).rgba
        "LEGENDARY" -> Color(255, 170, 0, alpha).rgba
        "EPIC" -> Color(170, 0, 170, alpha).rgba
        "RARE" -> Color( 85, 85, 255, alpha).rgba
        "UNCOMMON" -> Color( 85, 255, 85, alpha).rgba
        "DIVINE" -> Color( 85, 255, 255, alpha).rgba
        "SPECIAL" -> Color(255, 85, 85, alpha).rgba
        "VERY SPECIAL" -> Color(170, 0, 0, alpha).rgba
        else -> Color(170, 170, 170, alpha).rgba
    }

    fun rarityFromLore(lore: List<String>): Int =
        getSkyblockRarity(lore)?.let { rarityColor(it.loreName) } ?: slotBg

    fun rarityPrefix(tier: String): String = when (tier.uppercase()) {
        "COMMON" -> "§f"
        "UNCOMMON" -> "§a"
        "RARE" -> "§9"
        "EPIC" -> "§5"
        "LEGENDARY" -> "§6"
        "MYTHIC" -> "§d"
        "DIVINE" -> "§b"
        "SPECIAL", "VERY SPECIAL" -> "§c"
        else -> "§7"
    }

    fun petTierColor(tier: String): String = rarityPrefix(tier)
}