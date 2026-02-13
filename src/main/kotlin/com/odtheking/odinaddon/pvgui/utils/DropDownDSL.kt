package com.odtheking.odinaddon.pvgui.utils

import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.ui.HoverHandler
import com.odtheking.odin.utils.ui.animations.EaseInOutAnimation
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.core.Theme
import kotlin.math.floor

fun <T> dropDownMenu(
    box: Box,
    default: T,
    options: List<T>,
    spacer: Int,
    color: Int = Theme.buttonBg.rgba,
    radius: Float = Theme.roundness,
    extended: Boolean = false,
    dropDown: DropDownDSL<T>.() -> Unit
): DropDownDSL<T> = DropDownDSL(box, default, options, spacer, color, radius, extended).apply(dropDown)

class DropDownDSL<T>(
    private val box: Box,
    private val default: T,
    private val options: List<T>,
    private val spacer: Int,
    private val color: Int,
    private val radius: Float = 0f,
    var extended: Boolean = false
) {
    private val mainHover = HoverHandler(250)
    private val dropAnim = EaseInOutAnimation(250)

    private val itemAnims = List(options.size) { HoverHandler(250) }

    private var onExtend: () -> Unit = {}
    private var onSelect: (T) -> Unit = {}
    private var displayText: (T) -> String = { it.toString() }
    private var selectedText: (T) -> String = displayText

    private var selected = default
    private val trueOptions: List<T> get() = options.filterNot { it == selected }

    val getSelected: T get() = selected

    fun onSelect(init: (T) -> Unit) { onSelect = init }
    fun onExtend(init: () -> Unit) { onExtend = init }
    fun displayText(init: (T) -> String) { displayText = init }
    fun selectedText(init: (T) -> String) { selectedText = init }

    fun draw(mouseX: Int, mouseY: Int) {
        mainHover.handle(box.x, box.y, box.w, box.h, isHovered(box, mouseX, mouseY))

        val brightened = brightenColor(color, 1 + mainHover.percent() / 500f)
        NVGRenderer.rect(box.x, box.y, box.w, box.h, brightened, radius)

        Text.drawColored(
            text = selectedText(selected),
            x = box.centerX,
            y = box.centerY,
            height = 9 * 3f,
            defaultColor = Colors.WHITE,
            centering = Text.Centering.CENTER,
            alignment = Text.Alignment.MIDDLE
        )

        val progress = dropAnim.get(0f, (box.h + spacer) * (trueOptions.size + 1), !extended)
        val totalHeight = if (!dropAnim.isAnimating() && !extended) {
            return
        } else {
            box.h + floor(progress)
        }

        NVGRenderer.pushScissor(box.x, box.y, box.w, totalHeight)

        trueOptions.forEachIndexed { i, entry ->
            val y = box.y + (box.h + spacer) * (i + 1)

            itemAnims[i].handle(box.x, y, box.w, box.h, isHovered(Box(box.x, y, box.w, box.h), mouseX, mouseY))

            val itemColor = brightenColor(color, 1 + itemAnims[i].percent() / 500f)
            NVGRenderer.rect(box.x, y, box.w, box.h, itemColor, radius)

            Text.drawColored(
                text = displayText(entry),
                x = box.centerX,
                y = y + box.h / 2,
                height = 9 * 3f,
                defaultColor = Colors.WHITE,
                centering = Text.Centering.CENTER,
                alignment = Text.Alignment.MIDDLE
            )
        }

        NVGRenderer.popScissor()
    }

    fun click(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button != 0) return false

        if (isHovered(box, mouseX, mouseY)) {
            dropAnim.start()
            extended = !extended
            onExtend()
            return true
        }

        if (extended) {
            trueOptions.withIndex().forEach { (i, entry) ->
                val y = box.y + (box.h + spacer) * (i + 1)
                if (isHovered(Box(box.x, y, box.w, box.h), mouseX, mouseY)) {
                    dropAnim.start()
                    selected = entry
                    onSelect(entry)
                    extended = false
                    return true
                }
            }
            dropAnim.start()
            extended = false
            return true
        }

        return false
    }

    private fun isHovered(box: Box, mouseX: Int, mouseY: Int): Boolean =
        mouseX >= box.x && mouseX <= box.x + box.w &&
                mouseY >= box.y && mouseY <= box.y + box.h

    private fun brightenColor(color: Int, factor: Float): Int {
        val a = color shr 24 and 0xFF
        val r = ((color shr 16 and 0xFF) * factor).coerceIn(0f, 255f).toInt()
        val g = ((color shr 8 and 0xFF) * factor).coerceIn(0f, 255f).toInt()
        val b = ((color and 0xFF) * factor).coerceIn(0f, 255f).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}