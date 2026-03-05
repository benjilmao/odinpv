package com.odtheking.odinaddon.pvgui

import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.dsl.EntityQueue
import com.odtheking.odinaddon.pvgui.dsl.ItemQueue

abstract class PVPage {
    abstract val name: String

    var x = 0f; var y = 0f; var w = 0f; var h = 0f

    fun setBounds(x: Float, y: Float, w: Float, h: Float) {
        this.x = x; this.y = y; this.w = w; this.h = h
    }

    internal val capturedItems    = mutableListOf<ItemQueue.Entry>()
    internal val capturedEntities = mutableListOf<EntityQueue.Entry>()

    open fun draw() {}

    /** Called by PVScreen after NVG pass — replays both queues into the pending lists */
    fun replayQueues() {
        capturedItems.forEach    { ItemQueue.addEntry(it) }
        capturedEntities.forEach { EntityQueue.addEntry(it) }
    }

    open fun click(mouseX: Double, mouseY: Double): Boolean = false
    open fun onOpen() {}

    protected fun centeredText(msg: String, color: Int, size: Float = 26f) {
        val font = NVGRenderer.defaultFont
        val tw   = NVGRenderer.textWidth(msg, size, font)
        NVGRenderer.text(msg, x + (w - tw) / 2f, y + h / 2f - size / 2f, size, color, font)
    }
}
