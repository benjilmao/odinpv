package com.odtheking.odinaddon.pvgui2.pages

import com.odtheking.odin.utils.toFixed
import com.odtheking.odinaddon.pvgui2.utils.Utils
import com.odtheking.odinaddon.pvgui2.utils.Utils.without
import com.odtheking.odinaddon.pvgui2.utils.LevelUtils
import com.odtheking.odinaddon.pvgui2.utils.profileList
import com.odtheking.odinaddon.pvgui2.utils.profileOrSelected
import com.odtheking.odinaddon.pvgui2.Theme
import com.odtheking.odinaddon.pvgui2.PVGui
import com.odtheking.odinaddon.pvgui2.textList
import me.owdding.lib.builder.LayoutFactory
import me.owdding.lib.displays.Display
import me.owdding.lib.displays.Displays
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget

object ProfilePage {

    fun build(screen: PVGui, addWidget: (AbstractWidget) -> Unit) {
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
        val currentProfile = data.profileOrSelected(screen.profileName)

        val leftW = mainWidth / 2 - spacer * 2
        val rightW = mainWidth - leftW - spacer
        val rightHalfH = (mainHeight - spacer) / 2
        val crossGap = 10

        val skillAvgCapped = LevelUtils.cappedSkillAverage(profile.playerData)
        val skillAvgRaw = LevelUtils.skillAverage(profile.playerData)
        val skillLines = listOf(
            "§6Skill Avg§7: ${Utils.colorize(skillAvgCapped, 55.0)}${skillAvgCapped.toFixed(2)} §7(${skillAvgRaw.toFixed(2)})"
        ) + profile.playerData.experience
            .without("SKILL_DUNGEONEERING", "SKILL_SOCIAL", "SKILL_RUNECRAFTING")
            .entries.sortedByDescending { it.value }
            .mapNotNull { (key, exp) ->
                val skill = key.lowercase().substringAfter("skill_")
                if (LevelUtils.getSkillCap(skill) == -1) return@mapNotNull null
                val level = LevelUtils.getSkillLevel(skill, exp)
                val cap = LevelUtils.getSkillCap(skill).toDouble()
                val color = LevelUtils.getSkillColorCode(skill)
                "§$color${skill.replaceFirstChar { it.uppercase() }}§7: ${Utils.colorize(level.coerceAtMost(cap), cap)}${level.toFixed(2)}"
            }

        val slayerLines = profile.slayer.bosses.entries
            .sortedByDescending { it.value.xp }
            .map { (boss, bossData) ->
                val level = LevelUtils.getSlayerSkillLevel(bossData.xp.toDouble(), boss)
                val cap = LevelUtils.getSlayerCap(boss).toDouble()
                val color = LevelUtils.getSlayerColorCode(boss)
                "§$color${boss.replaceFirstChar { it.uppercase() }}§7: ${Utils.colorize(level, cap)}${level.toFixed(2)} §7(${Utils.truncate(bossData.xp.toDouble())})"
            }

        val purse = profile.currencies.coins
        val bankBalance = currentProfile?.banking?.balance ?: 0.0
        val personalBank = profile.profile.bankAccount
        val hasMultipleProfiles = data.profileList.size > 1
        val bankDisplay = if (hasMultipleProfiles)
            "${Utils.truncate(bankBalance)} §8| §7${Utils.truncate(personalBank)}"
        else Utils.truncate(bankBalance)
        val goldCollection = profile.collection["GOLD_INGOT"]

        val currencyLines = listOf(
            "§6Purse§7: ${Utils.truncate(purse)}",
            "§6Bank§7: $bankDisplay",
            "§6Gold§7: ${goldCollection?.let { "${Utils.colorizeNumber(it, 100_000_000)}${Utils.commas(it)}" } ?: "§70"}",
        )

        LayoutFactory.vertical(0) {
            display(textList(skillLines, leftW - spacer, mainHeight - spacer * 2, scale = 1.4f))
        }.apply { setPosition(mainX + spacer, mainY + spacer) }.visitWidgets { addWidget(it) }

        LayoutFactory.vertical(0) {
            display(textList(slayerLines, rightW - spacer * 3, rightHalfH - spacer * 2, scale = 1.3f))
        }.apply { setPosition(mainX + leftW + spacer * 2, mainY + spacer) }.visitWidgets { addWidget(it) }

        LayoutFactory.vertical(0) {
            display(textList(currencyLines, rightW - spacer * 3, rightHalfH - spacer * 2, scale = 1.3f))
        }.apply { setPosition(mainX + leftW + spacer * 2, mainY + rightHalfH + spacer * 2) }.visitWidgets { addWidget(it) }

        LayoutFactory.vertical(0) {
            display(object : Display {
                override fun getWidth() = mainWidth
                override fun getHeight() = mainHeight
                override fun render(g: GuiGraphics) {
                    val midX = leftW + spacer
                    val midY = mainHeight / 2
                    g.fill(midX, crossGap, midX + 1, mainHeight - crossGap, Theme.separator)
                    g.fill(midX + crossGap, midY, mainWidth - crossGap, midY + 1, Theme.separator)
                }
            })
        }.apply { setPosition(mainX, mainY) }.visitWidgets { addWidget(it) }
    }
}