package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.capitalizeWords
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.utils.TextBox
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import com.odtheking.odinaddon.pvgui.utils.colorCode
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import com.odtheking.odinaddon.pvgui.utils.ButtonGroup

object InventoryPage : PVPage("Inventory") {
    private val SUB_PAGES = listOf("Basic", "Wardrobe", "Talismans", "Backpacks", "Ender Chest")
    private const val TAB_H = 42f
    private const val PADDING = 8f
    private const val SLOT_SPACING = 6f
    private const val INFO_TEXT = 16f
    private const val BTN_H = 26f
    private const val BTN_SPACING = 5f
    private val SLOT_RADIUS get() = Theme.round

    var currentSubPage = "Basic"
    var currentPage = 1

    private val statLines: List<String> by resettableLazy {
        val data = member ?: return@resettableLazy emptyList()
        val power = data.accessoryBagStorage.selectedPower
        listOf(
            "§aSelected Power§7: §6${power?.capitalizeWords() ?: "§cNone!"}",
            "§5Abiphone§7: §f${data.crimsonIsle?.abiphone?.activeContacts?.size?.div(2) ?: 0}",
            "§dRift Prism§7: ${if (data.rift.access.consumedPrism) "§aObtained" else "§cMissing"}",
        ) + data.accessoryBagStorage.tuning.currentTunings.entries
            .map { (k, v) -> "§7${k.replace("_", " ").capitalizeWords()}§7: §f$v" }
    }

    private val talismans: List<HypixelData.ItemData?> by resettableLazy {
        member?.inventory?.bagContents?.get("talisman_bag")?.itemStacks.orEmpty()
    }

    private val subPageButtons = ButtonGroup(
        x = mainX, y = mainY, w = mainW, h = TAB_H,
        options = SUB_PAGES,
        spacing = 5f,
        vertical = false,
        label = { if (it == "Ender Chest") "E.Chest" else it },
        textSize = 18f,
    ).apply {
        onSelect { tab -> currentSubPage = tab; currentPage = 1 }
    }
    fun resetState() {
        currentSubPage = "Basic"
        currentPage = 1
    }

