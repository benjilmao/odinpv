package com.odtheking.odinaddon.features.impl.skyblock

import com.odtheking.odin.clickgui.settings.AlwaysActive
import com.odtheking.odin.clickgui.settings.impl.*
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.Category
import com.odtheking.odin.utils.Color

@AlwaysActive
object ProfileViewerModule : Module(
    name = "Profile Viewer",
    description = "Settings for the profile viewer GUI.",
    category = Category.SKYBLOCK
) {
    val pvCommand by BooleanSetting(
        name = "PV Command",
        default = true,
        desc = "Enables the /pv command."
    )

    var scale by NumberSetting(
        name = "Scale",
        default = 1.0,
        min = 0.5,
        max = 2.0,
        increment = 0.1,
        desc = "Scale of the profile viewer GUI.",
        unit = "x"
    )
    val rarityBackgrounds by BooleanSetting(
        name = "Rarity Backgrounds",
        default = true,
        desc = "Show item rarity as slot background color."
    )

    val dropShadow by BooleanSetting(
        name = "Drop Shadow",
        default = true,
        desc = "Show drop shadow behind the GUI."
    )

    val bgColor by ColorSetting(
        name = "Background",
        default = Color(26, 26, 46),
        allowAlpha = true,
        desc = "Main GUI background color."
    )

    val accentColor by ColorSetting(
        name = "Accent Color",
        default = Color(26, 74, 138),
        allowAlpha = true,
        desc = "Selected button / accent color."
    )

    val roundness by NumberSetting(
        name = "Roundness",
        default = 8f,
        min = 0f,
        max = 20f,
        increment = 0.5f,
        desc = "Corner roundness for the whole UI."
    )
}