package com.odtheking.odinaddon.pvgui.components

import com.odtheking.odinaddon.pvgui.core.Component
import com.odtheking.odinaddon.pvgui.core.RenderContext
import com.odtheking.odinaddon.pvgui.core.Renderer
import com.odtheking.odinaddon.pvgui.utils.Theme
import net.minecraft.world.item.ItemStack
import kotlin.math.min

fun String.asText() = TextBoxLine.Text(this)

fun String.withButton(
    label: String,
    onClick: () -> Unit,
    inline: Boolean = false,
    textSize: Float = 14f,
): TextBoxLine.WithButton = TextBoxLine.WithButton(this, Button(label, textSize).apply { this.onClick = onClick }, inline)

fun String.withButton(button: Button, inline: Boolean = false): TextBoxLine.WithButton =
    TextBoxLine.WithButton(this, button, inline)

fun String.withItem(stack: ItemStack, showTooltip: Boolean = true, inline: Boolean = false) =
    TextBoxLine.WithItem(this, stack, showTooltip, inline)

sealed class TextBoxLine {
    data class Text(val text: String) : TextBoxLine()
    data class WithButton(val text: String, val button: Button, val inline: Boolean = false) : TextBoxLine()
    data class WithItem(val text: String, val stack: ItemStack, val showTooltip: Boolean = true, val inline: Boolean = false) : TextBoxLine()
}

class TextBox(
    private val lines: List<TextBoxLine>,
    private val maxSize: Float = 14f,
    private val title: String? = null,
    private val titleScale: Float = maxSize * 1.2f,
) : Component() {

    override fun draw(ctx: RenderContext) {
        if (lines.isEmpty()) return
        ctx.register(this)

        val slots = lines.size + if (title != null) 1 else 0
        val rowH = h / slots
        val size = minOf(maxSize, rowH * 0.65f)
        val midY = rowH / 2f

        title?.let {
            val tw = Renderer.formattedTextWidth(it, titleScale)
            Renderer.formattedText(it, x + (w - tw) / 2f, y + rowH - midY - titleScale / 2f, titleScale)
        }

        lines.forEachIndexed { i, line ->
            val slot = i + if (title != null) 1 else 0
            val lineY = y + rowH * (slot + 1) - midY - size / 2f
            val rowTop = y + rowH * slot

            when (line) {
                is TextBoxLine.Text -> Renderer.formattedText(line.text, x, lineY, size)

                is TextBoxLine.WithButton -> {
                    Renderer.formattedText(line.text, x, lineY, size)
                    val textW = Renderer.formattedTextWidth(line.text, size)
                    val btnW = Renderer.formattedTextWidth(line.button.label, size) + 12f
                    val btnH = min(rowH * 0.6f, 22f)
                    val btnX = if (line.inline) x + textW + 6f else x + w - btnW
                    val btnY = rowTop + (rowH - btnH) / 2f
                    line.button.setBounds(btnX, btnY, btnW, btnH)
                    line.button.draw(ctx)
                }

                is TextBoxLine.WithItem -> {
                    Renderer.formattedText(line.text, x, lineY, size)
                    val textW = Renderer.formattedTextWidth(line.text, size)
                    val itemSize = 40f
                    val itemX = if (line.inline) x + textW + 6f else x + w - itemSize
                    val itemY = rowTop + (rowH - itemSize) / 2f
                    ctx.item(line.stack, itemX, itemY, itemSize, showTooltip = line.showTooltip, showStackSize = false)
                }
            }
        }
    }
}

class ItemSlot(
    private val stack: ItemStack,
    private val showTooltip: Boolean = true,
    private val showStackSize: Boolean = true,
    private val bg: Int = Theme.slotBg,
) : Component() {

    override fun draw(ctx: RenderContext) {
        if (bg != 0) Renderer.rect(x, y, w, h, bg, Theme.radius)
        ctx.item(stack, x, y, w, showTooltip, showStackSize)
    }
}

