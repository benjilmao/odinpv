package com.odtheking.odinaddon.pvgui.components

import com.odtheking.odinaddon.pvgui.core.Component
import com.odtheking.odinaddon.pvgui.core.RenderContext
import com.odtheking.odinaddon.pvgui.core.Renderer
import com.odtheking.odinaddon.pvgui.utils.Theme

class Buttons<T>(
    private val items: List<T>,
    private val spacing: Float = 6f,
    private val textSize: Float = 15f,
    private val vertical: Boolean = true,
    private val label: (T) -> String,
    var onSelect: ((T) -> Unit)? = null,
) : Component() {

    var selected: T? = null

    private fun itemSize() = if (vertical)
        (h - spacing * (items.size - 1).coerceAtLeast(0)) / items.size.coerceAtLeast(1)
    else
        (w - spacing * (items.size - 1).coerceAtLeast(0)) / items.size.coerceAtLeast(1)

    private fun itemX(i: Int) = if (vertical) x else x + i * (itemSize() + spacing)
    private fun itemY(i: Int) = if (vertical) y + i * (itemSize() + spacing) else y
    private fun itemW() = if (vertical) w else itemSize()
    private fun itemH() = if (vertical) itemSize() else h

    override fun draw(ctx: RenderContext) {
        ctx.register(this)
        val iw = itemW(); val ih = itemH()
        items.forEachIndexed { i, item ->
            val ix = itemX(i); val iy = itemY(i)
            val selected = item == selected
            val hovered = ctx.isHovered(ix, iy, iw, ih) && !selected
            Renderer.rect(ix, iy, iw, ih, when {
                selected -> Theme.btnSelected
                hovered  -> Theme.btnHover
                else     -> Theme.btnNormal
            }, Theme.radius)
            val text = label(item)
            val tw = Renderer.formattedTextWidth(text, textSize)
            Renderer.formattedText(text, ix + (iw - tw) / 2f, iy + (ih - textSize) / 2f, textSize)
        }
    }

    override fun click(ctx: RenderContext, mouseX: Double, mouseY: Double): Boolean {
        val iw = itemW(); val ih = itemH()
        items.forEachIndexed { i, item ->
            if (ctx.isHovered(itemX(i), itemY(i), iw, ih)) {
                selected = item
                onSelect?.invoke(item)
                return true
            }
        }
        return false
    }
}

class Button(
    val label: String,
    private val textSize: Float = 14f,
    private var selected: Boolean = false,
    var onClick: (() -> Unit)? = null,
) : Component() {

    override fun draw(ctx: RenderContext) {
        ctx.register(this)
        Renderer.rect(x, y, w, h, when {
            selected       -> Theme.btnSelected
            isHovered(ctx) -> Theme.btnHover
            else           -> Theme.btnNormal
        }, Theme.radius)
        val tw = Renderer.formattedTextWidth(label, textSize)
        Renderer.formattedText(label, x + (w - tw) / 2f, y + (h - textSize) / 2f, textSize)
    }

    override fun click(ctx: RenderContext, mouseX: Double, mouseY: Double): Boolean {
        if (!isHovered(ctx)) return false
        onClick?.invoke()
        return true
    }
}
