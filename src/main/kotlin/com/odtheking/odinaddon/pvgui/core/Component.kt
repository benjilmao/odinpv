package com.odtheking.odinaddon.pvgui.core

abstract class Component {
    var x: Float = 0f
    var y: Float = 0f
    var w: Float = 0f
    var h: Float = 0f

    abstract fun draw(ctx: RenderContext)
    open fun click(ctx: RenderContext, mouseX: Double, mouseY: Double): Boolean = false
    open fun scroll(ctx: RenderContext, delta: Double): Boolean = false

    protected fun isHovered(ctx: RenderContext) = ctx.isHovered(x, y, w, h)

    fun setBounds(x: Float, y: Float, w: Float, h: Float) {
        if (this.x == x && this.y == y && this.w == w && this.h == h) return
        this.x = x; this.y = y; this.w = w; this.h = h
        onBoundsChanged()
    }

    open fun onBoundsChanged() {}
}
