package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.toFixed
import com.odtheking.odinaddon.pvgui.utils.LevelUtils
import com.odtheking.odinaddon.pvgui.utils.LevelUtils.cataLevel
import com.odtheking.odinaddon.pvgui.utils.Utils
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.PageHandler
import com.odtheking.odinaddon.pvgui.PVLayout
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import tech.thatgravyboat.skyblockapi.api.remote.RepoItemsAPI
import kotlin.math.floor

object OverviewPage : PageHandler {
    private const val TITLE_SIZE = 24f
    private const val TEXT_SIZE = 16f
    private const val PADDING = 10f
    private const val BTN_H = 28f
    private const val BTN_SPACING = 6f
    private const val PET_SIZE = 32f

    private val BTN_RADIUS get() = ProfileViewerModule.buttonRoundness

    override fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, mouseX: Double, mouseY: Double) {
        val data = PVState.playerData ?: return
        val profile = PVState.selectedProfile() ?: return
        val member = PVState.memberData() ?: return

        val title = "§f${data.name} §7- §a${profile.cuteName ?: "Unknown"}"
        ctx.formattedText(title, x + (w - ctx.formattedTextWidth(title, TITLE_SIZE)) / 2f, y + PADDING, TITLE_SIZE)

        val titleBottom = y + PADDING + TITLE_SIZE + 6f
        ctx.line(x, titleBottom, x + w, titleBottom, 1f, Color(255, 255, 255, 0.15f))

        val profiles = data.profileData.profiles
        val btnY = y + h - BTN_H - PADDING / 2f
        if (profiles.size > 1) {
            val btnW = (w - PADDING * 2f - BTN_SPACING * (profiles.size - 1)) / profiles.size
            profiles.forEachIndexed { i, prof ->
                val bx = x + PADDING + i * (btnW + BTN_SPACING)
                val isSelected = prof.cuteName == PVState.profileName
                val isHovered = ctx.isHovered(mouseX, mouseY, bx, btnY, btnW, BTN_H)
                ctx.rect(bx, btnY, btnW, BTN_H, when {
                    isSelected -> ProfileViewerModule.buttonColor
                    isHovered -> Color(255, 255, 255, 0.12f)
                    else -> Color(255, 255, 255, 0.06f)
                }, BTN_RADIUS)
                val label = when (prof.gameMode?.lowercase()) {
                    "ironman" -> "§7☢ §7${prof.cuteName ?: "?"}"
                    "bingo" -> "§e☆ §e${prof.cuteName ?: "?"}"
                    "island" -> "§b◎ §b${prof.cuteName ?: "?"}"
                    else -> "§f${prof.cuteName ?: "?"}"
                }
                val lw = ctx.formattedTextWidth(label, TEXT_SIZE)
                ctx.formattedText(label, bx + (btnW - lw) / 2f, btnY + (BTN_H - TEXT_SIZE) / 2f, TEXT_SIZE)
            }
        }

        val mmComps = member.dungeons.dungeonTypes.mastermode.tierComps.filter { it.key != "total" }.values.sum()
        val floorComps = member.dungeons.dungeonTypes.catacombs.tierComps.filter { it.key != "total" }.values.sum()
        val totalRuns = (mmComps + floorComps).toDouble()
        val avgSecrets = if (totalRuns > 0) member.dungeons.secrets / totalRuns else 0.0
        val level = floor(member.leveling.experience / 100.0).toInt()
        val cata = member.dungeons.dungeonTypes.cataLevel
        val skillAvgCap = LevelUtils.cappedSkillAverage(member.playerData)
        val skillAvgRaw = LevelUtils.skillAverage(member.playerData)
        val secrets = member.dungeons.secrets
        val magicPower = member.assumedMagicalPower
        val activePet = member.pets.pets.firstOrNull { it.active }

        val lines = listOf(
            "§bSkyBlock Level§7: ${Utils.colorizeNumber(level.toLong(), 500)}$level",
            "§4Cata Level§7: ${Utils.colorize(cata, 50.0)}${cata.toFixed(2)}",
            "§6Skill Average§7: ${Utils.colorize(skillAvgCap, 55.0)}${skillAvgCap.toFixed(1)} §7(${skillAvgRaw.toFixed(2)})",
            "§bSecrets§7: ${Utils.colorizeNumber(secrets, 100000)}${Utils.commas(secrets)} §7(${Utils.colorize(avgSecrets, 15.0)}${avgSecrets.toFixed(2)}§7)",
            "§5Magical Power§7: ${Utils.colorize(magicPower.toDouble(), 1900.0)}$magicPower",
            "§6Active Pet§7: ${Utils.getActivePetDisplay(member.pets)}",
        )

        val statsTop = titleBottom + PADDING
        val statsH = btnY - statsTop - PADDING

        ctx.textList(lines, x + PADDING, statsTop, w - PADDING * 2f - PET_SIZE - PADDING, statsH, maxSize = 22f)
        activePet?.heldItem.let { heldId ->
            val lineSpacing = statsH / lines.size
            val size = minOf(22f, lineSpacing * 0.65f)
            val petLineY = statsTop + (lines.size - 1) * lineSpacing + (lineSpacing - size) / 2f
            val textEndX = x + PADDING + ctx.formattedTextWidth(lines.last(), size)
            val icon = heldId?.let { RepoItemsAPI.getItem(it) } ?: ItemStack(Items.BARRIER)
            ctx.text("(", textEndX + 2f, petLineY + (size - size) / 2f, size, Color(170, 170, 170))
            ctx.item(icon, textEndX + ctx.textWidth("(", size) + 4f, petLineY, size)
            ctx.text(")", textEndX + ctx.textWidth("(", size) + 4f + size + 2f, petLineY + (size - size) / 2f, size, Color(170, 170, 170))
        }
    }

    override fun onClick(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        val data = PVState.playerData ?: return
        val profiles = data.profileData.profiles
        if (profiles.size <= 1) return

        val btnY = PVLayout.MAIN_Y + PVLayout.MAIN_H - BTN_H - PADDING / 2f
        val btnW = (PVLayout.MAIN_W - PADDING * 2f - BTN_SPACING * (profiles.size - 1)) / profiles.size

        profiles.forEachIndexed { i, prof ->
            val bx = PVLayout.MAIN_X + PADDING + i * (btnW + BTN_SPACING)
            if (ctx.isHovered(mouseX, mouseY, bx, btnY, btnW, BTN_H)) {
                PVState.profileName = prof.cuteName
            }
        }
    }
}