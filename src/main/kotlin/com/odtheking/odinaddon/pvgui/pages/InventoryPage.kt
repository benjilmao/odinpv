package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.capitalizeWords
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.PageHandler
import com.odtheking.odinaddon.pvgui.PVLayout
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.utils.TextBox
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import com.odtheking.odinaddon.pvgui.utils.colorCode
import com.odtheking.odinaddon.pvgui.utils.resettableLazy

object InventoryPage : PageHandler {
    override val name = "Inventory"
    private val SUB_PAGES = listOf("Basic", "Wardrobe", "Talismans", "Backpacks", "Ender Chest")

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

    private val statLines: List<String> by resettableLazy {
        val member = PVState.memberData() ?: return@resettableLazy emptyList()
        val power = member.accessoryBagStorage.selectedPower
        listOf(
            "§aSelected Power§7: §6${power?.capitalizeWords() ?: "§cNone!"}",
            "§5Abiphone§7: §f${member.crimsonIsle?.abiphone?.activeContacts?.size?.div(2) ?: 0}",
            "§dRift Prism§7: ${if (member.rift.access.consumedPrism) "§aObtained" else "§cMissing"}",
        ) + member.accessoryBagStorage.tuning.currentTunings.entries
            .map { (k, v) -> "§7${k.replace("_", " ").capitalizeWords()}§7: §f$v" }
    }

    private val talismans: List<HypixelData.ItemData?> by resettableLazy {
        PVState.memberData()?.inventory?.bagContents?.get("talisman_bag")?.itemStacks.orEmpty()
    }

