package com.odtheking.odinaddon.pvgui.dsl

import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.PVState
import com.odtheking.odinaddon.pvgui.utils.Theme

private const val ANIM_MS = 180f

fun <T> dropDown(
    x: Float, y: Float, w: Float, h: Float,
    items: List<T>,
    label: (T) -> String,
    sublabel: ((T) -> String?)? = null,
    onSelect: DropDownDsl<T>.(T) -> Unit = {},
    onExtend: DropDownDsl<T>.(Boolean) -> Unit = {},
): DropDownDsl<T> = DropDownDsl(x, y, w, h, items, label, sublabel).also { dsl ->
    dsl.onSelectBlock = { item -> onSelect(dsl, item) }
    dsl.onExtendBlock = { open -> onExtend(dsl, open) }
}

class DropDownDsl<T>(
    x: Float, y: Float, w: Float, h: Float,
    items: List<T>,
    private val label: (T) -> String,
    @Suppress("UNUSED_PARAMETER") private val sublabel: ((T) -> String?)? = null,
) {
    var x = x; private set
    var y = y; private set
    var w = w; private set
    var h = h; private set

    fun moveTo(nx: Float, ny: Float, nw: Float, nh: Float) { x = nx; y = ny; w = nw; h = nh }

    var items: List<T> = items
        set(value) { field = value; if (selected != null && selected !in value) selected = value.firstOrNull() }

    var selected: T? = items.firstOrNull()
    var isOpen = false; private set

    private var openAnim   = 0f
    private var lastTimeMs = 0L

    internal var onSelectBlock: (T) -> Unit = {}
    internal var onExtendBlock: (Boolean) -> Unit = {}

    private val rowH get() = h
    private fun rowY(i: Int) = y + h + i * rowH

    fun draw() {
        val now = System.currentTimeMillis()
        val dt = if (lastTimeMs == 0L) 0f else (now - lastTimeMs).toFloat()
        lastTimeMs = now
        openAnim = if (isOpen) (openAnim + dt / ANIM_MS).coerceAtMost(1f)
                   else        (openAnim - dt / ANIM_MS).coerceAtLeast(0f)

        val font = NVGRenderer.defaultFont
        val headerHov = !isOpen && PVState.isHovered(x, y, w, h)
        val r = Theme.radius

        val headerCol = when { isOpen -> Theme.bg; headerHov -> Theme.btnHover; else -> Theme.btnNormal }
        NVGRenderer.rect(x, y, w, h, headerCol, r)
        NVGRenderer.rect(x, y + h - 1f, w, 1f, Theme.separator)

        val mainText = selected?.let { label(it) }?.stripCodes() ?: "—"
        NVGRenderer.text(mainText, x + 10f, y + (h - 14f) / 2f, 14f, Theme.textPrimary, font)
        drawArrow(x + w - 18f, y + h / 2f, openAnim)

        if (openAnim > 0f) {
            val listH = items.size * rowH * openAnim
            NVGRenderer.pushScissor(x, y + h, w, listH)
            items.forEachIndexed { i, item ->
                val ry = rowY(i)
                val rowHov = PVState.isHovered(x, ry, w, rowH)
                val rowSel = item == selected
                val isLast = i == items.lastIndex
                val rowCol = when { rowSel -> Theme.btnSelected; rowHov -> Theme.btnHover; else -> Theme.bg }
                NVGRenderer.rect(x, ry, w, rowH, rowCol, if (isLast && openAnim > 0.95f) r else 0f)
                NVGRenderer.text(label(item).stripCodes(), x + 10f, ry + (rowH - 14f) / 2f, 14f, Theme.textPrimary, font)
            }
            NVGRenderer.popScissor()
        }
    }

    fun click(mouseX: Double, mouseY: Double): Boolean {
        if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) { setOpen(!isOpen); return true }
        if (isOpen) {
            items.forEachIndexed { i, item ->
                val ry = rowY(i)
                if (mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + rowH) {
                    if (item != selected) { selected = item; onSelectBlock(item) }
                    setOpen(false); return true
                }
            }
            setOpen(false)
        }
        return false
    }

    fun setOpen(open: Boolean) { if (open == isOpen) return; isOpen = open; onExtendBlock(open) }

    private fun String.stripCodes() = replace(Regex("§."), "")

    private fun drawArrow(cx: Float, cy: Float, t: Float) {
        val hs = 5f; val vs = 3f; val dy = vs * (1f - 2f * t)
        NVGRenderer.line(cx - hs, cy - dy, cx, cy + dy, 1.5f, Theme.textSecondary)
        NVGRenderer.line(cx, cy + dy, cx + hs, cy - dy, 1.5f, Theme.textSecondary)
    }
}
