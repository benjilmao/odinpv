package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.dsl.formattedText
import com.odtheking.odinaddon.pvgui.nodes.ItemGridNode
import com.odtheking.odinaddon.pvgui.nodes.PagerNode
import com.odtheking.odinaddon.pvgui.utils.LevelUtils
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import com.odtheking.odinaddon.pvgui.utils.coloredName
import com.odtheking.odinaddon.pvgui.utils.commas
import com.odtheking.odinaddon.pvgui.utils.displayName
import com.odtheking.odinaddon.pvgui.utils.heldItemStack
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import com.odtheking.odinaddon.pvgui.utils.toItemStack
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity

object PetsPage : PVPage() {
    override val name = "Pets"

    private val spacing = 10f
    private val columns = 9
    private val pageSize = 81
    private val rarityOrder = listOf("MYTHIC", "LEGENDARY", "EPIC", "RARE", "UNCOMMON", "COMMON")

    private val panelWidth get() = w * 0.38f
    private val gridX get() = x + panelWidth + spacing
    private val gridWidth get() = w - panelWidth - spacing

    private val pets: List<HypixelData.Pet> by resettableLazy {
        PVState.member()?.pets?.pets?.sortedWith(
            compareBy(
                { rarityOrder.indexOf(it.tier.uppercase()).takeIf { i -> i >= 0 } ?: rarityOrder.size },
                { -LevelUtils.getPetLevel(it.exp, SkyBlockRarity.fromNameOrNull(it.tier) ?: SkyBlockRarity.COMMON, it.type) },
                { -it.exp },
            )
        ) ?: emptyList()
    }

    private val pager = PagerNode(
        pageItems = { pets },
        pageSize = pageSize,
        spacing = spacing,
        content = { page, pagePets, px, py, pw, ph, context, mouseX, mouseY ->
            buildGrid(page, pagePets).also { it.setBounds(px, py, pw, ph) }.draw(context, mouseX, mouseY)
        },
        onEnqueueItems = { page, pagePets, px, py, pw, ph, context, mouseX, mouseY ->
            buildGrid(page, pagePets).also { it.setBounds(px, py, pw, ph) }.enqueueItems(context, mouseX, mouseY)
        },
        onContentClick = { page, pagePets, mouseX, mouseY, px, py, pw, ph ->
            buildGrid(page, pagePets).also { it.setBounds(px, py, pw, ph) }.click(mouseX, mouseY)
        },
    )

    override fun onOpen() {
        pager.reset()
        PVState.selectedPet = -1
    }

