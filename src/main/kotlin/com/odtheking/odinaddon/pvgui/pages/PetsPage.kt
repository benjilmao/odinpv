package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.dsl.ButtonsDsl
import com.odtheking.odinaddon.pvgui.dsl.buttons
import com.odtheking.odinaddon.pvgui.dsl.formattedText
import com.odtheking.odinaddon.pvgui.dsl.itemGrid
import com.odtheking.odinaddon.pvgui.utils.LevelUtils
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import com.odtheking.odinaddon.pvgui.utils.coloredName
import com.odtheking.odinaddon.pvgui.utils.commas
import com.odtheking.odinaddon.pvgui.utils.displayName
import com.odtheking.odinaddon.pvgui.utils.heldItemStack
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import com.odtheking.odinaddon.pvgui.utils.toItemStack
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import kotlin.math.ceil
import kotlin.math.floor

object PetsPage : PVPage() {
    override val name = "Pets"

    private val SP get() = 10f
    private val COLS = 9
    private val PAGE_SIZE = 81
    private val RARITY_ORDER = listOf("MYTHIC", "LEGENDARY", "EPIC", "RARE", "UNCOMMON", "COMMON")

    private val panelW get() = w * 0.38f
    private val gridX  get() = x + panelW + SP
    private val gridW  get() = w - panelW - SP
    private val pageBtnH get() = (gridW - SP * 16f) / 18f

    private val pets: List<HypixelData.Pet> by resettableLazy {
        PVState.member()?.pets?.pets?.sortedWith(
            compareBy(
                { RARITY_ORDER.indexOf(it.tier.uppercase()).takeIf { i -> i >= 0 } ?: RARITY_ORDER.size },
                { -LevelUtils.getPetLevel(it.exp, SkyBlockRarity.fromNameOrNull(it.tier) ?: SkyBlockRarity.COMMON, it.type) },
                { -it.exp },
            )
        ) ?: emptyList()
    }

    private val pages get() = ceil(pets.size / PAGE_SIZE.toDouble()).toInt().coerceAtLeast(1)
    private var pageButtons: ButtonsDsl<Int>? = null

    override fun onOpen() {
        pageButtons = null
        PVState.selectedPet = -1
    }

    override fun draw() {
        val member = PVState.member()
        if (member == null) { centeredText("No data loaded", Theme.textSecondary); return }

        drawInfoPanel()
        if (pets.isEmpty()) return

        val btnY = y
        val btns = getOrBuildPageButtons(btnY).also { it.draw() }
        val page = (btns.selected ?: 1) - 1
        val pagePets = subset(pets, page, PAGE_SIZE)
        val pageStacks = pagePets.map { it.toItemStack() }

        val gridTopY = btnY + pageBtnH + SP
        val gap = SP / 2f
        val slotSize = (gridW - gap * (COLS - 1)) / COLS
        val rows = ceil(pagePets.size / COLS.toDouble()).toInt()
        val gridH = rows * slotSize + (rows - 1).coerceAtLeast(0) * gap
        val gy = gridTopY + ((y + h - gridTopY) - gridH) / 2f

        val selectedGlobal = if (PVState.selectedPet >= 0) PVState.selectedPet
        else pets.indexOfFirst { it.active }.takeIf { it >= 0 } ?: -1

        itemGrid(
            x = gridX, y = gy,
            cols = COLS, slotSize = slotSize, gap = gap,
            items = { pageStacks },
            colorHandler = { _, idx ->
                val globalIdx = page * PAGE_SIZE + idx
                val pet = pagePets.getOrNull(idx)
                when {
                    globalIdx == selectedGlobal -> Theme.btnSelected
                    pet?.active == true -> 0xFF1A6A3A.toInt()
                    ProfileViewerModule.rarityBackgrounds && pet != null -> Theme.rarityColor(pet.tier)
                    else -> Theme.slotBg
                }
            },
        ).draw()
    }

    override fun click(mouseX: Double, mouseY: Double): Boolean {
        if (pageButtons?.click(mouseX, mouseY) == true) return true

        val btns = pageButtons ?: return false
        val page = (btns.selected ?: 1) - 1
        val pagePets = subset(pets, page, PAGE_SIZE)

        val btnY = y
        val gridTopY = btnY + pageBtnH + SP
        val gap = SP / 2f
        val slotSize = (gridW - gap * (COLS - 1)) / COLS
        val rows = ceil(pagePets.size / COLS.toDouble()).toInt()
        val gridH = rows * slotSize + (rows - 1).coerceAtLeast(0) * gap
        val gy = gridTopY + ((y + h - gridTopY) - gridH) / 2f

        pagePets.forEachIndexed { idx, _ ->
            val col = idx % COLS
            val row = idx / COLS
            val sx = gridX + col * (slotSize + gap)
            val sy = gy + row * (slotSize + gap)
            if (PVState.isHovered(sx, sy, slotSize, slotSize)) {
                val globalIdx = page * PAGE_SIZE + idx
                PVState.selectedPet = if (PVState.selectedPet == globalIdx) -1 else globalIdx
                return true
            }
        }
        return false
    }

