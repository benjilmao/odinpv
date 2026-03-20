package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.capitalizeWords
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.PVPage
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.dsl.TextBox
import com.odtheking.odinaddon.pvgui.nodes.ItemGridNode
import com.odtheking.odinaddon.pvgui.nodes.PagerNode
import com.odtheking.odinaddon.pvgui.nodes.TabNode
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.api.HypixelData
import com.odtheking.odinaddon.pvgui.utils.colorize
import com.odtheking.odinaddon.pvgui.utils.resettableLazy
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack

object InventoryPage : PVPage() {
    override val name = "Inventory"

    private val spacing = 10f
    private val tabs = listOf("Basic", "Wardrobe", "Talismans", "Backpacks", "Ender Chest")

    private val wardrobePager = PagerNode(pageItems = { wardrobeContents }, pageSize = 36, spacing = spacing,
        content = { pageIndex, pageItems, px, py, pw, ph, ctx, mx, my ->
            drawWardrobePage(pageIndex, pageItems, px, py, pw, ph, ctx, mx, my)
        },
        onEnqueueItems = { pageIndex, pageItems, px, py, pw, ph, ctx, mx, my ->
            enqueueWardrobePage(pageIndex, pageItems, px, py, pw, ph, ctx, mx, my)
        },
    )
    private val talismanPager = PagerNode(pageItems = { talisman }, pageSize = 63, spacing = spacing,
        content = { _, pageItems, px, py, pw, ph, ctx, mx, my ->
            drawTalismanGrid(pageItems, px, py, pw, ph, ctx, mx, my)
        },
        onEnqueueItems = { _, pageItems, px, py, pw, ph, ctx, mx, my ->
            enqueueGrid(pageItems.map { it?.asItemStack }, px, py, pw, ph, ctx, mx, my)
        },
    )
    private val backpackPager = PagerNode(pageItems = { backpackKeys }, pageSize = 1, spacing = spacing,
        labelProvider = { backpackKeys.getOrNull(it)?.toString() ?: "" },
        content = { _, pageKeys, px, py, pw, ph, ctx, mx, my ->
            drawBackpackPage(pageKeys, px, py, pw, ph, ctx, mx, my)
        },
        onEnqueueItems = { _, pageKeys, px, py, pw, ph, ctx, mx, my ->
            val key = (pageKeys.firstOrNull() ?: backpackKeys.firstOrNull() ?: return@PagerNode) - 1
            val items = PVState.member()?.inventory?.backpackContents?.get(key.toString())?.itemStacks.orEmpty()
            enqueueGrid(items.map { it?.asItemStack }, px, py, pw, ph, ctx, mx, my)
        },
    )
    private val eChestPager = PagerNode(pageItems = { eChest }, pageSize = 45, spacing = spacing,
        content = { _, pageItems, px, py, pw, ph, ctx, mx, my ->
            drawEChestPage(pageItems, px, py, pw, ph, ctx, mx, my)
        },
        onEnqueueItems = { _, pageItems, px, py, pw, ph, ctx, mx, my ->
            enqueueGrid(pageItems.map { it?.asItemStack }, px, py, pw, ph, ctx, mx, my)
        },
    )

    private val tabNode = TabNode(
        tabs = tabs,
        spacing = spacing,
        content = { tab, tx, ty, tw, th, context, mouseX, mouseY ->
            drawTab(tab, tx, ty, tw, th, context, mouseX, mouseY)
        },
        onEnqueueItems = { tab, tx, ty, tw, th, context, mouseX, mouseY ->
            enqueueTab(tab, tx, ty, tw, th, context, mouseX, mouseY)
        },
        onContentClick = { tab, mouseX, mouseY, tx, ty, tw, th ->
            when (tab) {
                "Wardrobe" -> wardrobePager.click(mouseX, mouseY, tx, ty, tw, th)
                "Talismans" -> {
                    val panelWidth = tw * 0.38f
                    val gridX = tx + panelWidth + spacing
                    val gridWidth = tw - panelWidth - spacing
                    talismanPager.click(mouseX, mouseY, gridX, ty, gridWidth, th)
                }
                "Backpacks" -> backpackPager.click(mouseX, mouseY, tx, ty, tw, th)
                "Ender Chest" -> eChestPager.click(mouseX, mouseY, tx, ty, tw, th)
                else -> false
            }
        },
    )

