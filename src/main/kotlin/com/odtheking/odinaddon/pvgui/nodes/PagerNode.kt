package com.odtheking.odinaddon.pvgui.nodes

import com.odtheking.odinaddon.pvgui.dsl.ButtonsDsl
import com.odtheking.odinaddon.pvgui.dsl.buttons
import com.odtheking.odinaddon.pvgui.utils.Theme
import net.minecraft.client.gui.GuiGraphics

class PagerNode<T>(
    private val pageItems: () -> List<T>,
    private val pageSize: Int,
    private val spacing: Float = 10f,
    private val labelProvider: ((Int) -> String) = { (it + 1).toString() },
    private val content: PagerNode<T>.(pageIndex: Int, pageItems: List<T>, x: Float, y: Float, w: Float, h: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) -> Unit,
    private val onEnqueueItems: (PagerNode<T>.(pageIndex: Int, pageItems: List<T>, x: Float, y: Float, w: Float, h: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) -> Unit)? = null,
    private val onContentClick: (PagerNode<T>.(pageIndex: Int, pageItems: List<T>, mouseX: Double, mouseY: Double, x: Float, y: Float, w: Float, h: Float) -> Boolean)? = null,
) {
    private var buttons: ButtonsDsl<Int>? = null
    private var lastPageCount = -1

    var currentPage: Int = 0
        private set

    private fun pageCount() = maxOf(1, (pageItems().size + pageSize - 1) / pageSize)

    fun pageSubset(page: Int): List<T> {
        val list = pageItems()
        val start = page * pageSize
        return if (start >= list.size) emptyList()
        else list.subList(start, minOf(start + pageSize, list.size))
    }

    private fun buttonHeight(totalWidth: Float): Float = (totalWidth - spacing * 16f) / 18f

    private fun contentArea(x: Float, y: Float, w: Float, h: Float): Triple<Float, Float, Float> {
        val pageCount = pageCount()
        return if (pageCount > 1) {
            val btnHeight = buttonHeight(w)
            Triple(y + btnHeight + spacing, h - btnHeight - spacing, btnHeight)
        } else {
            Triple(y, h, 0f)
        }
    }

    private fun getOrBuildButtons(pageCount: Int, x: Float, y: Float, w: Float, h: Float): ButtonsDsl<Int> {
        val existing = buttons
        if (existing != null && existing.items.size == pageCount) {
            existing.x = x; existing.y = y; existing.w = w; existing.h = h
            return existing
        }
        currentPage = 0
        return buttons(
            x = x, y = y, w = w, h = h,
            items = (0 until pageCount).toList(),
            vertical = false,
            spacing = spacing / 2f,
            textSize = 14f,
            radius = Theme.radius,
            label = { labelProvider(it) },
        ) { currentPage = it }.also { it.selected = 0; buttons = it }
    }

    fun draw(x: Float, y: Float, w: Float, h: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val pageCount = pageCount()
        if (pageCount != lastPageCount) { buttons = null; lastPageCount = pageCount }
        val (contentY, contentH, btnHeight) = contentArea(x, y, w, h)
        if (pageCount > 1) getOrBuildButtons(pageCount, x, y, w, btnHeight).draw()
        content(currentPage, pageSubset(currentPage), x, contentY, w, contentH, context, mouseX, mouseY)
    }

    fun enqueueItems(x: Float, y: Float, w: Float, h: Float, context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val (contentY, contentH, _) = contentArea(x, y, w, h)
        onEnqueueItems?.invoke(this, currentPage, pageSubset(currentPage), x, contentY, w, contentH, context, mouseX, mouseY)
    }

    fun click(mouseX: Double, mouseY: Double, x: Float, y: Float, w: Float, h: Float): Boolean {
        val pageCount = pageCount()
        if (pageCount > 1) {
            val btnHeight = buttonHeight(w)
            if (getOrBuildButtons(pageCount, x, y, w, btnHeight).click(mouseX, mouseY)) return true
        }
        val (contentY, contentH, _) = contentArea(x, y, w, h)
        return onContentClick?.invoke(this, currentPage, pageSubset(currentPage), mouseX, mouseY, x, contentY, w, contentH) ?: false
    }

    fun reset() {
        buttons = null
        lastPageCount = -1
        currentPage = 0
    }
}