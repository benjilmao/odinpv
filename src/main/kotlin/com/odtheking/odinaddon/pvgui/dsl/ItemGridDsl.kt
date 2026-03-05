package com.odtheking.odinaddon.pvgui.dsl

import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.utils.Theme
import net.minecraft.world.item.ItemStack

private const val SLOT_RADIUS = 4f

fun itemGrid(
    x: Float, y: Float,
    cols: Int,
    slotSize: Float = 32f,
    gap: Float = 4f,
    items: () -> List<ItemStack?>,
    colorHandler: ((ItemStack?, Int) -> Int)? = null,
    tooltipHandler: ((ItemStack, Int) -> Boolean)? = null,
    customCount: ((ItemStack, Int) -> String?)? = null,
    onSlotClick: ((ItemStack, Int) -> Boolean)? = null,
    scissorRect: (() -> FloatArray?)? = null,
): ItemGridDsl = ItemGridDsl(x, y, cols, slotSize, gap, items, colorHandler, tooltipHandler, customCount, onSlotClick, scissorRect)

class ItemGridDsl(
    val x: Float,
    val y: Float,
    val cols: Int,
    val slotSize: Float,
    val gap: Float,
    private val itemsProvider: () -> List<ItemStack?>,
    private val colorHandler: ((ItemStack?, Int) -> Int)?,
    private val tooltipHandler: ((ItemStack, Int) -> Boolean)?,
    private val customCountHandler: ((ItemStack, Int) -> String?)?,
    private val onSlotClick: ((ItemStack, Int) -> Boolean)?,
    private val scissorRectProvider: (() -> FloatArray?)?,
) {
    private fun slotX(idx: Int) = x + (idx % cols) * (slotSize + gap)
    private fun slotY(idx: Int) = y + (idx / cols) * (slotSize + gap)

    val totalW: Float get() = cols * slotSize + (cols - 1) * gap

    fun totalH(itemCount: Int): Float {
        val rows = (itemCount + cols - 1) / cols
        return rows * slotSize + (rows - 1).coerceAtLeast(0) * gap
    }

    fun draw(forcedScissor: FloatArray? = null) {
        val items   = itemsProvider()
        val scissor = forcedScissor ?: scissorRectProvider?.invoke()

        items.forEachIndexed { idx, stack ->
            val sx = slotX(idx); val sy = slotY(idx)
            NVGRenderer.rect(sx, sy, slotSize, slotSize, colorHandler?.invoke(stack, idx) ?: Theme.slotBg, SLOT_RADIUS)

            if (stack != null && !stack.isEmpty) {
                if (PVState.isHovered(sx, sy, slotSize, slotSize))
                    NVGRenderer.rect(sx, sy, slotSize, slotSize, Theme.btnHover, SLOT_RADIUS)
                val showTip  = tooltipHandler?.invoke(stack, idx) ?: true
                val countStr = customCountHandler?.invoke(stack, idx)
                ItemQueue.queue(stack, sx, sy, slotSize, showTip, countStr, scissor)
            }
        }
    }

    fun click(mouseX: Double, mouseY: Double): Boolean {
        if (onSlotClick == null) return false
        itemsProvider().forEachIndexed { idx, stack ->
            if (stack != null && !stack.isEmpty) {
                val sx = slotX(idx); val sy = slotY(idx)
                if (mouseX >= sx && mouseX < sx + slotSize && mouseY >= sy && mouseY < sy + slotSize)
                    return onSlotClick.invoke(stack, idx)
            }
        }
        return false
    }

    fun hoveredIndex(mouseX: Double, mouseY: Double): Int {
        itemsProvider().indices.forEach { idx ->
            val sx = slotX(idx); val sy = slotY(idx)
            if (mouseX >= sx && mouseX < sx + slotSize && mouseY >= sy && mouseY < sy + slotSize) return idx
        }
        return -1
    }

    fun scissor(pad: Float = 0f): FloatArray {
        val items = itemsProvider()
        return floatArrayOf(x - pad, y - pad, x + totalW + pad, y + totalH(items.size) + pad)
    }
}
