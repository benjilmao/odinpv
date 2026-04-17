package com.odtheking.odinaddon.pvgui

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.ui.rendering.NVGPIPRenderer
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.dsl.ButtonsDsl
import com.odtheking.odinaddon.pvgui.dsl.RenderQueue
import com.odtheking.odinaddon.pvgui.dsl.buttons
import com.odtheking.odinaddon.pvgui.dsl.fillText
import com.odtheking.odinaddon.pvgui.utils.Theme
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.helpers.McClient
import kotlin.math.min

object PVScreen : Screen(Component.literal("Profile Viewer")) {

    private var sidebar: ButtonsDsl<PVPage>? = null

    override fun isPauseScreen() = false

    override fun init() { rebuildSidebar() }

    override fun onClose() {
        PVState.fullReset()
        super.onClose()
    }

    private fun rebuildSidebar() {
        sidebar = buttons(
            x = PAD, y = PAD,
            w = SIDEBAR_BTN_W,
            h = SIDEBAR_BTNS_AREA_H,
            items = PVState.pages,
            padding = PAD,
            vertical = true,
            textSize = 31.5f,
            radius = Theme.radius,
            label = { it.name },
        ) { page ->
            PVState.currentPage = page
            page.onOpen()
        }.also { it.selected = PVState.currentPage }
    }

    override fun mouseClicked(event: MouseButtonEvent, bl: Boolean): Boolean {
        if (event.button() == 0) {
            val mx = PVState.mouseX; val my = PVState.mouseY
            if (mx >= PAD && mx <= PAD + SIDEBAR_BTN_W &&
                my >= SIDEBAR_INFO_Y && my <= SIDEBAR_INFO_Y + SIDEBAR_INFO_H) {
                PVState.player?.name?.let { McClient.openUri("https://namemc.com/profile/$it") }
                return true
            }
            if (sidebar?.click(mx, my) == true) return true
            if (PVState.currentPage.click(mx, my)) return true
        }
        return super.mouseClicked(event, bl)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, hA: Double, vA: Double): Boolean {
        val dpr = NVGRenderer.devicePixelRatio()
        val nvgMX = (mouseX * (mc.window.width / dpr) / width.toDouble() - PVState.originX) / PVState.scale
        val nvgMY = (mouseY * (mc.window.height / dpr) / height.toDouble() - PVState.originY) / PVState.scale
        if (PVState.currentPage.scroll(nvgMX, nvgMY, vA)) return true
        return super.mouseScrolled(mouseX, mouseY, hA, vA)
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
        page.setBounds(CONTENT_X, CONTENT_Y, MAIN_W, MAIN_H)

        NVGPIPRenderer.draw(context, 0, 0, mc.window.width, mc.window.height) {
            NVGRenderer.push()
            NVGRenderer.translate(PVState.originX, PVState.originY)
            NVGRenderer.scale(PVState.scale, PVState.scale)

            drawBackground()
            drawSidebar()

            NVGRenderer.pushScissor(CONTENT_X, CONTENT_Y, MAIN_W, MAIN_H)
            if (PVState.player != null) page.draw(context, mouseX, mouseY)
            else drawLoading()
            NVGRenderer.popScissor()

            NVGRenderer.pop()
        }

        if (PVState.player != null) page.enqueueItems(context, mouseX, mouseY)
        RenderQueue.flush(context, mouseX, mouseY)
        super.render(context, mouseX, mouseY, delta)
    }

    private fun drawBackground() {
        if (ProfileViewerModule.dropShadow)
            NVGRenderer.dropShadow(0f, 0f, TOTAL_W, TOTAL_H, 18f, 6f, Theme.radius)
        NVGRenderer.rect(0f, 0f, TOTAL_W, TOTAL_H, Theme.bg, Theme.radius)
    }

    private fun drawSidebar() {
        val sb = sidebar ?: return.also { rebuildSidebar() }
        sb.selected = PVState.currentPage
        sb.draw()

        val isHov = PVState.mouseX >= PAD && PVState.mouseX <= PAD + SIDEBAR_BTN_W && PVState.mouseY >= SIDEBAR_INFO_Y && PVState.mouseY <= SIDEBAR_INFO_Y + SIDEBAR_INFO_H
        NVGRenderer.rect(PAD, SIDEBAR_INFO_Y, SIDEBAR_BTN_W, SIDEBAR_INFO_H,
            if (isHov) Theme.btnHover else Theme.btnNormal, Theme.radius)

        val infoText = if (PVState.currentPage !== PVState.pages.first())
            PVState.player?.name ?: "OdinPV" else "OdinPV"
        fillText(
            infoText,
            SIDEBAR_CENTER_X,
            SIDEBAR_INFO_Y + SIDEBAR_INFO_H / 2f,
            SIDEBAR_BTN_W - PAD * 2f,
            SIDEBAR_INFO_H - PAD * 2f,
            Theme.textPrimary,
        )
    }

    private fun drawLoading() {
        val msg = PVState.statusText
        val tw = NVGRenderer.textWidth(msg, 30f, NVGRenderer.defaultFont)
        NVGRenderer.text(msg, CONTENT_X + MAIN_W / 2f - tw / 2f,
            CONTENT_Y + MAIN_H / 2f - 15f, 30f, Theme.textSecondary, NVGRenderer.defaultFont)
    }

    private fun updateScale() {
        val dpr = NVGRenderer.devicePixelRatio()
        val nvgW = mc.window.width  / dpr
        val nvgH = mc.window.height / dpr
        val coverage = 0.85f * ProfileViewerModule.scale.toFloat()
        var scale = min(nvgW * coverage / TOTAL_W, nvgH * coverage / TOTAL_H)
        scale = ((scale * 2).toInt() / 2f).coerceAtLeast(0.5f)
        PVState.scale = scale
        PVState.originX = ((nvgW - TOTAL_W * scale) / 2f).toInt().toFloat()
        PVState.originY = ((nvgH - TOTAL_H * scale) / 2f).toInt().toFloat()
    }
}