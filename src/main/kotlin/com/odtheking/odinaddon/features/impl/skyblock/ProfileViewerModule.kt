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

    val guiBg by ColorSetting(
        name = "Background",
        default = Color(26, 26, 46),
        allowAlpha = true,
        desc = "Main GUI background color."
    )
    val buttonColor by ColorSetting(
        name = "Button",
        default = Color(26, 74, 138),
        allowAlpha = true,
        desc = "Selected/accent button color."
    )
    val buttonBg by ColorSetting(
        name = "Button",
        default = Color(255, 255, 255, 0.13f),
        allowAlpha = true,
        desc = "Unselected button background."
    )

    val buttonSelected by ColorSetting(
        name = "Button Selected",
        default = Color(26, 74, 138),
        allowAlpha = true,
        desc = "Selected button background."
    )

    val separatorColor by ColorSetting(
        name = "Separator",
        default = Color(255, 255, 255, 0.2f),
        allowAlpha = true,
        desc = "Separator line color."
    )

    val slotBg by ColorSetting(
        name = "Slot Background",
        default = Color(0, 0, 0, 0.53f),
        allowAlpha = true,
        desc = "Item slot background color."
    )

    val guiRoundness by NumberSetting(
        name = "GUI Roundness",
        default = 12f,
        min = 0f,
        max = 20f,
        increment = 0.5f,
        desc = "Corner roundness of the main GUI."
    )

    val buttonRoundness by NumberSetting(
        name = "Button Roundness",
        default = 6f,
        min = 0f,
        max = 20f,
        increment = 0.5f,
        desc = "Corner roundness of buttons."
    )

    val slotRoundness by NumberSetting(
        name = "Slot Roundness",
        default = 4f,
        min = 0f,
        max = 12f,
        increment = 0.5f,
        desc = "Corner roundness of item slots."
    )
}