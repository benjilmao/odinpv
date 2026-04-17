package com.odtheking.odinaddon.pvgui.dsl

import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.utils.Theme
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import kotlin.math.ceil

class ItemGridDsl(
    private val columns: Int,
    private val gap: Float = 10f,
    private val items: () -> List<ItemStack?>,
    private val colors: ((ItemStack?, Int) -> Int)? = null,
    private val onSlotClick: ((ItemStack, Int) -> Boolean)? = null,
    private val overrideSlotSize: Float? = null,
) {
    private var boundsX = 0f
    private var centerY = 0f
    private var boundsW = 0f

    fun setCenterBounds(x: Float, centerY: Float, width: Float) {
        boundsX = x
        this.centerY = centerY
        boundsW = width
    }

    private fun slotSize(): Float =
        overrideSlotSize ?: ((boundsW - (columns - 1) * gap) / columns.coerceAtLeast(1))

    private fun originY(count: Int): Float {
        val rows = ceil(count.toDouble() / columns).toInt().coerceAtLeast(1)
        val slot = slotSize()
        val gridH = rows * slot + (rows - 1).coerceAtLeast(0) * gap
        return centerY - gridH / 2f
    }

    fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val list = items()
        val slot = slotSize()
        val oy = originY(list.size)
        list.forEachIndexed { index, stack ->
            val sx = boundsX + (index % columns) * (slot + gap)
            val sy = oy + (index / columns) * (slot + gap)
            NVGRenderer.rect(sx, sy, slot, slot, colors?.invoke(stack, index) ?: Theme.slotBg, Theme.slotRadius)
            if (stack != null && !stack.isEmpty && PVState.isHovered(sx, sy, slot, slot))
                NVGRenderer.hollowRect(sx, sy, slot, slot, 1.5f, Theme.btnSelected, Theme.slotRadius)
        }
    }

    fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val list = items()
        val slot = slotSize()
        val oy = originY(list.size)
        list.forEachIndexed { index, stack ->
            if (stack != null && !stack.isEmpty) {
                val sx = boundsX + (index % columns) * (slot + gap)
                val sy = oy + (index / columns) * (slot + gap)
                RenderQueue.enqueueItem(stack, sx, sy, slot)
            }
        }
    }

    fun click(mouseX: Double, mouseY: Double): Boolean {
        if (onSlotClick == null) return false
        val list = items()
        val slot = slotSize()
        val oy = originY(list.size)
        list.forEachIndexed { index, stack ->
            if (stack != null && !stack.isEmpty) {
                val sx = boundsX + (index % columns) * (slot + gap)
                val sy = oy + (index / columns) * (slot + gap)
                if (mouseX >= sx && mouseX < sx + slot && mouseY >= sy && mouseY < sy + slot)
                    return onSlotClick.invoke(stack, index)
            }
        }
        return false
    }
}