package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.toFixed
import com.odtheking.odinaddon.pvgui.PADDING
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.components.DropDown
import com.odtheking.odinaddon.pvgui.components.TextBox
import com.odtheking.odinaddon.pvgui.components.TextBoxLine
import com.odtheking.odinaddon.pvgui.components.asText
import com.odtheking.odinaddon.pvgui.components.withItem
import com.odtheking.odinaddon.pvgui.core.Component
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
import tech.thatgravyboat.skyblockapi.helpers.McClient
import java.net.URI
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
    private fun modeSuffix(mode: String?) = modeIcon(mode)?.let { " $it" } ?: ""

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
            profileLabel(profile.cuteName) + modeSuffix(profile.gameMode),
            modeIcon(profile.gameMode),
            profiles.map { Triple(profileLabel(it.cuteName) + modeSuffix(it.gameMode), modeIcon(it.gameMode), it.cuteName == PVState.profileName) }
        )

        val avatarSize = 40f
        val avatarX = x + w - avatarSize - PADDING
        val avatarY = headerBotY + PADDING

        val avatar = object : Component() {
            override fun draw(ctx: RenderContext) {
                ctx.register(this)
                Renderer.rect(avatarX, avatarY, avatarSize, avatarSize, 0x40FFFFFF, avatarSize * 0.15f)
                val hov = ctx.isHovered(avatarX, avatarY, avatarSize, avatarSize)
                Renderer.hollowRect(avatarX - 2f, avatarY - 2f, avatarSize + 4f, avatarSize + 4f,
                    if (hov) 2.5f else 1.5f,
                    if (hov) Theme.accent else Colors.WHITE.rgba,
                    avatarSize * 0.15f + 2f)
                ctx.item(PVState.headItem(player.uuid), avatarX, avatarY, avatarSize, showTooltip = false, showStackSize = false)
            }
            override fun click(ctx: RenderContext, mouseX: Double, mouseY: Double): Boolean {
                if (!ctx.isHovered(avatarX, avatarY, avatarSize, avatarSize)) return false
                McClient.openUri(URI("https://namemc.com/profile/${player.name}"))
                return true
            }
        }
        avatar.setBounds(avatarX, avatarY, avatarSize, avatarSize)
        avatar.draw(ctx)

        TextBox(lines, maxSize = 24f).also {
            it.setBounds(x + PADDING, headerBotY + PADDING, w - PADDING * 2f - avatarSize - PADDING, h - (headerBotY + PADDING - y) - PADDING)
            it.draw(ctx)
        }
    }

    override fun onOpen() { dropdown.reset() }
}