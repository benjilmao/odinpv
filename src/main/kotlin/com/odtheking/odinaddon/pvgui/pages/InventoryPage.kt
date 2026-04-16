package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.capitalizeWords
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.CONTENT_X
import com.odtheking.odinaddon.pvgui.CONTENT_Y
import com.odtheking.odinaddon.pvgui.MAIN_H
import com.odtheking.odinaddon.pvgui.MAIN_W
import com.odtheking.odinaddon.pvgui.PAD
import com.odtheking.odinaddon.pvgui.QUAD_W
import com.odtheking.odinaddon.pvgui.INV_BTN_H
import com.odtheking.odinaddon.pvgui.INV_CENTER_Y
import com.odtheking.odinaddon.pvgui.INV_SEP_Y
import com.odtheking.odinaddon.pvgui.INV_START_Y
import com.odtheking.odinaddon.pvgui.INV_TAB_H
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.dsl.ButtonsDsl
import com.odtheking.odinaddon.pvgui.dsl.GridItems
import com.odtheking.odinaddon.pvgui.dsl.ItemGridDsl
import com.odtheking.odinaddon.pvgui.dsl.RenderQueue
import com.odtheking.odinaddon.pvgui.dsl.TextBox
import com.odtheking.odinaddon.pvgui.dsl.buttons
import com.odtheking.odinaddon.pvgui.dsl.itemGrid
import com.odtheking.odinaddon.pvgui.dsl.textBox
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import com.odtheking.odinaddon.pvgui.utils.colorize
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import kotlin.math.ceil
import kotlin.math.floor

object InventoryPage : PVPage() {
    override val name = "Inventory"

    private val inventoryApi: Boolean by resettableLazy { PVState.member()?.inventoryApi ?: false }

    // ── Tab buttons across the top (HC: buttons across mainWidth at y=spacer, h=inventoryPageHeight) ──
    private val tabLabels = listOf("Basic", "Wardrobe", "Talismans", "Backpacks", "Ender Chest")
    private val tabButtons: ButtonsDsl<String> by resettableLazy {
        buttons(
            x = CONTENT_X, y = CONTENT_Y + PAD,
            w = MAIN_W, h = INV_TAB_H,
            items = tabLabels, padding = PAD, vertical = false,
            textSize = 15f, radius = Theme.radius, label = { it },
        ) { subPageForLabel(it).onOpen() }
    }

    private val currentTab   get() = tabButtons.selected ?: "Basic"
    private fun currentSub() = subPageForLabel(currentTab)
    private fun subPageForLabel(label: String) = when (label) {
        "Basic"       -> BasicSub
        "Wardrobe"    -> WardrobeSub
        "Talismans"   -> TalismanSub
        "Backpacks"   -> BackpackSub
        "Ender Chest" -> EnderChestSub
        else          -> BasicSub
    }

    override fun onOpen() {
        BasicSub.onOpen(); WardrobeSub.onOpen()
        TalismanSub.onOpen(); BackpackSub.onOpen(); EnderChestSub.onOpen()
    }

