package com.odtheking.odinaddon.pvgui2

import com.odtheking.odin.OdinMod.mc
import me.owdding.lib.displays.Display
import net.minecraft.client.gui.GuiGraphics
import tech.thatgravyboat.skyblockapi.helpers.McFont
import tech.thatgravyboat.skyblockapi.utils.text.Text

fun textList(lines: List<String>, width: Int, height: Int, scale: Float = 1f): Display = object : Display {
    private val autoScale = run {
        val lineCount = lines.size.coerceAtLeast(1)
        val guiScale = mc.window.guiScale.toFloat()
        val maxByHeight = (height.toFloat() * guiScale) / (lineCount * (McFont.height + 2))
        val maxByWidth = (width.toFloat() * guiScale) / (lines.maxOfOrNull { McFont.width(Text.of(it)) }?.coerceAtLeast(1) ?: 1)
        minOf(scale * guiScale, maxByHeight, maxByWidth) / guiScale
    }

    override fun getWidth() = width
    override fun getHeight() = height
    override fun render(graphics: GuiGraphics) {
        if (lines.isEmpty()) return
        val lineSpacing = height / lines.size
        lines.forEachIndexed { i, line ->
            val y = i * lineSpacing + (lineSpacing - (McFont.height * autoScale).toInt()) / 2
            graphics.pose().pushMatrix()
            graphics.pose().translate(0f, y.toFloat())
            graphics.pose().scale(autoScale, autoScale)
            graphics.drawString(McFont.self, Text.of(line), 0, 0, -1, true)
            graphics.pose().popMatrix()
        }
    }
}

fun separator(width: Int): Display = object : Display {
    override fun getWidth() = width
    override fun getHeight() = 1
    override fun render(graphics: GuiGraphics) {
        graphics.fill(0, 0, width, 1, 0x55FFFFFF)
    }
}