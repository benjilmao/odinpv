package com.odtheking.odinaddon.pvgui2

import com.odtheking.odin.utils.Color

object Colors {
    val guiBg        = Color(26, 26, 46)
    val slotBg       = Color(0, 0, 0, 0.53f)
    val hotbarBg     = Color(34, 85, 34, 0.53f)
    val separator    = Color(255, 255, 255, 0.2f)
    val buttonBg     = Color(255, 255, 255, 0.13f)
    val buttonSelect = Color(26, 74, 138)
    val activePet    = Color(0, 170, 0)

    val rarityCommon    = Color(170, 170, 170, 0.53f)
    val rarityUncommon  = Color(85, 255, 85, 0.53f)
    val rarityRare      = Color(85, 85, 255, 0.53f)
    val rarityEpic      = Color(170, 0, 170, 0.53f)
    val rarityLegendary = Color(255, 170, 0, 0.53f)
    val rarityMythic    = Color(255, 85, 255, 0.53f)

    fun rarityColor(tier: String): Color = when (tier.uppercase()) {
        "COMMON"    -> rarityCommon
        "UNCOMMON"  -> rarityUncommon
        "RARE"      -> rarityRare
        "EPIC"      -> rarityEpic
        "LEGENDARY" -> rarityLegendary
        "MYTHIC"    -> rarityMythic
        else        -> buttonBg
    }
}