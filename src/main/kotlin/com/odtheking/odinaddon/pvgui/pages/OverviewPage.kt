package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.toFixed
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.PageHandler
import com.odtheking.odinaddon.pvgui.PVLayout
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.utils.DropDown
import com.odtheking.odinaddon.pvgui.utils.LevelUtils
import com.odtheking.odinaddon.pvgui.utils.LevelUtils.cataLevel
import com.odtheking.odinaddon.pvgui.utils.TextBox
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.activeDisplay
import com.odtheking.odinaddon.pvgui.utils.colorize
import com.odtheking.odinaddon.pvgui.utils.colorizeNumber
import com.odtheking.odinaddon.pvgui.utils.commas
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import com.odtheking.odinaddon.pvgui.utils.without
import tech.thatgravyboat.skyblockapi.api.remote.RepoItemsAPI
import tech.thatgravyboat.skyblockapi.helpers.McClient
import kotlin.math.floor

object OverviewPage : PageHandler {
    override val name = "Overview"
    private const val TITLE_SIZE = 26f
    private const val PADDING = 10f

    private val dropdown = DropDown()

    fun resetDropdown() = dropdown.reset()

    private val cachedLines: List<String> by resettableLazy {
        val member = PVState.memberData() ?: return@resettableLazy emptyList()
        val mmComps = member.dungeons.dungeonTypes.mastermode.tierComps.without("total").values.sum()
        val floorComps = member.dungeons.dungeonTypes.catacombs.tierComps.without("total").values.sum()
        val totalRuns = (mmComps + floorComps).toDouble()
        val avgSecrets = if (totalRuns > 0) member.dungeons.secrets / totalRuns else 0.0
        val level = floor(member.leveling.experience / 100.0).toInt()
        val cata = member.dungeons.dungeonTypes.cataLevel
        val skillAvgCap = LevelUtils.cappedSkillAverage(member.playerData)
        val skillAvgRaw = LevelUtils.skillAverage(member.playerData)
        val secrets = member.dungeons.secrets
        val magicPower = member.assumedMagicalPower
        listOf(
            "Level§7: §a$level",
            "§4Cata Level§7: ${cata.colorize(50.0)}",
            "§6Skill Average§7: ${skillAvgCap.colorize(55.0)} §7(${skillAvgRaw.toFixed(2)})",
            "§bSecrets§7: ${secrets.colorizeNumber(100000)}${secrets.commas} §7(${avgSecrets.colorize(15.0)}§7)",
            "Magical Power§7: ${magicPower.toDouble().colorize(1900.0, 0)}",
            member.pets.activeDisplay,
        )
    }

    private val cachedActivePetHeldItem: String? by resettableLazy {
        PVState.memberData()?.pets?.pets?.firstOrNull { it.active }?.heldItem
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
        val data = PVState.playerData ?: return
        val profile = PVState.selectedProfile() ?: return
        val profiles = data.profileData.profiles

        val ddW = (w * 0.38f).coerceAtLeast(120f)
        val ddX = x + w - ddW

        ctx.formattedText("§f${data.name}", x + PADDING, y + (dropdown.rowH - TITLE_SIZE) / 2f, TITLE_SIZE)

        val headerBottom = y + dropdown.rowH + PADDING  // ← was PADDING / 2f, now full PADDING for more breathing room
        ctx.line(x, headerBottom, x + w, headerBottom, 1f, Theme.separator)

        val avatarSize = 40f
        drawSkinHead(ctx, x + w - avatarSize - PADDING, headerBottom + PADDING, avatarSize, mouseX, mouseY)

        val statsTop = headerBottom + PADDING
        val statsH = h - (statsTop - y) - PADDING
        val statsW = w - PADDING * 2f - avatarSize - PADDING

        TextBox(ctx = ctx, x = x + PADDING, y = statsTop, w = statsW, h = statsH,
            title = null, titleScale = 0f, lines = cachedLines, scale = 24f).draw()

        drawPetHeldItem(ctx, statsTop, statsH)

        val entries = profiles.map { prof ->
            Triple(profileLabel(prof.cuteName), profileIcon(prof.gameMode), prof.cuteName == PVState.profileName)
        }
        dropdown.draw(ctx, ddX, y, ddW, profileLabel(profile.cuteName), profileIcon(profile.gameMode), entries, mouseX, mouseY)
    }

    private fun drawPetHeldItem(ctx: DrawContext, statsTop: Float, statsH: Float) {
        val heldId = cachedActivePetHeldItem ?: return
        val lineSpacing = statsH / cachedLines.size
        val iconSize = minOf(24f, lineSpacing * 0.65f)
        val petLineY = statsTop + (cachedLines.size - 1) * lineSpacing + (lineSpacing - iconSize) / 2f
        val textEndX = PADDING + ctx.formattedTextWidth(cachedLines.last(), iconSize)
        val slotPad = 2f
        val slotX = textEndX + 4f
        ctx.rect(slotX, petLineY, iconSize + slotPad * 2f, iconSize + slotPad * 2f, Color(255, 255, 255, 0.08f), 3f)
        ctx.item(RepoItemsAPI.getItem(heldId), slotX + slotPad, petLineY + slotPad, iconSize)
    }

    override fun onClick(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        val data = PVState.playerData ?: return
        val profiles = data.profileData.profiles
        val ddW = (PVLayout.MAIN_W * 0.38f).coerceAtLeast(120f)
        val ddX = PVLayout.MAIN_X + PVLayout.MAIN_W - ddW

        val avatarSize = 40f
        val avatarX = PVLayout.MAIN_X + PVLayout.MAIN_W - avatarSize - PADDING
        val avatarY = PVLayout.MAIN_Y + dropdown.rowH + PADDING / 2f + PADDING
        if (ctx.isHovered(mouseX, mouseY, avatarX, avatarY, avatarSize, avatarSize)) {
            McClient.openUri(java.net.URI("https://namemc.com/profile/${data.name}"))
            return
        }

        if (dropdown.isClickOnButton(ctx, mouseX, mouseY, ddX, PVLayout.MAIN_Y, ddW)) {
            dropdown.toggle()
            return
        }

        val idx = dropdown.indexAtClick(ctx, mouseX, mouseY, ddX, PVLayout.MAIN_Y, ddW)
        if (idx >= 0) {
            PVState.profileName = profiles.getOrNull(idx)?.cuteName
            PVState.invalidateCache()
            dropdown.close()
            return
        }
        if (dropdown.isOpen) dropdown.close()
    }

    override fun onOpen() = dropdown.reset()

    private fun drawSkinHead(ctx: DrawContext, x: Float, y: Float, size: Float, mouseX: Double, mouseY: Double) {
        val data = PVState.playerData ?: return
        val isHovered = ctx.isHovered(mouseX, mouseY, x, y, size, size)
        ctx.rect(x, y, size, size, Color(255, 255, 255, 0.35f), size * 0.15f)
        NVGRenderer.hollowRect(x - 2f, y - 2f, size + 4f, size + 4f,
            if (isHovered) 2.5f else 1.5f,
            if (isHovered) Theme.accent.rgba else Colors.WHITE.rgba,
            size * 0.15f + 2f)
        ctx.item(PVState.getPlayerHeadItem(data.uuid), x, y, size, showTooltip = false, showStackSize = false)
    }
}