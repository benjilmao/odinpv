package com.odtheking.odinaddon

import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odinaddon.commands.pvCommand
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.resources.ResourceLocation

object OdinPv : ClientModInitializer {

    override fun onInitializeClient() {
        val theirPhase = ResourceLocation.fromNamespaceAndPath("skyblockpv", "skyblock_pv_command")
        val ourPhase = ResourceLocation.fromNamespaceAndPath("odinaddon", "pv_override")
        ClientCommandRegistrationCallback.EVENT.addPhaseOrdering(theirPhase, ourPhase)
        ClientCommandRegistrationCallback.EVENT.register(ourPhase) { dispatcher, _ ->
            dispatcher.root.children.removeIf { it.name == "pv" }
            arrayOf(pvCommand).forEach { it.register(dispatcher) }
        }

        listOf(this).forEach { EventBus.subscribe(it) }

        ModuleManager.registerModules(
            ModuleConfig("OdinAddon.json"),
            ProfileViewerModule
        )
    }
}