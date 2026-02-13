package com.odtheking.odinaddon.pvgui.pages

import com.odtheking.odin.utils.Colors
import com.odtheking.odinaddon.pvgui.utils.apiutils.HypixelData
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.core.PVPage
import com.odtheking.odinaddon.pvgui.core.PageData
import com.odtheking.odinaddon.pvgui.core.Theme
import com.odtheking.odinaddon.pvgui.utils.*
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.ceil

/**
 * Pets Page - EXACT HateCheaters Layout
 */
object Pets : PVPage("Pets") {

    private var currentPage = 1
    private val pageSize = 20

    private var pageButtons: ButtonDSL<Int>? = null
    private var totalPages = 1

    override fun draw(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val member = profile ?: return
        val pets = member.pets
        totalPages = maxOf(1, ceil(pets.pets.size / pageSize.toDouble()).toInt())
        if (currentPage > totalPages) currentPage = totalPages

        drawActivePet(pets)
        if (totalPages > 1) {
            pageButtons = drawPagination(mouseX, mouseY)
        }
        drawPetList(pets)
    }

    private fun drawActivePet(pets: HypixelData.PetsData) {
        val activePetHeight = mainHeight * 0.1f

        // Active pet background
        NVGRenderer.rect(
            mainX.toFloat(),
            spacer.toFloat(),
            mainWidth.toFloat(),
            activePetHeight,
            Theme.secondaryBg.rgba,
            Theme.roundness
        )

        // Active pet text with fillText
        val activePet = pets.pets.find { it.active }
        val displayText = if (activePet == null) {
            "§7Active Pet§8: §7None"
        } else {
            val petName = formatPetName(activePet.type, activePet.tier)
            val held = activePet.heldItem?.let { " §7(§f${Utils.formatHeldItem(it)}§7)" } ?: ""
            "§7Active Pet§8: $petName §7$held"
        }

        Text.fillText(
            text = displayText,
            x = mainCenterX,
            y = spacer + activePetHeight / 2,
            maxWidth = mainWidth.toFloat() - spacer * 4,
            baseScale = 3f,
            defaultColor = Colors.WHITE
        )
    }

    private fun drawPagination(mouseX: Int, mouseY: Int): ButtonDSL<Int> {
        val activePetHeight = mainHeight * 0.1f
        val buttonHeight = 30f
        val buttonY = spacer * 2 + activePetHeight

        val buttonBox = Box(mainX, buttonY.toInt(), mainWidth, buttonHeight.toInt())

        return buttons(
            box = buttonBox,
            padding = (8 * PageData.scale).toInt(),
            default = currentPage,
            options = (1..totalPages).toList(),
            textScale = 2f * PageData.scale,
            color = Theme.buttonBg.rgba,
            selectedColor = Theme.buttonSelected.rgba,
            radius = Theme.buttonRoundness
        ) {
            onSelect { page ->
                currentPage = page
            }
        }.apply { draw(mouseX, mouseY) }
    }

    private fun drawPetList(pets: HypixelData.PetsData) {
        val activePetHeight = mainHeight * 0.1f
        val buttonHeight = if (totalPages > 1) 30f else 0f
        val listStartY = spacer * 2 + activePetHeight + (if (totalPages > 1) buttonHeight + spacer else 0f)
        val listHeight = mainHeight - listStartY + spacer

        // Pets grid background
        NVGRenderer.rect(
            mainX.toFloat(),
            listStartY,
            mainWidth.toFloat(),
            listHeight,
            Theme.secondaryBg.rgba,
            Theme.roundness
        )

        val allPets = pets.pets.sortedByDescending { it.exp }
        val currentPets = Utils.getSubset(allPets, currentPage - 1, pageSize)

        if (currentPets.isEmpty()) return

        // 2 columns like HateCheaters
        val leftColumnX = mainX + mainWidth * 0.28f
        val rightColumnX = mainX + mainWidth * 0.72f
        val rows = ceil(currentPets.size / 2.0).toInt()
        val rowHeight = (listHeight - spacer * 2) / rows
        val startY = listStartY + spacer + rowHeight / 2

        currentPets.forEachIndexed { index, pet ->
            val columnX = if (index % 2 == 0) leftColumnX else rightColumnX
            val row = index / 2
            val y = startY + row * rowHeight

            val petName = formatPetName(pet.type, pet.tier)
            val held = pet.heldItem?.let { " §7(§f${Utils.formatHeldItem(it)}§7)" } ?: ""
            val displayText = "$petName$held"

            Text.fillText(
                text = displayText,
                x = columnX,
                y = y,
                maxWidth = (mainWidth / 2f) - spacer * 4,
                baseScale = 2f,
                defaultColor = Colors.WHITE
            )
        }
    }

    fun setPlayer(player: HypixelData.PlayerInfo) {
        currentPage = 1
        totalPages = 1
    }

    private fun formatPetName(type: String, tier: String): String {
        val color = Theme.petTierColors[tier.uppercase()] ?: "§7"
        val name = type.lowercase()
            .replaceFirstChar { it.uppercase() }
            .replace("_", " ")
        return "$color$name"
    }

    override fun click(mouseX: Int, mouseY: Int, mouseButton: Int) {
        pageButtons?.click(mouseX, mouseY, mouseButton)
    }
}