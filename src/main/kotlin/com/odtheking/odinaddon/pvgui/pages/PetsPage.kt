package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.CONTENT_X
import com.odtheking.odinaddon.pvgui.CONTENT_Y
import com.odtheking.odinaddon.pvgui.INV_BTN_H
import com.odtheking.odinaddon.pvgui.MAIN_H
import com.odtheking.odinaddon.pvgui.MAIN_W
import com.odtheking.odinaddon.pvgui.PAD
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.dsl.ButtonsDsl
import com.odtheking.odinaddon.pvgui.dsl.buttons
import com.odtheking.odinaddon.pvgui.dsl.fillText
import com.odtheking.odinaddon.pvgui.dsl.ItemGridDsl
import com.odtheking.odinaddon.pvgui.utils.LevelUtils
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import com.odtheking.odinaddon.pvgui.utils.coloredName
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import com.odtheking.odinaddon.pvgui.utils.toItemStack
import net.minecraft.client.gui.GuiGraphics
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import kotlin.math.ceil

object PetsPage : PVPage() {
    override val name = "Pets"

    private val activePetH = MAIN_H * 0.1f
    private val petsStart = CONTENT_Y + activePetH + INV_BTN_H + 2f * PAD
    private val availH = MAIN_H - (petsStart - CONTENT_Y)

    private val COLS = 9
    private val ROWS = 7
    private val SLOT_GAP = PAD
    private val SLOT_SIZE = (availH + SLOT_GAP - ROWS * SLOT_GAP) / ROWS

    private val gridW = COLS * SLOT_SIZE + (COLS - 1) * SLOT_GAP
    private val gridX = CONTENT_X + (MAIN_W - gridW) / 2f
    private val gridCenterY = petsStart + availH / 2f

    private val pageSize = COLS * ROWS  // 63 pets per page

    private val pets: List<HypixelData.Pet> by resettableLazy {
        PVState.member()?.pets?.pets?.sortedWith(
            compareBy(
                { rarityOrder.indexOf(it.tier.uppercase()).let { i -> if (i < 0) rarityOrder.size else i } },
                { -LevelUtils.getPetLevel(it.exp, SkyBlockRarity.fromNameOrNull(it.tier) ?: SkyBlockRarity.COMMON, it.type) },
                { -it.exp },
            )
        ) ?: emptyList()
    }

    private val activePetDisplay: String by resettableLazy {
        val d = PVState.member() ?: return@resettableLazy "§7None!"
        d.pets.activePet?.coloredName ?: "§7None!"
    }

    private val pages: Int by resettableLazy {
        ceil(pets.size.toDouble() / pageSize).toInt().coerceAtLeast(1)
    }

    private val pageButtons: ButtonsDsl<Int> by resettableLazy {
        buttons(
            x = CONTENT_X, y = CONTENT_Y + activePetH + PAD,
            w = MAIN_W, h = INV_BTN_H,
            items = (1..pages).toList(), padding = PAD, vertical = false,
            textSize = 18f, radius = Theme.radius, label = { it.toString() },
        ) { cachedPage = -1; cachedGrid = null }
    }

    private var cachedPage = -1
    private var cachedGrid: ItemGridDsl? = null

    private fun getGrid(): ItemGridDsl {
        val idx = (pageButtons.selected ?: 1) - 1
        if (idx == cachedPage && cachedGrid != null) return cachedGrid!!
        val start = idx * pageSize
        val pageItems = if (start < pets.size) pets.subList(start, minOf(start + pageSize, pets.size)) else emptyList()
        val stacks = pageItems.map { it.toItemStack() }
        cachedPage = idx
        cachedGrid = ItemGridDsl(
            columns = COLS,
            gap = SLOT_GAP,
            items = { stacks },
            overrideSlotSize = SLOT_SIZE,
            colors = { _, i ->
                val pet = pageItems.getOrNull(i)
                when {
                    pet?.active == true -> Colors.MINECRAFT_DARK_GREEN.rgba
                    else -> Theme.slotBg
                }
            },
        ).also { it.setCenterBounds(gridX, gridCenterY, gridW) }
        return cachedGrid!!
    }

    override fun onOpen() {
        cachedPage = -1
        cachedGrid = null
    }

    override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val member = PVState.member()
        if (member == null) { centeredText("No data loaded", Theme.textSecondary); return }

        NVGRenderer.rect(CONTENT_X, CONTENT_Y, MAIN_W, activePetH, Theme.slotBg, Theme.radius)
        fillText(
            activePetDisplay,
            CONTENT_X + MAIN_W / 2f,
            CONTENT_Y + activePetH / 2f,
            MAIN_W - PAD * 2f,
            activePetH - PAD * 2f,
            Theme.textPrimary,
        )

        if (pages > 1) pageButtons.draw()
        NVGRenderer.rect(CONTENT_X, petsStart, MAIN_W, MAIN_H + PAD - petsStart, Theme.slotBg, Theme.radius)
        getGrid().draw(context, mouseX, mouseY)
    }

    override fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (PVState.member() == null) return
        getGrid().enqueueItems(context, mouseX, mouseY)
    }

    override fun click(mouseX: Double, mouseY: Double): Boolean {
        if (pages > 1 && pageButtons.click(mouseX, mouseY)) {
            cachedPage = -1; cachedGrid = null
            return true
        }
        return false
    }

    private val rarityOrder = listOf("MYTHIC", "LEGENDARY", "EPIC", "RARE", "UNCOMMON", "COMMON")
}