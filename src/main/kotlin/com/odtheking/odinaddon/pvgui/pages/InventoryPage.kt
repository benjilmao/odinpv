package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.capitalizeWords
import com.odtheking.odinaddon.pvgui2.utils.HypixelData
import com.odtheking.odinaddon.pvgui2.utils.Utils
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.PageHandler
import com.odtheking.odinaddon.pvgui.PVLayout
import com.odtheking.odinaddon.pvgui.PVState
import net.minecraft.world.item.ItemStack

object InventoryPage : PageHandler {
    private val SUB_PAGES = listOf("Basic", "Wardrobe", "Talismans", "Backpacks", "Ender Chest")
    var currentSubPage = "Basic"
    var currentPage    = 1

    private val COL_PANEL_BG   = Color(255, 255, 255, 0.05f)
    private val COL_SLOT_BG    = Color(0, 0, 0, 0.40f)
    private val COL_HOTBAR_BG  = Color(34, 85, 34, 0.40f)
    private val COL_SEPARATOR  = Color(255, 255, 255, 0.15f)
    private val COL_TAB_SEL    = Color(26, 74, 138)
    private val COL_TAB_HOVER  = Color(255, 255, 255, 0.18f)
    private val COL_TAB_NORM   = Color(255, 255, 255, 0.08f)
    private val COL_GRAY       = Color(170, 170, 170)
    private val COL_WHITE      = Color(255, 255, 255)
    private val COL_PAGE_SEL   = Color(26, 74, 138)
    private val COL_PAGE_NORM  = Color(255, 255, 255, 0.08f)

    private const val TAB_H        = 32f
    private const val TAB_RADIUS   = 12f
    private const val SLOT_RADIUS  = 12f
    private const val PANEL_RADIUS = 12f
    private const val PADDING      = 8f
    private const val TAB_SPACING  = 5f
    private const val SLOT_SPACING = 8f
    private const val TEXT_SIZE    = 20f
    private const val INFO_TEXT    = 16f

    private fun raritySlotColor(lore: List<String>): Color {
        val last = lore.lastOrNull()?.uppercase() ?: return COL_SLOT_BG
        return when {
            last.contains("MYTHIC")    -> Color(255, 85, 255, 0.35f)
            last.contains("LEGENDARY") -> Color(255, 170, 0, 0.35f)
            last.contains("EPIC")      -> Color(170, 0, 170, 0.35f)
            last.contains("RARE")      -> Color(85, 85, 255, 0.35f)
            last.contains("UNCOMMON")  -> Color(85, 255, 85, 0.35f)
            last.contains("COMMON")    -> Color(170, 170, 170, 0.35f)
            last.contains("DIVINE")    -> Color(85, 255, 255, 0.35f)
            last.contains("SPECIAL")   -> Color(255, 85, 85, 0.35f)
            else                       -> COL_SLOT_BG
        }
    }

