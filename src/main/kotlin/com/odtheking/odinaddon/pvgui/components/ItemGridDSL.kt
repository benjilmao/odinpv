package com.odtheking.odinaddon.pvgui.components

import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.core.PageData
import com.odtheking.odinaddon.pvgui.core.Theme
import com.odtheking.odinaddon.pvgui.utils.apiutils.HypixelData
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.helpers.McClient
import kotlin.math.ceil

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
    private var colorHandler: (index: Int, HypixelData.ItemData?) -> Int = { _, _ -> Theme.secondaryBg.rgba }

    fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val pvScale = PageData.scale
        val offsetX = PageData.offsetX
        val offsetY = PageData.offsetY
        val guiScale = Minecraft.getInstance().window.guiScale.toFloat()

        items.forEach { gridItems ->
            val slotSize = (gridItems.width - (gridItems.columns - 1) * padding) / gridItems.columns.coerceAtLeast(1)
            val totalRows = ceil(gridItems.items.size.toDouble() / gridItems.columns).toInt()
            val totalHeight = totalRows * (slotSize + padding) - padding
            val gridStartY = gridItems.centerY - (totalHeight / 2)

            gridItems.items.forEachIndexed { index, itemData ->
                val col = index % gridItems.columns
                val row = index / gridItems.columns
                val x = gridItems.x + col * (slotSize + padding)
                val y = gridStartY + row * (slotSize + padding)

                NVGRenderer.rect(x, y, slotSize, slotSize, colorHandler(index, itemData), radius)

                val stack = itemData?.asItemStack
                if (stack != null && !stack.isEmpty) {
                    val guiX = ((offsetX + x * pvScale) / guiScale).toInt()
                    val guiY = ((offsetY + y * pvScale) / guiScale).toInt()
                    val guiSize = (slotSize * pvScale / guiScale).toInt().coerceAtLeast(1)

                    pendingItems.add(PendingItem(stack, guiX, guiY, guiSize))

                    val guiMouseX = McClient.mouse.first.toInt()
                    val guiMouseY = McClient.mouse.second.toInt()
                    if (guiMouseX in guiX..(guiX + guiSize) && guiMouseY in guiY..(guiY + guiSize)) {
                        pendingTooltip = Triple(stack, guiMouseX, guiMouseY)
                    }
                }
            }
        }
    }

    fun colorHandler(init: (index: Int, HypixelData.ItemData?) -> Int) {
        colorHandler = init
    }

    fun updateItems(newItems: List<HypixelData.ItemData?>, index: Int = 0) {
        items[index].items = newItems
    }

    companion object {
        var pendingTooltip: Triple<ItemStack, Int, Int>? = null
        val pendingItems = mutableListOf<PendingItem>()
    }
}

data class PendingItem(val stack: ItemStack, val guiX: Int, val guiY: Int, val guiSize: Int)

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