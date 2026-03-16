package com.odtheking.odinaddon.pvgui

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.ui.rendering.NVGPIPRenderer
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.dsl.ButtonsDsl
import com.odtheking.odinaddon.pvgui.dsl.RenderQueue
import com.odtheking.odinaddon.pvgui.dsl.buttons
import com.odtheking.odinaddon.pvgui.pages.OverviewPage
import com.odtheking.odinaddon.pvgui.utils.Theme
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.helpers.McClient
import kotlin.math.min

object PVScreen : Screen(Component.literal("Profile Viewer")) {
    private const val SPACING = 10f
    private const val INFO_H = 46f
    private val INFO_Y get() = GUI_H - SPACING - INFO_H
    private val BTN_Y get() = SPACING
    private val BTN_AREA_H get() = INFO_Y - BTN_Y - SPACING
    private val BTN_X get() = SPACING
    private val BTN_W get() = SIDEBAR_W - SPACING * 2f

    private var sidebar: ButtonsDsl<PVPage>? = null

    override fun isPauseScreen() = false

    override fun init() { rebuildSidebar() }

    override fun onClose() {
        PVState.fullReset()
        super.onClose()
    }

    private fun rebuildSidebar() {
        sidebar = buttons(
            x = BTN_X, y = BTN_Y, w = BTN_W, h = BTN_AREA_H,
            items = PVState.pages,
            vertical = true,
            spacing = SPACING,
            textSize = 15f,
            radius = Theme.radius,
            label = { it.name },
        ) { page ->
            PVState.currentPage = page
            page.onOpen()
        }.also { it.selected = PVState.currentPage }
    }

    override fun mouseClicked(event: MouseButtonEvent, bl: Boolean): Boolean {
        if (event.button() == 0) {
            val mouseX = PVState.mouseX
            val mouseY = PVState.mouseY
            if (mouseX >= BTN_X && mouseX <= BTN_X + BTN_W && mouseY >= INFO_Y && mouseY <= INFO_Y + INFO_H) {
                val name = PVState.player?.name
                if (name != null) McClient.openUri("https://namemc.com/profile/$name")
                return true
            }
            if (sidebar?.click(mouseX, mouseY) == true) return true
            if (PVState.currentPage.click(mouseX.toDouble(), mouseY.toDouble())) return true
        }
        return super.mouseClicked(event, bl)
    }

    override fun mouseDragged(event: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        if (event.button() == 0 && PVState.currentPage === OverviewPage) {
            if (PVState.isHovered(OverviewPage.rightX, OverviewPage.dataY, OverviewPage.rightW, OverviewPage.dataH)) {
                OverviewPage.onMouseDrag(deltaX, deltaY)
                return true
            }
        }
        return super.mouseDragged(event, deltaX, deltaY)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == 256) { mc.setScreen(null); return true }
        return super.keyPressed(event)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        RenderQueue.clear()
        updateScale()

        val dpr = NVGRenderer.devicePixelRatio()
        PVState.mouseX = (mouseX * (mc.window.width / dpr) / context.guiWidth().toDouble() - PVState.originX) / PVState.scale
        PVState.mouseY = (mouseY * (mc.window.height / dpr) / context.guiHeight().toDouble() - PVState.originY) / PVState.scale

        val page = PVState.currentPage
        page.setBounds(CONTENT_X, CONTENT_Y, CONTENT_W, CONTENT_H)

        // NVG pass 1 — static chrome: background and sidebar
        NVGPIPRenderer.draw(context, 0, 0, mc.window.width, mc.window.height) {
            NVGRenderer.push()
            NVGRenderer.translate(PVState.originX, PVState.originY)
            NVGRenderer.scale(PVState.scale, PVState.scale)
            drawBackground()
            drawSidebar()
            NVGRenderer.pop()
        }

        // NVG pass 2 — page shapes: slots, panels, text
        NVGPIPRenderer.draw(context, 0, 0, mc.window.width, mc.window.height) {
            NVGRenderer.push()
            NVGRenderer.translate(PVState.originX, PVState.originY)
            NVGRenderer.scale(PVState.scale, PVState.scale)
            NVGRenderer.pushScissor(CONTENT_X, CONTENT_Y, CONTENT_W, CONTENT_H)
            if (PVState.player != null) page.draw(context, mouseX, mouseY)
            else drawLoading()
            NVGRenderer.popScissor()
            NVGRenderer.pop()
        }

        // Item/entity pass — synchronous, correct timing
        if (PVState.player != null) page.enqueueItems(context, mouseX, mouseY)
        RenderQueue.flush(context, mouseX, mouseY)
        super.render(context, mouseX, mouseY, delta)
    }

    private fun drawBackground() {
        if (ProfileViewerModule.dropShadow)
            NVGRenderer.dropShadow(0f, 0f, GUI_W, GUI_H, 18f, 6f, Theme.radius)
        NVGRenderer.rect(0f, 0f, GUI_W, GUI_H, Theme.bg, Theme.radius)
    }

    private fun drawSidebar() {
        val font = NVGRenderer.defaultFont
        sidebar?.selected = PVState.currentPage
        sidebar?.draw()
        val isHovered = PVState.mouseX >= BTN_X && PVState.mouseX <= BTN_X + BTN_W
                && PVState.mouseY >= INFO_Y && PVState.mouseY <= INFO_Y + INFO_H
        NVGRenderer.rect(BTN_X, INFO_Y, BTN_W, INFO_H, if (isHovered) Theme.btnHover else Theme.btnNormal, Theme.radius)
        val name = PVState.player?.name ?: "OdinPV"
        val textWidth = NVGRenderer.textWidth(name, 15f, font)
        NVGRenderer.text(name, BTN_X + (BTN_W - textWidth) / 2f, INFO_Y + (INFO_H - 15f) / 2f, 15f, Theme.textPrimary, font)
    }

    private fun drawLoading() {
        val message = PVState.statusText
        val textWidth = NVGRenderer.textWidth(message, 30f, NVGRenderer.defaultFont)
        NVGRenderer.text(message, CONTENT_X + (CONTENT_W - textWidth) / 2f, CONTENT_Y + CONTENT_H / 2f - 15f, 30f, Theme.textSecondary, NVGRenderer.defaultFont)
    }

    private fun updateScale() {
        val dpr = NVGRenderer.devicePixelRatio()
        val nvgW = mc.window.width / dpr
        val nvgH = mc.window.height / dpr
        val coverage = 0.85f * ProfileViewerModule.scale.toFloat()
        var scale = min(nvgW * coverage / GUI_W, nvgH * coverage / GUI_H)
        scale = ((scale * 2).toInt() / 2f).coerceAtLeast(0.5f)
        PVState.scale = scale
        PVState.originX = ((nvgW - GUI_W * scale) / 2f).toInt().toFloat()
        PVState.originY = ((nvgH - GUI_H * scale) / 2f).toInt().toFloat()
    }
}