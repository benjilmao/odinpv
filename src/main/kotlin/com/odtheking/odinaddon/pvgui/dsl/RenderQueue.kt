package com.odtheking.odinaddon.pvgui.dsl

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.pvgui.PVState
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

object RenderQueue {

    class ItemEntry(
        val stack: ItemStack,
        val gx: Int,
        val gy: Int,
        val scale: Float,
        val showTooltip: Boolean,
        val count: String?,
        val scissor: IntArray?,
        val nvgX: Float,
        val nvgY: Float,
        val nvgSize: Float,
    )

    class EntityEntry(
        val entity: LivingEntity,
        val x: Float,
        val y: Float,
        val w: Float,
        val h: Float,
    )

    private val pendingItems = mutableListOf<ItemEntry>()
    private val pendingEntities = mutableListOf<EntityEntry>()

    fun enqueueItem(
        stack: ItemStack,
        nvgX: Float,
        nvgY: Float,
        slotSize: Float,
        showTooltip: Boolean = true,
        count: String? = null,
        scissor: FloatArray? = null,
    ) {
        if (stack.isEmpty) return
        val dpr = NVGRenderer.devicePixelRatio()
        val toGuiPx = dpr / mc.window.guiScale.toFloat()
        val originX = PVState.originX
        val originY = PVState.originY
        val pvScale = PVState.scale
        val gx = ((originX + nvgX * pvScale) * toGuiPx).toInt()
        val gy = ((originY + nvgY * pvScale) * toGuiPx).toInt()
        val guiSize = slotSize * pvScale * toGuiPx
        val itemScale = guiSize / 16f
        val guiScissor = scissor?.let {
            intArrayOf(
                ((originX + it[0] * pvScale) * toGuiPx).toInt(),
                ((originY + it[1] * pvScale) * toGuiPx).toInt(),
                ((originX + it[2] * pvScale) * toGuiPx).toInt(),
                ((originY + it[3] * pvScale) * toGuiPx).toInt(),
            )
        }
        pendingItems.add(ItemEntry(stack, gx, gy, itemScale, showTooltip, count, guiScissor, nvgX, nvgY, slotSize))
    }

    fun enqueueEntity(entity: LivingEntity, x: Float, y: Float, w: Float, h: Float) {
        pendingEntities.add(EntityEntry(entity, x, y, w, h))
    }

    fun flush(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val dpr = NVGRenderer.devicePixelRatio()
        val toGuiPx = dpr / mc.window.guiScale.toFloat()
        val originX = PVState.originX
        val originY = PVState.originY
        val pvScale = PVState.scale

        for (entry in pendingItems) {
            entry.scissor?.let { context.enableScissor(it[0], it[1], it[2], it[3]) }
            context.pose().pushMatrix()
            context.pose().identity()
            context.pose().translate(entry.gx.toFloat(), entry.gy.toFloat())
            context.pose().scale(entry.scale, entry.scale)
            PVItemRenderer.draw(context, entry.stack, 0, 0)
            context.renderItemDecorations(mc.font, entry.stack, 0, 0, entry.count)
            context.pose().popMatrix()
            entry.scissor?.let { context.disableScissor() }
            if (entry.showTooltip) {
                val nvgMouseX = (mouseX * (mc.window.width / dpr) / context.guiWidth() - originX) / pvScale
                val nvgMouseY = (mouseY * (mc.window.height / dpr) / context.guiHeight() - originY) / pvScale
                if (nvgMouseX >= entry.nvgX && nvgMouseX < entry.nvgX + entry.nvgSize &&
                    nvgMouseY >= entry.nvgY && nvgMouseY < entry.nvgY + entry.nvgSize)
                    context.setTooltipForNextFrame(mc.font, entry.stack, mouseX, mouseY)
            }
        }

        for (entry in pendingEntities) {
            val sx = ((originX + entry.x * pvScale) * toGuiPx).toInt()
            val sy = ((originY + entry.y * pvScale) * toGuiPx).toInt()
            val sw = (entry.w * pvScale * toGuiPx).toInt()
            val sh = (entry.h * pvScale * toGuiPx).toInt()
            val entityScale = (sh * 0.4f).toInt()
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                context, sx, sy, sx + sw, sy + sh,
                entityScale, 0.0625f,
                mouseX.toFloat(), mouseY.toFloat(),
                entry.entity,
            )
        }

        pendingItems.clear()
        pendingEntities.clear()
    }

    fun clear() {
        pendingItems.clear()
        pendingEntities.clear()
    }
}