package com.odtheking.odinaddon.pvgui.utils

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.ItemRarity
import com.odtheking.odin.utils.getSkyblockRarity
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule

object Theme {
    val bg get() = ProfileViewerModule.bgColor.rgba
    val accent get() = ProfileViewerModule.accentColor.rgba
    val radius get() = ProfileViewerModule.roundness
    val btnNormal = Color(255, 255, 255, 0.07f).rgba
    val btnHover = Color(255, 255, 255, 0.13f).rgba
    val btnSelected get() = ProfileViewerModule.accentColor.rgba
    val separator = Color(255, 255, 255, 0.15f).rgba
    val border = Color(255, 255, 255, 0.12f).rgba
    val slotBg = Color(0, 0, 0, 0.35f).rgba
    val textPrimary = 0xFFFFFFFF.toInt()
    const val textSecondary = 0xFFAAAAAA.toInt()

    fun rarityColor(tier: String, alpha: Float = 0.25f): Int = when (tier.uppercase()) {
        "MYTHIC"       -> Color(255, 85, 255, alpha).rgba
        "LEGENDARY"    -> Color(255, 170,   0, alpha).rgba
        "EPIC"         -> Color(170,   0, 170, alpha).rgba
        "RARE"         -> Color( 85,  85, 255, alpha).rgba
        "UNCOMMON"     -> Color( 85, 255,  85, alpha).rgba
        "DIVINE"       -> Color( 85, 255, 255, alpha).rgba
        "SPECIAL"      -> Color(255,  85,  85, alpha).rgba
        "VERY SPECIAL" -> Color(170,   0,   0, alpha).rgba
        else           -> Color(170, 170, 170, alpha).rgba
    }


    fun rarityFromLore(lore: List<String>): Int =
        getSkyblockRarity(lore)?.let { rarityColor(it.loreName) } ?: slotBg

    fun rarityPrefix(tier: String): String =
        ItemRarity.entries.find { it.loreName == tier.uppercase() }?.colorCode ?: "§7"

    fun petTierColor(tier: String): String = rarityPrefix(tier)
}