    override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (PVState.member() == null) { centeredText("No data loaded", Theme.textSecondary); return }
        drawInfoPanel(context, mouseX, mouseY)
        if (pets.isEmpty()) return
        pager.draw(gridX, y, gridWidth, h, context, mouseX, mouseY)
    }

    override fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (PVState.member() == null || pets.isEmpty()) return
        enqueueInfoPanelItems(context, mouseX, mouseY)
        pager.enqueueItems(gridX, y, gridWidth, h, context, mouseX, mouseY)
    }

    override fun click(mouseX: Double, mouseY: Double): Boolean =
        pager.click(mouseX, mouseY, gridX, y, gridWidth, h)

    private fun resolveSelected(): Int =
        if (PVState.selectedPet >= 0) PVState.selectedPet
        else pets.indexOfFirst { it.active }.takeIf { it >= 0 } ?: -1

    private fun buildGrid(page: Int, pagePets: List<HypixelData.Pet>): ItemGridNode {
        val stacks = pagePets.map { it.toItemStack() }
        val selectedGlobal = resolveSelected()
        return ItemGridNode(
            columns = columns, gap = spacing / 2f,
            items = { stacks },
            colors = { _, index ->
                val globalIndex = page * pageSize + index
                val pet = pagePets.getOrNull(index)
                when {
                    globalIndex == selectedGlobal -> Theme.btnSelected
                    pet?.active == true -> 0xFF1A6A3A.toInt()
                    ProfileViewerModule.rarityBackgrounds && pet != null -> Theme.rarityColor(pet.tier)
                    else -> Theme.slotBg
                }
            },
            onSlotClick = { _, index ->
                val globalIndex = page * pageSize + index
                PVState.selectedPet = if (PVState.selectedPet == globalIndex) -1 else globalIndex
                true
            },
        )
    }

    private fun drawInfoPanel(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        NVGRenderer.rect(x, y, panelWidth, h, Theme.slotBg, Theme.radius)
        val pet = pets.getOrNull(resolveSelected())
        val font = NVGRenderer.defaultFont
        val pad = spacing
        val textSize = 15f
        if (pet == null) {
            val message = "No pet selected"
            val messageWidth = NVGRenderer.textWidth(message, textSize, font)
            NVGRenderer.text(message, x + (panelWidth - messageWidth) / 2f, y + h / 2f - textSize / 2f, textSize, Theme.textSecondary, font)
            return
        }
        val rarity = SkyBlockRarity.fromNameOrNull(pet.tier) ?: SkyBlockRarity.COMMON
        val level = LevelUtils.getPetLevel(pet.exp, rarity, pet.type).toInt()
        val progress = LevelUtils.getPetProgress(pet.exp, rarity, pet.type)
        val cap = if (pet.type.uppercase() in setOf("GOLDEN_DRAGON", "JADE_DRAGON", "ROSE_DRAGON")) 200 else 100
        val iconSize = (panelWidth * 0.45f).coerceAtMost(56f)
        val iconY = y + pad
        var currentY = iconY + iconSize + pad
        val petName = "${Theme.rarityPrefix(pet.tier)}${pet.displayName}"
        val nameWidth = NVGRenderer.textWidth(petName.replace(Regex("§."), ""), textSize + 1f, font)
        formattedText(petName, x + (panelWidth - nameWidth) / 2f, currentY, textSize + 1f)
        currentY += textSize + 6f
        val barWidth = panelWidth - pad * 2f
        val barHeight = 6f
        NVGRenderer.rect(x + pad, currentY, barWidth, barHeight, Theme.bg, 3f)
        NVGRenderer.rect(x + pad, currentY, barWidth * progress, barHeight, Theme.btnSelected, 3f)
        currentY += barHeight + 4f
        val percentString = if (level >= cap) "MAX" else "${"%.1f".format(progress * 100f)}%"
        val percentWidth = NVGRenderer.textWidth(percentString, 11f, font)
        NVGRenderer.text(percentString, x + (panelWidth - percentWidth) / 2f, currentY, 11f, Theme.textSecondary, font)
        currentY += 13f
        NVGRenderer.rect(x + pad, currentY, barWidth, 1f, Theme.separator, 0f)
        currentY += 8f
        val infoLines = buildList {
            add("§7Level§8: ${Theme.rarityPrefix(pet.tier)}$level§7/$cap")
            add("§7XP§8: §f${pet.exp.toLong().commas}")
            if (pet.active) add("§a● Active")
            if (pet.candyUsed > 0) add("§7Candy§8: §6${pet.candyUsed}§7/10")
            pet.skin?.let { add("§7Skin§8: §d${it.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() }}") }
        }
        infoLines.forEach { line ->
            formattedText(line, x + pad, currentY, textSize)
            currentY += textSize + 4f
        }
        val heldStack: ItemStack? = pet.heldItemStack
        if (heldStack != null && !heldStack.isEmpty) {
            currentY += 4f
            NVGRenderer.rect(x + pad, currentY, barWidth, 1f, Theme.separator, 0f)
            currentY += 8f
            formattedText("§7Held Item§8:", x + pad, currentY, textSize)
        }
    }

    private fun enqueueInfoPanelItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val pet = pets.getOrNull(resolveSelected()) ?: return
        val pad = spacing
        val iconSize = (panelWidth * 0.45f).coerceAtMost(56f)
        val iconX = x + (panelWidth - iconSize) / 2f
        val iconY = y + pad
        ItemGridNode(columns = 1, gap = 0f, items = { listOf(pet.toItemStack()) }, colors = { _, _ -> 0 })
            .also { it.setBounds(iconX, iconY, iconSize, iconSize) }.enqueueItems(context, mouseX, mouseY)
        val heldStack: ItemStack? = pet.heldItemStack
        if (heldStack != null && !heldStack.isEmpty) {
            val rarity = SkyBlockRarity.fromNameOrNull(pet.tier) ?: SkyBlockRarity.COMMON
            val level = LevelUtils.getPetLevel(pet.exp, rarity, pet.type).toInt()
            val progress = LevelUtils.getPetProgress(pet.exp, rarity, pet.type)
            val cap = if (pet.type.uppercase() in setOf("GOLDEN_DRAGON", "JADE_DRAGON", "ROSE_DRAGON")) 200 else 100
            val textSize = 15f
            val barWidth = panelWidth - pad * 2f
            var currentY = iconY + iconSize + pad + textSize + 6f + 6f + 4f + 13f + 8f
            val lines = buildList {
                add(Unit); add(Unit)
                if (pet.active) add(Unit)
                if (pet.candyUsed > 0) add(Unit)
                pet.skin?.let { add(Unit) }
            }
            currentY += lines.size * (textSize + 4f) + 4f + 8f + textSize + 6f
            val heldSize = barWidth.coerceAtMost(48f)
            val heldX = x + (panelWidth - heldSize) / 2f
            ItemGridNode(columns = 1, gap = 0f, items = { listOf(heldStack) }, colors = { _, _ -> Theme.slotBg })
                .also { it.setBounds(heldX, currentY, heldSize, heldSize) }.enqueueItems(context, mouseX, mouseY)
        }
    }
}