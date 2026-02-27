package com.odtheking.odinaddon.pvgui.utils

import com.odtheking.odinaddon.pvgui.DrawContext

class TextBox(
    private val ctx: DrawContext,
    private val x: Float,
    private val y: Float,
    private val w: Float,
    private val h: Float,
    private val title: String?,
    private val titleScale: Float,
    private val lines: List<String>,
    private val scale: Float,
    private val spacer: Float = 0f,
) {
    private val totalSlots  = lines.size + if (title != null) 1 else 0
    private val entryHeight = if (totalSlots > 0) h / totalSlots else h
    private val itemY = entryHeight / 2f
    private val centerX = x + w / 2f

    fun draw() {
        title?.let {
            val tw = ctx.formattedTextWidth(it, titleScale)
            ctx.formattedText(it, centerX - tw / 2f, y + entryHeight - itemY - titleScale / 2f, titleScale)
        }

        lines.forEachIndexed { i, line ->
            val slot = i + if (title != null) 1 else 0
            val ly = y + entryHeight * slot + itemY - scale / 2f
            ctx.formattedText(line, x + spacer, ly, scale)
        }
    }
}