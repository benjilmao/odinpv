package com.odtheking.odinaddon.pvgui.core

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Color.Companion.brighter
import com.odtheking.odin.utils.Colors
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule

data class ThemeData(
    val main: Color,
    val font: Color,
    val items: Color,
    val line: Color,
    val fontCode: String,
    val selected: Color,
    val button: Color,
    val roundness: Float,
    val buttonRoundness: Float,
    val inventoryRound: Float = 0f,
    val rarityBackgrounds: Boolean = false
) {
    companion object {
        val Odin = ThemeData(
            main = Colors.gray26,
            font = Colors.WHITE,
            items = Colors.gray38,
            line = Colors.WHITE,
            fontCode = "f",
            selected = Colors.MINECRAFT_DARK_AQUA,
            button = Colors.gray38,
            roundness = 12f,
            buttonRoundness = 6f,
            inventoryRound = 0f,
            rarityBackgrounds = false
        )

        val Midnight = ThemeData(
            main = Color("151345FF"),
            font = Colors.WHITE,
            items = Color("1c1d54FF"),
            line = Color("040622FF"),
            fontCode = "f",
            selected = Color("26236bFF"),
            button = Color("040622FF"),
            roundness = 12f,
            buttonRoundness = 6f,
            inventoryRound = 0f,
            rarityBackgrounds = false
        )

        val Light = ThemeData(
            main = Colors.WHITE,
            font = Colors.BLACK,
            items = Colors.MINECRAFT_DARK_GRAY,
            line = Colors.MINECRAFT_DARK_GRAY,
            fontCode = "0",
            selected = Colors.MINECRAFT_GRAY,
            button = Colors.MINECRAFT_DARK_GRAY,
            roundness = 12f,
            buttonRoundness = 6f,
            inventoryRound = 0f,
            rarityBackgrounds = false
        )

        val Sunrise = ThemeData(
            main = Color("fDf1CDFF"),
            font = Color("805690FF"),
            items = Color("f9dc90FF"),
            line = Color("805690FF"),
            fontCode = "b",
            selected = Color("f89e9dFF"),
            button = Color("d46f93FF"),
            roundness = 12f,
            buttonRoundness = 6f,
            inventoryRound = 0f,
            rarityBackgrounds = false
        )
    }
}

object Theme {
    val bgColor: Color get() = ProfileViewerModule.currentTheme.main
    val secondaryBg: Color get() = ProfileViewerModule.currentTheme.items
    val roundness: Float get() = ProfileViewerModule.currentTheme.roundness

    val buttonBg: Color get() = ProfileViewerModule.currentTheme.button
    val buttonHover: Color get() = ProfileViewerModule.currentTheme.button.brighter(1.2f)
    val buttonSelected: Color get() = ProfileViewerModule.currentTheme.selected
    val buttonRoundness: Float get() = ProfileViewerModule.currentTheme.buttonRoundness

    val lineColor: Color get() = ProfileViewerModule.currentTheme.line
    val fontColor: Color get() = ProfileViewerModule.currentTheme.font
    val accentColor: Color get() = ProfileViewerModule.currentTheme.selected

    val fontCode: String get() = ProfileViewerModule.currentTheme.fontCode

    val inventoryRound: Float get() = ProfileViewerModule.currentTheme.inventoryRound
    val rarityBackgrounds: Boolean get() = ProfileViewerModule.currentTheme.rarityBackgrounds

    val petTierColors = mapOf(
        "COMMON" to "§f",
        "UNCOMMON" to "§a",
        "RARE" to "§9",
        "EPIC" to "§5",
        "LEGENDARY" to "§6",
        "MYTHIC" to "§d"
    )
}