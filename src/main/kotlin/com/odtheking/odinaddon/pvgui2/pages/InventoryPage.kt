package com.odtheking.odinaddon.pvgui2.pages

import com.odtheking.odin.utils.capitalizeWords
import com.odtheking.odinaddon.pvgui2.utils.HypixelData
import com.odtheking.odinaddon.pvgui2.utils.profileOrSelected
import com.odtheking.odinaddon.pvgui2.Theme
import com.odtheking.odinaddon.pvgui2.PVGui
import com.odtheking.odinaddon.pvgui2.textList
import com.odtheking.odinaddon.pvgui2.withRoundedBackground
import com.odtheking.odinaddon.pvgui2.utils.Utils
import me.owdding.lib.builder.LayoutFactory
import me.owdding.lib.displays.Display
import me.owdding.lib.displays.Displays
import me.owdding.lib.displays.asButtonLeft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import kotlin.math.ceil
import kotlin.math.floor

object InventoryPage {

    var currentSubPage = "Basic"
    private var currentWardrobePage = 1
    private var currentTalismanPage = 1
    private var currentEnderPage = 1
    private var currentBackpack = 1

    private val subPages = listOf("Basic", "Wardrobe", "Talismans", "Backpacks", "Ender Chest")

    fun reset() {
        currentSubPage = "Basic"
        currentWardrobePage = 1
        currentTalismanPage = 1
        currentEnderPage = 1
        currentBackpack = 1
    }

    fun build(screen: PVGui, addWidget: (AbstractWidget) -> Unit) {
        val spacer = screen.spacer
        val mainX = screen.mainX
        val mainY = screen.mainY
        val mainWidth = screen.mainWidth
        val mainHeight = screen.mainHeight

        val data = screen.playerData
        if (data == null) {
            LayoutFactory.vertical(0) {
                display(Displays.center(mainWidth, mainHeight, Displays.text(screen.loadText, shadow = true)))
            }.apply { setPosition(mainX, mainY) }.visitWidgets { addWidget(it) }
            return
        }

        val profile = data.profileOrSelected(screen.profileName)?.members?.get(data.uuid) ?: return
        val inv = profile.inventory
        val navHeight = 16
        val buttonWidth = (mainWidth - spacer * (subPages.size - 1)) / subPages.size

        LayoutFactory.horizontal(spacer) {
            subPages.forEach { page ->
                val isSelected = page == currentSubPage
                val btn = Displays.center(buttonWidth, navHeight,
                    Displays.text(page, color = { if (isSelected) TextColor.WHITE.toUInt() else 0xFFAAAAAA.toUInt() })
                ).withRoundedBackground(if (isSelected) Theme.buttonSelect else Theme.buttonBg, Theme.btnRound)
                    .asButtonLeft { currentSubPage = page; screen.init() }
                    .also { it.withTexture(null) }
                widget(btn)
            }
        }.apply { setPosition(mainX, mainY) }.visitWidgets { addWidget(it) }

        LayoutFactory.vertical(0) {
            display(object : Display {
                override fun getWidth() = mainWidth
                override fun getHeight() = 1
                override fun render(graphics: GuiGraphics) { graphics.fill(0, 0, mainWidth, 1, Theme.separator) }
            })
        }.apply { setPosition(mainX, mainY + navHeight + spacer) }.visitWidgets { addWidget(it) }

        val gridY = mainY + navHeight + spacer * 2
        val gridHeight = mainHeight - navHeight - spacer * 2

        when (currentSubPage) {
            "Basic"      -> buildBasic(screen, inv, gridY, gridHeight, addWidget)
            "Wardrobe"   -> buildWardrobe(screen, inv, gridY, gridHeight, addWidget)
            "Talismans"  -> buildTalismans(screen, profile, inv, gridY, gridHeight, addWidget)
            "Backpacks"  -> buildBackpacks(screen, inv, gridY, gridHeight, addWidget)
            "Ender Chest"-> buildEnderChest(screen, inv, gridY, gridHeight, addWidget)
        }
    }

