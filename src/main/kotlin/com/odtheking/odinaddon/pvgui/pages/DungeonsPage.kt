package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.capitalizeFirst
import com.odtheking.odinaddon.pvgui.PADDING
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.components.TextBox
import com.odtheking.odinaddon.pvgui.components.TextBoxLine
import com.odtheking.odinaddon.pvgui.components.asText
import com.odtheking.odinaddon.pvgui.core.RenderContext
import com.odtheking.odinaddon.pvgui.core.Renderer
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

object DungeonsPage : PVPage() {
    override val name = "Dungeons"
    private const val GAP = 10f

    private val mainLines: List<TextBoxLine> by resettableLazy {
        val data = member ?: return@resettableLazy emptyList()
        listOf(
            "§bSecrets§7: ${data.dungeons.secrets.colorizeNumber(100000)}".asText(),
            "§dAvg Secrets§7: ${data.dungeons.avrSecrets.colorize(15.0)}".asText(),
            "§cBlood Kills§7: ${data.playerStats.bloodMobKills.toLong().commas}".asText(),
            "§7Spirit Pet§7: ${if (data.pets.pets.any { it.type == "SPIRIT" && it.tier == "LEGENDARY" }) "§l§2Found!" else "§l§4Missing!"}".asText(),
        )
    }

    private val classLines: List<TextBoxLine> by resettableLazy {
        val data = member ?: return@resettableLazy emptyList()
        val selected = data.dungeons.selectedClass?.capitalizeFirst() ?: "None"
        listOf("§aSelected§7: ${selected.colorClass}".asText()) +
                listOf("berserk", "archer", "mage", "tank", "healer").mapNotNull {
                cls -> data.dungeons.classes[cls]?.let {
                    "${cls.capitalizeFirst().colorClass}§7: ${it.classLevel.colorize(50.0)}".asText()
                }
            }
    }

    private val floorLines: List<TextBoxLine> by resettableLazy {
        val cata = member?.dungeons?.dungeonTypes?.catacombs ?: return@resettableLazy emptyList()
        (0..7).mapNotNull { floor -> cata.floorStats(floor.toString(), "§3")?.asText() }
            .ifEmpty { listOf("§7No floor data".asText()) }
    }

    private val mmLines: List<TextBoxLine> by resettableLazy {
        val mm = member?.dungeons?.dungeonTypes?.mastermode ?: return@resettableLazy emptyList()
        (1..7).mapNotNull { floor -> mm.floorStats(floor.toString(), "§cMM ")?.asText() }
            .ifEmpty { listOf("§7No master mode data".asText()) }
    }

    private val cataTitle by resettableLazy {
        "§4Cata Level§7: ${member?.dungeons?.dungeonTypes?.cataLevel?.colorize(50.0) ?: ""}"
    }

    private val classTitle by resettableLazy {
        "§6Class Average§7: ${member?.dungeons?.classAverage?.colorize(50.0) ?: ""}"
    }

    override fun draw(ctx: RenderContext) {
        if (mainLines.isEmpty()) return

        val leftW = w * 0.52f - GAP / 2f
        val rightW = w - leftW - GAP
        val halfH = h / 2f - GAP / 2f
        val rightX = x + leftW + GAP

        TextBox(mainLines, maxSize = 22f, title = cataTitle, titleScale = 32f).also {
            it.setBounds(x + PADDING, y, leftW - PADDING * 2f, halfH)
            it.draw(ctx)
        }

        TextBox(classLines, maxSize = 22f, title = classTitle, titleScale = 32f).also {
            it.setBounds(x + PADDING, y + halfH + GAP, leftW - PADDING * 2f, halfH)
            it.draw(ctx)
        }

        TextBox(floorLines, maxSize = 18f).also {
            it.setBounds(rightX + PADDING, y, rightW - PADDING * 2f, halfH)
            it.draw(ctx)
        }

        TextBox(mmLines, maxSize = 18f).also {
            it.setBounds(rightX + PADDING, y + halfH + GAP, rightW - PADDING * 2f, halfH)
            it.draw(ctx)
        }

        Renderer.line(x + leftW + GAP / 2f, y + 4f, x + leftW + GAP / 2f, y + h - 4f, 1f, Theme.separator)
        Renderer.line(x + 4f, y + halfH + GAP / 2f, x + w - 4f, y + halfH + GAP / 2f, 1f, Theme.separator)
    }

    private fun HypixelData.DungeonTypeData.floorStats(floor: String, color: String): String? {
        val comps = tierComps[floor]?.toLong() ?: return null
        val label = if (floor == "0") "Entrance" else "Floor $floor"
        return buildString {
            append("${color}$label§7: §f${comps.commas}")
            append(" §7| ${fastestTimes[floor]?.toDouble()?.let { "§f${msToTime(it)}" } ?: "§cDNF"}")
            append(" §7| ${fastestTimeS[floor]?.let { "§f${msToTime(it)}" } ?: "§cDNF"}")
            append(" §7| §a${fastestTimeSPlus[floor]?.let { msToTime(it) } ?: "§cDNF"}")
        }
    }

    private fun msToTime(ms: Double): String {
        val s = (ms / 1000).toInt()
        return if (s >= 60) "${s / 60}m ${s % 60}s" else "${s}s"
    }
}