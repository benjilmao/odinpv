package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Color
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.utils.HypixelData
import com.odtheking.odinaddon.pvgui.utils.LevelUtils
import com.odtheking.odinaddon.pvgui.utils.Utils
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.PageHandler
import com.odtheking.odinaddon.pvgui.PVLayout
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.Theme.rarityColor
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import tech.thatgravyboat.skyblockapi.api.remote.PetQuery
import tech.thatgravyboat.skyblockapi.api.remote.RepoPetsAPI
import tech.thatgravyboat.skyblockapi.api.remote.RepoItemsAPI

object PetsPage : PageHandler {
    override val name = "Pets"
    private const val PADDING = 8f
    private const val SLOT_SPACING = 4f
    private const val INFO_RATIO = 0.30f
    private const val COLS = 9
    private const val TEXT_SIZE  = 14f
    private const val INFO_TEXT = 12f
    private val SLOT_RADIUS get() = Theme.round
    private val rarityOrder = listOf("MYTHIC", "LEGENDARY", "EPIC", "RARE", "UNCOMMON", "COMMON")

    override fun onOpen() {
        PVState.petsScroll = 0
        PVState.selectedPetIndex = -1
    }

    private fun sortedPets(): List<HypixelData.Pet> =
        PVState.memberData()?.pets?.pets?.sortedWith(compareBy(
            { rarityOrder.indexOf(it.tier.uppercase()).takeIf { i -> i >= 0 } ?: rarityOrder.size },
            { -LevelUtils.getPetLevel(it.exp, rarity(it), it.type) },
            { -it.exp },
        )) ?: emptyList()

    private fun rarity(pet: HypixelData.Pet) =
        SkyBlockRarity.fromNameOrNull(pet.tier) ?: SkyBlockRarity.COMMON

    private fun resolvedIndex(pets: List<HypixelData.Pet>): Int {
        if (PVState.selectedPetIndex >= 0) return PVState.selectedPetIndex
        return pets.indexOfFirst { it.active }.takeIf { it >= 0 } ?: -1
    }

