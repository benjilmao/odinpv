package com.odtheking.odinaddon

import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odinaddon.commands.pvCommand
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.utils.NVGSpecialRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry

object OdinPv : ClientModInitializer {

    override fun onInitializeClient() {
        println("Odin Addon initialized!")

        // Register commands by adding to the array
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            arrayOf(
                pvCommand
            ).forEach { commodore -> commodore.register(dispatcher) }
        }

        SpecialGuiElementRegistry.register { context ->
            NVGSpecialRenderer(context.vertexConsumers())
        }

        // Register objects to event bus by adding to the list
        listOf(this).forEach { EventBus.subscribe(it) }

        ModuleManager.registerModules(
            ModuleConfig("OdinAddon.json"),
            ProfileViewerModule
        )
    }
}
