package com.odtheking.odinaddon.pvgui.utils

import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.ui.HoverHandler
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.core.Theme

fun <T> buttons(
    box: Box,
    padding: Int,
    default: T,
    options: List<T>,
    textScale: Float,
    color: Int = Theme.buttonBg.rgba,
    selectedColor: Int = Theme.buttonSelected.rgba,
    radius: Float = Theme.buttonRoundness,
    vertical: Boolean = false,
    buttonDSL: ButtonDSL<T>.() -> Unit
) = ButtonDSL(box, padding, default, options, textScale, color, selectedColor, radius, vertical).apply(buttonDSL)

class ButtonDSL<T>(
    private val box: Box,
    private val padding: Int,
    private val default: T,
    private val options: List<T>,
    private val textScale: Float,
    private val color: Int,
    private val selectedColor: Int,
    private val radius: Float,
    private val vertical: Boolean
) {
    // Use HoverHandler for smooth animations
    private val hoverHandlers = List(options.size) { HoverHandler(200) }

    var selected: T = default
        set(value) {
            if (field != value) {
                field = value
                onSelect(value)
            }
        }

    private var onSelect: (T) -> Unit = {}

    fun onSelect(init: (T) -> Unit) { onSelect = init }

    private val buttonWidth = if (!vertical) (box.w - (options.size - 1) * padding) / options.size else box.w
    private val buttonHeight = if (!vertical) box.h else (box.h - (options.size - 1) * padding) / options.size

    fun draw(mouseX: Int, mouseY: Int) {
        options.forEachIndexed { i, option ->
            val x = if (!vertical) box.x + (buttonWidth + padding) * i else box.x
            val y = if (!vertical) box.y else box.y + (buttonHeight + padding) * i

            val isHovered = mouseX in x.toInt()..(x + buttonWidth).toInt() &&
                    mouseY in y.toInt()..(y + buttonHeight).toInt()
            val isSelected = option == selected

            // Update hover animation
            hoverHandlers[i].handle(x, y, buttonWidth, buttonHeight, isHovered)

            // Choose color - SAME AS YOUR PROFILE SWITCHER!
            val finalColor = when {
                isSelected -> selectedColor
                isHovered || hoverHandlers[i].isHovered -> Theme.buttonHover.rgba
                else -> color
            }

            NVGRenderer.rect(x, y, buttonWidth, buttonHeight, finalColor, radius)

            Text.draw(
                text = option.toString(),
                x = x + buttonWidth / 2,
                y = y + buttonHeight / 2,
                scale = textScale,
                defaultColor = Colors.WHITE,
                centering = Text.Centering.CENTER,
                alignment = Text.Alignment.MIDDLE
            )
        }
    }

    fun click(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button != 0) return false
        options.forEachIndexed { i, option ->
            val x = if (!vertical) box.x + (buttonWidth + padding) * i else box.x
            val y = if (!vertical) box.y else box.y + (buttonHeight + padding) * i
            if (mouseX in x.toInt()..(x + buttonWidth).toInt() &&
                mouseY in y.toInt()..(y + buttonHeight).toInt()
            ) {
                if (option != selected) {
                    selected = option
                }
                return true
            }
        }
        return false
    }
}