    private fun buildBasic(
        screen: PVGui, inv: HypixelData.Inventory?,
        gridY: Int, gridHeight: Int, addWidget: (AbstractWidget) -> Unit
    ) {
        val armorItems = inv?.invArmor?.itemStacks?.reversed() ?: emptyList()
        val equipmentItems = inv?.equipment?.itemStacks ?: emptyList()
        val rawItems = inv?.invContents?.itemStacks ?: emptyList()

        val hotbarItems = rawItems.take(9)
        val mainItems = rawItems.drop(9)

        val topRow = armorItems + listOf(null) + equipmentItems
        val allItems = topRow + mainItems + hotbarItems
        val totalRows = (allItems.size + 8) / 9

        buildSlotGrid(
            screen = screen,
            items = allItems,
            cols = 9,
            gridY = gridY,
            gridHeight = gridHeight,
            addWidget = addWidget,
            colorProvider = { index, itemData ->
                val row = index / 9
                val isHotbar = row == totalRows - 1
                when {
                    isHotbar -> 0x55225522U
                    Theme.rarityBg && itemData != null -> Theme.rarityFromLore(itemData.lore)
                    else -> Theme.slotBg
                }
            }
        )
    }

    private fun buildWardrobe(
        screen: PVGui, inv: HypixelData.Inventory?,
        gridY: Int, gridHeight: Int, addWidget: (AbstractWidget) -> Unit
    ) {
        val allItems = inv?.wardrobeContents?.itemStacks ?: emptyList()
        val pageSize = 36
        val totalPages = ceil(allItems.size / pageSize.toDouble()).toInt().coerceAtLeast(1)
        currentWardrobePage = currentWardrobePage.coerceIn(1, totalPages)

        buildPagedSlotGrid(
            screen = screen,
            allItems = allItems,
            pageSize = pageSize,
            currentPage = currentWardrobePage,
            totalPages = totalPages,
            gridY = gridY,
            gridHeight = gridHeight,
            addWidget = addWidget,
            colorProvider = { _, itemData ->
                if (Theme.rarityBg && itemData != null) Theme.rarityFromLore(itemData.lore) else Theme.slotBg
            }
        ) { currentWardrobePage = it; screen.init() }
    }

    private fun buildTalismans(
        screen: PVGui,
        profile: HypixelData.MemberData,
        inv: HypixelData.Inventory?,
        gridY: Int, gridHeight: Int, addWidget: (AbstractWidget) -> Unit
    ) {
        val spacer = screen.spacer
        val mainX = screen.mainX
        val mainWidth = screen.mainWidth

        val leftW = (mainWidth * 0.38).toInt()
        val rightX = mainX + leftW + spacer
        val rightW = mainWidth - leftW - spacer

        val power = profile.accessoryBagStorage.selectedPower
        val contacts = profile.crimsonIsle.abiphone.activeContacts.size
        val hasPrism = profile.rift.access.consumedPrism
        val magicPower = profile.assumedMagicalPower
        val tunings = profile.accessoryBagStorage.tuning.currentTunings.entries.map { (k, v) ->
            "${k.replace("_", " ").capitalizeWords()}§7: $v"
        }

        val statLines = listOf(
            "§5Magical Power§7: ${Utils.colorize(magicPower.toDouble(), 1900.0)}$magicPower",
            "§aSelected Power§7: §6${power?.capitalizeWords() ?: "§cNone!"}",
            "§5Abiphone§7: ${floor(contacts / 2.0).toInt()}",
            "§dRift Prism§7: ${if (hasPrism) "§aObtained" else "§cMissing"}",
        ) + tunings

        LayoutFactory.vertical(0) {
            display(textList(statLines, leftW - spacer, gridHeight - spacer, scale = 1.1f))
        }.apply { setPosition(mainX + spacer, gridY) }.visitWidgets { addWidget(it) }

        LayoutFactory.vertical(0) {
            display(object : Display {
                override fun getWidth() = 1
                override fun getHeight() = gridHeight
                override fun render(graphics: GuiGraphics) { graphics.fill(0, 0, 1, gridHeight, Theme.separator) }
            })
        }.apply { setPosition(mainX + leftW, gridY) }.visitWidgets { addWidget(it) }

        val allTalismans = inv?.bagContents?.get("talisman_bag")?.itemStacks ?: emptyList()
        val pageSize = 63
        val totalPages = ceil(allTalismans.size / pageSize.toDouble()).toInt().coerceAtLeast(1)
        currentTalismanPage = currentTalismanPage.coerceIn(1, totalPages)

        buildPagedSlotGrid(
            screen = screen,
            allItems = allTalismans,
            pageSize = pageSize,
            currentPage = currentTalismanPage,
            totalPages = totalPages,
            gridY = gridY,
            gridHeight = gridHeight,
            addWidget = addWidget,
            overrideX = rightX,
            overrideWidth = rightW,
            colorProvider = { _, itemData ->
                if (Theme.rarityBg && itemData != null) Theme.rarityFromLore(itemData.lore) else Theme.slotBg
            }
        ) { currentTalismanPage = it; screen.init() }
    }

