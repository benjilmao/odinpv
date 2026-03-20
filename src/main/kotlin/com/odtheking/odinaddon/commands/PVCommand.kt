package com.odtheking.odinaddon.commands

import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.utils.GreedyString
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.OdinMod.scope
import com.odtheking.odin.utils.modMessage
import com.odtheking.odinaddon.pvgui.PVScreen
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.utils.api.RequestUtils
import kotlinx.coroutines.launch

val pvCommand = Commodore("pv", "opv", "profileviewer") {
    runs { playerName: GreedyString? ->
        val name = playerName?.string ?: mc.user?.name ?: return@runs
        scope.launch {
            val uuid = RequestUtils.getUuid(name)
            if (uuid.isFailure) {
                modMessage("§cPlayer §f$name §cdoes not exist.", "")
                return@launch
            }
            mc.execute { mc.setScreen(PVScreen) }
            PVState.loadPlayer(name)
        }
    }
}