    override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (!inventoryApi) {
            val msg = "Inventory API disabled!"
            val tw = NVGRenderer.textWidth(msg, 32f, NVGRenderer.defaultFont)
            NVGRenderer.text(msg, CONTENT_X + MAIN_W / 2f - tw / 2f,
                CONTENT_Y + MAIN_H / 2f - 16f, 32f, Colors.MINECRAFT_RED.rgba, NVGRenderer.defaultFont)
            return
        }
        tabButtons.draw()
        currentSub().draw(context, mouseX, mouseY)
    }

    override fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (!inventoryApi) return
        currentSub().enqueueItems(context, mouseX, mouseY)
    }

    override fun click(mouseX: Double, mouseY: Double): Boolean {
        if (tabButtons.click(mouseX, mouseY)) return true
        return currentSub().click(mouseX, mouseY)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Sub-pages — each is an object matching HC's inner objects
    // Key fix: grid is rebuilt when page index changes, not cached across pages
    // ══════════════════════════════════════════════════════════════════════════

    abstract class Sub {
        abstract fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int)
        open fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {}
        open fun click(mouseX: Double, mouseY: Double): Boolean = false
        open fun onOpen() {}
    }

    // ── Helper: build ItemGridDsl centered in given area ──────────────────────
    // HC ItemGridDSL: itemWidth = (gridItems.width - (columns-1)*padding) / columns
    //                 y = centerY - height/2 + row*(itemWidth+padding)
    // We replicate by passing the GridItems to itemGrid {}
    private fun makeGrid(
        items: List<ItemStack?>,
        cx: Float, centerY: Float, width: Float, columns: Int,
        padding: Float = PAD / 2f,
        colorFn: (Int, ItemStack?) -> Int = { _, _ -> Theme.slotBg },
    ): ItemGridDsl = itemGrid(
        listOf(GridItems(items, cx, centerY, width, columns)),
        radius = Theme.slotRadius, padding = padding,
    ) { colorHandler { i, s -> colorFn(i, s) } }

    // ── Basic ─────────────────────────────────────────────────────────────────
    object BasicSub : Sub() {
        private val invArmor   by resettableLazy { PVState.member()?.inventory?.invArmor?.itemStacks.orEmpty() }
        private val invContents by resettableLazy { PVState.member()?.inventory?.invContents?.itemStacks.orEmpty() }
        private val equipment  by resettableLazy { PVState.member()?.inventory?.equipment?.itemStacks.orEmpty() }
        private val allItems: List<HypixelData.ItemData?> by resettableLazy {
            val raw = invContents
            val fixed = if (raw.size >= 9) raw.subList(9, raw.size) + raw.subList(0, 9) else raw
            invArmor.reversed() + listOf(null) + equipment + fixed
        }
        private val stacks: List<ItemStack?> by resettableLazy { allItems.map { it?.asItemStack } }

        // HC: centerY = (startY + buttonHeight + spacer) + (mainHeight - (startY+buttonHeight))/2
        // but Basic has no sub-buttons so it uses the full area from INV_SEP_Y
        private val gridCenterY = INV_SEP_Y + (MAIN_H - INV_SEP_Y) / 2f

        private val grid: ItemGridDsl by resettableLazy {
            makeGrid(stacks, CONTENT_X, gridCenterY, MAIN_W, 9, PAD / 2f) { index, _ ->
                when {
                    index == 4 -> Colors.TRANSPARENT.rgba
                    ProfileViewerModule.rarityBackgrounds ->
                        Theme.rarityFromLore(allItems.getOrNull(index)?.lore.orEmpty())
                    else -> Theme.slotBg
                }
            }
        }

        override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) = grid.draw(context, mouseX, mouseY)
        override fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) = grid.enqueueItems(context, mouseX, mouseY)
    }

    // ── Wardrobe ──────────────────────────────────────────────────────────────
    object WardrobeSub : Sub() {
        private val wardrobe:        List<HypixelData.ItemData?> by resettableLazy { PVState.member()?.inventory?.wardrobeContents?.itemStacks.orEmpty() }
        private val wardrobeEquipped: Int?                       by resettableLazy { PVState.member()?.inventory?.wardrobeEquipped }
        private val invArmor:        List<HypixelData.ItemData?> by resettableLazy { PVState.member()?.inventory?.invArmor?.itemStacks.orEmpty() }

        private val pageButtons: ButtonsDsl<Int> by resettableLazy {
            val pageCount = ceil(wardrobe.size.toDouble() / 36).toInt().coerceAtLeast(1)
            buttons(
                x = CONTENT_X, y = INV_START_Y,
                w = MAIN_W, h = INV_BTN_H,
                items = (1..pageCount).toList(), padding = PAD, vertical = false,
                textSize = 15f, radius = Theme.radius, label = { it.toString() },
            ) { /* selection drives getGrid() */ }
        }

        private fun buildPage(pageIndex: Int): Pair<List<HypixelData.ItemData?>, Set<HypixelData.ItemData?>> {
            val start = pageIndex * 36
            val end   = minOf(start + 36, wardrobe.size)
            val page  = wardrobe.subList(start, end).toMutableList()
            val armorSet = invArmor.toSet()
            val equippedRaw  = wardrobeEquipped?.let { it - 1 } ?: -1
            val equippedSlot = if (equippedRaw in start until start + 36) equippedRaw - start else -1
            if (equippedSlot >= 0 && invArmor.isNotEmpty()) {
                invArmor.forEachIndexed { ai, arm ->
                    val slot = equippedSlot + 9 * (invArmor.size - 1 - ai)
                    if (slot < page.size) page[slot] = arm
                }
            }
            return page to armorSet
        }

        // Cache current page grid — rebuilt when page index changes
        private var cachedPageIndex = -1
        private var cachedGrid: ItemGridDsl? = null

        private fun getGrid(): ItemGridDsl {
            val idx = (pageButtons.selected ?: 1) - 1
            if (idx == cachedPageIndex && cachedGrid != null) return cachedGrid!!
            val (pageItems, armorSet) = buildPage(idx)
            val stacks = pageItems.map { it?.asItemStack }
            cachedPageIndex = idx
            cachedGrid = makeGrid(stacks, CONTENT_X, INV_CENTER_Y, MAIN_W, 9, PAD / 2f) { i, _ ->
                val item = pageItems.getOrNull(i)
                when {
                    item != null && item in armorSet -> Colors.MINECRAFT_BLUE.rgba
                    ProfileViewerModule.rarityBackgrounds -> Theme.rarityFromLore(item?.lore.orEmpty())
                    else -> Theme.slotBg
                }
            }
            return cachedGrid!!
        }

        override fun onOpen() { cachedPageIndex = -1; cachedGrid = null }
        override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) { pageButtons.draw(); getGrid().draw(context, mouseX, mouseY) }
        override fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) = getGrid().enqueueItems(context, mouseX, mouseY)
        override fun click(mouseX: Double, mouseY: Double): Boolean {
            val prev = pageButtons.selected
            if (pageButtons.click(mouseX, mouseY)) {
                if (pageButtons.selected != prev) { cachedPageIndex = -1; cachedGrid = null }
                return true
            }
            return false
        }
    }

    // ── Talismans ─────────────────────────────────────────────────────────────
    object TalismanSub : Sub() {
        private val talis: List<HypixelData.ItemData> by resettableLazy {
            PVState.member()?.inventory?.bagContents?.get("talisman_bag")
                ?.itemStacks?.filterNotNull()
                ?.sortedByDescending { it.magicalPower }.orEmpty()
        }
        private val magicPower: Int by resettableLazy { PVState.member()?.magicalPower ?: 0 }
        private val textLines: List<String> by resettableLazy {
            val d = PVState.member() ?: return@resettableLazy emptyList()
            listOf(
                "§aSelected Power§7: §6${d.accessoryBagStorage.selectedPower?.capitalizeWords() ?: "§cNone!"}",
                "§5Abiphone§7: §f${d.crimsonIsle.abiphone.activeContacts.size / 2}",
                "§dRift Prism§7: ${if (d.rift.access.consumedPrism) "§aObtained" else "§cMissing"}",
            ) + d.tunings.map { "§7$it" }
        }

        // HC: textBoxWidth = mainWidth * 0.38
        private val textBoxW  = MAIN_W * 0.38f
        private val separatorX = floor(CONTENT_X + textBoxW).toFloat()
        private val gridW     = MAIN_W - textBoxW - PAD

        private val textBox: TextBox by resettableLazy {
            textBox(CONTENT_X + PAD, INV_START_Y + PAD, textBoxW - 2*PAD, MAIN_H - INV_START_Y - PAD,
                title = "Magical Power: ${magicPower.toDouble().colorize(1800.0, 0)}",
                titleScale = 2.5f, lines = textLines, scale = 2.2f, spacer = PAD, color = Theme.textPrimary)
        }

        private val pages: Int by resettableLazy {
            ceil(talis.size.toDouble() / (ProfileViewerModule.maxRows * 9)).toInt().coerceAtLeast(1)
        }
        private val pageButtons: ButtonsDsl<Int> by resettableLazy {
            buttons(
                x = separatorX + PAD, y = INV_START_Y,
                w = gridW, h = INV_BTN_H,
                items = (1..pages).toList(), padding = PAD, vertical = false,
                textSize = 15f, radius = Theme.radius, label = { it.toString() },
            ) { }
        }

        private val gridCenterY = INV_START_Y + INV_BTN_H + PAD + (MAIN_H - INV_START_Y - INV_BTN_H - PAD) / 2f

        private var cachedPageIndex = -1
        private var cachedGrid: ItemGridDsl? = null

        private fun getGrid(): ItemGridDsl {
            val idx = (pageButtons.selected ?: 1) - 1
            if (idx == cachedPageIndex && cachedGrid != null) return cachedGrid!!
            val pageSize = ProfileViewerModule.maxRows * 9
            val start = idx * pageSize
            val end   = minOf(start + pageSize, talis.size)
            val pageItems = if (start < talis.size) talis.subList(start, end) else emptyList()
            val stacks = pageItems.map { it.asItemStack }
            cachedPageIndex = idx
            cachedGrid = makeGrid(stacks, separatorX + PAD, gridCenterY, gridW, 9, PAD / 2f) { i, _ ->
                if (ProfileViewerModule.rarityBackgrounds) Theme.rarityFromLore(pageItems.getOrNull(i)?.lore.orEmpty())
                else Theme.slotBg
            }
            return cachedGrid!!
        }

        override fun onOpen() { cachedPageIndex = -1; cachedGrid = null }
        override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
            NVGRenderer.rect(CONTENT_X, INV_START_Y, textBoxW, MAIN_H - INV_START_Y + PAD, Theme.slotBg, Theme.radius)
            pageButtons.draw(); textBox.draw(); getGrid().draw(context, mouseX, mouseY)
        }
        override fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) = getGrid().enqueueItems(context, mouseX, mouseY)
        override fun click(mouseX: Double, mouseY: Double): Boolean {
            val prev = pageButtons.selected
            if (pageButtons.click(mouseX, mouseY)) {
                if (pageButtons.selected != prev) { cachedPageIndex = -1; cachedGrid = null }
                return true
            }
            return false
        }
    }

    // ── Backpacks ─────────────────────────────────────────────────────────────
    object BackpackSub : Sub() {
        private val backpackKeys: List<Int> by resettableLazy {
            PVState.member()?.inventory?.backpackContents?.keys
                ?.mapNotNull { it.toIntOrNull()?.plus(1) }?.sorted().orEmpty()
        }
        private val pageButtons: ButtonsDsl<Int> by resettableLazy {
            buttons(
                x = CONTENT_X, y = INV_START_Y,
                w = MAIN_W, h = INV_BTN_H,
                items = backpackKeys, padding = PAD, vertical = false,
                textSize = 15f, radius = Theme.radius, label = { it.toString() },
            ) { }
        }

        // HC: centerY uses mainCenterX but width is mainWidth*0.8
        private val gridW     = MAIN_W * 0.8f
        private val gridX     = CONTENT_X + (MAIN_W - gridW) / 2f
        private val gridCenterY = INV_START_Y + INV_BTN_H + PAD + (MAIN_H - INV_START_Y - INV_BTN_H - PAD) / 2f

        private var cachedKey = -1
        private var cachedGrid: ItemGridDsl? = null

        private fun getGrid(): ItemGridDsl {
            val key = (pageButtons.selected ?: backpackKeys.firstOrNull()) ?: return emptyGrid()
            if (key == cachedKey && cachedGrid != null) return cachedGrid!!
            val items = PVState.member()?.inventory?.backpackContents?.get((key-1).toString())?.itemStacks.orEmpty()
            val stacks = items.map { it?.asItemStack }
            cachedKey = key
            cachedGrid = makeGrid(stacks, gridX, gridCenterY, gridW, 9, PAD / 2f) { i, _ ->
                if (ProfileViewerModule.rarityBackgrounds) Theme.rarityFromLore(items.getOrNull(i)?.lore.orEmpty())
                else Theme.slotBg
            }
            return cachedGrid!!
        }

        private fun emptyGrid() = itemGrid(listOf(GridItems(emptyList(), gridX, gridCenterY, gridW, 9)))

        override fun onOpen() { cachedKey = -1; cachedGrid = null }
        override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) { pageButtons.draw(); getGrid().draw(context, mouseX, mouseY) }
        override fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) = getGrid().enqueueItems(context, mouseX, mouseY)
        override fun click(mouseX: Double, mouseY: Double): Boolean {
            val prev = pageButtons.selected
            if (pageButtons.click(mouseX, mouseY)) {
                if (pageButtons.selected != prev) { cachedKey = -1; cachedGrid = null }
                return true
            }
            return false
        }
    }

    // ── Ender Chest ───────────────────────────────────────────────────────────
    object EnderChestSub : Sub() {
        private val items: List<HypixelData.ItemData?> by resettableLazy {
            PVState.member()?.inventory?.eChestContents?.itemStacks.orEmpty()
        }
        private val pages: Int by resettableLazy { ceil(items.size / 45.0).toInt().coerceAtLeast(1) }
        private val pageButtons: ButtonsDsl<Int> by resettableLazy {
            buttons(
                x = CONTENT_X, y = INV_START_Y,
                w = MAIN_W, h = INV_BTN_H,
                items = (1..pages).toList(), padding = PAD, vertical = false,
                textSize = 15f, radius = Theme.radius, label = { it.toString() },
            ) { }
        }

        private val gridW     = MAIN_W * 0.8f
        private val gridX     = CONTENT_X + (MAIN_W - gridW) / 2f
        private val gridCenterY = INV_START_Y + INV_BTN_H + PAD + (MAIN_H - INV_START_Y - INV_BTN_H - PAD) / 2f

        private var cachedPageIndex = -1
        private var cachedGrid: ItemGridDsl? = null

        private fun getGrid(): ItemGridDsl {
            val idx = (pageButtons.selected ?: 1) - 1
            if (idx == cachedPageIndex && cachedGrid != null) return cachedGrid!!
            val start = idx * 45
            val end   = minOf(start + 45, items.size)
            val pageItems = if (start < items.size) items.subList(start, end) else emptyList()
            val stacks = pageItems.map { it?.asItemStack }
            cachedPageIndex = idx
            cachedGrid = makeGrid(stacks, gridX, gridCenterY, gridW, 9, PAD / 2f) { i, _ ->
                if (ProfileViewerModule.rarityBackgrounds) Theme.rarityFromLore(pageItems.getOrNull(i)?.lore.orEmpty())
                else Theme.slotBg
            }
            return cachedGrid!!
        }

        override fun onOpen() { cachedPageIndex = -1; cachedGrid = null }
        override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) { pageButtons.draw(); getGrid().draw(context, mouseX, mouseY) }
        override fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) = getGrid().enqueueItems(context, mouseX, mouseY)
        override fun click(mouseX: Double, mouseY: Double): Boolean {
            val prev = pageButtons.selected
            if (pageButtons.click(mouseX, mouseY)) {
                if (pageButtons.selected != prev) { cachedPageIndex = -1; cachedGrid = null }
                return true
            }
            return false
        }
    }
}