    override fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, mouseX: Double, mouseY: Double) {
        val data = member ?: return
        if (!data.inventoryApi) {
            val msg = "API is disabled for this profile"
            ctx.text(msg, x + (w - ctx.textWidth(msg, 32f)) / 2f, y + h / 2f, 32f, Color(255, 85, 85))
            return
        }
        val inv = member!!.inventory

        subPageButtons.selected = currentSubPage
        subPageButtons.draw(ctx, mouseX, mouseY)

        val contentY = y + TAB_H + PADDING
        val contentH = h - TAB_H - PADDING

        when (currentSubPage) {
            "Basic" -> drawBasic(ctx, x, contentY, w, contentH, inv)
            "Wardrobe" -> drawPagedGrid(ctx, x, contentY, w, contentH, mouseX, mouseY,
                inv?.wardrobeContents?.itemStacks.orEmpty(), cols = 9, pageSize = 36)
            "Talismans" -> drawTalismans(ctx, x, contentY, w, contentH, mouseX, mouseY, member!!, inv)
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
        val totalTalisPages = ((talis.size + 62) / 63).coerceAtLeast(1)
        val talisGridTop: Float

        if (totalTalisPages > 1) {
            ButtonGroup(
                x = rightX, y = y, w = rightW, h = BTN_H,
                options = (1..totalTalisPages).toList(),
                spacing = BTN_SPACING,
                label = { it.toString() },
                textSize = INFO_TEXT,
                default = currentPage,
            ).draw(ctx, mouseX, mouseY)
            talisGridTop = y + BTN_H + PADDING
        } else {
            talisGridTop = y
        }
        drawSlotGrid(ctx, rightX, talisGridTop, rightW, h - (talisGridTop - y),
            talis.drop((currentPage - 1) * 63).take(63), cols = 9)
    }

    private fun drawPagedGrid(
        ctx: DrawContext, x: Float, y: Float, w: Float, h: Float,
        mouseX: Double, mouseY: Double,
        items: List<HypixelData.ItemData?>, cols: Int, pageSize: Int,
    ) {
        val totalPages = ((items.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        val gridTop: Float

        if (totalPages > 1) {
            ButtonGroup(
                x = x, y = y, w = w, h = BTN_H,
                options = (1..totalPages).toList(),
                spacing = BTN_SPACING,
                label = { it.toString() },
                textSize = INFO_TEXT,
                default = currentPage,
            ).draw(ctx, mouseX, mouseY)
            gridTop = y + BTN_H + PADDING
        } else {
            gridTop = y
        }
        drawSlotGrid(ctx, x, gridTop, w, h - (gridTop - y),
            items.drop((currentPage - 1) * pageSize).take(pageSize), cols)
    }

    private fun drawBackpacks(
        ctx: DrawContext, x: Float, y: Float, w: Float, h: Float,
        mouseX: Double, mouseY: Double, inv: HypixelData.Inventory?,
    ) {
        val backpackKeys = inv?.backpackContents?.keys?.mapNotNull { it.toIntOrNull() }?.sorted() ?: return
        if (backpackKeys.isEmpty()) return

        ButtonGroup(
            x = x, y = y, w = w, h = BTN_H,
            options = backpackKeys.map { it + 1 },
            spacing = BTN_SPACING,
            label = { it.toString() },
            textSize = INFO_TEXT,
            default = currentPage,
        ).draw(ctx, mouseX, mouseY)

        val gridTop = y + BTN_H + PADDING
        val items = inv.backpackContents[(currentPage - 1).toString()]?.itemStacks.orEmpty()
        drawSlotGrid(ctx, x, gridTop, w, h - BTN_H - PADDING, items, cols = 9)
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

        val gridW = slotSize * cols + SLOT_SPACING * (cols - 1)
        val gridH = slotSize * rows + SLOT_SPACING * (rows - 1)
        val startX = x + (w - gridW) / 2f
        val startY = y + (h - gridH) / 2f

        items.forEachIndexed { idx, item ->
            val col = idx % cols
            val row = idx / cols
            val slotX = startX + col * (slotSize + SLOT_SPACING)
            val slotY = startY + row * (slotSize + SLOT_SPACING)
            val bgColor = if (ProfileViewerModule.rarityBackgrounds && item != null)
                Theme.rarityFromLore(item.lore)
            else
                Theme.slotBg
            ctx.rect(slotX, slotY, slotSize, slotSize, bgColor, SLOT_RADIUS)
            item?.asItemStack?.let { stack -> ctx.item(stack, slotX, slotY, slotSize) }
        }
    }

    override fun onClick(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        val data = member ?: return
        if (!data.inventoryApi) return

        val x = mainX
        val y = mainY
        val w = mainW

        if (subPageButtons.onClick(ctx, mouseX, mouseY)) return

        val contentY = y + TAB_H + PADDING
        val inv = member!!.inventory
        when (currentSubPage) {
            "Backpacks" -> {
                val backpackKeys = inv?.backpackContents?.keys?.mapNotNull { it.toIntOrNull() }?.sorted() ?: return
                ButtonGroup(
                    x = x, y = contentY, w = w, h = BTN_H,
                    options = backpackKeys.map { it + 1 },
                    spacing = BTN_SPACING,
                    default = currentPage,
                ).also { if (it.onClick(ctx, mouseX, mouseY)) { currentPage = it.selected; return } }
            }
            "Wardrobe", "Ender Chest" -> {
                val pageItems = if (currentSubPage == "Wardrobe") inv?.wardrobeContents?.itemStacks.orEmpty()
                else inv?.eChestContents?.itemStacks.orEmpty()
                val pageSize = if (currentSubPage == "Wardrobe") 36 else 45
                val totalPages = ((pageItems.size + pageSize - 1) / pageSize).coerceAtLeast(1)
                if (totalPages > 1) {
                    ButtonGroup(
                        x = x, y = contentY, w = w, h = BTN_H,
                        options = (1..totalPages).toList(),
                        spacing = BTN_SPACING,
                        default = currentPage,
                    ).also { if (it.onClick(ctx, mouseX, mouseY)) { currentPage = it.selected; return } }
                }
            }
            "Talismans" -> {
                val infoW = w * 0.32f
                val rightX = x + infoW + PADDING
                val rightW = w - infoW - PADDING
                val totalPages = ((talismans.size + 62) / 63).coerceAtLeast(1)
                if (totalPages > 1) {
                    ButtonGroup(
                        x = rightX, y = contentY, w = rightW, h = BTN_H,
                        options = (1..totalPages).toList(),
                        spacing = BTN_SPACING,
                        default = currentPage,
                    ).also { if (it.onClick(ctx, mouseX, mouseY)) { currentPage = it.selected; return } }
                }
            }
        }
    }

    private fun List<HypixelData.ItemData?>?.orEmpty(): List<HypixelData.ItemData?> = this ?: emptyList()
}