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
) {
    private var boundsX = 0f
    private var centerY = 0f
    private var boundsW = 0f

    fun setCenterBounds(x: Float, centerY: Float, width: Float) {
        boundsX = x
        this.centerY = centerY
        boundsW = width
    }

    private fun itemWidth(itemList: List<*>): Float =
        (boundsW - (columns - 1) * gap) / columns.coerceAtLeast(1)

    private fun originY(itemList: List<*>): Float {
        val rows = ceil(itemList.size.toDouble() / columns).toInt().coerceAtLeast(1)
        val iw = itemWidth(itemList)
        val gridH = rows * iw + (rows - 1).coerceAtLeast(0) * gap
        return centerY - gridH / 2f
    }

    fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val list = items()
        val iw = itemWidth(list)
        val oy = originY(list)
        list.forEachIndexed { index, stack ->
            val sx = boundsX + (index % columns) * (iw + gap)
            val sy = oy + (index / columns) * (iw + gap)
            NVGRenderer.rect(sx, sy, iw, iw, colors?.invoke(stack, index) ?: Theme.slotBg, Theme.slotRadius)
            if (stack != null && !stack.isEmpty && PVState.isHovered(sx, sy, iw, iw))
                NVGRenderer.hollowRect(sx, sy, iw, iw, 1.5f, Theme.btnSelected, Theme.slotRadius)
        }
    }

    fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val list = items()
        val iw = itemWidth(list)
        val oy = originY(list)
        list.forEachIndexed { index, stack ->
            if (stack != null && !stack.isEmpty) {
                val sx = boundsX + (index % columns) * (iw + gap)
                val sy = oy + (index / columns) * (iw + gap)
                RenderQueue.enqueueItem(stack, sx, sy, iw)
            }
        }
    }

    fun click(mouseX: Double, mouseY: Double): Boolean {
        if (onSlotClick == null) return false
        val list = items()
        val iw = itemWidth(list)
        val oy = originY(list)
        list.forEachIndexed { index, stack ->
            if (stack != null && !stack.isEmpty) {
                val sx = boundsX + (index % columns) * (iw + gap)
                val sy = oy + (index / columns) * (iw + gap)
                if (mouseX >= sx && mouseX < sx + iw && mouseY >= sy && mouseY < sy + iw)
                    return onSlotClick.invoke(stack, index)
            }
        }
        return false
    }
}