    private fun drawInfoPanel() {
        NVGRenderer.rect(x, y, panelW, h, Theme.slotBg, Theme.radius)

        val selectedGlobal = if (PVState.selectedPet >= 0) PVState.selectedPet
        else pets.indexOfFirst { it.active }.takeIf { it >= 0 } ?: -1
        val pet = pets.getOrNull(selectedGlobal)
        val font = NVGRenderer.defaultFont
        val pad = SP
        val ts = 15f

        if (pet == null) {
            val msg = "No pet selected"
            val tw = NVGRenderer.textWidth(msg, ts, font)
            NVGRenderer.text(msg, x + (panelW - tw) / 2f, y + h / 2f - ts / 2f, ts, Theme.textSecondary, font)
            return
        }

        val rar = SkyBlockRarity.fromNameOrNull(pet.tier) ?: SkyBlockRarity.COMMON
        val level = LevelUtils.getPetLevel(pet.exp, rar, pet.type).toInt()
        val progress = LevelUtils.getPetProgress(pet.exp, rar, pet.type)
        val cap = if (pet.type.uppercase() in setOf("GOLDEN_DRAGON", "JADE_DRAGON", "ROSE_DRAGON")) 200 else 100

        val iconSize = (panelW * 0.45f).coerceAtMost(56f)
        val iconX = x + (panelW - iconSize) / 2f
        val iconY = y + pad
        itemGrid(
            x = iconX, y = iconY,
            cols = 1, slotSize = iconSize, gap = 0f,
            items = { listOf(pet.toItemStack()) },
            colorHandler = { _, _ -> 0 },
        ).draw()

        var cy = iconY + iconSize + pad

        val name = "${Theme.rarityPrefix(pet.tier)}${pet.displayName}"
        val ntw = NVGRenderer.textWidth(name.replace(Regex("§."), ""), ts + 1f, font)
        formattedText(name, x + (panelW - ntw) / 2f, cy, ts + 1f)
        cy += ts + 6f

        val barW = panelW - pad * 2f
        val barH = 6f
        NVGRenderer.rect(x + pad, cy, barW, barH, Theme.bg, 3f)
        NVGRenderer.rect(x + pad, cy, barW * progress, barH, Theme.btnSelected, 3f)
        cy += barH + 4f

        val pctStr = if (level >= cap) "MAX" else "${"%.1f".format(progress * 100f)}%"
        val ptw = NVGRenderer.textWidth(pctStr, 11f, font)
        NVGRenderer.text(pctStr, x + (panelW - ptw) / 2f, cy, 11f, Theme.textSecondary, font)
        cy += 13f

        NVGRenderer.rect(x + pad, cy, barW, 1f, Theme.separator, 0f)
        cy += 8f

        val lines = buildList {
            add("§7Level§8: ${Theme.rarityPrefix(pet.tier)}$level§7/$cap")
            add("§7XP§8: §f${pet.exp.toLong().commas}")
            if (pet.active) add("§a● Active")
            if (pet.candyUsed > 0) add("§7Candy§8: §6${pet.candyUsed}§7/10")
            pet.skin?.let { add("§7Skin§8: §d${it.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() }}") }
        }
        lines.forEach { line ->
            formattedText(line, x + pad, cy, ts)
            cy += ts + 4f
        }

        val heldStack: ItemStack? = pet.heldItemStack
        if (heldStack != null && !heldStack.isEmpty) {
            cy += 4f
            NVGRenderer.rect(x + pad, cy, barW, 1f, Theme.separator, 0f)
            cy += 8f
            val heldLabel = "§7Held Item§8:"
            formattedText(heldLabel, x + pad, cy, ts)
            cy += ts + 6f
            val heldSize = (barW).coerceAtMost(48f)
            val heldX = x + (panelW - heldSize) / 2f
            itemGrid(
                x = heldX, y = cy,
                cols = 1, slotSize = heldSize, gap = 0f,
                items = { listOf(heldStack) },
                colorHandler = { _, _ -> Theme.slotBg },
            ).draw()
        }
    }

    private fun getOrBuildPageButtons(btnY: Float): ButtonsDsl<Int> {
        val ex = pageButtons
        if (ex != null && ex.items.size == pages) {
            ex.x = gridX; ex.y = btnY; ex.w = gridW; ex.h = pageBtnH
            return ex
        }
        return buttons(
            x = gridX, y = btnY, w = gridW, h = pageBtnH,
            items = (1..pages).toList(),
            vertical = false,
            spacing = SP / 2f,
            textSize = 14f,
            radius = Theme.radius,
            label = { it.toString() },
        ) {}.also { it.selected = 1; pageButtons = it }
    }

    private fun <T> subset(list: List<T>, page: Int, size: Int): List<T> {
        val start = page * size
        return if (start >= list.size) emptyList()
        else list.subList(start, minOf(start + size, list.size))
    }
}