package com.odtheking.odinaddon.pvgui.pages

import com.mojang.authlib.GameProfile
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.OdinMod.scope
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.CONTENT_X
import com.odtheking.odinaddon.pvgui.CONTENT_Y
import com.odtheking.odinaddon.pvgui.MAIN_H
import com.odtheking.odinaddon.pvgui.MAIN_W
import com.odtheking.odinaddon.pvgui.PAD
import com.odtheking.odinaddon.pvgui.QUAD_W
import com.odtheking.odinaddon.pvgui.TOTAL_H
import com.odtheking.odinaddon.pvgui.TOTAL_W
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.dsl.DropDownDsl
import com.odtheking.odinaddon.pvgui.dsl.TextBox
import com.odtheking.odinaddon.pvgui.dsl.dropDown
import com.odtheking.odinaddon.pvgui.dsl.fillText
import com.odtheking.odinaddon.pvgui.dsl.formattedText
import com.odtheking.odinaddon.pvgui.dsl.textBox
import com.odtheking.odinaddon.pvgui.utils.LevelUtils
import com.odtheking.odinaddon.pvgui.utils.LevelUtils.cataLevel
import com.odtheking.odinaddon.pvgui.utils.ResettableLazy
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.colorize
import com.odtheking.odinaddon.pvgui.utils.colorizeNumber
import com.odtheking.odinaddon.pvgui.utils.commas
import com.odtheking.odinaddon.pvgui.utils.coloredName
import com.odtheking.odinaddon.pvgui.utils.heldItemStack
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import com.odtheking.odinaddon.pvgui.utils.without
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.item.component.ResolvableProfile
import kotlinx.coroutines.launch
import kotlin.math.floor

object OverviewPage : PVPage() {
    override val name = "Overview"
    private val nameW = (MAIN_W * 2f / 3f) / 2f
    private val nameH = MAIN_H * 0.1f
    private val dataH = MAIN_H - nameH - PAD
    private val dropW = (MAIN_W / 3f) - PAD / 2f
    private val dropX = CONTENT_X + PAD + nameW
    private val playerBoxX = dropX
    private val playerBoxY = CONTENT_Y + 2f * PAD + nameH
    private val statLines: List<String> by resettableLazy { buildStatLines() }

    private val textBox: TextBox by resettableLazy {
        textBox(
            x = CONTENT_X + PAD, y = CONTENT_Y + nameH + 2f * PAD + PAD,
            w = nameW - 2f * PAD, h = dataH - 2f * PAD,
            lines = statLines, scale = 2.5f, spacer = PAD,
            color = Theme.textPrimary,
        )
    }

    private var dropdown: DropDownDsl<String>? = null

    override fun onOpen() {
        dropdown = null
        playerEntity = null
        scope.launch { loadPlayerEntity() }
    }

    private fun buildOrGetDropdown(): DropDownDsl<String> {
        dropdown?.let {
            it.moveTo(dropX, CONTENT_Y, dropW, nameH)
            return it
        }
        val player = PVState.player ?: return buildEmptyDropdown()
        val selected = PVState.profile()
        val options = player.profileData.profiles.map {
            "§a${it.cuteName}§r §8(§7${it.gameMode ?: "normal"}§8)"
        }
        val default = "§a${selected?.cuteName ?: "?"}§r §8(§7${selected?.gameMode ?: "normal"}§8)"

        return dropDown(
            x = dropX, y = CONTENT_Y,
            w = dropW, h = nameH,
            items = options, default = default,
            spacer = PAD, radius = Theme.radius,
            label = { it },
        ) {
            onSelect { chosen ->
                val name = chosen.substringAfter("§a").substringBefore("§r ")
                PVState.profileName = name
                PVState.invalidate()
                ResettableLazy.resetAll()
                dropdown = null
            }
            onExtend { }
        }.also { dropdown = it }
    }

    private fun buildEmptyDropdown() = dropDown(
        x = dropX, y = CONTENT_Y, w = dropW, h = nameH,
        items = emptyList(), default = "—",
        label = { it },
    )

    private var playerEntity: RemotePlayer? = null

    private suspend fun loadPlayerEntity() {
//        // Check world first
//        mc.level?.playerEntities?.forEach { entity ->
//            if (entity.name.string == PVState.player?.name) {
//                playerEntity = entity as? RemotePlayer
//                return
//            }
//        }
//
//        val profile = PVState.playerGameProfile ?: return
//        val filled  = mc.sessionService.fillProfileProperties(GameProfile(profile.id, profile.name), true)
//        runCatching {
//            mc.skinManager.loadProfileTextures(filled, { _, _, _ -> }, false)
//        }
//        mc.level?.let { level ->
//            playerEntity = RemotePlayer(level, filled)
//        }
    }

    override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        NVGRenderer.rect(CONTENT_X, CONTENT_Y, nameW, nameH, Theme.slotBg, Theme.radius)
        fillText(
            "§${Theme.fontCode}${PVState.player?.name ?: PVState.statusText}",
            CONTENT_X + nameW / 2f,
            CONTENT_Y + nameH / 2f,
            nameW - 2f * PAD,
            nameH - 2f * PAD,
            Theme.textPrimary,
        )

        NVGRenderer.rect(CONTENT_X, CONTENT_Y + nameH + 2f * PAD, nameW, dataH, Theme.slotBg, Theme.radius)
        textBox.draw()
        buildOrGetDropdown().draw()
        val dd = dropdown
        if (dd == null || !dd.extended) {
            NVGRenderer.rect(playerBoxX, playerBoxY, dropW, dataH, Theme.slotBg, Theme.radius)
        }
    }

    override fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val dd = dropdown
        if (dd != null && dd.extended) return
        playerEntity?.let { entity ->
            com.odtheking.odinaddon.pvgui.dsl.RenderQueue.enqueueEntity(
                entity,
                playerBoxX, playerBoxY, dropW, dataH
            )
        }
    }

    override fun click(mouseX: Double, mouseY: Double): Boolean {
        return buildOrGetDropdown().click(mouseX, mouseY)
    }

    private fun buildStatLines(): List<String> {
        val data = PVState.member() ?: return listOf("§7${PVState.statusText}")
        val mmComps = data.dungeons.dungeonTypes.mastermode.tierComps
            .filter { it.key != "total" }.values.sum()
        val floorComps = data.dungeons.dungeonTypes.catacombs.tierComps
            .filter { it.key != "total" }.values.sum()
        val totalRuns = (mmComps + floorComps).toDouble().coerceAtLeast(1.0)
        val pet = data.pets.activePet
        return listOf(
            "Level§7: ${floor(data.leveling.experience / 100.0).toInt().toDouble().colorize(500.0, 0)}",
            "§4Cata Level§7: ${data.dungeons.dungeonTypes.cataLevel.colorize(50.0)}",
            "§6Skill Average§7: ${LevelUtils.cappedSkillAverage(data.playerData).colorize(55.0)} §7(${"%.2f".format(LevelUtils.skillAverage(data.playerData))})",
            "§bSecrets§7: ${data.dungeons.secrets.colorizeNumber(100_000)}${data.dungeons.secrets.commas} §7(${(data.dungeons.secrets.toDouble() / totalRuns).colorize(15.0)}§7)",
            "Magical Power§7: ${data.assumedMagicalPower.toDouble().colorize(1800.0, 0)}",
            "${pet?.coloredName ?: "§7None!"}${pet?.heldItemStack?.hoverName?.string?.let { " §7($it§7)" } ?: ""}",
        )
    }
}