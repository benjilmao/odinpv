package com.odtheking.odinaddon.commands

import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.utils.GreedyString
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.OdinMod.scope
import com.odtheking.odin.utils.modMessage
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.core.PVGui
import kotlinx.coroutines.launch

val pvCommand = Commodore("pv", "profileviewer") {
    runs { playerName: GreedyString? ->
        if (!ProfileViewerModule.enabled) {
            modMessage("§cProfile Viewer module is disabled.")
            return@runs
        }
        val name = playerName?.string ?: mc.user?.name ?: return@runs modMessage("§cUnable to get player name!")
        scope.launch {
            PVGui.loadPlayer(name)
            mc.execute {
                mc.setScreen(PVGui)
            }
        }
    }
}