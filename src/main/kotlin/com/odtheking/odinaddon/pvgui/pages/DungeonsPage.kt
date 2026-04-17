package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.capitalizeFirst
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.CONTENT_X
import com.odtheking.odinaddon.pvgui.CONTENT_Y
import com.odtheking.odinaddon.pvgui.MAIN_H
import com.odtheking.odinaddon.pvgui.MAIN_W
import com.odtheking.odinaddon.pvgui.PAD
import com.odtheking.odinaddon.pvgui.QUAD_W
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.dsl.TextBox
import com.odtheking.odinaddon.pvgui.dsl.textBox
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
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.floor

object DungeonsPage : PVPage() {
    override val name = "Dungeons"

    private val bgH = (MAIN_H / 2f) - (PAD / 2f)

    private val mmComps: Float by resettableLazy {
        PVState.member()?.dungeons?.dungeonTypes?.mastermode?.tierComps?.without("total")?.values?.sum() ?: 0f
    }
    private val floorComps: Float by resettableLazy {
        PVState.member()?.dungeons?.dungeonTypes?.catacombs?.tierComps?.without("total")?.values?.sum() ?: 0f
    }

    private val cataTitle: String by resettableLazy {
        val d = PVState.member() ?: return@resettableLazy ""
        "§4Cata Level§7: ${d.dungeons.dungeonTypes.cataLevel.colorize(50.0)}"
    }

    private val mainLines: List<String> by resettableLazy {
        val d = PVState.member() ?: return@resettableLazy emptyList()
        val total = (mmComps + floorComps).toDouble().coerceAtLeast(1.0)
        listOf(
            "§bSecrets§7: ${d.dungeons.secrets.colorizeNumber(100_000)}${d.dungeons.secrets.commas}",
            "§dAverage Secret Count§7: ${(d.dungeons.secrets.toDouble() / total).colorize(15.0)}",
            "§cBlood Mob Kills§7: ${d.playerStats.bloodMobKills.toLong().commas}",
            "§7Spirit Pet§7: ${if (d.pets.pets.any { it.type == "SPIRIT" && it.tier == "LEGENDARY" }) "§l§2Found!" else "§l§4Missing!"}",
        )
    }

    private val classTitle: String by resettableLazy {
        val d = PVState.member() ?: return@resettableLazy ""
        "§6Class Average§7: ${d.dungeons.classAverage.colorize(50.0)}"
    }

    private val classLines: List<String> by resettableLazy {
        val d = PVState.member() ?: return@resettableLazy emptyList()
        val sel = d.dungeons.selectedClass?.capitalizeFirst() ?: "None"
        listOf("§aSelected Class§7: ${sel.colorClass}") +
                d.dungeons.classes.entries.map { (n, cd) ->
                    "${n.capitalizeFirst().colorClass}§7: ${cd.classLevel.colorize(50.0)}"
                }
    }

    private val floorLines: List<String> by resettableLazy {
        val c = PVState.member()?.dungeons?.dungeonTypes?.catacombs ?: return@resettableLazy emptyList()
        (0..7).map { "§3${c.floorStats(it.toString())}" }
    }

    private val mmLines: List<String> by resettableLazy {
        val m = PVState.member()?.dungeons?.dungeonTypes?.mastermode ?: return@resettableLazy emptyList()
        (1..7).map { "§cMM ${m.floorStats(it.toString())}" }
    }

    private val mainBox: TextBox by resettableLazy {
        textBox(
            CONTENT_X + PAD, CONTENT_Y + PAD,
            QUAD_W - PAD * 2f, bgH - PAD * 2f,
            title = cataTitle, titleScale = 4f,
            lines = mainLines, scale = 2.5f, spacer = PAD,
            color = Theme.textPrimary,
        )
    }

    private val classBox: TextBox by resettableLazy {
        textBox(
            CONTENT_X + PAD, CONTENT_Y + PAD + bgH + PAD,
            QUAD_W - PAD * 2f, bgH - PAD * 2f,
            title = classTitle, titleScale = 3.5f,
            lines = classLines, scale = 2.5f, spacer = PAD,
            color = Theme.textPrimary,
        )
    }

    private val floorBox: TextBox by resettableLazy {
        textBox(
            CONTENT_X + PAD + QUAD_W + PAD, CONTENT_Y + PAD,
            QUAD_W - PAD * 2f, bgH - PAD * 2f,
            lines = floorLines, scale = 2.3f, spacer = PAD,
            color = Theme.textPrimary,
        )
    }

    private val mmBox: TextBox by resettableLazy {
        textBox(
            CONTENT_X + PAD + QUAD_W + PAD, CONTENT_Y + PAD + bgH + PAD,
            QUAD_W - PAD * 2f, bgH - PAD * 2f,
            lines = mmLines, scale = 2.3f, spacer = PAD,
            color = Theme.textPrimary,
        )
    }

    override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        NVGRenderer.rect(CONTENT_X, CONTENT_Y, QUAD_W, bgH, Theme.slotBg, Theme.radius)
        NVGRenderer.rect(CONTENT_X + PAD + QUAD_W, CONTENT_Y + bgH + PAD, QUAD_W, bgH, Theme.slotBg, Theme.radius)
        NVGRenderer.rect(CONTENT_X, CONTENT_Y + bgH + PAD, QUAD_W, bgH, Theme.slotBg, Theme.radius)
        NVGRenderer.rect(CONTENT_X + PAD + QUAD_W, CONTENT_Y, QUAD_W, bgH, Theme.slotBg, Theme.radius)
        mainBox.draw()
        classBox.draw()
        floorBox.draw()
        mmBox.draw()
    }

    private fun HypixelData.DungeonTypeData.floorStats(floor: String): String {
        val label = if (floor == "0") "Entrance" else "Floor $floor"
        val comps = tierComps[floor]?.toLong()?.commas ?: "§cDNF"
        val time = fastestTimes[floor]?.let { msToMinSec(it.toDouble()) } ?: "§cDNF"
        val timeS = fastestTimeS[floor]?.let { msToMinSec(it) } ?: "§cDNF"
        val timeSP = fastestTimeSPlus[floor]?.let { "§a${msToMinSec(it)}" } ?: "§cDNF"
        return "$label§7: §f$comps §7| §f$time §7| §f$timeS §7| $timeSP"
    }

    private fun msToMinSec(ms: Number): String {
        val totalSeconds = ms.toDouble() / 1000.0
        val mins = floor(totalSeconds / 60.0).toInt()
        val secs = (totalSeconds % 60.0).toInt()
        return "%d:%02d".format(mins, secs)
    }
}