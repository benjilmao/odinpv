package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.capitalizeFirst
import com.odtheking.odinaddon.pvgui.utils.apiutils.HypixelData
import com.odtheking.odin.utils.toFixed
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.core.PVPage
import com.odtheking.odinaddon.pvgui.core.Theme
import com.odtheking.odinaddon.pvgui.utils.*
import com.odtheking.odinaddon.pvgui.utils.Utils.without
import com.odtheking.odinaddon.pvgui.utils.apiutils.LevelUtils.cataLevel
import com.odtheking.odinaddon.pvgui.utils.apiutils.LevelUtils.classAverage
import com.odtheking.odinaddon.pvgui.utils.apiutils.LevelUtils.classLevel
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.floor

object Dungeons : PVPage("Dungeons") {

    private val mmComps: Float by resettableLazy {
        (profile?.dungeons?.dungeonTypes?.mastermode?.tierComps?.without("total"))?.values?.sum() ?: 0f
    }
    private val floorComps: Float by resettableLazy {
        (profile?.dungeons?.dungeonTypes?.catacombs?.tierComps?.without("total"))?.values?.sum() ?: 0f
    }

    private val text: List<String> by resettableLazy {
        val secrets = profile?.dungeons?.secrets ?: 0
        val avgSecrets = if ((mmComps + floorComps) > 0)
            (secrets.toDouble() / (mmComps + floorComps).toDouble()) else 0.0
        val bloodKills = profile?.playerStats?.bloodMobKills ?: 0
        val hasSpirit = profile?.pets?.pets?.any { it.type == "SPIRIT" && it.tier == "LEGENDARY" } == true

        listOf(
            "§bSecrets§7: ${Utils.colorizeNumber(secrets, 100000)}${Utils.commas(secrets)}",
            "§dAverage Secret Count§7: ${Utils.colorize(avgSecrets, 15.0)}${avgSecrets.toFixed(2)}",
            "§cBlood Mob Kills§7: ${Utils.commas(bloodKills.toLong())}",
            "§7Sprite Pet: ${if (hasSpirit) "§l§2Found!" else "§l§4Missing!"}",
        )
    }

    private val cataText by resettableLazy {
        val cata = profile?.dungeons?.dungeonTypes?.cataLevel ?: 0.0
        "§4Cata Level§7: ${Utils.colorize(cata, 50.0)}${cata.toFixed(2)}"
    }

    private val classTextList: List<String> by resettableLazy {
        val classOrder = listOf("berserk", "archer", "mage", "tank", "healer")
        classOrder.mapNotNull { className ->
            profile?.dungeons?.classes?.get(className)?.let { classData ->
                val level = classData.classLevel
                "${getClassColor(className)}${className.capitalizeFirst()}§7: ${Utils.colorize(level, 50.0)}${level.toFixed(2)}"
            }
        }
    }

    private val classAverageText: String by resettableLazy {
        val avg = profile?.dungeons?.classAverage ?: 0.0
        "§6Class Average§7: ${Utils.colorize(avg, 50.0)}${avg.toFixed(2)}"
    }

    private val selectedClass: String by resettableLazy {
        val selected = profile?.dungeons?.selectedClass?.capitalizeFirst() ?: "None"
        val color = getClassColor(profile?.dungeons?.selectedClass)
        "§7Selected Class§7: $color$selected"
    }
    private val classEntryText: List<String> by resettableLazy { listOf(selectedClass) + classTextList }

    private val floorData: List<String> by resettableLazy {
        val catacombs = profile?.dungeons?.dungeonTypes?.catacombs
        (0..7).map { floor ->
            "§3${catacombs?.floorStats(floor.toString()) ?: "Floor $floor: §cDNF"}"
        }
    }
    private val mmData: List<String> by resettableLazy {
        val mastermode = profile?.dungeons?.dungeonTypes?.mastermode
        (1..7).map { floor ->
            "§cMM ${mastermode?.floorStats(floor.toString()) ?: "MM Floor $floor: §cDNF"}"
        }
    }

