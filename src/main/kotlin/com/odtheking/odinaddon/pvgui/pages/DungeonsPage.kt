package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.capitalizeFirst
import com.odtheking.odin.utils.toFixed
import com.odtheking.odinaddon.pvgui2.utils.HypixelData
import com.odtheking.odinaddon.pvgui2.utils.LevelUtils.cataLevel
import com.odtheking.odinaddon.pvgui2.utils.LevelUtils.classAverage
import com.odtheking.odinaddon.pvgui2.utils.LevelUtils.classLevel
import com.odtheking.odinaddon.pvgui2.utils.Utils
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.PageHandler
import com.odtheking.odinaddon.pvgui.PVState
import kotlin.math.floor

object DungeonsPage : PageHandler {

    private val COL_PANEL_BG  = Color(255, 255, 255, 0.05f)
    private val COL_SEPARATOR = Color(255, 255, 255, 0.15f)

    private const val PADDING      = 10f
    private const val PANEL_RADIUS = 6f
    private const val GAP          = 10f

    override fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, mouseX: Double, mouseY: Double) {
        val member = PVState.memberData() ?: return

        val halfW = w / 2f - GAP / 2f
        val halfH = h / 2f - GAP / 2f

        val mmComps    = member.dungeons.dungeonTypes.mastermode.tierComps.filter { it.key != "total" }.values.sum()
        val floorComps = member.dungeons.dungeonTypes.catacombs.tierComps.filter { it.key != "total" }.values.sum()
        val totalRuns  = (mmComps + floorComps).toDouble()
        val avgSecrets = if (totalRuns > 0) member.dungeons.secrets / totalRuns else 0.0
        val cata       = member.dungeons.dungeonTypes.cataLevel
        val classAvg   = member.dungeons.classAverage
        val selected   = member.dungeons.selectedClass?.capitalizeFirst() ?: "None"

        val mainLines = listOf(
            "§4Cata§7: ${Utils.colorize(cata, 50.0)}${cata.toFixed(2)}",
            "§bSecrets§7: ${Utils.colorizeNumber(member.dungeons.secrets, 100000)}${Utils.commas(member.dungeons.secrets)}",
            "§dAvg Secrets§7: ${Utils.colorize(avgSecrets, 15.0)}${avgSecrets.toFixed(2)}",
            "§cBlood Kills§7: ${Utils.commas(member.playerStats.bloodMobKills.toLong())}",
            "§7Sprite Pet§7: ${if (member.pets.pets.any { it.type == "SPIRIT" && it.tier == "LEGENDARY" }) "§aFound" else "§cMissing"}",
        )

        val classLines = listOf(
            "§6Class Avg§7: ${Utils.colorize(classAvg, 50.0)}${classAvg.toFixed(2)}",
            "§7Selected§7: $selected",
        ) + listOf("berserk", "archer", "mage", "tank", "healer").mapNotNull { cls ->
            member.dungeons.classes[cls]?.let {
                val lvl = it.classLevel
                "${getClassColor(cls)}${cls.capitalizeFirst()}§7: ${Utils.colorize(lvl, 50.0)}${lvl.toFixed(2)}"
            }
        }

        val floorLines = (0..7).mapNotNull { floor ->
            member.dungeons.dungeonTypes.catacombs.floorStats(floor.toString())
        }.ifEmpty { listOf("§7No floor data") }

        val mmLines = (1..7).mapNotNull { floor ->
            member.dungeons.dungeonTypes.mastermode.floorStats(floor.toString())?.let { "§4MM $it" }
        }.ifEmpty { listOf("§7No master mode data") }

        ctx.textList(mainLines, x + PADDING, y, halfW - PADDING * 2f, halfH, maxSize = 22f)
        ctx.textList(classLines, x + PADDING, y + halfH + GAP, halfW - PADDING * 2f, halfH, maxSize = 22f)
        ctx.textList(floorLines, x + halfW + GAP + PADDING, y, halfW - PADDING * 2f, halfH, maxSize = 20f)
        ctx.textList(mmLines, x + halfW + GAP + PADDING, y + halfH + GAP, halfW - PADDING * 2f, halfH, maxSize = 20f)

        val midX = x + halfW + GAP / 2f
        val midY = y + halfH + GAP / 2f
        ctx.line(midX, y + 4f, midX, y + h - 4f, 1f, COL_SEPARATOR)
        ctx.line(x + 4f, midY, x + w - 4f, midY, 1f, COL_SEPARATOR)
    }

    private fun getClassColor(cls: String): String = when (cls.lowercase()) {
        "berserk" -> "§c"
        "archer"  -> "§6"
        "mage"    -> "§b"
        "tank"    -> "§2"
        "healer"  -> "§d"
        else      -> "§7"
    }

    private fun HypixelData.DungeonTypeData.floorStats(floor: String): String? {
        val comps = tierComps[floor]?.toLong() ?: return null
        val label = if (floor == "0") "Entrance" else "Floor $floor"
        val fastest     = fastestTimes[floor]?.toDouble()
        val fastestS    = fastestTimeS[floor]
        val fastestSPlus = fastestTimeSPlus[floor]
        return buildString {
            append("§3$label§7: §f${Utils.commas(comps)}")
            append(" §7| ${fastest?.let { "§f${formatTime(it)}" } ?: "§cDNF"}")
            append(" §7| ${fastestS?.let { "§f${formatTime(it)}" } ?: "§cDNF"}")
            append(" §7| ${fastestSPlus?.let { "§a${formatTime(it)}" } ?: "§cDNF"}")
        }
    }

    private fun formatTime(millis: Double): String {
        val secs    = millis / 1000.0
        val minutes = floor(secs / 60).toInt()
        val seconds = floor(secs % 60).toInt()
        return "%d:%02d".format(minutes, seconds)
    }
}