    override fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, mouseX: Double, mouseY: Double) {
        val pets = sortedPets()
        val infoW = w * INFO_RATIO
        val gridW = w - infoW - PADDING
        val slotSize = (gridW - PADDING - SLOT_SPACING * (COLS - 1)) / COLS

        val totalRows = (pets.size + COLS - 1) / COLS
        val visibleRows = ((h - PADDING * 2f) / (slotSize + SLOT_SPACING)).toInt()
        PVState.petsScroll = PVState.petsScroll.coerceIn(0, (totalRows - visibleRows).coerceAtLeast(0))

        val selectedIdx = resolvedIndex(pets)

        ctx.pushScissor(x, y, gridW + PADDING, h)
        val startIndex = PVState.petsScroll * COLS
        val endIndex = (startIndex + (visibleRows + 1) * COLS).coerceAtMost(pets.size)

        for (idx in startIndex until endIndex) {
            val pet = pets[idx]
            val sx = x + PADDING + (idx % COLS) * (slotSize + SLOT_SPACING)
            val sy = y + PADDING + (idx / COLS - PVState.petsScroll) * (slotSize + SLOT_SPACING)

            if (sy + slotSize < y || sy > y + h) continue
            if (pet.active) ctx.rect(sx, sy, slotSize, slotSize, Color(0, 180, 70), SLOT_RADIUS)
            else ctx.rect(sx, sy, slotSize, slotSize,
                if (ProfileViewerModule.rarityBackgrounds) rarityColor(pet.tier) else Theme.btnNormal,
                SLOT_RADIUS)

            if (idx == selectedIdx) ctx.hollowRect(sx, sy, slotSize, slotSize, 2f, Color(255, 255, 255, 0.9f), SLOT_RADIUS)

            val itemPad = slotSize * 0.05f
            ctx.item(
                RepoPetsAPI.getPetAsItem(PetQuery(
                    pet.type, rarity(pet),
                    LevelUtils.getPetLevel(pet.exp, rarity(pet), pet.type).toInt(),
                    pet.skin, pet.heldItem,
                )),
                sx + itemPad, sy + itemPad, slotSize - itemPad * 2f,
            )
        }
        ctx.popScissor()

        ctx.line(x + gridW + PADDING, y + 4f, x + gridW + PADDING, y + h - 4f, 1f, Color(255, 255, 255, 0.15f))
        drawInfoPanel(ctx, x + gridW + PADDING * 2f, y, infoW - PADDING, h, pets, selectedIdx)
    }

    private fun drawInfoPanel(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, pets: List<HypixelData.Pet>, selectedIdx: Int) {
        ctx.hollowRect(x, y, w, h, 1f, Color(255, 255, 255, 0.12f), 6f)

        if (selectedIdx < 0 || selectedIdx >= pets.size) {
            val msg = "No active pet"
            ctx.text(msg, x + (w - ctx.textWidth(msg, INFO_TEXT)) / 2f, y + h / 2f - INFO_TEXT / 2f, INFO_TEXT, Color(170, 170, 170))
            return
        }

        val pet = pets[selectedIdx]
        val rar = rarity(pet)
        val level = LevelUtils.getPetLevel(pet.exp, rar, pet.type).toInt()
        val progress = LevelUtils.getPetProgress(pet.exp, rar, pet.type)
        val prefix = Theme.rarityPrefix(pet.tier)
        var curY = y + PADDING

        val nameStr = "$prefix${pet.type.lowercase().split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }}"
        ctx.formattedText(nameStr, x + (w - ctx.formattedTextWidth(nameStr, INFO_TEXT + 1f)) / 2f, curY, INFO_TEXT + 1f)
        curY += INFO_TEXT + 6f

        ctx.formattedText("§7Level: $prefix$level", x + PADDING, curY, INFO_TEXT)
        curY += INFO_TEXT + 4f

        val barW = w - PADDING * 2f
        ctx.rect(x + PADDING, curY, barW, 6f, Color(40, 40, 40), 3f)
        ctx.rect(x + PADDING, curY, barW * progress, 6f, Color(80, 160, 255), 3f)
        curY += 10f

        ctx.formattedText("§7XP: §f${Utils.truncate(pet.exp.toLong())}", x + PADDING, curY, TEXT_SIZE)
        curY += TEXT_SIZE + 8f

        ctx.line(x + PADDING, curY, x + w - PADDING, curY, 1f, Color(255, 255, 255, 0.15f))
        curY += 8f

        pet.heldItem?.let { heldId ->
            ctx.formattedText("§7Held Item:", x + PADDING, curY, TEXT_SIZE)
            curY += TEXT_SIZE + 4f
            val itemName = RepoItemsAPI.getItemName(heldId).string
            ctx.formattedText(itemName, x + (w - ctx.textWidth(itemName, TEXT_SIZE)) / 2f, curY, TEXT_SIZE)
            curY += TEXT_SIZE + 4f
            val slotSize = 48f
            val pad = slotSize * 0.05f
            ctx.rect(x + (w - slotSize) / 2f, curY, slotSize, slotSize, Color(255, 255, 255, 0.08f), 3f)
            ctx.item(RepoItemsAPI.getItem(heldId), x + (w - slotSize) / 2f + pad, curY + pad, slotSize - pad * 2f)
            curY += slotSize + 8f
        }

        val bottomLines = buildList {
            if (pet.candyUsed > 0) add("§7Candy: §6${pet.candyUsed}§7/10")
            if (pet.active) add("§a● Active")
        }
        if (bottomLines.isNotEmpty()) {
            val statsH = TEXT_SIZE * bottomLines.size + 8f
            ctx.textList(bottomLines, x + PADDING, curY, w - PADDING * 2f, statsH, maxSize = TEXT_SIZE)
        }
    }

    override fun onClick(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        val pets = sortedPets()
        val infoW = PVLayout.MAIN_W * INFO_RATIO
        val gridW = PVLayout.MAIN_W - infoW - PADDING
        val slotSize = (gridW - PADDING - SLOT_SPACING * (COLS - 1)) / COLS
        val visibleRows = ((PVLayout.MAIN_H - PADDING * 2f) / (slotSize + SLOT_SPACING)).toInt()

        val startIndex = PVState.petsScroll * COLS
        val endIndex = (startIndex + (visibleRows + 1) * COLS).coerceAtMost(pets.size)

        for (idx in startIndex until endIndex) {
            val sx = PVLayout.MAIN_X + PADDING + (idx % COLS) * (slotSize + SLOT_SPACING)
            val sy = PVLayout.MAIN_Y + PADDING + (idx / COLS - PVState.petsScroll) * (slotSize + SLOT_SPACING)
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