    private val invArmor by resettableLazy { PVState.member()?.inventory?.invArmor?.itemStacks.orEmpty() }
    private val invContents by resettableLazy { PVState.member()?.inventory?.invContents?.itemStacks.orEmpty() }
    private val equipment by resettableLazy { PVState.member()?.inventory?.equipment?.itemStacks.orEmpty() }
    private val wardrobeContents by resettableLazy { PVState.member()?.inventory?.wardrobeContents?.itemStacks.orEmpty() }
    private val wardrobeEquipped by resettableLazy { PVState.member()?.inventory?.wardrobeEquipped }
    private val talisman by resettableLazy {
        PVState.member()?.inventory?.bagContents?.get("talisman_bag")?.itemStacks
            ?.filterNotNull()?.sortedByDescending { it.magicalPower }.orEmpty()
    }
    private val eChest by resettableLazy { PVState.member()?.inventory?.eChestContents?.itemStacks.orEmpty() }
    private val backpackKeys by resettableLazy {
        PVState.member()?.inventory?.backpackContents?.keys
            ?.mapNotNull { it.toIntOrNull()?.plus(1) }?.sorted().orEmpty()
    }
    private val magicPower by resettableLazy { PVState.member()?.magicalPower ?: 0 }
    private val taliTextLines by resettableLazy {
        val data = PVState.member() ?: return@resettableLazy emptyList<String>()
        listOf(
            "§aSelected Power§7: §6${data.accessoryBagStorage.selectedPower?.capitalizeWords() ?: "§cNone!"}",
            "§5Abiphone§7: §f${data.crimsonIsle?.abiphone?.activeContacts?.size?.div(2) ?: 0}",
            "§dRift Prism§7: ${if (data.rift.access.consumedPrism) "§aObtained" else "§cMissing"}",
        ) + data.tunings.map { "§7$it" }
    }

    override fun onOpen() {
        tabNode.reset()
        wardrobePager.reset(); talismanPager.reset(); backpackPager.reset(); eChestPager.reset()
    }

