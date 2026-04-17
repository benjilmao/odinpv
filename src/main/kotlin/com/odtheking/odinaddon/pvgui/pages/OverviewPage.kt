package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.CONTENT_X
import com.odtheking.odinaddon.pvgui.CONTENT_Y
import com.odtheking.odinaddon.pvgui.MAIN_H
import com.odtheking.odinaddon.pvgui.MAIN_W
import com.odtheking.odinaddon.pvgui.PAD
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.dsl.DropDownDsl
import com.odtheking.odinaddon.pvgui.dsl.RenderQueue
import com.odtheking.odinaddon.pvgui.dsl.dropDown
import com.odtheking.odinaddon.pvgui.dsl.fillText
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
import kotlin.math.floor

object OverviewPage : PVPage() {
    override val name = "Overview"

    private val nameH = MAIN_H * 0.1f
    private val leftW = (MAIN_W * 2f / 3f) - PAD / 2f
    private val rightW = (MAIN_W / 3f) - PAD / 2f
    private val rightX = CONTENT_X + leftW + PAD
    private val dataY = CONTENT_Y + nameH + PAD
    private val dataH = MAIN_H - nameH - PAD

    private val playerBoxY = dataY
    private val statLines: List<String> by resettableLazy { buildStatLines() }

    private var dropdownBuiltFor: String? = null
    private var dropdown: DropDownDsl<String>? = null

//    @Volatile private var playerEntity: RemotePlayer? = null
//    private var playerEntityFor: String? = null

    override fun onOpen() {
        ensureDropdown()
        maybeLoadPlayer()
    }

    private fun ensureDropdown() {
        val player = PVState.player ?: return
        if (dropdownBuiltFor == player.name) return
        dropdownBuiltFor = player.name

        val selected = PVState.profile()
        val options = player.profileData.profiles.map {
            "§a${it.cuteName}§r §8(§7${it.gameMode ?: "normal"}§8)"
        }
        val default = "§a${selected?.cuteName ?: "?"}§r §8(§7${selected?.gameMode ?: "normal"}§8)"

        dropdown = dropDown(
            x = rightX, y = CONTENT_Y,
            w = rightW, h = nameH,
            items = options,
            default = default,
            spacer = PAD,
            radius = Theme.radius,
            label = { it },
        ) {
            onSelect { chosen ->
                val name = chosen.substringAfter("§a").substringBefore("§r ")
                PVState.profileName = name
                PVState.invalidate()
                ResettableLazy.resetAll()
                dropdownBuiltFor = null
            }
            onExtend { /* might add click sound */ }
        }
    }

    // I'll figure this shit out later I cant be bothered
    private fun maybeLoadPlayer() {
//        val player = PVState.player ?: return
//        if (playerEntityFor == player.name) return
//        playerEntityFor = player.name
//        playerEntity = null
//
//        scope.launch {
//            // Try world entity first (same lobby)
//            val worldEntity = mc.level?.players()?.find { it.name.string == player.name }
//            if (worldEntity != null) {
//                playerEntity = worldEntity as? RemotePlayer
//                return@launch
//            }
//
//            // Fall back: build GameProfile and load skin
//            val uuid = runCatching {
//                val u = player.uuid.replace("-", "")
//                UUID.fromString("${u.take(8)}-${u.substring(8,12)}-${u.substring(12,16)}-${u.substring(16,20)}-${u.substring(20)}")
//            }.getOrNull() ?: return@launch
//
//            val gameProfile = GameProfile(uuid, player.name)
//            val filled = runCatching {
//                mc.sessionService().fillProfileProperties(gameProfile, true)
//            }.getOrElse { gameProfile }
//
//            runCatching {
//                mc.skinManager().registerTextures(filled) { _, _, _ -> }
//            }
//
//            val level = mc.level ?: return@launch
//            playerEntity = RemotePlayer(level, filled)
//        }
    }

    override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        ensureDropdown()
        maybeLoadPlayer()

        NVGRenderer.rect(CONTENT_X, CONTENT_Y, leftW, nameH, Theme.slotBg, Theme.radius)
        fillText(
            "§${Theme.fontCode}${PVState.player?.name ?: PVState.statusText}",
            CONTENT_X + leftW / 2f, CONTENT_Y + nameH / 2f,
            leftW - PAD * 2f, nameH - PAD * 2f,
            Theme.textPrimary,
        )

        NVGRenderer.rect(CONTENT_X, dataY, leftW, dataH, Theme.slotBg, Theme.radius)
        textBox(
            x = CONTENT_X + PAD, y = dataY + PAD,
            w = leftW - PAD * 2f, h = dataH - PAD * 2f,
            lines = statLines, scale = 2.5f, spacer = PAD,
            color = Theme.textPrimary,
        ).draw()

        dropdown?.moveTo(rightX, CONTENT_Y, rightW, nameH)
        dropdown?.draw() ?: NVGRenderer.rect(rightX, CONTENT_Y, rightW, nameH, Theme.slotBg, Theme.radius)

        val dd = dropdown
        if (dd == null || !dd.extended) {
            NVGRenderer.rect(rightX, playerBoxY, rightW, dataH, Theme.slotBg, Theme.radius)
        }
    }

    override fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val dd = dropdown
        if (dd != null && dd.extended) return
//        playerEntity?.let { entity ->
//            RenderQueue.enqueueEntity(entity, rightX, playerBoxY, rightW, dataH)
//        }
    }

    override fun click(mouseX: Double, mouseY: Double): Boolean = dropdown?.click(mouseX, mouseY) ?: false

    private fun buildStatLines(): List<String> {
        val data = PVState.member() ?: return listOf("§7${PVState.statusText}")
        val mmComps = data.dungeons.dungeonTypes.mastermode.tierComps.without("total").values.sum()
        val fComps = data.dungeons.dungeonTypes.catacombs.tierComps.without("total").values.sum()
        val totalRuns = (mmComps + fComps).toDouble().coerceAtLeast(1.0)
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