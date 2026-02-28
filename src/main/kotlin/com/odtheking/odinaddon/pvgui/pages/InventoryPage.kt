package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.capitalizeWords
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.components.ButtonRow
import com.odtheking.odinaddon.pvgui.components.SlotGrid
import com.odtheking.odinaddon.pvgui.components.TextBox
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import com.odtheking.odinaddon.pvgui.utils.colorCode
import com.odtheking.odinaddon.pvgui.utils.resettableLazy

object InventoryPage : PVPage("Inventory") {
    private val SUB_PAGES = listOf("Basic", "Wardrobe", "Talismans", "Backpacks", "Ender Chest")
    private const val TAB_H = 45f
    private const val SLOT_SPACING = 6f
    private const val INFO_TEXT = 16f
    private const val BTN_H = 32f
    private const val BTN_SPACING = 5f

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

    private var wardrobeButtons: ButtonRow<Int>? = null
    private var eChestButtons: ButtonRow<Int>? = null
    private var talismanButtons: ButtonRow<Int>? = null
    private var backpackButtons: ButtonRow<Int>? = null

    private val subPageButtons = ButtonRow(
        x = mainX, y = mainY, w = mainW, h = TAB_H,
        items = SUB_PAGES,
        spacing = 5f,
        label = { if (it == "Ender Chest") "E.Chest" else it },
        textSize = 20f,
    ) { onSelect { tab -> currentSubPage = tab; currentPage = 1 } }

    fun resetState() {
        currentSubPage = "Basic"
        currentPage = 1
        wardrobeButtons = null
        eChestButtons = null
        talismanButtons = null
        backpackButtons = null
    }

