package com.odtheking.odinaddon.pvgui.core

import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.ui.rendering.NVGRenderer

private val MC_COLORS = mapOf(
    '0' to 0xFF000000.toInt(), '1' to 0xFF0000AA.toInt(), '2' to 0xFF00AA00.toInt(),
    '3' to 0xFF00AAAA.toInt(), '4' to 0xFFAA0000.toInt(), '5' to 0xFFAA00AA.toInt(),
    '6' to 0xFFFFAA00.toInt(), '7' to 0xFFAAAAAA.toInt(), '8' to 0xFF555555.toInt(),
    '9' to 0xFF5555FF.toInt(), 'a' to 0xFF55FF55.toInt(), 'b' to 0xFF55FFFF.toInt(),
    'c' to 0xFFFF5555.toInt(), 'd' to 0xFFFF55FF.toInt(), 'e' to 0xFFFFFF55.toInt(),
    'f' to 0xFFFFFFFF.toInt(),
)
private const val WHITE = 0xFFFFFFFF.toInt()

fun stripFormatting(text: String): String {
    val sb = StringBuilder()
    var i = 0
    while (i < text.length) {
        if (text[i] == '§' && i + 1 < text.length) { i += 2; continue }
        sb.append(text[i++])
    }
    return sb.toString()
}

object Renderer {

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float = 0f) =
        NVGRenderer.rect(x, y, w, h, color, radius)

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Color, radius: Float = 0f) =
        NVGRenderer.rect(x, y, w, h, color.rgba, radius)

    fun hollowRect(x: Float, y: Float, w: Float, h: Float, stroke: Float, color: Int, radius: Float = 0f) =
        NVGRenderer.hollowRect(x, y, w, h, stroke, color, radius)

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, stroke: Float, color: Int) =
        NVGRenderer.line(x1, y1, x2, y2, stroke, color)

    fun circle(x: Float, y: Float, radius: Float, color: Int) =
        NVGRenderer.circle(x, y, radius, color)

    fun text(text: String, x: Float, y: Float, size: Float, color: Int = WHITE) =
        NVGRenderer.text(text, x.toInt().toFloat(), y.toInt().toFloat(), size, color, NVGRenderer.defaultFont)

    fun textWidth(text: String, size: Float): Float =
        NVGRenderer.textWidth(text, size, NVGRenderer.defaultFont)

    fun formattedText(text: String, x: Float, y: Float, size: Float): Float {
        var color = WHITE
        var cx = x
        var i = 0
        val sb = StringBuilder()

        fun flush() {
            if (sb.isEmpty()) return
            val seg = sb.toString()
            NVGRenderer.text(seg, cx.toInt().toFloat(), y.toInt().toFloat(), size, color, NVGRenderer.defaultFont)
            cx += NVGRenderer.textWidth(seg, size, NVGRenderer.defaultFont)
            sb.clear()
        }

        while (i < text.length) {
            if (text[i] == '§' && i + 1 < text.length) {
                flush()
                color = MC_COLORS[text[i + 1].lowercaseChar()] ?: WHITE
                i += 2; continue
            }
            sb.append(text[i++])
        }
        flush()
        return cx - x
    }

    fun formattedTextWidth(text: String, size: Float): Float =
        NVGRenderer.textWidth(stripFormatting(text), size, NVGRenderer.defaultFont)

    fun pushScissor(x: Float, y: Float, w: Float, h: Float) = NVGRenderer.pushScissor(x, y, w, h)
    fun popScissor() = NVGRenderer.popScissor()

    fun dropShadow(x: Float, y: Float, w: Float, h: Float, blur: Float, spread: Float, radius: Float) =
        NVGRenderer.dropShadow(x, y, w, h, blur, spread, radius)
}
