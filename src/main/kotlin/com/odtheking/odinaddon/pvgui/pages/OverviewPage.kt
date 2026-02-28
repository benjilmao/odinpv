package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.toFixed
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.components.DropDown
import com.odtheking.odinaddon.pvgui.components.ItemSlot
import com.odtheking.odinaddon.pvgui.components.TextBox
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
import kotlin.math.floor

object OverviewPage : PVPage("Overview") {
    private const val TITLE_SIZE = 26f

    private val dropdown = DropDown {
        onSelect { idx ->
            val profiles = player?.profileData?.profiles ?: return@onSelect
            PVState.profileName = profiles.getOrNull(idx)?.cuteName
            PVState.invalidateCache()
        }
    }

    fun resetDropdown() = dropdown.reset()

    private val cachedLines: List<String> by resettableLazy {
        val data = member ?: return@resettableLazy emptyList()
        listOf(
            "Level§7: §a${floor(data.leveling.experience / 100.0).toInt()}",
            "§4Cata Level§7: ${data.dungeons.dungeonTypes.cataLevel.colorize(50.0)}",
            "§6Skill Average§7: ${LevelUtils.cappedSkillAverage(data.playerData).colorize(55.0)} §7(${LevelUtils.skillAverage(data.playerData).toFixed(2)})",
            "§bSecrets§7: ${data.dungeons.secrets.colorizeNumber(100000)}${data.dungeons.secrets.commas} §7(${data.dungeons.avrSecrets.colorize(15.0)}§7)",
            "Magical Power§7: ${data.assumedMagicalPower.toDouble().colorize(1900.0, 0)}",
            data.pets.activeDisplay,
        )
    }

    private fun profileLabel(cuteName: String?) = "§f${cuteName ?: "?"}"

    private fun profileIcon(gameMode: String?): Pair<String, String>? = when (gameMode?.lowercase()) {
        "ironman" -> "§7" to "♲"
        "stranded" -> "§a" to "☀"
        "bingo" -> "§7" to "Ⓑ"
        else -> null
    }

    override fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, mouseX: Double, mouseY: Double) {
        if (cachedLines.isEmpty()) return
        val data = player ?: return
        val currentProfile = profile ?: return
        val profiles = data.profileData.profiles

        val ddW = (w * 0.38f).coerceAtLeast(120f)
        val ddX = x + w - ddW

        ctx.formattedText("§f${data.name}", x + padding, y + (dropdown.rowHeight - TITLE_SIZE) / 2f, TITLE_SIZE)
        val headerBottom = y + dropdown.rowHeight + padding
        ctx.line(x, headerBottom, x + w, headerBottom, 1f, Theme.separator)

        val avatarSize = 40f
        drawSkinHead(ctx, x + w - avatarSize - padding, headerBottom + padding, avatarSize, mouseX, mouseY)

        val statsTop = headerBottom + padding
        val statsH = h - (statsTop - y) - padding
        val statsW = w - padding * 2f - avatarSize - padding
        val statsX = x + padding
        TextBox(statsX, statsTop, statsW, statsH, null, 0f, cachedLines, 24f).draw(ctx, mouseX, mouseY)
        drawPetHeldItem(ctx, mouseX, mouseY, statsX, statsTop, statsH)

        val entries = profiles.map { prof ->
            Triple(profileLabel(prof.cuteName), profileIcon(prof.gameMode), prof.cuteName == PVState.profileName)
        }
        dropdown.draw(ctx, mouseX, mouseY, ddX, y, ddW, profileLabel(currentProfile.cuteName), profileIcon(currentProfile.gameMode), entries)
    }

    private fun drawPetHeldItem(ctx: DrawContext, mouseX: Double, mouseY: Double, statsX: Float, statsTop: Float, statsH: Float) {
        val item = member?.pets?.activePet?.heldItemStack ?: return
        val spacing = statsH / cachedLines.size
        val iconSize = (spacing * 0.7f).coerceAtMost(32f)
        val lastLineY = statsTop + (cachedLines.size - 1) * spacing + (spacing - iconSize) / 2f
        val textW = ctx.formattedTextWidth(cachedLines.last(), iconSize)
        ItemSlot(statsX + textW + 8f, lastLineY, iconSize, item).draw(ctx, mouseX, mouseY)
    }

    override fun onClick(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        val data = player ?: return

        val avatarSize = 40f
        val avatarX = mainX + mainW - avatarSize - padding
        val avatarY = mainY + dropdown.rowHeight + padding / 2f + padding
        if (ctx.isHovered(mouseX, mouseY, avatarX, avatarY, avatarSize, avatarSize)) {
            McClient.openUri(java.net.URI("https://namemc.com/profile/${data.name}"))
            return
        }

        dropdown.click(ctx, mouseX, mouseY)
    }

    override fun onOpen() = dropdown.reset()

    private fun drawSkinHead(ctx: DrawContext, x: Float, y: Float, size: Float, mouseX: Double, mouseY: Double) {
        val data = player ?: return
        val isHovered = ctx.isHovered(mouseX, mouseY, x, y, size, size)
        ctx.rect(x, y, size, size, Color(255, 255, 255, 0.35f), size * 0.15f)
        NVGRenderer.hollowRect(
            x - 2f, y - 2f, size + 4f, size + 4f,
            if (isHovered) 2.5f else 1.5f,
            if (isHovered) Theme.accent.rgba else Colors.WHITE.rgba,
            size * 0.15f + 2f,
        )
        ctx.item(PVState.getPlayerHeadItem(data.uuid), x, y, size, showTooltip = false, showStackSize = false)
    }
}