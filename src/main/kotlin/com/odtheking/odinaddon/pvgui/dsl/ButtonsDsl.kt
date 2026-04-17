package com.odtheking.odinaddon.pvgui.dsl

import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.PAD
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.utils.Theme

fun <T> buttons(
    x: Float, y: Float, w: Float, h: Float,
    items: List<T>,
    padding: Float = PAD,
    vertical: Boolean = false,
    textSize: Float = 15f,
    radius: Float = Theme.radius,
    label: (T) -> String,
    onSelect: ButtonsDsl<T>.(T) -> Unit = {},
): ButtonsDsl<T> = ButtonsDsl(x, y, w, h, items, padding, vertical, textSize, radius, label).also {
    it.onSelectBlock = { item -> onSelect(it, item) }
}

class ButtonsDsl<T>(
    var x: Float, var y: Float, var w: Float, var h: Float,
    val items: List<T>,
    private val padding: Float,
    private val vertical: Boolean,
    private val textSize: Float,
    private val radius: Float,
    private val label: (T) -> String,
) {
    var selected: T? = items.firstOrNull()
    internal var onSelectBlock: (T) -> Unit = {}

    private val n get() = items.size.coerceAtLeast(1)

    private val btnW: Float get() = if (!vertical) (w - (n - 1) * padding) / n else w
    private val btnH: Float get() = if (!vertical) h else (h - (n - 1) * padding) / n

    private fun bx(i: Int) = if (!vertical) x + (btnW + padding) * i else x
    private fun by(i: Int) = if (!vertical) y else y + (btnH + padding) * i

    private fun isHovered(i: Int): Boolean {
        val bxi = bx(i); val byi = by(i)
        return PVState.mouseX >= bxi && PVState.mouseX < bxi + btnW &&
                PVState.mouseY >= byi && PVState.mouseY < byi + btnH
    }

    fun draw() {
        val font = NVGRenderer.defaultFont
        items.forEachIndexed { i, item ->
            val bxi = bx(i); val byi = by(i)
            val sel = item == selected
            val hov = !sel && isHovered(i)
            NVGRenderer.rect(bxi, byi, btnW, btnH,
                when { sel -> Theme.btnSelected; hov -> Theme.btnHover; else -> Theme.btnNormal },
                radius)
            val txt = label(item)
            val tw = NVGRenderer.textWidth(txt.stripCodes(), textSize, font)
            formattedText(txt, bxi + (btnW - tw) / 2f, byi + (btnH - textSize) / 2f, textSize,
                if (sel) Theme.textPrimary else Theme.textSecondary)
        }
    }

    fun click(mouseX: Double, mouseY: Double): Boolean {
        items.forEachIndexed { i, item ->
            val bxi = bx(i); val byi = by(i)
            if (mouseX >= bxi && mouseX < bxi + btnW &&
                mouseY >= byi && mouseY < byi + btnH) {
                if (item != selected) { selected = item; onSelectBlock(item) }
                return true
            }
        }
        return false
    }
}