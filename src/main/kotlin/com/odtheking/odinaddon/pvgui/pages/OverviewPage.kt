package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.dsl.DropDownDsl
import com.odtheking.odinaddon.pvgui.dsl.RenderQueue
import com.odtheking.odinaddon.pvgui.dsl.TextBox
import com.odtheking.odinaddon.pvgui.dsl.dropDown
import com.odtheking.odinaddon.pvgui.utils.LevelUtils
import com.odtheking.odinaddon.pvgui.utils.LevelUtils.cataLevel
import com.odtheking.odinaddon.pvgui.utils.ResettableLazy
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.colorize
import com.odtheking.odinaddon.pvgui.utils.colorizeNumber
import com.odtheking.odinaddon.pvgui.utils.commas
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import com.odtheking.odinaddon.pvgui.utils.without
import com.odtheking.odinaddon.pvgui.utils.coloredName
import com.odtheking.odinaddon.pvgui.utils.heldItemStack
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.item.component.ResolvableProfile
import kotlin.math.floor

object OverviewPage : PVPage() {
    override val name = "Overview"

    private val spacing = 12f
    private val nameH get() = h * 0.10f
    val leftW get() = (w * 2f / 3f) - spacing / 2f
    val rightW get() = (w / 3f) - spacing / 2f
    val rightX get() = x + leftW + spacing
    val dataY get() = y + nameH + spacing
    val dataH get() = h - nameH - spacing

    private val statLines: List<String> by resettableLazy { buildStatLines() }
    private var dropdown: DropDownDsl<String>? = null
    private var dropdownOpen = false
    private var yaw = 0f

    override fun onOpen() = rebuildDropdown()

    private fun rebuildDropdown() {
        val player = PVState.player ?: return
        val selected = PVState.profile()
        val options = player.profileData.profiles.map {
            "§a${it.cuteName}§r §8(§7${it.gameMode ?: "normal"}§8)"
        }
        val default = "§a${selected?.cuteName ?: "?"}§r §8(§7${selected?.gameMode ?: "normal"}§8)"
        dropdown = dropDown(
            x = 0f, y = 0f, w = 100f, h = nameH,
            items = options,
            label = { it },
            onSelect = { chosen ->
                val name = chosen.substringAfter("§a").substringBefore("§r ")
                PVState.profileName = name
                PVState.invalidate()
                ResettableLazy.resetAll()
                rebuildDropdown()
            },
            onExtend = { open -> dropdownOpen = open },
        ).also { it.selected = default }
    }

    override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val font = NVGRenderer.defaultFont
        NVGRenderer.rect(x, y, leftW, nameH, Theme.slotBg, Theme.radius)
        val name = PVState.player?.name ?: PVState.statusText
        val nameWidth = NVGRenderer.textWidth(name, 24f, font)
        NVGRenderer.text(name, x + (leftW - nameWidth) / 2f, y + (nameH - 18f) / 2f, 24f, Theme.textPrimary, font)
        TextBox(x = x, y = dataY, w = leftW, h = dataH,
            lines = statLines, textSize = 22f,
            background = Theme.slotBg).draw()
        NVGRenderer.rect(rightX, dataY, rightW, dataH, Theme.slotBg, Theme.radius)
        dropdown?.moveTo(rightX, y, rightW, nameH)
        dropdown?.draw()
        if (dropdown == null) NVGRenderer.rect(rightX, y, rightW, nameH, Theme.slotBg, Theme.radius)
    }

    override fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (!dropdownOpen) fakePlayer?.let { RenderQueue.enqueueEntity(it, rightX, dataY, rightW, dataH) }
    }

    override fun click(mouseX: Double, mouseY: Double): Boolean =
        dropdown?.click(mouseX, mouseY) ?: false

    fun onMouseDrag(deltaX: Double, deltaY: Double) {
        yaw += deltaX.toFloat()
    }

    private val fakePlayer: RemotePlayer? by resettableLazy {
        val gameProfile = PVState.playerGameProfile ?: return@resettableLazy null
        mc.level ?: return@resettableLazy null
        ResolvableProfile.createUnresolved(gameProfile.id).also {
            it.resolveProfile(mc.services().profileResolver)
        }
        RemotePlayer(mc.level!!, gameProfile)
    }

    private fun buildStatLines(): List<String> {
        val data = PVState.member() ?: return listOf("§7${PVState.statusText}")
        val mastermodeCount = data.dungeons.dungeonTypes.mastermode.tierComps.without("total").values.sum()
        val catacombsCount = data.dungeons.dungeonTypes.catacombs.tierComps.without("total").values.sum()
        val totalRuns = (mastermodeCount + catacombsCount).toDouble().coerceAtLeast(1.0)
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