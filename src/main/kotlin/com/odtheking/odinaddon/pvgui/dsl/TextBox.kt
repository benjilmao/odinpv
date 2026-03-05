package com.odtheking.odinaddon.pvgui.dsl

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

/** Render a §-formatted string at (x,y). Returns pixel width consumed. */
fun formattedText(text: String, x: Float, y: Float, size: Float): Float {
    val font = NVGRenderer.defaultFont
    var color = WHITE; var cx = x; val sb = StringBuilder()
    fun flush() {
        if (sb.isEmpty()) return
        val s = sb.toString()
        NVGRenderer.text(s, cx, y, size, color, font)
        cx += NVGRenderer.textWidth(s, size, font)
        sb.clear()
    }
    var i = 0
    while (i < text.length) {
        if (text[i] == '§' && i + 1 < text.length) {
            flush(); color = MC_COLORS[text[i + 1].lowercaseChar()] ?: WHITE; i += 2; continue
        }
        sb.append(text[i++])
    }
    flush(); return cx - x
}

fun String.stripCodes() = replace(Regex("§."), "")

/**
 * Evenly distributes [lines] inside a box, with an optional §-formatted [title] centred in the first slot.
 */
class TextBox(
    private val x: Float,
    private val y: Float,
    private val w: Float,
    private val h: Float,
    private val lines: List<String>,
    private val textSize: Float = 14f,
    private val title: String? = null,
    private val titleSize: Float = textSize,
) {
    private val slots = lines.size + if (title != null) 1 else 0
    private val rowH  = if (slots > 0) h / slots else h

    fun draw() {
        val font = NVGRenderer.defaultFont
        title?.let {
            val tw = NVGRenderer.textWidth(it.stripCodes(), titleSize, font)
            formattedText(it, x + (w - tw) / 2f, y + rowH / 2f - titleSize / 2f, titleSize)
        }
        lines.forEachIndexed { i, line ->
            val slot = i + if (title != null) 1 else 0
            formattedText(line, x, y + rowH * slot + rowH / 2f - textSize / 2f, textSize)
        }
    }
}
