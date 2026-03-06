package com.odtheking.odinaddon.pvgui.dsl

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.PVState
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

object ItemQueue {

    data class Entry(
        val stack: ItemStack,
        val x: Float, val y: Float, val size: Float,
        val showTooltip: Boolean = true,
        val customCount: String? = null,
        val scissor: FloatArray? = null,
    )

    val pending = mutableListOf<Entry>()
    var captureTarget: MutableList<Entry>? = null

    fun queue(
        stack: ItemStack,
        x: Float, y: Float, size: Float,
        showTooltip: Boolean = true,
        customCount: String? = null,
        scissor: FloatArray? = null,
    ) {
        if (stack.isEmpty) return
        val entry = Entry(stack, x, y, size, showTooltip, customCount, scissor)
        captureTarget?.add(entry) ?: pending.add(entry)
    }

    fun addEntry(entry: Entry) { if (!entry.stack.isEmpty) pending.add(entry) }

    fun flush(context: GuiGraphics, mx: Int, my: Int) {
        if (pending.isEmpty()) return
        val dpr     = NVGRenderer.devicePixelRatio()
        val toGuiPx = dpr / mc.window.guiScale.toFloat()
        val originX = PVState.originX
        val originY = PVState.originY
        val scale   = PVState.scale

        for (e in pending) {
            val gx = ((originX + e.x * scale) * toGuiPx).toInt()
            val gy = ((originY + e.y * scale) * toGuiPx).toInt()
            val gs = e.size * scale * toGuiPx
            val sf = gs / 16f

            val sc = e.scissor
            if (sc != null) context.enableScissor(
                ((originX + sc[0] * scale) * toGuiPx).toInt(),
                ((originY + sc[1] * scale) * toGuiPx).toInt(),
                ((originX + sc[2] * scale) * toGuiPx).toInt(),
                ((originY + sc[3] * scale) * toGuiPx).toInt(),
            )

            context.pose().pushMatrix()
            context.pose().translate(gx.toFloat(), gy.toFloat())
            context.pose().scale(sf, sf)
            PVItemRenderer.draw(context, e.stack, 0, 0)
            context.renderItemDecorations(mc.font, e.stack, 0, 0, e.customCount)
            context.pose().popMatrix()

            if (sc != null) context.disableScissor()

            if (e.showTooltip) {
                val nvgMx = (mx * (mc.window.width  / dpr) / context.guiWidth()  - originX) / scale
                val nvgMy = (my * (mc.window.height / dpr) / context.guiHeight() - originY) / scale
                if (nvgMx >= e.x && nvgMx < e.x + e.size && nvgMy >= e.y && nvgMy < e.y + e.size)
                    context.setTooltipForNextFrame(mc.font, e.stack, mx, my)
            }
        }
        pending.clear()
    }
}

object EntityQueue {

    data class Entry(
        val entity: LivingEntity,
        val x: Float, val y: Float, val w: Float, val h: Float,
        val scale: Int = -1,
    )

    val pending = mutableListOf<Entry>()
    var captureTarget: MutableList<Entry>? = null

    fun queue(entity: LivingEntity, x: Float, y: Float, w: Float, h: Float, scale: Int = -1) {
        val entry = Entry(entity, x, y, w, h, scale)
        captureTarget?.add(entry) ?: pending.add(entry)
    }

    fun addEntry(entry: Entry) { pending.add(entry) }

    fun flush(context: GuiGraphics, mx: Int, my: Int) {
        if (pending.isEmpty()) return
        val dpr     = NVGRenderer.devicePixelRatio()
        val toGuiPx = dpr / mc.window.guiScale.toFloat()
        val originX = PVState.originX
        val originY = PVState.originY
        val scale   = PVState.scale

        for (e in pending) {
            val sx = ((originX + e.x * scale) * toGuiPx).toInt()
            val sy = ((originY + e.y * scale) * toGuiPx).toInt()
            val sw = (e.w * scale * toGuiPx).toInt()
            val sh = (e.h * scale * toGuiPx).toInt()
            val entityScale = if (e.scale < 0) (sh * 0.4f).toInt() else e.scale
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                context, sx, sy, sx + sw, sy + sh,
                entityScale, 0.0625f,
                mx.toFloat(), my.toFloat(),
                e.entity
            )
        }
        pending.clear()
    }
}
