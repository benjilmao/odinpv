package com.odtheking.odinaddon.pvgui.dsl

import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.utils.Theme

fun <T> buttons(
    x: Float, y: Float, w: Float, h: Float,
    items: List<T>,
    vertical: Boolean = true,
    spacing: Float = 6f,
    textSize: Float = 15f,
    radius: Float = 6f,
    label: (T) -> String,
    onSelect: ButtonsDsl<T>.(T) -> Unit = {},
): ButtonsDsl<T> = ButtonsDsl(x, y, w, h, items, vertical, spacing, textSize, radius, label).also {
    it.onSelectBlock = { item -> onSelect(it, item) }
}

class ButtonsDsl<T>(
    x: Float, y: Float, w: Float, h: Float,
    val items: List<T>,
    private val vertical: Boolean,
    private val spacing: Float,
    private val textSize: Float,
    private val radius: Float,
    private val label: (T) -> String,
) {
    var x = x; var y = y; var w = w; var h = h
    var selected: T? = items.firstOrNull()
    internal var onSelectBlock: (T) -> Unit = {}

    private val count get() = items.size.coerceAtLeast(1)
    private fun itemW() = if (vertical) w else (w - spacing * (count - 1)) / count
    private fun itemH() = if (vertical) (h - spacing * (count - 1)) / count else h
    private fun ix(i: Int) = if (vertical) x else x + i * (itemW() + spacing)
    private fun iy(i: Int) = if (vertical) y + i * (itemH() + spacing) else y

    fun draw() {
        val iw = itemW(); val ih = itemH()
        val font = NVGRenderer.defaultFont
        items.forEachIndexed { i, item ->
            val bx = ix(i); val by = iy(i)
            val isSel = item == selected
            val isHov = !isSel && PVState.isHovered(bx, by, iw, ih)
            val col = when { isSel -> Theme.btnSelected; isHov -> Theme.btnHover; else -> Theme.btnNormal }
            val textCol = if (isSel || isHov) Theme.textPrimary else Theme.textSecondary
            NVGRenderer.rect(bx, by, iw, ih, col, radius)
            val txt = label(item)
            val tw = NVGRenderer.textWidth(txt, textSize, font)
            NVGRenderer.text(txt, bx + (iw - tw) / 2f, by + (ih - textSize) / 2f, textSize, textCol, font)
        }
    }

    fun click(mouseX: Double, mouseY: Double): Boolean {
        val iw = itemW(); val ih = itemH()
        items.forEachIndexed { i, item ->
            val bx = ix(i); val by = iy(i)
            if (mouseX >= bx && mouseX < bx + iw && mouseY >= by && mouseY < by + ih) {
                if (item != selected) { selected = item; onSelectBlock(item) }
                return true
            }
        }
        return false
    }
}
