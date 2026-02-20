package com.odtheking.odinaddon.pvgui2.pages

import com.odtheking.odin.utils.toFixed
import com.odtheking.odinaddon.pvgui2.utils.Utils
import com.odtheking.odinaddon.pvgui2.utils.Utils.without
import com.odtheking.odinaddon.pvgui2.utils.LevelUtils
import com.odtheking.odinaddon.pvgui2.utils.profileOrSelected
import com.odtheking.odinaddon.pvgui2.TestGui
import com.odtheking.odinaddon.pvgui2.Theme
import com.odtheking.odinaddon.pvgui2.textList
import com.odtheking.odinaddon.pvgui2.utils.LevelUtils.cataLevel
import com.odtheking.odinaddon.pvgui2.withRoundedBackground
import me.owdding.lib.builder.LayoutFactory
import me.owdding.lib.displays.Display
import me.owdding.lib.displays.Displays
import me.owdding.lib.displays.asButtonLeft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import kotlin.math.floor

object OverviewPage {

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

        val profile = data.profileOrSelected(screen.profileName)?.members?.get(data.uuid)
            ?: run {
                LayoutFactory.vertical(0) {
                    display(Displays.center(mainWidth, mainHeight,
                        Displays.text("§cNo profile data found.", shadow = true)
                    ))
                }.apply { setPosition(mainX, mainY) }.visitWidgets { addWidget(it) }
                return
            }

        val selectedProfileName = data.profileData.profiles
            .find { it.cuteName == screen.profileName }?.cuteName ?: "Unknown"

        val mmComps = profile.dungeons.dungeonTypes.mastermode.tierComps.without("total").values.sum()
        val floorComps = profile.dungeons.dungeonTypes.catacombs.tierComps.without("total").values.sum()
        val totalRuns = (mmComps + floorComps).toDouble()
        val avgSecrets = if (totalRuns > 0) profile.dungeons.secrets / totalRuns else 0.0
        val level = floor(profile.leveling.experience / 100.0).toInt()
        val cata = profile.dungeons.dungeonTypes.cataLevel
        val skillAvgCapped = LevelUtils.cappedSkillAverage(profile.playerData)
        val skillAvgRaw = LevelUtils.skillAverage(profile.playerData)
        val secrets = profile.dungeons.secrets
        val magicPower = profile.assumedMagicalPower
        val activePet = Utils.getActivePetDisplay(profile.pets)
        val profiles = data.profileData.profiles
        val profileBtnW = (mainWidth - spacer * (profiles.size - 1)) / profiles.size.coerceAtLeast(1)

        val dataLines = listOf(
            "§bLevel§7: ${Utils.colorizeNumber(level.toLong(), 500)}$level",
            "§4Cata Level§7: ${Utils.colorize(cata, 50.0)}${cata.toFixed(2)}",
            "§6Skill Average§7: ${Utils.colorize(skillAvgCapped, 55.0)}${skillAvgCapped.toFixed(1)} §7(${skillAvgRaw.toFixed(2)})",
            "§bSecrets§7: ${Utils.colorizeNumber(secrets, 100000)}${Utils.commas(secrets)} §7(${Utils.colorize(avgSecrets, 15.0)}${avgSecrets.toFixed(2)}§7)",
            "§5Magical Power§7: ${Utils.colorize(magicPower.toDouble(), 1900.0)}$magicPower",
            "§6Active Pet§7: $activePet",
        )

        val titleHeight = 20
        val contentHeight = mainHeight - titleHeight - spacer * 3 - 14

        LayoutFactory.vertical(spacer) {
            display(Displays.center(mainWidth, titleHeight,
                Displays.text("§f${data.name} §7- §a$selectedProfileName", shadow = true)
            ))
            display(object : Display {
                override fun getWidth() = mainWidth
                override fun getHeight() = 1
                override fun render(graphics: GuiGraphics) {
                    graphics.fill(0, 0, mainWidth, 1, Theme.separator)
                }
            })
            display(textList(dataLines, mainWidth - spacer * 2, contentHeight, scale = 1.5f))
        }.apply {
            setPosition(mainX + spacer, mainY)
        }.visitWidgets { addWidget(it) }

        LayoutFactory.horizontal(spacer) {
            profiles.forEach { prof ->
                val isSelected = prof.cuteName == screen.profileName
                val label = prof.cuteName ?: "Unknown"
                val btn = Displays.center(profileBtnW, 14,
                    Displays.text(label, shadow = true, color = {
                        if (isSelected) TextColor.WHITE.toUInt() else 0xFFAAAAAA.toUInt()
                    })
                ).withRoundedBackground(if (isSelected) Theme.buttonSelect else Theme.buttonBg, Theme.btnRound)
                    .asButtonLeft {
                        screen.profileName = prof.cuteName
                        screen.init()
                    }.also { it.withTexture(null) }
                widget(btn)
            }
        }.apply { setPosition(mainX, mainY + mainHeight - 14) }.visitWidgets { addWidget(it) }

        val nameMCBtn = Displays.center(50, 14,
            Displays.text("§9NameMC", shadow = true)
        ).withRoundedBackground(Theme.buttonBg, Theme.btnRound)
            .asButtonLeft {
                McClient.openUri(
                    java.net.URI("https://namemc.com/profile/${data.name}")
                )
            }

        LayoutFactory.vertical(0) {
            widget(nameMCBtn)
        }.apply { setPosition(mainX + mainWidth - 50, mainY) }.visitWidgets { addWidget(it) }
    }
}