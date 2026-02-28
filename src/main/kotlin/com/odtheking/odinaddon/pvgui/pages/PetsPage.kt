package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Color
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.components.ItemSlot
import com.odtheking.odinaddon.pvgui.components.ProgressBar
import com.odtheking.odinaddon.pvgui.components.SlotGrid
import com.odtheking.odinaddon.pvgui.utils.LevelUtils
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.Theme.rarityColor
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import com.odtheking.odinaddon.pvgui.utils.displayName
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import com.odtheking.odinaddon.pvgui.utils.toItemStack
import com.odtheking.odinaddon.pvgui.utils.truncate
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import tech.thatgravyboat.skyblockapi.api.remote.RepoItemsAPI

object PetsPage : PVPage("Pets") {
    private const val SLOT_SPACING = 4f
    private const val INFO_RATIO = 0.30f
    private const val COLS = 9
    private const val TEXT_SIZE = 14f
    private const val INFO_TEXT = 12f
    private val rarityOrder = listOf("MYTHIC", "LEGENDARY", "EPIC", "RARE", "UNCOMMON", "COMMON")

    private val cachedPets: List<HypixelData.Pet> by resettableLazy {
        member?.pets?.pets?.sortedWith(compareBy(
            { rarityOrder.indexOf(it.tier.uppercase()).takeIf { i -> i >= 0 } ?: rarityOrder.size },
            { -LevelUtils.getPetLevel(it.exp, rarity(it), it.type) },
            { -it.exp },
        )) ?: emptyList()
    }

    private val petGrid: SlotGrid<HypixelData.Pet> by resettableLazy {
        val infoW = mainW * INFO_RATIO
        val gridW = mainW - infoW - padding
        SlotGrid(
            x = mainX,
            y = mainY,
            w = gridW,
            h = mainH,
            items = cachedPets,
            cols = COLS,
            spacing = SLOT_SPACING,
            scroll = { PVState.petsScroll },
            toItemStack = { it.toItemStack() },
            slotColor = { pet ->
                when {
                    pet.active -> Color(0, 180, 70)
                    ProfileViewerModule.rarityBackgrounds -> rarityColor(pet.tier)
                    else -> Theme.btnNormal
                }
            },
            selectedIndex = { resolvedIndex(cachedPets) },
        ) {
            onSlotClick { idx, _ ->
                PVState.selectedPetIndex = if (PVState.selectedPetIndex == idx) -1 else idx
            }
        }
    }

    private fun rarity(pet: HypixelData.Pet) =
        SkyBlockRarity.fromNameOrNull(pet.tier) ?: SkyBlockRarity.COMMON

    private fun resolvedIndex(pets: List<HypixelData.Pet>): Int {
        if (PVState.selectedPetIndex >= 0) return PVState.selectedPetIndex
        return pets.indexOfFirst { it.active }.takeIf { it >= 0 } ?: -1
    }

    override fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, mouseX: Double, mouseY: Double) {
        val pets = cachedPets
        val infoW = w * INFO_RATIO
        val gridW = w - infoW - padding
        val slotSize = (gridW - padding - SLOT_SPACING * (COLS - 1)) / COLS

        val totalRows = (pets.size + COLS - 1) / COLS
        val visibleRows = ((h - padding * 2f) / (slotSize + SLOT_SPACING)).toInt()
        PVState.petsScroll = PVState.petsScroll.coerceIn(0, (totalRows - visibleRows).coerceAtLeast(0))

        petGrid.draw(ctx, mouseX, mouseY)

        ctx.line(x + gridW + padding, y + 4f, x + gridW + padding, y + h - 4f, 1f, Color(255, 255, 255, 0.15f))
        drawInfoPanel(ctx, x + gridW + padding * 2f, y, infoW - padding, h, pets, resolvedIndex(pets), mouseX, mouseY)
    }

    private fun drawInfoPanel(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, pets: List<HypixelData.Pet>, selectedIdx: Int, mouseX: Double, mouseY: Double) {
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
        var curY = y + padding

        val nameStr = "${Theme.rarityPrefix(pet.tier)}${pet.displayName}"
        ctx.formattedText(nameStr, x + (w - ctx.formattedTextWidth(nameStr, INFO_TEXT + 1f)) / 2f, curY, INFO_TEXT + 1f)
        curY += INFO_TEXT + 6f

        ctx.formattedText("§7Level: $prefix$level", x + padding, curY, INFO_TEXT)
        curY += INFO_TEXT + 4f

        ProgressBar(x + padding, curY, w - padding * 2f, 6f, progress.toFloat()).draw(ctx, mouseX, mouseY)
        curY += 10f

        ctx.formattedText("§7XP: §f${pet.exp.toLong().truncate}", x + padding, curY, TEXT_SIZE)
        curY += TEXT_SIZE + 8f

        ctx.line(x + padding, curY, x + w - padding, curY, 1f, Color(255, 255, 255, 0.15f))
        curY += 8f

        pet.heldItem?.let { heldId ->
            ctx.formattedText("§7Held Item:", x + padding, curY, TEXT_SIZE)
            curY += TEXT_SIZE + 4f
            val slotSize = (w - padding * 2f).coerceAtMost(48f)

            ItemSlot(
                x + (w - slotSize) / 2f, curY, slotSize,
                RepoItemsAPI.getItem(heldId),
            ).draw(ctx, mouseX, mouseY)
            curY += slotSize + 8f
        }

        if (pet.candyUsed > 0) {
            ctx.formattedText("§7Candy: §6${pet.candyUsed}§7/10", x + padding, curY, TEXT_SIZE)
            curY += TEXT_SIZE + 4f
        }
        if (pet.active) {
            ctx.formattedText("§a● Active", x + padding, curY, TEXT_SIZE)
        }
    }

    override fun onClick(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        petGrid.click(ctx, mouseX, mouseY)
    }

    override fun onScroll(delta: Double) {
        PVState.petsScroll = (PVState.petsScroll - delta.toInt()).coerceAtLeast(0)
    }
}