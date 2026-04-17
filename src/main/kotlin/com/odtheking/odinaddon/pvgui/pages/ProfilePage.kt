package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.capitalizeWords
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

    private val halfH = (MAIN_H / 2f) - (PAD / 2f)

    private val skillAverage: String by resettableLazy {
        val d = PVState.member() ?: return@resettableLazy ""
        "§6Skill Average§7: ${LevelUtils.cappedSkillAverage(d.playerData).colorize(55.0)} §7(${
            "%.2f".format(LevelUtils.skillAverage(d.playerData))})"
    }

    private val skillText: List<String> by resettableLazy {
        val d = PVState.member() ?: return@resettableLazy emptyList()
        d.playerData.experience.without("SKILL_DUNGEONEERING").entries
            .sortedByDescending { it.value }
            .mapNotNull { (key, exp) ->
                val skill = key.lowercase().substringAfter("skill_")
                val cap = LevelUtils.skillCap(skill).takeIf { it != -1 }?.toDouble() ?: return@mapNotNull null
                val level = LevelUtils.skillLevel(skill, exp)
                "§${LevelUtils.skillColor(skill)}${skill.capitalizeWords()}§7: ${
                    level.coerceAtMost(cap).colorize(cap)} §7(${"%.2f".format(level)})"
            }
    }

    private val otherText: List<String> by resettableLazy {
        val d = PVState.member() ?: return@resettableLazy emptyList()
        val bank = PVState.profile()?.banking?.balance ?: 0.0
        val personal = d.profile.bankAccount
        val multi = (PVState.player?.profileData?.profiles?.size ?: 0) > 1
        val bankDisplay = if (multi) "${bank.truncate} | ${personal.truncate}" else bank.truncate
        val gold = d.collection["GOLD_INGOT"]
        listOf(
            "§6Purse§7: §r${d.currencies.coins.truncate}",
            "§6Bank§7: §r$bankDisplay",
            "§6Gold Collection§7: §r${gold?.let { "${it.colorizeNumber(100_000_000)}${it.commas} §8(${it.toString().length})" } ?: "§70"}",
        )
    }

    private val slayerText: List<String> by resettableLazy {
        val d = PVState.member() ?: return@resettableLazy emptyList()
        val bossToId = mapOf("revenant" to "zombie", "tarantula" to "spider", "sven" to "wolf",
            "voidgloom" to "enderman", "inferno_demonlord" to "blaze", "vampire" to "vampire")
        d.slayer.bosses.entries.sortedByDescending { it.value.xp }.map { (boss, bd) ->
            val id  = bossToId[boss] ?: boss
            val lv  = LevelUtils.slayerLevel(bd.xp.toDouble(), id)
            "§${LevelUtils.slayerColor(id)}${id.capitalizeWords()}§7: ${lv.colorize(LevelUtils.slayerCap(id).toDouble())} §7(${bd.xp.toDouble().truncate})"
        }
    }

    private val skillBox: TextBox by resettableLazy {
        textBox(
            CONTENT_X + PAD, CONTENT_Y + PAD,
            QUAD_W, MAIN_H - PAD * 2f,
            title = skillAverage, titleScale = 2.7f,
            lines = skillText, scale = 2.5f, spacer = PAD,
            color = Theme.textPrimary,
        )
    }

    private val slayerBox: TextBox by resettableLazy {
        textBox(
            CONTENT_X + PAD + QUAD_W + PAD, CONTENT_Y + PAD,
            QUAD_W, halfH - PAD * 2f,
            lines = slayerText, scale = 2.5f, spacer = PAD,
            color = Theme.textPrimary,
        )
    }

    private val purseBox: TextBox by resettableLazy {
        textBox(
            CONTENT_X + PAD + QUAD_W + PAD, CONTENT_Y + PAD + halfH + PAD,
            QUAD_W, halfH - PAD * 2f,
            lines = otherText, scale = 2.5f, spacer = PAD,
            color = Theme.textPrimary,
        )
    }

    override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        NVGRenderer.rect(CONTENT_X, CONTENT_Y, QUAD_W, MAIN_H, Theme.slotBg, Theme.radius)
        NVGRenderer.rect(CONTENT_X + PAD + QUAD_W, CONTENT_Y, QUAD_W, halfH, Theme.slotBg, Theme.radius)
        NVGRenderer.rect(CONTENT_X + PAD + QUAD_W, CONTENT_Y + PAD + halfH, QUAD_W, halfH, Theme.slotBg, Theme.radius)
        skillBox.draw()
        slayerBox.draw()
        purseBox.draw()
    }
}