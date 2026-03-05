package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.capitalizeWords
import com.odtheking.odin.utils.getSkyblockRarity
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.dsl.ButtonsDsl
import com.odtheking.odinaddon.pvgui.dsl.ItemQueue
import com.odtheking.odinaddon.pvgui.dsl.TextBox
import com.odtheking.odinaddon.pvgui.dsl.buttons
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import com.odtheking.odinaddon.pvgui.utils.colorize
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import kotlin.math.ceil
import kotlin.math.floor

object InventoryPage : PVPage() {
    override val name = "Inventory"

    private val SP get() = 10f

    private val SUB_PAGES = listOf("Basic", "Wardrobe", "Talismans", "Backpacks", "Ender Chest")
    private var currentSub = "Basic"

    // Layout mirrors HC exactly
    private val tabH      get() = ((h - SP * 7f) * 0.9f) / 6f
    private val startY    get() = y + tabH + SP
    private val pageBtnH  get() = (w - SP * 16f) / 18f
    private val centerY   get() = (startY + pageBtnH + SP) + (h - (startY - y + pageBtnH)) / 2f
    private val contentH  get() = h - tabH - SP

    private var tabButtons: ButtonsDsl<String>? = null
    private var subPageButtons: ButtonsDsl<Int>? = null

    fun resetState() {
        currentSub     = "Basic"
        tabButtons     = null
        subPageButtons = null
    }

    private fun buildTabs() {
        tabButtons = buttons(
            x = x, y = y, w = w, h = tabH,
            items = SUB_PAGES, vertical = false, spacing = SP,
            textSize = 15f, radius = Theme.radius, label = { it },
        ) { sub -> currentSub = sub; subPageButtons = null }.also { it.selected = currentSub }
    }

    override fun onOpen() { resetState(); buildTabs() }

    // ── Resettable data ────────────────────────────────────────────────────────

