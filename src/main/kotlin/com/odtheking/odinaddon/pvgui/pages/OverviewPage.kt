package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.toFixed
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.utils.LevelUtils
import com.odtheking.odinaddon.pvgui.utils.LevelUtils.cataLevel
import com.odtheking.odinaddon.pvgui.utils.Utils
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.PageHandler
import com.odtheking.odinaddon.pvgui.PVLayout
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.utils.Theme
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile
import tech.thatgravyboat.skyblockapi.api.remote.RepoItemsAPI
import tech.thatgravyboat.skyblockapi.helpers.McClient
import java.util.UUID
import kotlin.math.floor

object OverviewPage : PageHandler {
    override val name = "Overview"
    private const val TITLE_SIZE = 24f
    private const val TEXT_SIZE = 16f
    private const val PADDING = 10f
    private const val BTN_H = 28f
    private const val BTN_SPACING = 6f
    private const val PET_SIZE = 32f
    private val BTN_RADIUS get() = Theme.round

    private var cachedLines: List<String> = emptyList()
    private var cachedActivePetHeldItem: String? = null

    override fun onOpen() {
        val member = PVState.memberData() ?: return
        val mmComps = member.dungeons.dungeonTypes.mastermode.tierComps.filter { it.key != "total" }.values.sum()
        val floorComps = member.dungeons.dungeonTypes.catacombs.tierComps.filter { it.key != "total" }.values.sum()
        val totalRuns = (mmComps + floorComps).toDouble()
        val avgSecrets = if (totalRuns > 0) member.dungeons.secrets / totalRuns else 0.0
        val level = floor(member.leveling.experience / 100.0).toInt()
        val cata = member.dungeons.dungeonTypes.cataLevel
        val skillAvgCap = LevelUtils.cappedSkillAverage(member.playerData)
        val skillAvgRaw = LevelUtils.skillAverage(member.playerData)
        val secrets = member.dungeons.secrets
        val magicPower = member.assumedMagicalPower
        cachedActivePetHeldItem = member.pets.pets.firstOrNull { it.active }?.heldItem
        cachedLines = listOf(
            "Level§7: §a$level",
            "§4Cata Level§7: ${Utils.colorize(cata, 50.0)}${cata.toFixed(2)}",
            "§6Skill Average§7: ${Utils.colorize(skillAvgCap, 55.0)}${skillAvgCap.toFixed(1)} §7(${skillAvgRaw.toFixed(2)})",
            "§bSecrets§7: ${Utils.colorizeNumber(secrets, 100000)}${Utils.commas(secrets)} §7(${Utils.colorize(avgSecrets, 15.0)}${avgSecrets.toFixed(2)}§7)",
            "Magical Power§7: ${Utils.colorize(magicPower.toDouble(), 1900.0)}$magicPower",
            Utils.getActivePetDisplay(member.pets),
        )
    }

