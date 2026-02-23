package com.odtheking.odinaddon.pvgui

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.OdinMod.scope
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import com.odtheking.odin.utils.ui.rendering.NVGPIPRenderer
import com.odtheking.odinaddon.features.impl.skyblock.ProfileViewerModule
import com.odtheking.odinaddon.pvgui.utils.api.RequestUtils
import com.odtheking.odinaddon.pvgui.PVLayout.LOGICAL_H
import com.odtheking.odinaddon.pvgui.PVLayout.LOGICAL_W
import com.odtheking.odinaddon.pvgui.PVLayout.MAIN_H
import com.odtheking.odinaddon.pvgui.PVLayout.MAIN_W
import com.odtheking.odinaddon.pvgui.PVLayout.MAIN_X
import com.odtheking.odinaddon.pvgui.PVLayout.MAIN_Y
import com.odtheking.odinaddon.pvgui.PVLayout.PADDING
import com.odtheking.odinaddon.pvgui.PVLayout.SEPARATOR_X
import com.odtheking.odinaddon.pvgui.PVLayout.SIDEBAR_W
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

    private const val BTN_X = PADDING
    private const val BTN_SPACING = 6f
    private val BTN_COUNT get() = PVState.pages.size
    private val BTN_H get() = (LOGICAL_H - PADDING * 2f - BTN_SPACING * (BTN_COUNT - 1)) / BTN_COUNT
    private val BTN_W = SIDEBAR_W - PADDING * 2f

    private val COL_GUI_BG get() = Theme.bg
    private val COL_BTN_SEL get() = Theme.accent
    private val COL_BTN_HOVER get() = Theme.btnHover
    private val COL_BTN_NORMAL get() = Theme.btnNormal
    private val COL_SEPARATOR get() = Theme.separator
    private val GUI_RADIUS get() = Theme.round
    private val BTN_RADIUS get() = Theme.round
    private const val TEXT_SIZE = 16f

    private var scale = 1f
    private var originX = 0f
    private var originY = 0f
    private var mouseX = 0.0
    private var mouseY = 0.0

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

        PVState.pages.forEachIndexed { i, page ->
            if (ctx.isHovered(mouseX, mouseY, BTN_X, buttonLogicalY(i), BTN_W, BTN_H)) {
                if (PVState.currentPage !== page) {
                    PVState.currentPage = page
                    page.onOpen()
                }
                return true
            }
        }

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
        ctx.line(SEPARATOR_X, 12f, SEPARATOR_X, LOGICAL_H - 12f, 1f, COL_SEPARATOR)

        PVState.pages.forEachIndexed { i, page ->
            val by = buttonLogicalY(i)
            val isSelected = page === PVState.currentPage
            val isHovered = ctx.isHovered(mouseX, mouseY, BTN_X, by, BTN_W, BTN_H)

            val bgColor = when {
                isSelected -> COL_BTN_SEL
                isHovered -> COL_BTN_HOVER
                else -> COL_BTN_NORMAL
            }
            ctx.rect(BTN_X, by, BTN_W, BTN_H, bgColor, BTN_RADIUS)

            val textColor = if (isSelected || isHovered) Color(255, 255, 255) else Color(180, 180, 180)
            val tw = ctx.textWidth(page.name, TEXT_SIZE)
            ctx.text(page.name, BTN_X + (BTN_W - tw) / 2f, by + (BTN_H - TEXT_SIZE) / 2f - 1f, TEXT_SIZE, textColor)
        }
    }

    private fun drawMainArea(ctx: DrawContext) {
        if (PVState.playerData == null) {
            val tw = ctx.textWidth(PVState.loadText, TEXT_SIZE)
            ctx.text(PVState.loadText, MAIN_X + (MAIN_W - tw) / 2f, MAIN_Y + (MAIN_H - TEXT_SIZE) / 2f, TEXT_SIZE, Color(170, 170, 170))
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

    private fun buttonLogicalY(index: Int) = PADDING + index * (BTN_H + BTN_SPACING)
}

interface PageHandler {
    val name: String
    fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float, mouseX: Double, mouseY: Double)
    fun onClick(ctx: DrawContext, mouseX: Double, mouseY: Double) {}
    fun onScroll(delta: Double) {}
    fun onOpen() {}
}