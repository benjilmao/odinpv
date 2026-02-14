package com.odtheking.odinaddon.features.impl.skyblock

import com.odtheking.odin.clickgui.settings.AlwaysActive
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.*
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.Category
import com.odtheking.odin.utils.Colors
import com.odtheking.odinaddon.pvgui.core.ThemeData

@AlwaysActive
object ProfileViewerModule : Module(
    name = "Profile Viewer",
    description = "Settings for the profile viewer GUI.",
    category = Category.SKYBLOCK
) {
    // Command setting
    val pvCommand by BooleanSetting(
        name = "PV Command",
        default = true,
        desc = "Enables the /pv command. (Overwrites NEU PV)"
    )

    // GUI behavior
    val animations by BooleanSetting(
        name = "Animations",
        default = true,
        desc = "Enable opening animations for the GUI."
    )

    // Scale
    var scale by NumberSetting(
        name = "Scale",
        default = 1.0,
        min = 0.1,
        max = 3.0,
        increment = 0.1,
        desc = "Scale of the profile viewer GUI.",
        unit = "x"
    )

    // Max rows for talisman page
    val maxRows by NumberSetting(
        name = "Max Rows",
        default = 7,
        min = 1,
        max = 7,
        increment = 1,
        desc = "Maximum rows in the talisman page."
    )

    // Theme selection
    private val themeNames = listOf("Odin", "Midnight", "Light", "Sunrise", "Custom")
    val theme by SelectorSetting(
        name = "Theme",
        default = "Odin",
        options = themeNames,
        desc = "Color theme for the profile viewer."
    )

    // Custom theme colors
    val mainColor by ColorSetting(
        name = "Main Background",
        default = Colors.gray26,
        allowAlpha = true,
        desc = "Main background color."
    ).withDependency { theme == themeNames.lastIndex }

    val fontColor by ColorSetting(
        name = "Font",
        default = Colors.WHITE,
        allowAlpha = true,
        desc = "Font color."
    ).withDependency { theme == themeNames.lastIndex }

    val itemsColor by ColorSetting(
        name = "Items Background",
        default = Colors.gray38,
        allowAlpha = true,
        desc = "Background color for content panels."
    ).withDependency { theme == themeNames.lastIndex }

    val lineColor by ColorSetting(
        name = "Line",
        default = Colors.WHITE,
        allowAlpha = true,
        desc = "Separator line color."
    ).withDependency { theme == themeNames.lastIndex }

    val fontColorCode by StringSetting(
        name = "Font Code",
        default = "f",
        length = 1,
        desc = "Minecraft color code for font (e.g., f for white)."
    ).withDependency { theme == themeNames.lastIndex }

    val selectedColor by ColorSetting(
        name = "Selected",
        default = Colors.MINECRAFT_DARK_AQUA,
        allowAlpha = true,
        desc = "Color for selected buttons."
    ).withDependency { theme == themeNames.lastIndex }

    val buttonColor by ColorSetting(
        name = "Button",
        default = Colors.gray38,
        allowAlpha = true,
        desc = "Background color for buttons."
    ).withDependency { theme == themeNames.lastIndex }

    val roundness by NumberSetting(
        name = "Roundness",
        default = 12f,
        min = 0f,
        max = 20f,
        increment = 0.5f,
        desc = "Corner roundness of panels."
    ).withDependency { theme == themeNames.lastIndex }

    val buttonRoundness by NumberSetting(
        name = "Button Roundness",
        default = 6f,
        min = 0f,
        max = 20f,
        increment = 0.5f,
        desc = "Corner roundness of buttons."
    ).withDependency { theme == themeNames.lastIndex }

    val inventoryRound by NumberSetting(
        name = "Inventory Roundness",
        default = 0f,
        min = 0f,
        max = 20f,
        increment = 0.5f,
        desc = "Roundness for inventory item backgrounds."
    ).withDependency { theme == themeNames.lastIndex }

    val rarityBackgrounds by BooleanSetting(
        name = "Rarity Backgrounds",
        default = false,
        desc = "Render background colors according to item rarity in inventory."
    ).withDependency { theme == themeNames.lastIndex }

    val currentTheme: ThemeData
        get() = if (theme == themeNames.lastIndex) {
            ThemeData(
                main = mainColor,
                font = fontColor,
                items = itemsColor,
                line = lineColor,
                fontCode = fontColorCode,
                selected = selectedColor,
                button = buttonColor,
                roundness = roundness,
                buttonRoundness = buttonRoundness,
                inventoryRound = inventoryRound,
                rarityBackgrounds = rarityBackgrounds
            )
        } else {
            when (theme) {
                0 -> ThemeData.Odin
                1 -> ThemeData.Midnight
                2 -> ThemeData.Light
                3 -> ThemeData.Sunrise
                else -> ThemeData.Odin
            }
        }
}