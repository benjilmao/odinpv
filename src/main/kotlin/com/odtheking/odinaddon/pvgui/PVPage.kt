package com.odtheking.odinaddon.pvgui

import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import net.minecraft.client.gui.GuiGraphics

abstract class PVPage {
    abstract val name: String
    var x = 0f
    var y = 0f
    var w = 0f
    var h = 0f

    fun setBounds(x: Float, y: Float, w: Float, h: Float) {
        this.x = x; this.y = y; this.w = w; this.h = h
    }

    abstract fun draw(context: GuiGraphics, mouseX: Int, mouseY: Int)

    open fun enqueueItems(context: GuiGraphics, mouseX: Int, mouseY: Int) {}

    open fun click(mouseX: Double, mouseY: Double): Boolean = false

    open fun onOpen() {}

    protected fun centeredText(message: String, color: Int, size: Float = 26f) {
        val font = NVGRenderer.defaultFont
        val textWidth = NVGRenderer.textWidth(message, size, font)
        NVGRenderer.text(message, x + (w - textWidth) / 2f, y + h / 2f - size / 2f, size, color, font)
    }
}