package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.toFixed
import com.odtheking.odinaddon.pvgui.PADDING
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.components.DropDown
import com.odtheking.odinaddon.pvgui.components.TextBox
import com.odtheking.odinaddon.pvgui.components.TextBoxLine
import com.odtheking.odinaddon.pvgui.components.asText
import com.odtheking.odinaddon.pvgui.components.withItem
import com.odtheking.odinaddon.pvgui.core.RenderContext
import com.odtheking.odinaddon.pvgui.core.Renderer
import com.odtheking.odinaddon.pvgui.utils.LevelUtils
import com.odtheking.odinaddon.pvgui.utils.LevelUtils.cataLevel
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.activeDisplay
import com.odtheking.odinaddon.pvgui.utils.colorize
import com.odtheking.odinaddon.pvgui.utils.colorizeNumber
import com.odtheking.odinaddon.pvgui.utils.commas
import com.odtheking.odinaddon.pvgui.utils.heldItemStack
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.component.ResolvableProfile
import kotlin.math.floor

object OverviewPage : PVPage() {
    override val name = "Overview"
    private const val TITLE_SIZE = 22f
    private const val DD_H = 32f

    private val lines by resettableLazy { buildLines() }
    private val dropdown = DropDown(rowH = DD_H, textSize = 15f).apply {
        onSelect { idx ->
            val profiles = player?.profileData?.profiles ?: return@onSelect
            PVState.profileName = profiles.getOrNull(idx)?.cuteName
            PVState.invalidate()
        }
    }

    private fun buildLines(): List<TextBoxLine> {
        val data = member ?: return emptyList()
        val petItem = data.pets.activePet?.heldItemStack
        val base = listOf(
            "Level§7: §a${floor(data.leveling.experience / 100.0).toInt()}".asText(),
            "§4Cata Level§7: ${data.dungeons.dungeonTypes.cataLevel.colorize(50.0)}".asText(),
            "§6Skill Average§7: ${LevelUtils.cappedSkillAverage(data.playerData).colorize(55.0)} §7(${LevelUtils.skillAverage(data.playerData).toFixed(2)})".asText(),
            "§bSecrets§7: ${data.dungeons.secrets.colorizeNumber(100000)}${data.dungeons.secrets.commas} §7(${data.dungeons.avrSecrets.colorize(15.0)}§7)".asText(),
            "Magical Power§7: ${data.assumedMagicalPower.toDouble().colorize(1900.0, 0)}".asText(),
        )
        val petLine = if (petItem != null && !petItem.isEmpty)
            data.pets.activeDisplay.withItem(petItem, inline = true)
        else
            data.pets.activeDisplay.asText()
        return base + petLine
    }

    private fun profileLabel(cuteName: String?) = "§f${cuteName ?: "?"}"
    private fun modeIcon(mode: String?): String? = when (mode?.lowercase()) {
        "ironman" -> "§7♲"
        "stranded" -> "§a☀"
        "bingo" -> "§7Ⓑ"
        else -> null
    }

    private val fakePlayer: LivingEntity? by resettableLazy {
        val gp = PVState.playerGameProfile ?: return@resettableLazy null
        val level = mc.level ?: return@resettableLazy null
        ResolvableProfile.createUnresolved(gp.id).also {
            it.resolveProfile(mc.services().profileResolver)
        }
        RemotePlayer(level, gp)
    }

    override fun draw(ctx: RenderContext) {
        val player = player ?: return
        val profile = profile ?: return
        if (lines.isEmpty()) return

        val profiles = player.profileData.profiles
        val headerBotY = y + DD_H + PADDING

        Renderer.formattedText("§f${player.name}", x + PADDING, y + (DD_H - TITLE_SIZE) / 2f, TITLE_SIZE)
        Renderer.line(x, headerBotY, x + w, headerBotY, 1f, Theme.separator)

        val ddW = (w * 0.38f).coerceAtLeast(120f)
        dropdown.setBounds(x + w - ddW, y, ddW, DD_H)
        dropdown.draw(ctx,
            profileLabel(profile.cuteName),
            modeIcon(profile.gameMode),
            profiles.map { Triple(profileLabel(it.cuteName), modeIcon(it.gameMode), it.cuteName == PVState.profileName) }
        )

        fakePlayer?.let { entity ->
            val playerW = w * 0.28f
            val playerX = x + w - playerW - PADDING
            val playerY = headerBotY + PADDING
            val playerH = y + h - playerY
            val eyesX = (ctx.mouseX - playerX).toFloat()
            val eyesY = (ctx.mouseY - playerY).toFloat()
            ctx.entity(entity, playerX, playerY, playerW, playerH, eyesX, eyesY)
        }

        TextBox(lines, maxSize = 24f).also {
            it.setBounds(x + PADDING, headerBotY + PADDING, w - PADDING * 2f - w * 0.28f - PADDING, h - (headerBotY + PADDING - y) - PADDING)
            it.draw(ctx)
        }
    }

    override fun onOpen() { dropdown.reset() }
}