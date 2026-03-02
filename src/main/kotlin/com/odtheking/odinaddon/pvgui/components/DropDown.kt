package com.odtheking.odinaddon.pvgui.components

import com.odtheking.odin.utils.ui.animations.EaseInOutAnimation
import com.odtheking.odinaddon.pvgui.core.Component
import com.odtheking.odinaddon.pvgui.core.RenderContext
import com.odtheking.odinaddon.pvgui.core.Renderer
import com.odtheking.odinaddon.pvgui.utils.Theme

class DropDown(
    private val rowH: Float = 32f,
    private val rowGap: Float = 3f,
    private val textSize: Float = 15f,
    private val pad: Float = 8f,
) : Component() {

    private var open = false
    private val anim = EaseInOutAnimation(180)
    private var onSelect: ((Int) -> Unit)? = null
    private var entries: List<Triple<String, String?, Boolean>> = emptyList()

    fun onSelect(block: (Int) -> Unit) { onSelect = block }
    fun reset() { open = false }
    val isOpen get() = open

    fun draw(ctx: RenderContext, label: String, icon: String?, newEntries: List<Triple<String, String?, Boolean>>) {
        ctx.register(this)
        entries = newEntries

        Renderer.rect(x, y, w, rowH,
            if (open || ctx.isHovered(x, y, w, rowH)) Theme.btnHover else Theme.btnNormal,
            Theme.radius)

        drawEntry(label, icon, x + pad, y)

        val chevron = if (open) "▲" else "▼"
        val cw = Renderer.textWidth(chevron, textSize * 0.75f)
        Renderer.text(chevron, x + w - cw - pad, y + (rowH - textSize * 0.75f) / 2f, textSize * 0.75f)

        if (open || anim.isAnimating()) {
            val animH = anim.get(0f, (rowH + rowGap) * entries.size, !open)
            ctx.overlayText.add {
                Renderer.pushScissor(x, y + rowH, w, animH)
                Renderer.rect(x, y + rowH, w, animH, Theme.bg, Theme.radius)
                entries.forEachIndexed { i, (entryLabel, entryIcon, selected) ->
                    val ey = y + rowH + rowGap + i * (rowH + rowGap)
                    Renderer.rect(x, ey, w, rowH, when {
                        selected                    -> Theme.accent
                        ctx.isHovered(x, ey, w, rowH) -> Theme.btnHover
                        else                        -> Theme.btnNormal
                    }, Theme.radius)
                    drawEntry(entryLabel, entryIcon, x + pad, ey)
                }
                Renderer.popScissor()
            }
        }
    }

    override fun draw(ctx: RenderContext) {}

    override fun click(ctx: RenderContext, mouseX: Double, mouseY: Double): Boolean {
        if (ctx.isHovered(x, y, w, rowH)) {
            open = !open
            anim.start()
            return true
        }
        if (open) {
            entries.forEachIndexed { i, _ ->
                val ey = y + rowH + rowGap + i * (rowH + rowGap)
                if (ctx.isHovered(x, ey, w, rowH)) {
                    onSelect?.invoke(i)
                    open = false
                    anim.start()
                    return true
                }
            }
            open = false
            anim.start()
        }
        return false
    }

    private fun drawEntry(label: String, icon: String?, lx: Float, rowY: Float) {
        val lw = Renderer.formattedTextWidth(label, textSize)
        Renderer.formattedText(label, lx, rowY + (rowH - textSize) / 2f, textSize)
        if (icon != null) Renderer.formattedText(icon, lx + lw + 3f, rowY + (rowH - textSize) / 2f, textSize)
    }
}
