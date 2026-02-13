package com.odtheking.odinaddon.pvgui.utils

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.ui.rendering.NVGRenderer

object Text {

    enum class Alignment { ABOVE, MIDDLE, BELOW }
    enum class Centering { LEFT, CENTER, RIGHT }

    fun drawColored(
        text: String,
        x: Float,
        y: Float,
        height: Float,
        defaultColor: Color = Colors.WHITE,
        centering: Centering = Centering.LEFT,
        alignment: Alignment = Alignment.ABOVE
    ) {
        if (text.isEmpty()) return

        val yPos = when (alignment) {
            Alignment.ABOVE -> y
            Alignment.MIDDLE -> y - height / 2
            Alignment.BELOW -> y - height
        }

        val textWidth = NVGRenderer.textWidth(text.noControlCodes, height, NVGRenderer.defaultFont)
        val xStart = when (centering) {
            Centering.LEFT -> x
            Centering.CENTER -> x - textWidth / 2
            Centering.RIGHT -> x - textWidth
        }

        val segments = ColorUtils.parseColoredText(text, defaultColor.rgba)
        var xOffset = xStart
        segments.forEach { (segmentText, rgba) ->
            NVGRenderer.text(
                segmentText,
                xOffset,
                yPos,
                height,
                rgba,
                NVGRenderer.defaultFont
            )
            xOffset += NVGRenderer.textWidth(segmentText, height, NVGRenderer.defaultFont)
        }
    }

    fun draw(
        text: String,
        x: Float,
        y: Float,
        scale: Float = 1f,
        defaultColor: Color = Colors.WHITE,
        centering: Centering = Centering.LEFT,
        alignment: Alignment = Alignment.ABOVE
    ) {
        drawColored(text, x, y, 9 * scale, defaultColor, centering, alignment)
    }

    fun fillText(
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        baseScale: Float = 2f,
        defaultColor: Color = Colors.WHITE
    ) {
        if (text.isEmpty()) return

        val baseHeight = 9 * baseScale
        val textWidth = getWidth(text, baseHeight)
        val scale = if (textWidth > maxWidth) maxWidth / textWidth else 1f
        val finalHeight = baseHeight * scale

        drawColored(
            text = text,
            x = x,
            y = y,
            height = finalHeight,
            defaultColor = defaultColor,
            centering = Centering.CENTER,
            alignment = Alignment.MIDDLE
        )
    }

    fun getWidth(text: String, height: Float): Float =
        NVGRenderer.textWidth(text.noControlCodes, height, NVGRenderer.defaultFont)

    fun getWidthScaled(text: String, scale: Float = 1f): Float =
        getWidth(text, 9 * scale)
}

private val String.noControlCodes: String
    get() = this.replace(Regex("§[0-9a-fk-or]"), "")