    override fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, mouseX: Double, mouseY: Double) {
        val member = PVState.memberData() ?: return
        val inv    = member.inventory

        val tabW = (w - TAB_SPACING * (SUB_PAGES.size - 1)) / SUB_PAGES.size
        SUB_PAGES.forEachIndexed { i, tab ->
            val tx         = x + i * (tabW + TAB_SPACING)
            val isSelected = tab == currentSubPage
            val isHovered  = ctx.isHovered(mouseX, mouseY, tx, y, tabW, TAB_H)

            ctx.rect(tx, y, tabW, TAB_H, when {
                isSelected -> COL_TAB_SEL
                isHovered  -> COL_TAB_HOVER
                else       -> COL_TAB_NORM
            }, TAB_RADIUS)

            val label = if (tab == "Ender Chest") "E.Chest" else tab
            val tw    = ctx.textWidth(label, TEXT_SIZE)
            val col   = if (isSelected || isHovered) COL_WHITE else COL_GRAY
            ctx.text(label, tx + (tabW - tw) / 2f, y + (TAB_H - TEXT_SIZE) / 2f, TEXT_SIZE, col)
        }

        val contentY = y + TAB_H + PADDING
        val contentH = h - TAB_H - PADDING

        when (currentSubPage) {
            "Basic"       -> drawBasic(ctx, x, contentY, w, contentH, inv)
            "Wardrobe"    -> drawWardrobe(ctx, x, contentY, w, contentH, inv)
            "Talismans"   -> drawTalismans(ctx, x, contentY, w, contentH, member, inv)
            "Backpacks"   -> drawBackpacks(ctx, x, contentY, w, contentH, inv)
            "Ender Chest" -> drawPagedGrid(ctx, x, contentY, w, contentH, inv?.eChestContents?.itemStacks.orEmpty(), cols = 9, pageSize = 45)
        }
    }

    private fun drawBasic(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, inv: HypixelData.Inventory?) {
        val armor     = inv?.invArmor?.itemStacks?.reversed().orEmpty()
        val equipment = inv?.equipment?.itemStacks.orEmpty()
        val raw       = inv?.invContents?.itemStacks.orEmpty()
        val hotbar    = raw.take(9)
        val main      = raw.drop(9)

        val topRow: List<HypixelData.ItemData?> = armor + listOf(null) + equipment
        val allItems  = topRow + main + hotbar
        val totalRows = (allItems.size + 8) / 9

        drawSlotGrid(
            ctx, x, y, w, h, allItems, cols = 9,
            slotColorFn = { index, item ->
                val row = index / 9
                when {
                    row == totalRows - 1 -> COL_HOTBAR_BG
                    item != null         -> raritySlotColor(item.lore)
                    else                 -> COL_SLOT_BG
                }
            }
        )
    }

    private fun drawWardrobe(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, inv: HypixelData.Inventory?) {
        drawPagedGrid(ctx, x, y, w, h, inv?.wardrobeContents?.itemStacks.orEmpty(), cols = 9, pageSize = 36)
    }

    private fun drawTalismans(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, member: HypixelData.MemberData, inv: HypixelData.Inventory?) {
        val leftW  = w * 0.32f
        val rightX = x + leftW + PADDING
        val rightW = w - leftW - PADDING

        val power    = member.accessoryBagStorage.selectedPower
        val contacts = member.crimsonIsle.abiphone.activeContacts.size
        val hasPrism = member.rift.access.consumedPrism
        val mp       = member.assumedMagicalPower
        val tunings  = member.accessoryBagStorage.tuning.currentTunings.entries
            .map { (k, v) -> "§7${k.replace("_", " ").capitalizeWords()}§7: §f$v" }

        val statLines = listOf(
            "§5Magical Power§7: ${Utils.colorize(mp.toDouble(), 1900.0)}$mp",
            "§aSelected Power§7: §6${power?.capitalizeWords() ?: "§cNone!"}",
            "§5Abiphone§7: §f${(contacts / 2)}",
            "§dRift Prism§7: ${if (hasPrism) "§aObtained" else "§cMissing"}",
        ) + tunings

        ctx.rect(x, y, leftW, h, COL_PANEL_BG, PANEL_RADIUS)
        ctx.textList(statLines, x + PADDING, y, leftW - PADDING * 2f, h, maxSize = 20f)

        ctx.line(rightX - PADDING / 2f, y + 4f, rightX - PADDING / 2f, y + h - 4f, 1f, COL_SEPARATOR)

        val talismans = inv?.bagContents?.get("talisman_bag")?.itemStacks.orEmpty()
        drawPagedGrid(ctx, rightX, y, rightW, h, talismans, cols = 9, pageSize = 63)
    }

    private fun drawBackpacks(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, inv: HypixelData.Inventory?) {
        val backpackKeys = inv?.backpackContents?.keys
            ?.mapNotNull { it.toIntOrNull() }
            ?.sorted() ?: emptyList()

        if (backpackKeys.isEmpty()) {
            val msg = "No backpacks found"
            ctx.text(msg, x + (w - ctx.textWidth(msg, INFO_TEXT)) / 2f, y + h / 2f, INFO_TEXT, COL_GRAY)
            return
        }

        currentPage = currentPage.coerceIn(1, backpackKeys.size)

        val iconSize   = 24f
        val iconSpacing = 4f
        val navH       = iconSize + PADDING
        val totalNavW  = backpackKeys.size * (iconSize + iconSpacing) - iconSpacing
        var iconX      = x + (w - totalNavW) / 2f

        backpackKeys.forEachIndexed { i, key ->
            val isSelected = (i + 1) == currentPage
            val isHovered  = ctx.isHovered(mouseX = 0.0, mouseY = 0.0, iconX, y, iconSize, iconSize)
            if (isSelected) ctx.rect(iconX - 2f, y - 2f, iconSize + 4f, iconSize + 4f, COL_TAB_SEL, SLOT_RADIUS)

            val iconStack = inv?.backpackIcons?.get(key.toString())?.itemStacks?.firstOrNull()?.asItemStack
                ?: ItemStack.EMPTY
            ctx.item(iconStack, iconX, y, iconSize)

            iconX += iconSize + iconSpacing
        }

        ctx.line(x, y + navH, x + w, y + navH, 1f, COL_SEPARATOR)

        val selectedKey  = backpackKeys.getOrNull(currentPage - 1)?.toString() ?: return
        val bpItems      = inv?.backpackContents?.get(selectedKey)?.itemStacks.orEmpty()
        drawSlotGrid(ctx, x, y + navH + PADDING, w, h - navH - PADDING, bpItems, cols = 9)
    }

    private fun drawPagedGrid(
        ctx: DrawContext, x: Float, y: Float, w: Float, h: Float,
        items: List<HypixelData.ItemData?>, cols: Int, pageSize: Int,
    ) {
        val totalPages = ((items.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        currentPage    = currentPage.coerceIn(1, totalPages)

        var contentY = y
        var contentH = h

        if (totalPages > 1) {
            val btnSize    = 18f
            val btnSpacing = 4f
            val navW       = totalPages * (btnSize + btnSpacing) - btnSpacing
            var bx         = x + (w - navW) / 2f

            for (p in 1..totalPages) {
                val isSelected = p == currentPage
                ctx.rect(bx, y, btnSize, btnSize, if (isSelected) COL_PAGE_SEL else COL_PAGE_NORM, 4f)
                val label = "$p"
                val lw    = ctx.textWidth(label, TEXT_SIZE - 1f)
                ctx.text(label, bx + (btnSize - lw) / 2f, y + (btnSize - (TEXT_SIZE - 1f)) / 2f, TEXT_SIZE - 1f,
                    if (isSelected) COL_WHITE else COL_GRAY)
                bx += btnSize + btnSpacing
            }

            contentY += btnSize + PADDING
            contentH -= btnSize + PADDING
        }

        val pageItems = items.drop((currentPage - 1) * pageSize).take(pageSize)
        drawSlotGrid(ctx, x, contentY, w, contentH, pageItems, cols)
    }

    private fun drawSlotGrid(
        ctx: DrawContext,
        x: Float, y: Float, w: Float, h: Float,
        items: List<HypixelData.ItemData?>,
        cols: Int,
        slotColorFn: (index: Int, item: HypixelData.ItemData?) -> Color = { _, item ->
            if (item != null) raritySlotColor(item.lore) else COL_SLOT_BG
        },
    ) {
        if (items.isEmpty()) return

        val slotSize   = (w - SLOT_SPACING * (cols - 1) - PADDING * 2f) / cols
        val rows       = (items.size + cols - 1) / cols
        val totalH     = rows * (slotSize + SLOT_SPACING) - SLOT_SPACING
        val startY     = y + ((h - totalH) / 2f).coerceAtLeast(0f)

        items.forEachIndexed { index, itemData ->
            val col  = index % cols
            val row  = index / cols
            val sx   = x + PADDING + col * (slotSize + SLOT_SPACING)
            val sy   = startY + row * (slotSize + SLOT_SPACING)

            ctx.rect(sx, sy, slotSize, slotSize, slotColorFn(index, itemData), SLOT_RADIUS)

            val stack = itemData?.asItemStack ?: ItemStack.EMPTY
            if (!stack.isEmpty) {
                val itemPad  = slotSize * 0.10f
                val itemSize = slotSize - itemPad * 2f
                ctx.item(stack, sx + itemPad, sy + itemPad, itemSize)
            }
        }
    }

    override fun onClick(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        val x = PVLayout.MAIN_X
        val y = PVLayout.MAIN_Y
        val w = PVLayout.MAIN_W

        val tabW = (w - TAB_SPACING * (SUB_PAGES.size - 1)) / SUB_PAGES.size
        SUB_PAGES.forEachIndexed { i, tab ->
            val tx = x + i * (tabW + TAB_SPACING)
            if (ctx.isHovered(mouseX, mouseY, tx, y, tabW, TAB_H)) {
                if (currentSubPage != tab) {
                    currentSubPage = tab
                    currentPage    = 1
                }
                return
            }
        }

        val contentY = y + TAB_H + PADDING
        val contentH = PVLayout.MAIN_H - TAB_H - PADDING

        if (currentSubPage in listOf("Wardrobe", "Ender Chest", "Talismans", "Backpacks")) {
            handlePageClick(ctx, mouseX, mouseY, x, contentY, w, contentH)
        }

        if (currentSubPage == "Backpacks") {
            handleBackpackIconClick(ctx, mouseX, mouseY, x, contentY, w)
        }
    }

    private fun handlePageClick(ctx: DrawContext, mouseX: Double, mouseY: Double, x: Float, y: Float, w: Float, h: Float) {
        val btnSize    = 24f
        val btnSpacing = 4f
        val navW = 10 * (btnSize + btnSpacing)
        var bx   = x + (w - navW) / 2f
        for (p in 1..10) {
            if (ctx.isHovered(mouseX, mouseY, bx, y, btnSize, btnSize)) {
                currentPage = p
                return
            }
            bx += btnSize + btnSpacing
        }
    }

    private fun handleBackpackIconClick(ctx: DrawContext, mouseX: Double, mouseY: Double, x: Float, y: Float, w: Float) {
        val inv          = PVState.memberData()?.inventory ?: return
        val backpackKeys = inv.backpackContents.keys.mapNotNull { it.toIntOrNull() }.sorted()
        val iconSize     = 24f
        val iconSpacing  = 4f
        val totalNavW    = backpackKeys.size * (iconSize + iconSpacing) - iconSpacing
        var iconX        = x + (w - totalNavW) / 2f

        backpackKeys.forEachIndexed { i, _ ->
            if (ctx.isHovered(mouseX, mouseY, iconX, y, iconSize, iconSize)) {
                currentPage = i + 1
                return
            }
            iconX += iconSize + iconSpacing
        }
    }

    override fun onScroll(delta: Double) {
    }

    private fun List<HypixelData.ItemData?>?.orEmpty() = this ?: emptyList()
}