package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.capitalizeWords
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.PADDING
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.components.Buttons
import com.odtheking.odinaddon.pvgui.components.SlotGrid
import com.odtheking.odinaddon.pvgui.components.TextBox
import com.odtheking.odinaddon.pvgui.components.asText
import com.odtheking.odinaddon.pvgui.core.RenderContext
import com.odtheking.odinaddon.pvgui.core.Renderer
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import com.odtheking.odinaddon.pvgui.utils.colorCode
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import net.minecraft.world.item.ItemStack

object InventoryPage : PVPage() {
    override val name = "Inventory"
    private const val TAB_H = 45f
    private const val SLOT_SPACING = 6f
    private const val INFO_TEXT = 16f
    private const val BTN_H = 32f
    private const val BTN_SPACING = 5f
    private val SUB_PAGES = listOf("Basic", "Wardrobe", "Talismans", "Backpacks", "Ender Chest")

    var currentTab = "Basic"
        private set
    var currentPage = 1
        private set

    private val statLines: List<String> by resettableLazy {
        val data = member ?: return@resettableLazy emptyList()
        val power = data.accessoryBagStorage.selectedPower
        listOf(
            "§aSelected Power§7: §6${power?.capitalizeWords() ?: "§cNone!"}",
            "§5Abiphone§7: §f${data.crimsonIsle?.abiphone?.activeContacts?.size?.div(2) ?: 0}",
            "§dRift Prism§7: ${if (data.rift.access.consumedPrism) "§aObtained" else "§cMissing"}",
        ) + data.tunings.map { "§7$it" }
    }

    private val talismans: List<HypixelData.ItemData?> by resettableLazy {
        member?.inventory?.bagContents?.get("talisman_bag")?.itemStacks.orEmpty()
    }

    private var wardrobePageBtns: Buttons<Int>? = null
    private var eChestPageBtns: Buttons<Int>? = null
    private var talismanPageBtns: Buttons<Int>? = null
    private var backpackPageBtns: Buttons<Int>? = null

    private val tabs = Buttons(
        items = SUB_PAGES,
        spacing = 5f,
        textSize = 20f,
        vertical = false,
        label = { if (it == "Ender Chest") "E.Chest" else it },
    ) { tab ->
        currentTab = tab
        currentPage = 1
        resetPageBtns()
    }

    fun resetState() {
        currentTab = "Basic"
        currentPage = 1
        resetPageBtns()
    }

    private fun resetPageBtns() {
        wardrobePageBtns = null
        eChestPageBtns = null
        talismanPageBtns = null
        backpackPageBtns = null
    }

    override fun draw(ctx: RenderContext) {
        val data = member ?: return
        if (!data.inventoryApi) {
            val msg = "API is disabled for this profile"
            val tw = Renderer.textWidth(msg, 32f)
            Renderer.text(msg, x + (w - tw) / 2f, y + h / 2f - 16f, 32f, 0xFFFF5555.toInt())
            return
        }

        tabs.selected = currentTab
        tabs.setBounds(x, y, w, TAB_H)
        tabs.draw(ctx)

        val contentY = y + TAB_H + PADDING
        val contentH = h - TAB_H - PADDING
        val inv = data.inventory

        when (currentTab) {
            "Basic" -> drawBasic(ctx, x, contentY, w, contentH, inv)
            "Wardrobe" -> drawPaged(ctx, x, contentY, w, contentH,
                items = inv?.wardrobeContents?.itemStacks.orEmpty(),
                cols = 9, pageSize = 36,
                get = { wardrobePageBtns }, set = { wardrobePageBtns = it })
            "Talismans" -> drawTalismans(ctx, x, contentY, w, contentH, data, inv)
            "Backpacks" -> drawBackpacks(ctx, x, contentY, w, contentH, inv)
            "Ender Chest" -> drawPaged(ctx, x, contentY, w, contentH,
                items = inv?.eChestContents?.itemStacks.orEmpty(),
                cols = 9, pageSize = 45,
                get = { eChestPageBtns }, set = { eChestPageBtns = it })
        }
    }

    private fun drawBasic(ctx: RenderContext, x: Float, y: Float, w: Float, h: Float, inv: HypixelData.Inventory?) {
        val armor = inv?.invArmor?.itemStacks?.reversed().orEmpty()
        val equip = inv?.equipment?.itemStacks.orEmpty()
        val raw = inv?.invContents?.itemStacks.orEmpty()
        renderGrid(ctx, x, y, w, h, armor + listOf(null) + equip + raw.drop(9) + raw.take(9), cols = 9)
    }

