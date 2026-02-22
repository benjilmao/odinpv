package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.toFixed
import com.odtheking.odinaddon.pvgui.utils.LevelUtils
import com.odtheking.odinaddon.pvgui.utils.Utils
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.PageHandler
import com.odtheking.odinaddon.pvgui.PVState

object ProfilePage : PageHandler {

    private val COL_PANEL_BG  = Color(255, 255, 255, 0.05f)
    private val COL_SEPARATOR = Color(255, 255, 255, 0.15f)

    private const val PADDING      = 10f
    private const val PANEL_RADIUS = 6f
    private const val GAP          = 10f

    override fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, mouseX: Double, mouseY: Double) {
        val member = PVState.memberData() ?: return

        val leftW  = w * 0.5f - GAP / 2f
        val rightW = w - leftW - GAP
        val rightHalf = h / 2f - GAP / 2f
        val rx = x + leftW + GAP

        val skillAvgCapped = LevelUtils.cappedSkillAverage(member.playerData)
        val skillAvgRaw = LevelUtils.skillAverage(member.playerData)
        val skillLines = listOf(
            "§6Skill Avg§7: ${Utils.colorize(skillAvgCapped, 55.0)}${skillAvgCapped.toFixed(2)} §7(${skillAvgRaw.toFixed(2)})"
        ) + member.playerData.experience
            .filter { !listOf("SKILL_DUNGEONEERING", "SKILL_SOCIAL", "SKILL_RUNECRAFTING").contains(it.key) }
            .entries.sortedByDescending { it.value }
            .mapNotNull { (key, exp) ->
                val skill = key.lowercase().substringAfter("skill_")
                if (LevelUtils.getSkillCap(skill) == -1) return@mapNotNull null
                val level = LevelUtils.getSkillLevel(skill, exp)
                val cap = LevelUtils.getSkillCap(skill).toDouble()
                val color = LevelUtils.getSkillColorCode(skill)
                "§$color${skill.replaceFirstChar { it.uppercase() }}§7: ${Utils.colorize(level.coerceAtMost(cap), cap)}${level.toFixed(2)}"
            }

        val bossToId = mapOf(
            "revenant" to "zombie",
            "tarantula" to "spider",
            "sven" to "wolf",
            "voidgloom" to "enderman",
            "inferno_demonlord" to "blaze",
            "vampire" to "vampire",
        )
        val slayerLines = member.slayer.bosses.entries
            .sortedByDescending { it.value.xp }
            .map { (boss, bossData) ->
                val id = bossToId[boss] ?: boss
                val level = LevelUtils.getSlayerSkillLevel(bossData.xp.toDouble(), id)
                val cap = LevelUtils.getSlayerCap(id).toDouble()
                val color = LevelUtils.getSlayerColorCode(id)
                "§$color${id.replaceFirstChar { it.uppercase() }}§7: ${Utils.colorize(level, cap)}${level.toFixed(2)} §7(${Utils.truncate(bossData.xp.toDouble())})"
            }

        val purse = member.currencies.coins
        val bankBalance = PVState.selectedProfile()?.banking?.balance ?: 0.0
        val personalBank = member.profile.bankAccount
        val hasMultipleProfiles = (PVState.playerData?.profileData?.profiles?.size ?: 0) > 1
        val bankDisplay = if (hasMultipleProfiles)
            "${Utils.truncate(bankBalance)} §8| §7${Utils.truncate(personalBank)}"
        else Utils.truncate(bankBalance)
        val goldCollection = member.collection?.get("GOLD_INGOT")

        val currencyLines = listOf(
            "§6Purse§7: ${Utils.truncate(purse)}",
            "§6Bank§7: $bankDisplay",
            "§6Gold§7: ${goldCollection?.let { "${Utils.colorizeNumber(it, 100_000_000)}${Utils.commas(it)}" } ?: "§70"}",
        )

        ctx.textList(skillLines, x + PADDING, y, leftW - PADDING * 2f, h, maxSize = 22f)
        ctx.textList(slayerLines, rx + PADDING, y, rightW - PADDING * 2f, rightHalf, maxSize = 22f)
        ctx.textList(currencyLines, rx + PADDING, y + rightHalf + GAP, rightW - PADDING * 2f, rightHalf, maxSize = 22f)

        ctx.line(x + leftW + GAP / 2f, y + 4f, x + leftW + GAP / 2f, y + h - 4f, 1f, COL_SEPARATOR)
        ctx.line(rx, y + rightHalf + GAP / 2f, rx + rightW, y + rightHalf + GAP / 2f, 1f, COL_SEPARATOR)
    }
}