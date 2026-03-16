package com.odtheking.odinaddon.pvgui.nodes

import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.dsl.RenderQueue
import com.odtheking.odinaddon.pvgui.utils.Theme
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import kotlin.math.ceil
import kotlin.math.min

class ItemGridNode(
    private val columns: Int,
    private val gap: Float = 4f,
    private val items: () -> List<ItemStack?>,
    private val colors: ((ItemStack?, Int) -> Int)? = null,
    private val tooltips: ((ItemStack, Int) -> Boolean)? = null,
    private val customCount: ((ItemStack, Int) -> String?)? = null,
    private val onSlotClick: ((ItemStack, Int) -> Boolean)? = null,
    private val scissor: (() -> FloatArray?)? = null,
) {
    private var boundsX = 0f
    private var boundsY = 0f
    private var boundsW = 0f
    private var boundsH = 0f

    fun setBounds(x: Float, y: Float, w: Float, h: Float) {
        boundsX = x; boundsY = y; boundsW = w; boundsH = h
    }

    private fun slotSize(itemCount: Int): Float {
        val rows = ceil(itemCount.toDouble() / columns).toInt().coerceAtLeast(1)
        val fromWidth = (boundsW - gap * (columns - 1)) / columns
        val fromHeight = (boundsH - gap * (rows - 1).coerceAtLeast(0)) / rows
        return min(fromWidth, fromHeight)
    }

    private fun gridOrigin(itemCount: Int): Pair<Float, Float> {
        val slotSize = slotSize(itemCount)
        val rows = ceil(itemCount.toDouble() / columns).toInt()
        val gridWidth = columns * slotSize + (columns - 1) * gap
        val gridHeight = rows * slotSize + (rows - 1).coerceAtLeast(0) * gap
        val gridX = boundsX + (boundsW - gridWidth) / 2f
        val gridY = boundsY + (boundsH - gridHeight) / 2f
        return gridX to gridY
    }

    private fun slotX(index: Int, originX: Float, slotSize: Float) =
        originX + (index % columns) * (slotSize + gap)

    private fun slotY(index: Int, originY: Float, slotSize: Float) =
        originY + (index / columns) * (slotSize + gap)

    fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val itemList = items()
        val slotSize = slotSize(itemList.size)
        val (originX, originY) = gridOrigin(itemList.size)
        itemList.forEachIndexed { index, stack ->
            val sx = slotX(index, originX, slotSize)
            val sy = slotY(index, originY, slotSize)
            NVGRenderer.rect(sx, sy, slotSize, slotSize, colors?.invoke(stack, index) ?: Theme.slotBg, Theme.slotRadius)
            if (stack != null && !stack.isEmpty && PVState.isHovered(sx, sy, slotSize, slotSize))
                NVGRenderer.rect(sx, sy, slotSize, slotSize, Theme.btnHover, Theme.slotRadius)
        }
    }

    fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val itemList = items()
        val slotSize = slotSize(itemList.size)
        val (originX, originY) = gridOrigin(itemList.size)
        val scissorRect = scissor?.invoke()
        itemList.forEachIndexed { index, stack ->
            if (stack != null && !stack.isEmpty) {
                val sx = slotX(index, originX, slotSize)
                val sy = slotY(index, originY, slotSize)
                val showTooltip = tooltips?.invoke(stack, index) ?: true
                val count = customCount?.invoke(stack, index)
                RenderQueue.enqueueItem(stack, sx, sy, slotSize, showTooltip, count, scissorRect)
            }
        }
    }

    fun click(mouseX: Double, mouseY: Double): Boolean {
        if (onSlotClick == null) return false
        val itemList = items()
        val slotSize = slotSize(itemList.size)
        val (originX, originY) = gridOrigin(itemList.size)
        itemList.forEachIndexed { index, stack ->
            if (stack != null && !stack.isEmpty) {
                val sx = slotX(index, originX, slotSize)
                val sy = slotY(index, originY, slotSize)
                if (mouseX >= sx && mouseX < sx + slotSize && mouseY >= sy && mouseY < sy + slotSize)
                    return onSlotClick.invoke(stack, index)
            }
        }
        return false
    }
}