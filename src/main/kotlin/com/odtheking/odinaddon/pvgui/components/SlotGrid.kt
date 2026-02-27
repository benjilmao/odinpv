package com.odtheking.odinaddon.pvgui.components

import com.odtheking.odin.utils.Color
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.utils.Theme
import net.minecraft.world.item.ItemStack

class SlotGrid<T>(
    x: Float, y: Float, w: Float, h: Float,
    private val items: List<T>,
    private val cols: Int,
    private val spacing: Float = 6f,
    private val scroll: () -> Int = { 0 },
    private val toItemStack: (T) -> ItemStack,
    private val slotColor: (T) -> Color = { Theme.slotBg },
    private val selectedIndex: () -> Int = { -1 },
    private val radius: Float = Theme.round,
    init: SlotGrid<T>.() -> Unit = {},
) : Component(x, y, w, h) {

    private var onSlotClick: (index: Int, item: T) -> Unit = { _, _ -> }
    fun onSlotClick(block: (index: Int, item: T) -> Unit) { onSlotClick = block }

    init { init() }

    private fun computeSlotSize(): Float {
        val rows = (items.size + cols - 1) / cols
        return minOf(
            (w - spacing * (cols - 1)) / cols,
            (h - spacing * (rows - 1)) / rows,
        )
    }

    private fun slotX(idx: Int, slotSize: Float) = x + (idx % cols) * (slotSize + spacing)
    private fun slotY(idx: Int, slotSize: Float) = y + (idx / cols - scroll()) * (slotSize + spacing)

    override fun draw(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        if (items.isEmpty()) return
        val slotSize = computeSlotSize()
        val visibleRows = (h / (slotSize + spacing)).toInt()
        val startIdx = scroll() * cols
        val endIdx = (startIdx + (visibleRows + 1) * cols).coerceAtMost(items.size)

        ctx.pushScissor(x, y, w, h)
        for (idx in startIdx until endIdx) {
            val item = items[idx]
            val sx = slotX(idx, slotSize)
            val sy = slotY(idx, slotSize)
            if (sy + slotSize < y || sy > y + h) continue

            ctx.rect(sx, sy, slotSize, slotSize, slotColor(item), radius)

            if (idx == selectedIndex()) {
                ctx.hollowRect(sx, sy, slotSize, slotSize, 2f, Color(255, 255, 255, 0.9f), radius)
            }

            val stack = toItemStack(item)
            if (!stack.isEmpty) {
                val pad = slotSize * 0.05f
                ctx.item(stack, sx + pad, sy + pad, slotSize - pad * 2f)
            }
        }
        ctx.popScissor()
    }

    override fun click(ctx: DrawContext, mouseX: Double, mouseY: Double): Boolean {
        if (items.isEmpty()) return false
        val slotSize = computeSlotSize()
        val visibleRows = (h / (slotSize + spacing)).toInt()
        val startIdx = scroll() * cols
        val endIdx = (startIdx + (visibleRows + 1) * cols).coerceAtMost(items.size)

        for (idx in startIdx until endIdx) {
            if (ctx.isHovered(mouseX, mouseY, slotX(idx, slotSize), slotY(idx, slotSize), slotSize, slotSize)) {
                onSlotClick(idx, items[idx])
                return true
            }
        }
        return false
    }
}