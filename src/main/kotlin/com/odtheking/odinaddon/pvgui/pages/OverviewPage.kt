package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.toFixed
import com.odtheking.odinaddon.pvgui2.utils.LevelUtils
import com.odtheking.odinaddon.pvgui2.utils.LevelUtils.cataLevel
import com.odtheking.odinaddon.pvgui2.utils.Utils
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.PageHandler
import com.odtheking.odinaddon.pvgui.PVLayout
import com.odtheking.odinaddon.pvgui.PVState
import kotlin.math.floor

object OverviewPage : PageHandler {

    private val COL_PANEL_BG  = Color(255, 255, 255, 0.05f)
    private val COL_SEPARATOR = Color(255, 255, 255, 0.15f)
    private val COL_BTN_BG    = Color(255, 255, 255, 0.10f)
    private val COL_BTN_SEL   = Color(26, 74, 138)

    private const val TEXT_SIZE   = 16f
    private const val TITLE_SIZE  = 24f
    private const val PADDING     = 10f
    private const val PANEL_RADIUS = 6f

    override fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, mouseX: Double, mouseY: Double) {
        val data    = PVState.playerData ?: return
        val profile = PVState.selectedProfile() ?: return
        val member  = PVState.memberData() ?: return

        val profileName = profile.cuteName ?: "Unknown"
        val title = "§f${data.name} §7- §a$profileName"
        val titleW = ctx.formattedTextWidth(title, TITLE_SIZE)
        ctx.formattedText(title, x + (w - titleW) / 2f, y + PADDING, TITLE_SIZE)

        val titleBottom = y + PADDING + TITLE_SIZE + 6f
        ctx.line(x, titleBottom, x + w, titleBottom, 1f, COL_SEPARATOR)

        val mmComps    = member.dungeons.dungeonTypes.mastermode.tierComps.filter { it.key != "total" }.values.sum()
        val floorComps = member.dungeons.dungeonTypes.catacombs.tierComps.filter { it.key != "total" }.values.sum()
        val totalRuns  = (mmComps + floorComps).toDouble()
        val avgSecrets = if (totalRuns > 0) member.dungeons.secrets / totalRuns else 0.0

        val level       = floor(member.leveling.experience / 100.0).toInt()
        val cata        = member.dungeons.dungeonTypes.cataLevel
        val skillAvgCap = LevelUtils.cappedSkillAverage(member.playerData)
        val skillAvgRaw = LevelUtils.skillAverage(member.playerData)
        val secrets     = member.dungeons.secrets
        val magicPower  = member.assumedMagicalPower
        val activePet   = Utils.getActivePetDisplay(member.pets)

        val lines = listOf(
            "§bSkyBlock Level§7: ${Utils.colorizeNumber(level.toLong(), 500)}$level",
            "§4Cata Level§7: ${Utils.colorize(cata, 50.0)}${cata.toFixed(2)}",
            "§6Skill Average§7: ${Utils.colorize(skillAvgCap, 55.0)}${skillAvgCap.toFixed(1)} §7(${skillAvgRaw.toFixed(2)})",
            "§bSecrets§7: ${Utils.colorizeNumber(secrets, 100000)}${Utils.commas(secrets)} §7(${Utils.colorize(avgSecrets, 15.0)}${avgSecrets.toFixed(2)}§7)",
            "§5Magical Power§7: ${Utils.colorize(magicPower.toDouble(), 1900.0)}$magicPower",
            "§6Active Pet§7: $activePet",
        )

        val statsTop  = titleBottom + PADDING
        val panelW    = w - PADDING * 2f
        val panelH    = h - (titleBottom - y) - PADDING * 2f - 40f

        ctx.rect(x + PADDING, statsTop, panelW, panelH, COL_PANEL_BG, PANEL_RADIUS)
        ctx.textList(lines, x + PADDING * 2f, statsTop, panelW - PADDING * 2f, panelH, maxSize = 24f)

        val profiles = data.profileData.profiles
        if (profiles.size > 1) {
            val btnY    = y + h - 28f
            val btnH    = 22f
            val spacing = 6f
            val totalW  = w - PADDING * 2f
            val btnW    = (totalW - spacing * (profiles.size - 1)) / profiles.size

            profiles.forEachIndexed { i, prof ->
                val bx         = x + PADDING + i * (btnW + spacing)
                val isSelected = prof.cuteName == PVState.profileName
                val isHovered  = ctx.isHovered(mouseX, mouseY, bx, btnY, btnW, btnH)

                ctx.rect(bx, btnY, btnW, btnH, when {
                    isSelected -> COL_BTN_SEL
                    isHovered  -> Color(255, 255, 255, 0.18f)
                    else       -> COL_BTN_BG
                }, 5f)

                val label = when (prof.gameMode?.lowercase()) {
                    "ironman" -> "§7☢ §7${prof.cuteName ?: "?"}"
                    "bingo"   -> "§e☆ §e${prof.cuteName ?: "?"}"
                    "island"  -> "§b◎ §b${prof.cuteName ?: "?"}"
                    else      -> "§f${prof.cuteName ?: "?"}"
                }
                val lw = ctx.formattedTextWidth(label, TEXT_SIZE)
                ctx.formattedText(label, bx + (btnW - lw) / 2f, btnY + (btnH - TEXT_SIZE) / 2f, TEXT_SIZE)
            }
        }
    }

    override fun onClick(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        val data     = PVState.playerData ?: return
        val profiles = data.profileData.profiles
        if (profiles.size <= 1) return

        val btnY    = PVLayout.MAIN_Y + PVLayout.MAIN_H - 28f
        val btnH    = 22f
        val spacing = 6f
        val totalW  = PVLayout.MAIN_W - PADDING * 2f
        val btnW    = (totalW - spacing * (profiles.size - 1)) / profiles.size

        profiles.forEachIndexed { i, prof ->
            val bx = PVLayout.MAIN_X + PADDING + i * (btnW + spacing)
            if (ctx.isHovered(mouseX, mouseY, bx, btnY, btnW, btnH)) {
                PVState.profileName = prof.cuteName
            }
        }
    }
}