    private fun buildBackpacks(
        screen: PVGui, inv: HypixelData.Inventory?,
        gridY: Int, gridHeight: Int, addWidget: (AbstractWidget) -> Unit
    ) {
        val spacer = screen.spacer
        val mainX = screen.mainX
        val mainWidth = screen.mainWidth

        val backpacks = inv?.backpackContents ?: emptyMap()
        if (backpacks.isEmpty()) {
            LayoutFactory.vertical(0) {
                display(Displays.center(mainWidth, gridHeight, Displays.text("§7No backpacks found.", shadow = true)))
            }.apply { setPosition(mainX, gridY) }.visitWidgets { addWidget(it) }
            return
        }

        val backpackKeys = backpacks.keys.mapNotNull { it.toIntOrNull()?.plus(1) }.sorted()
        currentBackpack = currentBackpack.coerceIn(1, backpackKeys.max())

        val bpNavHeight = 14
        val bpBtnWidth = (mainWidth - spacer * (backpackKeys.size - 1)) / backpackKeys.size.coerceAtLeast(1)

        LayoutFactory.horizontal(spacer) {
            backpackKeys.forEach { key ->
                val isSelected = key == currentBackpack
                val btn = Displays.center(bpBtnWidth, bpNavHeight,
                    Displays.text("$key", color = { if (isSelected) TextColor.WHITE.toUInt() else 0xFFAAAAAA.toUInt() })
                ).withRoundedBackground(if (isSelected) Theme.buttonSelect else Theme.buttonBg, Theme.btnRound)
                    .asButtonLeft { currentBackpack = key; screen.init() }
                    .also { it.withTexture(null) }
                widget(btn)
            }
        }.apply { setPosition(mainX, gridY) }.visitWidgets { addWidget(it) }

        val bpItems = backpacks["${currentBackpack - 1}"]?.itemStacks ?: emptyList()
        buildSlotGrid(
            screen = screen,
            items = bpItems,
            cols = 9,
            gridY = gridY + bpNavHeight + spacer,
            gridHeight = gridHeight - bpNavHeight - spacer,
            addWidget = addWidget,
            colorProvider = { _, itemData ->
                if (Theme.rarityBg && itemData != null) Theme.rarityFromLore(itemData.lore) else Theme.slotBg
            }
        )
    }

