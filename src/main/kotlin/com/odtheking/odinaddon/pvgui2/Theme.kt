package com.odtheking.odinaddon.pvgui2

import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule

object Theme {
    val guiBg        get() = ProfileViewerModule.guiBg.rgba.toUInt()
    val buttonBg     get() = ProfileViewerModule.buttonBg.rgba.toUInt()
    val buttonSelect get() = ProfileViewerModule.buttonSelected.rgba.toUInt()
    val separator    get() = ProfileViewerModule.separatorColor.rgba
    val slotBg       get() = ProfileViewerModule.slotBg.rgba.toUInt()
    val guiRound     get() = ProfileViewerModule.guiRoundness
    val btnRound     get() = ProfileViewerModule.buttonRoundness.toFloat()
    val slotRound    get() = ProfileViewerModule.slotRoundness.toFloat()
    val rarityBg     get() = ProfileViewerModule.rarityBackgrounds

    fun raritySlotColor(tier: String): UInt = when (tier.uppercase()) {
        "COMMON"    -> 0x88AAAAAAU
        "UNCOMMON"  -> 0x8855FF55U
        "RARE"      -> 0x885555FFU
        "EPIC"      -> 0x88AA00AAU
        "LEGENDARY" -> 0x88FFAA00U
        "MYTHIC"    -> 0x88FF55FFU
        else        -> slotBg
    }

    fun rarityFromLore(lore: List<String>): UInt {
        val last = lore.lastOrNull()?.uppercase() ?: return slotBg
        return when {
            last.contains("MYTHIC")    -> 0x88FF55FFU
            last.contains("LEGENDARY") -> 0x88FFAA00U
            last.contains("EPIC")      -> 0x88AA00AAU
            last.contains("RARE")      -> 0x885555FFU
            last.contains("UNCOMMON")  -> 0x8855FF55U
            last.contains("COMMON")    -> 0x88AAAAAAU
            last.contains("DIVINE")    -> 0x8855FFFFU
            last.contains("SPECIAL")   -> 0x88FF5555U
            else                       -> slotBg
        }
    }

    fun petTierColor(tier: String): String = when (tier.uppercase()) {
        "COMMON"    -> "§f"
        "UNCOMMON"  -> "§a"
        "RARE"      -> "§9"
        "EPIC"      -> "§5"
        "LEGENDARY" -> "§6"
        "MYTHIC"    -> "§d"
        else        -> "§7"
    }
}