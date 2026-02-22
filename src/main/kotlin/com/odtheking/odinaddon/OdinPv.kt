package com.odtheking.odinaddon

import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odinaddon.commands.pvCommand
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback

object OdinPv : ClientModInitializer {

    override fun onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            arrayOf(
                pvCommand
            ).forEach { commodore -> commodore.register(dispatcher) }
        }

        listOf(this).forEach { EventBus.subscribe(it) }

        ModuleManager.registerModules(
            ModuleConfig("OdinAddon.json"),
            ProfileViewerModule
        )
    }
}
