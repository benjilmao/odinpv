package com.odtheking.odinaddon.pvgui.core

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import me.owdding.lib.displays.Displays
import me.owdding.lib.displays.asWidget
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.world.item.ItemStack

class RenderContext(
    val scale: Float,
    val originX: Float,
    val originY: Float,
    val mouseX: Double,
    val mouseY: Double,
    val itemWidgets: MutableList<Pair<AbstractWidget, IntArray?>>,
    val overlayText: MutableList<() -> Unit>,
    private val clickRegistry: MutableList<Component>,
) {
    private val scissorStack = ArrayDeque<FloatArray>()
    private val currentScissor: FloatArray? get() = scissorStack.lastOrNull()

    fun register(c: Component) { clickRegistry.add(c) }

    fun pushScissor(x: Float, y: Float, w: Float, h: Float) {
        val prev = currentScissor
        val nx = if (prev != null) maxOf(x, prev[0]) else x
        val ny = if (prev != null) maxOf(y, prev[1]) else y
        val nx2 = if (prev != null) minOf(x + w, prev[2]) else x + w
        val ny2 = if (prev != null) minOf(y + h, prev[3]) else y + h
        scissorStack.addLast(floatArrayOf(nx, ny, nx2, ny2))
        Renderer.pushScissor(nx, ny, nx2 - nx, ny2 - ny)
    }

    fun popScissor() {
        scissorStack.removeLastOrNull()
        Renderer.popScissor()
    }

    fun isHovered(x: Float, y: Float, w: Float, h: Float) =
        mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h

    fun item(stack: ItemStack, x: Float, y: Float, size: Float, showTooltip: Boolean = true, showStackSize: Boolean = true) {
        if (stack.isEmpty) return
        val sc = currentScissor
        if (sc != null && (x + size <= sc[0] || x >= sc[2] || y + size <= sc[1] || y >= sc[3])) return

        val dpr = NVGRenderer.devicePixelRatio()
        val toGuiPx = dpr / mc.window.guiScale.toFloat()
        val gx = ((originX + x * scale) * toGuiPx).toInt()
        val gy = ((originY + y * scale) * toGuiPx).toInt()
        val gs = (size * scale * toGuiPx).toInt().coerceAtLeast(1)

        val guiScissor = sc?.let { intArrayOf(
            ((originX + it[0] * scale) * toGuiPx).toInt(),
            ((originY + it[1] * scale) * toGuiPx).toInt(),
            ((originX + it[2] * scale) * toGuiPx).toInt(),
            ((originY + it[3] * scale) * toGuiPx).toInt(),
        )}

        Displays.item(stack, gs, gs, showTooltip = showTooltip, showStackSize = false).asWidget().also {
            it.setPosition(gx, gy)
            itemWidgets.add(it to guiScissor)
        }

        if (showStackSize && stack.count > 1) {
            val count = stack.count.toString()
            val countSize = size * 0.4f
            val tx = x + size - Renderer.textWidth(count, countSize)
            val ty = y + size - countSize
            overlayText.add { Renderer.text(count, tx, ty, countSize) }
        }
    }
}