    override fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (PVState.member() == null) { centeredText("No data loaded", Theme.textSecondary); return }
        if (PVState.member()?.inventoryApi == false) { centeredText("Inventory API disabled", Colors.MINECRAFT_RED.rgba); return }
        tabNode.draw(x, y, w, h, context, mouseX, mouseY)
    }

    override fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (PVState.member() == null || PVState.member()?.inventoryApi == false) return
        tabNode.enqueueItems(x, y, w, h, context, mouseX, mouseY)
    }

    override fun click(mouseX: Double, mouseY: Double): Boolean {
        if (PVState.member() == null) return false
        return tabNode.click(mouseX, mouseY, x, y, w, h)
    }

    private fun drawTab(tab: String, tx: Float, ty: Float, tw: Float, th: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) {
        when (tab) {
            "Basic" -> drawBasic(tx, ty, tw, th, context, mouseX, mouseY)
            "Wardrobe" -> wardrobePager.draw(tx, ty, tw, th, context, mouseX, mouseY)
            "Talismans" -> drawTalismans(tx, ty, tw, th, context, mouseX, mouseY)
            "Backpacks" -> backpackPager.draw(tx, ty, tw, th, context, mouseX, mouseY)
            "Ender Chest" -> eChestPager.draw(tx, ty, tw, th, context, mouseX, mouseY)
        }
    }

    private fun enqueueTab(tab: String, tx: Float, ty: Float, tw: Float, th: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) {
        when (tab) {
            "Basic" -> enqueueBasic(tx, ty, tw, th, context, mouseX, mouseY)
            "Wardrobe" -> wardrobePager.enqueueItems(tx, ty, tw, th, context, mouseX, mouseY)
            "Talismans" -> {
                val panelWidth = tw * 0.38f
                val gridX = tx + panelWidth + spacing
                val gridWidth = tw - panelWidth - spacing
                talismanPager.enqueueItems(gridX, ty, gridWidth, th, context, mouseX, mouseY)
            }
            "Backpacks" -> backpackPager.enqueueItems(tx, ty, tw, th, context, mouseX, mouseY)
            "Ender Chest" -> eChestPager.enqueueItems(tx, ty, tw, th, context, mouseX, mouseY)
        }
    }

    private fun drawBasic(tx: Float, ty: Float, tw: Float, th: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val raw = invContents
        val fixedInv = if (raw.size >= 9) raw.subList(9, raw.size) + raw.subList(0, 9) else raw
        val items: List<HypixelData.ItemData?> = invArmor.reversed() + listOf(null) + equipment + fixedInv
        val stacks = items.map { it?.asItemStack }
        ItemGridNode(columns = 9, gap = spacing / 2f, items = { stacks },
            colors = { _, index ->
                if (index == 4) Colors.TRANSPARENT.rgba
                else if (ProfileViewerModule.rarityBackgrounds) Theme.rarityFromLore(items.getOrNull(index)?.lore.orEmpty())
                else Theme.slotBg
            },
        ).also { it.setBounds(tx, ty, tw, th) }.draw(context, mouseX, mouseY)
    }

    private fun enqueueBasic(tx: Float, ty: Float, tw: Float, th: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val raw = invContents
        val fixedInv = if (raw.size >= 9) raw.subList(9, raw.size) + raw.subList(0, 9) else raw
        val items: List<HypixelData.ItemData?> = invArmor.reversed() + listOf(null) + equipment + fixedInv
        val stacks = items.map { it?.asItemStack }
        ItemGridNode(columns = 9, gap = spacing / 2f, items = { stacks },
            colors = { _, index ->
                if (index == 4) Colors.TRANSPARENT.rgba
                else if (ProfileViewerModule.rarityBackgrounds) Theme.rarityFromLore(items.getOrNull(index)?.lore.orEmpty())
                else Theme.slotBg
            },
        ).also { it.setBounds(tx, ty, tw, th) }.enqueueItems(context, mouseX, mouseY)
    }

    private fun drawWardrobePage(pageIndex: Int, pageItems: List<HypixelData.ItemData?>, px: Float, py: Float, pw: Float, ph: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val (mutablePage, armorSet) = buildWardrobePage(pageIndex, pageItems)
        val stacks = mutablePage.map { it?.asItemStack }
        ItemGridNode(columns = 9, gap = spacing / 2f, items = { stacks },
            colors = { _, index ->
                val item = mutablePage.getOrNull(index)
                if (item != null && item in armorSet) Colors.MINECRAFT_BLUE.rgba
                else if (ProfileViewerModule.rarityBackgrounds) Theme.rarityFromLore(item?.lore.orEmpty())
                else Theme.slotBg
            },
        ).also { it.setBounds(px, py, pw, ph) }.draw(context, mouseX, mouseY)
    }

    private fun enqueueWardrobePage(pageIndex: Int, pageItems: List<HypixelData.ItemData?>, px: Float, py: Float, pw: Float, ph: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val (mutablePage, armorSet) = buildWardrobePage(pageIndex, pageItems)
        val stacks = mutablePage.map { it?.asItemStack }
        ItemGridNode(columns = 9, gap = spacing / 2f, items = { stacks },
            colors = { _, index ->
                val item = mutablePage.getOrNull(index)
                if (item != null && item in armorSet) Colors.MINECRAFT_BLUE.rgba
                else if (ProfileViewerModule.rarityBackgrounds) Theme.rarityFromLore(item?.lore.orEmpty())
                else Theme.slotBg
            },
        ).also { it.setBounds(px, py, pw, ph) }.enqueueItems(context, mouseX, mouseY)
    }

    private fun buildWardrobePage(pageIndex: Int, pageItems: List<HypixelData.ItemData?>): Pair<MutableList<HypixelData.ItemData?>, Set<HypixelData.ItemData?>> {
        val mutablePage = pageItems.toMutableList()
        val armorSet = invArmor.toSet()
        val equipped = wardrobeEquipped?.let { it - 1 } ?: -1
        val rangeStart = pageIndex * 9
        val equippedSlot = if (equipped in rangeStart until rangeStart + 9) equipped - rangeStart else -1
        if (equippedSlot >= 0 && invArmor.isNotEmpty()) {
            invArmor.forEachIndexed { armorIndex, armItem ->
                val slot = equippedSlot + 9 * (invArmor.size - 1 - armorIndex)
                if (slot < mutablePage.size) mutablePage[slot] = armItem
            }
        }
        return mutablePage to armorSet
    }

    private fun drawTalismans(tx: Float, ty: Float, tw: Float, th: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val panelWidth = tw * 0.38f
        val gridX = tx + panelWidth + spacing
        val gridWidth = tw - panelWidth - spacing
        TextBox(x = tx, y = ty, w = panelWidth, h = th,
            lines = taliTextLines, textSize = 17f,
            title = "§5Magical Power§7: ${magicPower.toDouble().colorize(1800.0, 0)}", titleSize = 22f,
            background = Theme.slotBg).draw()
        talismanPager.draw(gridX, ty, gridWidth, th, context, mouseX, mouseY)
    }

    private fun drawTalismanGrid(pageItems: List<HypixelData.ItemData?>, px: Float, py: Float, pw: Float, ph: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val stacks = pageItems.map { it?.asItemStack }
        ItemGridNode(columns = 9, gap = spacing / 2f, items = { stacks },
            colors = { _, index ->
                if (ProfileViewerModule.rarityBackgrounds) Theme.rarityFromLore(pageItems.getOrNull(index)?.lore.orEmpty())
                else Theme.slotBg
            },
        ).also { it.setBounds(px, py, pw, ph) }.draw(context, mouseX, mouseY)
    }

    private fun drawBackpackPage(pageKeys: List<Int>, px: Float, py: Float, pw: Float, ph: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val key = (pageKeys.firstOrNull() ?: backpackKeys.firstOrNull() ?: return) - 1
        val items = PVState.member()?.inventory?.backpackContents?.get(key.toString())?.itemStacks.orEmpty()
        val stacks = items.map { it?.asItemStack }
        ItemGridNode(columns = 9, gap = spacing / 2f, items = { stacks },
            colors = { _, index ->
                if (ProfileViewerModule.rarityBackgrounds) Theme.rarityFromLore(items.getOrNull(index)?.lore.orEmpty())
                else Theme.slotBg
            },
        ).also { it.setBounds(px, py, pw, ph) }.draw(context, mouseX, mouseY)
    }

    private fun drawEChestPage(pageItems: List<HypixelData.ItemData?>, px: Float, py: Float, pw: Float, ph: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val stacks = pageItems.map { it?.asItemStack }
        ItemGridNode(columns = 9, gap = spacing / 2f, items = { stacks },
            colors = { _, index ->
                if (ProfileViewerModule.rarityBackgrounds) Theme.rarityFromLore(pageItems.getOrNull(index)?.lore.orEmpty())
                else Theme.slotBg
            },
        ).also { it.setBounds(px, py, pw, ph) }.draw(context, mouseX, mouseY)
    }

    private fun enqueueGrid(stacks: List<ItemStack?>, px: Float, py: Float, pw: Float, ph: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) {
        ItemGridNode(columns = 9, gap = spacing / 2f, items = { stacks },
        ).also { it.setBounds(px, py, pw, ph) }.enqueueItems(context, mouseX, mouseY)
    }
}