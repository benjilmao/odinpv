package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.capitalizeWords
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.dsl.ButtonsDsl
import com.odtheking.odinaddon.pvgui.dsl.TextBox
import com.odtheking.odinaddon.pvgui.dsl.buttons
import com.odtheking.odinaddon.pvgui.dsl.itemGrid
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import com.odtheking.odinaddon.pvgui.utils.colorize
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import net.minecraft.world.item.ItemStack
import kotlin.math.ceil
import kotlin.math.floor

object InventoryPage : PVPage() {
    override val name = "Inventory"

    // ── Layout ────────────────────────────────────────────────────────────────
    // Mirrors HC: tab row = (h * 0.9 - SP * 5) / 6
    private val SP       get() = 10f
    private val tabH     get() = ((h * 0.9f) - SP * 5f) / 6f
    private val startY   get() = y + tabH + SP          // content area top
    private val pageBtnH get() = (w - SP * 16f) / 18f   // sub-page button height
    private val centerY  get() = startY + (h - (startY - y)) / 2f  // mid of content area

    // ── Tab state ─────────────────────────────────────────────────────────────
    private val SUB_PAGES = listOf("Basic", "Wardrobe", "Talismans", "Backpacks", "Ender Chest")
    private var currentSub = "Basic"

    private var tabButtons:     ButtonsDsl<String>? = null
    private var subPageButtons: ButtonsDsl<Int>?    = null

    fun resetState() {
        currentSub     = "Basic"
        tabButtons     = null
        subPageButtons = null
    }

    override fun onOpen() { resetState(); buildTabs() }

    private fun buildTabs() {
        tabButtons = buttons(
            x = x, y = y, w = w, h = tabH,
            items = SUB_PAGES, vertical = false,
            spacing = SP, textSize = 15f, radius = Theme.radius,
            label = { it },
        ) { sub ->
            currentSub     = sub
            subPageButtons = null
        }.also { it.selected = currentSub }
    }

    // ── Resettable data ───────────────────────────────────────────────────────
    private val invArmor          by resettableLazy { PVState.member()?.inventory?.invArmor?.itemStacks.orEmpty() }
    private val invContents       by resettableLazy { PVState.member()?.inventory?.invContents?.itemStacks.orEmpty() }
    private val equipment         by resettableLazy { PVState.member()?.inventory?.equipment?.itemStacks.orEmpty() }
    private val wardrobeContents  by resettableLazy { PVState.member()?.inventory?.wardrobeContents?.itemStacks.orEmpty() }
    private val wardrobeEquipped  by resettableLazy { PVState.member()?.inventory?.wardrobeEquipped }
    private val talisman          by resettableLazy {
        PVState.member()?.inventory?.bagContents?.get("talisman_bag")?.itemStacks
            ?.filterNotNull()?.sortedByDescending { it.magicalPower }.orEmpty()
    }
    private val eChest            by resettableLazy { PVState.member()?.inventory?.eChestContents?.itemStacks.orEmpty() }
    private val backpackKeys      by resettableLazy {
        PVState.member()?.inventory?.backpackContents?.keys
            ?.mapNotNull { it.toIntOrNull()?.plus(1) }?.sorted().orEmpty()
    }
    private val magicPower        by resettableLazy { PVState.member()?.magicalPower ?: 0 }
    private val taliTextLines     by resettableLazy {
        val data = PVState.member() ?: return@resettableLazy emptyList<String>()
        listOf(
            "§aSelected Power§7: §6${data.accessoryBagStorage.selectedPower?.capitalizeWords() ?: "§cNone!"}",
            "§5Abiphone§7: §f${data.crimsonIsle?.abiphone?.activeContacts?.size?.div(2) ?: 0}",
            "§dRift Prism§7: ${if (data.rift.access.consumedPrism) "§aObtained" else "§cMissing"}",
        ) + data.tunings.map { "§7$it" }
    }

