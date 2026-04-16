package com.odtheking.odinaddon.pvgui.dsl

import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.PAD
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.utils.Theme
import kotlin.math.floor

fun <T> dropDown(
    x: Float, y: Float, w: Float, h: Float,
    items: List<T>,
    default: T,
    spacer: Float = PAD,
    radius: Float = Theme.radius,
    label: (T) -> String = { it.toString() },
    dropDown: DropDownDsl<T>.() -> Unit = {},
): DropDownDsl<T> = DropDownDsl(x, y, w, h, default, items, spacer, radius, label).apply(dropDown)

/**
 * Port of HC's DropDownDSL.
 *
 * HC: selected item shown in main box.
 *     Options list (excluding selected) shown below when extended.
 *     Animation: EaseInOut on height scissor.
 *     isObjectHovered used for hover detection.
 *     brighter() on hover.
 *
 * We simplify the animation to a direct open/close (same behaviour as HC with animations=false)
 * but keep the structure identical so the UI looks the same.
 */
class DropDownDsl<T>(
    var x: Float, var y: Float, var w: Float, var h: Float,
    default: T,
    private val items: List<T>,
    private val spacer: Float,
    private val radius: Float,
    private val label: (T) -> String,
) {
    var selected: T = default
        private set
    var extended: Boolean = false
        private set

    private var onSelect:  (T) -> Unit = {}
    private var onExtend:  ()  -> Unit = {}

    fun onSelect(block: (T) -> Unit)  { onSelect = block }
    fun onExtend(block: () -> Unit)   { onExtend = block }

    // HC: trueOptions = options.without(selected)
    private val trueOptions: List<T> get() = items.filter { it != selected }

    // HC: dropDown height for each row = box.h
    private val rowH get() = h

    private fun rowY(i: Int) = y + h + (rowH + spacer) * i       // HC stacks below main box

    private fun isMainHovered() = PVState.isHovered(x, y, w, h)
    private fun isRowHovered(i: Int) = PVState.isHovered(x, rowY(i), w, rowH)

    fun draw() {
        val font = NVGRenderer.defaultFont

        // Main box — HC uses brighter on hover
        val mainHov = isMainHovered()
        NVGRenderer.rect(x, y, w, h,
            if (mainHov) brighten(Theme.btnNormal) else Theme.btnNormal, radius)

        // HC: Text.fillText(selectedText(selected), box.x+box.w/2, box.y+box.h/2, ...)
        fillText(label(selected), x + w / 2f, y + h / 2f,
            w - 2f * spacer, h - 2f * spacer, Theme.textPrimary)

        if (!extended) return

        // HC scissor to totalHeight of dropdown
        val totalH = h + (rowH + spacer) * trueOptions.size
        NVGRenderer.pushScissor(x, y, w, totalH)

        trueOptions.forEachIndexed { i, entry ->
            val ry = rowY(i)
            val hov = isRowHovered(i)
            NVGRenderer.rect(x, ry, w, rowH,
                if (hov) brighten(Theme.btnNormal) else Theme.btnNormal, radius)
            fillText(label(entry), x + w / 2f, ry + rowH / 2f,
                w - 2f * spacer, rowH - 2f * spacer, Theme.textPrimary)
        }

        NVGRenderer.popScissor()
    }

    fun click(mouseX: Double, mouseY: Double): Boolean {
        if (isMainHovered()) {
            extended = !extended
            onExtend()
            return true
        }
        if (extended) {
            trueOptions.forEachIndexed { i, entry ->
                if (isRowHovered(i)) {
                    selected = entry
                    extended = false
                    onSelect(entry)
                    return true
                }
            }
            // Click outside — close
            extended = false
        }
        return false
    }

    fun moveTo(nx: Float, ny: Float, nw: Float, nh: Float) {
        x = nx; y = ny; w = nw; h = nh
    }

    // Simple brightness bump (HC: color.brighter(1 + hover.percent()/500f), max ~1.2)
    private fun brighten(color: Int): Int {
        val r = ((color shr 16 and 0xFF) * 1.15f).toInt().coerceAtMost(255)
        val g = ((color shr  8 and 0xFF) * 1.15f).toInt().coerceAtMost(255)
        val b = ((color        and 0xFF) * 1.15f).toInt().coerceAtMost(255)
        val a =   color ushr 24
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}