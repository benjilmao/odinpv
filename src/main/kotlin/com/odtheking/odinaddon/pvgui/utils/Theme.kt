package com.odtheking.odinaddon.pvgui.utils

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.ItemRarity
import com.odtheking.odin.utils.getSkyblockRarity
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule

object Theme {
    val bg         get() = ProfileViewerModule.bgColor
    val accent     get() = ProfileViewerModule.accentColor
    val btnNormal  get() = Color(255, 255, 255, 0.07f)
    val btnHover   get() = Color(255, 255, 255, 0.13f)
    val separator  get() = Color(255, 255, 255, 0.15f)
    val slotBg     get() = Color(0, 0, 0, 0.35f)
    val round      get() = ProfileViewerModule.roundness
    val rarityBg   get() = ProfileViewerModule.rarityBackgrounds

    fun rarityColor(tier: String, alpha: Float = 0.35f): Color = when (tier.uppercase()) {
        "MYTHIC"    -> Color(255, 85, 255, alpha)
        "LEGENDARY" -> Color(255, 170, 0, alpha)
        "EPIC"      -> Color(170, 0, 170, alpha)
        "RARE"      -> Color(85, 85, 255, alpha)
        "UNCOMMON"  -> Color(85, 255, 85, alpha)
        "DIVINE"    -> Color(85, 255, 255, alpha)
        "SPECIAL"   -> Color(255, 85, 85, alpha)
        else        -> Color(170, 170, 170, alpha)
    }

    fun rarityFromLore(lore: List<String>): Color =
        getSkyblockRarity(lore)?.let { rarityColor(it.loreName) } ?: slotBg

    fun rarityPrefix(tier: String): String =
        ItemRarity.entries.find { it.loreName == tier.uppercase() }?.colorCode ?: "§7"

    fun petTierColor(tier: String): String = rarityPrefix(tier)
}