    // ── Draw ──────────────────────────────────────────────────────────────────
    override fun draw() {
        if (tabButtons == null) buildTabs()
        tabButtons!!.also { it.x = x; it.y = y; it.w = w; it.h = tabH }.draw()

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

    // ── Sub-pages ─────────────────────────────────────────────────────────────

    private fun drawBasic() {
        // HC: reversed armor + null gap (equipment slot 4) + equipment + fixFirstNine(inv)
        val raw      = invContents
        val fixedInv = if (raw.size >= 9) raw.subList(9, raw.size) + raw.subList(0, 9) else raw
        val items    = invArmor.reversed() + listOf(null) + equipment + fixedInv

        val gap      = SP / 2f
        val slotSize = slotFromWidth(w, 9, gap)
        val rows     = ceil(items.size / 9.0).toInt()
        val gridW    = 9 * slotSize + 8 * gap
        val gridH    = rows * slotSize + (rows - 1).coerceAtLeast(0) * gap
        val gx       = x + (w - gridW) / 2f
        val gy       = (centerY - gridH / 2f).coerceIn(startY, y + h - gridH)

        grid(items, gx, gy, 9, slotSize, gap) { item, idx ->
            if (idx == 4) 0x00000000 else Theme.rarityFromLore(item?.lore.orEmpty())
        }
    }

    private fun drawWardrobe() {
        val wardrobe  = wardrobeContents
        val pages     = ceil(wardrobe.size / 36.0).toInt().coerceAtLeast(1)
        val btns      = getOrBuildSubPager(pages).also { it.draw() }
        val pageIdx   = (btns.selected ?: 1) - 1
        val pageItems = subset(wardrobe, pageIdx, 36).toMutableList()
        val armorSet  = invArmor.toSet()

        // HC: insert equipped armor pieces at their correct wardrobe slots
        val equipped    = wardrobeEquipped?.let { it - 1 } ?: -1
        val rangeStart  = pageIdx * 9
        val equippedSlot = if (equipped in rangeStart until rangeStart + 9) equipped - rangeStart else -1
        if (equippedSlot >= 0 && invArmor.isNotEmpty()) {
            invArmor.forEachIndexed { armorIdx, armItem ->
                val slot = equippedSlot + 9 * (invArmor.size - 1 - armorIdx)
                if (slot < pageItems.size) pageItems[slot] = armItem
            }
        }

        val gap      = SP / 2f
        val slotSize = slotFromWidth(w, 9, gap)
        val rows     = ceil(pageItems.size / 9.0).toInt()
        val gridH    = rows * slotSize + (rows - 1).coerceAtLeast(0) * gap
        val gy       = centerY - gridH / 2f

        grid(pageItems, x, gy, 9, slotSize, gap) { item, _ ->
            if (item != null && item in armorSet) 0xFF1A3A6A.toInt()
            else Theme.rarityFromLore(item?.lore.orEmpty())
        }
    }

    private fun drawTalismans() {
        val talis    = talisman
        val pageSize = 7 * 9   // HC: maxRows = 7
        val pages    = ceil(talis.size / pageSize.toFloat()).toInt().coerceAtLeast(1)

        // Left panel — 38% width (HC magic number)
        val textBoxW   = w * 0.38f
        val separatorX = floor(x + textBoxW).toFloat()
        val rightX     = separatorX + SP
        val rightW     = w - (separatorX - x) - SP
        val panelH     = h - (startY - y)

        NVGRenderer.rect(x, startY, textBoxW, panelH, Theme.panel, Theme.radius)
        TextBox(
            x = x + SP, y = startY + SP,
            w = textBoxW - SP * 2f,
            h = panelH - SP * 2f,
            lines     = taliTextLines,
            textSize  = 17f,
            title     = "§5Magical Power§7: ${magicPower.toDouble().colorize(1800.0, 0)}",
            titleSize = 22f,
        ).draw()

        // Right panel — page buttons if needed, then grid
        val btns     = if (pages > 1) getOrBuildSubPager(pages, rightX, rightW).also { it.draw() } else null
        val gridTopY = if (btns != null) startY + pageBtnH + SP else startY
        val availH   = y + h - gridTopY
        val page     = (btns?.selected ?: 1) - 1
        val pageItems = subset(talis, page, pageSize)
        val gap      = SP / 2f
        val slotSize = slotFromWidth(rightW, 9, gap)
        val rows     = ceil(pageItems.size / 9.0).toInt()
        val gridH    = rows * slotSize + (rows - 1).coerceAtLeast(0) * gap
        val gy       = gridTopY + (availH - gridH) / 2f
        val scissor  = floatArrayOf(rightX, startY, rightX + rightW, y + h)

        grid(pageItems, rightX, gy, 9, slotSize, gap, scissor) { item, _ ->
            Theme.rarityFromLore(item?.lore.orEmpty())
        }
    }

    private fun drawBackpacks() {
        val keys = backpackKeys
        if (keys.isEmpty()) { centeredText("No backpacks found", Theme.textSecondary); return }

        // Backpack buttons are keyed by Int (backpack slot numbers)
        val btns = (subPageButtons?.takeIf { it.items == keys }
            ?.also { it.x = x; it.y = startY; it.w = w; it.h = pageBtnH })
            ?: buttons(
                x = x, y = startY, w = w, h = pageBtnH,
                items = keys, vertical = false,
                spacing = SP / 2f, textSize = 14f, radius = Theme.radius,
                label = { it.toString() },
            ) {}.also { it.selected = keys.first(); subPageButtons = it }
        btns.draw()

        val selKey   = (btns.selected ?: keys.first()) - 1
        val items    = PVState.member()?.inventory?.backpackContents?.get(selKey.toString())?.itemStacks.orEmpty()
        val gridW80  = w * 0.8f
        val gx       = x + (w - gridW80) / 2f
        val gap      = SP / 2f
        val slotSize = slotFromWidth(gridW80, 9, gap)
        val rows     = ceil(items.size / 9.0).toInt()
        val gridH    = rows * slotSize + (rows - 1).coerceAtLeast(0) * gap
        val gy       = centerY - gridH / 2f

        grid(items, gx, gy, 9, slotSize, gap) { item, _ ->
            Theme.rarityFromLore(item?.lore.orEmpty())
        }
    }

    private fun drawEnderChest() {
        val items = eChest
        val pages = ceil(items.size / 45.0).toInt().coerceAtLeast(1)
        val btns  = if (pages > 1) getOrBuildSubPager(pages).also { it.draw() } else null
        val page  = (btns?.selected ?: 1) - 1
        val pageItems = subset(items, page, 45)
        val gridW80  = w * 0.8f
        val gx       = x + (w - gridW80) / 2f
        val gap      = SP / 2f
        val slotSize = slotFromWidth(gridW80, 9, gap)
        val rows     = ceil(pageItems.size / 9.0).toInt()
        val gridH    = rows * slotSize + (rows - 1).coerceAtLeast(0) * gap
        val gy       = centerY - gridH / 2f

        grid(pageItems, gx, gy, 9, slotSize, gap) { item, _ ->
            Theme.rarityFromLore(item?.lore.orEmpty())
        }
    }

    private fun slotFromWidth(availW: Float, cols: Int, gap: Float) =
        (availW - gap * (cols - 1)) / cols

    private fun <T> subset(list: List<T>, page: Int, pageSize: Int): List<T> {
        val start = page * pageSize
        return if (start >= list.size) emptyList()
        else list.subList(start, minOf(start + pageSize, list.size))
    }

    private fun getOrBuildSubPager(
        count: Int,
        bx: Float = x, bw: Float = w,
    ): ButtonsDsl<Int> {
        val ex = subPageButtons
        if (ex != null && ex.items.size == count && ex.x == bx && ex.w == bw)
            return ex.also { it.y = startY; it.h = pageBtnH }
        return buttons(
            x = bx, y = startY, w = bw, h = pageBtnH,
            items = (1..count).toList(),
            vertical = false,
            spacing = SP / 2f,
            textSize = 14f,
            radius = Theme.radius,
            label = { it.toString() },
        ) {}.also { it.selected = 1; subPageButtons = it }
    }


    private fun grid(
        items: List<HypixelData.ItemData?>,
        gx: Float, gy: Float,
        cols: Int, slotSize: Float, gap: Float,
        scissor: FloatArray? = null,
        bgColor: (HypixelData.ItemData?, Int) -> Int,
    ) {
        val stacks: List<ItemStack?> = items.map { it?.asItemStack }
        itemGrid(
            x = gx,
            y = gy,
            cols = cols,
            slotSize = slotSize,
            gap = gap,
            items = { stacks },
            colorHandler = { _, idx -> bgColor(items.getOrNull(idx), idx) },
            scissorRect = scissor?.let { sc -> { sc } },
        ).draw()
    }
}