class ProgressBar(
    private val progress: Float,
    private val trackColor: Int = 0x33FFFFFF,
    private val fillColor: Int = 0xFF55FFFF.toInt(),
) : Component() {

    override fun draw(ctx: RenderContext) {
        Renderer.rect(x, y, w, h, trackColor, h / 2f)
        val fw = (w * progress.coerceIn(0f, 1f)).coerceAtLeast(0f)
        if (fw > 0f) Renderer.rect(x, y, fw, h, fillColor, h / 2f)
    }
}

class Separator(
    private val vertical: Boolean = false,
    private val color: Int = Theme.separator,
    private val margin: Float = 4f,
) : Component() {

    override fun draw(ctx: RenderContext) {
        if (vertical)
            Renderer.line(x + w / 2f, y + margin, x + w / 2f, y + h - margin, 1f, color)
        else
            Renderer.line(x + margin, y + h / 2f, x + w - margin, y + h / 2f, 1f, color)
    }
}

class SlotGrid<T>(
    val items: List<T>,
    val cols: Int,
    val spacing: Float = 4f,
    private val toStack: (T) -> ItemStack,
    private val itemBg: ((T) -> Int)? = null,
    initialScroll: Int = 0,
    initialSelected: Int = -1,
    private val onSelect: ((Int, T) -> Unit)? = null,
    private val onScroll: ((Int) -> Unit)? = null,
) : Component() {

    var scrollOffset = initialScroll
        private set
    var selectedIndex = initialSelected
        private set

    private val slotSize get() = (w - spacing * (cols - 1)) / cols
    private fun visibleRows() = ((h + spacing) / (slotSize + spacing)).toInt()
    private fun totalRows() = (items.size + cols - 1) / cols
    private fun maxScroll() = (totalRows() - visibleRows()).coerceAtLeast(0)

    override fun draw(ctx: RenderContext) {
        ctx.register(this)
        val ss = slotSize
        scrollOffset = scrollOffset.coerceIn(0, maxScroll())

        ctx.pushScissor(x, y, w, h)

        val firstRow = scrollOffset
        val lastRow = (firstRow + visibleRows() + 1).coerceAtMost(totalRows())

        for (row in firstRow until lastRow) {
            for (col in 0 until cols) {
                val idx = row * cols + col
                if (idx >= items.size) break
                val sx = x + col * (ss + spacing)
                val sy = y + (row - firstRow) * (ss + spacing)
                if (sy + ss <= y || sy >= y + h) continue
                val item = items[idx]
                Renderer.rect(sx, sy, ss, ss, itemBg?.invoke(item) ?: Theme.slotBg, Theme.radius)
                ctx.item(toStack(item), sx, sy, ss)
                if (idx == selectedIndex) Renderer.hollowRect(sx, sy, ss, ss, 2f, 0xFFFFFFFF.toInt(), Theme.radius)
            }
        }
        ctx.popScissor()
    }

    override fun click(ctx: RenderContext, mouseX: Double, mouseY: Double): Boolean {
        if (!isHovered(ctx)) return false
        val ss = slotSize
        val col = ((mouseX - x) / (ss + spacing)).toInt().coerceIn(0, cols - 1)
        val row = ((mouseY - y) / (ss + spacing)).toInt() + scrollOffset
        val idx = row * cols + col
        if (idx in items.indices) {
            selectedIndex = if (selectedIndex == idx) -1 else idx
            onSelect?.invoke(selectedIndex, items[idx])
            return true
        }
        return false
    }

    override fun scroll(ctx: RenderContext, delta: Double): Boolean {
        if (!isHovered(ctx)) return false
        val prev = scrollOffset
        scrollOffset = (scrollOffset - delta.toInt()).coerceIn(0, maxScroll())
        if (prev != scrollOffset) onScroll?.invoke(scrollOffset)
        return true
    }
}
