package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.capitalizeFirst
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.dsl.TextBox
import com.odtheking.odinaddon.pvgui.utils.LevelUtils.cataLevel
import com.odtheking.odinaddon.pvgui.utils.LevelUtils.classAverage
import com.odtheking.odinaddon.pvgui.utils.LevelUtils.classLevel
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import com.odtheking.odinaddon.pvgui.utils.colorClass
import com.odtheking.odinaddon.pvgui.utils.colorize
import com.odtheking.odinaddon.pvgui.utils.colorizeNumber
import com.odtheking.odinaddon.pvgui.utils.commas
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import com.odtheking.odinaddon.pvgui.utils.without

object DungeonsPage : PVPage() {
    override val name = "Dungeons"

    private val SP     get() = 12f
    private val panW   get() = w / 2f - SP / 2f
    private val panH   get() = h / 2f - SP / 2f
    private val rightX get() = x + panW + SP
    private val botY   get() = y + panH + SP

    private val cataTitle: String by resettableLazy {
        val data = PVState.member() ?: return@resettableLazy ""
        "§4Cata Level§7: ${data.dungeons.dungeonTypes.cataLevel.colorize(50.0)}"
    }

    private val mainLines: List<String> by resettableLazy {
        val data       = PVState.member() ?: return@resettableLazy emptyList()
        val mmComps    = data.dungeons.dungeonTypes.mastermode.tierComps.without("total").values.sum()
        val floorComps = data.dungeons.dungeonTypes.catacombs.tierComps.without("total").values.sum()
        val totalRuns  = (mmComps + floorComps).toDouble().takeIf { it > 0.0 } ?: 1.0
        listOf(
            "§bSecrets§7: ${data.dungeons.secrets.colorizeNumber(100_000)}${data.dungeons.secrets.commas}",
            "§dAverage Secret Count§7: ${(data.dungeons.secrets.toDouble() / totalRuns).colorize(15.0)}",
            "§cBlood Mob Kills§7: ${data.playerStats.bloodMobKills.toLong().commas}",
            "§7Spirit Pet§7: ${if (data.pets.pets.any { it.type == "SPIRIT" && it.tier == "LEGENDARY" }) "§l§2Found!" else "§l§4Missing!"}",
        )
    }

    private val classTitle: String by resettableLazy {
        val data = PVState.member() ?: return@resettableLazy ""
        "§6Class Average§7: ${data.dungeons.classAverage.colorize(50.0)}"
    }

    private val classLines: List<String> by resettableLazy {
        val data     = PVState.member() ?: return@resettableLazy emptyList()
        val selected = data.dungeons.selectedClass?.capitalizeFirst() ?: "None"
        listOf("§aSelected Class§7: ${selected.colorClass}") +
                data.dungeons.classes.entries.map { (name, classData) ->
                    "${name.capitalizeFirst().colorClass}§7: ${classData.classLevel.colorize(50.0)}"
                }
    }

    private val floorLines: List<String> by resettableLazy {
        val cata = PVState.member()?.dungeons?.dungeonTypes?.catacombs ?: return@resettableLazy emptyList()
        (0..7).map { "§3${cata.floorStats(it.toString())}" }
    }

    private val mmLines: List<String> by resettableLazy {
        val mm = PVState.member()?.dungeons?.dungeonTypes?.mastermode ?: return@resettableLazy emptyList()
        (1..7).map { "§cMM ${mm.floorStats(it.toString())}" }
    }

    override fun draw() {
        NVGRenderer.rect(x, y, panW, panH, Theme.panel, Theme.radius)
        TextBox(x = x + SP, y = y, w = panW - SP * 2f, h = panH,
            lines = mainLines, textSize = 20f, title = cataTitle, titleSize = 30f).draw()

        NVGRenderer.rect(rightX, y, panW, panH, Theme.panel, Theme.radius)
        TextBox(x = rightX + SP, y = y, w = panW - SP * 2f, h = panH,
            lines = floorLines, textSize = 18f).draw()

        NVGRenderer.rect(x, botY, panW, panH, Theme.panel, Theme.radius)
        TextBox(x = x + SP, y = botY, w = panW - SP * 2f, h = panH,
            lines = classLines, textSize = 20f, title = classTitle, titleSize = 30f).draw()

        NVGRenderer.rect(rightX, botY, panW, panH, Theme.panel, Theme.radius)
        TextBox(x = rightX + SP, y = botY, w = panW - SP * 2f, h = panH,
            lines = mmLines, textSize = 18f).draw()
    }

    private fun HypixelData.DungeonTypeData.floorStats(floor: String): String {
        val label  = if (floor == "0") "Entrance" else "Floor $floor"
        val comps  = tierComps[floor]?.toLong()?.commas ?: "§cDNF"
        val time   = fastestTimes[floor]?.toDouble()?.let { msToTime(it) } ?: "§cDNF"
        val timeS  = fastestTimeS[floor]?.let { msToTime(it) } ?: "§cDNF"
        val timeSP = fastestTimeSPlus[floor]?.let { "§a${msToTime(it)}" } ?: "§cDNF"
        return "$label§7: §f$comps §7| §f$time §7| §f$timeS §7| $timeSP"
    }

    private fun msToTime(ms: Double): String {
        val s = (ms / 1000).toInt()
        return if (s >= 60) "${s / 60}m ${s % 60}s" else "${s}s"
    }
}
