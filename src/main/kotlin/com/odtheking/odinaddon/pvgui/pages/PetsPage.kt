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
import com.odtheking.odinaddon.pvgui.dsl.RenderQueue
import com.odtheking.odinaddon.pvgui.dsl.buttons
import com.odtheking.odinaddon.pvgui.dsl.ButtonsDsl
import com.odtheking.odinaddon.pvgui.dsl.fillText
import com.odtheking.odinaddon.pvgui.utils.LevelUtils
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import com.odtheking.odinaddon.pvgui.utils.coloredName
import com.odtheking.odinaddon.pvgui.utils.heldItemStack
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import com.odtheking.odinaddon.pvgui.utils.toItemStack
import net.minecraft.client.gui.GuiGraphics
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import kotlin.math.ceil

object PetsPage : PVPage() {
    override val name = "Pets"

    // ── HC-mirrored geometry ──────────────────────────────────────────────────
    // activePetBox  = Box(mainX, spacer, mainWidth, mainHeight*0.1) → h = 54
    private val activePetH = MAIN_H * 0.1f     // 54

    // pageButtonRow = Box(mainX, activePetH+spacer, mainWidth, INV_BTN_H) → 34.1
    // petsStart     = activePetH + INV_BTN_H + 2*spacer = 118.1
    private val petsStart = CONTENT_Y + activePetH + INV_BTN_H + 2f * PAD   // 118.1

    // Available height for the icon grid
    private val availH = MAIN_H - (petsStart - CONTENT_Y)    // 431.9

    // ── Icon grid: 9 cols, 7 rows, square slots derived from availH ───────────
    // 7*(slot+gap) - gap = availH  →  slot = (availH+gap-7*gap)/7 = 53.1px
    private const val COLS    = 9
    private const val ROWS    = 7
    private val SLOT_GAP      = PAD
    private val SLOT_SIZE     = (availH + SLOT_GAP - ROWS * SLOT_GAP) / ROWS  // 53.1px
    private val OVERLAY_SIZE  = SLOT_SIZE * 0.38f   // held-item overlay in bottom-right

    // Grid centred horizontally in mainWidth
    private val gridW = COLS * SLOT_SIZE + (COLS - 1) * SLOT_GAP   // 558.1
    private val gridX = CONTENT_X + (MAIN_W - gridW) / 2f          // 323.9

    // Vertical: grid origin = petsStart (fills exactly to bottom)
    private val gridOriginY = petsStart

    private val pageSize = COLS * ROWS   // 63 per page

    // ── Data ──────────────────────────────────────────────────────────────────
    private val pets: List<HypixelData.Pet> by resettableLazy {
        PVState.member()?.pets?.pets?.sortedWith(
            compareByDescending<HypixelData.Pet> { rarityOrder.indexOf(it.tier.uppercase()).let { i -> if (i < 0) rarityOrder.size else i }.let { i -> -i } }
                .thenByDescending { LevelUtils.getPetLevel(it.exp, SkyBlockRarity.fromNameOrNull(it.tier) ?: SkyBlockRarity.COMMON, it.type) }
                .thenByDescending { it.exp }
        ) ?: emptyList()
    }

    private val activePetDisplay: String by resettableLazy {
        PVState.member()?.pets?.activePet?.coloredName ?: "§7None!"
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
        ) { /* page index drives grid */ }
    }

    override fun onOpen() { /* resettable lazy auto-resets */ }

    // ── Draw ──────────────────────────────────────────────────────────────────
    override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val member = PVState.member()
        if (member == null) { centeredText("No data loaded", Theme.textSecondary); return }

        // Active pet display box
        NVGRenderer.rect(CONTENT_X, CONTENT_Y, MAIN_W, activePetH, Theme.slotBg, Theme.radius)
        fillText(
            activePetDisplay,
            CONTENT_X + MAIN_W / 2f,
            CONTENT_Y + activePetH / 2f,
            MAIN_W - PAD * 2f,
            activePetH - PAD * 2f,
            Theme.textPrimary,
        )

        // Page buttons (only if more than one page)
        if (pages > 1) pageButtons.draw()

        // Draw INDIVIDUAL slot backgrounds — NO big background rect
        // Each slot gets its own NVGRenderer.rect() so slots are visually distinct
        val pageIdx    = (pageButtons.selected ?: 1) - 1
        val pageStart  = pageIdx * pageSize
        val pagePets   = if (pageStart < pets.size) pets.subList(pageStart, minOf(pageStart + pageSize, pets.size)) else emptyList()

        for (i in 0 until (ROWS * COLS)) {
            val pet = pagePets.getOrNull(i)
            val sx  = gridX + (i % COLS) * (SLOT_SIZE + SLOT_GAP)
            val sy  = gridOriginY + (i / COLS) * (SLOT_SIZE + SLOT_GAP)

            // Only draw slots that have pets, or draw all empty slots too for a full grid look
            val bgColor = when {
                pet?.active == true -> Colors.MINECRAFT_DARK_GREEN.rgba
                else                -> Theme.slotBg
            }
            NVGRenderer.rect(sx, sy, SLOT_SIZE, SLOT_SIZE, bgColor, Theme.slotRadius)

            // Hover highlight
            if (pet != null && PVState.isHovered(sx, sy, SLOT_SIZE, SLOT_SIZE))
                NVGRenderer.hollowRect(sx, sy, SLOT_SIZE, SLOT_SIZE, 1.5f, Theme.btnSelected, Theme.slotRadius)
        }
    }

    // ── Item rendering — called outside NVG ───────────────────────────────────
    override fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (PVState.member() == null) return

        val pageIdx   = (pageButtons.selected ?: 1) - 1
        val pageStart = pageIdx * pageSize
        val pagePets  = if (pageStart < pets.size) pets.subList(pageStart, minOf(pageStart + pageSize, pets.size)) else emptyList()

        pagePets.forEachIndexed { i, pet ->
            val sx = gridX + (i % COLS) * (SLOT_SIZE + SLOT_GAP)
            val sy = gridOriginY + (i / COLS) * (SLOT_SIZE + SLOT_GAP)

            // Pet icon (full slot size)
            val petStack = pet.toItemStack()
            if (!petStack.isEmpty)
                RenderQueue.enqueueItem(petStack, sx, sy, SLOT_SIZE)

            // Held item overlay — bottom-right corner, smaller
            val heldStack = pet.heldItemStack
            if (heldStack != null && !heldStack.isEmpty) {
                val ox = sx + SLOT_SIZE - OVERLAY_SIZE
                val oy = sy + SLOT_SIZE - OVERLAY_SIZE
                // Small dark bg so the overlay item is readable
                NVGRenderer.rect(ox - 2f, oy - 2f, OVERLAY_SIZE + 2f, OVERLAY_SIZE + 2f,
                    Theme.overlayBg, Theme.slotRadius)
                RenderQueue.enqueueItem(heldStack, ox, oy, OVERLAY_SIZE)
            }
        }
    }

    override fun click(mouseX: Double, mouseY: Double): Boolean {
        if (pages > 1) return pageButtons.click(mouseX, mouseY)
        return false
    }

    private val rarityOrder = listOf("MYTHIC","LEGENDARY","EPIC","RARE","UNCOMMON","COMMON")
}