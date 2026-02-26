package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.toFixed
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.PageHandler
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.utils.TextBox
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import com.odtheking.odinaddon.pvgui.utils.LevelUtils
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.colorize
import com.odtheking.odinaddon.pvgui.utils.colorizeNumber
import com.odtheking.odinaddon.pvgui.utils.commas
import com.odtheking.odinaddon.pvgui.utils.truncate

object ProfilePage : PageHandler {
    override val name = "Profile"
    private const val PADDING = 10f
    private const val GAP = 10f

    private val cachedSkillTitle: String by resettableLazy {
        val member = PVState.memberData() ?: return@resettableLazy ""
        val cap = LevelUtils.cappedSkillAverage(member.playerData)
        val raw = LevelUtils.skillAverage(member.playerData)
        "§6Skill Average§7: ${cap.colorize(55.0)} §7(${raw.toFixed(2)})"
    }

    private val cachedSkillLines: List<String> by resettableLazy {
        val member = PVState.memberData() ?: return@resettableLazy emptyList()
        member.playerData.experience
            .filter { !listOf("SKILL_DUNGEONEERING", "SKILL_SOCIAL", "SKILL_RUNECRAFTING").contains(it.key) }
            .entries.sortedByDescending { it.value }
            .mapNotNull { (key, exp) ->
                val skill = key.lowercase().substringAfter("skill_")
                if (LevelUtils.getSkillCap(skill) == -1) return@mapNotNull null
                val level = LevelUtils.getSkillLevel(skill, exp)
                val cap = LevelUtils.getSkillCap(skill).toDouble()
                val color = LevelUtils.getSkillColorCode(skill)
                "§$color${skill.replaceFirstChar { it.uppercase() }}§7: ${cap.colorize(level.coerceAtMost(cap), 1)} §7(${level.toFixed(2)})"
            }
    }

    private val cachedSlayerLines: List<String> by resettableLazy {
        val member = PVState.memberData() ?: return@resettableLazy emptyList()
        val bossToId = mapOf(
            "revenant" to "zombie", "tarantula" to "spider", "sven" to "wolf",
            "voidgloom" to "enderman", "inferno_demonlord" to "blaze", "vampire" to "vampire",
        )
        member.slayer.bosses.entries.sortedByDescending { it.value.xp }.map { (boss, data) ->
            val id = bossToId[boss] ?: boss
            val level = LevelUtils.getSlayerSkillLevel(data.xp.toDouble(), id)
            val cap = LevelUtils.getSlayerCap(id).toDouble()
            val color = LevelUtils.getSlayerColorCode(id)
            "§$color${id.replaceFirstChar { it.uppercase() }}§7: ${cap.colorize(level)} §7(${data.xp.toDouble().truncate})"
        }
    }

    private val cachedCurrencyLines: List<String> by resettableLazy {
        val member = PVState.memberData() ?: return@resettableLazy emptyList()
        val purse = member.currencies.coins
        val bank = PVState.selectedProfile()?.banking?.balance ?: 0.0
        val personal = member.profile.bankAccount
        val multiProfile = (PVState.playerData?.profileData?.profiles?.size ?: 0) > 1
        val bankDisplay = if (multiProfile) "${bank.truncate} §8| §7${personal.truncate}"
        else bank.truncate
        val gold = member.collection?.get("GOLD_INGOT")
        listOf(
            "§6Purse§7: ${purse.truncate}",
            "§6Bank§7: $bankDisplay",
            "§6Gold§7: ${gold?.let { "${it.colorizeNumber(100_000_000)}${it.commas} §8(${it.toString().length})" } ?: "§70"}",
        )
    }

    override fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, mouseX: Double, mouseY: Double) {
        if (cachedSkillLines.isEmpty()) return
        val leftW = w * 0.5f - GAP / 2f
        val rightW = w - leftW - GAP
        val rightHalf = h / 2f - GAP / 2f
        val rx = x + leftW + GAP

        TextBox(ctx, x + PADDING, y, leftW - PADDING * 2f, h,
            cachedSkillTitle, 24f, cachedSkillLines, 22f).draw()

        TextBox(ctx, rx + PADDING, y, rightW - PADDING * 2f, rightHalf,
            null, 0f, cachedSlayerLines, 22f).draw()

        TextBox(ctx, rx + PADDING, y + rightHalf + GAP, rightW - PADDING * 2f, rightHalf,
            null, 0f, cachedCurrencyLines, 22f).draw()

        ctx.line(x + leftW + GAP / 2f, y + 4f, x + leftW + GAP / 2f, y + h - 4f, 1f, Theme.separator)
        ctx.line(rx, y + rightHalf + GAP / 2f, rx + rightW, y + rightHalf + GAP / 2f, 1f, Theme.separator)
    }
}