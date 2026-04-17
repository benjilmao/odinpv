package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.capitalizeWords
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.CONTENT_X
import com.odtheking.odinaddon.pvgui.CONTENT_Y
import com.odtheking.odinaddon.pvgui.INV_BTN_H
import com.odtheking.odinaddon.pvgui.INV_CENTER_Y
import com.odtheking.odinaddon.pvgui.INV_SEP_Y
import com.odtheking.odinaddon.pvgui.INV_START_Y
import com.odtheking.odinaddon.pvgui.INV_TAB_H
import com.odtheking.odinaddon.pvgui.INV_TALI_GRID_W
import com.odtheking.odinaddon.pvgui.INV_TALI_GRID_X
import com.odtheking.odinaddon.pvgui.MAIN_H
import com.odtheking.odinaddon.pvgui.MAIN_W
import com.odtheking.odinaddon.pvgui.PAD
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.dsl.ButtonsDsl
import com.odtheking.odinaddon.pvgui.dsl.buttons
import com.odtheking.odinaddon.pvgui.dsl.textBox
import com.odtheking.odinaddon.pvgui.dsl.ItemGridDsl
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import com.odtheking.odinaddon.pvgui.utils.colorize
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.ceil
import kotlin.math.floor

object InventoryPage : PVPage() {
    override val name = "Inventory"

    private val tabLabels = listOf("Basic", "Wardrobe", "Talismans", "Backpacks", "Ender Chest")
    private val tabButtons: ButtonsDsl<String> by resettableLazy {
        buttons(
            x = CONTENT_X, y = CONTENT_Y,
            w = MAIN_W, h = INV_TAB_H,
            items = tabLabels, padding = PAD, vertical = false,
            textSize = 18f, radius = Theme.radius, label = { it },
        ) { subPageForLabel(it).onOpen() }
    }

    private val currentTab get() = tabButtons.selected ?: "Basic"
    private fun currentSub() = subPageForLabel(currentTab)
    private fun subPageForLabel(l: String) = when (l) {
        "Basic" -> BasicSub
        "Wardrobe" -> WardrobeSub
        "Talismans" -> TalismanSub
        "Backpacks" -> BackpackSub
        "Ender Chest" -> EnderChestSub
        else -> BasicSub
    }

    override fun onOpen() {
        BasicSub.onOpen(); WardrobeSub.onOpen()
        TalismanSub.onOpen(); BackpackSub.onOpen(); EnderChestSub.onOpen()
    }

