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
import tech.thatgravyboat.skyblockapi.helpers.McClient
import kotlin.math.min

object PVScreen : Screen(Component.literal("Profile Viewer")) {
    private const val SP = 10f
    private const val INFO_H = 46f
    private val INFO_Y get() = GUI_H - SP - INFO_H
    private val BTN_Y get() = SP
    private val BTN_AREA_H get() = INFO_Y - BTN_Y - SP
    private val BTN_X get() = SP
    private val BTN_W get() = SIDEBAR_W - SP * 2f

    private var sidebar: ButtonsDsl<PVPage>? = null

    override fun isPauseScreen() = false

    override fun init() {
        rebuildSidebar()
    }

    override fun onClose() {
        PVState.fullReset()
        super.onClose()
    }

    private fun rebuildSidebar() {
        sidebar = buttons(
            x = BTN_X,
            y = BTN_Y,
            w = BTN_W,
            h = BTN_AREA_H,
            items = PVState.pages,
            vertical = true,
            spacing = SP,
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
            val mx = PVState.mouseX
            val my = PVState.mouseY
            if (mx >= BTN_X && mx <= BTN_X + BTN_W && my >= INFO_Y && my <= INFO_Y + INFO_H) {
                val name = PVState.player?.name
                if (name != null) McClient.openUri("https://namemc.com/profile/$name")
                return true
            }
            if (sidebar?.click(mx, my) == true) return true
            if (PVState.currentPage.click(mx, my)) return true
        }
        return super.mouseClicked(event, bl)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == 256) { mc.setScreen(null); return true }
        return super.keyPressed(event)
    }

    override fun render(context: GuiGraphics, mx: Int, my: Int, delta: Float) {
        ItemQueue.pending.clear()
        EntityQueue.pending.clear()
        updateScale()

        val dpr = NVGRenderer.devicePixelRatio()
        PVState.mouseX = (mx * (mc.window.width / dpr) / context.guiWidth().toDouble() - PVState.originX) / PVState.scale
        PVState.mouseY = (my * (mc.window.height / dpr) / context.guiHeight().toDouble() - PVState.originY) / PVState.scale

        val page = PVState.currentPage
        page.setBounds(CONTENT_X, CONTENT_Y, CONTENT_W, CONTENT_H)

        NVGPIPRenderer.draw(context, 0, 0, mc.window.width, mc.window.height) {
            NVGRenderer.push()
            NVGRenderer.translate(PVState.originX, PVState.originY)
            NVGRenderer.scale(PVState.scale, PVState.scale)

            drawBackground()
            drawSidebar()

            val current = PVState.currentPage
            current.capturedItems.clear()
            current.capturedEntities.clear()
            ItemQueue.captureTarget = current.capturedItems
            EntityQueue.captureTarget = current.capturedEntities
            NVGRenderer.pushScissor(CONTENT_X, CONTENT_Y, CONTENT_W, CONTENT_H)
            if (PVState.player != null) current.draw()
            else drawLoading()
            NVGRenderer.popScissor()
            ItemQueue.captureTarget = null
            EntityQueue.captureTarget = null

            NVGRenderer.pop()
        }

        super.render(context, mx, my, delta)
        page.replayQueues()
        ItemQueue.flush(context, mx, my)
        EntityQueue.flush(context, mx, my)
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
        val tw = NVGRenderer.textWidth(name, 15f, font)
        NVGRenderer.text(name, BTN_X + (BTN_W - tw) / 2f, INFO_Y + (INFO_H - 15f) / 2f, 15f, Theme.textPrimary, font)
    }

    private fun drawLoading() {
        val msg = PVState.statusText
        val tw = NVGRenderer.textWidth(msg, 30f, NVGRenderer.defaultFont)
        NVGRenderer.text(
            msg,
            CONTENT_X + (CONTENT_W - tw) / 2f,
            CONTENT_Y + CONTENT_H / 2f - 15f,
            30f, Theme.textSecondary, NVGRenderer.defaultFont
        )
    }

    private fun updateScale() {
        val dpr = NVGRenderer.devicePixelRatio()
        val nvgW = mc.window.width / dpr
        val nvgH = mc.window.height / dpr
        val coverage = 0.85f * ProfileViewerModule.scale.toFloat()
        var sc = min(nvgW * coverage / GUI_W, nvgH * coverage / GUI_H)
        sc = ((sc * 2).toInt() / 2f).coerceAtLeast(0.5f)
        PVState.scale = sc
        PVState.originX = ((nvgW - GUI_W * sc) / 2f).toInt().toFloat()
        PVState.originY = ((nvgH - GUI_H * sc) / 2f).toInt().toFloat()
    }
}