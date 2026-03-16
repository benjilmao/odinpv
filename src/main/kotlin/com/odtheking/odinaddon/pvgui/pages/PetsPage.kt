package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.dsl.formattedText
import com.odtheking.odinaddon.pvgui.nodes.ItemGridNode
import com.odtheking.odinaddon.pvgui.utils.LevelUtils
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import com.odtheking.odinaddon.pvgui.utils.commas
import com.odtheking.odinaddon.pvgui.utils.displayName
import com.odtheking.odinaddon.pvgui.utils.heldItemStack
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import com.odtheking.odinaddon.pvgui.utils.toItemStack
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object PetsPage : PVPage() {
    override val name = "Pets"

    private val spacing = 10f
    private val columns = 9
    private val rarityOrder = listOf("MYTHIC", "LEGENDARY", "EPIC", "RARE", "UNCOMMON", "COMMON")

    private val panelWidth get() = w * 0.25f
    private val gridWidth get() = w - panelWidth - spacing
    private val panelX get() = x + gridWidth + spacing
    private var scrollRow = 0

    private val pets: List<HypixelData.Pet> by resettableLazy {
        PVState.member()?.pets?.pets?.sortedWith(
            compareBy(
                { rarityOrder.indexOf(it.tier.uppercase()).takeIf { i -> i >= 0 } ?: rarityOrder.size },
                { -LevelUtils.getPetLevel(it.exp, SkyBlockRarity.fromNameOrNull(it.tier) ?: SkyBlockRarity.COMMON, it.type) },
                { -it.exp },
            )
        ) ?: emptyList()
    }

    private fun resolveSelected(): Int =
        if (PVState.selectedPet >= 0) PVState.selectedPet
        else pets.indexOfFirst { it.active }.takeIf { it >= 0 } ?: -1

    private fun slotSize(): Float {
        val fromWidth = (gridWidth - spacing / 2f * (columns - 1)) / columns
        return fromWidth
    }

    private fun visibleRows(): Int {
        val slot = slotSize()
        return max(1, ((h + spacing / 2f) / (slot + spacing / 2f)).toInt())
    }

    private fun totalRows(): Int = ceil(pets.size.toDouble() / columns).toInt()

    private fun maxScroll(): Int = max(0, totalRows() - visibleRows())

    private fun visiblePets(): List<HypixelData.Pet?> {
        val slot = slotSize()
        val rows = visibleRows()
        val start = scrollRow * columns
        val end = min(start + rows * columns, pets.size)
        return if (start >= pets.size) emptyList()
        else pets.subList(start, end)
    }

    override fun onOpen() {
        scrollRow = 0
        PVState.selectedPet = -1
    }

    override fun scroll(mouseX: Double, mouseY: Double, scrollY: Double): Boolean {
        if (mouseX < x || mouseX > x + gridWidth) return false
        scrollRow = (scrollRow - scrollY.toInt()).coerceIn(0, maxScroll())
        if (scrollY > 0) scrollRow = (scrollRow - 1).coerceAtLeast(0)
        else if (scrollY < 0) scrollRow = (scrollRow + 1).coerceAtMost(maxScroll())
        return true
    }

    override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (PVState.member() == null) { centeredText("No data loaded", Theme.textSecondary); return }
        drawScrollableGrid(context, mouseX, mouseY)
        drawScrollbar()
        drawInfoPanel(context, mouseX, mouseY)
    }

    override fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (PVState.member() == null || pets.isEmpty()) return
        enqueueScrollableGrid(context, mouseX, mouseY)
        enqueueInfoPanelItems(context, mouseX, mouseY)
    }

    override fun click(mouseX: Double, mouseY: Double): Boolean {
        if (pets.isEmpty()) return false
        val visible = visiblePets()
        val stacks = visible.map { it?.toItemStack() }
        return buildGrid(visible, stacks)
            .also { it.setBounds(x, y, gridWidth, h) }.click(mouseX, mouseY)
    }

    private fun buildGrid(visible: List<HypixelData.Pet?>, stacks: List<ItemStack?>): ItemGridNode {
        val selectedGlobal = resolveSelected()
        return ItemGridNode(
            columns = columns,
            gap = spacing / 2f,
            items = { stacks },
            colors = { _, index ->
                val globalIndex = scrollRow * columns + index
                val pet = visible.getOrNull(index)
                when {
                    globalIndex == selectedGlobal -> Theme.btnSelected
                    pet?.active == true -> 0xFF1A6A3A.toInt()
                    ProfileViewerModule.rarityBackgrounds && pet != null -> Theme.rarityColor(pet.tier)
                    else -> Theme.slotBg
                }
            },
            onSlotClick = { _, index ->
                val globalIndex = scrollRow * columns + index
                PVState.selectedPet = if (PVState.selectedPet == globalIndex) -1 else globalIndex
                true
            },
        )
    }

    private fun drawScrollableGrid(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (pets.isEmpty()) return
        val visible = visiblePets()
        val stacks = visible.map { it?.toItemStack() }
        NVGRenderer.pushScissor(x, y, gridWidth, h)
        buildGrid(visible, stacks).also { it.setBounds(x, y, gridWidth, h) }.draw(context, mouseX, mouseY)
        NVGRenderer.popScissor()
    }

    private fun enqueueScrollableGrid(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val visible = visiblePets()
        val stacks = visible.map { it?.toItemStack() }
        buildGrid(visible, stacks).also { it.setBounds(x, y, gridWidth, h) }.enqueueItems(context, mouseX, mouseY)
    }

    private fun drawScrollbar() {
        val total = totalRows()
        val visible = visibleRows()
        if (total <= visible) return
        val barX = x + gridWidth - 4f
        val trackH = h
        val thumbH = (trackH * visible / total).coerceAtLeast(20f)
        val thumbY = y + (trackH - thumbH) * scrollRow / maxScroll().coerceAtLeast(1)
        NVGRenderer.rect(barX, y, 3f, trackH, Theme.bg, 2f)
        NVGRenderer.rect(barX, thumbY, 3f, thumbH, Theme.btnSelected, 2f)
    }

    private fun drawInfoPanel(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        NVGRenderer.rect(panelX, y, panelWidth, h, Theme.slotBg, Theme.radius)
        val pet = pets.getOrNull(resolveSelected())
        val font = NVGRenderer.defaultFont
        val pad = spacing
        val textSize = 14f
        if (pet == null) {
            val message = "No pet selected"
            val mw = NVGRenderer.textWidth(message, textSize, font)
            NVGRenderer.text(message, panelX + (panelWidth - mw) / 2f, y + h / 2f - textSize / 2f, textSize, Theme.textSecondary, font)
            return
        }
        val rarity = SkyBlockRarity.fromNameOrNull(pet.tier) ?: SkyBlockRarity.COMMON
        val level = LevelUtils.getPetLevel(pet.exp, rarity, pet.type).toInt()
        val progress = LevelUtils.getPetProgress(pet.exp, rarity, pet.type)
        val cap = if (pet.type.uppercase() in setOf("GOLDEN_DRAGON", "JADE_DRAGON", "ROSE_DRAGON")) 200 else 100
        val iconSize = (panelWidth * 0.5f).coerceAtMost(52f)
        val iconY = y + pad
        var currentY = iconY + iconSize + pad
        val petName = "${Theme.rarityPrefix(pet.tier)}${pet.displayName}"
        val nameWidth = NVGRenderer.textWidth(petName.replace(Regex("§."), ""), textSize, font)
        formattedText(petName, panelX + (panelWidth - nameWidth) / 2f, currentY, textSize)
        currentY += textSize + 6f
        val barWidth = panelWidth - pad * 2f
        val barHeight = 5f
        NVGRenderer.rect(panelX + pad, currentY, barWidth, barHeight, Theme.bg, 2f)
        NVGRenderer.rect(panelX + pad, currentY, barWidth * progress, barHeight, Theme.btnSelected, 2f)
        currentY += barHeight + 3f
        val pctStr = if (level >= cap) "MAX" else "${"%.1f".format(progress * 100f)}%"
        val pctWidth = NVGRenderer.textWidth(pctStr, 10f, font)
        NVGRenderer.text(pctStr, panelX + (panelWidth - pctWidth) / 2f, currentY, 10f, Theme.textSecondary, font)
        currentY += 12f
        NVGRenderer.rect(panelX + pad, currentY, barWidth, 1f, Theme.separator, 0f)
        currentY += 7f
        val lines = buildList {
            add("§7Level§8: ${Theme.rarityPrefix(pet.tier)}$level§7/$cap")
            add("§7XP§8: §f${pet.exp.toLong().commas}")
            if (pet.active) add("§a● Active")
            if (pet.candyUsed > 0) add("§7Candy§8: §6${pet.candyUsed}§7/10")
            pet.skin?.let { add("§7Skin§8: §d${it.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() }}") }
        }
        lines.forEach { line ->
            formattedText(line, panelX + pad, currentY, textSize)
            currentY += textSize + 3f
        }
        val heldStack: ItemStack? = pet.heldItemStack
        if (heldStack != null && !heldStack.isEmpty) {
            currentY += 3f
            NVGRenderer.rect(panelX + pad, currentY, barWidth, 1f, Theme.separator, 0f)
            currentY += 7f
            formattedText("§7Held Item§8:", panelX + pad, currentY, textSize)
        }
    }

    private fun enqueueInfoPanelItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val pet = pets.getOrNull(resolveSelected()) ?: return
        val pad = spacing
        val iconSize = (panelWidth * 0.5f).coerceAtMost(52f)
        val iconX = panelX + (panelWidth - iconSize) / 2f
        val iconY = y + pad
        ItemGridNode(columns = 1, gap = 0f, items = { listOf(pet.toItemStack()) }, colors = { _, _ -> 0 })
            .also { it.setBounds(iconX, iconY, iconSize, iconSize) }.enqueueItems(context, mouseX, mouseY)
        val heldStack: ItemStack? = pet.heldItemStack
        if (heldStack != null && !heldStack.isEmpty) {
            val textSize = 14f
            val barWidth = panelWidth - pad * 2f
            val rarity = SkyBlockRarity.fromNameOrNull(pet.tier) ?: SkyBlockRarity.COMMON
            val level = LevelUtils.getPetLevel(pet.exp, rarity, pet.type).toInt()
            val cap = if (pet.type.uppercase() in setOf("GOLDEN_DRAGON", "JADE_DRAGON", "ROSE_DRAGON")) 200 else 100
            var currentY = iconY + iconSize + pad + textSize + 6f + 5f + 3f + 12f + 7f
            val lines = buildList {
                add(Unit); add(Unit)
                if (pet.active) add(Unit)
                if (pet.candyUsed > 0) add(Unit)
                pet.skin?.let { add(Unit) }
            }
            currentY += lines.size * (textSize + 3f) + 3f + 7f + textSize + 7f
            val heldSize = barWidth.coerceAtMost(44f)
            val heldX = panelX + (panelWidth - heldSize) / 2f
            ItemGridNode(columns = 1, gap = 0f, items = { listOf(heldStack) }, colors = { _, _ -> Theme.slotBg })
                .also { it.setBounds(heldX, currentY, heldSize, heldSize) }.enqueueItems(context, mouseX, mouseY)
        }
    }
}