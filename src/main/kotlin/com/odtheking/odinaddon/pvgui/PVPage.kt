package com.odtheking.odinaddon.pvgui

abstract class PVPage(override val name: String) : PageHandler {
    protected val member  get() = PVState.memberData()
    protected val profile get() = PVState.selectedProfile()
    protected val player  get() = PVState.playerData

    protected val mainX get() = PVLayout.MAIN_X
    protected val mainY get() = PVLayout.MAIN_Y
    protected val mainW get() = PVLayout.MAIN_W
    protected val mainH get() = PVLayout.MAIN_H
}