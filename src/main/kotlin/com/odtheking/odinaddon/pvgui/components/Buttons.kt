package com.odtheking.odinaddon.pvgui.components

import com.odtheking.odin.utils.Color
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.utils.Theme

class Button(
    x: Float, y: Float, w: Float, h: Float,
    val label: String,
    var selected: Boolean = false,
    private val textSize: Float = 16f,
    private val radius: Float = Theme.round,
    init: Button.() -> Unit = {},
) : Component(x, y, w, h) {

    private var onClick: () -> Unit = {}
    fun onClick(block: () -> Unit) { onClick = block }

    init { init() }

    override fun draw(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        val hovered = isHovered(ctx, mouseX, mouseY)
        ctx.rect(x, y, w, h, when {
            selected -> Theme.accent
            hovered  -> Theme.btnHover
            else     -> Theme.btnNormal
        }, radius)
        val tw = ctx.textWidth(label, textSize)
        ctx.text(
            label,
            x + (w - tw) / 2f,
            y + (h - textSize) / 2f - 1f,
            textSize,
            if (selected || hovered) Color(255, 255, 255) else Color(180, 180, 180),
        )
    }

    override fun click(ctx: DrawContext, mouseX: Double, mouseY: Double): Boolean {
        if (!isHovered(ctx, mouseX, mouseY)) return false
        onClick()
        return true
    }
}

class ButtonRow<T>(
    x: Float, y: Float, w: Float, h: Float,
    private val items: List<T>,
    private val spacing: Float = 6f,
    private val label: (T) -> String = { it.toString() },
    private val textSize: Float = 16f,
    private val radius: Float = Theme.round,
    default: T = items.first(),
    init: ButtonRow<T>.() -> Unit = {},
) : Component(x, y, w, h) {

    var selected: T = default
    private var onSelect: (T) -> Unit = {}
    fun onSelect(block: (T) -> Unit) { onSelect = block }

    init { init() }

    private val btnW get() = (w - spacing * (items.size - 1)) / items.size.coerceAtLeast(1)

    private fun btnX(i: Int) = x + i * (btnW + spacing)

    override fun draw(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        items.forEachIndexed { i, item ->
            Button(btnX(i), y, btnW, h, label(item), item == selected, textSize, radius)
                .draw(ctx, mouseX, mouseY)
        }
    }

    override fun click(ctx: DrawContext, mouseX: Double, mouseY: Double): Boolean {
        items.forEachIndexed { i, item ->
            if (ctx.isHovered(mouseX, mouseY, btnX(i), y, btnW, h)) {
                if (item != selected) { selected = item; onSelect(item) }
                return true
            }
        }
        return false
    }
}

class ButtonColumn<T>(
    x: Float, y: Float, w: Float, h: Float,
    private val items: List<T>,
    private val spacing: Float = 6f,
    private val label: (T) -> String = { it.toString() },
    private val textSize: Float = 16f,
    private val radius: Float = Theme.round,
    default: T = items.first(),
    init: ButtonColumn<T>.() -> Unit = {},
) : Component(x, y, w, h) {

    var selected: T = default
    private var onSelect: (T) -> Unit = {}
    fun onSelect(block: (T) -> Unit) { onSelect = block }

    init { init() }

    private val btnH get() = (h - spacing * (items.size - 1)) / items.size.coerceAtLeast(1)

    private fun btnY(i: Int) = y + i * (btnH + spacing)

    override fun draw(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        items.forEachIndexed { i, item ->
            Button(x, btnY(i), w, btnH, label(item), item == selected, textSize, radius)
                .draw(ctx, mouseX, mouseY)
        }
    }

    override fun click(ctx: DrawContext, mouseX: Double, mouseY: Double): Boolean {
        items.forEachIndexed { i, item ->
            if (ctx.isHovered(mouseX, mouseY, x, btnY(i), w, btnH)) {
                if (item != selected) { selected = item; onSelect(item) }
                return true
            }
        }
        return false
    }
}

class RowButton(
    x: Float, y: Float, w: Float, h: Float,
    private val label: String,
    private val labelScale: Float = 16f,
    private val btnLabel: String = "Check",
    private val btnW: Float = 50f,
    private val gap: Float = 6f,
    init: RowButton.() -> Unit = {},
) : Component(x, y, w, h) {

    private var onClick: () -> Unit = {}
    fun onClick(block: () -> Unit) { onClick = block }

    init { init() }

    private val btn get() = Button(
        x = x + w - btnW, y = y,
        w = btnW, h = h,
        label = btnLabel,
        textSize = labelScale * 0.85f,
    )

    override fun draw(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        ctx.formattedText(label, x, y + (h - labelScale) / 2f, labelScale)
        btn.draw(ctx, mouseX, mouseY)
    }

    override fun click(ctx: DrawContext, mouseX: Double, mouseY: Double): Boolean {
        if (!ctx.isHovered(mouseX, mouseY, x + w - btnW, y, btnW, h)) return false
        onClick()
        return true
    }
}