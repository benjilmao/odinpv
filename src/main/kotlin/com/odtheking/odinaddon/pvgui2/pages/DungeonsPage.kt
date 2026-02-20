package com.odtheking.odinaddon.pvgui2.pages

import com.odtheking.odin.utils.capitalizeFirst
import com.odtheking.odin.utils.toFixed
import com.odtheking.odinaddon.pvgui2.utils.Utils
import com.odtheking.odinaddon.pvgui2.utils.Utils.without
import com.odtheking.odinaddon.pvgui2.utils.HypixelData
import com.odtheking.odinaddon.pvgui2.utils.LevelUtils.cataLevel
import com.odtheking.odinaddon.pvgui2.utils.LevelUtils.classAverage
import com.odtheking.odinaddon.pvgui2.utils.LevelUtils.classLevel
import com.odtheking.odinaddon.pvgui2.utils.profileOrSelected
import com.odtheking.odinaddon.pvgui2.Theme
import com.odtheking.odinaddon.pvgui2.TestGui
import com.odtheking.odinaddon.pvgui2.textList
import me.owdding.lib.builder.LayoutFactory
import me.owdding.lib.displays.Display
import me.owdding.lib.displays.Displays
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import kotlin.math.floor

object DungeonsPage {

    fun build(screen: TestGui, addWidget: (AbstractWidget) -> Unit) {
        val spacer = screen.spacer
        val mainX = screen.mainX
        val mainY = screen.mainY
        val mainWidth = screen.mainWidth
        val mainHeight = screen.mainHeight

        val data = screen.playerData
        if (data == null) {
            LayoutFactory.vertical(0) {
                display(Displays.center(mainWidth, mainHeight,
                    Displays.text(screen.loadText, shadow = true)
                ))
            }.apply { setPosition(mainX, mainY) }.visitWidgets { addWidget(it) }
            return
        }

        val profile = data.profileOrSelected(screen.profileName)?.members?.get(data.uuid) ?: return

        val halfW = mainWidth / 2 - spacer * 2
        val halfH = mainHeight / 2
        val crossGap = 10

        val mmComps = profile.dungeons.dungeonTypes.mastermode.tierComps.without("total").values.sum()
        val floorComps = profile.dungeons.dungeonTypes.catacombs.tierComps.without("total").values.sum()
        val totalRuns = (mmComps + floorComps).toDouble()
        val avgSecrets = if (totalRuns > 0) profile.dungeons.secrets / totalRuns else 0.0
        val bloodKills = profile.playerStats.bloodMobKills
        val hasSpirit = profile.pets.pets.any { it.type == "SPIRIT" && it.tier == "LEGENDARY" }
        val cata = profile.dungeons.dungeonTypes.cataLevel

        val mainLines = listOf(
            "§4Cata Level§7: ${Utils.colorize(cata, 50.0)}${cata.toFixed(2)}",
            "§bSecrets§7: ${Utils.colorizeNumber(profile.dungeons.secrets, 100000)}${Utils.commas(profile.dungeons.secrets)}",
            "§dAvg Secrets§7: ${Utils.colorize(avgSecrets, 15.0)}${avgSecrets.toFixed(2)}",
            "§cBlood Kills§7: ${Utils.commas(bloodKills.toLong())}",
            "§7Spirit Pet§7: ${if (hasSpirit) "§l§2Found!" else "§l§4Missing!"}",
        )

        val classOrder = listOf("berserk", "archer", "mage", "tank", "healer")
        val classAvg = profile.dungeons.classAverage
        val selectedClass = profile.dungeons.selectedClass?.capitalizeFirst() ?: "None"
        val selectedColor = getClassColor(profile.dungeons.selectedClass)
        val classLines = listOf(
            "§6Class Avg§7: ${Utils.colorize(classAvg, 50.0)}${classAvg.toFixed(2)}",
            "§7Selected§7: $selectedColor$selectedClass",
        ) + classOrder.mapNotNull { className ->
            profile.dungeons.classes[className]?.let { classData ->
                val level = classData.classLevel
                "${getClassColor(className)}${className.capitalizeFirst()}§7: ${Utils.colorize(level, 50.0)}${level.toFixed(2)}"
            }
        }

        val floorLines = (0..7).map { floor ->
            "§3${profile.dungeons.dungeonTypes.catacombs.floorStats(floor.toString()) ?: "Floor $floor: §cDNF"}"
        }

        val mmLines = (1..7).map { floor ->
            "§c${profile.dungeons.dungeonTypes.mastermode.floorStats(floor.toString())?.let { "MM $it" } ?: "MM Floor $floor: §cDNF"}"
        }

        LayoutFactory.vertical(0) {
            display(textList(mainLines, halfW - spacer, halfH - spacer * 2, scale = 1.4f))
        }.apply { setPosition(mainX + spacer, mainY + spacer) }.visitWidgets { addWidget(it) }

        LayoutFactory.vertical(0) {
            display(textList(classLines, halfW - spacer, halfH - spacer * 2, scale = 1.4f))
        }.apply { setPosition(mainX + spacer, mainY + halfH + spacer * 2) }.visitWidgets { addWidget(it) }

        LayoutFactory.vertical(0) {
            display(textList(floorLines, halfW - spacer * 3, halfH - spacer * 2, scale = 1.4f))
        }.apply { setPosition(mainX + halfW + spacer * 2, mainY + spacer) }.visitWidgets { addWidget(it) }

        LayoutFactory.vertical(0) {
            display(textList(mmLines, halfW - spacer * 3, halfH - spacer * 2, scale = 1.4f))
        }.apply { setPosition(mainX + halfW + spacer * 2, mainY + halfH + spacer * 2) }.visitWidgets { addWidget(it) }

        LayoutFactory.vertical(0) {
            display(object : Display {
                override fun getWidth() = mainWidth
                override fun getHeight() = mainHeight
                override fun render(graphics: GuiGraphics) {
                    val midX = halfW + spacer
                    val midY = halfH + spacer
                    graphics.fill(midX, crossGap, midX + 1, midY - crossGap, Theme.separator)
                    graphics.fill(midX, midY + crossGap, midX + 1, mainHeight - crossGap, Theme.separator)
                    graphics.fill(crossGap, midY, midX - crossGap, midY + 1, Theme.separator)
                    graphics.fill(midX + crossGap, midY, mainWidth - crossGap, midY + 1, Theme.separator)
                }
            })
        }.apply { setPosition(mainX, mainY) }.visitWidgets { addWidget(it) }
    }

    private fun HypixelData.DungeonTypeData.floorStats(floor: String): String? {
        val comps = tierComps[floor]?.toLong() ?: return null
        val fastest = fastestTimes[floor]?.toDouble()
        val fastestS = fastestTimeS[floor]?.toDouble()
        val fastestSPlus = fastestTimeSPlus[floor]?.toDouble()
        val label = if (floor == "0") "Entrance" else "Floor $floor"
        return "$label: §f${Utils.commas(comps)} §7| §f${fastest?.let { formatTime(it) } ?: "§cDNF"} §7| §f${fastestS?.let { formatTime(it) } ?: "§cDNF"} §7| §a${fastestSPlus?.let { formatTime(it) } ?: "§cDNF"}"
    }

    private fun formatTime(millis: Double): String {
        val seconds = millis / 1000.0
        val minutes = floor(seconds / 60).toInt()
        val secs = floor(seconds % 60).toInt()
        return String.format("%d:%02d", minutes, secs)
    }

    private fun getClassColor(className: String?): String = when (className?.lowercase()) {
        "berserk" -> "§c"
        "archer"  -> "§6"
        "mage"    -> "§b"
        "tank"    -> "§2"
        "healer"  -> "§d"
        else      -> "§7"
    }
}