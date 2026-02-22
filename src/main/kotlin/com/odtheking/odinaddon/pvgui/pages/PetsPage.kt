package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Color
import com.odtheking.odinaddon.pvgui2.utils.HypixelData
import com.odtheking.odinaddon.pvgui2.utils.LevelUtils
import com.odtheking.odinaddon.pvgui2.utils.Utils
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.PageHandler
import com.odtheking.odinaddon.pvgui.PVLayout
import com.odtheking.odinaddon.pvgui.PVState
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import tech.thatgravyboat.skyblockapi.api.remote.PetQuery
import tech.thatgravyboat.skyblockapi.api.remote.RepoPetsAPI
import tech.thatgravyboat.skyblockapi.api.remote.RepoItemsAPI

object PetsPage : PageHandler {

    private val COL_PANEL_BG  = Color(255, 255, 255, 0.05f)
    private val COL_SELECTED  = Color(0, 200, 80, 0.6f)
    private val COL_ACTIVE_BG = Color(0, 170, 0, 0.25f)
    private val COL_SEPARATOR = Color(255, 255, 255, 0.15f)
    private val COL_WHITE     = Color(255, 255, 255)
    private val COL_GRAY      = Color(170, 170, 170)
    private val COL_BAR_BG    = Color(40, 40, 40)
    private val COL_BAR_FG    = Color(80, 160, 255)

    private const val PADDING      = 8f
    private const val SLOT_RADIUS  = 4f
    private const val PANEL_RADIUS = 6f
    private const val TEXT_SIZE    = 14f
    private const val INFO_TEXT    = 12f
    private const val COLS         = 9
    private const val SLOT_SPACING = 4f
    private const val INFO_RATIO   = 0.30f

    private val rarityOrder = listOf("MYTHIC", "LEGENDARY", "EPIC", "RARE", "UNCOMMON", "COMMON")

    private fun rarityColor(tier: String): Color = when (tier.uppercase()) {
        "MYTHIC"    -> Color(255, 85, 255, 0.35f)
        "LEGENDARY" -> Color(255, 170, 0, 0.35f)
        "EPIC"      -> Color(170, 0, 170, 0.35f)
        "RARE"      -> Color(85, 85, 255, 0.35f)
        "UNCOMMON"  -> Color(85, 255, 85, 0.35f)
        else        -> Color(170, 170, 170, 0.35f)
    }

    private fun rarityPrefix(tier: String): String = when (tier.uppercase()) {
        "MYTHIC"    -> "§d"
        "LEGENDARY" -> "§6"
        "EPIC"      -> "§5"
        "RARE"      -> "§9"
        "UNCOMMON"  -> "§a"
        else        -> "§f"
    }

    private fun sortedPets(): List<HypixelData.Pet> =
        PVState.memberData()?.pets?.pets?.sortedWith(compareBy(
            { rarityOrder.indexOf(it.tier.uppercase()).takeIf { i -> i >= 0 } ?: rarityOrder.size },
            { -LevelUtils.getPetLevel(it.exp, rarity(it), it.type) },
            { -it.exp },
        )) ?: emptyList()

    private fun rarity(pet: HypixelData.Pet) =
        SkyBlockRarity.fromNameOrNull(pet.tier) ?: SkyBlockRarity.COMMON