    override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val member = PVState.member()
        if (member == null || !member.inventoryApi) {
            centeredText(if (member == null) "No data loaded" else "Inventory API disabled", Colors.MINECRAFT_RED.rgba)
            return
        }
        tabButtons.draw()
        currentSub().draw(context, mouseX, mouseY)
    }

    override fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (PVState.member()?.inventoryApi != true) return
        currentSub().enqueueItems(context, mouseX, mouseY)
    }

    override fun click(mouseX: Double, mouseY: Double): Boolean {
        if (tabButtons.click(mouseX, mouseY)) return true
        return currentSub().click(mouseX, mouseY)
    }

    abstract class Sub {
        abstract fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int)
        open fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {}
        open fun click(mouseX: Double, mouseY: Double): Boolean = false
        open fun onOpen() {}
    }

    private fun buildGrid(
        items: List<HypixelData.ItemData?>,
        gx: Float = CONTENT_X,
        centerY: Float = INV_CENTER_Y,
        width: Float = MAIN_W,
        columns: Int = 9,
        colorFn: (Int, HypixelData.ItemData?) -> Int = { _, _ -> Theme.slotBg },
    ): ItemGridDsl {
        val stacks = items.map { it?.asItemStack }
        return ItemGridDsl(columns = columns, gap = PAD,
            items = { stacks },
            colors = { _, i -> colorFn(i, items.getOrNull(i)) },
        ).also { it.setCenterBounds(gx, centerY, width) }
    }

    object BasicSub : Sub() {
        private val BASIC_CONTENT_START = INV_SEP_Y + PAD
        private val BASIC_CENTER_Y = BASIC_CONTENT_START + ((CONTENT_Y + MAIN_H) - BASIC_CONTENT_START) / 2f

        private val invArmor by resettableLazy { PVState.member()?.inventory?.invArmor?.itemStacks.orEmpty() }
        private val invContents by resettableLazy { PVState.member()?.inventory?.invContents?.itemStacks.orEmpty() }
        private val equipment by resettableLazy { PVState.member()?.inventory?.equipment?.itemStacks.orEmpty() }
        private val allItems: List<HypixelData.ItemData?> by resettableLazy {
            val raw = invContents
            val fixed = if (raw.size >= 9) raw.subList(9, raw.size) + raw.subList(0, 9) else raw
            invArmor.reversed() + listOf(null) + equipment + fixed
        }

        private fun grid() = buildGrid(allItems, centerY = BASIC_CENTER_Y) { index, item ->
            when {
                index == 4 -> Colors.TRANSPARENT.rgba
                ProfileViewerModule.rarityBackgrounds -> Theme.rarityFromLore(item?.lore.orEmpty())
                else -> Theme.slotBg
            }
        }

        override fun draw(c: GuiGraphics, mx: Int, my: Int) = grid().draw(c, mx, my)
        override fun enqueueItems(c: GuiGraphics, mx: Int, my: Int) = grid().enqueueItems(c, mx, my)
    }

    object WardrobeSub : Sub() {
        private val wardrobe by resettableLazy { PVState.member()?.inventory?.wardrobeContents?.itemStacks.orEmpty() }
        private val invArmor by resettableLazy { PVState.member()?.inventory?.invArmor?.itemStacks.orEmpty() }
        private val wardrobeEquipped by resettableLazy { PVState.member()?.inventory?.wardrobeEquipped }
        private val pages by resettableLazy { ceil(wardrobe.size.toDouble() / 36).toInt().coerceAtLeast(1) }

        private val pageButtons: ButtonsDsl<Int> by resettableLazy {
            buttons(x = CONTENT_X, y = INV_START_Y, w = MAIN_W, h = INV_BTN_H,
                items = (1..pages).toList(), padding = PAD, vertical = false,
                textSize = 18f, radius = Theme.radius, label = { it.toString() },
            ) { cachedIdx = -1; cachedGrid = null }
        }

        private var cachedIdx = -1; private var cachedGrid: ItemGridDsl? = null

        private fun getGrid(): ItemGridDsl {
            val idx = (pageButtons.selected ?: 1) - 1
            if (idx == cachedIdx && cachedGrid != null) return cachedGrid!!
            val start = idx * 36
            val page = wardrobe.subList(start, minOf(start + 36, wardrobe.size)).toMutableList<HypixelData.ItemData?>()
            val armorSet = invArmor.toSet()
            val eq = wardrobeEquipped?.let { it - 1 } ?: -1
            val eqLocal = if (eq in start until start + 36) eq - start else -1
            if (eqLocal >= 0 && invArmor.isNotEmpty()) {
                invArmor.forEachIndexed { ai, arm ->
                    val slot = eqLocal + 9 * (invArmor.size - 1 - ai)
                    if (slot < page.size) page[slot] = arm
                }
            }
            cachedIdx = idx
            cachedGrid = buildGrid(page) { _, item ->
                when {
                    item != null && item in armorSet -> Colors.MINECRAFT_BLUE.rgba
                    ProfileViewerModule.rarityBackgrounds -> Theme.rarityFromLore(item?.lore.orEmpty())
                    else -> Theme.slotBg
                }
            }
            return cachedGrid!!
        }

        override fun onOpen() { cachedIdx = -1; cachedGrid = null }
        override fun draw(c: GuiGraphics, mx: Int, my: Int) { pageButtons.draw(); getGrid().draw(c, mx, my) }
        override fun enqueueItems(c: GuiGraphics, mx: Int, my: Int) = getGrid().enqueueItems(c, mx, my)
        override fun click(mx: Double, my: Double): Boolean {
            val prev = pageButtons.selected
            if (pageButtons.click(mx, my)) { if (pageButtons.selected != prev) { cachedIdx = -1; cachedGrid = null }; return true }
            return false
        }
    }

    object TalismanSub : Sub() {
        private val talis by resettableLazy {
            PVState.member()?.inventory?.bagContents?.get("talisman_bag")?.itemStacks
                ?.filterNotNull()?.sortedByDescending { it.magicalPower }.orEmpty()
        }
        private val magicPower by resettableLazy { PVState.member()?.magicalPower ?: 0 }
        private val textLines by resettableLazy {
            val d = PVState.member() ?: return@resettableLazy emptyList<String>()
            listOf(
                "§aSelected Power: §6${d.accessoryBagStorage.selectedPower?.capitalizeWords() ?: "§cNone!"}",
                "§5Abicase: ${floor(d.crimsonIsle.abiphone.activeContacts.size / 2.0).toInt()}",
                "§dRift Prism: ${if (d.rift.access.consumedPrism) "§aObtained" else "§cMissing"}",
            ) + d.tunings
        }
        private val pages by resettableLazy {
            ceil(talis.size.toDouble() / (ProfileViewerModule.maxRows * 9)).toInt().coerceAtLeast(1)
        }

        private val pageButtons: ButtonsDsl<Int> by resettableLazy {
            buttons(x = INV_TALI_GRID_X, y = INV_START_Y, w = INV_TALI_GRID_W, h = INV_BTN_H,
                items = (1..pages).toList(), padding = PAD, vertical = false,
                textSize = 18f, radius = Theme.radius, label = { it.toString() },
            ) { cachedPage = -1; cachedGrid = null }
        }

        private var cachedPage = -1; private var cachedGrid: ItemGridDsl? = null

        private fun getGrid(): ItemGridDsl {
            val idx = (pageButtons.selected ?: 1) - 1
            if (idx == cachedPage && cachedGrid != null) return cachedGrid!!
            val pageSize = ProfileViewerModule.maxRows * 9
            val start = idx * pageSize
            val pageItems = if (start < talis.size) talis.subList(start, minOf(start + pageSize, talis.size)) else emptyList()
            val stacks = pageItems.map { it.asItemStack }
            cachedPage = idx
            cachedGrid = ItemGridDsl(columns = 9, gap = PAD,
                items = { stacks },
                colors = { _, i ->
                    if (ProfileViewerModule.rarityBackgrounds) Theme.rarityFromLore(pageItems.getOrNull(i)?.lore.orEmpty())
                    else Theme.slotBg
                },
            ).also { it.setCenterBounds(INV_TALI_GRID_X, INV_CENTER_Y, INV_TALI_GRID_W) }
            return cachedGrid!!
        }

        override fun onOpen() { cachedPage = -1; cachedGrid = null }
        override fun draw(c: GuiGraphics, mx: Int, my: Int) {
            NVGRenderer.rect(CONTENT_X, INV_START_Y, MAIN_W * 0.38f, MAIN_H - INV_START_Y + PAD, Theme.slotBg, Theme.radius)
            textBox(CONTENT_X + PAD, INV_START_Y + PAD, MAIN_W * 0.38f - 2f * PAD, MAIN_H - INV_START_Y - PAD,
                title = "§5Magical Power§7: ${magicPower.toDouble().colorize(1800.0, 0)}",
                titleScale = 2.5f, lines = textLines, scale = 2.2f, spacer = PAD,
                color = Theme.textPrimary).draw()
            pageButtons.draw()
            getGrid().draw(c, mx, my)
        }
        override fun enqueueItems(c: GuiGraphics, mx: Int, my: Int) = getGrid().enqueueItems(c, mx, my)
        override fun click(mx: Double, my: Double): Boolean {
            val prev = pageButtons.selected
            if (pageButtons.click(mx, my)) { if (pageButtons.selected != prev) { cachedPage = -1; cachedGrid = null }; return true }
            return false
        }
    }

    object BackpackSub : Sub() {
        private val backpackKeys by resettableLazy {
            PVState.member()?.inventory?.backpackContents?.keys
                ?.mapNotNull { it.toIntOrNull()?.plus(1) }?.sorted().orEmpty()
        }
        private val gridW = MAIN_W * 0.8f
        private val gridX = CONTENT_X + (MAIN_W - gridW) / 2f

        private val pageButtons: ButtonsDsl<Int> by resettableLazy {
            buttons(x = CONTENT_X, y = INV_START_Y, w = MAIN_W, h = INV_BTN_H,
                items = backpackKeys, padding = PAD, vertical = false,
                textSize = 18f, radius = Theme.radius, label = { it.toString() },
            ) { cachedKey = -1; cachedGrid = null }
        }

        private var cachedKey = -1; private var cachedGrid: ItemGridDsl? = null

        private fun getGrid(): ItemGridDsl {
            val key = (pageButtons.selected ?: backpackKeys.firstOrNull()) ?: return empty()
            if (key == cachedKey && cachedGrid != null) return cachedGrid!!
            val items = PVState.member()?.inventory?.backpackContents?.get((key - 1).toString())?.itemStacks.orEmpty()
            val stacks = items.map { it?.asItemStack }
            cachedKey = key
            cachedGrid = ItemGridDsl(columns = 9, gap = PAD,
                items = { stacks },
                colors = { _, i -> if (ProfileViewerModule.rarityBackgrounds) Theme.rarityFromLore(items.getOrNull(i)?.lore.orEmpty()) else Theme.slotBg },
            ).also { it.setCenterBounds(gridX, INV_CENTER_Y, gridW) }
            return cachedGrid!!
        }

        private fun empty() = ItemGridDsl(columns = 9, gap = PAD, items = { emptyList() })
            .also { it.setCenterBounds(gridX, INV_CENTER_Y, gridW) }

        override fun onOpen() { cachedKey = -1; cachedGrid = null }
        override fun draw(c: GuiGraphics, mx: Int, my: Int) { pageButtons.draw(); getGrid().draw(c, mx, my) }
        override fun enqueueItems(c: GuiGraphics, mx: Int, my: Int) = getGrid().enqueueItems(c, mx, my)
        override fun click(mx: Double, my: Double): Boolean {
            val prev = pageButtons.selected
            if (pageButtons.click(mx, my)) { if (pageButtons.selected != prev) { cachedKey = -1; cachedGrid = null }; return true }
            return false
        }
    }

    object EnderChestSub : Sub() {
        private val items by resettableLazy { PVState.member()?.inventory?.eChestContents?.itemStacks.orEmpty() }
        private val pages by resettableLazy { ceil(items.size / 45.0).toInt().coerceAtLeast(1) }
        private val gridW = MAIN_W * 0.8f
        private val gridX = CONTENT_X + (MAIN_W - gridW) / 2f

        private val pageButtons: ButtonsDsl<Int> by resettableLazy {
            buttons(x = CONTENT_X, y = INV_START_Y, w = MAIN_W, h = INV_BTN_H,
                items = (1..pages).toList(), padding = PAD, vertical = false,
                textSize = 18f, radius = Theme.radius, label = { it.toString() },
            ) { cachedIdx = -1; cachedGrid = null }
        }

        private var cachedIdx = -1; private var cachedGrid: ItemGridDsl? = null

        private fun getGrid(): ItemGridDsl {
            val idx = (pageButtons.selected ?: 1) - 1
            if (idx == cachedIdx && cachedGrid != null) return cachedGrid!!
            val start = idx * 45
            val pageItems = if (start < items.size) items.subList(start, minOf(start + 45, items.size)) else emptyList()
            val stacks = pageItems.map { it?.asItemStack }
            cachedIdx = idx
            cachedGrid = ItemGridDsl(columns = 9, gap = PAD,
                items = { stacks },
                colors = { _, i -> if (ProfileViewerModule.rarityBackgrounds) Theme.rarityFromLore(pageItems.getOrNull(i)?.lore.orEmpty()) else Theme.slotBg },
            ).also { it.setCenterBounds(gridX, INV_CENTER_Y, gridW) }
            return cachedGrid!!
        }

        override fun onOpen() { cachedIdx = -1; cachedGrid = null }
        override fun draw(c: GuiGraphics, mx: Int, my: Int) { pageButtons.draw(); getGrid().draw(c, mx, my) }
        override fun enqueueItems(c: GuiGraphics, mx: Int, my: Int) = getGrid().enqueueItems(c, mx, my)
        override fun click(mx: Double, my: Double): Boolean {
            val prev = pageButtons.selected
            if (pageButtons.click(mx, my)) { if (pageButtons.selected != prev) { cachedIdx = -1; cachedGrid = null }; return true }
            return false
        }
    }
}