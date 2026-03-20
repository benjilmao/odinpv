package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.capitalizeFirst
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.dsl.TextBox
import com.odtheking.odinaddon.pvgui.utils.LevelUtils
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
import net.minecraft.client.gui.GuiGraphics

object DungeonsPage : PVPage() {
    override val name = "Dungeons"

    private val spacing = 12f
    private val panelW get() = w / 2f - spacing / 2f
    private val panelH get() = h / 2f - spacing / 2f
    private val rightX get() = x + panelW + spacing
    private val botY get() = y + panelH + spacing

    private val cataTitle: String by resettableLazy {
        val data = PVState.member() ?: return@resettableLazy ""
        "§4Cata Level§7: ${data.dungeons.dungeonTypes.cataLevel.colorize(50.0)}"
    }

    private val mainLines: List<String> by resettableLazy {
        val data = PVState.member() ?: return@resettableLazy emptyList()
        listOf(
            "§bSecrets§7: ${data.dungeons.secrets.colorizeNumber(100_000)}${data.dungeons.secrets.commas}",
            "§dAverage Secret Count§7: ${data.dungeons.avrSecrets.colorize(15.0)}",
            "§cBlood Mob Kills§7: ${data.playerStats.bloodMobKills.toLong().commas}",
            "§7Spirit Pet§7: ${if (data.pets.pets.any { it.type == "SPIRIT" && it.tier == "LEGENDARY" }) "§l§2Found!" else "§l§4Missing!"}",
        )
    }

    private val classTitle: String by resettableLazy {
        val data = PVState.member() ?: return@resettableLazy ""
        "§6Class Average§7: ${data.dungeons.classAverage.colorize(50.0)}"
    }

    private val classLines: List<String> by resettableLazy {
        val data = PVState.member() ?: return@resettableLazy emptyList()
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
        val mastermode = PVState.member()?.dungeons?.dungeonTypes?.mastermode ?: return@resettableLazy emptyList()
        (1..7).map { "§c${mastermode.floorStats(it.toString())}" }
    }

    override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        TextBox(x = x, y = y, w = panelW, h = panelH,
            lines = mainLines, textSize = 20f,
            title = cataTitle, titleSize = 30f,
            background = Theme.slotBg).draw()

        TextBox(x = rightX, y = y, w = panelW, h = panelH,
            lines = floorLines, textSize = 18f,
            background = Theme.slotBg).draw()

        TextBox(x = x, y = botY, w = panelW, h = panelH,
            lines = classLines, textSize = 20f,
            title = classTitle, titleSize = 30f,
            background = Theme.slotBg).draw()

        TextBox(x = rightX, y = botY, w = panelW, h = panelH,
            lines = mmLines, textSize = 18f,
            background = Theme.slotBg).draw()
    }

    private fun HypixelData.DungeonTypeData.floorStats(floor: String): String {
        val label = if (floor == "0") "Entrance" else "Floor $floor"
        val completions = tierComps[floor]?.toLong()?.commas ?: "§cDNF"
        val time = fastestTimes[floor]?.toDouble()?.let { msToTime(it) } ?: "§cDNF"
        val timeS = fastestTimeS[floor]?.let { msToTime(it) } ?: "§cDNF"
        val timeSPlus = fastestTimeSPlus[floor]?.let { "§a${msToTime(it)}" } ?: "§cDNF"
        return "$label§7: §f$completions §7| §f$time §7| §f$timeS §7| $timeSPlus"
    }

    private fun msToTime(ms: Double): String {
        val seconds = (ms / 1000).toInt()
        return if (seconds >= 60) "${seconds / 60}m ${seconds % 60}s" else "${seconds}s"
    }
}