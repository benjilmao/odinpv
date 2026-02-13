package com.odtheking.odinaddon.pvgui.utils

import com.odtheking.odin.utils.Colors

class TextBox(
    val box: Box,
    val title: String? = null,
    val titleScale: Float = 4f,
    val text: List<String>,
    val textScale: Float = 2.5f,
    val spacer: Float = 10f,
    val defaultColor: Int = Colors.WHITE.rgba
) {
    private val entryHeight = box.h / (text.size + if (title != null) 1 else 0)
    private val itemY = entryHeight / 2

    fun draw() {
        title?.let {
            Text.drawColored(
                text = it,
                x = box.x + box.w / 2,
                y = box.y + entryHeight - itemY,
                height = 9 * titleScale,
                defaultColor = Colors.WHITE,
                centering = Text.Centering.CENTER,
                alignment = Text.Alignment.MIDDLE
            )
        }

        text.forEachIndexed { i, line ->
            val y = box.y + (entryHeight * (i + if (title != null) 2 else 1))
            Text.drawColored(
                text = line,
                x = box.x,
                y = y - itemY,
                height = 9 * textScale,
                defaultColor = Colors.WHITE,
                centering = Text.Centering.LEFT,
                alignment = Text.Alignment.MIDDLE
            )
        }
    }
}