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
    val itemWidgets: MutableList<AbstractWidget>,
    val overlayText: MutableList<() -> Unit>,
    private val clickRegistry: MutableList<Component>,
    var clipX: Float = Float.MIN_VALUE,
    var clipY: Float = Float.MIN_VALUE,
    var clipW: Float = Float.MAX_VALUE,
    var clipH: Float = Float.MAX_VALUE,
) {
    fun register(c: Component) { clickRegistry.add(c) }

    fun isHovered(x: Float, y: Float, w: Float, h: Float) =
        mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h

    fun item(stack: ItemStack, x: Float, y: Float, size: Float, showTooltip: Boolean = true, showStackSize: Boolean = true) {
        if (stack.isEmpty) return
        if (x + size <= clipX || x >= clipX + clipW) return
        if (y + size <= clipY || y >= clipY + clipH) return

        val dpr = NVGRenderer.devicePixelRatio()
        val toGuiPx = dpr / mc.window.guiScale.toFloat()
        val gx = ((originX + x * scale) * toGuiPx).toInt()
        val gy = ((originY + y * scale) * toGuiPx).toInt()
        val gs = (size * scale * toGuiPx).toInt().coerceAtLeast(1)

        Displays.item(stack, gs, gs, showTooltip = showTooltip, showStackSize = false).asWidget().also {
            it.setPosition(gx, gy)
            itemWidgets.add(it)
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
