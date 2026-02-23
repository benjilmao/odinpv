package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.capitalizeWords
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.utils.HypixelData
import com.odtheking.odinaddon.pvgui.utils.Utils
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.PageHandler
import com.odtheking.odinaddon.pvgui.PVLayout
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.utils.Theme
import net.minecraft.world.item.ItemStack

object InventoryPage : PageHandler {
    override val name = "Inventory"
    private val SUB_PAGES = listOf("Basic", "Wardrobe", "Talismans", "Backpacks", "Ender Chest")
    private var lastTotalPages = 1
    var currentSubPage = "Basic"
    var currentPage = 1

    private const val TAB_H = 42f
    private const val PADDING = 8f
    private const val TAB_SPACING = 5f
    private const val SLOT_SPACING = 6f
    private const val TEXT_SIZE = 18f
    private const val INFO_TEXT = 16f
    private const val BTN_H = 26f
    private const val BTN_SPACING = 5f

    private val TAB_RADIUS get() = Theme.round
    private val SLOT_RADIUS get() = Theme.round

    override fun onOpen() {
        currentSubPage = "Basic"
        currentPage = 1
    }

    private fun btnColor(selected: Boolean, hovered: Boolean) = when {
        selected -> Theme.accent
        hovered -> Theme.btnHover
        else -> Theme.btnNormal
    }