    private val invArmor        by resettableLazy { PVState.member()?.inventory?.invArmor?.itemStacks.orEmpty() }
    private val invContents     by resettableLazy { PVState.member()?.inventory?.invContents?.itemStacks.orEmpty() }
    private val equipment       by resettableLazy { PVState.member()?.inventory?.equipment?.itemStacks.orEmpty() }
    private val wardrobeContents by resettableLazy { PVState.member()?.inventory?.wardrobeContents?.itemStacks.orEmpty() }
    private val wardrobeEquipped: Int? by resettableLazy { PVState.member()?.inventory?.wardrobeEquipped }
    private val talisman        by resettableLazy {
        PVState.member()?.inventory?.bagContents?.get("talisman_bag")?.itemStacks
            ?.filterNotNull()?.sortedByDescending { it.magicalPower }.orEmpty()
    }
    private val eChest          by resettableLazy { PVState.member()?.inventory?.eChestContents?.itemStacks.orEmpty() }
    private val backpackKeys    by resettableLazy {
        PVState.member()?.inventory?.backpackContents?.keys
            ?.mapNotNull { it.toIntOrNull()?.plus(1) }?.sorted().orEmpty()
    }
    private val magicPower      by resettableLazy { PVState.member()?.magicalPower ?: 0 }
    private val taliTextLines   by resettableLazy {
        val data = PVState.member() ?: return@resettableLazy emptyList<String>()
        val power = data.accessoryBagStorage.selectedPower
        listOf(
            "§aSelected Power§7: §6${power?.capitalizeWords() ?: "§cNone!"}",
            "§5Abiphone§7: §f${data.crimsonIsle?.abiphone?.activeContacts?.size?.div(2) ?: 0}",
            "§dRift Prism§7: ${if (data.rift.access.consumedPrism) "§aObtained" else "§cMissing"}",
        ) + data.tunings.map { "§7$it" }
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    override fun draw() {
        if (tabButtons == null) buildTabs()
        tabButtons!!.also { it.x = x; it.y = y; it.w = w; it.h = tabH }
        tabButtons!!.draw()

        if (PVState.member() == null) { centeredText("No data loaded", Theme.textSecondary); return }
        if (PVState.member()?.inventoryApi == false) { centeredText("Inventory API disabled", 0xFFFF5555.toInt()); return }

        when (currentSub) {
            "Basic"       -> drawBasic()
            "Wardrobe"    -> drawWardrobe()
            "Talismans"   -> drawTalismans()
            "Backpacks"   -> drawBackpacks()
            "Ender Chest" -> drawEnderChest()
        }
    }

    override fun click(mouseX: Double, mouseY: Double): Boolean {
        if (tabButtons?.click(mouseX, mouseY) == true) return true
        if (subPageButtons?.click(mouseX, mouseY) == true) return true
        return false
    }

    // ── Sub-pages ──────────────────────────────────────────────────────────────

    private fun drawBasic() {
        val raw      = invContents
        val fixedInv = if (raw.size >= 9) raw.subList(9, raw.size) + raw.subList(0, 9) else raw
        val items    = invArmor.reversed() + listOf(null) + equipment + fixedInv
        val gap      = SP / 2f
        val slotSize = slotSizeFromWidth(w, 9, gap)
        val rows     = ceil(items.size / 9f)
        val gridW    = 9 * slotSize + 8 * gap
        val gridH    = rows * slotSize + (rows - 1) * gap
        val gx       = x + (w - gridW) / 2f
        val basicCY  = startY + (y + h - startY) / 2f
        val gy       = (basicCY - gridH / 2f).coerceIn(startY, (y + h) - gridH)
        drawGrid(items, gx, gy, 9, slotSize, gap) { item, idx -> if (idx == 4) 0x00000000 else rarityColor(item) }
    }

    private fun drawWardrobe() {
        val wardrobe  = wardrobeContents
        val pages     = ceil(wardrobe.size / 36.0).toInt().coerceAtLeast(1)
        val btns      = getOrBuildSubButtons(x, startY, w, pageBtnH, pages).also { it.draw() }
        val pageIdx   = (btns.selected ?: 1) - 1
        val pageItems = getSubset(wardrobe, pageIdx, 36)
        val armorSet  = invArmor.toSet()
        val gap       = SP / 2f
        val slotSize  = slotSizeFromWidth(w, 9, gap)
        val rows      = ceil(pageItems.size / 9f)
        val gridH     = rows * slotSize + (rows - 1) * gap
        val gy        = centerY - gridH / 2f
        drawGrid(pageItems, x, gy, 9, slotSize, gap) { item, _ ->
            if (item != null && item in armorSet) 0xFF1A3A6A.toInt() else rarityColor(item)
        }
    }

    private fun drawTalismans() {
        val talis      = talisman
        val pageSize   = 7 * 9
        val pages      = ceil(talis.size / pageSize.toFloat()).toInt().coerceAtLeast(1)
        val textBoxW   = w * 0.38f
        val separatorX = floor(x + textBoxW).toFloat()
        val rightX     = separatorX + SP
        val rightW     = w - (separatorX - x) - SP
        val panelH     = contentH

        NVGRenderer.rect(x, startY, textBoxW, panelH, Theme.panel, Theme.radius)
        TextBox(
            x = x + SP, y = startY + SP,
            w = textBoxW - SP * 2f, h = panelH - SP * 2f,
            lines = taliTextLines, textSize = 17f,
            title = "§5Magical Power§7: ${magicPower.toDouble().colorize(1800.0, 0)}", titleSize = 22f,
        ).draw()

        val btns     = if (pages > 1) getOrBuildSubButtons(rightX, startY, rightW, pageBtnH, pages).also { it.draw() } else null
        val gridTopY = if (btns != null) startY + pageBtnH + SP else startY
        val availH   = (y + h) - gridTopY
        val page     = (btns?.selected ?: 1) - 1
        val pageItems = getSubset(talis, page, pageSize)
        val gap       = SP / 2f
        val slotSize  = slotSizeFromWidth(rightW, 9, gap)
        val rows      = ceil(pageItems.size / 9f)
        val gridH     = rows * slotSize + (rows - 1) * gap
        val gy        = gridTopY + (availH - gridH) / 2f
        val scissor   = floatArrayOf(rightX, startY, rightX + rightW, y + h)
        drawGrid(pageItems, rightX, gy, 9, slotSize, gap, scissor) { item, _ -> rarityColor(item) }
    }

    private fun drawBackpacks() {
        val keys = backpackKeys
        if (keys.isEmpty()) { centeredText("No backpacks found", Theme.textSecondary); return }
        val btns = subPageButtons?.also { it.x = x; it.y = startY; it.w = w; it.h = pageBtnH }
            ?: buttons(x = x, y = startY, w = w, h = pageBtnH, items = keys, vertical = false,
                spacing = SP / 2f, textSize = 14f, radius = Theme.radius, label = { it.toString() },
            ) {}.also { it.selected = keys.first(); subPageButtons = it }
        btns.draw()
        val selectedKey = (btns.selected ?: keys.first()) - 1
        val items    = PVState.member()?.inventory?.backpackContents?.get(selectedKey.toString())?.itemStacks.orEmpty()
        val gridW80  = w * 0.8f
        val gx       = x + (w - gridW80) / 2f
        val gap      = SP / 2f
        val slotSize = slotSizeFromWidth(gridW80, 9, gap)
        val rows     = ceil(items.size / 9f)
        val gridH    = rows * slotSize + (rows - 1) * gap
        val gy       = centerY - gridH / 2f
        drawGrid(items, gx, gy, 9, slotSize, gap) { item, _ -> rarityColor(item) }
    }

    private fun drawEnderChest() {
        val items = eChest
        val pages = ceil(items.size / 45.0).toInt().coerceAtLeast(1)
        val btns  = if (pages > 1) getOrBuildSubButtons(x, startY, w, pageBtnH, pages).also { it.draw() } else null
        val page  = (btns?.selected ?: 1) - 1
        val pageItems = getSubset(items, page, 45)
        val gridW80  = w * 0.8f
        val gx       = x + (w - gridW80) / 2f
        val gap      = SP / 2f
        val slotSize = slotSizeFromWidth(gridW80, 9, gap)
        val rows     = ceil(pageItems.size / 9f)
        val gridH    = rows * slotSize + (rows - 1) * gap
        val gy       = centerY - gridH / 2f
        drawGrid(pageItems, gx, gy, 9, slotSize, gap) { item, _ -> rarityColor(item) }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun slotSizeFromWidth(availW: Float, cols: Int, gap: Float) =
        (availW - gap * (cols - 1)) / cols

    private fun getOrBuildSubButtons(bx: Float, by: Float, bw: Float, bh: Float, count: Int): ButtonsDsl<Int> {
        val existing = subPageButtons
        if (existing != null && existing.items.size == count) {
            existing.x = bx; existing.y = by; existing.w = bw; existing.h = bh; return existing
        }
        return buttons(x = bx, y = by, w = bw, h = bh, items = (1..count).toList(),
            vertical = false, spacing = SP / 2f, textSize = 14f, radius = Theme.radius,
            label = { it.toString() },
        ) {}.also { it.selected = 1; subPageButtons = it }
    }

    private fun getSubset(items: List<HypixelData.ItemData?>, page: Int, pageSize: Int): List<HypixelData.ItemData?> {
        val start = page * pageSize
        return if (start >= items.size) emptyList()
        else items.subList(start, minOf(start + pageSize, items.size))
    }

    private fun drawGrid(
        items: List<HypixelData.ItemData?>,
        gx: Float, gy: Float,
        cols: Int, slotSize: Float, gap: Float,
        scissor: FloatArray? = null,
        bgColor: (HypixelData.ItemData?, Int) -> Int,
    ) {
        items.forEachIndexed { idx, item ->
            val sx = gx + (idx % cols) * (slotSize + gap)
            val sy = gy + (idx / cols) * (slotSize + gap)
            NVGRenderer.rect(sx, sy, slotSize, slotSize, bgColor(item, idx), Theme.radius / 2f)
            if (item != null) {
                val stack = item.asItemStack
                if (!stack.isEmpty) {
                    if (PVState.isHovered(sx, sy, slotSize, slotSize))
                        NVGRenderer.rect(sx, sy, slotSize, slotSize, Theme.btnHover, Theme.radius / 2f)
                    ItemQueue.queue(stack, sx, sy, slotSize, showTooltip = true, scissor = scissor)
                }
            }
        }
    }

    private fun rarityColor(item: HypixelData.ItemData?): Int {
        if (item == null) return Theme.slotBg
        return when (getSkyblockRarity(item.lore)?.colorCode) {
            "§f" -> 0xFF2A2A2A.toInt()
            "§2" -> 0xFF1A3A1A.toInt()
            "§9" -> 0xFF1A1A3A.toInt()
            "§5" -> 0xFF2A1A2A.toInt()
            "§6" -> 0xFF3A2A00.toInt()
            "§d" -> 0xFF321A32.toInt()
            "§b" -> 0xFF1A2A2A.toInt()
            "§c" -> 0xFF3A1A1A.toInt()
            else -> Theme.slotBg
        }
    }
}
