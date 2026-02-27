package com.odtheking.odinaddon.pvgui.components

import com.odtheking.odinaddon.pvgui.DrawContext

abstract class Component(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
) {
    protected fun isHovered(ctx: DrawContext, mouseX: Double, mouseY: Double) =
        ctx.isHovered(mouseX, mouseY, x, y, w, h)

    abstract fun draw(ctx: DrawContext, mouseX: Double, mouseY: Double)
    open fun click(ctx: DrawContext, mouseX: Double, mouseY: Double): Boolean = false
}