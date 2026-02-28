package com.odtheking.odinaddon.pvgui

import com.odtheking.odinaddon.pvgui.utils.Theme
abstract class PVPage(override val name: String) : PageHandler {
    protected val member  get() = PVState.memberData()
    protected val profile get() = PVState.selectedProfile()
    protected val player  get() = PVState.playerData

    protected val mainX get() = MAIN_X
    protected val mainY get() = MAIN_Y
    protected val mainW get() = MAIN_W
    protected val mainH get() = MAIN_H
    protected val slotRadius get() = Theme.round
    protected val padding get() = PADDING
}