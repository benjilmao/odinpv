package com.odtheking.odinaddon.pvgui.components

import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.core.Theme
import com.odtheking.odinaddon.pvgui.utils.apiutils.HypixelData
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.ceil

/**
 * Simple ItemGridDSL - Just shows 2-char item IDs with rarity colors
 * This is the WORKING version before we added item rendering
 */

fun itemGrid(
    items: List<GridItems>,
    radius: Float = Theme.roundness,
    padding: Float = 10f,
    itemGrid: ItemGridDSL.() -> Unit
) = ItemGridDSL(items, radius, padding).apply(itemGrid)

class ItemGridDSL(
    private val items: List<GridItems>,
    private val radius: Float,
    private val padding: Float,
) {
    private var tooltipHandler: (HypixelData.ItemData) -> List<String> = { listOf(it.name) + it.lore }
    private var colorHandler: (index: Int, HypixelData.ItemData?) -> Int = { _, _ -> Theme.secondaryBg.rgba }

    private var hoveredItem: HoverData? = null

    fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        hoveredItem = null

        items.forEach { gridItems ->
            // Auto-calculate slot size (HateCheaters formula)
            val slotSize = (gridItems.width - (gridItems.columns - 1) * padding) / gridItems.columns.coerceAtLeast(1)

            gridItems.items.forEachIndexed { index, itemData ->
                val col = index % gridItems.columns
                val row = index / gridItems.columns
                val x = gridItems.x + col * (slotSize + padding)

                // Auto-center vertically (HateCheaters formula)
                val totalRows = ceil(gridItems.items.size.toDouble() / gridItems.columns).toInt()
                val totalHeight = totalRows * (slotSize + padding) - padding
                val gridStartY = gridItems.centerY - (totalHeight / 2)
                val y = gridStartY + row * (slotSize + padding)

                // Draw slot background with rarity color
                val bgColor = colorHandler(index, itemData)
                NVGRenderer.rect(x, y, slotSize, slotSize, bgColor, radius)

                // Draw item ID (2 chars) - NO ITEM RENDERING YET
                if (itemData != null) {
                    drawItemSimple(itemData, x, y, slotSize)
                }

                // Check hover for tooltip
                if (mouseX in x.toInt()..(x + slotSize).toInt() &&
                    mouseY in y.toInt()..(y + slotSize).toInt()) {
                    hoveredItem = itemData?.let { HoverData(it, mouseX, mouseY) }
                }
            }
        }

        // Draw tooltip for hovered item
        hoveredItem?.let { (item, hoverX, hoverY) ->
            drawTooltip(context, item, hoverX, hoverY)
        }
    }

    // Simple rendering - just show 2-char item ID
    private fun drawItemSimple(item: HypixelData.ItemData, x: Float, y: Float, size: Float) {
        val displayText = item.id.take(2)
        Text.draw(
            text = displayText,
            x = x + size / 2,
            y = y + size / 2,
            scale = size / 16f,
            defaultColor = Colors.WHITE,
            centering = Text.Centering.CENTER,
            alignment = Text.Alignment.MIDDLE
        )
    }

    // Draw tooltip with NVG
    private fun drawTooltip(context: GuiGraphics, item: HypixelData.ItemData, x: Int, y: Int) {
        val lines = tooltipHandler(item)
        if (lines.isEmpty()) return

        val maxWidth = lines.maxOf { line ->
            NVGRenderer.textWidth(line, 16f, NVGRenderer.defaultFont)
        }

        val tooltipWidth = maxWidth + 16f
        val tooltipHeight = lines.size * 20f + 8f
        val tooltipX = x.toFloat() + 12f
        val tooltipY = y.toFloat() - 12f

        NVGRenderer.rect(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 0xCC000000.toInt(), 4f)

        lines.forEachIndexed { index, line ->
            Text.draw(
                text = line,
                x = tooltipX + 8f,
                y = tooltipY + 8f + index * 20f,
                scale = 2f,
                defaultColor = Colors.WHITE,
                centering = Text.Centering.LEFT,
                alignment = Text.Alignment.ABOVE
            )
        }
    }

    fun tooltipHandler(init: (HypixelData.ItemData) -> List<String>) {
        tooltipHandler = init
    }

    fun colorHandler(init: (index: Int, HypixelData.ItemData?) -> Int) {
        colorHandler = init
    }

    fun updateItems(newItems: List<HypixelData.ItemData?>, index: Int = 0) {
        items[index].items = newItems
    }
}

// Data classes
data class GridItems(
    var items: List<HypixelData.ItemData?>,
    val x: Float,
    val centerY: Float,
    val width: Float,
    val columns: Int
) {
    constructor(items: List<HypixelData.ItemData?>, x: Int, centerY: Int, width: Int, columns: Int) :
            this(items, x.toFloat(), centerY.toFloat(), width.toFloat(), columns)
}

private data class HoverData(val item: HypixelData.ItemData, val mouseX: Int, val mouseY: Int)