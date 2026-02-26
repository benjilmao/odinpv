package com.odtheking.odinaddon.pvgui.utils

import com.odtheking.odin.utils.Color.Companion.brighter
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.ui.animations.EaseInOutAnimation
import com.odtheking.odinaddon.pvgui.DrawContext

class DropDown(
    val rowH: Float = 36f,
    private val rowGap: Float = 3f,
    private val textSize: Float = 26f,
    private val iconScale: Float = 0.65f,
    private val padding: Float = 10f,
) {
    private var open = false
    private val anim = EaseInOutAnimation(180)

    val isOpen get() = open

    fun reset() { open = false }
    fun toggle() { open = !open; anim.start() }
    fun close() { if (!open) return; open = false; anim.start() }

    fun draw(
        ctx: DrawContext,
        x: Float, y: Float, w: Float,
        label: String, icon: Pair<String, String>?,
        entries: List<Triple<String, Pair<String, String>?, Boolean>>,
        mouseX: Double, mouseY: Double,
    ) {
        val hovered = ctx.isHovered(mouseX, mouseY, x, y, w, rowH)
        ctx.rect(x, y, w, rowH, if (open || hovered) Theme.btnHover else Theme.btnNormal, Theme.round)
        drawLabelWithIcon(ctx, label, icon, x + padding / 2f, y)
        val chevron = if (open) "↑" else "↓"
        val cw = ctx.textWidth(chevron, textSize)
        ctx.text(chevron, x + w - cw - padding / 2f, y + (rowH - textSize) / 2f, textSize, Colors.WHITE)

        if (!open && !anim.isAnimating()) return
        val animH = anim.get(0f, (rowH + rowGap) * entries.size, !open)
        ctx.overlayText.add {
            ctx.pushScissor(x, y + rowH, w, animH)
            ctx.rect(x, y + rowH, w, animH, Theme.bg.brighter(1.8f), Theme.round)
            entries.forEachIndexed { i, (entryLabel, entryIcon, selected) ->
                val ey = y + rowH + rowGap + i * (rowH + rowGap)
                val entryHovered = ctx.isHovered(mouseX, mouseY, x, ey, w, rowH)
                ctx.rect(x, ey, w, rowH, when {
                    selected     -> Theme.accent
                    entryHovered -> Theme.btnHover
                    else         -> Theme.btnNormal
                }, Theme.round)
                drawLabelWithIcon(ctx, entryLabel, entryIcon, x + padding / 2f, ey)
            }
            ctx.popScissor()
        }
    }

    private fun drawLabelWithIcon(ctx: DrawContext, label: String, icon: Pair<String, String>?, x: Float, rowY: Float) {
        val labelW = ctx.formattedText(label, x, rowY + (rowH - textSize) / 2f, textSize)
        if (icon != null) {
            val iSize = textSize * iconScale
            ctx.formattedText(icon.first + icon.second, x + labelW + 3f, rowY + (rowH - iSize) / 2f, iSize)
        }
    }

    fun isClickOnButton(ctx: DrawContext, mouseX: Double, mouseY: Double, x: Float, y: Float, w: Float) =
        ctx.isHovered(mouseX, mouseY, x, y, w, rowH)

    fun indexAtClick(ctx: DrawContext, mouseX: Double, mouseY: Double, x: Float, y: Float, w: Float): Int {
        if (!open) return -1
        for (i in 0..99) {
            val ey = y + rowH + rowGap + i * (rowH + rowGap)
            if (ctx.isHovered(mouseX, mouseY, x, ey, w, rowH)) return i
        }
        return -1
    }
}