    fun resetState() {
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
            "Wardrobe" -> drawPagedGrid(ctx, x, contentY, w, contentH, mouseX, mouseY,
                inv?.wardrobeContents?.itemStacks.orEmpty(), cols = 9, pageSize = 36)
            "Talismans" -> drawTalismans(ctx, x, contentY, w, contentH, mouseX, mouseY, member, inv)
            "Backpacks" -> drawBackpacks(ctx, x, contentY, w, contentH, mouseX, mouseY, inv)
            "Ender Chest" -> drawPagedGrid(ctx, x, contentY, w, contentH, mouseX, mouseY,
                inv?.eChestContents?.itemStacks.orEmpty(), cols = 9, pageSize = 45)
        }
    }

    private fun drawBasic(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, inv: HypixelData.Inventory?) {
        val armor = inv?.invArmor?.itemStacks?.reversed().orEmpty()
        val equip = inv?.equipment?.itemStacks.orEmpty()
        val raw = inv?.invContents?.itemStacks.orEmpty()
        val allItems = armor + listOf(null) + equip + raw.drop(9) + raw.take(9)
        drawSlotGrid(ctx, x, y, w, h, allItems, cols = 9)
    }

    private fun drawTalismans(
        ctx: DrawContext, x: Float, y: Float, w: Float, h: Float,
        mouseX: Double, mouseY: Double,
        member: HypixelData.MemberData, inv: HypixelData.Inventory?,
    ) {
        val infoW = w * 0.32f
        val rightX = x + infoW + PADDING
        val rightW = w - infoW - PADDING

        ctx.rect(x, y, infoW, h, Theme.btnNormal, Theme.round)
        TextBox(
            ctx, x + PADDING, y + PADDING, infoW - PADDING * 2f, h - PADDING * 2f,
            "§5Magical Power§7: ${member.assumedMagicalPower.toDouble().colorCode(1900.0)}${member.assumedMagicalPower}",
            22f, statLines, 20f
        ).draw()

        val talis = talismans
        val totalPages = ((talis.size + 62) / 63).coerceAtLeast(1)
        if (totalPages > 1) {
            val btnW = (rightW - BTN_SPACING * (totalPages - 1)) / totalPages
            for (p in 1..totalPages) {
                val bx = rightX + (p - 1) * (btnW + BTN_SPACING)
                val isSel = p == currentPage
                val isHov = ctx.isHovered(mouseX, mouseY, bx, y, btnW, BTN_H)
                ctx.rect(bx, y, btnW, BTN_H, btnColor(isSel, isHov), TAB_RADIUS)
                val lbl = p.toString()
                ctx.text(lbl, bx + (btnW - ctx.textWidth(lbl, INFO_TEXT)) / 2f, y + (BTN_H - INFO_TEXT) / 2f, INFO_TEXT,
                    if (isSel || isHov) Color(255, 255, 255) else Color(170, 170, 170))
            }
        }
        val gridY = y + if (totalPages > 1) BTN_H + PADDING else 0f
        drawSlotGrid(ctx, rightX, gridY, rightW, h - (gridY - y), talis.drop((currentPage - 1) * 63).take(63), cols = 9)
    }

    private fun drawPagedGrid(
        ctx: DrawContext, x: Float, y: Float, w: Float, h: Float,
        mouseX: Double, mouseY: Double,
        items: List<HypixelData.ItemData?>, cols: Int, pageSize: Int,
    ) {
        val totalPages = ((items.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        if (totalPages > 1) {
            val btnW = (w - BTN_SPACING * (totalPages - 1)) / totalPages
            for (p in 1..totalPages) {
                val bx = x + (p - 1) * (btnW + BTN_SPACING)
                val isSel = p == currentPage
                val isHov = ctx.isHovered(mouseX, mouseY, bx, y, btnW, BTN_H)
                ctx.rect(bx, y, btnW, BTN_H, btnColor(isSel, isHov), TAB_RADIUS)
                val lbl = p.toString()
                ctx.text(lbl, bx + (btnW - ctx.textWidth(lbl, INFO_TEXT)) / 2f, y + (BTN_H - INFO_TEXT) / 2f, INFO_TEXT,
                    if (isSel || isHov) Color(255, 255, 255) else Color(170, 170, 170))
            }
        }
        val gridY = y + if (totalPages > 1) BTN_H + PADDING else 0f
        drawSlotGrid(ctx, x, gridY, w, h - (gridY - y), items.drop((currentPage - 1) * pageSize).take(pageSize), cols)
    }

    private fun drawBackpacks(
        ctx: DrawContext, x: Float, y: Float, w: Float, h: Float,
        mouseX: Double, mouseY: Double, inv: HypixelData.Inventory?,
    ) {
        val backpackKeys = inv?.backpackContents?.keys?.mapNotNull { it.toIntOrNull() }?.sorted() ?: return
        if (backpackKeys.isEmpty()) return

        val btnW = (w - BTN_SPACING * (backpackKeys.size - 1)) / backpackKeys.size
        backpackKeys.forEachIndexed { i, key ->
            val bx = x + i * (btnW + BTN_SPACING)
            val isSel = (i + 1) == currentPage
            val isHov = ctx.isHovered(mouseX, mouseY, bx, y, btnW, BTN_H)
            ctx.rect(bx, y, btnW, BTN_H, btnColor(isSel, isHov), TAB_RADIUS)
            val lbl = (key + 1).toString()
            ctx.text(lbl, bx + (btnW - ctx.textWidth(lbl, INFO_TEXT)) / 2f, y + (BTN_H - INFO_TEXT) / 2f, INFO_TEXT,
                if (isSel || isHov) Color(255, 255, 255) else Color(170, 170, 170))
        }
        val gridY = y + BTN_H + PADDING
        val items = inv.backpackContents[(currentPage - 1).toString()]?.itemStacks.orEmpty()
        drawSlotGrid(ctx, x, gridY, w, h - BTN_H - PADDING, items, cols = 9)
    }

    private fun drawSlotGrid(
        ctx: DrawContext, x: Float, y: Float, w: Float, h: Float,
        items: List<HypixelData.ItemData?>, cols: Int,
    ) {
        if (items.isEmpty()) return
        val rows = (items.size + cols - 1) / cols
        val slotSize = minOf(
            (w - SLOT_SPACING * (cols - 1)) / cols,
            (h - SLOT_SPACING * (rows - 1)) / rows,
        )
        items.forEachIndexed { idx, item ->
            val col = idx % cols
            val row = idx / cols
            val sx = x + col * (slotSize + SLOT_SPACING)
            val sy = y + row * (slotSize + SLOT_SPACING)
            val bgColor = if (ProfileViewerModule.rarityBackgrounds && item != null)
                Theme.rarityFromLore(item.lore)
            else
                Theme.slotBg
            ctx.rect(sx, sy, slotSize, slotSize, bgColor, SLOT_RADIUS)
            item?.asItemStack?.let { stack -> ctx.item(stack, sx, sy, slotSize) }
        }
    }

    override fun onClick(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        val member = PVState.memberData() ?: return
        if (!member.inventoryApi) return

        val x = PVLayout.MAIN_X
        val y = PVLayout.MAIN_Y
        val w = PVLayout.MAIN_W

        val tabW = (w - TAB_SPACING * (SUB_PAGES.size - 1)) / SUB_PAGES.size
        SUB_PAGES.forEachIndexed { i, tab ->
            val tx = x + i * (tabW + TAB_SPACING)
            if (ctx.isHovered(mouseX, mouseY, tx, y, tabW, TAB_H)) {
                if (tab != currentSubPage) { currentSubPage = tab; currentPage = 1 }
                return
            }
        }

        val contentY = y + TAB_H + PADDING
        val inv = member.inventory
        when (currentSubPage) {
            "Backpacks" -> {
                val backpackKeys = inv?.backpackContents?.keys?.mapNotNull { it.toIntOrNull() }?.sorted() ?: return
                val btnW = (w - BTN_SPACING * (backpackKeys.size - 1)) / backpackKeys.size
                backpackKeys.forEachIndexed { i, _ ->
                    val bx = x + i * (btnW + BTN_SPACING)
                    if (ctx.isHovered(mouseX, mouseY, bx, contentY, btnW, BTN_H)) { currentPage = i + 1; return }
                }
            }
            "Wardrobe", "Ender Chest" -> {
                val items = if (currentSubPage == "Wardrobe") inv?.wardrobeContents?.itemStacks.orEmpty()
                else inv?.eChestContents?.itemStacks.orEmpty()
                val pageSize = if (currentSubPage == "Wardrobe") 36 else 45
                val total = ((items.size + pageSize - 1) / pageSize).coerceAtLeast(1)
                if (total > 1) {
                    val btnW = (w - BTN_SPACING * (total - 1)) / total
                    for (p in 1..total) {
                        val bx = x + (p - 1) * (btnW + BTN_SPACING)
                        if (ctx.isHovered(mouseX, mouseY, bx, contentY, btnW, BTN_H)) { currentPage = p; return }
                    }
                }
            }
            "Talismans" -> {
                val infoW = w * 0.32f
                val rightX = x + infoW + PADDING
                val rightW = w - infoW - PADDING
                val total = ((talismans.size + 62) / 63).coerceAtLeast(1)
                if (total > 1) {
                    val btnW = (rightW - BTN_SPACING * (total - 1)) / total
                    for (p in 1..total) {
                        val bx = rightX + (p - 1) * (btnW + BTN_SPACING)
                        if (ctx.isHovered(mouseX, mouseY, bx, contentY, btnW, BTN_H)) { currentPage = p; return }
                    }
                }
            }
        }
    }

    private fun List<HypixelData.ItemData?>?.orEmpty(): List<HypixelData.ItemData?> = this ?: emptyList()
}