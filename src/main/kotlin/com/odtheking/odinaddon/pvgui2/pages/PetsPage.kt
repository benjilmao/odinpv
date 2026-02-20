package com.odtheking.odinaddon.pvgui2.pages

import com.odtheking.odinaddon.pvgui2.utils.Utils
import com.odtheking.odinaddon.pvgui2.utils.profileOrSelected
import com.odtheking.odinaddon.pvgui2.Theme
import com.odtheking.odinaddon.pvgui2.TestGui
import com.odtheking.odinaddon.pvgui2.utils.LevelUtils
import com.odtheking.odinaddon.pvgui2.withRoundedBackground
import me.owdding.lib.builder.LayoutFactory
import me.owdding.lib.displays.Display
import me.owdding.lib.displays.Displays
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import tech.thatgravyboat.skyblockapi.api.remote.PetQuery
import tech.thatgravyboat.skyblockapi.api.remote.RepoPetsAPI

object PetsPage {

    var scrollOffset = 0

    fun scroll(delta: Double) {
        scrollOffset = (scrollOffset - delta.toInt()).coerceAtLeast(0)
    }

    fun reset() {
        scrollOffset = 0
    }

    fun build(screen: TestGui, addWidget: (AbstractWidget) -> Unit) {
        val spacer = screen.spacer
        val mainX = screen.mainX
        val mainY = screen.mainY
        val mainWidth = screen.mainWidth
        val mainHeight = screen.mainHeight

        val data = screen.playerData
        if (data == null) {
            LayoutFactory.vertical(0) {
                display(Displays.center(mainWidth, mainHeight,
                    Displays.text(screen.loadText, shadow = true)
                ))
            }.apply { setPosition(mainX, mainY) }.visitWidgets { addWidget(it) }
            return
        }

        val profile = data.profileOrSelected(screen.profileName)?.members?.get(data.uuid) ?: return
        val rarityOrder = listOf("MYTHIC", "LEGENDARY", "EPIC", "RARE", "UNCOMMON", "COMMON")
        val level200Pets = setOf("GOLDEN_DRAGON", "JADE_DRAGON", "ROSE_DRAGON")
        val pets = profile.pets.pets.sortedWith(compareBy(
            { rarityOrder.indexOf(it.tier.uppercase()).takeIf { i -> i >= 0 } ?: rarityOrder.size },
            { if (it.type.uppercase() in level200Pets) 0 else 1 },
            { it.type.uppercase() }
        ))
        val activePet = pets.find { it.active }
        val activePetText = if (activePet == null) "§7No active pet"
        else "§7Active: ${Utils.getActivePetDisplay(profile.pets)}"

        val infoBoxHeight = 20
        val gridStartY = mainY + infoBoxHeight + spacer * 2

        LayoutFactory.vertical(0) {
            display(Displays.center(mainWidth, infoBoxHeight,
                Displays.text(activePetText, shadow = true)
            ))
        }.apply { setPosition(mainX, mainY) }.visitWidgets { addWidget(it) }

        LayoutFactory.vertical(0) {
            display(object : Display {
                override fun getWidth() = mainWidth
                override fun getHeight() = 1
                override fun render(graphics: GuiGraphics) {
                    graphics.fill(0, 0, mainWidth, 1, Theme.separator)
                }
            })
        }.apply { setPosition(mainX, mainY + infoBoxHeight + spacer) }.visitWidgets { addWidget(it) }

        val cols = 12
        val slotSize = (mainWidth - spacer * (cols + 1)) / cols
        val gridHeight = mainHeight - infoBoxHeight - spacer * 3
        val visibleRows = gridHeight / (slotSize + spacer)
        val totalRows = (pets.size + cols - 1) / cols

        scrollOffset = scrollOffset.coerceIn(0, maxOf(0, totalRows - visibleRows))

        val firstPet = scrollOffset * cols
        val lastPet = minOf(pets.size, firstPet + visibleRows * cols)

        pets.subList(firstPet, lastPet).forEachIndexed { index, pet ->
            val actualIndex = firstPet + index
            val col = actualIndex % cols
            val row = actualIndex / cols - scrollOffset
            val x = mainX + spacer + col * (slotSize + spacer)
            val y = gridStartY + spacer + row * (slotSize + spacer)

            if (y + slotSize > mainY + mainHeight) return@forEachIndexed

            val rarity = SkyBlockRarity.fromNameOrNull(pet.tier) ?: SkyBlockRarity.COMMON
            val itemStack = RepoPetsAPI.getPetAsItem(PetQuery(
                id = pet.type,
                rarity = rarity,
                level = LevelUtils.getPetLevel(pet.exp, rarity, pet.type).toInt(),
                skin = pet.skin,
                heldItem = pet.heldItem,
            ))

            val bgColor = if (Theme.rarityBg) Theme.raritySlotColor(pet.tier) else Theme.slotBg

            LayoutFactory.vertical(0) {
                display(
                    Displays.item(itemStack, slotSize, slotSize, showTooltip = true)
                        .withRoundedBackground(if (pet.active) 0xFF00AA00U else bgColor, Theme.slotRound)
                )
            }.apply { setPosition(x, y) }.visitWidgets { addWidget(it) }
        }

        if (totalRows > visibleRows) {
            val thumbHeight = (visibleRows.toFloat() / totalRows * gridHeight).toInt().coerceAtLeast(20)
            val thumbY = (scrollOffset.toFloat() / (totalRows - visibleRows) * (gridHeight - thumbHeight)).toInt()

            LayoutFactory.vertical(0) {
                display(object : Display {
                    override fun getWidth() = 3
                    override fun getHeight() = gridHeight
                    override fun render(graphics: GuiGraphics) {
                        graphics.fill(0, 0, 3, gridHeight, 0x22FFFFFF)
                        graphics.fill(0, thumbY, 3, thumbY + thumbHeight, 0x88FFFFFFU.toInt())
                    }
                })
            }.apply { setPosition(mainX + mainWidth - spacer, gridStartY + spacer) }.visitWidgets { addWidget(it) }
        }
    }
}