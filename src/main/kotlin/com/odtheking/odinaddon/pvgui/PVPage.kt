package com.odtheking.odinaddon.pvgui

import com.odtheking.odinaddon.pvgui.core.Component

abstract class PVPage : Component() {
    abstract val name: String
    protected val member get() = PVState.member()
    protected val profile get() = PVState.profile()
    protected val player get() = PVState.player

    open fun onOpen() {}
}