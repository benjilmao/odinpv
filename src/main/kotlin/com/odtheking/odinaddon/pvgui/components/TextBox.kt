package com.odtheking.odinaddon.pvgui.components

import com.odtheking.odinaddon.pvgui.DrawContext

class TextBox(
    x: Float, y: Float, w: Float, h: Float,
    private val title: String?,
    private val titleScale: Float,
    private val lines: List<String>,
    private val lineScale: Float,
    private val spacer: Float = 0f,
) : Component(x, y, w, h) {

    private val totalSlots  get() = lines.size + if (title != null) 1 else 0
    private val entryHeight get() = if (totalSlots > 0) h / totalSlots else h

    override fun draw(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        val slotH = entryHeight
        val itemY = slotH / 2f

        title?.let {
            val tw = ctx.formattedTextWidth(it, titleScale)
            ctx.formattedText(it, x + (w - tw) / 2f, y + slotH - itemY - titleScale / 2f, titleScale)
        }

        lines.forEachIndexed { i, line ->
            val slot = i + if (title != null) 1 else 0
            val ly = y + slotH * slot + itemY - lineScale / 2f
            ctx.formattedText(line, x + spacer, ly, lineScale)
        }
    }
}