    private fun buildEnderChest(
        screen: PVGui, inv: HypixelData.Inventory?,
        gridY: Int, gridHeight: Int, addWidget: (AbstractWidget) -> Unit
    ) {
        val allItems = inv?.eChestContents?.itemStacks ?: emptyList()
        val pageSize = 45
        val totalPages = ceil(allItems.size / pageSize.toDouble()).toInt().coerceAtLeast(1)
        currentEnderPage = currentEnderPage.coerceIn(1, totalPages)

        buildPagedSlotGrid(
            screen = screen,
            allItems = allItems,
            pageSize = pageSize,
            currentPage = currentEnderPage,
            totalPages = totalPages,
            gridY = gridY,
            gridHeight = gridHeight,
            addWidget = addWidget,
            colorProvider = { _, itemData ->
                if (Theme.rarityBg && itemData != null) Theme.rarityFromLore(itemData.lore) else Theme.slotBg
            }
        ) { currentEnderPage = it; screen.init() }
    }

    private fun buildPagedSlotGrid(
        screen: PVGui,
        allItems: List<HypixelData.ItemData?>,
        pageSize: Int,
        currentPage: Int,
        totalPages: Int,
        gridY: Int,
        gridHeight: Int,
        addWidget: (AbstractWidget) -> Unit,
        overrideX: Int? = null,
        overrideWidth: Int? = null,
        colorProvider: (index: Int, item: HypixelData.ItemData?) -> UInt,
        onPageChange: (Int) -> Unit,
    ) {
        val spacer = screen.spacer
        val startX = overrideX ?: screen.mainX
        val width = overrideWidth ?: screen.mainWidth

        var contentY = gridY
        var contentHeight = gridHeight

        if (totalPages > 1) {
            val btnH = 14
            val btnW = (width - spacer * (totalPages - 1)) / totalPages
            LayoutFactory.horizontal(spacer) {
                (1..totalPages).forEach { page ->
                    val isSelected = page == currentPage
                    val btn = Displays.center(btnW, btnH,
                        Displays.text("$page", color = { if (isSelected) TextColor.WHITE.toUInt() else 0xFFAAAAAA.toUInt() })
                    ).withRoundedBackground(if (isSelected) Theme.buttonSelect else Theme.buttonBg, Theme.btnRound)
                        .asButtonLeft { onPageChange(page) }
                        .also { it.withTexture(null) }
                    widget(btn)
                }
            }.apply { setPosition(startX, contentY) }.visitWidgets { addWidget(it) }
            contentY += btnH + spacer
            contentHeight -= btnH + spacer
        }

        val pageItems = allItems.drop((currentPage - 1) * pageSize).take(pageSize)
        buildSlotGrid(screen, pageItems, 9, contentY, contentHeight, addWidget, overrideX = startX, overrideWidth = width, colorProvider = colorProvider)
    }

    private fun buildSlotGrid(
        screen: PVGui,
        items: List<HypixelData.ItemData?>,
        cols: Int,
        gridY: Int,
        gridHeight: Int,
        addWidget: (AbstractWidget) -> Unit,
        overrideX: Int? = null,
        overrideWidth: Int? = null,
        colorProvider: (index: Int, item: HypixelData.ItemData?) -> UInt = { _, _ -> Theme.slotBg },
    ) {
        if (items.isEmpty()) return
        val spacer = screen.spacer
        val mainX = overrideX ?: screen.mainX
        val mainWidth = overrideWidth ?: screen.mainWidth

        val slotSize = (mainWidth - spacer * (cols + 1)) / cols
        val rows = (items.size + cols - 1) / cols
        val totalHeight = rows * (slotSize + spacer) - spacer
        val startY = gridY + (gridHeight - totalHeight) / 2

        items.forEachIndexed { index, itemData ->
            val col = index % cols
            val row = index / cols
            val x = mainX + spacer + col * (slotSize + spacer)
            val y = startY + row * (slotSize + spacer)
            val stack = itemData?.asItemStack ?: ItemStack.EMPTY
            val bg = colorProvider(index, itemData)

            LayoutFactory.vertical(0) {
                display(
                    Displays.item(stack, slotSize, slotSize, showTooltip = !stack.isEmpty)
                        .withRoundedBackground(bg, Theme.slotRound)
                )
            }.apply { setPosition(x, y) }.visitWidgets { addWidget(it) }
        }
    }
}