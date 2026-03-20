package com.odtheking.odinaddon.pvgui.nodes

import com.odtheking.odinaddon.pvgui.dsl.ButtonsDsl
import com.odtheking.odinaddon.pvgui.dsl.buttons
import com.odtheking.odinaddon.pvgui.utils.Theme
import net.minecraft.client.gui.GuiGraphics

class TabNode(
    private val tabs: List<String>,
    private val spacing: Float = 10f,
    private val content: (tab: String, x: Float, y: Float, w: Float, h: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) -> Unit,
    private val onEnqueueItems: ((tab: String, x: Float, y: Float, w: Float, h: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) -> Unit)? = null,
    private val onContentClick: ((tab: String, mouseX: Double, mouseY: Double, x: Float, y: Float, w: Float, h: Float) -> Boolean)? = null,
) {
    private var buttons: ButtonsDsl<String>? = null

    val currentTab: String get() = buttons?.selected ?: tabs.first()

    private fun tabHeight(totalHeight: Float): Float = ((totalHeight * 0.9f) - spacing * 5f) / 6f

    private fun getOrBuildButtons(x: Float, y: Float, w: Float, h: Float): ButtonsDsl<String> {
        val existing = buttons
        if (existing != null) {
            existing.x = x; existing.y = y; existing.w = w; existing.h = h
            return existing
        }
        return buttons(
            x = x, y = y, w = w, h = h,
            items = tabs,
            vertical = false,
            spacing = spacing,
            textSize = 15f,
            radius = Theme.radius,
            label = { it },
        ) { buttons?.selected = it }.also { it.selected = tabs.first(); buttons = it }
    }

    fun draw(x: Float, y: Float, w: Float, h: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val tabH = tabHeight(h)
        getOrBuildButtons(x, y, w, tabH).draw()
        val contentY = y + tabH + spacing
        val contentH = h - tabH - spacing
        content(currentTab, x, contentY, w, contentH, context, mouseX, mouseY)
    }

    fun enqueueItems(x: Float, y: Float, w: Float, h: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val tabH = tabHeight(h)
        val contentY = y + tabH + spacing
        val contentH = h - tabH - spacing
        onEnqueueItems?.invoke(currentTab, x, contentY, w, contentH, context, mouseX, mouseY)
    }

    fun click(mouseX: Double, mouseY: Double, x: Float, y: Float, w: Float, h: Float): Boolean {
        val tabH = tabHeight(h)
        if (getOrBuildButtons(x, y, w, tabH).click(mouseX, mouseY)) return true
        val contentY = y + tabH + spacing
        val contentH = h - tabH - spacing
        return onContentClick?.invoke(currentTab, mouseX, mouseY, x, contentY, w, contentH) ?: false
    }

    fun reset() {
        buttons = null
    }
}