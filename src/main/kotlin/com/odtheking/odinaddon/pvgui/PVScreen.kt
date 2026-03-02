package com.odtheking.odinaddon.pvgui

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.OdinMod.scope
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.ui.rendering.NVGPIPRenderer
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.components.Buttons
import com.odtheking.odinaddon.pvgui.core.Component as UIComponent
import com.odtheking.odinaddon.pvgui.core.RenderContext
import com.odtheking.odinaddon.pvgui.core.Renderer
import com.odtheking.odinaddon.pvgui.utils.Theme
import com.odtheking.odinaddon.pvgui.utils.api.RequestUtils
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
    private val overlays = mutableListOf<() -> Unit>()
    private val registry = mutableListOf<UIComponent>()

    private var scale = 1f
    private var originX = 0f
    private var originY = 0f
    private var mouseX = 0.0
    private var mouseY = 0.0

    private val sidebar = Buttons(
        items = PVState.pages,
        spacing = 6f,
        textSize = 16f,
        vertical = true,
        label = { it.name }
    ) { page ->
        PVState.currentPage = page
        page.onOpen()
    }

    private fun ctx() = RenderContext(
        scale = scale,
        originX = originX,
        originY = originY,
        mouseX = mouseX,
        mouseY = mouseY,
        itemWidgets = itemWidgets,
        overlayText = overlays,
        clickRegistry = registry
    )

    fun loadPlayer(name: String) = scope.launch {
        PVState.reset()
        PVState.statusText = "Loading $name..."
        RequestUtils.getProfile(name).fold(
            onSuccess = { data ->
                if (data.profileData.profiles.isEmpty()) {
                    PVState.statusText = "No profiles found for ${data.name}."
                } else {
                    PVState.player = data
                    PVState.profileName = data.profileData.profiles.find { it.selected }?.cuteName
                        ?: data.profileData.profiles.firstOrNull()?.cuteName
                }
            },
            onFailure = { e ->
                modMessage(e.message ?: "Unknown error")
                PVState.statusText = "Failed to load profile."
            }
        )
    }

    override fun onClose() { PVState.reset(); super.onClose() }
    override fun isPauseScreen() = false

    override fun mouseClicked(event: MouseButtonEvent, bl: Boolean): Boolean {
        if (event.button() != 0) return super.mouseClicked(event, bl)
        val ctx = ctx()
        for (i in registry.indices.reversed()) {
            if (registry[i].click(ctx, mouseX, mouseY)) return true
        }
        return super.mouseClicked(event, bl)
    }

    override fun mouseScrolled(mx: Double, my: Double, hAmount: Double, vAmount: Double): Boolean {
        val ctx = ctx()
        if (ctx.isHovered(CONTENT_X, CONTENT_Y, CONTENT_W, CONTENT_H)) {
            for (i in registry.indices.reversed()) {
                if (registry[i].scroll(ctx, vAmount)) return true
            }
        }
        return super.mouseScrolled(mx, my, hAmount, vAmount)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == 256) { mc.setScreen(null); return true }
        return super.keyPressed(event)
    }

    override fun render(context: GuiGraphics, mx: Int, my: Int, delta: Float) {
        updateScale()
        val dpr = NVGRenderer.devicePixelRatio()
        mouseX = mx * (mc.window.width / dpr) / context.guiWidth().toDouble()
        mouseY = my * (mc.window.height / dpr) / context.guiHeight().toDouble()
        mouseX = (mouseX - originX) / scale
        mouseY = (mouseY - originY) / scale

        registry.clear()
        val ctx = ctx()

        NVGPIPRenderer.draw(context, 0, 0, mc.window.width, mc.window.height) {
            NVGRenderer.push()
            NVGRenderer.translate(originX, originY)
            NVGRenderer.scale(scale, scale)
            drawBackground()
            drawSidebar(ctx)
            drawContent(ctx)
            NVGRenderer.pop()
        }

        super.render(context, mx, my, delta)

        val guiScale = mc.window.guiScale.toFloat()
        val toGuiPx = dpr / guiScale
        val sx = ((originX + CONTENT_X * scale) * toGuiPx).toInt()
        val sy = ((originY + CONTENT_Y * scale) * toGuiPx).toInt()
        val sw = (CONTENT_W * scale * toGuiPx).toInt()
        val sh = (CONTENT_H * scale * toGuiPx).toInt()
        context.enableScissor(sx, sy, sx + sw, sy + sh)
        itemWidgets.forEach { it.render(context, mx, my, delta) }
        context.disableScissor()
        itemWidgets.clear()

        NVGPIPRenderer.draw(context, 0, 0, mc.window.width, mc.window.height) {
            NVGRenderer.push()
            NVGRenderer.translate(originX, originY)
            NVGRenderer.scale(scale, scale)
            overlays.forEach { it() }
            NVGRenderer.pop()
        }
        overlays.clear()
    }

    private fun drawBackground() {
        if (ProfileViewerModule.dropShadow)
            Renderer.dropShadow(0f, 0f, GUI_W, GUI_H, 12f, 8f, Theme.radius)
        Renderer.rect(0f, 0f, GUI_W, GUI_H, Theme.bg, Theme.radius)
    }

    private fun drawSidebar(ctx: RenderContext) {
        if (PVState.player == null) return
        Renderer.line(DIVIDER_X, 12f, DIVIDER_X, GUI_H - 12f, 1f, Theme.separator)
        sidebar.selected = PVState.currentPage
        sidebar.setBounds(PADDING, PADDING, SIDEBAR_W - PADDING * 2f, GUI_H - PADDING * 2f)
        sidebar.draw(ctx)
    }

    private fun drawContent(ctx: RenderContext) {
        if (PVState.player == null) {
            val tw = Renderer.textWidth(PVState.statusText, 40f)
            Renderer.text(PVState.statusText, (GUI_W - tw) / 2f, (GUI_H - 40f) / 2f, 40f, 0xFFAAAAAA.toInt())
            return
        }
        ctx.clipX = CONTENT_X; ctx.clipY = CONTENT_Y
        ctx.clipW = CONTENT_W; ctx.clipH = CONTENT_H
        PVState.currentPage.setBounds(CONTENT_X, CONTENT_Y, CONTENT_W, CONTENT_H)
        PVState.currentPage.draw(ctx)
    }

    private fun updateScale() {
        val dpr = NVGRenderer.devicePixelRatio()
        val nvgW = mc.window.width / dpr
        val nvgH = mc.window.height / dpr
        val coverage = 0.8f * ProfileViewerModule.scale.toFloat()
        scale = min(nvgW * coverage / GUI_W, nvgH * coverage / GUI_H)
        scale = (scale * 2).toInt() / 2f
        originX = ((nvgW - GUI_W * scale) / 2f).toInt().toFloat()
        originY = ((nvgH - GUI_H * scale) / 2f).toInt().toFloat()
    }
}