    override fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, mouseX: Double, mouseY: Double) {
        if (cachedLines.isEmpty()) onOpen()
        if (cachedLines.isEmpty()) return

        val data = PVState.playerData ?: return
        val profile = PVState.selectedProfile() ?: return

        val title = "§f${data.name} §7- §a${profile.cuteName ?: "Unknown"}"
        ctx.formattedText(title, x + (w - ctx.formattedTextWidth(title, TITLE_SIZE)) / 2f, y + PADDING, TITLE_SIZE)

        val titleBottom = y + PADDING + TITLE_SIZE + 6f
        ctx.line(x, titleBottom, x + w, titleBottom, 1f, Theme.separator)

        val avatarSize = 52f
        val avatarX = x + w - avatarSize - PADDING
        val avatarY = y + PADDING
        drawSkinHead(ctx, avatarX, avatarY, avatarSize, mouseX, mouseY)

        val profiles = data.profileData.profiles
        val btnY = y + h - BTN_H - PADDING / 2f
        if (profiles.size > 1) {
            val btnW = (w - PADDING * 2f - BTN_SPACING * (profiles.size - 1)) / profiles.size
            profiles.forEachIndexed { i, prof ->
                val bx = x + PADDING + i * (btnW + BTN_SPACING)
                val isSelected = prof.cuteName == PVState.profileName
                val isHovered = ctx.isHovered(mouseX, mouseY, bx, btnY, btnW, BTN_H)
                ctx.rect(bx, btnY, btnW, BTN_H, when {
                    isSelected -> Theme.accent
                    isHovered -> Theme.btnHover
                    else -> Theme.btnNormal
                }, BTN_RADIUS)
                val label = when (prof.gameMode?.lowercase()) {
                    "ironman" -> "§7☢ §f${prof.cuteName ?: "?"}"
                    "bingo" -> "§e☆ §f${prof.cuteName ?: "?"}"
                    "stranded" -> "§b◎ §f${prof.cuteName ?: "?"}"
                    else -> "§f${prof.cuteName ?: "?"}"
                }
                val lw = ctx.formattedTextWidth(label, TEXT_SIZE)
                ctx.formattedText(label, bx + (btnW - lw) / 2f, btnY + (BTN_H - TEXT_SIZE) / 2f, TEXT_SIZE)
            }
        }

        val lines = cachedLines
        val statsTop = titleBottom + PADDING
        val statsH = btnY - statsTop - PADDING

        ctx.textList(lines, x + PADDING, statsTop, w - PADDING * 2f - PET_SIZE - PADDING, statsH, maxSize = 22f)

        if (lines.isNotEmpty()) {
            cachedActivePetHeldItem.let { heldId ->
                val lineSpacing = statsH / lines.size
                val size = minOf(24f, lineSpacing * 0.65f)
                val petLineY = statsTop + (lines.size - 1) * lineSpacing + (lineSpacing - size) / 2f
                val textEndX = x + PADDING + ctx.formattedTextWidth(lines.last(), size)
                val icon = heldId?.let { RepoItemsAPI.getItem(it) } ?: ItemStack(Items.BARRIER)
                val slotPad = 2f
                val slotX = textEndX + 4f
                ctx.rect(slotX, petLineY, size + slotPad * 2f, size + slotPad * 2f, Color(255, 255, 255, 0.08f), 3f)
                ctx.item(icon, slotX + slotPad, petLineY + slotPad, size)
            }
        }
    }

    private fun getPlayerHeadItem(uuid: String, name: String): ItemStack {
        val stack = ItemStack(Items.PLAYER_HEAD)
        val javaUuid = runCatching {
            val u = uuid.replace("-", "")
            UUID.fromString("${u.take(8)}-${u.substring(8,12)}-${u.substring(12,16)}-${u.substring(16,20)}-${u.substring(20)}")
        }.getOrNull() ?: return stack
        stack.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(javaUuid))
        return stack
    }

    private fun drawSkinHead(ctx: DrawContext, x: Float, y: Float, size: Float, mouseX: Double, mouseY: Double) {
        val data = PVState.playerData ?: return
        val isHovered = ctx.isHovered(mouseX, mouseY, x, y, size, size)
        ctx.rect(x, y, size, size, Color(255, 255, 255, 0.35f), size * 0.15f)
        NVGRenderer.hollowRect(
            x - 2f, y - 2f, size + 4f, size + 4f,
            if (isHovered) 2.5f else 1.5f,
            if (isHovered) Theme.accent.rgba else Colors.WHITE.rgba,
            size * 0.15f + 2f
        )
        ctx.item(getPlayerHeadItem(data.uuid, data.name), x, y, size, showTooltip = false, showStackSize = false)
    }

    override fun onClick(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        val data = PVState.playerData ?: return
        val avatarSize = 52f
        val avatarX = PVLayout.MAIN_X + PVLayout.MAIN_W - avatarSize - PADDING
        val avatarY = PVLayout.MAIN_Y + PADDING
        if (ctx.isHovered(mouseX, mouseY, avatarX, avatarY, avatarSize, avatarSize)) {
            val name = PVState.playerData?.name ?: return
            McClient.openUri(java.net.URI("https://namemc.com/profile/$name"))
            return
        }

        val profiles = data.profileData.profiles
        if (profiles.size <= 1) return
        val btnY = PVLayout.MAIN_Y + PVLayout.MAIN_H - BTN_H - PADDING / 2f
        val btnW = (PVLayout.MAIN_W - PADDING * 2f - BTN_SPACING * (profiles.size - 1)) / profiles.size
        profiles.forEachIndexed { i, prof ->
            val bx = PVLayout.MAIN_X + PADDING + i * (btnW + BTN_SPACING)
            if (ctx.isHovered(mouseX, mouseY, bx, btnY, btnW, BTN_H)) {
                PVState.profileName = prof.cuteName
                PVState.invalidateCache()
            }
        }
    }
}