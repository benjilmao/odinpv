package com.odtheking.odinaddon.pvgui

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.ui.rendering.Font
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import me.owdding.lib.displays.Displays
import me.owdding.lib.displays.asWidget
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.world.item.ItemStack

private val MC_COLORS = mapOf(
    '0' to 0xFF000000.toInt(), '1' to 0xFF0000AA.toInt(), '2' to 0xFF00AA00.toInt(),
    '3' to 0xFF00AAAA.toInt(), '4' to 0xFFAA0000.toInt(), '5' to 0xFFAA00AA.toInt(),
    '6' to 0xFFFFAA00.toInt(), '7' to 0xFFAAAAAA.toInt(), '8' to 0xFF555555.toInt(),
    '9' to 0xFF5555FF.toInt(), 'a' to 0xFF55FF55.toInt(), 'b' to 0xFF55FFFF.toInt(),
    'c' to 0xFFFF5555.toInt(), 'd' to 0xFFFF55FF.toInt(), 'e' to 0xFFFFFF55.toInt(),
    'f' to 0xFFFFFFFF.toInt(),
)
private const val DEFAULT_TEXT_COLOR = 0xFFFFFFFF.toInt()

fun stripFormatting(text: String): String {
    val sb = StringBuilder()
    var i = 0
    while (i < text.length) {
        if (text[i] == '§' && i + 1 < text.length) { i += 2; continue }
        sb.append(text[i++])
    }
    return sb.toString()
}

class DrawContext(
    val scale: Float,
    val originX: Float,
    val originY: Float,
    val font: Font,
    val itemWidgets: MutableList<AbstractWidget>,
    val overlayText: MutableList<() -> Unit> = mutableListOf(),
) {
    fun item(stack: ItemStack, x: Float, y: Float, size: Float, showTooltip: Boolean = true, showStackSize: Boolean = true) {
        val dpr = NVGRenderer.devicePixelRatio()
        val guiScale = mc.window.guiScale.toFloat()
        val toGuiPx = dpr / guiScale
        val gx = ((originX + x * scale) * toGuiPx).toInt()
        val gy = ((originY + y * scale) * toGuiPx).toInt()
        val gs = (size * scale * toGuiPx).toInt().coerceAtLeast(1)
        Displays.item(stack, gs, gs, showTooltip = showTooltip, showStackSize = false).asWidget().also {
            it.setPosition(gx, gy)
            itemWidgets.add(it)
        }
        if (showStackSize && stack.count > 1) {
            val countStr = stack.count.toString()
            val countSize = size * 0.4f
            val tx = x + size - textWidth(countStr, countSize)
            val ty = y + size - countSize
            overlayText.add { text(countStr, tx, ty, countSize, 0xFFFFFFFF.toInt()) }
        }
    }

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Color, radius: Float = 0f) =
        NVGRenderer.rect(x, y, w, h, color.rgba, radius)

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float = 0f) =
        NVGRenderer.rect(x, y, w, h, color, radius)

    fun hollowRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Color, radius: Float = 0f) =
        NVGRenderer.hollowRect(x, y, w, h, thickness, color.rgba, radius)

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Color) =
        NVGRenderer.line(x1, y1, x2, y2, thickness, color.rgba)

    fun text(text: String, x: Float, y: Float, size: Float, color: Color) =
        NVGRenderer.text(text, x, y, size, color.rgba, font)

    fun text(text: String, x: Float, y: Float, size: Float, color: Int) =
        NVGRenderer.text(text, x, y, size, color, font)

    fun textWidth(text: String, size: Float): Float =
        NVGRenderer.textWidth(text, size, font)

    fun formattedText(text: String, x: Float, y: Float, size: Float): Float {
        var color = DEFAULT_TEXT_COLOR
        var cursorX = x
        var i = 0
        val sb = StringBuilder()

        fun flush() {
            if (sb.isEmpty()) return
            val seg = sb.toString()
            NVGRenderer.text(seg, cursorX, y, size, color, font)
            cursorX += NVGRenderer.textWidth(seg, size, font)
            sb.clear()
        }

        while (i < text.length) {
            if (text[i] == '§' && i + 1 < text.length) {
                flush()
                color = MC_COLORS[text[i + 1].lowercaseChar()] ?: DEFAULT_TEXT_COLOR
                i += 2; continue
            }
            sb.append(text[i++])
        }
        flush()
        return cursorX - x
    }

    fun formattedTextWidth(text: String, size: Float): Float =
        NVGRenderer.textWidth(stripFormatting(text), size, font)

    fun textList(lines: List<String>, x: Float, y: Float, w: Float, h: Float, maxSize: Float = 14f, title: String? = null) {
        if (lines.isEmpty()) return
        val totalSlots = lines.size + if (title != null) 1 else 0
        val entryHeight = h / totalSlots
        val itemY = entryHeight / 2f

        title?.let {
            val titleSize = minOf(maxSize * 1.2f, entryHeight * 0.75f)
            val tw = formattedTextWidth(it, titleSize)
            formattedText(it, x + (w - tw) / 2f, y + entryHeight - itemY - titleSize / 2f, titleSize)
        }

        val size = minOf(maxSize, entryHeight * 0.65f)
        lines.forEachIndexed { i, line ->
            val slot = i + if (title != null) 1 else 0
            formattedText(line, x, y + entryHeight * (slot + 1) - itemY - size / 2f, size)
        }
    }

    fun pushScissor(x: Float, y: Float, w: Float, h: Float) = NVGRenderer.pushScissor(x, y, w, h)
    fun popScissor() = NVGRenderer.popScissor()

    fun isHovered(mouseX: Double, mouseY: Double, x: Float, y: Float, w: Float, h: Float): Boolean {
        val lmx = (mouseX - originX) / scale
        val lmy = (mouseY - originY) / scale
        return lmx >= x && lmx <= x + w && lmy >= y && lmy <= y + h
    }
}