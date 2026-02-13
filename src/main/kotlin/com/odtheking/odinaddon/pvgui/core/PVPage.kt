package com.odtheking.odinaddon.pvgui.core

import com.odtheking.odinaddon.pvgui.utils.apiutils.HypixelData
import com.odtheking.odinaddon.pvgui.utils.apiutils.profileOrSelected
import net.minecraft.client.gui.GuiGraphics

abstract class PVPage(val name: String) {

    protected val mainX get() = PageData.mainX
    protected val mainWidth get() = PageData.mainWidth
    protected val mainHeight get() = PageData.mainHeight
    protected val mainCenterX get() = PageData.mainCenterX
    protected val spacer get() = PageData.spacer
    protected val mainY: Float get() = spacer.toFloat()

    protected val player: HypixelData.PlayerInfo
        get() = PVGui.playerData ?: PVGui.dummyPlayer

    protected val profile: HypixelData.MemberData?
        get() = player.profileOrSelected(PVGui.profileName)?.members?.get(player.uuid)

    protected val profileName: String?
        get() = PVGui.profileName

    protected val theme get() = Theme

    abstract fun draw(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int)
    open fun click(mouseX: Int, mouseY: Int, mouseButton: Int) {}
}