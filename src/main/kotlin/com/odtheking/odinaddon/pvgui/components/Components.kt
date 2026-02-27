package com.odtheking.odinaddon.pvgui.components

import com.odtheking.odin.utils.Color
import com.odtheking.odinaddon.pvgui.DrawContext
import com.odtheking.odinaddon.pvgui.utils.Theme
import net.minecraft.world.item.ItemStack

class ItemSlot(
    x: Float, y: Float, size: Float,
    private val item: ItemStack,
    private val bgColor: Color = Color(255, 255, 255, 0.08f),
    private val radius: Float = Theme.round,
    private val padding: Float = 0.05f, // fraction of size
) : Component(x, y, size, size) {

    override fun draw(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        if (item.isEmpty) return
        ctx.rect(x, y, w, h, bgColor, radius)
        val pad = w * padding
        ctx.item(item, x + pad, y + pad, w - pad * 2f)
    }
}

class ProgressBar(
    x: Float, y: Float, w: Float, h: Float,
    private val progress: Float,
    private val fillColor: Color = Color(80, 160, 255),
    private val bgColor: Color = Color(40, 40, 40),
    private val radius: Float = 3f,
    ) : Component(x, y, w, h) {

    override fun draw(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        ctx.rect(x, y, w, h, bgColor, radius)
        ctx.rect(x, y, (w * progress.coerceIn(0f, 1f)), h, fillColor, radius)
    }
}

class Separator(
    x: Float, y: Float, w: Float, h: Float,
    private val color: Color = Theme.separator,
) : Component(x, y, w, h) {

    override fun draw(ctx: DrawContext, mouseX: Double, mouseY: Double) {
        if (w >= h) ctx.line(x, y + h / 2f, x + w, y + h / 2f, h, color)
        else        ctx.line(x + w / 2f, y, x + w / 2f, y + h, w, color)
    }
}