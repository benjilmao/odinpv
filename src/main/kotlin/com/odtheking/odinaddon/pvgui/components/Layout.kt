package com.odtheking.odinaddon.pvgui.components

import com.odtheking.odinaddon.pvgui.DrawContext

sealed class Size {
    data class Fixed(val px: Float) : Size()
    data class Percent(val fraction: Float) : Size()
    object Remaining : Size()
}

fun Float.px() = Size.Fixed(this)
fun Float.percent() = Size.Percent(this / 100f)
fun remaining() = Size.Remaining

class Row(
    x: Float, y: Float, w: Float, h: Float,
    private val spacing: Float = 0f,
    init: Row.() -> Unit = {},
) : Component(x, y, w, h) {

    private data class Child(val size: Size, val build: (x: Float, y: Float, w: Float, h: Float) -> Component?)
    private val children = mutableListOf<Child>()

    fun child(size: Size, build: (x: Float, y: Float, w: Float, h: Float) -> Component?) {
        children.add(Child(size, build))
    }

    init { init() }

    private fun resolvedWidths(): List<Float> {
        val totalSpacing = spacing * (children.size - 1).coerceAtLeast(0)
        val available = w - totalSpacing
        val fixedTotal = children.sumOf { c ->
            when (val s = c.size) {
                is Size.Fixed     -> s.px.toDouble()
                is Size.Percent   -> (s.fraction * available).toDouble()
                is Size.Remaining -> 0.0
            }
        }
        val remaining = (available - fixedTotal.toFloat()).coerceAtLeast(0f)
        val remainingCount = children.count { it.size is Size.Remaining }.coerceAtLeast(1)
        return children.map<Child, Float> { c ->
            when (val s = c.size) {
                is Size.Fixed     -> s.px
                is Size.Percent   -> s.fraction * available
                is Size.Remaining -> remaining / remainingCount
            }
        }
    }

    override fun draw(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        val widths = resolvedWidths()
        var curX = x
        children.forEachIndexed { i, child ->
            child.build(curX, y, widths[i], h)?.draw(ctx, mouseX, mouseY)
            curX += widths[i] + spacing
        }
    }

    override fun click(ctx: DrawContext, mouseX: Double, mouseY: Double): Boolean {
        val widths = resolvedWidths()
        var curX = x
        for ((i, child) in children.withIndex()) {
            val comp = child.build(curX, y, widths[i], h)
            if (comp?.click(ctx, mouseX, mouseY) == true) return true
            curX += widths[i] + spacing
        }
        return false
    }
}

class Column(
    x: Float, y: Float, w: Float, h: Float,
    private val spacing: Float = 0f,
    init: Column.() -> Unit = {},
) : Component(x, y, w, h) {

    private data class Child(val size: Size, val build: (x: Float, y: Float, w: Float, h: Float) -> Component?)
    private val children = mutableListOf<Child>()

    fun child(size: Size, build: (x: Float, y: Float, w: Float, h: Float) -> Component?) {
        children.add(Child(size, build))
    }

    init { init() }

    private fun resolvedHeights(): List<Float> {
        val totalSpacing = spacing * (children.size - 1).coerceAtLeast(0)
        val available = h - totalSpacing
        val fixedTotal = children.sumOf { c ->
            when (val s = c.size) {
                is Size.Fixed     -> s.px.toDouble()
                is Size.Percent   -> (s.fraction * available).toDouble()
                is Size.Remaining -> 0.0
            }
        }
        val remaining = (available - fixedTotal.toFloat()).coerceAtLeast(0f)
        val remainingCount = children.count { it.size is Size.Remaining }.coerceAtLeast(1)
        return children.map<Child, Float> { c ->
            when (val s = c.size) {
                is Size.Fixed     -> s.px
                is Size.Percent   -> s.fraction * available
                is Size.Remaining -> remaining / remainingCount
            }
        }
    }

    override fun draw(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        val heights = resolvedHeights()
        var curY = y
        children.forEachIndexed { i, child ->
            child.build(x, curY, w, heights[i])?.draw(ctx, mouseX, mouseY)
            curY += heights[i] + spacing
        }
    }

    override fun click(ctx: DrawContext, mouseX: Double, mouseY: Double): Boolean {
        val heights = resolvedHeights()
        var curY = y
        for ((i, child) in children.withIndex()) {
            val comp = child.build(x, curY, w, heights[i])
            if (comp?.click(ctx, mouseX, mouseY) == true) return true
            curY += heights[i] + spacing
        }
        return false
    }
}