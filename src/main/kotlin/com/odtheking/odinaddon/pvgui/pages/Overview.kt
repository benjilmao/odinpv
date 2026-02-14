package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.toFixed
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.core.PVGui
import com.odtheking.odinaddon.pvgui.core.PVPage
import com.odtheking.odinaddon.pvgui.core.Theme
import com.odtheking.odinaddon.pvgui.utils.*
import com.odtheking.odinaddon.pvgui.utils.Utils.without
import com.odtheking.odinaddon.pvgui.utils.apiutils.HypixelData
import com.odtheking.odinaddon.pvgui.utils.apiutils.LevelUtils
import com.odtheking.odinaddon.pvgui.utils.apiutils.LevelUtils.cataLevel
import com.odtheking.odinaddon.pvgui.utils.apiutils.profileList
import com.odtheking.odinaddon.pvgui.utils.apiutils.profileOrSelected
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.floor

object Overview : PVPage("Overview") {

    private val nameBox get() = Box(mainX, spacer, (mainWidth * 2 / 3), (mainHeight * 0.1).toInt())
    private val dropDownBox get() = Box(
        (mainX + nameBox.w + spacer).toInt(),
        spacer,
        (mainWidth - nameBox.w - spacer).toInt(),
        (mainHeight * 0.1).toInt()
    )
    private val dataBox get() = Box(
        mainX,
        (nameBox.y + nameBox.h + spacer).toInt(),
        nameBox.w,
        (mainHeight - nameBox.h - spacer).toInt()
    )
    private val playerBox get() = Box(
        (mainX + dataBox.w + spacer).toInt(),
        (dropDownBox.y + dropDownBox.h + spacer).toInt(),
        dropDownBox.w,
        dataBox.h
    )

    private val dropdown: DropDownDSL<String> by resettableLazy {
        val profiles = player.profileList
        if (PVGui.profileName == null && profiles.isNotEmpty()) {
            PVGui.profileName = profiles.first().first
        }

        val currentProfile = player.profileOrSelected(PVGui.profileName)
        val options = profiles.map { "§a${it.first}§r §8(§7${it.second}§8)" }
        val default = if (currentProfile != null) {
            "§a${currentProfile.cuteName ?: "Unknown"}§7 §8(§7${currentProfile.gameMode ?: "normal"}§8)"
        } else {
            "§7No profile"
        }

        dropDownMenu(
            box = dropDownBox,
            default = default,
            options = options,
            spacer = spacer,
            color = Theme.buttonBg.rgba,
            radius = Theme.roundness
        ) {
            displayText { it }
            selectedText { it }
            onSelect { selected ->
                PVGui.profileName = selected.substringAfter("§a").substringBefore("§r ")
                ResettableLazy.resetAll()
            }
            onExtend { }
        }
    }

    private val data: List<String> by resettableLazy {
        val member = profile ?: return@resettableLazy listOf("§7No profile selected")
        val mmComps = member.dungeons.dungeonTypes.mastermode.tierComps.without("total").values.sum()
        val floorComps = member.dungeons.dungeonTypes.catacombs.tierComps.without("total").values.sum()
        val totalRuns = (mmComps + floorComps).toDouble()
        val avgSecrets = if (totalRuns > 0) member.dungeons.secrets / totalRuns else 0.0

        val level = floor(member.leveling.experience / 100.0).toInt()

        listOf(
            "Level§7: ${Utils.colorizeNumber(level.toLong(), 500)}$level",
            "§4Cata Level§7: ${Utils.colorize(member.dungeons.dungeonTypes.cataLevel, 50.0)}${member.dungeons.dungeonTypes.cataLevel.toFixed(2)}",
            "§6Skill Average§7: ${Utils.colorize(LevelUtils.cappedSkillAverage(member.playerData), 55.0)}${LevelUtils.cappedSkillAverage(member.playerData).toFixed(1)} §7(${LevelUtils.skillAverage(member.playerData).toFixed(2)})",
            "§bSecrets§7: ${Utils.colorizeNumber(member.dungeons.secrets, 100000)}${Utils.commas(member.dungeons.secrets)} §7(${Utils.colorize(avgSecrets, 15.0)}${avgSecrets.toFixed(2)}§7)",
            "§5Magical Power§7: ${Utils.colorize(member.assumedMagicalPower.toDouble(), 1900.0)}${member.assumedMagicalPower}",
            "§6Active Pet§7: ${Utils.getActivePetDisplay(member.pets)}"
        )
    }

    private val textBox by resettableLazy {
        TextBox(
            box = Box(
                dataBox.x + spacer,
                dataBox.y + spacer,
                dataBox.w - 2 * spacer,
                dataBox.h - 2 * spacer
            ),
            title = null,
            text = data,
            textScale = 2.5f,
            spacer = spacer.toFloat(),
            defaultColor = Theme.fontColor.rgba
        )
    }

    override fun draw(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (player.uuid.isEmpty()) return // loading

        NVGRenderer.rect(nameBox.x, nameBox.y, nameBox.w, nameBox.h, Theme.secondaryBg.rgba, Theme.roundness)
        Text.fillText(
            text = "§6${player.name}",
            x = nameBox.centerX,
            y = nameBox.centerY,
            maxWidth = nameBox.w - 2 * spacer,
            baseScale = 4f,
            defaultColor = Colors.WHITE
        )
        dropdown.draw(mouseX, mouseY)

        NVGRenderer.rect(dataBox.x, dataBox.y, dataBox.w, dataBox.h, Theme.secondaryBg.rgba, Theme.roundness)
        textBox.draw()

        if (!dropdown.extended) {
            NVGRenderer.rect(playerBox.x, playerBox.y, playerBox.w, playerBox.h, Theme.secondaryBg.rgba, Theme.roundness)
            Text.drawColored(
                text = "§7Player",
                x = playerBox.centerX,
                y = playerBox.centerY,
                height = 9 * 2f,
                defaultColor = Colors.WHITE,
                centering = Text.Centering.CENTER,
                alignment = Text.Alignment.MIDDLE
            )
        }
    }

    override fun click(mouseX: Int, mouseY: Int, mouseButton: Int) {
        dropdown.click(mouseX, mouseY, mouseButton)
    }

    fun setPlayer(player: HypixelData.PlayerInfo) {}
}