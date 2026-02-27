package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.capitalizeFirst
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.utils.LevelUtils.cataLevel
import com.odtheking.odinaddon.pvgui.utils.LevelUtils.classAverage
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import com.odtheking.odinaddon.pvgui.utils.TextBox
import com.odtheking.odinaddon.pvgui.utils.LevelUtils.classLevel
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.colorize
import com.odtheking.odinaddon.pvgui.utils.commas
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import com.odtheking.odinaddon.pvgui.utils.colorClass
import com.odtheking.odinaddon.pvgui.utils.colorizeNumber
import com.odtheking.odinaddon.pvgui.utils.without

object DungeonsPage : PVPage("Dungeons") {
    private const val PADDING = 10f
    private const val GAP = 10f

    private val cachedMainLines: List<String> by resettableLazy {
        val data = member ?: return@resettableLazy emptyList()
        val mmComps = data.dungeons.dungeonTypes.mastermode.tierComps.without("total").values.sum()
        val floorComps = data.dungeons.dungeonTypes.catacombs.tierComps.without("total").values.sum()
        val totalRuns = (mmComps + floorComps).toDouble()
        val avgSecrets = if (totalRuns > 0) data.dungeons.secrets / totalRuns else 0.0
        listOf(
            "§bSecrets§7: ${data.dungeons.secrets.colorizeNumber(100000)}",
            "§dAvg Secrets§7: ${avgSecrets.colorize(15.0)}",
            "§cBlood Kills§7: ${data.playerStats.bloodMobKills.toLong().commas}",
            "§7Spirit Pet§7: ${if (data.pets.pets.any { it.type == "SPIRIT" && it.tier == "LEGENDARY" }) "§l§2Found!" else "§l§4Missing!"}",
        )
    }

    private val cachedClassLines: List<String> by resettableLazy {
        val data = member ?: return@resettableLazy emptyList()
        val selected = data.dungeons.selectedClass?.capitalizeFirst() ?: "None"
        listOf("§aSelected§7: ${selected.lowercase().colorClass}") +
                listOf("berserk", "archer", "mage", "tank", "healer").mapNotNull { cls ->
                    data.dungeons.classes[cls]?.let {
                        "${cls.capitalizeFirst().colorClass}§7: ${it.classLevel.colorize(50.0)}"
                    }
                }
    }

    private val cachedFloorLines: List<String> by resettableLazy {
        val catacombs = member?.dungeons?.dungeonTypes?.catacombs ?: return@resettableLazy emptyList()
        (0..7).mapNotNull { floor -> catacombs.floorStats(floor.toString(), "§3") }
            .ifEmpty { listOf("§7No floor data") }
    }

    private val cachedMmLines: List<String> by resettableLazy {
        val mm = member?.dungeons?.dungeonTypes?.mastermode ?: return@resettableLazy emptyList()
        (1..7).mapNotNull { floor -> mm.floorStats(floor.toString(), "§cMM ") }
            .ifEmpty { listOf("§7No master mode data") }
    }

    override fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, mouseX: Double, mouseY: Double) {
        if (cachedMainLines.isEmpty()) return
        val data = member ?: return

        val leftW = w * 0.50f - GAP / 2f
        val rightW = w - leftW - GAP
        val halfH = h / 2f - GAP / 2f
        val rightX = x + leftW + GAP

        val mainTitle  = "§4Cata Level§7: ${data.dungeons.dungeonTypes.cataLevel.colorize(50.0)}"
        val classTitle = "§6Class Average§7: ${data.dungeons.classAverage.colorize(50.0)}"

        TextBox(ctx, x + PADDING, y, leftW - PADDING * 2f, halfH, mainTitle, 36f, cachedMainLines, 22f).draw()
        TextBox(ctx, x + PADDING, y + halfH + GAP, leftW - PADDING * 2f, halfH, classTitle, 32f, cachedClassLines, 22f).draw()
        TextBox(ctx, rightX + PADDING, y, rightW - PADDING * 2f, halfH, null, 0f, cachedFloorLines, 20f).draw()
        TextBox(ctx, rightX + PADDING, y + halfH + GAP, rightW - PADDING * 2f, halfH, null, 0f, cachedMmLines, 20f).draw()

        val midX = x + leftW + GAP / 2f
        val midY = y + halfH + GAP / 2f
        ctx.line(midX, y + 4f, midX, y + h - 4f, 1f, Theme.separator)
        ctx.line(x + 4f, midY, x + w - 4f, midY, 1f, Theme.separator)
    }

    private fun HypixelData.DungeonTypeData.floorStats(floor: String, color: String): String? {
        val comps = tierComps[floor]?.toLong() ?: return null
        val label = if (floor == "0") "Entrance" else "Floor $floor"
        val fastest = fastestTimes[floor]?.toDouble()
        val fastestS = fastestTimeS[floor]
        val fastestSPlus = fastestTimeSPlus[floor]
        return buildString {
            append("${color}$label§7: §f${comps.commas}")
            append(" §7| ${fastest?.let { "§f${formatTime(it)}" } ?: "§cDNF"}")
            append(" §7| ${fastestS?.let { "§f${formatTime(it)}" } ?: "§cDNF"}")
            append(" §7| ${fastestSPlus?.let { "§a${formatTime(it)}" } ?: "§cDNF"}")
        }
    }

    private fun formatTime(millis: Double): String {
        val total = (millis / 1000.0).toLong()
        val m = total / 60
        val s = total % 60
        return if (m > 0) "${m}m${s}s" else "${s}s"
    }
}