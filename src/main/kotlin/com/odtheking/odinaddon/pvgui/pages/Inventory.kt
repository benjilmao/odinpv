package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.capitalizeWords
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.components.Box
import com.odtheking.odinaddon.pvgui.components.GridItems
import com.odtheking.odinaddon.pvgui.components.TextBox
import com.odtheking.odinaddon.pvgui.components.buttons
import com.odtheking.odinaddon.pvgui.components.itemGrid
import com.odtheking.odinaddon.pvgui.core.PVPage
import com.odtheking.odinaddon.pvgui.core.PageData
import com.odtheking.odinaddon.pvgui.core.Theme
import com.odtheking.odinaddon.pvgui.utils.*
import com.odtheking.odinaddon.pvgui.utils.apiutils.HypixelData
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Complete Inventory Page - With Rarity Toggle
 * Basic, Wardrobe, Talismans, Backpacks, EnderChest pages
 */
object Inventory : PVPage("Inventory") {

    // Layout constants
    private val separatorLineY by resettableLazy {
        spacer + ((PageData.mainHeight - spacer * 5) * 0.9) / 6
    }
    private val startY by resettableLazy { separatorLineY + spacer + 1 }
    private val buttonHeight by resettableLazy { (mainWidth - (spacer * 16)) / 18f }
    private val centerY by resettableLazy {
        (startY + buttonHeight + spacer) + (mainHeight - (startY + buttonHeight)) / 2
    }

    // Shared data
    private val invArmor by resettableLazy { profile?.inventory?.invArmor?.itemStacks ?: emptyList() }

    // Navigation
    private var currentPage = "Basic"

    private val navButtons by resettableLazy {
        buttons(
            box = Box(mainX, spacer, mainWidth, separatorLineY.toInt() - spacer),
            padding = spacer,
            default = "Basic",
            options = listOf("Basic", "Wardrobe", "Talismans", "Backpacks", "Ender Chest"),
            textScale = 2f * PageData.scale,
            color = Theme.buttonBg.rgba,
            selectedColor = Theme.buttonSelected.rgba,
            radius = Theme.roundness
        ) {
            onSelect { pageName -> currentPage = pageName }
        }
    }

    override fun draw(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // Draw navigation
        navButtons.draw(mouseX, mouseY)

        // Draw current page
        when (currentPage) {
            "Basic" -> BasicPage.draw(guiGraphics, mouseX, mouseY)
            "Wardrobe" -> WardrobePage.draw(guiGraphics, mouseX, mouseY)
            "Talismans" -> TalismansPage.draw(guiGraphics, mouseX, mouseY)
            "Backpacks" -> BackpacksPage.draw(guiGraphics, mouseX, mouseY)
            "Ender Chest" -> EnderChestPage.draw(guiGraphics, mouseX, mouseY)
        }
    }

    override fun click(mouseX: Int, mouseY: Int, mouseButton: Int) {
        navButtons.click(mouseX, mouseY, mouseButton)

        when (currentPage) {
            "Wardrobe" -> WardrobePage.click(mouseX, mouseY, mouseButton)
            "Talismans" -> TalismansPage.click(mouseX, mouseY, mouseButton)
            "Backpacks" -> BackpacksPage.click(mouseX, mouseY, mouseButton)
            "Ender Chest" -> EnderChestPage.click(mouseX, mouseY, mouseButton)
        }
    }

    // ========== BASIC PAGE ==========

    object BasicPage {

        private val itemGrid by resettableLazy {
            val inv = profile?.inventory ?: return@resettableLazy null

            val armor = inv.invArmor.itemStacks.reversed()
            val equipment = inv.equipment.itemStacks
            val inventory = InventoryUtils.fixFirstNine(inv.invContents.itemStacks)
            val allItems = armor + listOf(null) + equipment + inventory

            // HateCheaters uses different centerY calculation for Basic!
            val basicCenterY = (startY + (mainHeight - separatorLineY) / 2).toInt()

            itemGrid(
                listOf(GridItems(allItems, mainX, basicCenterY, mainWidth, 9)),
                Theme.roundness,
                spacer.toFloat()
            ) {
                colorHandler { index, item ->
                    when {
                        index == 4 -> 0x00000000  // Transparent
                        ProfileViewerModule.rarityBackgrounds && item != null -> item.getRarityColor()
                        else -> Theme.secondaryBg.rgba
                    }
                }
            }
        }

        fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
            itemGrid?.draw(context, mouseX, mouseY)
        }
    }

    // ========== WARDROBE PAGE ==========

    object WardrobePage {

        private val wardrobe by resettableLazy {
            profile?.inventory?.wardrobeContents?.itemStacks ?: emptyList()
        }

        private var selectedPage = 1

        // Calculate which wardrobe slot is equipped on current page
        private val equippedOnPage: Int get() {
            val equippedSlot = profile?.inventory?.wardrobeEquipped?.let { it - 1 } ?: return -1
            val pageStart = (selectedPage - 1) * 36
            val pageEnd = pageStart + 36

            return if (equippedSlot in pageStart until pageEnd) {
                equippedSlot - pageStart
            } else {
                -1
            }
        }

        // Overlay currently equipped armor on the wardrobe page
        private val wardrobeWithArmor: List<HypixelData.ItemData?> get() {
            val pageItems = InventoryUtils.getSubset(wardrobe, selectedPage - 1, 36)

            if (equippedOnPage == -1 || invArmor.isEmpty()) {
                return pageItems
            }

            // Overlay armor at positions (HateCheaters logic)
            val result = pageItems.toMutableList()
            invArmor.reversed().forEachIndexed { armorIndex, armor ->
                val overlayIndex = equippedOnPage + (9 * armorIndex)
                if (overlayIndex in result.indices) {
                    result[overlayIndex] = armor
                }
            }

            return result
        }

        private val pageButtons by resettableLazy {
            val totalPages = ceil(wardrobe.size / 36.0).toInt()

            buttons(
                box = Box(mainX, startY.toInt(), mainWidth, buttonHeight.toInt()),
                padding = spacer,
                default = 1,
                options = (1..totalPages).toList(),
                textScale = 2f * PageData.scale,
                color = Theme.buttonBg.rgba,
                selectedColor = Theme.buttonSelected.rgba,
                radius = Theme.roundness
            ) {
                onSelect { page ->
                    selectedPage = page
                    itemGrid.updateItems(wardrobeWithArmor)
                }
            }
        }

        private val itemGrid by resettableLazy {
            itemGrid(
                listOf(GridItems(wardrobeWithArmor, mainX, centerY.toInt(), mainWidth, 9)),
                Theme.roundness,
                spacer.toFloat()
            ) {
                colorHandler { _, item ->
                    // Highlight equipped armor in blue
                    if (item != null && invArmor.contains(item)) {
                        0xFF5555FF.toInt()  // Blue
                    } else if (ProfileViewerModule.rarityBackgrounds && item != null) {
                        item.getRarityColor()
                    } else {
                        Theme.secondaryBg.rgba
                    }
                }
            }
        }

        fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
            pageButtons.draw(mouseX, mouseY)
            itemGrid.draw(context, mouseX, mouseY)
        }

        fun click(mouseX: Int, mouseY: Int, mouseButton: Int) {
            pageButtons.click(mouseX, mouseY, mouseButton)
        }
    }

    // ========== TALISMANS PAGE ==========

    object TalismansPage {

        private const val MAX_MAGICAL_POWER = 1900

        private val talismans by resettableLazy {
            profile?.inventory?.bagContents?.get("talisman_bag")?.itemStacks
                ?.filterNotNull()
                ?.sortedByDescending { it.magicalPower }
                ?: emptyList()
        }

        private val magicalPower by resettableLazy { profile?.magicalPower ?: 0 }

        private val textList by resettableLazy {
            val power = profile?.accessoryBagStorage?.selectedPower
            val contacts = profile?.crimsonIsle?.abiphone?.activeContacts?.size ?: 0
            val hasPrism = profile?.rift?.access?.consumedPrism == true
            val tunings = profile?.accessoryBagStorage?.tuning?.currentTunings?.map {
                "${it.key.replace("_", " ").capitalizeWords()}§7: ${it.value}"
            } ?: emptyList()

            listOf(
                "§aSelected Power: §6${power?.capitalizeWords() ?: "§cNone!"}",
                "§5Abiphone: ${floor(contacts / 2.0).toInt()}",
                "§dRift Prism: ${if (hasPrism) "§aObtained" else "§cMissing"}"
            ) + tunings
        }

        // Layout
        private val textBoxWidth by resettableLazy { (mainWidth * 0.38).toInt() }
        private val separatorX by resettableLazy { mainX + textBoxWidth }
        private val gridWidth by resettableLazy { mainWidth - textBoxWidth - spacer }

        private val totalPages by resettableLazy {
            ceil(talismans.size / 63.0).toInt()
        }

        private var selectedPage = 1

        // TextBox for left panel
        private val infoTextBox by resettableLazy {
            TextBox(
                box = Box(
                    mainX + spacer,
                    (startY + spacer).toInt(),
                    textBoxWidth - 2 * spacer,
                    (mainHeight - startY - spacer).toInt()
                ),
                title = "Magical Power: $magicalPower",
                titleScale = 2.2f,
                text = textList,
                textScale = 2.2f,
                spacer = spacer.toFloat(),
                defaultColor = Theme.fontColor.rgba
            )
        }

        private val pageButtons by resettableLazy {
            buttons(
                box = Box(separatorX + spacer, startY.toInt(), gridWidth, buttonHeight.toInt()),
                padding = spacer,
                default = 1,
                options = (1..totalPages).toList(),
                textScale = 2f * PageData.scale,
                color = Theme.buttonBg.rgba,
                selectedColor = Theme.buttonSelected.rgba,
                radius = Theme.roundness
            ) {
                onSelect { page ->
                    selectedPage = page
                    itemGrid.updateItems(InventoryUtils.getSubset(talismans, selectedPage - 1, 63))
                }
            }
        }

        private val itemGrid by resettableLazy {
            itemGrid(
                listOf(
                    GridItems(
                        InventoryUtils.getSubset(talismans, 0, 63),
                        separatorX + spacer + 1,
                        centerY.toInt(),
                        gridWidth,
                        9
                    )
                ),
                Theme.roundness,
                spacer.toFloat()
            ) {
                colorHandler { _, item ->
                    if (ProfileViewerModule.rarityBackgrounds && item != null) {
                        item.getRarityColor()
                    } else {
                        Theme.secondaryBg.rgba
                    }
                }

                tooltipHandler { item ->
                    val mpColor = when {
                        item.magicalPower >= 22 -> "§6"
                        item.magicalPower >= 16 -> "§c"
                        else -> "§7"
                    }
                    listOf("${item.name} §7($mpColor${item.magicalPower}§7)") + item.lore
                }
            }
        }

        fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
            // Draw background for text box
            NVGRenderer.rect(
                mainX.toFloat(),
                startY.toFloat(),
                textBoxWidth.toFloat(),
                (mainHeight - startY + spacer).toFloat(),
                Theme.secondaryBg.rgba,
                Theme.roundness
            )

            infoTextBox.draw() // draws title and list

            pageButtons.draw(mouseX, mouseY)
            itemGrid.draw(context, mouseX, mouseY)
        }

        fun click(mouseX: Int, mouseY: Int, mouseButton: Int) {
            pageButtons.click(mouseX, mouseY, mouseButton)
        }
    }

    // ========== BACKPACKS PAGE ==========

    object BackpacksPage {

        private var selectedBackpack = 1

        // Compute items on the fly (no lazy)
        private fun currentItems(): List<HypixelData.ItemData?> =
            profile?.inventory?.backpackContents?.get("${selectedBackpack - 1}")?.itemStacks ?: emptyList()

        private val gridWidth by resettableLazy { (mainWidth * 0.8).toInt() }
        private val gridX by resettableLazy { mainX + (mainWidth - gridWidth) / 2 }

        private val pageButtons by resettableLazy {
            val backpackKeys = profile?.inventory?.backpackContents?.keys
                ?.mapNotNull { it.toIntOrNull()?.plus(1) }
                ?.sorted() ?: listOf(1)

            buttons(
                box = Box(mainX, startY.toInt(), mainWidth, buttonHeight.toInt()),
                padding = spacer,
                default = 1,
                options = backpackKeys,
                textScale = 2f * PageData.scale,
                color = Theme.buttonBg.rgba,
                selectedColor = Theme.buttonSelected.rgba,
                radius = Theme.roundness
            ) {
                onSelect { page ->
                    selectedBackpack = page
                    itemGrid.updateItems(currentItems())
                }
            }
        }

        private val itemGrid by resettableLazy {
            itemGrid(
                listOf(GridItems(currentItems(), gridX, centerY.toInt(), gridWidth, 9)),
                Theme.roundness,
                spacer.toFloat()
            ) {
                colorHandler { _, item ->
                    if (ProfileViewerModule.rarityBackgrounds && item != null) {
                        item.getRarityColor()
                    } else {
                        Theme.secondaryBg.rgba
                    }
                }
            }
        }

        fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
            pageButtons.draw(mouseX, mouseY)
            itemGrid.updateItems(currentItems()) // refresh items each frame
            itemGrid.draw(context, mouseX, mouseY)
        }

        fun click(mouseX: Int, mouseY: Int, mouseButton: Int) {
            pageButtons.click(mouseX, mouseY, mouseButton)
        }
    }

    // ========== ENDER CHEST PAGE ==========

    object EnderChestPage {

        private val items by resettableLazy {
            profile?.inventory?.eChestContents?.itemStacks ?: emptyList()
        }

        private val totalPages by resettableLazy { ceil(items.size / 45.0).toInt() }

        private var selectedPage = 1

        // 80% width, centered
        private val gridWidth by resettableLazy { (mainWidth * 0.8).toInt() }
        private val gridX by resettableLazy { mainX + (mainWidth - gridWidth) / 2 }

        private val pageButtons by resettableLazy {
            buttons(
                box = Box(mainX, startY.toInt(), mainWidth, buttonHeight.toInt()),
                padding = spacer,
                default = 1,
                options = (1..totalPages).toList(),
                textScale = 2f * PageData.scale,
                color = Theme.buttonBg.rgba,
                selectedColor = Theme.buttonSelected.rgba,
                radius = Theme.roundness
            ) {
                onSelect { page ->
                    selectedPage = page
                    itemGrid.updateItems(InventoryUtils.getSubset(items, selectedPage - 1, 45))
                }
            }
        }

        private val itemGrid by resettableLazy {
            itemGrid(
                listOf(
                    GridItems(
                        InventoryUtils.getSubset(items, 0, 45),
                        gridX,
                        centerY.toInt(),
                        gridWidth,
                        9
                    )
                ),
                Theme.roundness,
                spacer.toFloat()
            ) {
                colorHandler { _, item ->
                    if (ProfileViewerModule.rarityBackgrounds && item != null) {
                        item.getRarityColor()
                    } else {
                        Theme.secondaryBg.rgba
                    }
                }
            }
        }

        fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
            pageButtons.draw(mouseX, mouseY)
            itemGrid.draw(context, mouseX, mouseY)
        }

        fun click(mouseX: Int, mouseY: Int, mouseButton: Int) {
            pageButtons.click(mouseX, mouseY, mouseButton)
        }
    }

    fun setPlayer(player: HypixelData.PlayerInfo) {}
}