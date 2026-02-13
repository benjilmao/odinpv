package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.toFixed
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.core.PVPage
import com.odtheking.odinaddon.pvgui.core.Theme
import com.odtheking.odinaddon.pvgui.utils.*
import com.odtheking.odinaddon.pvgui.utils.Utils.without
import com.odtheking.odinaddon.pvgui.utils.apiutils.HypixelData
import com.odtheking.odinaddon.pvgui.utils.apiutils.LevelUtils
import com.odtheking.odinaddon.pvgui.utils.apiutils.profileList
import com.odtheking.odinaddon.pvgui.utils.apiutils.profileOrSelected
import net.minecraft.client.gui.GuiGraphics

object Profile : PVPage("Profile") {

    private val quadrantWidth = (mainWidth - spacer) / 2f
    private val quadrantHeight = (mainHeight - spacer) / 2f

    // Get the current profile for banking (not member)
    private val currentProfile by lazy { player.profileOrSelected(profileName) }

    private val skillAverageText: String by resettableLazy {
        val playerData = profile?.playerData ?: return@resettableLazy "§7Loading skills..."
        val skillAvg = LevelUtils.cappedSkillAverage(playerData)
        val rawAvg = LevelUtils.skillAverage(playerData)
        "§6Skill Average§7: ${Utils.colorize(skillAvg, 55.0)}${skillAvg.toFixed(2)} §7(${rawAvg.toFixed(2)})"
    }

    private val skillText: List<String> by resettableLazy {
        val playerData = profile?.playerData ?: return@resettableLazy emptyList()
        playerData.experience.without("SKILL_DUNGEONEERING").entries
            .sortedByDescending { it.value }
            .map { (key, exp) ->
                val skill = key.lowercase().substringAfter("skill_")
                val level = LevelUtils.getSkillLevel(skill, exp)
                val cap = LevelUtils.getSkillCap(skill).toDouble()
                val colorCode = LevelUtils.getSkillColorCode(skill)
                val display = skill.replaceFirstChar { it.uppercase() }
                "§$colorCode$display§7: ${Utils.colorize(level.coerceAtMost(cap), cap)}${level.toFixed(2)} §7(${level.toFixed(2)})"
            }
    }

    private val otherText: List<String> by resettableLazy {
        val purse = profile?.currencies?.coins ?: 0.0
        val bankBalance = currentProfile?.banking?.balance ?: 0.0
        val personalBank = profile?.profile?.bankAccount ?: 0.0
        val hasMultipleProfiles = player.profileList.size > 1

        val bankDisplay = if (hasMultipleProfiles) {
            "${Utils.truncate(bankBalance)} | ${Utils.truncate(personalBank)}"
        } else {
            Utils.truncate(bankBalance)
        }

        listOf(
            "§6Purse§7: ${Utils.truncate(purse)}",
            "§6Bank§7: $bankDisplay",
            "§6Gold Collection§7: ${
                profile?.collection?.get("GOLD_INGOT")?.let {
                    "${Utils.colorizeNumber(it, 100_000_000)}${Utils.commas(it)} §8(${it.toString().length})"
                } ?: "§70"
            }"
        )
    }

    private val slayerText: List<String> by resettableLazy {
        val slayer = profile?.slayer ?: return@resettableLazy emptyList()
        slayer.bosses.entries.sortedByDescending { it.value.xp }.map { (boss, data) ->
            val level = LevelUtils.getSlayerSkillLevel(data.xp.toDouble(), boss)
            val cap = LevelUtils.getSlayerCap(boss).toDouble()
            val colorCode = LevelUtils.getSlayerColorCode(boss)
            val display = boss.replaceFirstChar { it.uppercase() }
            "§$colorCode$display§7: ${Utils.colorize(level, cap)}${level.toFixed(2)} §7(${Utils.truncate(data.xp.toDouble())})"
        }
    }

    private val skillBox by resettableLazy {
        TextBox(
            box = Box(
                mainX + spacer,
                2 * spacer,
                quadrantWidth,
                mainHeight - spacer * 2
            ),
            title = skillAverageText,
            titleScale = 2.7f,
            text = skillText,
            textScale = 2.5f,
            spacer = spacer.toFloat(),
            defaultColor = Theme.fontColor.rgba
        )
    }

    private val slayerBox by resettableLazy {
        TextBox(
            box = Box(
                mainX + 2 * spacer + quadrantWidth,
                2 * spacer,
                quadrantWidth,
                quadrantHeight - 2 * spacer
            ),
            title = null,
            text = slayerText,
            textScale = 2.5f,
            spacer = spacer.toFloat(),
            defaultColor = Theme.fontColor.rgba
        )
    }

    private val purseBox by resettableLazy {
        TextBox(
            box = Box(
                mainX + 2 * spacer + quadrantWidth,
                3 * spacer + quadrantHeight,
                quadrantWidth,
                quadrantHeight - 2 * spacer
            ),
            title = null,
            text = otherText,
            textScale = 2.5f,
            spacer = spacer.toFloat(),
            defaultColor = Theme.fontColor.rgba
        )
    }

    override fun draw(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        NVGRenderer.rect(
            mainX.toFloat(), spacer.toFloat(),
            quadrantWidth, mainHeight.toFloat(),
            Theme.secondaryBg.rgba, Theme.roundness
        )
        NVGRenderer.rect(
            (mainX + spacer + quadrantWidth).toFloat(), spacer.toFloat(),
            quadrantWidth, quadrantHeight,
            Theme.secondaryBg.rgba, Theme.roundness
        )
        NVGRenderer.rect(
            (mainX + spacer + quadrantWidth).toFloat(), (2 * spacer + quadrantHeight).toFloat(),
            quadrantWidth, quadrantHeight,
            Theme.secondaryBg.rgba, Theme.roundness
        )

        skillBox.draw()
        slayerBox.draw()
        purseBox.draw()
    }

    fun setPlayer(player: HypixelData.PlayerInfo) {}

    override fun click(mouseX: Int, mouseY: Int, mouseButton: Int) {}
}