package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.capitalizeWords
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.dsl.TextBox
import com.odtheking.odinaddon.pvgui.utils.LevelUtils
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.colorize
import com.odtheking.odinaddon.pvgui.utils.colorizeNumber
import com.odtheking.odinaddon.pvgui.utils.commas
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import com.odtheking.odinaddon.pvgui.utils.truncate
import com.odtheking.odinaddon.pvgui.utils.without
import net.minecraft.client.gui.GuiGraphics

object ProfilePage : PVPage() {
    override val name = "Profile"

    private val spacing = 12f
    private val leftW get() = w / 2f - spacing / 2f
    private val rightW get() = w - leftW - spacing
    private val rightX get() = x + leftW + spacing
    private val topH get() = h / 2f - spacing / 2f
    private val botY get() = y + topH + spacing

    private val skillTitle: String by resettableLazy {
        val data = PVState.member() ?: return@resettableLazy ""
        "§6Skill Average§7: ${LevelUtils.cappedSkillAverage(data.playerData).colorize(55.0)} §7(${"%.2f".format(LevelUtils.skillAverage(data.playerData))})"
    }

    private val skillLines: List<String> by resettableLazy {
        val data = PVState.member() ?: return@resettableLazy emptyList()
        data.playerData.experience
            .without("SKILL_DUNGEONEERING")
            .entries.sortedByDescending { it.value }
            .mapNotNull { (key, exp) ->
                val skill = key.lowercase().substringAfter("skill_")
                val cap = LevelUtils.skillCap(skill).takeIf { it != -1 }?.toDouble() ?: return@mapNotNull null
                val level = LevelUtils.skillLevel(skill, exp)
                "§${LevelUtils.skillColor(skill)}${skill.capitalizeWords()}§7: ${level.coerceAtMost(cap).colorize(cap)} §7(${"%.2f".format(level)})"
            }
    }

    private val slayerLines: List<String> by resettableLazy {
        val data = PVState.member() ?: return@resettableLazy emptyList()
        val bossToId = mapOf(
            "revenant" to "zombie", "tarantula" to "spider", "sven" to "wolf",
            "voidgloom" to "enderman", "inferno_demonlord" to "blaze", "vampire" to "vampire",
        )
        data.slayer.bosses.entries.sortedByDescending { it.value.xp }.map { (boss, bossData) ->
            val id = bossToId[boss] ?: boss
            val level = LevelUtils.slayerLevel(bossData.xp.toDouble(), id)
            val cap = LevelUtils.slayerCap(id).toDouble()
            "§${LevelUtils.slayerColor(id)}${id.capitalizeWords()}§7: ${level.colorize(cap)} §7(${bossData.xp.toDouble().truncate})"
        }
    }

    private val currencyLines: List<String> by resettableLazy {
        val data = PVState.member() ?: return@resettableLazy emptyList()
        val profile = PVState.profile()
        val bank = profile?.banking?.balance ?: 0.0
        val personal = data.profile.bankAccount
        val multiProfile = (PVState.player?.profileData?.profiles?.size ?: 0) > 1
        val bankDisplay = if (multiProfile) "${bank.truncate} | ${personal.truncate}" else bank.truncate
        val gold = data.collection["GOLD_INGOT"]
        listOf(
            "§6Purse§7: §r${data.currencies.coins.truncate}",
            "§6Bank§7: §r$bankDisplay",
            "§6Gold Collection§7: §r${gold?.let { "${it.colorizeNumber(100_000_000)}${it.commas} §8(${it.toString().length})" } ?: "§70"}",
        )
    }

    override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        TextBox(x = x, y = y, w = leftW, h = h,
            lines = skillLines, textSize = 22f,
            title = skillTitle, titleSize = 30f,
            background = Theme.slotBg).draw()

        TextBox(x = rightX, y = y, w = rightW, h = topH,
            lines = slayerLines, textSize = 20f,
            background = Theme.slotBg).draw()

        TextBox(x = rightX, y = botY, w = rightW, h = topH,
            lines = currencyLines, textSize = 20f,
            background = Theme.slotBg).draw()
    }
}