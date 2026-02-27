package com.odtheking.odinaddon.pvgui.components

import com.odtheking.odin.utils.Color.Companion.brighter
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.ui.animations.EaseInOutAnimation
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.utils.Theme

class DropDown(
    private val rowH: Float = 36f,
    private val rowGap: Float = 3f,
    private val textSize: Float = 26f,
    private val iconScale: Float = 0.65f,
    private val padding: Float = 10f,
    init: DropDown.() -> Unit = {},
) : Component(0f, 0f, 0f, 0f) {

    private var drawX = 0f
    private var drawY = 0f
    private var drawW = 0f

    private var open = false
    private val anim = EaseInOutAnimation(180)
    private var onSelect: (index: Int) -> Unit = {}
    private var lastEntries: List<Triple<String, Pair<String, String>?, Boolean>> = emptyList()

    fun onSelect(block: (index: Int) -> Unit) { onSelect = block }
    fun reset() { open = false }
    val isOpen get() = open
    val rowHeight get() = rowH

    init { init() }

    fun draw(
        ctx: DrawContext,
        mouseX: Double, mouseY: Double,
        atX: Float, atY: Float, atW: Float,
        label: String,
        icon: Pair<String, String>?,
        entries: List<Triple<String, Pair<String, String>?, Boolean>>,
    ) {
        drawX = atX
        drawY = atY
        drawW = atW
        lastEntries = entries

        val hovered = ctx.isHovered(mouseX, mouseY, drawX, drawY, drawW, rowH)
        ctx.rect(drawX, drawY, drawW, rowH, if (open || hovered) Theme.btnHover else Theme.btnNormal, Theme.round)
        drawLabelWithIcon(ctx, label, icon, drawX + padding / 2f, drawY)
        val chevron = if (open) "↑" else "↓"
        val cw = ctx.textWidth(chevron, textSize)
        ctx.text(chevron, drawX + drawW - cw - padding / 2f, drawY + (rowH - textSize) / 2f, textSize, Colors.WHITE)

        if (!open && !anim.isAnimating()) return
        val animH = anim.get(0f, (rowH + rowGap) * entries.size, !open)
        ctx.overlayText.add {
            ctx.pushScissor(drawX, drawY + rowH, drawW, animH)
            ctx.rect(drawX, drawY + rowH, drawW, animH, Theme.bg.brighter(1.8f), Theme.round)
            entries.forEachIndexed { i, (entryLabel, entryIcon, selected) ->
                val ey = drawY + rowH + rowGap + i * (rowH + rowGap)
                val entryHovered = ctx.isHovered(mouseX, mouseY, drawX, ey, drawW, rowH)
                ctx.rect(drawX, ey, drawW, rowH, when {
                    selected -> Theme.accent
                    entryHovered -> Theme.btnHover
                    else -> Theme.btnNormal
                }, Theme.round)
                drawLabelWithIcon(ctx, entryLabel, entryIcon, drawX + padding / 2f, ey)
            }
            ctx.popScissor()
        }
    }

    override fun draw(ctx: DrawContext, mouseX: Double, mouseY: Double) {}

    override fun click(ctx: DrawContext, mouseX: Double, mouseY: Double): Boolean {
        if (ctx.isHovered(mouseX, mouseY, drawX, drawY, drawW, rowH)) {
            open = !open; anim.start()
            return true
        }
        if (open) {
            lastEntries.forEachIndexed { i, _ ->
                val ey = drawY + rowH + rowGap + i * (rowH + rowGap)
                if (ctx.isHovered(mouseX, mouseY, drawX, ey, drawW, rowH)) {
                    onSelect(i)
                    open = false; anim.start()
                    return true
                }
            }
            open = false; anim.start()
        }
        return false
    }

    private fun drawLabelWithIcon(ctx: DrawContext, label: String, icon: Pair<String, String>?, lx: Float, rowY: Float) {
        val labelW = ctx.formattedText(label, lx, rowY + (rowH - textSize) / 2f, textSize)
        if (icon != null) {
            val iSize = textSize * iconScale
            ctx.formattedText(icon.first + icon.second, lx + labelW + 3f, rowY + (rowH - iSize) / 2f, iSize)
        }
    }
}