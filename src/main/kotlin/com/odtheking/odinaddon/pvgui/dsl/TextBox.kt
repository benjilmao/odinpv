package com.odtheking.odinaddon.pvgui.dsl

import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.utils.Theme

// ── Minecraft colour-code renderer ───────────────────────────────────────────
private val MC_COLORS = mapOf(
    '0' to 0xFF000000.toInt(), '1' to 0xFF0000AA.toInt(), '2' to 0xFF00AA00.toInt(),
    '3' to 0xFF00AAAA.toInt(), '4' to 0xFFAA0000.toInt(), '5' to 0xFFAA00AA.toInt(),
    '6' to 0xFFFFAA00.toInt(), '7' to 0xFFAAAAAA.toInt(), '8' to 0xFF555555.toInt(),
    '9' to 0xFF5555FF.toInt(), 'a' to 0xFF55FF55.toInt(), 'b' to 0xFF55FFFF.toInt(),
    'c' to 0xFFFF5555.toInt(), 'd' to 0xFFFF55FF.toInt(), 'e' to 0xFFFFFF55.toInt(),
    'f' to 0xFFFFFFFF.toInt(),
)
private const val WHITE_RGBA = 0xFFFFFFFF.toInt()

/**
 * Renders a Minecraft §-coded string left-to-right using NVGRenderer.
 * Returns the total pixel width rendered.
 * This replaces HC's Text.colorText.
 */
fun formattedText(text: String, x: Float, y: Float, size: Float, defaultColor: Int = WHITE_RGBA): Float {
    val font = NVGRenderer.defaultFont
    var color = defaultColor
    var cx = x
    var i = 0
    while (i < text.length) {
        if (text[i] == '§' && i + 1 < text.length) {
            color = MC_COLORS[text[i + 1].lowercaseChar()] ?: defaultColor
            i += 2
            continue
        }
        val ch = text[i].toString()
        NVGRenderer.text(ch, cx, y, size, color, font)
        cx += NVGRenderer.textWidth(ch, size, font)
        i++
    }
    return cx - x
}

/** Strip §-codes for width measurement. */
fun String.stripCodes(): String = replace(Regex("§."), "")

/**
 * Port of HC's TextBox.
 *
 * HC divides the box into (lines.size + if(title!=null) 1 else 0) equal rows.
 * Each row is entryHeight tall. itemY = entryHeight/2 (text vertical centre offset).
 * Title is centred horizontally; lines are left-aligned with [spacer] padding.
 *
 * HC's scale param maps as: nvgFontSize = 9f * scale  (HC uses 9*scale px fonts)
 */
class TextBox(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val title: String?,
    val titleScale: Float,      // HC scale unit (nvgSize = 9f * titleScale)
    val lines: List<String>,
    val scale: Float,           // HC scale unit (nvgSize = 9f * scale)
    val spacer: Float,
    val defaultColor: Int = WHITE_RGBA,
) {
    private val slotCount   = lines.size + if (title != null) 1 else 0
    private val entryHeight = if (slotCount > 0) h / slotCount else h
    private val itemY       = entryHeight / 2f      // HC: itemY = entryHeight / 2
    private val centerX     = w / 2f

    private val titleSize = 9f * titleScale
    private val lineSize  = 9f * scale

    fun draw() {
        val font = NVGRenderer.defaultFont

        // Title — HC: centres horizontally, placed at row 0
        title?.let {
            val tw = NVGRenderer.textWidth(it.stripCodes(), titleSize, font)
            // HC: Text.text(title, box.x+centerX, box.y+entryHeight-itemY, ...)
            // with Alignment.MIDDLE → y - height/2
            // net y = box.y + entryHeight - itemY - titleSize/2
            //       = box.y + entryHeight - entryHeight/2 - titleSize/2
            //       = box.y + entryHeight/2 - titleSize/2
            formattedText(it, x + centerX - tw / 2f, y + entryHeight / 2f - titleSize / 2f, titleSize, defaultColor)
        }

        // Lines — HC: LEFT alignment, starts at row (1 if title, else 0)
        lines.forEachIndexed { i, line ->
            val row = i + if (title != null) 1 else 0
            // HC: val rowY = box.y + (entryHeight * (row + 1))
            //     draws at rowY - itemY with Alignment.MIDDLE
            //     net y = box.y + entryHeight*(row+1) - itemY - lineSize/2
            //           = box.y + entryHeight*(row+1) - entryHeight/2 - lineSize/2
            //           = box.y + entryHeight*row + entryHeight/2 - lineSize/2
            val rowY = y + entryHeight * row + entryHeight / 2f - lineSize / 2f
            formattedText(line, x + spacer, rowY, lineSize, defaultColor)
        }
    }
}

/** Convenience builder */
fun textBox(
    x: Float, y: Float, w: Float, h: Float,
    title: String? = null,
    titleScale: Float = 4f,
    lines: List<String> = emptyList(),
    scale: Float = 2.5f,
    spacer: Float = 10f,
    color: Int = Theme.textPrimary,
) = TextBox(x, y, w, h, title, titleScale, lines, scale, spacer, color)

/** fillText: shrinks font to fit width, like HC's Text.fillText */
fun fillText(text: String, cx: Float, cy: Float, maxWidth: Float, maxHeight: Float, color: Int) {
    val font = NVGRenderer.defaultFont
    val natural = NVGRenderer.textWidth(text.stripCodes(), maxHeight, font)
    val size = if (natural > maxWidth) maxHeight * (maxWidth / natural) else maxHeight
    val tw = NVGRenderer.textWidth(text.stripCodes(), size, font)
    formattedText(text, cx - tw / 2f, cy - size / 2f, size, color)
}