    override fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, mouseX: Double, mouseY: Double) {
        val data = member ?: return
        if (!data.inventoryApi) {
            val msg = "API is disabled for this profile"
            ctx.text(msg, x + (w - ctx.textWidth(msg, 32f)) / 2f, y + h / 2f, 32f, Color(255, 85, 85))
            return
        }
        val inv = data.inventory

        subPageButtons.selected = currentSubPage
        subPageButtons.draw(ctx, mouseX, mouseY)

        val contentY = y + TAB_H + padding
        val contentH = h - TAB_H - padding

        when (currentSubPage) {
            "Basic" -> drawBasic(ctx, x, contentY, w, contentH, mouseX, mouseY, inv)
            "Wardrobe" -> drawPagedGrid(ctx, x, contentY, w, contentH, mouseX, mouseY, inv?.wardrobeContents?.itemStacks.orEmpty(), cols = 9, pageSize = 36, { wardrobeButtons }, { wardrobeButtons = it })
            "Talismans" -> drawTalismans(ctx, x, contentY, w, contentH, mouseX, mouseY, data, inv)
            "Backpacks" -> drawBackpacks(ctx, x, contentY, w, contentH, mouseX, mouseY, inv)
            "Ender Chest" -> drawPagedGrid(ctx, x, contentY, w, contentH, mouseX, mouseY, inv?.eChestContents?.itemStacks.orEmpty(), cols = 9, pageSize = 45, { eChestButtons }, { eChestButtons = it })
        }
    }

    private fun drawBasic(
        ctx: DrawContext, x: Float, y: Float, w: Float, h: Float,
        mouseX: Double, mouseY: Double, inv: HypixelData.Inventory?,
    ) {
        val armor = inv?.invArmor?.itemStacks?.reversed().orEmpty()
        val equip = inv?.equipment?.itemStacks.orEmpty()
        val raw = inv?.invContents?.itemStacks.orEmpty()
        val allItems = armor + listOf(null) + equip + raw.drop(9) + raw.take(9)
        slotGrid(ctx, x, y, w, h, mouseX, mouseY, allItems, cols = 9)
    }

    private fun drawTalismans(
        ctx: DrawContext, x: Float, y: Float, w: Float, h: Float,
        mouseX: Double, mouseY: Double,
        data: HypixelData.MemberData, inv: HypixelData.Inventory?,
    ) {
        val infoW = w * 0.32f
        val rightX = x + infoW + padding
        val rightW = w - infoW - padding

        ctx.rect(x, y, infoW, h, Theme.btnNormal, Theme.round)
        TextBox(
            x + padding, y + padding, infoW - padding * 2f, h - padding * 2f,
            "§5Magical Power§7: ${data.assumedMagicalPower.toDouble().colorCode(1900.0)}${data.assumedMagicalPower}",
            22f, statLines, 20f
        ).draw(ctx, mouseX, mouseY)

        val talis = talismans
        val totalPages = ((talis.size + 62) / 63).coerceAtLeast(1)
        val gridTop: Float

        if (totalPages > 1) {
            val row = talismanButtons ?: ButtonRow(rightX, y, rightW, BTN_H, (1..totalPages).toList(), BTN_SPACING, { it.toString() }, INFO_TEXT) {
                onSelect { currentPage = it }
            }.also { talismanButtons = it }
            row.selected = currentPage
            row.draw(ctx, mouseX, mouseY)
            gridTop = y + BTN_H + padding
        } else {
            gridTop = y
        }
        slotGrid(ctx, rightX, gridTop, rightW, h - (gridTop - y), mouseX, mouseY,
            talis.drop((currentPage - 1) * 63).take(63), cols = 9)
    }

    private fun drawPagedGrid(
        ctx: DrawContext, x: Float, y: Float, w: Float, h: Float,
        mouseX: Double, mouseY: Double,
        items: List<HypixelData.ItemData?>, cols: Int, pageSize: Int,
        getRow: () -> ButtonRow<Int>?,
        setRow: (ButtonRow<Int>) -> Unit,
    ) {
        val totalPages = ((items.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        val gridTop: Float

        if (totalPages > 1) {
            val row = getRow() ?: ButtonRow(x, y, w, BTN_H, (1..totalPages).toList(), BTN_SPACING, { it.toString() }, INFO_TEXT) {
                onSelect { currentPage = it }
            }.also(setRow)
            row.selected = currentPage
            row.draw(ctx, mouseX, mouseY)
            gridTop = y + BTN_H + padding
        } else {
            gridTop = y
        }
        slotGrid(ctx, x, gridTop, w, h - (gridTop - y), mouseX, mouseY,
            items.drop((currentPage - 1) * pageSize).take(pageSize), cols)
    }

    private fun drawBackpacks(
        ctx: DrawContext, x: Float, y: Float, w: Float, h: Float,
        mouseX: Double, mouseY: Double, inv: HypixelData.Inventory?,
    ) {
        val backpackKeys = inv?.backpackContents?.keys?.mapNotNull { it.toIntOrNull() }?.sorted() ?: return
        if (backpackKeys.isEmpty()) return

        val row = backpackButtons ?: ButtonRow(x, y, w, BTN_H, backpackKeys.map { it + 1 }, BTN_SPACING, { it.toString() }, INFO_TEXT) {
            onSelect { currentPage = it }
        }.also { backpackButtons = it }
        row.selected = currentPage
        row.draw(ctx, mouseX, mouseY)

        val items = inv.backpackContents[(currentPage - 1).toString()]?.itemStacks.orEmpty()
        slotGrid(ctx, x, y + BTN_H + padding, w, h - BTN_H - padding, mouseX, mouseY, items, cols = 9)
    }

    private fun slotGrid(
        ctx: DrawContext, x: Float, y: Float, w: Float, h: Float,
        mouseX: Double, mouseY: Double,
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

        SlotGrid(
            x = startX, y = startY, w = gridW, h = gridH,
            items = items,
            cols = cols,
            spacing = SLOT_SPACING,
            toItemStack = { it?.asItemStack ?: net.minecraft.world.item.ItemStack.EMPTY },
            slotColor = { item -> if (ProfileViewerModule.rarityBackgrounds && item != null)
                Theme.rarityFromLore(item.lore)
            else
                Theme.slotBg
            },
        ).draw(ctx, mouseX, mouseY)
    }

    override fun onClick(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        val data = member ?: return
        if (!data.inventoryApi) return
        if (subPageButtons.click(ctx, mouseX, mouseY)) return

        val contentY = mainY + TAB_H + padding
        when (currentSubPage) {
            "Wardrobe"    -> wardrobeButtons?.click(ctx, mouseX, mouseY)
            "Ender Chest" -> eChestButtons?.click(ctx, mouseX, mouseY)
            "Backpacks"   -> backpackButtons?.click(ctx, mouseX, mouseY)
            "Talismans"   -> talismanButtons?.click(ctx, mouseX, mouseY)
        }
    }

    private fun List<HypixelData.ItemData?>?.orEmpty(): List<HypixelData.ItemData?> = this ?: emptyList()
}