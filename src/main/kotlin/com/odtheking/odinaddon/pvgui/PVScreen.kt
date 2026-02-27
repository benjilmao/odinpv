package com.odtheking.odinaddon.pvgui

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.OdinMod.scope
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odin.utils.ui.rendering.NVGPIPRenderer
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.utils.api.RequestUtils
import com.odtheking.odinaddon.pvgui.components.ButtonColumn
import com.odtheking.odinaddon.pvgui.utils.Theme
import kotlinx.coroutines.launch
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import kotlin.math.min

object PVScreen : Screen(Component.literal("Profile Viewer")) {
    private val itemWidgets = mutableListOf<AbstractWidget>()
    private val GUI_RADIUS get() = Theme.round
    private val COL_GUI_BG get() = Theme.bg
    private val COL_SEPARATOR get() = Theme.separator

    private var scale = 1f
    private var originX = 0f
    private var originY = 0f
    private var mouseX = 0.0
    private var mouseY = 0.0

    private val sidebarButtons = ButtonColumn(
        x = PADDING,
        y = PADDING,
        w = SIDEBAR_W - PADDING * 2f,
        h = LOGICAL_H - PADDING * 2f,
        items = PVState.pages,
        spacing = 6f,
        label = { it.name },
        textSize = 16f,
    ) {
        onSelect { page -> PVState.currentPage = page; page.onOpen() }
    }

    private fun makeCtx() = DrawContext(scale, originX, originY, NVGRenderer.defaultFont, itemWidgets)

    fun loadPlayer(name: String) = scope.launch {
        PVState.reset()
        PVState.loadText = "Loading $name..."
        RequestUtils.getProfile(name).fold(
            onSuccess = { data ->
                if (data.profileData.profiles.isEmpty()) {
                    PVState.loadText = "No profiles found for ${data.name}."
                } else {
                    PVState.playerData = data
                    PVState.profileName =
                        data.profileData.profiles.find { it.selected }?.cuteName
                            ?: data.profileData.profiles.firstOrNull()?.cuteName
                }
            },
            onFailure = { e ->
                modMessage(e.message ?: "Unknown error")
                PVState.loadText = "Failed to load profile."
            }
        )
    }

    override fun onClose() { PVState.reset(); super.onClose() }
    override fun isPauseScreen() = false

    override fun mouseClicked(event: MouseButtonEvent, bl: Boolean): Boolean {
        if (event.button() != 0) return super.mouseClicked(event, bl)
        val ctx = makeCtx()

        if (sidebarButtons.click(ctx, mouseX, mouseY)) return true
        PVState.currentPage.onClick(ctx, mouseX, mouseY)
        return super.mouseClicked(event, bl)
    }

    override fun mouseScrolled(mx: Double, my: Double, hAmount: Double, vAmount: Double): Boolean {
        val ctx = makeCtx()
        if (ctx.isHovered(mouseX, mouseY, MAIN_X, MAIN_Y, MAIN_W, MAIN_H)) {
            PVState.currentPage.onScroll(vAmount)
        }
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == 256) { mc.setScreen(null); return true }
        return super.keyPressed(event)
    }

    override fun render(context: GuiGraphics, mx: Int, my: Int, delta: Float) {
        recalcScale()
        val dpr = NVGRenderer.devicePixelRatio()
        mouseX = mx * (mc.window.width / dpr) / context.guiWidth().toDouble()
        mouseY = my * (mc.window.height / dpr) / context.guiHeight().toDouble()
        val ctx = makeCtx()

        NVGPIPRenderer.draw(context, 0, 0, mc.window.width, mc.window.height) {
            NVGRenderer.push()
            NVGRenderer.translate(originX, originY)
            NVGRenderer.scale(scale, scale)
            drawBackground(ctx)
            drawSidebar(ctx)
            drawMainArea(ctx)
            NVGRenderer.pop()
        }
        super.render(context, mx, my, delta)

        val guiScale = mc.window.guiScale.toFloat()
        val toGuiPx = dpr / guiScale
        val sx = ((originX + MAIN_X * scale) * toGuiPx).toInt()
        val sy = ((originY + MAIN_Y * scale) * toGuiPx).toInt()
        val sw = (MAIN_W * scale * toGuiPx).toInt()
        val sh = (MAIN_H * scale * toGuiPx).toInt()
        context.enableScissor(sx, sy, sx + sw, sy + sh)
        itemWidgets.forEach { it.render(context, mx, my, delta) }
        context.disableScissor()
        itemWidgets.clear()

        NVGPIPRenderer.draw(context, 0, 0, mc.window.width, mc.window.height) {
            NVGRenderer.push()
            NVGRenderer.translate(originX, originY)
            NVGRenderer.scale(scale, scale)
            ctx.overlayText.forEach { it() }
            NVGRenderer.pop()
        }
        ctx.overlayText.clear()
    }

    private fun drawBackground(ctx: DrawContext) {
        if (ProfileViewerModule.dropShadow)
            NVGRenderer.dropShadow(0f, 0f, LOGICAL_W, LOGICAL_H, 12f, 8f, GUI_RADIUS)
        ctx.rect(0f, 0f, LOGICAL_W, LOGICAL_H, COL_GUI_BG, GUI_RADIUS)
    }

    private fun drawSidebar(ctx: DrawContext) {
        if (PVState.playerData == null) return
        ctx.line(SEPARATOR_X, 12f, SEPARATOR_X, LOGICAL_H - 12f, 1f, COL_SEPARATOR)

        sidebarButtons.selected = PVState.currentPage
        sidebarButtons.draw(ctx, mouseX, mouseY)
    }

    private fun drawMainArea(ctx: DrawContext) {
        if (PVState.playerData == null) {
            val size = 40f
            val tw = ctx.textWidth(PVState.loadText, size)
            ctx.text(PVState.loadText, (LOGICAL_W - tw) / 2f, (LOGICAL_H - size) / 2f, size, Color(170, 170, 170))
            return
        }
        ctx.pushScissor(MAIN_X, MAIN_Y, MAIN_W, MAIN_H)
        PVState.currentPage.draw(ctx, MAIN_X, MAIN_Y, MAIN_W, MAIN_H, mouseX, mouseY)
        ctx.popScissor()
    }

    private fun recalcScale() {
        val dpr = NVGRenderer.devicePixelRatio()
        val nvgW = mc.window.width / dpr
        val nvgH = mc.window.height / dpr
        val coverage = 0.8f * ProfileViewerModule.scale.toFloat()
        scale = min(nvgW * coverage / LOGICAL_W, nvgH * coverage / LOGICAL_H)
        scale = (scale * 2).toInt() / 2f
        originX = ((nvgW - LOGICAL_W * scale) / 2f).toInt().toFloat()
        originY = ((nvgH - LOGICAL_H * scale) / 2f).toInt().toFloat()
    }
}

interface PageHandler {
    val name: String
    fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, mouseX: Double, mouseY: Double)
    fun onClick(ctx: DrawContext, mouseX: Double, mouseY: Double) {}
    fun onScroll(delta: Double) {}
    fun onOpen() {}
}