    private val quadrantWidth = (mainWidth - spacer) / 2f
    private val quadrantHeight = (mainHeight - spacer) / 2f
    private val backgroundHeight = quadrantHeight

    private val mainBox by resettableLazy {
        TextBox(
            Box(
                mainX + spacer,
                2 * spacer,
                quadrantWidth - 2 * spacer,
                backgroundHeight - 2 * spacer
            ),
            cataText, 4f, text, 2.5f, spacer.toFloat(), Colors.WHITE.rgba
        )
    }

    private val classBox by resettableLazy {
        TextBox(
            Box(
                mainX + spacer,
                3 * spacer + backgroundHeight,
                quadrantWidth - 2 * spacer,
                backgroundHeight - 2 * spacer
            ),
            classAverageText, 3.5f, classEntryText, 2.5f, spacer.toFloat(), Colors.WHITE.rgba
        )
    }

    private val floorBox by resettableLazy {
        TextBox(
            Box(
                mainX + 2 * spacer + quadrantWidth,
                2 * spacer,
                quadrantWidth - 2 * spacer,
                backgroundHeight - 2 * spacer
            ),
            null, 0f, floorData, 2.5f, spacer.toFloat(), Colors.WHITE.rgba
        )
    }

    private val mmBox by resettableLazy {
        TextBox(
            Box(
                mainX + 2 * spacer + quadrantWidth,
                3 * spacer + backgroundHeight,
                quadrantWidth - 2 * spacer,
                backgroundHeight - 2 * spacer
            ),
            null, 0f, mmData, 2.3f, spacer.toFloat(), Colors.WHITE.rgba
        )
    }

    override fun draw(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // If profile is null, don't draw anything (or draw placeholder)
        if (profile == null) return

        NVGRenderer.rect(mainX.toFloat(), spacer.toFloat(), quadrantWidth, backgroundHeight, Theme.secondaryBg.rgba, Theme.roundness)
        NVGRenderer.rect(
            (mainX + quadrantWidth + spacer), (spacer + backgroundHeight + spacer),
            quadrantWidth, backgroundHeight, Theme.secondaryBg.rgba, Theme.roundness
        )
        NVGRenderer.rect(mainX.toFloat(), (spacer + backgroundHeight + spacer), quadrantWidth, backgroundHeight, Theme.secondaryBg.rgba, Theme.roundness)
        NVGRenderer.rect(
            (mainX + quadrantWidth + spacer), spacer.toFloat(),
            quadrantWidth, backgroundHeight, Theme.secondaryBg.rgba, Theme.roundness
        )

        mainBox.draw()
        classBox.draw()
        floorBox.draw()
        mmBox.draw()
    }

    private fun HypixelData.DungeonTypeData.floorStats(floor: String): String {
        val comps = this.tierComps[floor]?.toLong()
        val fastest = this.fastestTimes[floor]?.toDouble()
        val fastestS = this.fastestTimeS[floor]?.toDouble()
        val fastestSPlus = this.fastestTimeSPlus[floor]?.toDouble()

        return "${if (floor == "0") "Entrance" else "Floor $floor"}: §f${comps?.let { Utils.commas(it) } ?: "§cDNF"} " +
                "§7| §f${fastest?.let { formatTime(it) } ?: "§cDNF"} " +
                "§7| §f${fastestS?.let { formatTime(it) } ?: "§cDNF"} " +
                "§7| §a${fastestSPlus?.let { formatTime(it) } ?: "§cDNF"}"
    }

    private fun formatTime(millis: Double): String {
        val seconds = millis / 1000.0
        val minutes = floor(seconds / 60).toInt()
        val secs = floor(seconds % 60).toInt()
        return String.format("%d:%02d", minutes, secs)
    }

    private fun getClassColor(className: String?): String = when (className?.lowercase()) {
        "berserk" -> "§c"
        "archer" -> "§6"
        "mage" -> "§b"
        "tank" -> "§2"
        "healer" -> "§d"
        else -> "§7"
    }

    fun setPlayer(player: HypixelData.PlayerInfo) {}

    override fun click(mouseX: Int, mouseY: Int, mouseButton: Int) {}
}