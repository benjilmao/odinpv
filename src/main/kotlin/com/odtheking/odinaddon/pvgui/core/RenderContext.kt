package com.odtheking.odinaddon.pvgui.core

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import me.owdding.lib.displays.Displays
import me.owdding.lib.displays.asWidget
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.world.entity.LivingEntity
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

    private fun toScreenRect(x: Float, y: Float, w: Float, h: Float): Pair<IntArray, IntArray> {
        val dpr = NVGRenderer.devicePixelRatio()
        val toGuiPx = dpr / mc.window.guiScale.toFloat()
        val sx = ((originX + x * scale) * toGuiPx).toInt()
        val sy = ((originY + y * scale) * toGuiPx).toInt()
        val sw = (w * scale * toGuiPx).toInt()
        val sh = (h * scale * toGuiPx).toInt()
        val scissorArr = currentScissor?.let { sc ->
            intArrayOf(
                ((originX + sc[0] * scale) * toGuiPx).toInt(),
                ((originY + sc[1] * scale) * toGuiPx).toInt(),
                ((originX + sc[2] * scale) * toGuiPx).toInt(),
                ((originY + sc[3] * scale) * toGuiPx).toInt(),
            )
        } ?: intArrayOf()
        return intArrayOf(sx, sy, sw, sh) to scissorArr
    }

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

    fun isHovered(x: Float, y: Float, w: Float, h: Float): Boolean =
        mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h

    fun item(stack: ItemStack, x: Float, y: Float, size: Float, showTooltip: Boolean = true, showStackSize: Boolean = true) {
        if (stack.isEmpty) return
        val sc = currentScissor
        if (sc != null && (x + size <= sc[0] || x >= sc[2] || y + size <= sc[1] || y >= sc[3])) return

        val (screenPos, scissorArr) = toScreenRect(x, y, size, size)
        val gx = screenPos[0]
        val gy = screenPos[1]
        val gs = screenPos[2]

        Displays.item(stack, gs, gs, showTooltip = showTooltip, showStackSize = false).asWidget().also {
            it.setPosition(gx, gy)
            itemWidgets.add(it to scissorArr.takeIf { it.isNotEmpty() })
        }

        if (showStackSize && stack.count > 1) {
            val count = stack.count.toString()
            val countSize = size * 0.4f
            val tx = x + size - Renderer.textWidth(count, countSize)
            val ty = y + size - countSize
            overlayText.add { Renderer.text(count, tx, ty, countSize) }
        }
    }

    fun entity(entity: LivingEntity, x: Float, y: Float, w: Float, h: Float, mouseX: Float, mouseY: Float) {
        val sc = currentScissor
        if (sc != null && (x + w <= sc[0] || x >= sc[2] || y + h <= sc[1] || y >= sc[3])) return

        val (screenPos, scissorArr) = toScreenRect(x, y, w, h)
        val screenX = screenPos[0]
        val screenY = screenPos[1]
        val screenW = screenPos[2]
        val screenH = screenPos[3]

        val widgetMouseX = screenW / 2f + (mouseX - x - w / 2f) / w * screenW * 3f
        val widgetMouseY = (mouseY - y) / h * screenH
        val headOffsetY = screenH * (0.5f - 0.9f)

        val display = Displays.entity(entity, screenW, screenH, (screenH * 0.4f).toInt(), widgetMouseX, widgetMouseY - headOffsetY)

        val widget = object : AbstractWidget(screenX, screenY, screenW, screenH, net.minecraft.network.chat.Component.literal("")) {
            override fun renderWidget(graphics: GuiGraphics, mx: Int, my: Int, partialTick: Float) {
                display.render(graphics, screenX, screenY)
            }
            override fun updateWidgetNarration(b: NarrationElementOutput) {}
        }
        itemWidgets.add(widget to scissorArr.takeIf { it.isNotEmpty() })
    }
}