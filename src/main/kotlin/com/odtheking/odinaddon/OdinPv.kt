package com.odtheking.odinaddon

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.OdinMod.scope
import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.utils.modMessage
import com.odtheking.odinaddon.commands.pvCommand
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import kotlinx.coroutines.launch
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.resources.ResourceLocation

object OdinPv : ClientModInitializer {

    override fun onInitializeClient() {
        scope.launch {
            runCatching {
                val url = java.net.URL("https://api.github.com/repos/benjilmao/odinpv/releases/latest")
                val json = com.google.gson.JsonParser.parseString(url.readText()).asJsonObject
                val latestTag = json.get("tag_name").asString
                val currentVersion = "v" + net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getModContainer("odinaddon").get().metadata.version.friendlyString
                if (latestTag != currentVersion) {
                    mc.execute {
                        modMessage("§aNew OdinPV update available: §f$latestTag §7(you have §f$currentVersion§7)")
                        modMessage("§7Download: §b§nhttps://github.com/benjilmao/odinpv/releases/latest")
                    }
                }
            }
        }


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
