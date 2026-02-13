package com.odtheking.odinaddon.pvgui.utils

data class Box(val x: Float, val y: Float, val w: Float, val h: Float) {
    constructor(x: Number, y: Number, w: Number, h: Number) :
            this(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())

    val centerX: Float get() = x + w / 2
    val centerY: Float get() = y + h / 2
    val right: Float get() = x + w
    val bottom: Float get() = y + h
}