    override fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, mouseX: Double, mouseY: Double) {
        val pets = sortedPets()

        val infoW    = w * INFO_RATIO
        val gridW    = w - infoW - PADDING
        val slotSize = (gridW - PADDING - SLOT_SPACING * (COLS - 1)) / COLS

        val totalRows   = (pets.size + COLS - 1) / COLS
        val visibleRows = ((h - PADDING * 2f) / (slotSize + SLOT_SPACING)).toInt()
        PVState.petsScroll = PVState.petsScroll.coerceIn(0, (totalRows - visibleRows).coerceAtLeast(0))

        ctx.pushScissor(x, y, gridW + PADDING, h)

        val startIndex = PVState.petsScroll * COLS
        val endIndex   = (startIndex + (visibleRows + 1) * COLS).coerceAtMost(pets.size)

        for (idx in startIndex until endIndex) {
            val pet = pets[idx]
            val col = idx % COLS
            val row = idx / COLS - PVState.petsScroll
            val sx  = x + PADDING + col * (slotSize + SLOT_SPACING)
            val sy  = y + PADDING + row * (slotSize + SLOT_SPACING)

            if (sy + slotSize < y || sy > y + h) continue

            ctx.rect(sx, sy, slotSize, slotSize, rarityColor(pet.tier), SLOT_RADIUS)
            if (idx == PVState.selectedPetIndex) ctx.hollowRect(sx, sy, slotSize, slotSize, 2f, COL_SELECTED, SLOT_RADIUS)
            if (pet.active) ctx.rect(sx, sy, slotSize, slotSize, COL_ACTIVE_BG, SLOT_RADIUS)

            val itemPad  = slotSize * 0.12f
            val itemSize = slotSize - itemPad * 2f
            ctx.item(
                RepoPetsAPI.getPetAsItem(PetQuery(
                    pet.type, rarity(pet),
                    LevelUtils.getPetLevel(pet.exp, rarity(pet), pet.type).toInt(),
                    pet.skin, pet.heldItem,
                )),
                sx + itemPad, sy + itemPad, itemSize,
            )
        }
        ctx.popScissor()

        val sepX = x + gridW + PADDING
        ctx.line(sepX, y + 4f, sepX, y + h - 4f, 1f, COL_SEPARATOR)

        drawInfoPanel(ctx, sepX + PADDING, y, infoW - PADDING, h, pets)
    }

    private fun drawInfoPanel(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, pets: List<HypixelData.Pet>) {
        val idx = PVState.selectedPetIndex
        if (idx < 0 || idx >= pets.size) {
            val msg = "Select a pet"
            ctx.text(msg, x + (w - ctx.textWidth(msg, INFO_TEXT)) / 2f, y + h / 2f - INFO_TEXT / 2f, INFO_TEXT, COL_GRAY)
            return
        }

        val pet      = pets[idx]
        val rar      = rarity(pet)
        val level    = LevelUtils.getPetLevel(pet.exp, rar, pet.type).toInt()
        val progress = LevelUtils.getPetProgress(pet.exp, rar, pet.type)
        val prefix   = rarityPrefix(pet.tier)

        ctx.rect(x, y, w, h, COL_PANEL_BG, PANEL_RADIUS)
        var curY = y + PADDING

        val name    = pet.type.lowercase().split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        val nameStr = "$prefix$name"
        ctx.formattedText(nameStr, x + (w - ctx.formattedTextWidth(nameStr, INFO_TEXT + 1f)) / 2f, curY, INFO_TEXT + 1f)
        curY += INFO_TEXT + 6f

        ctx.formattedText("§7Level: $prefix$level", x + PADDING, curY, INFO_TEXT)
        curY += INFO_TEXT + 4f

        val barW = w - PADDING * 2f
        val barH = 6f
        ctx.rect(x + PADDING, curY, barW, barH, COL_BAR_BG, 3f)
        ctx.rect(x + PADDING, curY, barW * progress, barH, COL_BAR_FG, 3f)
        curY += barH + 4f

        ctx.formattedText("§7XP: §f${Utils.truncate(pet.exp.toLong())}", x + PADDING, curY, TEXT_SIZE)
        curY += TEXT_SIZE + 8f

        ctx.line(x + PADDING, curY, x + w - PADDING, curY, 1f, COL_SEPARATOR)
        curY += 8f

        pet.heldItem?.let { heldId ->
            ctx.formattedText("§7Held Item:", x + PADDING, curY, TEXT_SIZE)
            curY += TEXT_SIZE + 4f

            val itemName = Utils.formatHeldItem(heldId)
            ctx.text(itemName, x + (w - ctx.textWidth(itemName, TEXT_SIZE)) / 2f, curY, TEXT_SIZE, COL_WHITE)
            curY += TEXT_SIZE + 4f

            val iconSize = 20f
            ctx.item(RepoItemsAPI.getItem(heldId), x + (w - iconSize) / 2f, curY, iconSize)
            curY += iconSize + 8f
        }

        if (pet.candyUsed > 0) {
            ctx.formattedText("§7Candy: §6${pet.candyUsed}§7/10", x + PADDING, curY, TEXT_SIZE)
            curY += TEXT_SIZE + 6f
        }

        if (pet.active) {
            val badge = "§a● Active"
            ctx.formattedText(badge, x + (w - ctx.formattedTextWidth(badge, TEXT_SIZE)) / 2f, curY, TEXT_SIZE)
        }
    }

    override fun onClick(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        val pets = sortedPets()
        val x    = PVLayout.MAIN_X
        val y    = PVLayout.MAIN_Y
        val w    = PVLayout.MAIN_W
        val h    = PVLayout.MAIN_H

        val infoW    = w * INFO_RATIO
        val gridW    = w - infoW - PADDING
        val slotSize = (gridW - PADDING - SLOT_SPACING * (COLS - 1)) / COLS
        val visibleRows = ((h - PADDING * 2f) / (slotSize + SLOT_SPACING)).toInt()

        val startIndex = PVState.petsScroll * COLS
        val endIndex   = (startIndex + (visibleRows + 1) * COLS).coerceAtMost(pets.size)

        for (idx in startIndex until endIndex) {
            val col = idx % COLS
            val row = idx / COLS - PVState.petsScroll
            val sx  = x + PADDING + col * (slotSize + SLOT_SPACING)
            val sy  = y + PADDING + row * (slotSize + SLOT_SPACING)
            if (ctx.isHovered(mouseX, mouseY, sx, sy, slotSize, slotSize)) {
                PVState.selectedPetIndex = if (PVState.selectedPetIndex == idx) -1 else idx
                return
            }
        }
    }

    override fun onScroll(delta: Double) {
        PVState.petsScroll = (PVState.petsScroll - delta.toInt()).coerceAtLeast(0)
    }
}