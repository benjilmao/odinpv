package com.odtheking.odinaddon.features.impl.skyblock

import com.odtheking.odin.clickgui.settings.AlwaysActive
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors

@AlwaysActive
object ProfileViewerModule : Module(
    name = "Profile Viewer",
    description = "Settings for the profile viewer GUI.",
    category = Category.SKYBLOCK
) {
    val scale by NumberSetting("Scale", default = 1.0, min = 0.5, max = 2.0, increment = 0.1, desc = "Scale of the profile viewer GUI.")
    val dropShadow by BooleanSetting("Drop Shadow", default = true, desc = "Show drop shadow behind the GUI.")
    val rarityBackgrounds by BooleanSetting("Rarity Background", default = false, desc = "Renders a background according to the rarity of the item in front of it.")

    private val themesList = arrayListOf("Odin", "Midnight", "Light", "Sunrise", "Custom")
    val themes by SelectorSetting("Theme", default = "Odin", themesList, desc = "Preferred theme.")

    val main by ColorSetting("Background", default = Colors.gray26, allowAlpha = true, desc = "Color for the background.").withDependency { themes == themesList.lastIndex }
    val font by ColorSetting("Font", default = Colors.WHITE, allowAlpha = true, desc = "Font color.").withDependency { themes == themesList.lastIndex }
    val items by ColorSetting("Items", default = Colors.gray38, allowAlpha = true, desc = "Background color of items.").withDependency { themes == themesList.lastIndex }
    val line by ColorSetting("Line", default = Colors.WHITE, allowAlpha = true, desc = "Separator line color.").withDependency { themes == themesList.lastIndex }
    val selected by ColorSetting("Selected", default = Colors.MINECRAFT_DARK_AQUA, allowAlpha = true, desc = "Color for selected buttons.").withDependency { themes == themesList.lastIndex }
    val button by ColorSetting("Button", default = Colors.gray38, allowAlpha = true, desc = "Color for buttons.").withDependency { themes == themesList.lastIndex }
    val roundness by NumberSetting("Roundness", default = 10f, min = 0f, max = 20f, increment = 0.5f, desc = "Roundness for the whole GUI.").withDependency { themes == themesList.lastIndex }
    val inventoryRound by NumberSetting("Inventory Roundness", default = 8f, min = 0f, max = 20f, increment = 0.5f, desc = "Roundness for inventory item backgrounds.").withDependency { themes == themesList.lastIndex }

    data class PVTheme(
        val name: String,
        val main: Color,
        val font: Color,
        val items: Color,
        val line: Color,
        val selected: Color,
        val button: Color,
        val roundness: Float,
        val inventoryRound: Float,
    )

    val themeEntries = listOf(
        PVTheme("Odin", Colors.gray26, Colors.WHITE, Colors.gray38, Colors.WHITE, Colors.MINECRAFT_DARK_AQUA, Colors.gray38, 10f, 8f),
        PVTheme("Midnight", Color("151345FF"), Colors.WHITE, Color("1c1d54FF"), Color("040622FF"), Color("26236bFF"), Color("040622FF"), 10f, 8f),
        PVTheme("Light", Colors.WHITE, Colors.BLACK, Colors.MINECRAFT_DARK_GRAY,Colors.MINECRAFT_DARK_GRAY, Colors.MINECRAFT_GRAY, Colors.MINECRAFT_DARK_GRAY, 10f, 8f),
        PVTheme("Sunrise", Color("fDf1CDFF"), Color("805690FF"), Color("f9dc90FF"), Color("805690FF"), Color("f89e9dFF"), Color("d46f93FF"), 10f, 8f),
    )

    val maxRows by NumberSetting(
        "Tali Rows",
        default = 7,
        min = 1f,
        max = 7f,
        increment = 1f,
        desc = "Maximum rows shown in talisman page. Lower = better performance."
    )

    val currentTheme get() = themeEntries.getOrNull(themes) ?: PVTheme(
        "Custom", main, font, items, line, selected, button, roundness, inventoryRound
    )
}