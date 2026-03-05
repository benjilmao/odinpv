package com.odtheking.odinaddon.pvgui

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.ui.rendering.NVGPIPRenderer
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.dsl.ButtonsDsl
import com.odtheking.odinaddon.pvgui.dsl.EntityQueue
import com.odtheking.odinaddon.pvgui.dsl.ItemQueue
import com.odtheking.odinaddon.pvgui.dsl.buttons
import com.odtheking.odinaddon.pvgui.utils.Theme
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import kotlin.math.min

object PVScreen : Screen(Component.literal("Profile Viewer")) {

    // HC sidebar constants
    private const val SP      = 10f
    private const val ACCENT_H = 2f
    private const val INFO_H   = 34f
    private val INFO_Y   get() = GUI_H - SP - INFO_H
    private val BTN_Y    get() = SP + ACCENT_H + SP
    private val BTN_AREA_H get() = INFO_Y - BTN_Y - SP
    private val BTN_X    get() = SP
    private val BTN_W    get() = SIDEBAR_W - SP * 2f

    private var sidebar: ButtonsDsl<PVPage>? = null

    override fun isPauseScreen() = false

    override fun init() {
        PVState.reset()
        rebuildSidebar()
    }

    override fun onClose() {
        PVState.reset()
        super.onClose()
    }

    private fun rebuildSidebar() {
        sidebar = buttons(
            x        = BTN_X,
            y        = BTN_Y,
            w        = BTN_W,
            h        = BTN_AREA_H,
            items    = PVState.pages,
            vertical = true,
            spacing  = SP,
            textSize = 15f,
            radius   = Theme.radius,
            label    = { it.name },
        ) { page ->
            PVState.currentPage = page
            page.onOpen()
        }.also { it.selected = PVState.currentPage }
    }

    // ── Input ──────────────────────────────────────────────────────────────────

    override fun mouseClicked(event: MouseButtonEvent, bl: Boolean): Boolean {
        if (event.button() == 0) {
            if (sidebar?.click(PVState.mouseX, PVState.mouseY) == true) return true
            if (PVState.currentPage.click(PVState.mouseX, PVState.mouseY)) return true
        }
        return super.mouseClicked(event, bl)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == 256) { mc.setScreen(null); return true }
        return super.keyPressed(event)
    }

    // ── Render ─────────────────────────────────────────────────────────────────

    override fun render(context: GuiGraphics, mx: Int, my: Int, delta: Float) {
        updateScale()

        val dpr = NVGRenderer.devicePixelRatio()
        PVState.mouseX = (mx * (mc.window.width / dpr) / context.guiWidth().toDouble()  - PVState.originX) / PVState.scale
        PVState.mouseY = (my * (mc.window.height / dpr) / context.guiHeight().toDouble() - PVState.originY) / PVState.scale

        val page = PVState.currentPage
        page.setBounds(CONTENT_X, CONTENT_Y, CONTENT_W, CONTENT_H)

        NVGPIPRenderer.draw(context, 0, 0, mc.window.width, mc.window.height) {
            NVGRenderer.push()
            NVGRenderer.translate(PVState.originX, PVState.originY)
            NVGRenderer.scale(PVState.scale, PVState.scale)

            drawBackground()
            drawSidebar()

            // Pass 1 — NVG drawing, items captured
            val current = PVState.currentPage
            current.capturedItems.clear()
            current.capturedEntities.clear()
            ItemQueue.captureTarget   = current.capturedItems
            EntityQueue.captureTarget = current.capturedEntities
            NVGRenderer.pushScissor(CONTENT_X, CONTENT_Y, CONTENT_W, CONTENT_H)
            if (PVState.player != null) current.draw()
            else drawLoading()
            NVGRenderer.popScissor()
            ItemQueue.captureTarget   = null
            EntityQueue.captureTarget = null

            NVGRenderer.pop()
        }

        // Pass 2 — MC item/entity rendering outside NVG context
        super.render(context, mx, my, delta)
        page.replayQueues()
        ItemQueue.flush(context, mx, my)
        EntityQueue.flush(context, mx, my)
    }

    // ── Draw helpers ───────────────────────────────────────────────────────────

    private fun drawBackground() {
        if (ProfileViewerModule.dropShadow)
            NVGRenderer.dropShadow(0f, 0f, GUI_W, GUI_H, 18f, 6f, Theme.radius)
        NVGRenderer.rect(0f, 0f, GUI_W, GUI_H, Theme.bg, Theme.radius)
        // Sidebar panel — rounded left, flat right edge
        NVGRenderer.rect(0f, 0f, SIDEBAR_W, GUI_H, Theme.panel, Theme.radius)
        NVGRenderer.rect(SIDEBAR_W / 2f, 0f, SIDEBAR_W / 2f, GUI_H, Theme.panel, 0f)
        NVGRenderer.line(DIVIDER_X, SP * 1.5f, DIVIDER_X, GUI_H - SP * 1.5f, 1f, Theme.separator)
    }

    private fun drawSidebar() {
        val font = NVGRenderer.defaultFont
        // Accent bar below top pad
        NVGRenderer.rect(BTN_X, SP, BTN_W, ACCENT_H, Theme.accent, 0f)
        sidebar?.selected = PVState.currentPage
        sidebar?.draw()
        // Info box at sidebar bottom
        NVGRenderer.rect(BTN_X, INFO_Y, BTN_W, INFO_H, Theme.bg, Theme.radius)
        val info = PVState.player?.name ?: "OdinPV"
        val tw   = NVGRenderer.textWidth(info, 13f, font)
        NVGRenderer.text(info, BTN_X + (BTN_W - tw) / 2f, INFO_Y + (INFO_H - 13f) / 2f, 13f, Theme.textPrimary, font)
    }

    private fun drawLoading() {
        val msg = PVState.statusText
        val tw  = NVGRenderer.textWidth(msg, 30f, NVGRenderer.defaultFont)
        NVGRenderer.text(
            msg,
            CONTENT_X + (CONTENT_W - tw) / 2f,
            CONTENT_Y + CONTENT_H / 2f - 15f,
            30f, Theme.textSecondary, NVGRenderer.defaultFont
        )
    }

    // ── Scale ──────────────────────────────────────────────────────────────────

    private fun updateScale() {
        val dpr  = NVGRenderer.devicePixelRatio()
        val nvgW = mc.window.width  / dpr
        val nvgH = mc.window.height / dpr
        val coverage = 0.85f * ProfileViewerModule.scale.toFloat()
        var sc = min(nvgW * coverage / GUI_W, nvgH * coverage / GUI_H)
        sc = ((sc * 2).toInt() / 2f).coerceAtLeast(0.5f)
        PVState.scale   = sc
        PVState.originX = ((nvgW - GUI_W * sc) / 2f).toInt().toFloat()
        PVState.originY = ((nvgH - GUI_H * sc) / 2f).toInt().toFloat()
    }
}
