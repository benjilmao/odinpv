package com.odtheking.odinaddon.commands

import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.utils.GreedyString
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.OdinMod.scope
import com.odtheking.odinaddon.pvgui2.TestGui
import kotlinx.coroutines.launch

val pvCommand = Commodore("pv", "profileviewer") {
    runs { playerName: GreedyString? ->
        val name = playerName?.string ?: mc.user?.name ?: return@runs
        scope.launch {
            TestGui.loadPlayer(name)
            mc.execute { mc.setScreen(TestGui) }
        }
    }
}