    override fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, mouseX: Double, mouseY: Double) {
        val member = PVState.memberData() ?: return
        if (!member.inventoryApi) {
            val msg = "API is disabled for this profile"
            ctx.text(msg, x + (w - ctx.textWidth(msg, 32f)) / 2f, y + h / 2f, 32f, Color(255, 85, 85))
            return
        }
        val inv = member.inventory

        val tabW = (w - TAB_SPACING * (SUB_PAGES.size - 1)) / SUB_PAGES.size
        SUB_PAGES.forEachIndexed { i, tab ->
            val tx = x + i * (tabW + TAB_SPACING)
            val isSelected = tab == currentSubPage
            val isHovered = ctx.isHovered(mouseX, mouseY, tx, y, tabW, TAB_H)
            ctx.rect(tx, y, tabW, TAB_H, btnColor(isSelected, isHovered), TAB_RADIUS)
            val label = if (tab == "Ender Chest") "E.Chest" else tab
            val tw = ctx.textWidth(label, TEXT_SIZE)
            ctx.text(label, tx + (tabW - tw) / 2f, y + (TAB_H - TEXT_SIZE) / 2f, TEXT_SIZE,
                if (isSelected || isHovered) Color(255, 255, 255) else Color(170, 170, 170))
        }

        val contentY = y + TAB_H + PADDING
        val contentH = h - TAB_H - PADDING

        when (currentSubPage) {
            "Basic" -> drawBasic(ctx, x, contentY, w, contentH, inv)
            "Wardrobe" -> drawPagedGrid(ctx, x, contentY, w, contentH, mouseX, mouseY, inv?.wardrobeContents?.itemStacks.orEmpty(), cols = 9, pageSize = 36)
            "Talismans" -> drawTalismans(ctx, x, contentY, w, contentH, mouseX, mouseY, member, inv)
            "Backpacks" -> drawBackpacks(ctx, x, contentY, w, contentH, mouseX, mouseY, inv)
            "Ender Chest" -> drawPagedGrid(ctx, x, contentY, w, contentH, mouseX, mouseY, inv?.eChestContents?.itemStacks.orEmpty(), cols = 9, pageSize = 45)
        }
    }

    private fun drawBasic(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, inv: HypixelData.Inventory?) {
        val armor = inv?.invArmor?.itemStacks?.reversed().orEmpty()
        val equip = inv?.equipment?.itemStacks.orEmpty()
        val raw = inv?.invContents?.itemStacks.orEmpty()
        val allItems: List<HypixelData.ItemData?> = armor + listOf(null) + equip + raw.drop(9) + raw.take(9)
        drawSlotGrid(ctx, x, y, w, h, allItems, cols = 9)
    }

    private fun drawTalismans(
        ctx: DrawContext, x: Float, y: Float, w: Float, h: Float,
        mouseX: Double, mouseY: Double,
        member: HypixelData.MemberData, inv: HypixelData.Inventory?
    ) {
        val leftW = w * 0.32f
        val rightX = x + leftW + PADDING
        val rightW = w - leftW - PADDING

        val mp = member.assumedMagicalPower
        val power = member.accessoryBagStorage.selectedPower
        val tunings = member.accessoryBagStorage.tuning.currentTunings.entries
            .map { (k, v) -> "§7${k.replace("_", " ").capitalizeWords()}§7: §f$v" }

        val statLines = listOf(
            "§5Magical Power§7: ${Utils.colorize(mp.toDouble(), 1900.0)}$mp",
            "§aSelected Power§7: §6${power?.capitalizeWords() ?: "§cNone!"}",
            "§5Abiphone§7: §f${member.crimsonIsle?.abiphone?.activeContacts?.size?.div(2) ?: 0}",
            "§dRift Prism§7: ${if (member.rift.access.consumedPrism) "§aObtained" else "§cMissing"}",
        ) + tunings

        ctx.hollowRect(x, y, leftW, h, 1f, Color(255, 255, 255, 0.12f), 6f)
        ctx.textList(statLines, x + PADDING, y, leftW - PADDING * 2f, h, maxSize = 20f)
        ctx.line(rightX - PADDING / 2f, y + 4f, rightX - PADDING / 2f, y + h - 4f, 1f, Color(255, 255, 255, 0.15f))

        val talismans = inv?.bagContents?.get("talisman_bag")?.itemStacks.orEmpty()
        drawPagedGrid(ctx, rightX, y, rightW, h, mouseX, mouseY, talismans, cols = 9, pageSize = 63)
    }

    private fun drawBackpacks(
        ctx: DrawContext, x: Float, y: Float, w: Float, h: Float,
        mouseX: Double, mouseY: Double,
        inv: HypixelData.Inventory?
    ) {
        val backpackKeys = inv?.backpackContents?.keys?.mapNotNull { it.toIntOrNull() }?.sorted() ?: emptyList()

        if (backpackKeys.isEmpty()) {
            val msg = "No backpacks found"
            ctx.text(msg, x + (w - ctx.textWidth(msg, INFO_TEXT)) / 2f, y + h / 2f, INFO_TEXT, Color(170, 170, 170))
            return
        }

        currentPage = currentPage.coerceIn(1, backpackKeys.size)

        val btnW = (w - BTN_SPACING * (backpackKeys.size - 1)) / backpackKeys.size
        val navH = BTN_H + PADDING

        backpackKeys.forEachIndexed { i, _ ->
            val bx = x + i * (btnW + BTN_SPACING)
            val isSelected = (i + 1) == currentPage
            val isHovered = ctx.isHovered(mouseX, mouseY, bx, y, btnW, BTN_H)
            ctx.rect(bx, y, btnW, BTN_H, btnColor(isSelected, isHovered), TAB_RADIUS)
            val label = "${i + 1}"
            val lw = ctx.textWidth(label, TEXT_SIZE - 2f)
            ctx.text(label, bx + (btnW - lw) / 2f, y + (BTN_H - (TEXT_SIZE - 2f)) / 2f, TEXT_SIZE - 2f,
                if (isSelected || isHovered) Color(255, 255, 255) else Color(170, 170, 170))
        }

        ctx.line(x, y + navH, x + w, y + navH, 1f, Color(255, 255, 255, 0.15f))

        val selectedKey = backpackKeys.getOrNull(currentPage - 1)?.toString() ?: return
        val bpItems = inv?.backpackContents?.get(selectedKey)?.itemStacks.orEmpty()
        drawSlotGrid(ctx, x, y + navH + PADDING, w, h - navH - PADDING, bpItems, cols = 9)
    }

    private fun drawPagedGrid(
        ctx: DrawContext, x: Float, y: Float, w: Float, h: Float,
        mouseX: Double, mouseY: Double,
        items: List<HypixelData.ItemData?>, cols: Int, pageSize: Int,
    ) {
        val totalPages = ((items.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        currentPage = currentPage.coerceIn(1, totalPages)

        var contentY = y
        var contentH = h

        if (totalPages > 1) {
            val btnW = (w - BTN_SPACING * (totalPages - 1)) / totalPages
            for (p in 1..totalPages) {
                val bx = x + (p - 1) * (btnW + BTN_SPACING)
                val isSel = p == currentPage
                val isHov = ctx.isHovered(mouseX, mouseY, bx, y, btnW, BTN_H)
                ctx.rect(bx, y, btnW, BTN_H, btnColor(isSel, isHov), TAB_RADIUS)
                val label = "$p"
                val lw = ctx.textWidth(label, TEXT_SIZE - 2f)
                ctx.text(label, bx + (btnW - lw) / 2f, y + (BTN_H - (TEXT_SIZE - 2f)) / 2f, TEXT_SIZE - 2f,
                    if (isSel || isHov) Color(255, 255, 255) else Color(170, 170, 170))
            }
            contentY += BTN_H + PADDING
            contentH -= BTN_H + PADDING
        }
        lastTotalPages = totalPages
        drawSlotGrid(ctx, x, contentY, w, contentH, items.drop((currentPage - 1) * pageSize).take(pageSize), cols)
    }

    private fun drawSlotGrid(
        ctx: DrawContext, x: Float, y: Float, w: Float, h: Float,
        items: List<HypixelData.ItemData?>, cols: Int,
        slotColorFn: (Int, HypixelData.ItemData?) -> Color = { _, item ->
            if (item != null && ProfileViewerModule.rarityBackgrounds) Theme.rarityFromLore(item.lore)
            else if (item != null) Theme.btnNormal
            else Color(0, 0, 0, 0.35f)
        },
    ) {
        if (items.isEmpty()) return
        val slotSize = (w - SLOT_SPACING * (cols - 1) - PADDING * 2f) / cols
        val rows = (items.size + cols - 1) / cols
        val totalH = rows * (slotSize + SLOT_SPACING) - SLOT_SPACING
        val startY = y + ((h - totalH) / 2f).coerceAtLeast(0f)

        items.forEachIndexed { index, itemData ->
            val sx = x + PADDING + (index % cols) * (slotSize + SLOT_SPACING)
            val sy = startY + (index / cols) * (slotSize + SLOT_SPACING)
            ctx.rect(sx, sy, slotSize, slotSize, slotColorFn(index, itemData), SLOT_RADIUS)
            val stack = itemData?.asItemStack ?: ItemStack.EMPTY
            if (!stack.isEmpty) {
                val pad = slotSize * 0.05f
                ctx.item(stack, sx + pad, sy + pad, slotSize - pad * 2f)
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
                if (currentSubPage != tab) { currentSubPage = tab; currentPage = 1 }
                return
            }
        }

        val contentY = y + TAB_H + PADDING

        when (currentSubPage) {
            "Backpacks" -> {
                val inv = PVState.memberData()?.inventory ?: return
                val backpackKeys = inv.backpackContents.keys.mapNotNull { it.toIntOrNull() }.sorted()
                val btnW = (w - BTN_SPACING * (backpackKeys.size - 1)) / backpackKeys.size
                backpackKeys.forEachIndexed { i, _ ->
                    val bx = x + i * (btnW + BTN_SPACING)
                    if (ctx.isHovered(mouseX, mouseY, bx, contentY, btnW, BTN_H)) { currentPage = i + 1; return }
                }
            }
            "Wardrobe", "Ender Chest" -> {
                val items = if (currentSubPage == "Wardrobe")
                    PVState.memberData()?.inventory?.wardrobeContents?.itemStacks.orEmpty()
                else
                    PVState.memberData()?.inventory?.eChestContents?.itemStacks.orEmpty()
                val pageSize = if (currentSubPage == "Wardrobe") 36 else 45
                val totalPages = ((items.size + pageSize - 1) / pageSize).coerceAtLeast(1)
                if (totalPages > 1) {
                    val btnW = (w - BTN_SPACING * (totalPages - 1)) / totalPages
                    for (p in 1..totalPages) {
                        val bx = x + (p - 1) * (btnW + BTN_SPACING)
                        if (ctx.isHovered(mouseX, mouseY, bx, contentY, btnW, BTN_H)) { currentPage = p; return }
                    }
                }
            }
            "Talismans" -> {
                val leftW = w * 0.32f
                val rightX = x + leftW + PADDING
                val rightW = w - leftW - PADDING
                val talismans = PVState.memberData()?.inventory?.bagContents?.get("talisman_bag")?.itemStacks.orEmpty()
                val totalPages = ((talismans.size + 62) / 63).coerceAtLeast(1)
                if (totalPages > 1) {
                    val btnW = (rightW - BTN_SPACING * (totalPages - 1)) / totalPages
                    for (p in 1..totalPages) {
                        val bx = rightX + (p - 1) * (btnW + BTN_SPACING)
                        if (ctx.isHovered(mouseX, mouseY, bx, contentY, btnW, BTN_H)) { currentPage = p; return }
                    }
                }
            }
        }
    }

    override fun onScroll(delta: Double) {}

    private fun List<HypixelData.ItemData?>?.orEmpty() = this ?: emptyList()
}