package com.odtheking.odinaddon.pvgui.utils

import com.odtheking.odin.utils.Color
import com.odtheking.odinaddon.pvgui.DrawContext

class ButtonGroup<T>(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val options: List<T>,
    private val spacing: Float = 6f,
    private val vertical: Boolean = false,
    val label: (T) -> String = { it.toString() },
    private val textSize: Float = 16f,
    default: T = options.first(),
) {
    var selected: T = default
    private var onSelect: (T) -> Unit = {}

    fun onSelect(block: (T) -> Unit) { onSelect = block }

    private val buttonWidth get() = if (vertical) w
    else (w - spacing * (options.size - 1)) / options.size.coerceAtLeast(1)

    private val buttonHeight get() = if (vertical)
        (h - spacing * (options.size - 1)) / options.size.coerceAtLeast(1)
    else h

    private fun buttonX(index: Int) = if (vertical) x else x + index * (buttonWidth + spacing)
    private fun buttonY(index: Int) = if (vertical) y + index * (buttonHeight + spacing) else y

    fun draw(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        options.forEachIndexed { index, option ->
            val buttonLeft = buttonX(index)
            val buttonTop  = buttonY(index)
            val isSelected = option == selected
            val isHovered  = ctx.isHovered(mouseX, mouseY, buttonLeft, buttonTop, buttonWidth, buttonHeight)

            ctx.rect(buttonLeft, buttonTop, buttonWidth, buttonHeight, when {
                isSelected -> Theme.accent
                isHovered  -> Theme.btnHover
                else       -> Theme.btnNormal
            }, Theme.round)

            val text      = label(option)
            val textWidth = ctx.textWidth(text, textSize)
            ctx.text(
                text,
                buttonLeft + (buttonWidth  - textWidth) / 2f,
                buttonTop  + (buttonHeight - textSize)  / 2f - 1f,
                textSize,
                if (isSelected || isHovered) Color(255, 255, 255) else Color(180, 180, 180)
            )
        }
    }

    fun onClick(ctx: DrawContext, mouseX: Double, mouseY: Double): Boolean {
        options.forEachIndexed { index, option ->
            if (ctx.isHovered(mouseX, mouseY, buttonX(index), buttonY(index), buttonWidth, buttonHeight)) {
                if (option != selected) { selected = option; onSelect(option) }
                return true
            }
        }
        return false
    }
}