    private fun drawTalismans(ctx: RenderContext, x: Float, y: Float, w: Float, h: Float, data: HypixelData.MemberData, inv: HypixelData.Inventory?) {
        val infoW = w * 0.32f
        val rightX = x + infoW + PADDING
        val rightW = w - infoW - PADDING

        Renderer.rect(x, y, infoW, h, Theme.btnNormal, Theme.radius)
        TextBox(
            lines = statLines.map { it.asText() },
            maxSize = INFO_TEXT,
            title = "§5Magical Power§7: ${data.assumedMagicalPower.toDouble().colorCode(1900.0)}${data.assumedMagicalPower}",
            titleScale = 22f
        ).also {
            it.setBounds(x + PADDING, y + PADDING, infoW - PADDING * 2f, h - PADDING * 2f)
            it.draw(ctx)
        }

        val talis = talismans
        val totalPages = ((talis.size + 62) / 63).coerceAtLeast(1)
        val gridY = if (totalPages > 1) {
            val row = talismanPageBtns ?: makePageBtns(rightX, y, rightW, BTN_H, totalPages).also { talismanPageBtns = it }
            row.selected = currentPage
            row.setBounds(rightX, y, rightW, BTN_H)
            row.draw(ctx)
            y + BTN_H + PADDING
        } else y

        renderGrid(ctx, rightX, gridY, rightW, h - (gridY - y), talis.drop((currentPage - 1) * 63).take(63), cols = 9)
    }

    private fun drawPaged(ctx: RenderContext, x: Float, y: Float, w: Float, h: Float, items: List<HypixelData.ItemData?>, cols: Int, pageSize: Int, get: () -> Buttons<Int>?, set: (Buttons<Int>) -> Unit) {
        val totalPages = ((items.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        val gridY = if (totalPages > 1) {
            val row = get() ?: makePageBtns(x, y, w, BTN_H, totalPages).also(set)
            row.selected = currentPage
            row.setBounds(x, y, w, BTN_H)
            row.draw(ctx)
            y + BTN_H + PADDING
        } else y

        renderGrid(ctx, x, gridY, w, h - (gridY - y), items.drop((currentPage - 1) * pageSize).take(pageSize), cols)
    }

    private fun drawBackpacks(ctx: RenderContext, x: Float, y: Float, w: Float, h: Float, inv: HypixelData.Inventory?) {
        val keys = inv?.backpackContents?.keys?.mapNotNull { it.toIntOrNull() }?.sorted() ?: return
        if (keys.isEmpty()) return

        val row = backpackPageBtns ?: makePageBtns(x, y, w, BTN_H, keys.size, keys.map { it + 1 }).also { backpackPageBtns = it }
        row.selected = currentPage
        row.setBounds(x, y, w, BTN_H)
        row.draw(ctx)

        val items = inv.backpackContents[(currentPage - 1).toString()]?.itemStacks.orEmpty()
        renderGrid(ctx, x, y + BTN_H + PADDING, w, h - BTN_H - PADDING, items, cols = 9)
    }

    private fun makePageBtns(x: Float, y: Float, w: Float, h: Float, count: Int, keys: List<Int> = (1..count).toList()): Buttons<Int> =
        Buttons(items = keys, spacing = BTN_SPACING, textSize = INFO_TEXT, vertical = false, label = { it.toString() }) { currentPage = it }
            .also { it.selected = keys.first() }

    private fun renderGrid(ctx: RenderContext, x: Float, y: Float, w: Float, h: Float, items: List<HypixelData.ItemData?>, cols: Int) {
        if (items.isEmpty()) return
        val rows = (items.size + cols - 1) / cols
        val slotSize = minOf(
            (w - SLOT_SPACING * (cols - 1)) / cols,
            (h - SLOT_SPACING * (rows - 1)) / rows,
        )
        val gridW = slotSize * cols + SLOT_SPACING * (cols - 1)
        val gridH = slotSize * rows + SLOT_SPACING * (rows - 1)

        SlotGrid(
            items = items,
            cols = cols,
            spacing = SLOT_SPACING,
            toStack = { it?.asItemStack ?: ItemStack.EMPTY },
            itemBg = { item ->
                if (ProfileViewerModule.rarityBackgrounds && item != null) Theme.rarityFromLore(item.lore)
                else Theme.slotBg
            },
        ).also {
            it.setBounds(x + (w - gridW) / 2f, y + (h - gridH) / 2f, gridW, gridH)
            it.draw(ctx)
        }
    }
}